package com.houseofvino.data

import com.houseofvino.core.inventory.MovementKind
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.search.SearchFilters
import com.houseofvino.core.search.SearchQuery
import com.houseofvino.data.repo.LotInput
import com.houseofvino.data.repo.VintageInput
import com.houseofvino.data.repo.WineInput
import com.houseofvino.data.repo.WineTarget
import com.houseofvino.data.sync.SyncEngine
import com.houseofvino.data.sync.TransientSyncException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncEngineTest {
    private val server = FakeSyncServer()

    /** Two devices of the same user, plus a household member, sharing one collection. */
    private val phone = TestApp()
    private val tablet = TestApp()
    private val c = phone.collectionId

    private val phoneRemote = server.Client("alice")
    private val tabletRemote = server.Client("alice")
    private val phoneSync = SyncEngine(phone.db, phoneRemote)
    private val tabletSync = SyncEngine(tablet.db, tabletRemote)

    init {
        server.roles["alice"] = hashMapOf(c to "owner")
    }

    private fun addWine(app: TestApp, producer: String, qty: Int = 3): String =
        app.cellar.addWine(c, WineTarget.NewWine(WineInput(producer)), VintageInput(VintageYear.Year(2015)), LotInput(quantity = qty))

    @Test
    fun offlineChangesSyncToAnotherDevice() = runTest {
        phoneRemote.offline = true
        val v = addWine(phone, "Château Latour", qty = 6)
        assertFailsWith<TransientSyncException> { phoneSync.sync(c) }
        assertTrue(phone.outboxCount() > 0, "changes stay queued while offline")
        phoneRemote.offline = false
        phone.clock.advance(60 * 60_000) // past any backoff
        phoneSync.sync(c)
        assertEquals(0, phone.outboxCount())
        tabletSync.sync(c)
        assertEquals(6, tablet.cellar.quantityForVintage(v))
        assertEquals(1, tablet.search.searchCellar(c, SearchQuery("latour"), 0, 10, 2026, null).total)
        assertEquals(0, tablet.outboxCount(), "pulled rows are not echoed back")
    }

    @Test
    fun lostResponseIsRetriedIdempotently() = runTest {
        addWine(phone, "Penfolds")
        phoneRemote.dropNextResponse = true
        assertFailsWith<TransientSyncException> { phoneSync.sync(c) }
        phone.clock.advance(60 * 60_000)
        phoneSync.sync(c)
        assertEquals(1, server.rowCount("wine"))
        assertEquals(1, server.rowCount("movement"))
        assertEquals(0, phone.outboxCount())
    }

    @Test
    fun concurrentEditsAreDetectedAndResolvable() = runTest {
        val v = addWine(phone, "Ridge")
        phoneSync.sync(c); tabletSync.sync(c)
        val wineId = phone.cellar.vintage(v)!!.wineId
        phone.cellar.updateWine(wineId, WineInput("Ridge", notes = "phone edit"))
        tablet.cellar.updateWine(wineId, WineInput("Ridge", notes = "tablet edit"))
        phoneSync.sync(c)
        val report = tabletSync.sync(c)
        assertEquals(1, report.conflicts)
        // Server (phone) version wins locally until the user chooses…
        assertEquals("phone edit", tablet.cellar.wine(wineId)!!.notes)
        val conflict = tablet.db.local.openConflicts().executeAsList().single()
        // …and the user keeps the tablet version.
        tabletSync.resolveKeepLocal(conflict.id)
        assertEquals("tablet edit", tablet.cellar.wine(wineId)!!.notes)
        tabletSync.sync(c)
        phoneSync.sync(c)
        assertEquals("tablet edit", phone.cellar.wine(wineId)!!.notes)
        assertTrue(tablet.db.local.openConflicts().executeAsList().isEmpty())
    }

    @Test
    fun sequentialEditsOnOneDeviceDoNotConflictWithThemselves() = runTest {
        val v = addWine(phone, "Rayas")
        val wineId = phone.cellar.vintage(v)!!.wineId
        phoneSync.sync(c)
        phone.cellar.updateWine(wineId, WineInput("Rayas", notes = "one"))
        phoneRemote.dropNextResponse = true
        assertFailsWith<TransientSyncException> { phoneSync.sync(c) }
        phone.cellar.updateWine(wineId, WineInput("Rayas", notes = "two")) // queued after an attempted op
        phone.clock.advance(60 * 60_000)
        val report = phoneSync.sync(c)
        assertEquals(0, report.conflicts)
        assertEquals("two", (server.row("wine", wineId)!!["notes"] as kotlinx.serialization.json.JsonPrimitive).content)
    }

    @Test
    fun concurrentStockChangesMergeAndOverConsumptionIsFlagged() = runTest {
        val v = addWine(phone, "Leroy", qty = 2)
        phoneSync.sync(c); tabletSync.sync(c)
        val lot = phone.cellar.stockLines(v).single().lotId
        // Both devices drink offline: phone 1 bottle, tablet 2 bottles (only 2 exist).
        phone.cellar.depart(lot, null, 1, MovementKind.CONSUME)
        tablet.cellar.depart(lot, null, 2, MovementKind.CONSUME)
        phoneSync.sync(c)
        val report = tabletSync.sync(c)
        assertEquals(0, report.conflicts, "ledger rows never conflict")
        assertEquals(1, report.stockConflicts)
        assertEquals(-1, tablet.cellar.quantityForVintage(v))
        phoneSync.sync(c)
        assertEquals(-1, phone.cellar.quantityForVintage(v), "both devices converge on the same ledger")
        assertEquals(3, server.rowCount("movement")) // purchase + one departure from each device
    }

    @Test
    fun deletesPropagateAsTombstones() = runTest {
        val v = addWine(phone, "Clos Rougeard", qty = 0)
        phoneSync.sync(c); tabletSync.sync(c)
        assertNotNull(tablet.cellar.vintage(v))
        phone.cellar.deleteVintage(v)
        phoneSync.sync(c); tabletSync.sync(c)
        assertEquals(0, tablet.search.searchCellar(c, SearchQuery("rougeard", filters = SearchFilters(inStockOnly = false)), 0, 10, 2026, null).total)
        assertNotNull(tablet.db.catalog.vintageById(v).executeAsOne().deleted_at)
    }

    @Test
    fun viewersCannotWriteAndStrangersSeeNothing() = runTest {
        server.roles["bob"] = hashMapOf(c to "viewer")
        val bobApp = TestApp()
        val bob = SyncEngine(bobApp.db, server.Client("bob"))
        addWine(phone, "Shared Wine")
        phoneSync.sync(c)
        bob.sync(c)
        assertEquals(1, bobApp.search.searchCellar(c, SearchQuery("shared"), 0, 10, 2026, null).total, "viewer can read")
        addWine(bobApp, "Bob's Wine")
        val report = bob.sync(c)
        assertTrue(report.rejected > 0)
        assertEquals(0, bobApp.db.local.dueOps(c, Long.MAX_VALUE, 100).executeAsList().size, "rejected ops are not retried forever")
        assertTrue(bob.status.value.failedOps > 0)
        assertNull(server.row("wine", bobApp.cellar.findWinesByIdentity(c, "Bob's Wine", null).single().id))

        val eveApp = TestApp()
        SyncEngine(eveApp.db, server.Client("eve")).sync(c)
        assertEquals(0, eveApp.search.searchCellar(c, SearchQuery(), 0, 10, 2026, null).total, "non-members see nothing")
    }

    @Test
    fun retryBacksOffAndRecovers() = runTest {
        addWine(phone, "Trimbach")
        phoneRemote.offline = true
        assertFailsWith<TransientSyncException> { phoneSync.sync(c) }
        val calls = phoneRemote.pushCalls
        phoneRemote.offline = false
        phoneSync.sync(c) // ops are not due yet: nothing is pushed
        assertEquals(calls, phoneRemote.pushCalls)
        assertTrue(phone.outboxCount() > 0)
        phone.clock.advance(SyncEngine.MAX_BACKOFF_MS + 1)
        phoneSync.sync(c)
        assertEquals(0, phone.outboxCount())
    }
}
