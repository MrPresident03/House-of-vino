package com.houseofvino.core.money

/**
 * An ISO-4217 currency code with its number of minor-unit digits.
 *
 * Codes not in [KNOWN] are accepted only when the caller states the minor digits explicitly
 * (via [of] with `minorDigits`), so an unknown currency is never silently treated as having
 * two decimals.
 */
class CurrencyCode private constructor(val code: String, val minorDigits: Int) {

    init {
        require(code.length == 3 && code.all { it in 'A'..'Z' }) { "Invalid ISO-4217 code: $code" }
        require(minorDigits in 0..4) { "Unsupported minor digits $minorDigits for $code" }
    }

    /** 10^minorDigits, e.g. 100 for EUR, 1 for JPY, 1000 for KWD. */
    val minorFactor: Long get() = POW10[minorDigits]

    override fun equals(other: Any?): Boolean = other is CurrencyCode && other.code == code
    override fun hashCode(): Int = code.hashCode()
    override fun toString(): String = code

    companion object {
        internal val POW10 = longArrayOf(1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L,
            10_000_000L, 100_000_000L, 1_000_000_000L)

        /** Minor digits per ISO-4217 (subset covering every currency in active use for wine trade). */
        private val KNOWN: Map<String, Int> = buildMap {
            listOf(
                "AED", "AMD", "ARS", "AUD", "AZN", "BDT", "BGN", "BRL", "CAD", "CHF", "CNY", "COP",
                "CZK", "DKK", "EGP", "EUR", "GBP", "GEL", "HKD", "HUF", "IDR", "ILS", "INR", "KES",
                "KZT", "LKR", "MAD", "MDL", "MXN", "MYR", "NGN", "NOK", "NZD", "PEN", "PHP", "PKR",
                "PLN", "QAR", "RON", "RSD", "SAR", "SEK", "SGD", "THB", "TRY", "TWD", "UAH", "USD",
                "UYU", "ZAR",
            ).forEach { put(it, 2) }
            // Zero-decimal currencies.
            listOf("CLP", "GNF", "ISK", "JPY", "KMF", "KRW", "PYG", "RWF", "UGX", "VND", "XAF", "XOF", "XPF")
                .forEach { put(it, 0) }
            // Three-decimal currencies.
            listOf("BHD", "IQD", "JOD", "KWD", "LYD", "OMR", "TND").forEach { put(it, 3) }
        }

        val EUR = of("EUR")
        val USD = of("USD")
        val GBP = of("GBP")
        val NZD = of("NZD")
        val AUD = of("AUD")
        val JPY = of("JPY")

        /** Currency codes the app offers in pickers, sorted. */
        val supported: List<CurrencyCode> by lazy { KNOWN.keys.sorted().map { CurrencyCode(it, KNOWN.getValue(it)) } }

        fun isKnown(code: String): Boolean = code.uppercase() in KNOWN

        /** Returns the known currency for [code] (case-insensitive) or throws. */
        fun of(code: String): CurrencyCode {
            val upper = code.trim().uppercase()
            val digits = KNOWN[upper] ?: throw IllegalArgumentException("Unknown currency code: $code")
            return CurrencyCode(upper, digits)
        }

        /** Returns the known currency for [code], or `null` if it is not a known ISO-4217 code. */
        fun ofOrNull(code: String?): CurrencyCode? {
            if (code == null) return null
            val upper = code.trim().uppercase()
            val digits = KNOWN[upper] ?: return null
            return CurrencyCode(upper, digits)
        }

        /** Declares a currency not in the built-in table, with explicit minor digits. */
        fun of(code: String, minorDigits: Int): CurrencyCode {
            val upper = code.trim().uppercase()
            KNOWN[upper]?.let { known ->
                require(known == minorDigits) { "$upper has $known minor digits, not $minorDigits" }
            }
            return CurrencyCode(upper, minorDigits)
        }
    }
}
