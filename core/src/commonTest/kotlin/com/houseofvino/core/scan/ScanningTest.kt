package com.houseofvino.core.scan

import com.houseofvino.core.csv.ExistingVintage
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.model.WineStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LabelTextParserTest {

    @Test
    fun readsATypicalBordeauxLabel() {
        val lines = listOf(
            OcrLine("GRAND VIN", 95, 30),
            OcrLine("CHÂTEAU MONTROSE", 92, 80),
            OcrLine("Saint-Estèphe", 90, 40),
            OcrLine("APPELLATION SAINT-ESTÈPHE CONTRÔLÉE", 88, 15),
            OcrLine("2016", 97, 50),
            OcrLine("Mis en bouteille au château · Produit de France", 85, 10),
            OcrLine("13,5% vol    750 ml", 90, 10),
        )
        val s = LabelTextParser.parse(lines, currentYear = 2026)
        assertEquals("Château Montrose", s.producer?.value)
        assertEquals(VintageYear.Year(2016), s.vintage?.value)
        assertEquals(Confidence.MEDIUM, s.vintage?.confidence)
        assertEquals(1350, s.abvBasisPoints?.value)
        assertEquals(750, s.formatMl?.value)
        assertEquals("France", s.country?.value)
        assertEquals(Confidence.HIGH, s.country?.confidence)
        assertEquals("SAINT-ESTÈPHE", s.appellation?.value)
    }

    @Test
    fun ignoresFoundingYearsAndFlagsAmbiguity() {
        val s = LabelTextParser.parse(listOf(OcrLine("Maison fondée en 1843", 90, 20), OcrLine("Champagne Brut", 90, 60)), 2026)
        assertNull(s.vintage)
        assertEquals(WineStyle.SPARKLING, s.style?.value)
        val two = LabelTextParser.parse(listOf(OcrLine("2015", 90, 20), OcrLine("2018", 90, 20)), 2026)
        assertEquals(Confidence.LOW, two.vintage?.confidence)
        val future = LabelTextParser.parse(listOf(OcrLine("Cuvée 2099", 90, 20)), 2026)
        assertNull(future.vintage)
    }

    @Test
    fun lowOcrConfidenceLowersFieldConfidence() {
        val s = LabelTextParser.parse(listOf(OcrLine("Domaine Tempier", 40, 60), OcrLine("Bandol Rouge 2019", 45, 30)), 2026)
        assertEquals(Confidence.LOW, s.vintage?.confidence)
        assertEquals(WineColor.RED, s.color?.value)
        assertEquals("Domaine Tempier", s.producer?.value)
    }

    @Test
    fun emptyInputGivesEmptySuggestion() {
        assertTrue(LabelTextParser.parse(emptyList(), 2026).isEmpty)
        assertTrue(LabelTextParser.parse(listOf(OcrLine("   ", null, null)), 2026).isEmpty)
    }
}

class CandidateMatcherTest {
    private val existing = listOf(
        ExistingVintage("v15", "w1", "Château Montrose", null, VintageYear.Year(2015)),
        ExistingVintage("v16", "w1", "Château Montrose", null, VintageYear.Year(2016)),
        ExistingVintage("vx", "w2", "Ridge Vineyards", "Monte Bello", VintageYear.Year(2016)),
    )
    private val mapping = listOf(BarcodeMapping("3760000000001", "w1", "v15", 750))

    @Test
    fun barcodeAloneDoesNotConfirmVintageOrFormat() {
        val s = WineSuggestion(barcode = "3760000000001")
        val matches = CandidateMatcher.match(s, existing, mapping)
        assertEquals(setOf("v15", "v16"), matches.map { it.existing.vintageId }.toSet())
        assertTrue(matches.none { it.vintageConfirmed })
        assertTrue(matches.all { m -> m.warnings.any { "Vintage" in it } })
    }

    @Test
    fun labelVintageSelectsTheRightRecord() {
        val s = WineSuggestion(
            barcode = "3760000000001",
            producer = Suggested("Chateau Montrose", Confidence.MEDIUM, "label"),
            vintage = Suggested(VintageYear.Year(2016), Confidence.MEDIUM, "label"),
            formatMl = Suggested(750, Confidence.HIGH, "label"),
        )
        val matches = CandidateMatcher.match(s, existing, mapping, formatsByVintage = mapOf("v16" to setOf(750)))
        val top = matches.first()
        assertEquals("v16", top.existing.vintageId)
        assertTrue(top.vintageConfirmed)
        assertTrue(top.formatConfirmed)
        val older = matches.first { it.existing.vintageId == "v15" }
        assertTrue(older.warnings.any { "Label says 2016" in it })
        assertFalse(older.vintageConfirmed)
    }

    @Test
    fun producerConflictIsReported() {
        val s = WineSuggestion(barcode = "3760000000001", producer = Suggested("Penfolds", Confidence.MEDIUM, "label"))
        val matches = CandidateMatcher.match(s, existing, mapping)
        assertTrue(matches.all { m -> m.warnings.any { "differs" in it } })
    }

    @Test
    fun noEvidenceNoMatch() {
        assertTrue(CandidateMatcher.match(WineSuggestion(barcode = "999"), existing, mapping).isEmpty())
    }
}
