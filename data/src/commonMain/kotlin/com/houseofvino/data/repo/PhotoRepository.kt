package com.houseofvino.data.repo

import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.db.Photo
import com.houseofvino.data.remote.PhotoStorage
import com.houseofvino.data.sync.SyncTables

enum class PhotoOwner { VINTAGE, TASTING, LOT }

/**
 * Photo metadata. Image bytes live in app-private files (platform) and, once signed in, in the
 * private storage bucket. Uploads and downloads are retried by [syncFiles]; a failed upload
 * never blocks local use.
 */
class PhotoRepository(private val database: LocalDatabase) {
    private val q get() = database.journal

    fun forOwner(owner: PhotoOwner, ownerId: String): List<Photo> = q.photosFor(owner.name, ownerId).executeAsList()

    fun byId(id: String): Photo? = q.photoById(id).executeAsOneOrNull()

    fun add(collectionId: String, owner: PhotoOwner, ownerId: String, localPath: String, width: Int, height: Int, bytes: Long): String =
        database.write {
            val id = database.newId()
            q.insertPhoto(id, collectionId, owner.name, ownerId, localPath, null, "image/jpeg", width.toLong(), height.toLong(), bytes, now, now)
            touched(SyncTables.PHOTO, id)
            id
        }

    /** Reserves an id before the file is written, so the file can be named after it. */
    fun newPhotoId(): String = database.newId()

    fun addWithId(id: String, collectionId: String, owner: PhotoOwner, ownerId: String, localPath: String, width: Int, height: Int, bytes: Long) =
        database.write {
            q.insertPhoto(id, collectionId, owner.name, ownerId, localPath, null, "image/jpeg", width.toLong(), height.toLong(), bytes, now, now)
            touched(SyncTables.PHOTO, id)
        }

    fun delete(id: String) = database.write {
        q.softDeletePhoto(now, id)
        touched(SyncTables.PHOTO, id)
    }

    data class FileSyncResult(val uploaded: Int, val downloaded: Int, val failed: Int)

    /**
     * Uploads local photos that aren't in storage yet and downloads remote photos missing locally.
     * [readLocal]/[writeLocal] are the platform file functions.
     */
    suspend fun syncFiles(
        storage: PhotoStorage,
        readLocal: (String) -> ByteArray?,
        writeLocal: (id: String, bytes: ByteArray) -> String,
        limit: Long = 20,
    ): FileSyncResult {
        var up = 0; var down = 0; var failed = 0
        for (p in q.photosAwaitingUpload(limit).executeAsList()) {
            val bytes = p.local_path?.let(readLocal) ?: continue
            try {
                val path = storage.upload(p.collection_id, p.id, bytes)
                database.write { q.setPhotoRemotePath(path, now, p.id); touched(SyncTables.PHOTO, p.id) }
                up++
            } catch (e: Exception) { failed++ }
        }
        for (p in q.photosAwaitingDownload(limit).executeAsList()) {
            try {
                val bytes = storage.download(p.remote_path!!)
                val local = writeLocal(p.id, bytes)
                database.write { q.setPhotoLocalPath(local, p.id); touchedLocal("photo") }
                down++
            } catch (e: Exception) { failed++ }
        }
        return FileSyncResult(up, down, failed)
    }
}
