package com.houseofvino.data.sync

import com.houseofvino.core.encoding.Base64
import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.repo.SettingsRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What a QR code / invite code carries (docs/SYNC.md, "Invites"). */
@Serializable
data class Invite(
    val v: Int = 1,
    val c: String,
    val n: String,
    val k: String,
    val d: String,
    val dn: String,
) {
    val collectionId get() = c
    val householdName get() = n
    val inviterDevice get() = d
    val inviterName get() = dn
    val key: HouseholdKey get() = HouseholdKey(Base64.decode(k) ?: throw SyncFileException("This invite code is damaged."))

    fun encode(): String = PREFIX + Base64.encode(json.encodeToString(serializer(), this).encodeToByteArray(), urlSafe = true)

    companion object {
        const val PREFIX = "HOV1-"
        private val json = Json { ignoreUnknownKeys = true }

        fun decode(text: String): Invite {
            val trimmed = text.trim().filterNot { it.isWhitespace() }
            if (!trimmed.startsWith(PREFIX)) throw SyncFileException("That isn't a House of Vino invite.")
            val bytes = Base64.decode(trimmed.removePrefix(PREFIX)) ?: throw SyncFileException("This invite code is damaged.")
            val invite = try {
                json.decodeFromString(serializer(), bytes.decodeToString())
            } catch (e: Exception) {
                throw SyncFileException("This invite code is damaged.")
            }
            if (invite.v > 1) throw SyncFileException("This invite was made by a newer version of House of Vino. Update the app.")
            invite.key // validates the key
            return invite
        }
    }
}

data class PeerInfo(val deviceId: String, val name: String, val lastHeardAt: Long?, val lastSentAt: Long?, val removed: Boolean)

data class HouseholdInfo(
    val collectionId: String,
    val name: String,
    val keyId: String,
    val safetyCode: String,
    val deviceId: String,
    val deviceName: String,
    val peers: List<PeerInfo>,
) {
    val activePeers get() = peers.filterNot { it.removed }
}

/**
 * Creating, joining and leaving a household. A household is this phone's collection plus a
 * shared key; the partners are recorded as peers when they first send changes.
 */
class HouseholdRepository(private val database: LocalDatabase, private val defaultDeviceName: String = "This phone") {
    private val q get() = database.sync

    fun deviceName(): String = database.local.getSetting(SettingsRepository.KEY_DEVICE_NAME).executeAsOneOrNull() ?: defaultDeviceName

    fun setDeviceName(name: String) = database.write {
        val clean = name.trim().take(40)
        require(clean.isNotEmpty()) { "Give this phone a name" }
        database.local.putSetting(SettingsRepository.KEY_DEVICE_NAME, clean)
        touchedLocal("setting")
    }

    fun key(collectionId: String): HouseholdKey? =
        q.currentKey(collectionId).executeAsOneOrNull()?.let { HouseholdKey(Base64.decode(it.secret)!!) }

    fun info(collectionId: String): HouseholdInfo? {
        val key = key(collectionId) ?: return null
        val name = database.catalog.collectionById(collectionId).executeAsOneOrNull()?.name ?: "Our cellar"
        val peers = q.peers(collectionId).executeAsList().map { PeerInfo(it.device_id, it.name, it.last_heard_at, it.last_sent_at, it.removed_at != null) }
        return HouseholdInfo(collectionId, name, key.id, key.safetyCode, database.deviceId, deviceName(), peers)
    }

    /** Starts sharing this phone's cellar. Returns the invite to show the partner. */
    fun create(collectionId: String, name: String): Invite {
        database.write {
            require(q.currentKey(collectionId).executeAsOneOrNull() == null) { "This cellar is already shared" }
            val key = HouseholdKey.generate()
            q.insertKey(key.id, collectionId, Base64.encode(key.secret), now)
            database.catalog.renameCollection(name.trim().ifEmpty { "Our cellar" }.take(60), now, collectionId)
            touchedLocal("household_key")
            touchedLocal("collection")
        }
        return invite(collectionId)
    }

    fun invite(collectionId: String): Invite {
        val info = info(collectionId) ?: throw IllegalStateException("This cellar isn't shared yet")
        return Invite(c = collectionId, n = info.name, k = Base64.encode(key(collectionId)!!.secret, urlSafe = true), d = database.deviceId, dn = deviceName())
    }

    /**
     * Joins the household in [invite]. This phone's own cellar (in [currentCollectionId]) is
     * moved into the household and will be shared with the partner. Returns the household's
     * collection id, which becomes the active collection.
     */
    fun join(invite: Invite, currentCollectionId: String): String {
        val key = invite.key
        if (invite.inviterDevice == database.deviceId) throw SyncFileException("That's this phone's own invite. Scan it on your partner's phone.")
        val existing = key(currentCollectionId)
        if (existing != null && currentCollectionId != invite.collectionId) {
            throw SyncFileException("This phone already shares a cellar. Stop sharing first, then join.")
        }
        database.write(fromSync = true) {
            val target = invite.collectionId
            if (currentCollectionId != target) {
                rekey(currentCollectionId, target, invite.householdName, now)
            }
            q.retireKeys(now, target)
            if (q.keyById(key.id).executeAsOneOrNull() == null) q.insertKey(key.id, target, Base64.encode(key.secret), now)
            q.upsertPeer(invite.inviterDevice, target, invite.inviterName.take(40), now)
            database.local.putSetting(SettingsRepository.KEY_ACTIVE_COLLECTION, target)
            touchedLocal("household_key")
            touchedLocal("collection")
        }
        return invite.collectionId
    }

    /**
     * Moves every row from [from] to [to] and rebuilds the journal, so this phone's whole cellar
     * is offered to the household as new changes. Ledger immutability is lifted only here.
     */
    private fun com.houseofvino.data.db.WriteScope.rekey(from: String, to: String, name: String, now: Long) {
        database.exec("PRAGMA defer_foreign_keys = ON")
        if (database.catalog.collectionById(to).executeAsOneOrNull() == null) {
            database.catalog.insertCollection(to, name.take(60), null, "editor", now, now)
        }
        database.local.setMaintenance(1)
        for (table in SyncTables.ALL.map { it.name } + "sync_conflict") {
            database.driver.execute(null, "UPDATE ${RowCodec.quote(table)} SET collection_id = ? WHERE collection_id = ?", 2) {
                bindString(0, to); bindString(1, from)
            }
        }
        database.local.setMaintenance(0)
        database.driver.execute(null, "DELETE FROM collection WHERE id = ?", 1) { bindString(0, from) }
        q.clearJournal()
        q.clearRowClocks()
        database.changeLog.recordUnjournaled(to)
        SyncTables.ALL.forEach { touchedLocal(it.name) }
    }

    /**
     * Removes a partner: rotates the key so files they make from now on are rejected. What they
     * already have stays with them. Other remaining phones must join again with a new invite.
     */
    fun removePartner(collectionId: String, deviceId: String) = database.write {
        q.removePeer(now, deviceId)
        q.retireKeys(now, collectionId)
        val key = HouseholdKey.generate()
        q.insertKey(key.id, collectionId, Base64.encode(key.secret), now)
        touchedLocal("household_key")
    }

    /** Stops sharing on this phone. The cellar stays; the key and partner list are forgotten. */
    fun stopSharing(collectionId: String) = database.write {
        q.retireKeys(now, collectionId)
        database.driver.execute(null, "DELETE FROM peer WHERE collection_id = ?", 1) { bindString(0, collectionId) }
        touchedLocal("household_key")
    }
}
