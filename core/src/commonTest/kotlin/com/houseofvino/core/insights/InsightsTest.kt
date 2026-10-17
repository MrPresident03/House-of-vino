package com.houseofvino.core.insights

import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.CurrencyConverter
import com.houseofvino.core.money.ExchangeRate
import com.houseofvino.core.money.Money
import com.houseofvino.core.search.WindowFilter
import com.houseofvino.core.time.IsoDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InsightsTest {

    @Test
    fun drinkingWindowStatus() {
        assertEquals(WindowFilter.UNKNOWN, DrinkingWindow.status(null, null, 2026))
        assertEquals(WindowFilter.HOLD, DrinkingWindow.status(2030, 2050, 2026))
        assertEquals(WindowFilter.READY, DrinkingWindow.status(2020, 2030, 2026))
        assertEquals(WindowFilter.READY, DrinkingWindow.status(null, 2030, 2026))
        assertEquals(WindowFilter.READY, DrinkingWindow.status(2020, null, 2026))
        assertEquals(WindowFilter.DRINK_SOON, DrinkingWindow.status(2020, 2026, 2026))
        assertEquals(WindowFilter.PAST, DrinkingWindow.status(2010, 2020, 2026))
    }

    @Test
    fun costIsExactPerCurrencyAndConvertedOnlyWithRates() {
        val eur = CurrencyCode.EUR
        val gbp = CurrencyCode.GBP
        val lines = listOf(CostLine(Money(4999, eur), 6), CostLine(Money(12000, gbp), 2), CostLine(Money(1, eur), 3))
        val noRates = InsightMath.cost(lines, reporting = null, converter = null)
        assertEquals(Money(29997, eur), noRates.byCurrency[eur])
        assertEquals(Money(24000, gbp), noRates.byCurrency[gbp])
        assertEquals(11, noRates.bottlesCounted)
        assertNull(noRates.converted)

        val converter = CurrencyConverter(listOf(ExchangeRate(gbp, eur, ExchangeRate.parseRate("1.15"), IsoDate.of(2026, 10, 1), "manual")))
        val withRate = InsightMath.cost(lines, eur, converter)
        assertEquals(Money(29997 + 27600, eur), withRate.converted!!.total)
        assertEquals(emptyMap(), withRate.converted!!.unconverted)
    }

    @Test
    fun breakdownAndHorizon() {
        assertEquals(listOf("Red" to 750, "White" to 250), InsightMath.breakdown(mapOf("White" to 1, "Red" to 3)))
        assertEquals(emptyList(), InsightMath.breakdown(emptyMap<String, Int>()))
        val horizon = InsightMath.horizon(listOf(Triple(2027, 2030, 6), Triple(null, 2026, 2)), fromYear = 2026, years = 3)
        assertEquals(mapOf(WindowFilter.HOLD to 6, WindowFilter.DRINK_SOON to 2), horizon[0])
        assertEquals(mapOf(WindowFilter.READY to 6, WindowFilter.PAST to 2), horizon[1])
    }
}
