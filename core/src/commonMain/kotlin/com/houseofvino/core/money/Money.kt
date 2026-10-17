package com.houseofvino.core.money

/**
 * An exact monetary amount in minor units of a single currency. No floating point is ever
 * used: amounts are parsed from and formatted to strings digit-by-digit.
 */
data class Money(val minor: Long, val currency: CurrencyCode) : Comparable<Money> {

    val isZero: Boolean get() = minor == 0L
    val isNegative: Boolean get() = minor < 0L

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(Math64.addExact(minor, other.minor), currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(Math64.subtractExact(minor, other.minor), currency)
    }

    operator fun times(quantity: Int): Money = Money(Math64.multiplyExact(minor, quantity.toLong()), currency)

    operator fun unaryMinus(): Money = Money(Math64.negateExact(minor), currency)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minor.compareTo(other.minor)
    }

    /**
     * Splits this amount into [parts] amounts that differ by at most one minor unit and sum
     * exactly to this amount (e.g. a case price split across bottles).
     */
    fun allocate(parts: Int): List<Money> {
        require(parts > 0) { "parts must be positive" }
        val base = minor / parts
        val remainder = minor % parts // same sign as minor
        val step = if (remainder < 0) -1L else 1L
        val extra = kotlin.math.abs(remainder).toInt()
        return List(parts) { i -> Money(base + if (i < extra) step else 0L, currency) }
    }

    /** Plain decimal representation without grouping, e.g. "1234.50", "-0.05", "1500" (JPY). */
    fun toPlainString(): String = formatMinor(minor, currency.minorDigits, groupSeparator = null, decimalSeparator = '.')

    /** Machine-stable string used in exports: "EUR 1234.50". */
    override fun toString(): String = "${currency.code} ${toPlainString()}"

    private fun requireSameCurrency(other: Money) {
        require(other.currency == currency) { "Currency mismatch: ${currency.code} vs ${other.currency.code}" }
    }

    companion object {
        fun zero(currency: CurrencyCode) = Money(0L, currency)

        fun ofMinor(minor: Long, currencyCode: String) = Money(minor, CurrencyCode.of(currencyCode))

        /**
         * Parses a decimal string such as "1234.5", "1,234.50", "1.234,50" or "€ 12".
         *
         * @param decimalSeparator the separator used for decimals in this input; `null` to
         *   infer it (the last of '.' or ',' that is followed by at most [CurrencyCode.minorDigits]
         *   digits, if any).
         * @param rounding how to treat more decimal digits than the currency allows. The default
         *   rejects such input instead of silently rounding.
         */
        fun parse(
            input: String,
            currency: CurrencyCode,
            decimalSeparator: Char? = null,
            rounding: ExcessPrecision = ExcessPrecision.REJECT,
        ): Money = Money(DecimalText.parseToMinor(input, currency.minorDigits, decimalSeparator, rounding), currency)

        fun parseOrNull(
            input: String,
            currency: CurrencyCode,
            decimalSeparator: Char? = null,
            rounding: ExcessPrecision = ExcessPrecision.REJECT,
        ): Money? = try {
            parse(input, currency, decimalSeparator, rounding)
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: ArithmeticException) {
            null
        }

        fun sum(amounts: Iterable<Money>, currency: CurrencyCode): Money =
            amounts.fold(zero(currency)) { acc, m -> acc + m }

        internal fun formatMinor(minor: Long, digits: Int, groupSeparator: Char?, decimalSeparator: Char): String {
            val negative = minor < 0
            // Work on the absolute value as a string to avoid Long.MIN_VALUE overflow.
            val absDigits = minor.toString().removePrefix("-")
            val padded = absDigits.padStart(digits + 1, '0')
            val intPart = padded.substring(0, padded.length - digits)
            val fracPart = padded.substring(padded.length - digits)
            val groupedInt = if (groupSeparator == null) intPart else buildString {
                intPart.forEachIndexed { i, c ->
                    if (i > 0 && (intPart.length - i) % 3 == 0) append(groupSeparator)
                    append(c)
                }
            }
            return buildString {
                if (negative) append('-')
                append(groupedInt)
                if (digits > 0) {
                    append(decimalSeparator)
                    append(fracPart)
                }
            }
        }
    }
}

/** How to treat input with more decimal places than the currency's minor unit. */
enum class ExcessPrecision { REJECT, HALF_EVEN }

/** Locale-like formatting rules supplied by the UI layer (platform locale data). */
data class MoneyFormatStyle(
    val decimalSeparator: Char = '.',
    val groupSeparator: Char? = ',',
    val symbolBefore: Boolean = true,
    val spaceBetween: Boolean = false,
    val symbols: Map<String, String> = DEFAULT_SYMBOLS,
) {
    fun format(money: Money): String {
        val number = Money.formatMinor(money.minor, money.currency.minorDigits, groupSeparator, decimalSeparator)
        val symbol = symbols[money.currency.code] ?: money.currency.code
        val gap = if (spaceBetween || symbol.length > 1 && symbol.all { it.isLetter() }) " " else ""
        val negative = number.startsWith('-')
        val unsigned = number.removePrefix("-")
        val body = if (symbolBefore) "$symbol$gap$unsigned" else "$unsigned$gap$symbol"
        return if (negative) "-$body" else body
    }

    companion object {
        val DEFAULT_SYMBOLS = mapOf(
            "EUR" to "€", "USD" to "$", "GBP" to "£", "JPY" to "¥", "NZD" to "NZ$", "AUD" to "A$",
            "CAD" to "CA$", "CHF" to "CHF", "HKD" to "HK$", "SGD" to "S$", "CNY" to "CN¥",
        )
    }
}

internal object DecimalText {

    fun parseToMinor(input: String, minorDigits: Int, decimalSeparator: Char?, rounding: ExcessPrecision): Long {
        var s = input.trim()
        require(s.isNotEmpty()) { "Empty amount" }
        var negative = false
        if (s.startsWith('(') && s.endsWith(')')) { // accounting negative
            negative = true
            s = s.substring(1, s.length - 1).trim()
        }
        // Strip currency symbols/codes and spaces (including NBSP and thin spaces).
        s = s.filter { it.isDigit() || it == '.' || it == ',' || it == '-' || it == '+' || it == '\'' }
        if (s.startsWith('-')) { negative = !negative; s = s.substring(1) }
        else if (s.startsWith('+')) s = s.substring(1)
        require(s.isNotEmpty() && s.none { it == '-' || it == '+' }) { "Not a number: $input" }

        val sep = decimalSeparator ?: inferDecimalSeparator(s, minorDigits)
        val intRaw: String
        val fracRaw: String
        if (sep != null && s.contains(sep)) {
            val idx = s.lastIndexOf(sep)
            require(s.indexOf(sep) == idx) { "Multiple decimal separators in: $input" }
            intRaw = s.substring(0, idx)
            fracRaw = s.substring(idx + 1)
        } else {
            intRaw = s
            fracRaw = ""
        }
        val groupChars = setOf('.', ',', '\'') - setOfNotNull(sep)
        val intDigits = intRaw.filter { it !in groupChars }
        require(intDigits.all { it.isDigit() } && fracRaw.all { it.isDigit() }) { "Not a number: $input" }
        require(intDigits.isNotEmpty() || fracRaw.isNotEmpty()) { "Not a number: $input" }
        validateGrouping(intRaw, groupChars, input)

        var frac = fracRaw
        var roundUp = false
        if (frac.length > minorDigits) {
            val kept = frac.substring(0, minorDigits)
            val dropped = frac.substring(minorDigits)
            when (rounding) {
                ExcessPrecision.REJECT -> require(dropped.all { it == '0' }) {
                    "Too many decimal places for this currency: $input"
                }
                ExcessPrecision.HALF_EVEN -> {
                    val first = dropped[0]
                    val restNonZero = dropped.substring(1).any { it != '0' }
                    val lastKeptDigit = (intDigits + kept).lastOrNull()?.digitToInt() ?: 0
                    roundUp = first > '5' || (first == '5' && (restNonZero || lastKeptDigit % 2 == 1))
                }
            }
            frac = kept
        }
        frac = frac.padEnd(minorDigits, '0')
        val digits = (intDigits + frac).trimStart('0').ifEmpty { "0" }
        require(digits.length <= 18) { "Amount too large: $input" }
        var value = digits.toLong()
        if (roundUp) value = Math64.addExact(value, 1L)
        return if (negative) -value else value
    }

    /**
     * Infers which of '.' and ',' is the decimal separator. A single separator followed by
     * exactly three digits is ambiguous ("1,234" or "1.234"); for currencies with fewer than
     * three minor digits it is treated as grouping.
     */
    private fun inferDecimalSeparator(s: String, minorDigits: Int): Char? {
        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        if (lastDot < 0 && lastComma < 0) return null
        if (lastDot >= 0 && lastComma >= 0) return if (lastDot > lastComma) '.' else ','
        val sep = if (lastDot >= 0) '.' else ','
        val occurrences = s.count { it == sep }
        val trailing = s.length - s.lastIndexOf(sep) - 1
        if (occurrences > 1) return null // "1.234.567" → grouping only
        if (trailing == 3 && minorDigits < 3) return null // "1,234" → grouping
        return sep
    }

    private fun validateGrouping(intRaw: String, groupChars: Set<Char>, input: String) {
        if (intRaw.none { it in groupChars }) return
        val groups = intRaw.split(*groupChars.toCharArray())
        require(groups.first().isNotEmpty() && groups.first().length <= 3 && groups.drop(1).all { it.length == 3 }) {
            "Invalid digit grouping in: $input"
        }
    }
}
