package com.houseofvino.data.sync

import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.db.Sync_conflict
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** One field that two phones changed independently. */
data class ConflictField(val column: String, val current: String?, val other: String?)

/**
 * Review of sync conflicts. A field conflict keeps the newer edit everywhere and remembers the
 * other one here; the user can keep the current value or switch to the other. A stock conflict
 * is fixed by counting the bottles (an adjustment), then dismissed.
 */
class ConflictService(private val database: LocalDatabase) {
    private val json = Json { ignoreUnknownKeys = true }

    fun open(): List<Sync_conflict> = database.local.openConflicts().executeAsList()

    fun fields(c: Sync_conflict): List<ConflictField> {
        if (c.entity == "stock") return emptyList()
        val other = parse(c.local_payload)
        val current = c.server_payload?.let(::parse) ?: JsonObject(emptyMap())
        return other.keys.map { col -> ConflictField(col, text(current[col]), text(other[col])) }
    }

    /** Keeps what every phone shows now. */
    fun keepCurrent(conflictId: String) = database.write {
        database.local.resolveConflict(now, conflictId)
        touchedLocal("sync_conflict")
    }

    /**
     * Switches to the other phone's values. This is a normal edit, so it reaches the partner,
     * and the partner's copy of this conflict closes when it arrives.
     */
    fun useOther(conflictId: String) = database.write {
        val c = requireNotNull(database.local.conflictById(conflictId).executeAsOneOrNull()) { "Conflict not found" }
        require(c.entity != "stock") { "Stock conflicts are fixed by counting the bottles" }
        val table = SyncTables.byName(c.entity)
        val other = parse(c.local_payload).filterKeys { table.type(it) != null && it !in table.localOnlyColumns && it != "id" }
        if (other.isNotEmpty()) {
            val cols = other.keys.toList() + (if ("updated_at" in other || table.type("updated_at") == null) emptyList() else listOf("updated_at"))
            database.driver.execute(null,
                "UPDATE ${RowCodec.quote(table.name)} SET ${cols.joinToString { "${RowCodec.quote(it)} = ?" }} WHERE id = ?", cols.size + 1) {
                cols.forEachIndexed { i, col ->
                    val v = if (col == "updated_at" && col !in other) JsonPrimitive(now) else other.getValue(col)
                    when (table.type(col)) {
                        ColType.TEXT -> bindString(i, (v as? JsonPrimitive)?.takeUnless { v is JsonNull }?.contentOrNull)
                        else -> bindLong(i, (v as? JsonPrimitive)?.takeUnless { v is JsonNull }?.content?.toLongOrNull())
                    }
                }
                bindString(cols.size, c.entity_id)
            }
            touched(table, c.entity_id)
        }
        database.local.resolveConflict(now, conflictId)
        touchedLocal("sync_conflict")
    }

    private fun parse(text: String): JsonObject = runCatching { json.parseToJsonElement(text) as JsonObject }.getOrDefault(JsonObject(emptyMap()))

    private fun text(e: kotlinx.serialization.json.JsonElement?): String? = (e as? JsonPrimitive)?.takeUnless { e is JsonNull }?.contentOrNull
}
