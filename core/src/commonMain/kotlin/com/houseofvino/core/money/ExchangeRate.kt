package com.houseofvino.core.money

import com.houseofvino.core.time.IsoDate

/**
 * A user-supplied exchange rate: 1 [base] = [rateE8] / 10^8 [quote]. Rates always carry the
 * date and source they came from; the app never invents them.
 */
data class ExchangeRate(
    val base: CurrencyCode,
    val quote: CurrencyCode,
    val rateE8: Long,
    val asOf: IsoDate,
    val source: String,
) {
    init {
        require(base != quote) { "Rate must be between two different currencies" }
        require(rateE8 > 0) { "Rate must be positive" }
        require(source.isNotBlank()) { "Rate source is required" }
    }

    /** Converts [amount] (in [base]) to [quote], rounding half-even to the quote's minor unit. */
    fun convert(amount: Money): Money {
        require(amount.currency == base) { "Expected ${base.code}, got ${amount.currency.code}" }
        // amount.minor / 10^baseDigits * rate / 10^8 * 10^quoteDigits
        val numeratorScale = quote.minorFactor
        val denominator = Math64.multiplyExact(base.minorFactor, SCALE)
        val scaledRate = Math64.multiplyExact(rateE8, numeratorScale)
        return Money(Math64.mulDivHalfEven(amount.minor, scaledRate, denominator), quote)
    }

    /** The inverse rate (quote → base), rounded half-even to 8 decimal places. */
    fun inverted(): ExchangeRate =
        ExchangeRate(quote, base, Math64.mulDivHalfEven(SCALE, SCALE, rateE8), asOf, source)

    /** Decimal text of the rate, trailing zeros trimmed: "1.0825". */
    fun rateText(): String = Money.formatMinor(rateE8, 8, null, '.').trimEnd('0').trimEnd('.')

    companion object {
        const val SCALE = 100_000_000L

        /** Parses a decimal rate such as "1.0825" (max 8 decimals, '.' or ',' separator). */
        fun parseRate(text: String): Long {
            val minor = DecimalText.parseToMinor(text, 8, decimalSeparator = null, rounding = ExcessPrecision.REJECT)
            require(minor > 0) { "Rate must be positive" }
            return minor
        }
    }
}

/**
 * Converts amounts into a single reporting currency using only the rates the user supplied.
 * Amounts with no applicable rate are reported separately rather than guessed.
 */
class CurrencyConverter(private val rates: List<ExchangeRate>) {

    data class Result(
        val total: Money,
        val ratesUsed: List<ExchangeRate>,
        val unconverted: Map<CurrencyCode, Money>,
    )

    fun rateFor(from: CurrencyCode, to: CurrencyCode): ExchangeRate? {
        if (from == to) return null
        val direct = rates.filter { it.base == from && it.quote == to }.maxByOrNull { it.asOf }
        val inverse = rates.filter { it.base == to && it.quote == from }.maxByOrNull { it.asOf }
        return when {
            direct != null && inverse != null -> if (direct.asOf >= inverse.asOf) direct else inverse.inverted()
            direct != null -> direct
            inverse != null -> inverse.inverted()
            else -> null
        }
    }

    fun totalIn(target: CurrencyCode, amounts: Iterable<Money>): Result {
        var total = Money.zero(target)
        val used = linkedSetOf<ExchangeRate>()
        val unconverted = mutableMapOf<CurrencyCode, Money>()
        for (amount in amounts) {
            if (amount.currency == target) {
                total += amount
                continue
            }
            val rate = rateFor(amount.currency, target)
            if (rate == null) {
                unconverted[amount.currency] = (unconverted[amount.currency] ?: Money.zero(amount.currency)) + amount
            } else {
                total += rate.convert(amount)
                used += rate
            }
        }
        return Result(total, used.toList(), unconverted)
    }
}
