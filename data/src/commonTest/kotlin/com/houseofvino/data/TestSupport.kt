package com.houseofvino.data

import app.cash.sqldelight.db.SqlDriver
import com.houseofvino.core.id.IdGenerator
import com.houseofvino.core.time.Clock
import com.houseofvino.data.db.DatabaseFactory
import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.io.ExportService
import com.houseofvino.data.io.ImportService
import com.houseofvino.data.repo.CellarRepository
import com.houseofvino.data.repo.InsightsRepository
import com.houseofvino.data.repo.JournalRepository
import com.houseofvino.data.repo.LocationRepository
import com.houseofvino.data.repo.SettingsRepository
import com.houseofvino.data.repo.WishlistRepository
import com.houseofvino.data.search.SearchEngine

/** A fresh in-memory database for the current platform's driver. */
expect fun createTestDriver(): SqlDriver

class TestClock(var now: Long = 1_760_000_000_000L) : Clock {
    override fun nowMillis(): Long = now
    fun advance(ms: Long = 1_000) { now += ms }
}

/** Everything a test needs, wired the same way the app wires it. */
class TestApp(
    val clock: TestClock = TestClock(),
    ids: IdGenerator = IdGenerator.Random,
    driver: SqlDriver = createTestDriver(),
    val collectionId: String = "c0000000-0000-4000-8000-000000000001",
) {
    val db: LocalDatabase = DatabaseFactory.open(driver, clock, ids)
    val cellar = CellarRepository(db)
    val locations = LocationRepository(db)
    val journal = JournalRepository(db, cellar)
    val wishlist = WishlistRepository(db, cellar)
    val settings = SettingsRepository(db)
    val search = SearchEngine(db)
    val insights = InsightsRepository(db, locations)
    val importer = ImportService(db, cellar, locations)
    val exporter = ExportService(db, locations)
    val household = com.houseofvino.data.sync.HouseholdRepository(db)
    val syncFiles = com.houseofvino.data.sync.SyncFileService(db, household)
    val conflicts = com.houseofvino.data.sync.ConflictService(db)

    init {
        db.catalog.insertCollection(collectionId, "My cellar", null, "owner", clock.now, clock.now)
    }

    fun journalCount(): Long = db.queryLong("SELECT count(*) FROM journal") ?: 0L
}
