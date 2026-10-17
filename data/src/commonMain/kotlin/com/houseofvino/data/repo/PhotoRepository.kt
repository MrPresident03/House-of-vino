package com.houseofvino.data.repo

import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.db.Photo
import com.houseofvino.data.sync.SyncTables

enum class PhotoOwner { VINTAGE, TASTING, LOT }

/**
 * Photo metadata. Image bytes live in app-private files (platform); partners receive them
 * inside sync bundles (docs/SYNC.md).
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
}
