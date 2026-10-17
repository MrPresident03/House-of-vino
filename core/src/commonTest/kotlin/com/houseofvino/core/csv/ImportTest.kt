package com.houseofvino.core.csv

import com.houseofvino.core.model.GrapeShare
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.model.WineStyle
import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.Money
import com.houseofvino.core.time.DateOrder
import com.houseofvino.core.time.IsoDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImportTest {

    private val cellarTrackerHeader = listOf(
        "iWine", "Type", "Vintage", "Wine", "Producer", "Varietal", "Country", "Region", "SubRegion", "Appellation",
        "Size", "Quantity", "Price", "Currency", "Location", "Bin", "BeginConsume", "EndConsume", "Purchase Date",
    )

    @Test
    fun suggestsMappingForCellarTrackerExport() {
        val m = ColumnMapping.suggest(cellarTrackerHeader)
        assertEquals(4, m[ImportField.PRODUCER])
        assertEquals(3, m[ImportField.WINE_NAME])
        assertEquals(2, m[ImportField.VINTAGE])
        assertEquals(1, m[ImportField.COLOR])
        assertEquals(5, m[ImportField.GRAPES])
        assertEquals(10, m[ImportField.FORMAT])
        assertEquals(11, m[ImportField.QUANTITY])
        assertEquals(12, m[ImportField.UNIT_PRICE])
        assertEquals(15, m[ImportField.BIN])
        assertEquals(16, m[ImportField.DRINK_FROM])
        assertEquals(17, m[ImportField.DRINK_UNTIL])
        assertEquals(18, m[ImportField.PURCHASE_DATE])
        assertTrue(m.missingRequired().isEmpty())
    }

    @Test
    fun suggestsMappingFromLooseHeaders() {
        val m = ColumnMapping.suggest(listOf("Winery", "Cuvée", "Year", "Purchase date (UTC)", "Qty", "Merchant"))
        assertEquals(0, m[ImportField.PRODUCER])
        assertEquals(1, m[ImportField.WINE_NAME])
        assertEquals(2, m[ImportField.VINTAGE])
        assertEquals(3, m[ImportField.PURCHASE_DATE])
        assertEquals(4, m[ImportField.QUANTITY])
        assertEquals(5, m[ImportField.SOURCE])
    }

    @Test
    fun parsesValidRowsAndReportsErrorsPerRow() {
        val csv = """
            Producer,Wine,Vintage,Type,Varietal,Size,Quantity,Price,Currency,Purchase Date,Location,BeginConsume,EndConsume
            Château Margaux,,2015,Red,"Cabernet Sauvignon 87%, Merlot 8%",750ml,6,"1,050.00",EUR,2018-05-01,Main cellar/Rack A,2030,2060
            Krug,Grande Cuvée,NV,White - Sparkling,,Magnum,1,,GBP,,,,
            ,Missing producer,2010,Red,,,1,,,,,,
            Roulot,Meursault,20X5,White,,1.5L,two,12.345,XYZ,31/31/2020,,2030,2020
        """.trimIndent()
        val table = Csv.read(csv)
        val parser = ImportParser(ColumnMapping.suggest(table.header), ImportOptions(defaultCurrency = CurrencyCode.NZD))
        val result = parser.parse(table)

        assertEquals(2, result.records.size)
        val margaux = result.records[0]
        assertEquals("Château Margaux", margaux.producer)
        assertNull(margaux.name)
        assertEquals(VintageYear.Year(2015), margaux.vintage)
        assertEquals(WineColor.RED, margaux.color)
        assertEquals(listOf(GrapeShare("Cabernet Sauvignon", 87), GrapeShare("Merlot", 8)), margaux.grapes)
        assertEquals(750, margaux.formatMl)
        assertEquals(6, margaux.quantity)
        assertEquals(Money(105000, CurrencyCode.EUR), margaux.unitPrice)
        assertEquals(IsoDate.of(2018, 5, 1), margaux.acquiredOn)
        assertEquals(listOf("Main cellar", "Rack A"), margaux.locationPath)
        assertEquals(2030, margaux.drinkFrom)

        val krug = result.records[1]
        assertEquals(VintageYear.NonVintage, krug.vintage)
        assertEquals(WineColor.WHITE, krug.color)
        assertEquals(WineStyle.SPARKLING, krug.style)
        assertEquals(1500, krug.formatMl)
        assertNull(krug.unitPrice)

        assertEquals(setOf(4, 5), result.errorRows)
        val row5 = result.issues.filter { it.row == 5 }.map { it.field }.toSet()
        assertTrue(ImportField.VINTAGE in row5)
        assertTrue(ImportField.QUANTITY in row5)
        assertTrue(ImportField.CURRENCY in row5)
        assertTrue(ImportField.PURCHASE_DATE in row5)
        assertTrue(ImportField.DRINK_UNTIL in row5)
    }

    @Test
    fun totalPriceIsSplitAcrossBottles() {
        val table = Csv.read("Producer,Quantity,Total\nA,3,100.00\nB,4,100.00")
        val result = ImportParser(ColumnMapping.suggest(table.header), ImportOptions(CurrencyCode.EUR)).parse(table)
        assertEquals(Money(3333, CurrencyCode.EUR), result.records[0].unitPrice)
        assertEquals(Money(2500, CurrencyCode.EUR), result.records[1].unitPrice)
        assertTrue(result.issues.any { it.row == 2 && it.severity == Severity.WARNING })
    }

    @Test
    fun missingRequiredMappingBlocksImport() {
        val table = Csv.read("Wine,Vintage\nX,2010")
        val result = ImportParser(ColumnMapping.suggest(table.header), ImportOptions(CurrencyCode.EUR)).parse(table)
        assertTrue(result.records.isEmpty())
        assertEquals(ImportField.PRODUCER, result.issues.single().field)
    }

    @Test
    fun usesDayFirstDatesWhenChosen() {
        val table = Csv.read("Producer,Purchase date\nA,03/04/2021")
        val result = ImportParser(ColumnMapping.suggest(table.header), ImportOptions(CurrencyCode.EUR, dateOrder = DateOrder.DMY)).parse(table)
        assertEquals(IsoDate.of(2021, 4, 3), result.records.single().acquiredOn)
    }

    @Test
    fun formatParsing() {
        assertEquals(750, ImportParser.parseFormat("750"))
        assertEquals(750, ImportParser.parseFormat("75"))
        assertEquals(750, ImportParser.parseFormat("75cl"))
        assertEquals(1500, ImportParser.parseFormat("1.5"))
        assertEquals(1500, ImportParser.parseFormat("1,5 L"))
        assertEquals(1500, ImportParser.parseFormat("Magnum"))
        assertEquals(375, ImportParser.parseFormat("Half bottle"))
        assertEquals(375, ImportParser.parseFormat("375ml"))
        assertEquals(6000, ImportParser.parseFormat("Imperial"))
        assertNull(ImportParser.parseFormat("huge"))
        assertNull(ImportParser.parseFormat("5"))
    }

    @Test
    fun grapeParsing() {
        assertEquals(listOf(GrapeShare("Syrah", null), GrapeShare("Grenache", null)), ImportParser.parseGrapes("Syrah; Grenache"))
        assertEquals(listOf(GrapeShare("Merlot", 85), GrapeShare("Cabernet Franc", 15)), ImportParser.parseGrapes("Merlot (85%), Cabernet Franc (15%)"))
        assertEquals(listOf(GrapeShare("Nebbiolo", 100)), ImportParser.parseGrapes("100% Nebbiolo"))
        assertEquals(listOf(GrapeShare("Pinot Noir", null)), ImportParser.parseGrapes("Pinot Noir, pinot noir"))
    }
}

class DuplicateDetectorTest {
    private val existing = listOf(
        ExistingVintage("v-margaux-2015", "w-margaux", "Château Margaux", null, VintageYear.Year(2015)),
        ExistingVintage("v-palmer-2010", "w-palmer", "Château Palmer", null, VintageYear.Year(2010)),
        ExistingVintage("v-leflaive-2018", "w-leflaive", "Domaine Leflaive", "Puligny-Montrachet Les Pucelles", VintageYear.Year(2018)),
    )
    private val lots = listOf(
        ExistingLot("lot-1", "v-palmer-2010", 750, IsoDate.of(2015, 3, 1), Money(30000, CurrencyCode.EUR), "Merchant X", 6),
    )

    private fun record(row: Int, producer: String, name: String?, vintage: VintageYear, qty: Int = 1,
                       date: IsoDate? = null, price: Money? = null, source: String? = null) = ImportRecord(
        row, producer, name, vintage, null, null, null, null, null, null, null, emptyList(), 750, qty, date, price,
        source, emptyList(), null, null, null, emptyList(), null, null,
    )

    @Test
    fun classifiesEveryCase() {
        val records = listOf(
            record(2, "CHATEAU MARGAUX", null, VintageYear.Year(2015)),
            record(3, "Château Margaux", null, VintageYear.Year(2016)),
            record(4, "Domaine Leflaive", "Puligny Montrachet Les Pucelle", VintageYear.Year(2018)),
            record(5, "Château Palmer", null, VintageYear.Year(2010), 6, IsoDate.of(2015, 3, 1), Money(30000, CurrencyCode.EUR), "Merchant X"),
            record(6, "Some New Winery", "Red", VintageYear.Year(2020)),
            record(7, "Some New Winery", "Red", VintageYear.Year(2020)),
        )
        val out = DuplicateDetector(existing, lots).assess(records)

        assertEquals(DuplicateAssessment.Kind.SAME_VINTAGE, out[0].kind)
        assertEquals(ImportDecision.AddToVintage("v-margaux-2015"), out[0].defaultDecision)

        // Different vintage of the same wine: attaches to the wine, never to the 2015 record.
        assertEquals(DuplicateAssessment.Kind.OTHER_VINTAGE_OF_WINE, out[1].kind)
        assertEquals(ImportDecision.AddVintageToWine("w-margaux"), out[1].defaultDecision)

        // Typo in the name: flagged for review, not merged by default.
        assertEquals(DuplicateAssessment.Kind.POSSIBLE_DUPLICATE, out[2].kind)
        assertIs<ImportDecision.CreateNew>(out[2].defaultDecision)
        assertTrue(out[2].needsReview)

        assertEquals(DuplicateAssessment.Kind.LIKELY_REIMPORT, out[3].kind)
        assertEquals(ImportDecision.Skip, out[3].defaultDecision)

        assertEquals(DuplicateAssessment.Kind.NEW, out[4].kind)
        assertEquals(DuplicateAssessment.Kind.DUPLICATE_IN_FILE, out[5].kind)
        assertTrue(out[5].needsReview)
    }

    @Test
    fun fuzzyMatchNeverCrossesVintages() {
        val out = DuplicateDetector(existing, emptyList()).assess(listOf(record(2, "Chateau Margeaux", null, VintageYear.Year(2014))))
        // Flagged as maybe the same *wine*, but never pre-merged into the 2015 vintage.
        assertEquals(DuplicateAssessment.Kind.POSSIBLE_SAME_WINE, out.single().kind)
        assertEquals(ImportDecision.CreateNew, out.single().defaultDecision)
        assertTrue(out.single().needsReview)
        val unrelated = DuplicateDetector(existing, emptyList()).assess(listOf(record(2, "Ridge", "Monte Bello", VintageYear.Year(2014))))
        assertEquals(DuplicateAssessment.Kind.NEW, unrelated.single().kind)
    }
}
