package com.houseofvino.data

import com.houseofvino.data.sync.PushOp
import com.houseofvino.data.sync.PushResult
import com.houseofvino.data.sync.SyncRemote
import com.houseofvino.data.sync.SyncTables
import com.houseofvino.data.sync.TransientSyncException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * In-memory model of the server contract implemented by `supabase/migrations` (sync_push and
 * RLS-scoped pulls): monotonically increasing revs, idempotent op ids, optimistic concurrency
 * on mutable rows, append-only movements and role-based write permission.
 */
class FakeSyncServer {
    private var revCounter = 0L
    private val tables = HashMap<String, LinkedHashMap<String, JsonObject>>()
    private val applied = HashMap<String, PushResult>()
    /** userId → (collectionId → role) */
    val roles = HashMap<String, HashMap<String, String>>()

    fun rowCount(table: String) = tables[table]?.size ?: 0
    fun row(table: String, id: String) = tables[table]?.get(id)

    fun push(userId: String, collectionId: String, ops: List<PushOp>): List<PushResult> = ops.map { op ->
        applied[op.opId]?.let { return@map it }
        val role = roles[userId]?.get(collectionId)
        val result: PushResult = when {
            role == null -> PushResult.Rejected(op.opId, "42501", "Not a member of this collection", permanent = true)
            role == "viewer" -> PushResult.Rejected(op.opId, "42501", "Viewers can't make changes", permanent = true)
            (op.row["collection_id"] as? JsonPrimitive)?.content != collectionId ->
                PushResult.Rejected(op.opId, "22023", "Row belongs to another collection", permanent = true)
            else -> apply(op)
        }
        // Like sync_push: only outcomes that must not be re-evaluated are remembered.
        if (result is PushResult.Applied || result is PushResult.Conflict) applied[op.opId] = result
        result
    }

    private fun apply(op: PushOp): PushResult {
        val table = SyncTables.byName(op.entity)
        val rows = tables.getOrPut(op.entity) { LinkedHashMap() }
        val current = rows[op.id]
        if (table.appendOnly) {
            if (current != null) return PushResult.Applied(op.opId, rev(current))
            val rev = ++revCounter
            rows[op.id] = JsonObject(op.row + ("rev" to JsonPrimitive(rev)))
            return PushResult.Applied(op.opId, rev)
        }
        if (current != null && rev(current) != op.baseRev) return PushResult.Conflict(op.opId, current)
        val rev = ++revCounter
        rows.remove(op.id) // re-insert so iteration order follows rev
        rows[op.id] = JsonObject(op.row + ("rev" to JsonPrimitive(rev)))
        return PushResult.Applied(op.opId, rev)
    }

    fun pull(userId: String, collectionId: String, entity: String, afterRev: Long, limit: Int): List<JsonObject> {
        if (roles[userId]?.get(collectionId) == null) return emptyList() // RLS: nothing visible
        return tables[entity].orEmpty().values
            .filter { (it["collection_id"] as? JsonPrimitive)?.content == collectionId && rev(it) > afterRev }
            .sortedBy { rev(it) }
            .take(limit)
    }

    private fun rev(row: JsonObject) = (row["rev"] as? JsonPrimitive)?.longOrNull ?: 0L

    /** A device's view of the server, with switches to simulate failures. */
    inner class Client(val userId: String) : SyncRemote {
        var offline = false
        /** Apply the push on the server but lose the response (the classic duplicate-send case). */
        var dropNextResponse = false
        var pushCalls = 0

        override suspend fun push(collectionId: String, ops: List<PushOp>): List<PushResult> {
            pushCalls++
            if (offline) throw TransientSyncException("offline")
            val results = this@FakeSyncServer.push(userId, collectionId, ops)
            if (dropNextResponse) {
                dropNextResponse = false
                throw TransientSyncException("connection reset")
            }
            return results
        }

        override suspend fun pull(collectionId: String, entity: String, afterRev: Long, limit: Int): List<JsonObject> {
            if (offline) throw TransientSyncException("offline")
            return this@FakeSyncServer.pull(userId, collectionId, entity, afterRev, limit)
        }
    }
}
