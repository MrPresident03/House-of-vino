package com.houseofvino.data.sync

import com.houseofvino.core.encoding.Base64
import com.houseofvino.data.db.LocalDatabase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

data class ExportedSyncFile(val fileName: String, val text: String, val changes: Int, val photos: Int, val sizeBytes: Int)

data class ImportedSyncFile(
    val fromName: String,
    val applied: Int,
    val duplicates: Int,
    val conflicts: Int,
    val stockConflicts: Int,
    val photos: Int,
    val incomplete: Boolean,
) {
    val nothingNew get() = applied == 0 && photos == 0
}

/**
 * The file transport (ADR-13 transport 1): an encrypted bundle of everything the household's
 * other phones haven't seen yet, as a text file that can go through any messaging app.
 */
class SyncFileService(
    private val database: LocalDatabase,
    private val household: HouseholdRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Builds the file for every active partner: changes beyond the least any of them is known
     * to have (everything, if a partner has never sent a file back).
     */
    fun export(collectionId: String, includePhotos: Boolean, readPhoto: (String) -> ByteArray?): ExportedSyncFile {
        val key = household.key(collectionId) ?: throw SyncFileException("Share this cellar first.")
        val peers = database.sync.peers(collectionId).executeAsList().filter { it.removed_at == null }
        val known = if (peers.isEmpty()) emptyMap() else peers.map { decodeVector(it.known_vector) }.reduce { acc, v ->
            (acc.keys intersect v.keys).associateWith { minOf(acc.getValue(it), v.getValue(it)) }
        }
        val changes = database.changeLog.changesMissingFrom(known)
        val photos = if (!includePhotos) emptyList() else changes.filter { it.table == "photo" }.map { it.rowId }.distinct().mapNotNull { id ->
            val path = database.driver.executeQuery(null, "SELECT local_path FROM photo WHERE id = ? AND deleted_at IS NULL", { c ->
                app.cash.sqldelight.db.QueryResult.Value(if (c.next().value) c.getString(0) else null)
            }, 1) { bindString(0, id) }.value ?: return@mapNotNull null
            readPhoto(path)?.let { BundlePhoto(id, Base64.encode(it)) }
        }
        val payload = BundlePayload(
            household = collectionId, from = database.deviceId, fromName = household.deviceName(), createdAt = database.now(),
            vector = database.changeLog.vector(), changes = changes, photos = photos,
        )
        val text = BundleCodec.armor(BundleCodec.seal(payload, key))
        database.write { database.sync.sentToPeers(now, collectionId); touchedLocal("peer") }
        val stamp = database.now().toString(36)
        return ExportedSyncFile("house-of-vino-$stamp.${BundleCodec.FILE_EXTENSION}", text, changes.size, photos.size, text.length)
    }

    /** Opens, verifies and applies a sync file. Nothing is changed if any check fails. */
    fun import(collectionId: String, text: String, savePhoto: (id: String, bytes: ByteArray) -> String): ImportedSyncFile {
        val key = household.key(collectionId) ?: throw SyncFileException("Join the household before receiving its changes.")
        val envelope = BundleCodec.dearmor(text)
        val payload = BundleCodec.open(envelope, key)
        if (payload.household != collectionId) throw SyncFileException("This file is from a different household.")
        if (payload.from == database.deviceId) throw SyncFileException("This file was made on this phone. Send it to your partner instead.")
        database.sync.peerById(payload.from).executeAsOneOrNull()?.let {
            if (it.removed_at != null) throw SyncFileException("This file is from a phone that was removed from the household.")
        }
        // Photos are stored before the transaction (file writes can't be rolled back anyway).
        val photoPaths = payload.photos.mapNotNull { p ->
            if (localPhotoPath(p.id) != null) return@mapNotNull null
            Base64.decode(p.data)?.let { p.id to savePhoto(p.id, it) }
        }
        val report = database.write(fromSync = true) {
            val r = database.changeLog.ingest(this, collectionId, payload.changes)
            for ((id, path) in photoPaths) {
                database.driver.execute(null, "UPDATE photo SET local_path = ? WHERE id = ?", 2) { bindString(0, path); bindString(1, id) }
            }
            database.sync.upsertPeer(payload.from, collectionId, payload.fromName.take(40), now)
            database.sync.heardFromPeer(encodeVector(payload.vector), now, payload.fromName.take(40), payload.from)
            touchedLocal("peer")
            r
        }
        return ImportedSyncFile(payload.fromName, report.applied, report.duplicates, report.conflicts, report.stockConflicts, photoPaths.size, report.incomplete)
    }

    private fun localPhotoPath(id: String): String? =
        database.driver.executeQuery(null, "SELECT local_path FROM photo WHERE id = ?", { c ->
            app.cash.sqldelight.db.QueryResult.Value(if (c.next().value) c.getString(0) else null)
        }, 1) { bindString(0, id) }.value

    private fun decodeVector(text: String): Map<String, Long> =
        runCatching { json.parseToJsonElement(text) as JsonObject }.getOrNull()?.mapValues { (_, v) ->
            (v as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull() ?: 0L
        }.orEmpty()

    private fun encodeVector(v: Map<String, Long>): String =
        JsonObject(v.mapValues { kotlinx.serialization.json.JsonPrimitive(it.value) }).toString()
}
