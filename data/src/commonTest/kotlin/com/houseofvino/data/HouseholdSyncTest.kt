package com.houseofvino.data

import app.cash.sqldelight.db.QueryResult
import com.houseofvino.core.inventory.MovementKind
import com.houseofvino.core.model.LocationKind
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.Money
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.repo.LocationInput
import com.houseofvino.data.repo.LotInput
import com.houseofvino.data.repo.PhotoOwner
import com.houseofvino.data.repo.PhotoRepository
import com.houseofvino.data.repo.TastingInput
import com.houseofvino.data.repo.VintageInput
import com.houseofvino.data.repo.WineInput
import com.houseofvino.data.repo.WineTarget
import com.houseofvino.data.sync.BundleCodec
import com.houseofvino.data.sync.Invite
import com.houseofvino.data.sync.SyncFileException
import com.houseofvino.data.sync.SyncTables
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phones exchanging sync files (ADR-13). Each [Phone] is a full app database; "sending" is
 * exporting a file on one phone and importing its text on another.
 */
class HouseholdSyncTest {

    private val sharedClock = TestClock()

    /** One phone. Photos are kept in a map standing in for app-private files. */
    inner class Phone(name: String, collection: String) {
        val app = TestApp(clock = sharedClock, collectionId = collection)
        var cid = collection
        val files = HashMap<String, ByteArray>()
        init { app.household.setDeviceName(name) }

        fun send(includePhotos: Boolean = true): String =
            app.syncFiles.export(cid, includePhotos) { files[it] }.text

        fun receive(text: String) = app.syncFiles.import(cid, text) { id, bytes -> "mem://$id".also { files[it] = bytes } }

        fun share(name: String = "Our cellar"): Invite = app.household.create(cid, name)

        fun join(invite: Invite) { cid = app.household.join(Invite.decode(invite.encode()), cid) }

        fun wine(producer: String, year: Int, bottles: Int, location: String? = null): String =
            app.cellar.addWine(cid, WineTarget.NewWine(WineInput(producer, color = WineColor.RED, region = "Bordeaux")),
                VintageInput(VintageYear.Year(year)),
                LotInput(quantity = bottles, locationId = location, unitPrice = Money(4500, CurrencyCode.EUR), acquiredOn = IsoDate.of(2024, 3, 1)))

        fun lotOf(vintageId: String) = app.cellar.stockLines(vintageId).first()

        /** Every synced row, as text, for comparing two phones. */
        fun dump(): List<String> = SyncTables.ALL.flatMap { table ->
            val cols = table.syncedColumns.map { it.first }.filter { it != "rev" }
            app.db.driver.executeQuery(null,
                "SELECT ${cols.joinToString { "\"$it\"" }} FROM \"${table.name}\" ORDER BY id", { c ->
                    val out = ArrayList<String>()
                    while (c.next().value) out += table.name + ":" + cols.indices.joinToString("|") { i ->
                        if (table.type(cols[i]) == com.houseofvino.data.sync.ColType.TEXT) c.getString(i).toString() else c.getLong(i).toString()
                    }
                    QueryResult.Value(out)
                }, 0).value
        }

        fun bottles(): Long = app.db.queryLong("SELECT coalesce(sum(quantity), 0) FROM stock_level") ?: 0
        fun openConflicts() = app.conflicts.open()
    }

    private fun pair(): Pair<Phone, Phone> {
        val a = Phone("Ana's iPhone", "c0000000-0000-4000-8000-00000000000a")
        val b = Phone("Ben's Pixel", "c0000000-0000-4000-8000-00000000000b")
        return a to b
    }

    private fun tick(ms: Long = 1_000) = sharedClock.advance(ms)

    @Test
    fun partnersEndUpWithTheSameCellarIncludingWhatTheJoinerAlreadyHad() {
        val (a, b) = pair()
        val rack = a.app.locations.create(a.cid, LocationInput("Rack", LocationKind.RACK))
        val margaux = a.wine("Château Margaux", 2015, 6, rack)
        a.app.journal.add(a.cid, margaux, TastingInput(IsoDate.of(2025, 1, 1), rating = 95, notes = "Cassis, cedar"))
        tick()
        b.wine("Domaine Leflaive", 2019, 3) // Ben already had a cellar

        val invite = a.share()
        b.join(invite)
        assertEquals(a.cid, b.cid, "the joiner moves into the household")

        val toBen = a.receiveFrom(b) // Ben's cellar travels to Ana
        val toAna = b.receive(a.send())
        assertEquals("Ana's iPhone", toAna.fromName)
        assertTrue(toBen.applied > 0 && toAna.applied > 0)

        assertEquals(a.dump(), b.dump())
        assertEquals(9L, a.bottles())
        assertEquals(9L, b.bottles())
        assertEquals("Cassis, cedar", b.app.journal.forVintage(margaux).single().notes)
    }

    private fun Phone.receiveFrom(other: Phone) = receive(other.send())

    @Test
    fun editsToDifferentFieldsOnEachPhoneBothSurvive() {
        val (a, b) = pair()
        val v = a.wine("Château Talbot", 2016, 4)
        b.join(a.share()); b.receive(a.send()); a.receive(b.send())

        tick()
        a.app.cellar.updateVintage(v, VintageInput(VintageYear.Year(2016), drinkFrom = 2026, drinkUntil = 2040, drinkWindowSource = "My estimate"))
        tick()
        b.app.cellar.updateVintage(v, VintageInput(VintageYear.Year(2016), notes = "Bought for the wedding"))
        // Each phone sends only the fields it changed, so neither edit undoes the other.
        a.receive(b.send()); b.receive(a.send())

        assertEquals(a.dump(), b.dump())
        val vintage = a.app.cellar.vintage(v)!!
        assertEquals(2040, vintage.drinkUntil)
        assertEquals("Bought for the wedding", vintage.notes)
        assertTrue(a.openConflicts().isEmpty() && b.openConflicts().isEmpty())
    }

    @Test
    fun sameFieldChangedOnBothPhonesKeepsTheLaterEditAndAsksTheUser() {
        val (a, b) = pair()
        val v = a.wine("Château Lynch-Bages", 2010, 2)
        b.join(a.share()); b.receive(a.send()); a.receive(b.send())

        tick()
        a.app.cellar.updateVintage(v, VintageInput(VintageYear.Year(2010), notes = "Ana's note"))
        tick()
        b.app.cellar.updateVintage(v, VintageInput(VintageYear.Year(2010), notes = "Ben's note"))
        val fromBen = b.send()
        val fromAna = a.send()
        a.receive(fromBen); b.receive(fromAna)

        assertEquals("Ben's note", a.app.cellar.vintage(v)!!.notes)
        assertEquals("Ben's note", b.app.cellar.vintage(v)!!.notes)
        val conflictOnAna = a.openConflicts().single()
        val field = a.app.conflicts.fields(conflictOnAna).single()
        assertEquals("notes", field.column)
        assertEquals("Ben's note", field.current)
        assertEquals("Ana's note", field.other)
        assertEquals(1, b.openConflicts().size)

        // Ana prefers her own note: that's a new edit, which reaches Ben and closes his conflict.
        tick()
        a.app.conflicts.useOther(conflictOnAna.id)
        b.receive(a.send())
        assertEquals("Ana's note", b.app.cellar.vintage(v)!!.notes)
        assertTrue(a.openConflicts().isEmpty())
        assertTrue(b.openConflicts().isEmpty())
        a.receive(b.send())
        assertEquals(a.dump(), b.dump())
    }

    @Test
    fun bothDrinkingTheLastBottleIsFlaggedNotLost() {
        val (a, b) = pair()
        val v = a.wine("Penfolds Grange", 2008, 1)
        b.join(a.share()); b.receive(a.send()); a.receive(b.send())

        tick()
        val lot = a.lotOf(v)
        a.app.cellar.depart(lot.lotId, lot.locationId, 1, MovementKind.CONSUME, note = "Anniversary")
        b.app.cellar.depart(lot.lotId, lot.locationId, 1, MovementKind.CONSUME, note = "Friday")
        a.receive(b.send()); b.receive(a.send())

        assertEquals(-1L, a.bottles())
        assertEquals(-1L, b.bottles())
        assertEquals(a.dump(), b.dump())
        assertEquals("stock", a.openConflicts().single().entity)
        assertEquals(2, a.app.cellar.movementHistory(v).count { it.movement.kind == MovementKind.CONSUME })
    }

    @Test
    fun receivingTheSameFileTwiceChangesNothing() {
        val (a, b) = pair()
        a.wine("Vega Sicilia", 2011, 3)
        b.join(a.share())
        val file = a.send()
        val first = b.receive(file)
        val before = b.dump()
        val second = b.receive(file)
        assertTrue(first.applied > 0)
        assertEquals(0, second.applied)
        assertEquals(first.applied, second.duplicates)
        assertTrue(second.nothingNew)
        assertEquals(before, b.dump())
    }

    @Test
    fun aThirdPhoneGetsEverythingThroughAnotherPartner() {
        val (a, b) = pair()
        val c = Phone("Ana's iPad", "c0000000-0000-4000-8000-00000000000c")
        a.wine("Krug", 2002, 2)
        val invite = a.share()
        b.join(invite)
        c.join(invite)
        b.receive(a.send())
        b.receive(c.send()) // Ben learns that the iPad exists (and that it has nothing yet)
        tick()
        b.wine("Dom Pérignon", 2012, 1)
        c.receive(b.send()) // the iPad never hears from Ana directly
        assertEquals(b.dump(), c.dump())
        assertEquals(3L, c.bottles())
    }

    @Test
    fun filesThatAreDamagedForeignOrFromYourselfAreRefused() {
        val (a, b) = pair()
        a.wine("Opus One", 2018, 2)
        b.join(a.share())
        val good = a.send()

        val body = good.lines()
        val tampered = body.mapIndexed { i, l -> if (i == 3) l.replaceRange(10, 11, if (l[10] == 'A') "B" else "A") else l }.joinToString("\n")
        assertFailsWith<SyncFileException> { b.receive(tampered) }
        assertFailsWith<SyncFileException> { b.receive("hello") }
        assertFailsWith<SyncFileException> { a.receive(good) } // made on this phone

        val stranger = Phone("Someone else", "c0000000-0000-4000-8000-0000000000ff")
        stranger.wine("Other", 2000, 1)
        stranger.share("Theirs")
        assertFailsWith<SyncFileException> { b.receive(stranger.send()) }

        assertEquals(0L, b.bottles(), "nothing applied from rejected files")
        b.receive(good)
        assertEquals(2L, b.bottles())
    }

    @Test
    fun aRemovedPartnersNewFilesAreRefused() {
        val (a, b) = pair()
        b.join(a.share())
        a.receive(b.send())
        val benDevice = b.app.db.deviceId
        a.app.household.removePartner(a.cid, benDevice)
        tick()
        b.wine("Sneaky", 2020, 12)
        assertFailsWith<SyncFileException> { a.receive(b.send()) }
        assertEquals(0L, a.bottles())
        assertTrue(a.app.household.info(a.cid)!!.peers.single { it.deviceId == benDevice }.removed)
    }

    @Test
    fun invitesRoundTripAndBothPhonesShowTheSameSafetyCode() {
        val (a, b) = pair()
        val invite = a.share("The Smiths' cellar")
        val decoded = Invite.decode("  " + invite.encode() + "\n")
        assertEquals(invite, decoded)
        assertTrue(invite.encode().startsWith("HOV1-"))
        b.join(decoded)
        val ia = a.app.household.info(a.cid)!!
        val ib = b.app.household.info(b.cid)!!
        assertEquals(ia.safetyCode, ib.safetyCode)
        assertEquals("The Smiths' cellar", ib.name)
        assertTrue(Regex("\\d{4} \\d{4} \\d{4}").matches(ia.safetyCode))
        assertFailsWith<SyncFileException> { Invite.decode("HOV1-garbage!!") }
        assertFailsWith<SyncFileException> { Invite.decode("hello") }
        assertFailsWith<SyncFileException> { a.app.household.join(invite, a.cid) } // own invite
    }

    @Test
    fun photosTravelInsideTheFile() {
        val (a, b) = pair()
        val v = a.wine("Sassicaia", 2016, 2)
        val jpeg = Random(3).nextBytes(5_000)
        a.files["/a/p1.jpg"] = jpeg
        PhotoRepository(a.app.db).add(a.cid, PhotoOwner.VINTAGE, v, "/a/p1.jpg", 800, 1200, jpeg.size.toLong())
        b.join(a.share())
        val result = b.receive(a.send(includePhotos = true))
        assertEquals(1, result.photos)
        val path = b.app.db.driver.executeQuery(null, "SELECT local_path FROM photo", { c ->
            QueryResult.Value(if (c.next().value) c.getString(0) else null)
        }, 0).value
        assertContentEquals(jpeg, b.files[path])
        assertEquals(a.dump(), b.dump()) // photo metadata identical; local paths are per phone
    }

    @Test
    fun theFileIsArmouredTextAndEncrypted() {
        val (a, b) = pair()
        a.wine("Château Secret", 2005, 1)
        b.join(a.share())
        val text = a.send()
        assertTrue(text.contains(BundleCodec.BEGIN) && text.contains(BundleCodec.END))
        assertTrue("Secret" !in text && "Bordeaux" !in text, "wine details must not be readable in the file")
        assertTrue(text.lines().all { it.length <= 120 })
    }

    @Test
    fun exportsOnlyContainWhatThePartnerLacks() {
        val (a, b) = pair()
        a.wine("Cheval Blanc", 2000, 2)
        b.join(a.share())
        b.receive(a.send())
        a.receive(b.send()) // Ana now knows what Ben has
        val nothing = a.app.syncFiles.export(a.cid, true) { null }
        assertEquals(0, nothing.changes)
        tick()
        a.wine("Petrus", 2001, 1)
        val small = a.app.syncFiles.export(a.cid, true) { null }
        assertTrue(small.changes in 1..10, "only the new wine's rows (${small.changes})")
    }

    @Test
    fun rowsFromBeforeTheJournalAreSharedToo() {
        val (a, b) = pair()
        a.wine("Old Wine", 1990, 2)
        // Simulate an install that predates the journal.
        a.app.db.exec("DELETE FROM journal")
        a.app.db.exec("DELETE FROM row_clock")
        a.app.db.write(fromSync = true) { a.app.db.changeLog.recordUnjournaled(a.cid) }
        b.join(a.share())
        b.receive(a.send())
        assertEquals(2L, b.bottles())
        assertEquals(a.dump(), b.dump())
    }

    @Test
    fun erasingThePhoneGivesItANewIdentity() {
        val (a, _) = pair()
        val before = a.app.db.deviceId
        a.wine("X", 2000, 1)
        a.app.db.wipeAll()
        assertNotEquals(before, a.app.db.deviceId)
        assertEquals(0L, a.app.journalCount())
        assertNull(a.app.household.key(a.cid))
    }

    /**
     * Random activity on two phones with files exchanged at random moments (sometimes lost,
     * sometimes delivered twice or late). After a final exchange both phones are identical and
     * the stock ledger matches its cache.
     */
    @Test
    fun randomActivityAlwaysConverges() {
        repeat(8) { round ->
            val rnd = Random(1000 + round)
            val (a, b) = pair()
            b.join(a.share())
            val phones = listOf(a, b)
            val inFlight = ArrayList<Pair<Phone, String>>() // (recipient, file)
            repeat(40) { step ->
                tick(rnd.nextLong(1, 2_000))
                val p = phones.random(rnd)
                val other = phones.first { it !== p }
                when (rnd.nextInt(7)) {
                    0, 1 -> p.wine("Producer $round-$step", 2000 + rnd.nextInt(5), 1 + rnd.nextInt(3))
                    2 -> vintages(p).randomOrNull(rnd)?.let { v ->
                        p.app.cellar.updateVintage(v, VintageInput(p.app.cellar.vintage(v)!!.vintage,
                            drinkUntil = 2030 + rnd.nextInt(10), notes = "n${rnd.nextInt(3)}"))
                    }
                    3 -> vintages(p).randomOrNull(rnd)?.let { v ->
                        p.app.cellar.stockLines(v).firstOrNull { it.quantity > 0 }?.let { s ->
                            p.app.cellar.depart(s.lotId, s.locationId, 1, MovementKind.CONSUME)
                        }
                    }
                    4 -> inFlight += other to p.send(includePhotos = false)
                    5 -> if (inFlight.isNotEmpty()) {
                        val (to, file) = inFlight.removeAt(rnd.nextInt(inFlight.size))
                        to.receive(file)
                        if (rnd.nextInt(4) == 0) to.receive(file) // delivered twice
                    }
                    6 -> if (inFlight.isNotEmpty() && rnd.nextInt(3) == 0) inFlight.removeAt(0) // lost
                }
            }
            repeat(2) { a.receive(b.send()); b.receive(a.send()) }
            assertEquals(a.dump(), b.dump(), "round $round")
            assertEquals(a.bottles(), b.bottles())
            assertEquals(0, a.app.cellar.stockDrift())
            assertEquals(0, b.app.cellar.stockDrift())
        }
    }

    private fun vintages(p: Phone): List<String> =
        p.app.db.driver.executeQuery(null, "SELECT id FROM vintage WHERE deleted_at IS NULL ORDER BY id", { c ->
            val out = ArrayList<String>()
            while (c.next().value) out += c.getString(0)!!
            QueryResult.Value(out)
        }, 0).value
}
