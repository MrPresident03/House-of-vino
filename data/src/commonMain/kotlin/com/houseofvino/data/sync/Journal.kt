package com.houseofvino.data.sync

import app.cash.sqldelight.db.QueryResult
import com.houseofvino.core.sync.FieldMerge
import com.houseofvino.core.sync.FieldState
import com.houseofvino.core.sync.FieldWrite
import com.houseofvino.core.sync.Hlc
import com.houseofvino.core.sync.HlcClock
import com.houseofvino.core.sync.SyncValue
import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.db.WriteScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** One journal entry on the wire (docs/SYNC.md, "Changes"). */
@Serializable
data class WireChange(
    @SerialName("d") val device: String,
    @SerialName("s") val seq: Long,
    @SerialName("h") val hlc: String,
    @SerialName("c") val collection: String,
    @SerialName("t") val table: String,
    @SerialName("r") val rowId: String,
    /** Mutable tables: `[column, value, base]` triples. */
    @SerialName("w") val writes: List<JsonArray>? = null,
    /** Append-only tables: the whole row. */
    val row: JsonObject? = null,
)

data class IngestReport(
    val applied: Int,
    val duplicates: Int,
    val skipped: Int,
    val conflicts: Int,
    val stockConflicts: Int,
    /** Some changes were held back because earlier ones from the same phone are missing. */
    val incomplete: Boolean,
)

/**
 * The replicated change journal (ADR-13). Records this phone's changes as they commit, applies
 * other phones' changes with the field merge rules from `core.sync`, and answers "what does a
 * phone with this version vector still need?".
 *
 * All methods run inside the caller's transaction ([LocalDatabase.write]).
 */
class Journal(private val database: LocalDatabase) {
    private val json = Json { encodeDefaults = false; ignoreUnknownKeys = true }
    private var clock: HlcClock? = null
    private var clockNode: String? = null

    private val q get() = database.sync

    private fun clock(): HlcClock {
        val node = database.deviceId
        val existing = clock
        if (existing != null && clockNode == node) return existing
        val last = Hlc.parseOrNull(q.maxHlc().executeAsOneOrNull()?.hlc)
        return HlcClock(node, database::now, last?.let { Hlc(it.millis, it.counter, node) }).also { clock = it; clockNode = node }
    }

    // ---------------------------------------------------------------------------------------------
    // Recording local changes

    /** Journals the current state of row [id]: only fields that changed since its last entry. */
    fun record(table: SyncTable, id: String) {
        val row = RowCodec.read(database.driver, table, id) ?: return
        val collection = (row["collection_id"] as? JsonPrimitive)?.contentOrNull ?: return
        if (table.appendOnly) {
            if (q.hasChangeForRow(table.name, id).executeAsOne() > 0) return // ledger rows never change
            append(collection, table.name, id, row = JsonObject(row - NOT_SYNCED))
            return
        }
        val state = loadState(table.name, id)
        val values = row.filterKeys { it !in NOT_SYNCED }.mapValues { toValue(it.value) }
        val writes = FieldMerge.localWrites(state, values)
        if (writes.isEmpty()) return
        val hlc = append(collection, table.name, id, writes = writes.map(::encodeWrite))
        saveState(table.name, id, FieldMerge.receive(state, hlc, writes).state)
    }

    /** Journals every row of [collectionId] that has never been journaled (upgrades, re-keying). */
    fun recordUnjournaled(collectionId: String): Int {
        var n = 0
        for (table in SyncTables.ALL) {
            val sql = if (table.appendOnly) {
                "SELECT id FROM ${RowCodec.quote(table.name)} t WHERE collection_id = ? AND NOT EXISTS " +
                    "(SELECT 1 FROM journal j WHERE j.tbl = '${table.name}' AND j.row_id = t.id)"
            } else {
                "SELECT id FROM ${RowCodec.quote(table.name)} t WHERE collection_id = ? AND NOT EXISTS " +
                    "(SELECT 1 FROM row_clock c WHERE c.tbl = '${table.name}' AND c.row_id = t.id)"
            }
            val ids = database.driver.executeQuery(null, "$sql ORDER BY rowid", { c ->
                val out = ArrayList<String>()
                while (c.next().value) out += c.getString(0)!!
                QueryResult.Value(out)
            }, 1) { bindString(0, collectionId) }.value
            ids.forEach { record(table, it) }
            n += ids.size
        }
        return n
    }

    private fun append(collection: String, table: String, rowId: String, writes: List<JsonArray>? = null, row: JsonObject? = null): Hlc {
        val hlc = clock().tick()
        val seq = q.lastSeqFor(database.deviceId).executeAsOne() + 1
        val change = WireChange(database.deviceId, seq, hlc.encode(), collection, table, rowId, writes, row)
        q.appendChange(database.deviceId, seq, hlc.encode(), collection, table, rowId, json.encodeToString(WireChange.serializer(), change), database.now())
        return hlc
    }

    // ---------------------------------------------------------------------------------------------
    // Versions and exports

    fun vector(): Map<String, Long> = q.vector().executeAsList().associate { it.device_id to (it.seq ?: 0L) }

    /** Every change this phone has that a phone with [known] lacks, in clock order. */
    fun changesMissingFrom(known: Map<String, Long>): List<WireChange> {
        val out = ArrayList<WireChange>()
        for ((device, latest) in vector()) {
            val have = known[device] ?: 0L
            if (have >= latest) continue
            q.changesAfter(device, have).executeAsList().forEach { out += json.decodeFromString(WireChange.serializer(), it.body) }
        }
        return out.sortedWith(compareBy<WireChange>({ it.hlc }, { it.device }, { it.seq }))
    }

    fun size(): Long = q.journalCount().executeAsOne()

    // ---------------------------------------------------------------------------------------------
    // Applying other phones' changes

    /**
     * Applies [changes] for household [collectionId]. Must run inside `write(fromSync = true)` so
     * the rows it touches are re-indexed but not journaled again.
     */
    fun ingest(scope: WriteScope, collectionId: String, changes: List<WireChange>): IngestReport {
        database.exec("PRAGMA defer_foreign_keys = ON")
        var applied = 0; var duplicates = 0; var skipped = 0; var conflicts = 0
        var incomplete = false
        val nextSeq = HashMap<String, Long>()
        val ordered = changes.sortedWith(compareBy<WireChange>({ it.hlc }, { it.device }, { it.seq }))
        val blocked = HashSet<String>()
        for (c in ordered) {
            if (c.collection != collectionId) { skipped++; continue }
            val hlc = Hlc.parseOrNull(c.hlc)
            if (hlc == null || c.seq <= 0) { skipped++; continue }
            if (q.hasChange(c.device, c.seq).executeAsOne() > 0) { duplicates++; continue }
            val expected = nextSeq.getOrPut(c.device) { q.lastSeqFor(c.device).executeAsOne() + 1 }
            if (c.device in blocked || c.seq != expected) {
                blocked += c.device // a gap: hold back the rest of this phone's changes
                incomplete = true
                continue
            }
            val table = runCatching { SyncTables.byName(c.table) }.getOrNull()
            q.appendChange(c.device, c.seq, c.hlc, c.collection, c.table, c.rowId, json.encodeToString(WireChange.serializer(), c), database.now())
            nextSeq[c.device] = c.seq + 1
            clock().observe(hlc)
            if (table == null) { skipped++; continue } // a table from a newer app version
            if (table.appendOnly) {
                val row = c.row ?: run { skipped++; continue }
                RowCodec.applyRemote(database.driver, table, row)
            } else {
                val writes = c.writes?.mapNotNull(::decodeWrite) ?: run { skipped++; continue }
                conflicts += applyWrites(table, c.rowId, collectionId, hlc, writes)
            }
            scope.touched(table, c.rowId)
            applied++
        }
        val stock = recordStockConflicts(scope, collectionId)
        return IngestReport(applied, duplicates, skipped, conflicts, stock, incomplete)
    }

    private fun applyWrites(table: SyncTable, id: String, collectionId: String, hlc: Hlc, writes: List<FieldWrite>): Int {
        val state = loadState(table.name, id)
        val result = FieldMerge.receive(state, hlc, writes.filter { table.type(it.column) != null && it.column !in table.localOnlyColumns })
        saveState(table.name, id, result.state)
        if (result.changed.isNotEmpty()) writeRow(table, id, result.state, result.changed)
        if (result.fastForwarded.isNotEmpty()) {
            database.local.openConflictFor(table.name, id).executeAsOneOrNull()?.let { open ->
                val columns = runCatching { json.parseToJsonElement(open.local_payload) as JsonObject }.getOrNull()?.keys.orEmpty()
                if (columns.any { it in result.fastForwarded }) database.local.resolveConflictsFor(database.now(), table.name, id)
            }
        }
        val reportable = result.conflicts.filter { it.column !in QUIET_COLUMNS }
        if (reportable.isEmpty()) return 0
        val lost = buildJsonObject { reportable.forEach { put(it.column, fromValue(it.lost)) } }
        val kept = buildJsonObject { reportable.forEach { put(it.column, fromValue(it.kept)) } }
        val open = database.local.openConflictFor(table.name, id).executeAsOneOrNull()
        val (mergedLost, mergedKept) = if (open == null) lost to kept else {
            database.local.resolveConflict(database.now(), open.id)
            val oldLost = runCatching { json.parseToJsonElement(open.local_payload) as JsonObject }.getOrDefault(JsonObject(emptyMap()))
            val oldKept = runCatching { open.server_payload?.let { json.parseToJsonElement(it) as JsonObject } }.getOrNull() ?: JsonObject(emptyMap())
            JsonObject(oldLost + lost) to JsonObject(oldKept + kept)
        }
        database.local.insertConflict(
            id = database.newId(), entity = table.name, entity_id = id, collection_id = collectionId,
            local_payload = mergedLost.toString(), server_payload = mergedKept.toString(),
            reason = "Changed on two phones before they synced", detected_at = database.now(),
        )
        return 1
    }

    /** Updates the changed columns, or inserts the row from its merge state if it's new here. */
    private fun writeRow(table: SyncTable, id: String, state: Map<String, FieldState>, changed: Map<String, SyncValue>) {
        val exists = database.driver.executeQuery(null, "SELECT 1 FROM ${RowCodec.quote(table.name)} WHERE id = ?", { c ->
                QueryResult.Value(c.next().value)
            }, 1) { bindString(0, id) }.value
        if (exists) {
            val cols = changed.keys.filter { it != "id" && table.type(it) != null }
            if (cols.isEmpty()) return
            database.driver.execute(null,
                "UPDATE ${RowCodec.quote(table.name)} SET ${cols.joinToString { "${RowCodec.quote(it)} = ?" }} WHERE id = ?", cols.size + 1) {
                cols.forEachIndexed { i, col -> bind(this, i, table.type(col)!!, changed.getValue(col)) }
                bindString(cols.size, id)
            }
        } else {
            val cols = table.syncedColumns.map { it.first }.filter { it != "rev" && (it in state || it == "id") }
            database.driver.execute(null,
                "INSERT INTO ${RowCodec.quote(table.name)}(${cols.joinToString { RowCodec.quote(it) }}) VALUES (${cols.joinToString { "?" }})", cols.size) {
                cols.forEachIndexed { i, col ->
                    val v = if (col == "id") SyncValue.Text(id) else state.getValue(col).value
                    bind(this, i, table.type(col)!!, v)
                }
            }
        }
    }

    private fun bind(stmt: app.cash.sqldelight.db.SqlPreparedStatement, index: Int, type: ColType, value: SyncValue) {
        when (value) {
            SyncValue.Null -> if (type == ColType.TEXT) stmt.bindString(index, null) else stmt.bindLong(index, null)
            is SyncValue.Text -> if (type == ColType.TEXT) stmt.bindString(index, value.value) else stmt.bindLong(index, value.value.toLongOrNull())
            is SyncValue.Num -> if (type == ColType.INTEGER) stmt.bindLong(index, value.value) else stmt.bindString(index, value.value.toString())
        }
    }

    /** Negative stock after a merge (both phones took the last bottle) becomes a stock conflict. */
    fun recordStockConflicts(scope: WriteScope, collectionId: String): Int {
        val negatives = database.inventory.negativeStock(collectionId).executeAsList()
        var n = 0
        for (s in negatives) {
            val key = "${s.lot_id}|${s.location_key}"
            if (database.local.openConflictFor("stock", key).executeAsOneOrNull() != null) continue
            database.local.insertConflict(
                id = database.newId(), entity = "stock", entity_id = key, collection_id = collectionId,
                local_payload = """{"lot_id":"${s.lot_id}","location_id":"${s.location_key}","quantity":${s.quantity},"vintage_id":"${s.vintage_id}"}""",
                server_payload = null,
                reason = "More bottles were taken out than were in stock (changes from two phones). Count the bottles and adjust.",
                detected_at = database.now(),
            )
            n++
        }
        if (n > 0) scope.touchedLocal("sync_conflict")
        return n
    }

    // ---------------------------------------------------------------------------------------------
    // State encoding

    private fun loadState(table: String, id: String): Map<String, FieldState> {
        val text = q.rowClock(table, id).executeAsOneOrNull() ?: return emptyMap()
        val obj = json.parseToJsonElement(text) as JsonObject
        return obj.mapNotNull { (col, v) ->
            val arr = v as? JsonArray ?: return@mapNotNull null
            val hlc = Hlc.parseOrNull(arr.getOrNull(1)?.jsonPrimitive?.contentOrNull) ?: return@mapNotNull null
            col to FieldState(toValue(arr[0]), hlc)
        }.toMap()
    }

    private fun saveState(table: String, id: String, state: Map<String, FieldState>) {
        val obj = JsonObject(state.mapValues { (_, s) -> JsonArray(listOf(fromValue(s.value), JsonPrimitive(s.clock.encode()))) })
        q.putRowClock(table, id, obj.toString())
    }

    private fun encodeWrite(w: FieldWrite): JsonArray =
        JsonArray(listOf(JsonPrimitive(w.column), fromValue(w.value), w.base?.let { JsonPrimitive(it.encode()) } ?: JsonNull))

    private fun decodeWrite(arr: JsonArray): FieldWrite? {
        if (arr.size < 2) return null
        val col = (arr[0] as? JsonPrimitive)?.contentOrNull ?: return null
        if (col in NOT_SYNCED) return null
        val base = arr.getOrNull(2)?.let { if (it is JsonNull) null else Hlc.parseOrNull((it as? JsonPrimitive)?.contentOrNull) }
        return FieldWrite(col, toValue(arr[1]), base)
    }

    companion object {
        /** Never synced: the old server revision. (Device-local columns are excluded by SyncTable.) */
        val NOT_SYNCED = setOf("rev")

        /** Bookkeeping fields that change with every edit; independent writes to them aren't worth reporting. */
        val QUIET_COLUMNS = setOf("updated_at", "created_at", "producer_sort")

        fun toValue(e: JsonElement?): SyncValue = when {
            e == null || e is JsonNull -> SyncValue.Null
            e is JsonPrimitive && e.isString -> SyncValue.Text(e.content)
            e is JsonPrimitive -> e.longOrNull?.let { SyncValue.Num(it) } ?: SyncValue.Text(e.content)
            else -> SyncValue.Text(e.toString())
        }

        fun fromValue(v: SyncValue): JsonElement = when (v) {
            SyncValue.Null -> JsonNull
            is SyncValue.Text -> JsonPrimitive(v.value)
            is SyncValue.Num -> JsonPrimitive(v.value)
        }
    }
}
