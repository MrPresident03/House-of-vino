package com.houseofvino.core.money

import com.houseofvino.core.time.IsoDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoneyTest {
    private val eur = CurrencyCode.EUR
    private val jpy = CurrencyCode.JPY
    private val kwd = CurrencyCode.of("KWD")

    @Test
    fun parsesCommonFormats() {
        assertEquals(123450L, Money.parse("1234.50", eur).minor)
        assertEquals(123450L, Money.parse("1,234.50", eur).minor)
        assertEquals(123450L, Money.parse("1.234,50", eur).minor)
        assertEquals(123450L, Money.parse("€ 1 234,50", eur).minor)
        assertEquals(123400L, Money.parse("1,234", eur).minor) // three trailing digits → grouping
        assertEquals(1200L, Money.parse("12", eur).minor)
        assertEquals(1250L, Money.parse("12.5", eur).minor)
        assertEquals(50L, Money.parse(".50", eur).minor)
        assertEquals(-1999L, Money.parse("-19.99", eur).minor)
        assertEquals(-1999L, Money.parse("(19.99)", eur).minor)
        assertEquals(1500L, Money.parse("¥1,500", jpy).minor)
        assertEquals(1234L, Money.parse("1.234", kwd).minor) // KWD has 3 decimals
        assertEquals(123456789L, Money.parse("1'234'567.89", eur).minor)
    }

    @Test
    fun explicitDecimalSeparatorWins() {
        assertEquals(123400L, Money.parse("1.234", eur, decimalSeparator = ',').minor)
        assertEquals(123L, Money.parse("1,23", eur, decimalSeparator = ',').minor)
    }

    @Test
    fun rejectsExcessPrecisionByDefault() {
        assertFailsWith<IllegalArgumentException> { Money.parse("12.345", eur, decimalSeparator = '.') }
        assertEquals(1230L, Money.parse("12.300", eur, decimalSeparator = '.').minor) // trailing zeros are fine
        assertFailsWith<IllegalArgumentException> { Money.parse("1.5", jpy) }
    }

    @Test
    fun halfEvenRoundingWhenRequested() {
        fun r(s: String) = Money.parse(s, eur, '.', ExcessPrecision.HALF_EVEN).minor
        assertEquals(1234L, r("12.345")) // 4 is even → stays
        assertEquals(1236L, r("12.355")) // 5 is odd → up
        assertEquals(1235L, r("12.3451")) // above half → up
        assertEquals(1234L, r("12.3449"))
        assertEquals(-1234L, r("-12.345"))
    }

    @Test
    fun rejectsGarbage() {
        listOf("", "abc", "1.2.3,4", "12-3", "1,2,3", "12,34,567.00", "--5").forEach {
            assertNull(Money.parseOrNull(it, eur), "should reject \"$it\"")
        }
    }

    @Test
    fun formatsPlainAndStyled() {
        assertEquals("1234.50", Money(123450, eur).toPlainString())
        assertEquals("-0.05", Money(-5, eur).toPlainString())
        assertEquals("1500", Money(1500, jpy).toPlainString())
        assertEquals("0.001", Money(1, kwd).toPlainString())
        assertEquals("€1,234.50", MoneyFormatStyle().format(Money(123450, eur)))
        assertEquals("-€0.05", MoneyFormatStyle().format(Money(-5, eur)))
        val de = MoneyFormatStyle(decimalSeparator = ',', groupSeparator = '.', symbolBefore = false, spaceBetween = true)
        assertEquals("1.234.567,00 €", de.format(Money(123456700, eur)))
        assertEquals("NZD 10.00", Money(1000, CurrencyCode.NZD).toString())
        assertEquals("CHF 5.00", MoneyFormatStyle().format(Money(500, CurrencyCode.of("CHF"))))
    }

    @Test
    fun formatsExtremeValuesWithoutOverflow() {
        assertEquals("-92233720368547758.08", Money(Long.MIN_VALUE, eur).toPlainString())
    }

    @Test
    fun arithmeticIsExactAndCurrencySafe() {
        val a = Money(1999, eur)
        assertEquals(Money(5997, eur), a * 3)
        assertEquals(Money(0, eur), a - a)
        assertFailsWith<IllegalArgumentException> { a + Money(1, CurrencyCode.USD) }
        assertFailsWith<ArithmeticException> { Money(Long.MAX_VALUE, eur) + Money(1, eur) }
        assertFailsWith<ArithmeticException> { Money(Long.MAX_VALUE / 2 + 1, eur) * 2 }
    }

    @Test
    fun allocationSumsExactly() {
        val parts = Money(10000, eur).allocate(3)
        assertEquals(listOf(3334L, 3333L, 3333L), parts.map { it.minor })
        assertEquals(Money(10000, eur), Money.sum(parts, eur))
        val negative = Money(-100, eur).allocate(3)
        assertEquals(-100L, negative.sumOf { it.minor })
        assertTrue(negative.all { it.minor in -34L..-33L })
    }

    @Test
    fun unknownCurrencyIsRejected() {
        assertFailsWith<IllegalArgumentException> { CurrencyCode.of("XYZ") }
        assertNull(CurrencyCode.ofOrNull("eurx"))
        assertEquals(2, CurrencyCode.of("XYZ", 2).minorDigits)
        assertFailsWith<IllegalArgumentException> { CurrencyCode.of("JPY", 2) }
        assertEquals(CurrencyCode.EUR, CurrencyCode.of(" eur "))
    }
}

class ExchangeRateTest {
    private val date = IsoDate.of(2026, 10, 1)
    private val eur = CurrencyCode.EUR
    private val usd = CurrencyCode.USD
    private val jpy = CurrencyCode.JPY

    @Test
    fun convertsWithHalfEven() {
        val rate = ExchangeRate(eur, usd, ExchangeRate.parseRate("1.0825"), date, "ECB")
        assertEquals(Money(10825, usd), rate.convert(Money(10000, eur)))
        // 0.10 EUR * 1.0825 = 0.10825 → 0.11 (above half)
        assertEquals(Money(11, usd), rate.convert(Money(10, eur)))
        // 2 EUR * 1.0025 = 2.005 → 2.00 (half, 0 is even)
        val r2 = ExchangeRate(eur, usd, ExchangeRate.parseRate("1.0025"), date, "test")
        assertEquals(Money(200, usd), r2.convert(Money(200, eur)))
        // 6 EUR * 1.0025 = 6.015 → 6.02 (half, 1 is odd → up)
        assertEquals(Money(602, usd), r2.convert(Money(600, eur)))
    }

    @Test
    fun handlesDifferentMinorUnits() {
        val rate = ExchangeRate(eur, jpy, ExchangeRate.parseRate("162.5"), date, "manual")
        assertEquals(Money(16250, jpy), rate.convert(Money(10000, eur)))
        val inverse = rate.inverted()
        assertEquals(Money(10000, eur), inverse.convert(Money(16250, jpy)))
    }

    @Test
    fun largeAmountsUse128BitIntermediate() {
        val rate = ExchangeRate(eur, usd, ExchangeRate.parseRate("1.23456789"), date, "manual")
        // 9e15 minor units * 123456789 overflows Long in the intermediate product.
        val big = Money(9_000_000_000_000_000L, eur)
        assertEquals(Money(11_111_111_010_000_000L, usd), rate.convert(big))
    }

    @Test
    fun converterReportsWhatItCannotConvert() {
        val conv = CurrencyConverter(listOf(ExchangeRate(usd, eur, ExchangeRate.parseRate("0.92"), date, "manual")))
        val result = conv.totalIn(eur, listOf(Money(1000, eur), Money(1000, usd), Money(500, CurrencyCode.GBP)))
        assertEquals(Money(1920, eur), result.total)
        assertEquals(1, result.ratesUsed.size)
        assertEquals(mapOf(CurrencyCode.GBP to Money(500, CurrencyCode.GBP)), result.unconverted)
    }

    @Test
    fun usesInverseOfNewestRate() {
        val old = ExchangeRate(eur, usd, ExchangeRate.parseRate("1.50"), IsoDate.of(2020, 1, 1), "old")
        val newer = ExchangeRate(usd, eur, ExchangeRate.parseRate("0.80"), IsoDate.of(2026, 1, 1), "new")
        val rate = CurrencyConverter(listOf(old, newer)).rateFor(eur, usd)!!
        assertEquals("1.25", rate.rateText())
        assertEquals("new", rate.source)
    }

    @Test
    fun rejectsInvalidRates() {
        assertFailsWith<IllegalArgumentException> { ExchangeRate(eur, eur, 100, date, "x") }
        assertFailsWith<IllegalArgumentException> { ExchangeRate(eur, usd, 0, date, "x") }
        assertFailsWith<IllegalArgumentException> { ExchangeRate(eur, usd, 100, date, " ") }
        assertFailsWith<IllegalArgumentException> { ExchangeRate.parseRate("1.123456789") }
    }

    @Test
    fun mulDivMatchesReferenceOnRandomInputs() {
        val rnd = kotlin.random.Random(42)
        repeat(2000) {
            val a = rnd.nextLong(-1_000_000_000_000L, 1_000_000_000_000L)
            val b = rnd.nextLong(1, 1_000_000L)
            val d = rnd.nextLong(1, 1_000_000L)
            val expected = referenceHalfEven(a, b, d)
            assertEquals(expected, Math64.mulDivHalfEven(a, b, d), "a=$a b=$b d=$d")
        }
    }

    private fun referenceHalfEven(a: Long, b: Long, d: Long): Long {
        // a*b fits in Long for these ranges (|a·b| < 1e18).
        val p = a * b
        val neg = p < 0
        val ap = if (neg) -p else p
        var q = ap / d
        val r = ap % d
        if (r * 2 > d || (r * 2 == d && q % 2 == 1L)) q++
        return if (neg) -q else q
    }
}
