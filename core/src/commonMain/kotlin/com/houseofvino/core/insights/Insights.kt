package com.houseofvino.core.insights

import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.CurrencyConverter
import com.houseofvino.core.money.Money
import com.houseofvino.core.search.WindowFilter
import com.houseofvino.core.time.IsoDate

/** Drinking-window status for a vintage in a given year. */
object DrinkingWindow {

    fun status(drinkFrom: Int?, drinkUntil: Int?, currentYear: Int): WindowFilter = when {
        drinkFrom == null && drinkUntil == null -> WindowFilter.UNKNOWN
        drinkFrom != null && currentYear < drinkFrom -> WindowFilter.HOLD
        drinkUntil != null && currentYear > drinkUntil -> WindowFilter.PAST
        drinkUntil != null && currentYear == drinkUntil -> WindowFilter.DRINK_SOON
        else -> WindowFilter.READY
    }

    fun label(status: WindowFilter): String = when (status) {
        WindowFilter.HOLD -> "Hold"
        WindowFilter.READY -> "Ready"
        WindowFilter.DRINK_SOON -> "Drink soon"
        WindowFilter.PAST -> "Past window"
        WindowFilter.UNKNOWN -> "No window"
    }
}

/** Aggregate inputs produced by SQL in the data layer; the domain does the money maths. */
data class CostLine(val unitPrice: Money, val bottles: Int)
data class ValueLine(val amount: Money, val bottles: Int, val source: String, val asOf: IsoDate)

data class MoneySummary(
    /** Exact totals per currency — always shown. */
    val byCurrency: Map<CurrencyCode, Money>,
    /** Total in the reporting currency, only for amounts with a user-supplied rate. */
    val converted: CurrencyConverter.Result?,
    /** Bottles that contributed (priced/valued bottles). */
    val bottlesCounted: Int,
)

object InsightMath {

    fun cost(lines: List<CostLine>, reporting: CurrencyCode?, converter: CurrencyConverter?): MoneySummary {
        val totals = lines.map { it.unitPrice * it.bottles }
        return summarise(totals, lines.sumOf { it.bottles }, reporting, converter)
    }

    fun value(lines: List<ValueLine>, reporting: CurrencyCode?, converter: CurrencyConverter?): MoneySummary {
        val totals = lines.map { it.amount * it.bottles }
        return summarise(totals, lines.sumOf { it.bottles }, reporting, converter)
    }

    private fun summarise(totals: List<Money>, bottles: Int, reporting: CurrencyCode?, converter: CurrencyConverter?): MoneySummary {
        val byCurrency = totals.groupBy { it.currency }.mapValues { (c, list) -> Money.sum(list, c) }
        val converted = if (reporting != null && converter != null && byCurrency.isNotEmpty()) {
            converter.totalIn(reporting, byCurrency.values)
        } else null
        return MoneySummary(byCurrency, converted, bottles)
    }

    /** Share of each bucket in 0..1000 (per-mille) so bars can be drawn without floating maths in the domain. */
    fun <K> breakdown(counts: Map<K, Int>): List<Pair<K, Int>> {
        val total = counts.values.sum()
        if (total == 0) return emptyList()
        return counts.entries.sortedByDescending { it.value }.map { it.key to (it.value * 1000L / total).toInt() }
    }

    /**
     * Bottles per year in each drinking-window status for the next [years] years, assuming no
     * consumption — the "drinking horizon" chart.
     */
    fun horizon(windows: List<Triple<Int?, Int?, Int>>, fromYear: Int, years: Int): List<Map<WindowFilter, Int>> =
        (0 until years).map { offset ->
            val y = fromYear + offset
            windows.groupBy { (from, until, _) -> DrinkingWindow.status(from, until, y) }
                .mapValues { (_, list) -> list.sumOf { it.third } }
        }
}
