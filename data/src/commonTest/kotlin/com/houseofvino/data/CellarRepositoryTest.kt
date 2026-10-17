package com.houseofvino.data

import com.houseofvino.core.inventory.LedgerError
import com.houseofvino.core.inventory.MovementKind
import com.houseofvino.core.model.GrapeShare
import com.houseofvino.core.model.LocationKind
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.Money
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.repo.DuplicateWineException
import com.houseofvino.data.repo.LocationInput
import com.houseofvino.data.repo.LocationInUseException
import com.houseofvino.data.repo.LotInput
import com.houseofvino.data.repo.OpenedBottle
import com.houseofvino.data.repo.StockRemainingException
import com.houseofvino.data.repo.TastingInput
import com.houseofvino.data.repo.VintageExistsException
import com.houseofvino.data.repo.VintageInput
import com.houseofvino.data.repo.WineInput
import com.houseofvino.data.repo.WineTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CellarRepositoryTest {
    private val app = TestApp()
    private val c = app.collectionId
    private val eur = CurrencyCode.EUR

    private val margaux = WineInput("Château Margaux", null, WineColor.RED, country = "France", region = "Bordeaux",
        appellation = "Margaux", grapes = listOf(GrapeShare("Cabernet Sauvignon", 90), GrapeShare("Merlot", 10)))

    private fun location(name: String, parent: String? = null, rows: Int? = null, cols: Int? = null) =
        app.locations.create(c, LocationInput(name, LocationKind.RACK, parent, rows, cols))

    @Test
    fun addWineWithPurchaseCreatesStockAndOutbox() {
        val rack = location("Rack A")
        val v = app.cellar.addWine(c, WineTarget.NewWine(margaux), VintageInput(VintageYear.Year(2015), 2030, 2060, "Producer"),
            LotInput(quantity = 6, locationId = rack, unitPrice = Money(45000, eur), acquiredOn = IsoDate.of(2018, 5, 1), source = "Merchant"),
            tags = listOf("Special", "Gift"))
        val detail = assertNotNull(app.cellar.vintageDetail(v))
        assertEquals(6, detail.totalBottles)
        assertEquals("Rack A", detail.stock.single().locationName)
        assertEquals(listOf("Gift", "Special"), detail.tags.map { it.name })
        assertEquals(2, detail.wine.grapes.size)
        // wine + 2 grapes + vintage + lot + movement + 2 tags + 2 links + location = 11 synced rows
        assertEquals(11, app.outboxCount())
        assertEquals(0, app.cellar.stockDrift())
    }

    @Test
    fun duplicateWinesAreCaughtAndExistingVintagesReused() {
        val v1 = app.cellar.addWine(c, WineTarget.NewWine(margaux), VintageInput(VintageYear.Year(2015)), LotInput(quantity = 1))
        val err = assertFailsWith<DuplicateWineException> {
            app.cellar.addWine(c, WineTarget.NewWine(margaux.copy(producer = "CHATEAU MARGAUX")), VintageInput(VintageYear.Year(2016)), null)
        }
        val wineId = app.cellar.vintage(v1)!!.wineId
        assertEquals(listOf(wineId), err.existingWineIds)
        // Same vintage via the existing wine → same record, new purchase.
        val again = app.cellar.addWine(c, WineTarget.ExistingWine(wineId), VintageInput(VintageYear.Year(2015)), LotInput(quantity = 2))
        assertEquals(v1, again)
        assertEquals(3, app.cellar.quantityForVintage(v1))
        // A different vintage is a different record.
        val v16 = app.cellar.addWine(c, WineTarget.ExistingWine(wineId), VintageInput(VintageYear.Year(2016)), LotInput(quantity = 1))
        assertTrue(v16 != v1)
        val nv = app.cellar.addWine(c, WineTarget.ExistingWine(wineId), VintageInput(VintageYear.NonVintage), null)
        assertTrue(nv != v1 && nv != v16)
    }

    @Test
    fun consumptionTransferAndReversalKeepTheLedgerConsistent() {
        val a = location("Rack A")
        val b = location("Fridge")
        val v = app.cellar.addWine(c, WineTarget.NewWine(margaux), VintageInput(VintageYear.Year(2015)), LotInput(quantity = 6, locationId = a))
        val lot = app.cellar.stockLines(v).single().lotId
        app.cellar.transfer(lot, a, b, 2)
        val drink = app.cellar.depart(lot, b, 1, MovementKind.CONSUME, note = "Birthday")
        assertEquals(mapOf<String?, Int>(a to 4, b to 1), app.cellar.stockLines(v).associate { it.locationId to it.quantity })
        assertFailsWith<LedgerError.InsufficientStock> { app.cellar.depart(lot, b, 2, MovementKind.CONSUME) }
        app.cellar.reverse(drink.id)
        assertFailsWith<LedgerError.AlreadyReversed> { app.cellar.reverse(drink.id) }
        assertEquals(6, app.cellar.quantityForVintage(v))
        val history = app.cellar.movementHistory(v)
        assertEquals(5, history.size) // purchase, 2 transfer legs, consume, reversal
        assertTrue(history.first { it.movement.id == drink.id }.reversed)
        assertEquals(0, app.cellar.stockDrift())
    }

    @Test
    fun departFromVintageAllocatesOldestPurchaseFirst() {
        val v = app.cellar.addWine(c, WineTarget.NewWine(margaux), VintageInput(VintageYear.Year(2015)),
            LotInput(quantity = 2, acquiredOn = IsoDate.of(2019, 1, 1)))
        val newer = app.cellar.addLot(c, v, LotInput(quantity = 3, acquiredOn = IsoDate.of(2022, 1, 1)))
        val moves = app.cellar.departFromVintage(v, 3, MovementKind.SELL)
        assertEquals(listOf(-2, -1), moves.map { it.delta })
        assertEquals(2, app.cellar.stockLines(v).single { it.lotId == newer }.quantity)
    }

    @Test
    fun deletingAVintageWithStockIsRefusedAndHistorySurvivesDeletion() {
        val v = app.cellar.addWine(c, WineTarget.NewWine(margaux), VintageInput(VintageYear.Year(2015)), LotInput(quantity = 1))
        assertFailsWith<StockRemainingException> { app.cellar.deleteVintage(v) }
        val lot = app.cellar.stockLines(v).single().lotId
        val tastingId = app.journal.add(c, v, TastingInput(IsoDate.of(2026, 10, 1), rating = 95, notes = "Superb"), OpenedBottle(lot, null))
        assertEquals(0, app.cellar.quantityForVintage(v))
        val tasting = assertNotNull(app.journal.tasting(tastingId))
        assertNotNull(tasting.movementId)
        app.cellar.deleteVintage(v)
        // The tasting remains, linked to the (tombstoned) vintage, and still shows in the journal.
        assertEquals(1, app.journal.count(c))
        assertEquals(1, app.search.searchJournal(c, com.houseofvino.core.search.SearchQuery(), 0, 10).total)
    }

    @Test
    fun vintageChangesNeverMergeSilently() {
        val v15 = app.cellar.addWine(c, WineTarget.NewWine(margaux), VintageInput(VintageYear.Year(2015)), LotInput(quantity = 1))
        val wineId = app.cellar.vintage(v15)!!.wineId
        val v16 = app.cellar.addWine(c, WineTarget.ExistingWine(wineId), VintageInput(VintageYear.Year(2016)), LotInput(quantity = 2))
        assertFailsWith<VintageExistsException> { app.cellar.updateVintage(v16, VintageInput(VintageYear.Year(2015))) }
        assertFailsWith<IllegalArgumentException> { app.cellar.mergeVintages(v16, v15, confirmDifferentVintage = false) }
        app.cellar.mergeVintages(v16, v15, confirmDifferentVintage = true)
        assertEquals(3, app.cellar.quantityForVintage(v15))
        assertNotNull(app.cellar.vintage(v16)) // tombstoned, not destroyed
    }

    @Test
    fun locationsTreeSlotsAndDeletionRules() {
        val cellar = location("Main cellar")
        val rack = location("Rack A", cellar, rows = 2, cols = 3)
        val v = app.cellar.addWine(c, WineTarget.NewWine(margaux), VintageInput(VintageYear.Year(2015)), LotInput(quantity = 2, locationId = rack))
        val lot = app.cellar.stockLines(v).single().lotId
        val tree = app.locations.tree(c)
        assertEquals(listOf("Main cellar", "Rack A"), tree.map { it.location.name })
        assertEquals(2, tree.first().bottlesTotal)
        app.locations.placeBottle(rack, 1, 1, lot)
        app.locations.placeBottle(rack, 1, 2, lot)
        assertFailsWith<LocationInUseException> { app.locations.placeBottle(rack, 2, 1, lot) } // only 2 bottles
        app.cellar.depart(lot, rack, 1, MovementKind.CONSUME)
        assertEquals(1, app.locations.grid(rack).count { it.lotId != null }) // a position was released
        assertFailsWith<LocationInUseException> { app.locations.delete(cellar) }
        assertFailsWith<IllegalArgumentException> { app.locations.update(cellar, LocationInput("Main cellar", LocationKind.CELLAR, parentId = rack)) }
    }

    @Test
    fun invalidInputIsRejectedBeforeWriting() {
        assertFailsWith<IllegalArgumentException> {
            app.cellar.addWine(c, WineTarget.NewWine(WineInput(" ")), VintageInput(VintageYear.Year(2015)), null)
        }
        assertFailsWith<IllegalArgumentException> {
            app.cellar.addWine(c, WineTarget.NewWine(margaux), VintageInput(VintageYear.Year(2015), 2040, 2030), null)
        }
        assertFailsWith<IllegalArgumentException> {
            app.cellar.addWine(c, WineTarget.NewWine(margaux.copy(grapes = listOf(GrapeShare("A", 80), GrapeShare("B", 40)))),
                VintageInput(VintageYear.Year(2015)), null)
        }
        assertEquals(0, app.outboxCount())
    }
}
