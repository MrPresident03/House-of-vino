package com.houseofvino.core.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IsoDateTest {

    @Test
    fun epochDayRoundTripsAcrossCenturies() {
        assertEquals(0L, IsoDate.of(1970, 1, 1).epochDay)
        assertEquals(IsoDate.of(2000, 2, 29), IsoDate.ofEpochDay(IsoDate.of(2000, 2, 29).epochDay))
        var d = IsoDate.of(1899, 12, 25)
        val end = IsoDate.of(2101, 1, 5)
        var prev = d.epochDay - 1
        while (d <= end) {
            assertEquals(prev + 1, d.epochDay)
            assertEquals(d, IsoDate.ofEpochDay(d.epochDay))
            prev = d.epochDay
            d = d.plusDays(1)
        }
    }

    @Test
    fun validatesCalendar() {
        assertFailsWith<IllegalArgumentException> { IsoDate.of(2023, 2, 29) }
        assertFailsWith<IllegalArgumentException> { IsoDate.of(1900, 2, 29) }
        IsoDate.of(2000, 2, 29)
        IsoDate.of(2024, 2, 29)
        assertFailsWith<IllegalArgumentException> { IsoDate.of(2024, 13, 1) }
        assertFailsWith<IllegalArgumentException> { IsoDate.parse("2024-1-01") }
        assertEquals("0999-01-02", IsoDate.of(999, 1, 2).toString())
    }

    @Test
    fun fromEpochMillisHonoursOffset() {
        val millis = 1_760_659_200_000L // 2025-10-17T00:00:00Z
        assertEquals(IsoDate.of(2025, 10, 17), IsoDate.fromEpochMillis(millis))
        assertEquals(IsoDate.of(2025, 10, 16), IsoDate.fromEpochMillis(millis - 1))
        assertEquals(IsoDate.of(2025, 10, 17), IsoDate.fromEpochMillis(millis - 1, utcOffsetMinutes = 13 * 60))
        assertEquals(IsoDate.of(1969, 12, 31), IsoDate.fromEpochMillis(-1))
    }

    @Test
    fun flexibleParserRespectsOrder() {
        assertEquals(IsoDate.of(2021, 3, 4), FlexibleDateParser.parse("2021-03-04", DateOrder.DMY))
        assertEquals(IsoDate.of(2021, 3, 4), FlexibleDateParser.parse("2021-03-04T10:15:00Z", DateOrder.MDY))
        assertEquals(IsoDate.of(2021, 4, 3), FlexibleDateParser.parse("03/04/2021", DateOrder.DMY))
        assertEquals(IsoDate.of(2021, 3, 4), FlexibleDateParser.parse("03/04/2021", DateOrder.MDY))
        assertEquals(IsoDate.of(2021, 4, 3), FlexibleDateParser.parse("3.4.21", DateOrder.DMY))
        assertEquals(IsoDate.of(1999, 4, 3), FlexibleDateParser.parse("3.4.99", DateOrder.DMY))
        assertNull(FlexibleDateParser.parse("31/02/2021", DateOrder.DMY))
        assertNull(FlexibleDateParser.parse("2019", DateOrder.YMD))
        assertNull(FlexibleDateParser.parse("03/04/2021", DateOrder.YMD))
    }

    @Test
    fun ordering() {
        assertTrue(IsoDate.of(2020, 1, 1) < IsoDate.of(2020, 1, 2))
        assertEquals(IsoDate.parse("2020-01-01"), IsoDate.of(2020, 1, 1))
    }
}
