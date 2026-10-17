package com.houseofvino.core.csv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CsvTest {

    @Test
    fun readsQuotedMultilineAndEscapedFields() {
        val text = "﻿Producer,Wine,Notes\r\n" +
            "\"Château Margaux\",\"Pavillon Rouge\",\"Line one\nline two, with comma\"\r\n" +
            "Krug,\"Grande Cuvée \"\"170ème\"\"\",\r\n" +
            "\r\n" +
            ",,\r\n" +
            "Roulot,Meursault,last"
        val t = Csv.read(text)
        assertEquals(listOf("Producer", "Wine", "Notes"), t.header)
        assertEquals(3, t.rows.size)
        assertEquals("Line one\nline two, with comma", t.rows[0][2])
        assertEquals("Grande Cuvée \"170ème\"", t.rows[1][1])
        assertEquals("", t.rows[1][2])
        assertEquals(listOf("Roulot", "Meursault", "last"), t.rows[2])
        assertEquals(listOf(2, 4, 7), t.sourceLines)
    }

    @Test
    fun detectsDelimiters() {
        assertEquals(';', Csv.detectDelimiter("Producer;Wine;Vintage\nA;B;2010"))
        assertEquals('\t', Csv.detectDelimiter("Producer\tWine\tVintage"))
        assertEquals(',', Csv.detectDelimiter("\"a;b\",c,d"))
        assertEquals(listOf("A", "B", "2010"), Csv.read("Producer;Wine;Vintage\nA;B;2010").rows.single())
    }

    @Test
    fun lenientWithStrayQuotesStrictWithBrokenQuoting() {
        assertEquals("5\" magnum", Csv.read("a\n5\" magnum").rows.single()[0])
        assertFailsWith<Csv.ParseException> { Csv.read("a,b\n\"open,b") }
        assertFailsWith<Csv.ParseException> { Csv.read("a,b\n\"x\"y,b") }
    }

    @Test
    fun writesAndRoundTrips() {
        val header = listOf("Producer", "Notes", "Price")
        val rows = listOf(
            listOf("Château \"Le Pin\"", "tasty, rich\nlong", "1200.00"),
            listOf("=HYPERLINK(\"evil\")", " padded ", "-5"),
            listOf(null, "@sum", "+3.5"),
        )
        val out = Csv.write(header, rows.asSequence())
        val back = Csv.read(out)
        assertEquals(header, back.header)
        assertEquals(listOf("Château \"Le Pin\"", "tasty, rich\nlong", "1200.00"), back.rows[0])
        assertEquals("'=HYPERLINK(\"evil\")", back.rows[1][0]) // formula injection neutralised
        assertEquals(" padded ", back.rows[1][1])
        assertEquals("-5", back.rows[1][2]) // plain numbers untouched
        assertEquals(listOf("", "'@sum", "+3.5"), back.rows[2])
    }
}
