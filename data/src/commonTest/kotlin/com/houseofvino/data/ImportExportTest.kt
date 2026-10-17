package com.houseofvino.data

import com.houseofvino.core.csv.Csv
import com.houseofvino.core.csv.DuplicateAssessment
import com.houseofvino.core.csv.ImportDecision
import com.houseofvino.core.csv.ImportOptions
import com.houseofvino.core.inventory.MovementKind
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.Money
import com.houseofvino.core.search.SearchQuery
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.repo.LotInput
import com.houseofvino.data.repo.TastingInput
import com.houseofvino.data.repo.VintageInput
import com.houseofvino.data.repo.WineInput
import com.houseofvino.data.repo.WineTarget
import com.houseofvino.data.repo.WishlistInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportExportTest {
    private val app = TestApp()
    private val c = app.collectionId
    private val options = ImportOptions(defaultCurrency = CurrencyCode.EUR)

    private val csv = """
        Producer,Wine,Vintage,Type,Varietal,Size,Quantity,Price,Currency,Purchase Date,Location,Tags
        Château Margaux,,2015,Red,Cabernet Sauvignon,750ml,6,450.00,EUR,2018-05-01,Main cellar/Rack A,"Gift, Special"
        Château Margaux,,2016,Red,Cabernet Sauvignon,750ml,3,420.00,EUR,2019-05-01,Main cellar/Rack A,
        Krug,Grande Cuvée,NV,White - Sparkling,,Magnum,1,,GBP,,Fridge,
        ,No producer,2010,Red,,,1,,,,,
        Roulot,Meursault,2019,White,Chardonnay,750ml,12,85.00,EUR,2021-03-01,Main cellar/Rack B,
    """.trimIndent()

    @Test
    fun importsValidRowsReportsErrorsAndCreatesLocations() {
        val preview = app.importer.preview(c, csv, options = options)
        assertEquals(4, preview.validRows)
        assertEquals(1, preview.errorRows)
        assertEquals(22, preview.bottles)
        val report = app.importer.commit(c, preview)
        assertEquals(3, report.winesCreated) // the 2016 Margaux attaches to the 2015 wine
        assertEquals(4, report.vintagesCreated)
        assertEquals(22, report.bottlesAdded)
        assertTrue(report.failed.isEmpty())
        assertEquals(listOf("Fridge", "Main cellar", "Rack A", "Rack B"), app.locations.tree(c).map { it.location.name })
        assertEquals(2, app.search.searchCellar(c, SearchQuery("margaux"), 0, 10, 2026, null).total)
        assertEquals(0, app.cellar.stockDrift())
    }

    @Test
    fun reimportingTheSameFileIsDetected() {
        app.importer.commit(c, app.importer.preview(c, csv, options = options))
        val again = app.importer.preview(c, csv, options = options)
        val kinds = again.assessments.map { it.kind }
        assertEquals(3, kinds.count { it == DuplicateAssessment.Kind.LIKELY_REIMPORT }) // rows with a date or price
        val report = app.importer.commit(c, again)
        assertEquals(3, report.skipped)
        // Krug (no date/price) is added again as a purchase of the same vintage, never a new wine.
        assertEquals(0, report.winesCreated)
        assertEquals(0, report.vintagesCreated)
    }

    @Test
    fun userDecisionsOverrideDefaults() {
        app.importer.commit(c, app.importer.preview(c, csv, options = options))
        val again = app.importer.preview(c, csv, options = options)
        val margauxRow = again.assessments.first { it.kind == DuplicateAssessment.Kind.LIKELY_REIMPORT }.record.row
        val report = app.importer.commit(c, again, decisions = again.assessments.associate { a ->
            a.record.row to if (a.record.row == margauxRow) a.candidates.first().let { ImportDecision.AddToVintage(it.vintageId) } else ImportDecision.Skip
        })
        assertEquals(6, report.bottlesAdded)
    }

    @Test
    fun exportsRoundTripThroughImportMapping() {
        app.importer.commit(c, app.importer.preview(c, csv, options = options))
        val exported = app.exporter.inventoryCsv(c)
        val table = Csv.read(exported)
        assertEquals(4, table.rows.size)
        val fresh = TestApp()
        val preview = fresh.importer.preview(fresh.collectionId, exported, options = options)
        assertEquals(0, preview.errorRows, preview.parse.issues.toString())
        val report = fresh.importer.commit(fresh.collectionId, preview)
        assertEquals(22, report.bottlesAdded)
        assertEquals(listOf("Fridge", "Main cellar", "Rack A", "Rack B"), fresh.locations.tree(fresh.collectionId).map { it.location.name })
    }

    @Test
    fun tastingAndFinancialExports() {
        val v = app.cellar.addWine(c, WineTarget.NewWine(WineInput("Vega Sicilia", "Único")), VintageInput(VintageYear.Year(2010)),
            LotInput(quantity = 3, unitPrice = Money(40000, CurrencyCode.EUR), acquiredOn = IsoDate.of(2020, 1, 1)))
        val lot = app.cellar.stockLines(v).single().lotId
        app.cellar.depart(lot, null, 1, MovementKind.SELL, amount = Money(60000, CurrencyCode.EUR), counterparty = "Auction house")
        val undone = app.cellar.depart(lot, null, 1, MovementKind.SELL, amount = Money(1, CurrencyCode.EUR))
        app.cellar.reverse(undone.id)
        app.journal.add(c, v, TastingInput(IsoDate.of(2026, 1, 1), 97, notes = "Majestic, \"long\" finish"))
        val tastings = Csv.read(app.exporter.tastingsCsv(c))
        assertEquals("Majestic, \"long\" finish", tastings.rows.single()[5])
        val financial = Csv.read(app.exporter.financialCsv(c))
        assertEquals(listOf("purchase", "sell"), financial.rows.map { it[1] }) // reversed sale excluded
        assertEquals("1200.00", financial.rows[0][8])
        assertEquals("600.00", financial.rows[1][8])
    }

    @Test
    fun wishlistConversionCreatesStockAndClosesTheItem() {
        val item = app.wishlist.add(c, WishlistInput(producer = "Domaine Jamet", name = "Côte-Rôtie", vintage = VintageYear.Year(2019),
            targetPrice = Money(9000, CurrencyCode.EUR)))
        val v = app.wishlist.convertToInventory(item, null, null, LotInput(quantity = 2, unitPrice = Money(8500, CurrencyCode.EUR)))
        assertEquals(2, app.cellar.quantityForVintage(v))
        val closed = app.wishlist.item(item)!!
        assertEquals(com.houseofvino.core.model.WishlistStatus.ACQUIRED, closed.status)
        assertEquals(app.cellar.stockLines(v).single().lotId, closed.acquiredLotId)
        assertEquals(VintageYear.Year(2019), app.cellar.vintage(v)!!.vintage)
    }

    @Test
    fun insightsUseExactMoneyAndProvenance() {
        val v = app.cellar.addWine(c, WineTarget.NewWine(WineInput("A")), VintageInput(VintageYear.Year(2015), 2020, 2030, "test"),
            LotInput(quantity = 3, unitPrice = Money(3333, CurrencyCode.EUR)))
        app.cellar.addWine(c, WineTarget.NewWine(WineInput("B")), VintageInput(VintageYear.Year(2018)),
            LotInput(quantity = 2, formatMl = 1500, unitPrice = Money(10000, CurrencyCode.GBP)))
        app.db.write {
            app.db.journal.insertValuation(app.db.newId(), c, v, 750, 5000, "EUR", "Auction results", "2026-09-01", null, now, now)
        }
        val i = app.insights.compute(c, 2026, null, null, IsoDate.of(2026, 10, 17))
        assertEquals(5, i.totals.bottles)
        assertEquals(700L, i.totals.standardEquivalentsCentis) // 3 + 2 magnums = 7 bottle-equivalents
        assertEquals(Money(9999, CurrencyCode.EUR), i.cost.byCurrency[CurrencyCode.EUR])
        assertEquals(Money(20000, CurrencyCode.GBP), i.cost.byCurrency[CurrencyCode.GBP])
        assertEquals(Money(15000, CurrencyCode.EUR), i.value.byCurrency[CurrencyCode.EUR])
        assertEquals("Auction results", i.valueSources.single().source)
        assertEquals(2, i.totals.unvaluedBottles)
        assertEquals(3, i.windows[com.houseofvino.core.search.WindowFilter.READY])
    }
}
