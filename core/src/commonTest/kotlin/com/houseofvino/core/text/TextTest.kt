package com.houseofvino.core.text

import com.houseofvino.core.model.WineColor
import com.houseofvino.core.model.WineStyle
import com.houseofvino.core.search.QueryParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextNormalizerTest {

    @Test
    fun foldsDiacriticsLigaturesAndPunctuation() {
        assertEquals("chateau d yquem", TextNormalizer.normalize("Château d'Yquem"))
        assertEquals("chateau d yquem", TextNormalizer.normalize("CHÂTEAU D’YQUEM"))
        assertEquals("saint emilion grand cru", TextNormalizer.normalize("St-Émilion Grand Cru"))
        assertEquals("coeur de cuvee", TextNormalizer.normalize("Cœur de Cuvée"))
        assertEquals("grosses gewachs", TextNormalizer.normalize("Großes Gewächs"))
        assertEquals("nuits saint georges 1er cru", TextNormalizer.normalize("Nuits-St-Georges 1er Cru"))
        assertEquals("", TextNormalizer.normalize("  -- "))
        assertEquals(listOf("2015"), TextNormalizer.tokens("2015"))
    }

    @Test
    fun handlesDecomposedUnicode() {
        // "é" as e + combining acute accent.
        assertEquals("cuvee", TextNormalizer.normalize("Cuvée"))
    }
}

class FuzzyTest {

    @Test
    fun damerauLevenshtein() {
        assertEquals(0, Fuzzy.distance("margaux", "margaux"))
        assertEquals(1, Fuzzy.distance("margeux", "margaux"))
        assertEquals(1, Fuzzy.distance("magraux", "margaux")) // transposition counts once
        assertEquals(1, Fuzzy.distance("margau", "margaux"))
        assertEquals(1, Fuzzy.distance("chateu", "chateau"))
        assertEquals(3, Fuzzy.distance("kitten", "sitting"))
        assertEquals(3, Fuzzy.distance("", "abc"))
        assertEquals(2, Fuzzy.distance("abcdefgh", "zzzzzzzz", max = 1)) // capped at max+1
    }

    @Test
    fun suggestsNearestFrequentTerms() {
        val vocab = mapOf("margaux" to 40, "margot" to 2, "barolo" to 30, "bardolino" to 3)
        assertEquals("margaux", Fuzzy.suggest("margeaux", vocab).first().term)
        assertEquals("barolo", Fuzzy.suggest("barollo", vocab).first().term)
        assertTrue(Fuzzy.suggest("bar", vocab).isEmpty(), "very short tokens get no typo budget")
        assertTrue(Fuzzy.suggest("xyzzyq", vocab).isEmpty())
    }

    @Test
    fun similarity() {
        assertEquals(100, Fuzzy.similarity("Château Margaux", "Chateau Margaux"))
        assertTrue(Fuzzy.similarity("Chateau Margeaux", "Château Margaux") >= 80)
        assertTrue(Fuzzy.similarity("Domaine Leflaive", "Domaine Leroy") < 80)
        assertEquals(100, Fuzzy.similarity(null, ""))
        assertEquals(0, Fuzzy.similarity("Something", null))
    }
}

class QueryParserTest {

    @Test
    fun extractsFieldsAndKeepsFreeText() {
        val p = QueryParser.parse("""margaux producer:"chateau palmer" vintage:2005-2010 color:rouge grape:merlot unknown:thing""")
        assertEquals("margaux unknown:thing", p.freeText)
        assertEquals(listOf("margaux", "unknown", "thing"), p.tokens)
        assertEquals(setOf("chateau palmer"), p.producers)
        assertEquals(2005, p.vintageFrom)
        assertEquals(2010, p.vintageTo)
        assertEquals(setOf(WineColor.RED), p.colors)
        assertEquals(setOf("merlot"), p.grapes)
    }

    @Test
    fun singleYearNvAndStyle() {
        val p = QueryParser.parse("v:2016 style:champagne")
        assertEquals(2016, p.vintageFrom)
        assertEquals(2016, p.vintageTo)
        assertEquals(setOf(WineStyle.SPARKLING), p.styles)
        assertTrue(QueryParser.parse("vintage:NV krug").nonVintageOnly)
        assertEquals("vintage:abc", QueryParser.parse("vintage:abc").freeText)
    }

    @Test
    fun ftsMatchExpression() {
        assertEquals("chateau* margaux*", QueryParser.toFtsMatch(listOf("chateau", "margaux")))
        assertNull(QueryParser.toFtsMatch(emptyList()))
        assertEquals("ab*", QueryParser.toFtsMatch(listOf("a\"b", "*", "")))
    }
}
