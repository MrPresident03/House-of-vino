package com.houseofvino.data

import app.cash.sqldelight.db.QueryResult
import com.houseofvino.data.db.HouseOfVinoDatabase
import com.houseofvino.data.sync.ColType
import com.houseofvino.data.sync.SyncTables
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SchemaTest {

    private fun columns(app: TestApp, table: String): List<Pair<String, String>> =
        app.db.driver.executeQuery(null, "PRAGMA table_info(\"$table\")", { c ->
            val out = ArrayList<Pair<String, String>>()
            while (c.next().value) out += c.getString(1)!! to c.getString(2)!!.uppercase()
            QueryResult.Value(out)
        }, 0).value

    @Test
    fun syncRegistryMatchesTheRealSchema() {
        val app = TestApp()
        for (table in SyncTables.ALL) {
            val actual = columns(app, table.name)
            assertTrue(actual.isNotEmpty(), "table ${table.name} exists")
            assertEquals(actual.map { it.first }.toSet(), table.columnNames.toSet(), "columns of ${table.name}")
            for ((name, type) in table.columns) {
                val sqlType = actual.first { it.first == name }.second
                assertEquals(if (type == ColType.TEXT) "TEXT" else "INTEGER", sqlType, "${table.name}.$name type")
            }
        }
    }

    @Test
    fun freshSchemaHasExpectedVersionAndForeignKeys() {
        val app = TestApp()
        assertTrue(HouseOfVinoDatabase.Schema.version >= 1)
        assertEquals(1L, app.db.queryLong("PRAGMA foreign_keys"))
        assertFailsWith<Exception> {
            app.db.exec("INSERT INTO wine(id, collection_id, producer, producer_sort, created_at, updated_at) VALUES ('w', 'missing', 'X', 'x', 0, 0)")
        }
    }

    @Test
    fun ledgerRowsAreImmutable() {
        val app = TestApp()
        val vintageId = app.cellar.addWine(app.collectionId,
            com.houseofvino.data.repo.WineTarget.NewWine(com.houseofvino.data.repo.WineInput("Test")),
            com.houseofvino.data.repo.VintageInput(com.houseofvino.core.model.VintageYear.Year(2020)),
            com.houseofvino.data.repo.LotInput(quantity = 2))
        val movementId = app.cellar.movementHistory(vintageId).single().movement.id
        assertFailsWith<Exception> { app.db.exec("UPDATE movement SET delta = 5 WHERE id = '$movementId'") }
        assertFailsWith<Exception> { app.db.exec("DELETE FROM movement WHERE id = '$movementId'") }
        app.db.exec("UPDATE movement SET rev = 7 WHERE id = '$movementId'") // rev is the only mutable column
        assertEquals(7L, app.db.queryLong("SELECT rev FROM movement WHERE id = ?", movementId))
    }

    @Test
    fun wipeAllErasesEverythingIncludingTheLedgerButKeepsTheDeviceId() {
        val app = TestApp()
        val deviceId = app.settings.get("device_id")
        // Nested locations and a reversal: self-referencing rows must not block the wipe.
        val cellarId = app.locations.create(app.collectionId, com.houseofvino.data.repo.LocationInput("Cellar", com.houseofvino.core.model.LocationKind.CELLAR))
        val rackId = app.locations.create(app.collectionId, com.houseofvino.data.repo.LocationInput("Rack A", com.houseofvino.core.model.LocationKind.RACK, parentId = cellarId))
        val vintageId = app.cellar.addWine(app.collectionId,
            com.houseofvino.data.repo.WineTarget.NewWine(com.houseofvino.data.repo.WineInput("Wipe Test")),
            com.houseofvino.data.repo.VintageInput(com.houseofvino.core.model.VintageYear.Year(2018)),
            com.houseofvino.data.repo.LotInput(quantity = 3, locationId = rackId))
        app.cellar.reverse(app.cellar.movementHistory(vintageId).single().movement.id)
        assertEquals(1L, app.db.queryLong("SELECT count(*) FROM movement WHERE reversal_of IS NOT NULL"))
        com.houseofvino.data.repo.PhotoRepository(app.db).add(app.collectionId, com.houseofvino.data.repo.PhotoOwner.VINTAGE, vintageId, "/photos/p1.jpg", 10, 10, 100)
        app.settings.put("onboarded", "true")
        assertTrue((app.db.queryLong("SELECT count(*) FROM sync_outbox") ?: 0) > 0)

        val paths = app.db.wipeAll()

        assertEquals(listOf("/photos/p1.jpg"), paths)
        for (table in SyncTables.ALL.map { it.name } + listOf("collection", "stock_level", "sync_outbox", "search_doc", "search_fts")) {
            assertEquals(0L, app.db.queryLong("SELECT count(*) FROM \"$table\""), "$table is empty")
        }
        assertEquals(deviceId, app.settings.get("device_id"))
        assertEquals(null, app.settings.get("onboarded"))
        // Immutability is back on after the wipe.
        assertEquals(0L, app.db.queryLong("SELECT count(*) FROM local_maintenance WHERE active = 1"))
    }

    @Test
    fun ftsIsAvailableOnThisPlatform() {
        val app = TestApp()
        assertEquals(0L, app.db.queryLong("SELECT count(*) FROM search_fts"))
        assertEquals("1", app.settings.get("search_index_version"))
    }
}
