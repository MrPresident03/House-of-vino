package com.houseofvino.core.model

import com.houseofvino.core.csv.ImportParser
import com.houseofvino.core.text.TextNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GrapeVarietiesTest {

    private val forbidden = ",;/|+%()".toSet()

    @Test
    fun listIsLargeAndSorted() {
        val all = GrapeVarieties.all
        assertTrue(all.size >= 350, "only ${all.size} varieties")
        val keys = all.map { TextNormalizer.normalize(it.name) }
        assertEquals(keys.sorted(), keys)
        assertTrue(all.all { it.color == WineColor.RED || it.color == WineColor.WHITE })
    }

    @Test
    fun namesAndSynonymsAreUnique() {
        val seen = HashMap<String, String>()
        for (v in GrapeVarieties.all) {
            for (text in listOf(v.name) + v.synonyms) {
                val key = TextNormalizer.normalize(text)
                assertTrue(key.isNotEmpty(), "blank name for ${v.name}")
                val clash = seen.put(key, text)
                assertNull(clash, "\"$text\" duplicates \"$clash\"")
            }
        }
    }

    @Test
    fun noParserCharactersInNamesOrSynonyms() {
        for (v in GrapeVarieties.all) {
            for (text in listOf(v.name) + v.synonyms) {
                assertTrue(text.none { it in forbidden }, "\"$text\" contains a reserved character")
                assertEquals(text.trim(), text)
            }
        }
    }

    @Test
    fun commonEntriesAreInAll() {
        assertTrue(GrapeVarieties.common.size >= 25)
        assertTrue(GrapeVarieties.all.containsAll(GrapeVarieties.common))
    }

    @Test
    fun findMatchesNamesAndSynonymsIgnoringCaseAndAccents() {
        assertEquals("Syrah", GrapeVarieties.find("shiraz")?.name)
        assertEquals("Pinot Gris", GrapeVarieties.find("PINOT GRIGIO")?.name)
        assertEquals("Mourvèdre", GrapeVarieties.find("monastrell")?.name)
        assertEquals("Mourvèdre", GrapeVarieties.find("Mourvedre")?.name)
        assertEquals("Tempranillo", GrapeVarieties.find("Tinta Roriz")?.name)
        assertEquals("Zinfandel", GrapeVarieties.find("Primitivo")?.name)
        assertNull(GrapeVarieties.find("Not a grape"))
        assertNull(GrapeVarieties.find("  "))
    }

    @Test
    fun searchRanksNamePrefixesFirst() {
        val pinot = GrapeVarieties.search("pinot")
        assertTrue(pinot.isNotEmpty())
        val prefixed = pinot.takeWhile { it.name.startsWith("Pinot") }
        assertTrue(prefixed.size >= 4)
        assertTrue(pinot.drop(prefixed.size).none { it.name.startsWith("Pinot") })
        assertTrue(GrapeVarieties.search("gruner").any { it.name == "Grüner Veltliner" })
        assertEquals("Syrah", GrapeVarieties.search("shiraz").first().name)
        assertEquals(GrapeVarieties.all, GrapeVarieties.search(""))
        assertTrue(GrapeVarieties.search("pinot", limit = 2).size == 2)
        assertTrue(GrapeVarieties.search("zzzz").isEmpty())
    }

    @Test
    fun everyNameRoundTripsThroughTheGrapeParser() {
        for (v in GrapeVarieties.all) {
            val parsed = ImportParser.parseGrapes(v.name)
            assertEquals(1, parsed.size, v.name)
            assertEquals(v.name, parsed.single().grape)
            assertNull(parsed.single().percent, v.name)
        }
        assertNotNull(GrapeVarieties.find("Syrah"))
    }
}
