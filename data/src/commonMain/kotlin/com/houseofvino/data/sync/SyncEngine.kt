package com.houseofvino.data.sync

import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.db.Sync_outbox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.random.Random

/** One queued change as sent to the server. */
data class PushOp(
    val opId: String,
    val entity: String,
    val id: String,
    val baseRev: Long?,
    val row: JsonObject,
)

sealed interface PushResult {
    val opId: String
    /** Applied now, or previously (idempotent replay). [rev] is the server revision of the row. */
    data class Applied(override val opId: String, val rev: Long) : PushResult
    /** The server row changed since [PushOp.baseRev]; [serverRow] is the current server state. */
    data class Conflict(override val opId: String, val serverRow: JsonObject?) : PushResult
    /** Refused (permissions, validation). Permanent rejections are not retried automatically. */
    data class Rejected(override val opId: String, val code: String, val message: String, val permanent: Boolean) : PushResult
}

/** The server side of sync. Implemented by Supabase in production and by an in-memory fake in tests. */
interface SyncRemote {
    /** Applies [ops] in order. Every op gets exactly one result. */
    suspend fun push(collectionId: String, ops: List<PushOp>): List<PushResult>

    /** Rows of [entity] with rev > [afterRev], ordered by rev ascending, at most [limit]. */
    suspend fun pull(collectionId: String, entity: String, afterRev: Long, limit: Int): List<JsonObject>
}

/** Thrown by [SyncRemote] implementations for transport failures (offline, timeouts, 5xx). */
class TransientSyncException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Thrown when the session is missing or expired; sync pauses until the user signs in again. */
class AuthRequiredException(message: String = "Sign in to sync") : Exception(message)

data class SyncStatus(
    val running: Boolean = false,
    val lastSuccessAt: Long? = null,
    val lastError: String? = null,
    val pendingOps: Int = 0,
    val failedOps: Int = 0,
    val openConflicts: Int = 0,
    val needsSignIn: Boolean = false,
)

data class SyncReport(val pushed: Int, val pulled: Int, val conflicts: Int, val rejected: Int, val stockConflicts: Int)

/**
 * Two-phase sync: push the outbox, then pull every table since its cursor.
 *
 * Guarantees:
 *  - At-least-once delivery with idempotent op ids: retrying after a lost response is safe.
 *  - One op per entity per batch, in queue order; later ops are rebased onto the new server rev.
 *  - A pulled row never overwrites a local row with unsent changes.
 *  - Stale writes are detected server-side (base rev) and surfaced as conflicts, server wins
 *    locally until the user chooses.
 *  - Movements are append-only, so concurrent stock changes merge without conflicts; negative
 *    stock after a merge is reported as a stock conflict.
 */
class SyncEngine(
    private val database: LocalDatabase,
    private val remote: SyncRemote,
    private val batchSize: Int = 100,
    private val pullPageSize: Int = 500,
    private val random: Random = Random.Default,
) {
    private val mutex = Mutex()
    private val json = Json { encodeDefaults = true }
    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    suspend fun sync(collectionId: String): SyncReport = mutex.withLock {
        _status.value = _status.value.copy(running = true, lastError = null)
        try {
            database.local.resetInFlight() // recover ops left in flight by a crash
            val pushed = pushAll(collectionId)
            val pulled = pullAll(collectionId)
            val stockConflicts = recordStockConflicts(collectionId)
            val report = SyncReport(pushed.applied, pulled, pushed.conflicts, pushed.rejected, stockConflicts)
            _status.value = currentStatus().copy(lastSuccessAt = database.now(), needsSignIn = false)
            report
        } catch (e: AuthRequiredException) {
            _status.value = currentStatus().copy(lastError = e.message, needsSignIn = true)
            throw e
        } catch (e: Exception) {
            _status.value = currentStatus().copy(lastError = e.message ?: "Sync failed")
            throw e
        }
    }

    fun refreshStatus() {
        _status.value = currentStatus().copy(lastSuccessAt = _status.value.lastSuccessAt, lastError = _status.value.lastError)
    }

    private fun currentStatus(): SyncStatus {
        val summary = database.local.outboxSummary().executeAsList()
        return _status.value.copy(
            running = false,
            pendingOps = summary.filter { it.state != "FAILED_PERMANENT" }.sumOf { it.ops.toInt() },
            failedOps = summary.filter { it.state == "FAILED_PERMANENT" }.sumOf { it.ops.toInt() },
            openConflicts = database.local.openConflicts().executeAsList().size,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Push

    private data class PushTally(var applied: Int = 0, var conflicts: Int = 0, var rejected: Int = 0)

    private suspend fun pushAll(collectionId: String): PushTally {
        val tally = PushTally()
        var rounds = 0
        while (rounds++ < 1000) {
            val due = database.local.dueOps(collectionId, database.now(), (batchSize * 4).toLong()).executeAsList()
            if (due.isEmpty()) break
            // One op per entity per batch, preserving queue order.
            val seen = HashSet<Pair<String, String>>()
            val batch = due.filter { seen.add(it.entity to it.entity_id) }.take(batchSize)
            database.local.markInFlight(batch.map { it.seq })
            val ops = batch.map { PushOp(it.op_id, it.entity, it.entity_id, it.base_rev, database.outbox.decode(it)) }
            val results = try {
                remote.push(collectionId, ops)
            } catch (e: TransientSyncException) {
                batch.forEach { scheduleRetry(it, e.message ?: "Network error") }
                throw e
            } catch (e: AuthRequiredException) {
                database.local.resetInFlight()
                throw e
            }
            val byOp = results.associateBy { it.opId }
            database.write(fromSync = true) {
                for (op in batch) {
                    when (val r = byOp[op.op_id]) {
                        is PushResult.Applied -> { applyAck(op, r.rev); tally.applied++ }
                        is PushResult.Conflict -> { recordConflict(op, r.serverRow); tally.conflicts++ }
                        is PushResult.Rejected -> {
                            if (r.permanent) database.local.failPermanently("${r.code}: ${r.message}", op.op_id)
                            else scheduleRetry(op, r.message)
                            tally.rejected++
                        }
                        null -> scheduleRetry(op, "No result from server")
                    }
                }
                touchedLocal("sync_outbox")
            }
            if (results.all { it is PushResult.Rejected && !it.permanent }) break
        }
        return tally
    }

    private fun applyAck(op: Sync_outbox, rev: Long) {
        val table = SyncTables.byName(op.entity)
        database.local.deleteOp(op.op_id)
        RowCodec.setRev(database.driver, table, op.entity_id, rev)
        if (!table.appendOnly) database.local.rebaseOps(newRev = rev, entity = op.entity, entityId = op.entity_id, oldRev = op.base_rev)
    }

    /**
     * The server has a newer version. Server wins locally (so this device converges), the local
     * version is preserved in `sync_conflict` for the user, and queued edits for the row are
     * folded into that record.
     */
    private fun com.houseofvino.data.db.WriteScope.recordConflict(op: Sync_outbox, serverRow: JsonObject?) {
        val table = SyncTables.byName(op.entity)
        val latestLocal = database.local.pendingOpsFor(op.entity, op.entity_id).executeAsList().lastOrNull()?.payload ?: op.payload
        database.local.deleteOpsForEntity(op.entity, op.entity_id)
        database.local.deleteOp(op.op_id)
        if (database.local.openConflictFor(op.entity, op.entity_id).executeAsOneOrNull() == null) {
            database.local.insertConflict(
                id = database.newId(), entity = op.entity, entity_id = op.entity_id, collection_id = op.collection_id,
                local_payload = latestLocal, server_payload = serverRow?.let { json.encodeToString(JsonObject.serializer(), it) },
                reason = "Changed on another device", detected_at = now,
            )
        }
        if (serverRow != null) {
            RowCodec.applyRemote(database.driver, table, serverRow)
            touched(table, op.entity_id) // re-index; fromSync = true so nothing is enqueued
        }
    }

    private fun scheduleRetry(op: Sync_outbox, error: String) {
        val attempt = op.attempts + 1
        val base = 2_000L shl attempt.coerceAtMost(9).toInt() // 4 s … ~17 min
        val delay = base.coerceAtMost(MAX_BACKOFF_MS) / 2 + random.nextLong(base.coerceAtMost(MAX_BACKOFF_MS) / 2 + 1)
        database.local.scheduleRetry(nextAt = database.now() + delay, error = error, opId = op.op_id)
    }

    // ---------------------------------------------------------------------------------------------
    // Pull

    private suspend fun pullAll(collectionId: String): Int {
        var total = 0
        for (table in SyncTables.ALL) {
            var cursor = database.local.cursorFor(collectionId, table.name).executeAsOneOrNull() ?: 0L
            while (true) {
                val rows = remote.pull(collectionId, table.name, cursor, pullPageSize)
                if (rows.isEmpty()) break
                val maxRev = rows.maxOf { (it["rev"] as? JsonPrimitive)?.longOrNull ?: cursor }
                database.write(fromSync = true) {
                    database.exec("PRAGMA defer_foreign_keys = ON")
                    for (row in rows) {
                        val id = (row["id"] as? JsonPrimitive)?.content ?: continue
                        val rowCollection = (row["collection_id"] as? JsonPrimitive)?.content
                        if (rowCollection != collectionId) continue // defence in depth; RLS already scopes rows
                        val hasLocalChanges = database.local.hasPendingFor(table.name, id).executeAsOne() > 0
                        if (hasLocalChanges && !table.appendOnly) continue // our push will detect the conflict
                        if (RowCodec.applyRemote(database.driver, table, row)) touched(table, id)
                    }
                    database.local.putCursor(collectionId, table.name, maxRev)
                }
                total += rows.size
                cursor = maxRev
                if (rows.size < pullPageSize) break
            }
        }
        return total
    }

    private fun recordStockConflicts(collectionId: String): Int {
        val negatives = database.inventory.negativeStock(collectionId).executeAsList()
        if (negatives.isEmpty()) return 0
        database.write(fromSync = true) {
            for (n in negatives) {
                val key = "${n.lot_id}|${n.location_key}"
                if (database.local.openConflictFor("stock", key).executeAsOneOrNull() != null) continue
                database.local.insertConflict(
                    id = database.newId(), entity = "stock", entity_id = key, collection_id = collectionId,
                    local_payload = """{"lot_id":"${n.lot_id}","location_id":"${n.location_key}","quantity":${n.quantity},"vintage_id":"${n.vintage_id}"}""",
                    server_payload = null,
                    reason = "More bottles were removed than were in stock (changes from two devices). Count the bottles and adjust.",
                    detected_at = now,
                )
            }
            touchedLocal("sync_conflict")
        }
        return negatives.size
    }

    // ---------------------------------------------------------------------------------------------
    // Conflict resolution

    /** Keep the server version: mark resolved, nothing else to do (server state is already local). */
    fun resolveKeepServer(conflictId: String) = database.write {
        database.local.resolveConflict(now, conflictId)
        touchedLocal("sync_conflict")
    }

    /** Keep this device's version: re-apply it locally and queue it against the current server rev. */
    fun resolveKeepLocal(conflictId: String) = database.write {
        val c = requireNotNull(database.local.conflictById(conflictId).executeAsOneOrNull()) { "Conflict not found" }
        require(c.entity != "stock") { "Stock conflicts are fixed by counting the bottles and adjusting stock" }
        val table = SyncTables.byName(c.entity)
        val local = json.decodeFromString(JsonObject.serializer(), c.local_payload)
        val serverRev = RowCodec.rev(database.driver, table, c.entity_id)
        val withRev = JsonObject(local + ("rev" to (serverRev?.let(::JsonPrimitive) ?: kotlinx.serialization.json.JsonNull)))
        RowCodec.applyRemote(database.driver, table, withRev)
        touched(table, c.entity_id) // enqueues a fresh op with base_rev = server rev
        database.local.resolveConflict(now, conflictId)
        touchedLocal("sync_conflict")
    }

    fun retryFailed() = database.write {
        database.local.retryFailed()
        touchedLocal("sync_outbox")
    }

    companion object {
        const val MAX_BACKOFF_MS = 15 * 60_000L
    }
}
