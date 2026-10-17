package com.houseofvino.data.catalogue

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.model.WineStyle
import com.houseofvino.core.scan.Confidence
import java.io.File
import java.util.zip.GZIPInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogueTest {

    private fun pack(vararg rows: String): LwinCatalogue {
        val header = CataloguePack.TSV_COLUMNS.joinToString("\t")
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CataloguePack.build(driver, CataloguePack.parseTsv(sequenceOf(header, *rows)), mapOf("source" to "test"))
        return LwinCatalogue(driver)
    }

    // lwin, title, producer, wine, country, region, sub_region, site, parcel, colour, type, sub_type, designation, classification, config, first, final
    private fun row(vararg cells: String) = (cells.toList() + List(17 - cells.size) { "" }).joinToString("\t")

    private val small by lazy {
        pack(
            row("1", "", "Cloudy Bay", "Sauvignon Blanc", "New Zealand", "Marlborough", "", "", "", "White", "Wine", "Still"),
            row("2", "", "Cloudy Bay", "Te Koko Sauvignon Blanc", "New Zealand", "Marlborough", "", "", "", "White", "Wine", "Still"),
            row("3", "", "Cloudy Bay", "Pelorus", "New Zealand", "Marlborough", "", "", "", "White", "Wine", "Sparkling"),
            row("4", "Chateau", "Margaux", "", "France", "Bordeaux", "Margaux", "", "", "Red", "Wine", "Still", "AOP", "Premier Cru Classe"),
            row("5", "Chateau", "Margaux", "Pavillon Rouge du Chateau Margaux", "France", "Bordeaux", "Margaux", "", "", "Red", "Wine", "Still", "AOP"),
            row("6", "Chateau", "Palmer", "", "France", "Bordeaux", "Margaux", "", "", "Red", "Wine", "Still", "AOP", "3eme Cru Classe"),
            row("7", "", "Penfolds", "Grange", "Australia", "South Australia", "", "", "", "Red", "Wine", "Still", "", "", "sequential", "1951"),
            row("8", "", "Taylor's", "Vintage Port", "Portugal", "Douro", "", "", "", "Red", "Fortified Wine", "Port"),
        )
    }

    @Test fun producer_and_cuvee_on_the_label_fill_the_wine() {
        val m = small.match("CLOUDY BAY\nSAUVIGNON BLANC\nMARLBOROUGH\n2023\n13.5% vol 750ml")
        assertEquals(1L, m.first().wine.lwin)
        assertEquals(Confidence.MEDIUM, m.first().confidence)
        val s = m.first().wine.toSuggestion(m.first().confidence)
        assertEquals("Cloudy Bay", s.producer?.value)
        assertEquals("Sauvignon Blanc", s.name?.value)
        assertEquals(WineColor.WHITE, s.color?.value)
        assertEquals("Marlborough", s.region?.value)
        assertNull(s.vintage, "vintage never comes from the catalogue")
    }

    @Test fun grand_vin_with_accents_and_appellation_noise() {
        val m = small.match("CHÂTEAU MARGAUX\nPREMIER GRAND CRU CLASSÉ\nAPPELLATION MARGAUX CONTRÔLÉE\n2015\nMIS EN BOUTEILLE AU CHÂTEAU")
        assertEquals(4L, m.first().wine.lwin)
        assertTrue(m.none { it.wine.lwin == 6L }, "Palmer's producer word isn't on the label")
    }

    @Test fun no_producer_means_no_match() {
        assertTrue(small.match("Sauvignon Blanc Marlborough 2022").isEmpty())
        assertTrue(small.match("").isEmpty())
    }

    @Test fun ambiguous_label_is_only_a_low_confidence_hint() {
        val m = small.match("Cloudy Bay")
        assertTrue(m.size >= 2)
        assertTrue(m.all { it.confidence == Confidence.LOW })
    }

    @Test fun vintage_outside_the_wines_range_is_flagged() {
        val m = small.match("Penfolds Grange 1940", year = 1940)
        assertTrue(m.first().warnings.isNotEmpty())
        assertEquals(Confidence.LOW, m.first().confidence)
    }

    @Test fun ocr_typo_in_a_long_word_still_counts() {
        // "GRANCE" is one OCR slip from "Grange".
        val m = small.match("PENFOLDS GRANCE")
        assertEquals(7L, m.first().wine.lwin)
        assertTrue(m.first().reasons.any { it.startsWith("Wine name") })
    }

    @Test fun styles_map_from_lwin_types() {
        assertEquals(WineStyle.FORTIFIED, small.byLwin(8)?.style)
        assertEquals(WineStyle.SPARKLING, small.byLwin(3)?.style)
        assertEquals("Chateau Margaux, Premier Cru Classe", small.byLwin(4)?.displayName)
    }

    /** The real pack: built from the committed LWIN extract, checked against real labels. */
    @Test fun real_lwin_pack() {
        val src = listOf(File("../tools/lwin/lwin.tsv.gz"), File("tools/lwin/lwin.tsv.gz")).firstOrNull { it.exists() } ?: return
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val t0 = System.nanoTime()
        val n = GZIPInputStream(src.inputStream()).bufferedReader().useLines { CataloguePack.build(driver, CataloguePack.parseTsv(it), emptyMap()) }
        println("LWIN pack: $n wines built in ${(System.nanoTime() - t0) / 1_000_000} ms")
        assertTrue(n > 150_000)
        val cat = LwinCatalogue(driver)

        val labels = listOf(
            "CLOUDY BAY\nSAUVIGNON BLANC\nMARLBOROUGH\n2023\n13.5% vol 750ml\nWINE OF NEW ZEALAND" to "Cloudy Bay|Sauvignon Blanc",
            "Penfolds\nBIN 389\nCABERNET SHIRAZ\n2019\nSOUTH AUSTRALIA\n750mL" to "Penfolds|Bin 389 Cabernet Shiraz",
            "CHÂTEAU MARGAUX\nPREMIER GRAND CRU CLASSÉ\nAPPELLATION MARGAUX CONTRÔLÉE\n2015\nMIS EN BOUTEILLE AU CHÂTEAU" to "Chateau Margaux|",
            "FELTON ROAD\nBLOCK 3\nPINOT NOIR\nBANNOCKBURN\nCENTRAL OTAGO\n2021" to "Felton Road|Block 3 Pinot Noir",
            "Penfolds GRANGE\nBIN 95\nSHIRAZ\n2016" to "Penfolds|Grange",
        )
        for ((label, expected) in labels) {
            val t = System.nanoTime()
            val m = cat.match(label, year = Regex("""\b(19|20)\d\d\b""").find(label)?.value?.toInt())
            val ms = (System.nanoTime() - t) / 1_000_000
            println("LABEL ${label.replace("\n", " / ")}  [$ms ms]")
            m.forEach { println("   ${it.score} ${it.confidence} ${it.wine.lwin} ${it.wine.displayName} ${it.warnings}") }
            val (producer, wine) = expected.split("|")
            val top = m.firstOrNull()
            assertEquals(producer, top?.wine?.producer, label)
            if (wine.isNotEmpty()) assertEquals(wine, top?.wine?.wine, label)
            assertTrue(ms < 1500, "match took $ms ms")
        }
    }
}
