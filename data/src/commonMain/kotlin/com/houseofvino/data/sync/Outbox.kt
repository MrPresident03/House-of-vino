package com.houseofvino.data.sync

import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.db.Sync_outbox
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * The durable sync queue. Operations are rows in `sync_outbox`, written in the same transaction
 * as the change they describe, so a crash can never lose a change or send one that didn't commit.
 */
class Outbox(private val database: LocalDatabase) {

    private val json = Json { encodeDefaults = true }

    /**
     * Enqueues the current state of row [id]. An operation that has never been attempted is
     * updated in place (it can't have reached the server); otherwise a new operation is added so
     * every attempted op_id keeps the payload it was first sent with (idempotent retries).
     */
    fun enqueue(table: SyncTable, id: String) {
        val row = RowCodec.read(database.driver, table, id) ?: return
        val collectionId = (row["collection_id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return
        val payload = json.encodeToString(JsonObject.serializer(), row)
        val q = database.local
        if (!table.appendOnly) {
            val unsent = q.unsentOpFor(table.name, id).executeAsOneOrNull()
            if (unsent != null) {
                q.replacePayload(payload, unsent.seq)
                return
            }
        }
        val baseRev = if (table.appendOnly) null else {
            // The base is the newest server revision this device knows for the row: either the
            // row's own rev or, if an earlier op is still in flight, that op's base.
            RowCodec.rev(database.driver, table, id)
        }
        q.enqueueOp(
            op_id = database.newId(),
            entity = table.name,
            entity_id = id,
            collection_id = collectionId,
            base_rev = baseRev,
            payload = payload,
            created_at = database.now(),
        )
    }

    fun decode(op: Sync_outbox): JsonObject = json.decodeFromString(JsonObject.serializer(), op.payload)
}
