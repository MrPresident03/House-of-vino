package com.houseofvino.app

import app.cash.sqldelight.db.SqlDriver
import com.houseofvino.app.platform.PlatformServices
import com.houseofvino.core.id.IdGenerator
import com.houseofvino.core.money.CurrencyConverter
import com.houseofvino.core.time.Clock
import com.houseofvino.data.db.DatabaseFactory
import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.integrations.BarcodeLookupService
import com.houseofvino.data.integrations.OpenFoodFactsBarcodeLookup
import com.houseofvino.data.integrations.UnavailableBarcodeLookup
import com.houseofvino.data.io.ExportService
import com.houseofvino.data.io.ImportService
import com.houseofvino.data.repo.CellarRepository
import com.houseofvino.data.repo.InsightsRepository
import com.houseofvino.data.repo.JournalRepository
import com.houseofvino.data.repo.LocationRepository
import com.houseofvino.data.repo.PhotoRepository
import com.houseofvino.data.repo.SettingsRepository
import com.houseofvino.data.repo.WishlistRepository
import com.houseofvino.data.search.SearchEngine
import com.houseofvino.data.sync.ConflictService
import com.houseofvino.data.sync.HouseholdRepository
import com.houseofvino.data.sync.SyncFileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Hand-wired dependency graph (ADR-02). Created once per process by the platform entry point.
 * There is no backend (ADR-13): sharing with a partner goes through encrypted sync files.
 */
class AppGraph(
    val platform: PlatformServices,
    driver: SqlDriver,
    clock: Clock = Clock.System,
    ids: IdGenerator = IdGenerator.Random,
    barcodeLookup: BarcodeLookupService? = null,
) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val messenger = Messenger()
    val handoff = Handoff()
    val db: LocalDatabase = DatabaseFactory.open(driver, clock, ids)

    val settings = SettingsRepository(db)
    val cellar = CellarRepository(db)
    val locations = LocationRepository(db)
    val journal = JournalRepository(db, cellar)
    val wishlist = WishlistRepository(db, cellar)
    val photos = PhotoRepository(db)
    val search = SearchEngine(db)
    val insights = InsightsRepository(db, locations)
    val importer = ImportService(db, cellar, locations)
    val exporter = ExportService(db, locations)

    val household = HouseholdRepository(db, defaultDeviceName = "${platform.platformName} phone")
    val syncFiles = SyncFileService(db, household)
    val conflicts = ConflictService(db)

    /** Barcode lookup is an external service (Open Food Facts, ODbL); manual entry always works. */
    val barcodes: BarcodeLookupService = barcodeLookup ?: OpenFoodFactsBarcodeLookup.createOrNull() ?: UnavailableBarcodeLookup

    /** The active collection. Changes when this phone joins a household or is erased. */
    var collectionId: String = ensureLocalCollection()
        private set

    fun converter(): CurrencyConverter = CurrencyConverter(settings.rates(collectionId))

    /** Switches to the household's collection after joining it. */
    fun useCollection(id: String) {
        collectionId = id
    }

    /** Erases this phone's copy of the cellar ("Erase this phone"). */
    fun wipeLocalData() {
        db.wipeAll().forEach { path -> runCatching { platform.deletePhoto(path) } }
        // The old collection id may still belong to a household on the server; start afresh with
        // a new one so a later sign-in can never collide with it.
        collectionId = ensureLocalCollection()
    }

    private fun ensureLocalCollection(): String {
        settings.get(SettingsRepository.KEY_ACTIVE_COLLECTION)?.let { id ->
            if (db.catalog.collectionById(id).executeAsOneOrNull() != null) return id
        }
        val id = db.newId()
        val now = db.now()
        db.write {
            db.catalog.insertCollection(id, "My cellar", null, "owner", now, now)
            db.local.putSetting(SettingsRepository.KEY_ACTIVE_COLLECTION, id)
            touchedLocal("collection")
        }
        return id
    }
}

/**
 * Passes non-serialisable values (e.g. a scan's suggestions) to the next screen. Navigation
 * arguments carry only the key; values are consumed once.
 */
class Handoff {
    private val values = HashMap<String, Any>()
    private var counter = 0

    fun put(value: Any): String {
        val key = "h${++counter}"
        values[key] = value
        return key
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> take(key: String?): T? = key?.let { values.remove(it) as? T }
}
