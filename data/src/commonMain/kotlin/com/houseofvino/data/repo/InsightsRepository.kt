package com.houseofvino.data.repo

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import com.houseofvino.core.insights.CostLine
import com.houseofvino.core.insights.DrinkingWindow
import com.houseofvino.core.insights.InsightMath
import com.houseofvino.core.insights.MoneySummary
import com.houseofvino.core.insights.ValueLine
import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.CurrencyConverter
import com.houseofvino.core.search.WindowFilter
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.db.bindAny

data class CollectionTotals(
    val bottles: Int,
    /** Bottle-equivalents of 750 ml, in hundredths (a magnum counts 200). */
    val standardEquivalentsCentis: Long,
    val wines: Int,
    val vintages: Int,
    val unpricedBottles: Int,
    val unvaluedBottles: Int,
)

data class Breakdown(val label: String, val bottles: Int)

data class ConsumptionMonth(val month: String, val drunk: Int, val sold: Int, val gifted: Int, val lost: Int)

data class Insights(
    val totals: CollectionTotals,
    val cost: MoneySummary,
    /** Latest valuation per vintage and size; each line keeps its source and date. */
    val value: MoneySummary,
    val valueSources: List<ValueLine>,
    val windows: Map<WindowFilter, Int>,
    val horizon: List<Map<WindowFilter, Int>>,
    val byColor: List<Breakdown>,
    val byCountry: List<Breakdown>,
    val byRegion: List<Breakdown>,
    val byGrape: List<Breakdown>,
    val byDecade: List<Breakdown>,
    val byLocation: List<Breakdown>,
    val consumption: List<ConsumptionMonth>,
)

/** Collection analytics computed with SQL aggregates; only aggregate rows reach Kotlin. */
class InsightsRepository(private val database: LocalDatabase, private val locations: LocationRepository) {

    fun compute(collectionId: String, currentYear: Int, reporting: CurrencyCode?, converter: CurrencyConverter?, today: IsoDate): Insights {
        val inStock = """
            SELECT l.id AS lot_id, l.vintage_id, l.format_ml, l.unit_price_minor, l.unit_price_currency, s.location_key, s.quantity
            FROM lot l JOIN stock_level s ON s.lot_id = l.id
            WHERE l.collection_id = ? AND l.deleted_at IS NULL AND s.quantity > 0
        """.trimIndent()

        val totals = one("""
            SELECT coalesce(sum(st.quantity), 0), coalesce(sum(st.quantity * st.format_ml * 100 / 750), 0),
                   count(DISTINCT v.wine_id), count(DISTINCT st.vintage_id),
                   coalesce(sum(CASE WHEN st.unit_price_minor IS NULL THEN st.quantity ELSE 0 END), 0)
            FROM ($inStock) st JOIN vintage v ON v.id = st.vintage_id
        """.trimIndent(), collectionId) { c ->
            CollectionTotals(c.int(0), c.getLong(1) ?: 0, c.int(2), c.int(3), c.int(4), 0)
        }

        val costLines = rows("""
            SELECT st.unit_price_minor, st.unit_price_currency, sum(st.quantity) FROM ($inStock) st
            WHERE st.unit_price_minor IS NOT NULL GROUP BY st.unit_price_currency, st.unit_price_minor
        """.trimIndent(), collectionId) { c -> money(c.getLong(0), c.getString(1))?.let { CostLine(it, c.int(2)) } }.filterNotNull()

        // Latest valuation for each (vintage, size) that is in stock.
        val valueLines = rows("""
            SELECT va.amount_minor, va.amount_currency, sum(st.quantity), va.source, va.as_of
            FROM ($inStock) st
            JOIN valuation va ON va.id = (
                SELECT v2.id FROM valuation v2 WHERE v2.vintage_id = st.vintage_id AND v2.format_ml = st.format_ml
                AND v2.deleted_at IS NULL ORDER BY v2.as_of DESC, v2.created_at DESC LIMIT 1)
            GROUP BY va.id
        """.trimIndent(), collectionId) { c ->
            money(c.getLong(0), c.getString(1))?.let { ValueLine(it, c.int(2), c.getString(3)!!, IsoDate.parse(c.getString(4)!!)) }
        }.filterNotNull()
        val valued = valueLines.sumOf { it.bottles }

        val windowRows = rows("""
            SELECT v.drink_from, v.drink_until, sum(st.quantity) FROM ($inStock) st JOIN vintage v ON v.id = st.vintage_id
            GROUP BY v.id
        """.trimIndent(), collectionId) { c -> Triple(c.getLong(0)?.toInt(), c.getLong(1)?.toInt(), c.int(2)) }
        val windows = windowRows.groupBy { DrinkingWindow.status(it.first, it.second, currentYear) }.mapValues { e -> e.value.sumOf { it.third } }

        fun breakdown(select: String, join: String = "", group: String = "1"): List<Breakdown> = rows("""
            SELECT $select AS label, sum(st.quantity) FROM ($inStock) st JOIN vintage v ON v.id = st.vintage_id
            JOIN wine w ON w.id = v.wine_id $join GROUP BY $group ORDER BY 2 DESC LIMIT 12
        """.trimIndent(), collectionId) { c -> Breakdown(c.getString(0) ?: "Not recorded", c.int(1)) }

        val locationNames = locations.all(collectionId).associate { it.id to it.name }
        val byLocation = rows("""
            SELECT st.location_key, sum(st.quantity) FROM ($inStock) st GROUP BY st.location_key ORDER BY 2 DESC
        """.trimIndent(), collectionId) { c ->
            val key = c.getString(0)!!
            Breakdown(if (key.isEmpty()) "Not put away" else locationNames[key] ?: "Removed location", c.int(1))
        }

        val from = today.plusDays(-365)
        val consumption = rows("""
            SELECT strftime('%Y-%m', m.occurred_at / 1000, 'unixepoch') AS month,
                   sum(CASE WHEN m.kind = 'CONSUME' THEN -m.delta ELSE 0 END),
                   sum(CASE WHEN m.kind = 'SELL' THEN -m.delta ELSE 0 END),
                   sum(CASE WHEN m.kind = 'GIFT_OUT' THEN -m.delta ELSE 0 END),
                   sum(CASE WHEN m.kind = 'LOSS' THEN -m.delta ELSE 0 END)
            FROM movement m
            WHERE m.collection_id = ? AND m.kind IN ('CONSUME', 'SELL', 'GIFT_OUT', 'LOSS') AND m.occurred_at >= ?
              AND m.id NOT IN (SELECT reversal_of FROM movement WHERE kind = 'REVERSAL' AND reversal_of IS NOT NULL)
            GROUP BY month ORDER BY month
        """.trimIndent(), collectionId, from.epochDay * 86_400_000L) { c ->
            ConsumptionMonth(c.getString(0)!!, c.int(1), c.int(2), c.int(3), c.int(4))
        }

        return Insights(
            totals = totals.copy(unvaluedBottles = (totals.bottles - valued).coerceAtLeast(0)),
            cost = InsightMath.cost(costLines, reporting, converter),
            value = InsightMath.value(valueLines, reporting, converter),
            valueSources = valueLines,
            windows = windows,
            horizon = InsightMath.horizon(windowRows, currentYear, 10),
            byColor = breakdown("w.color"),
            byCountry = breakdown("w.country"),
            byRegion = breakdown("w.region"),
            byGrape = breakdown("g.grape", "JOIN wine_grape g ON g.wine_id = w.id AND g.deleted_at IS NULL AND g.position = 0", "lower(g.grape)"),
            byDecade = breakdown("CASE WHEN v.vintage_kind = 'YEAR' THEN (v.year / 10 * 10) || 's' WHEN v.vintage_kind = 'NV' THEN 'Non-vintage' END"),
            byLocation = byLocation,
            consumption = consumption,
        )
    }

    data class QuickTotals(val bottles: Int, val vintages: Int, val readyBottles: Int, val drinkSoonBottles: Int, val pastBottles: Int)

    /** Cheap summary for the cellar header (one aggregate query). */
    fun quickTotals(collectionId: String, currentYear: Int): QuickTotals = one("""
        SELECT coalesce(sum(q.qty), 0), count(*),
               coalesce(sum(CASE WHEN (v.drink_from IS NULL OR v.drink_from <= ?) AND v.drink_until > ? THEN q.qty
                                 WHEN v.drink_from <= ? AND v.drink_until IS NULL THEN q.qty ELSE 0 END), 0),
               coalesce(sum(CASE WHEN v.drink_until = ? THEN q.qty ELSE 0 END), 0),
               coalesce(sum(CASE WHEN v.drink_until < ? THEN q.qty ELSE 0 END), 0)
        FROM (SELECT l.vintage_id, sum(s.quantity) AS qty FROM lot l JOIN stock_level s ON s.lot_id = l.id
              WHERE l.collection_id = ? AND l.deleted_at IS NULL AND s.quantity > 0 GROUP BY l.vintage_id) q
        JOIN vintage v ON v.id = q.vintage_id WHERE v.deleted_at IS NULL
    """.trimIndent(), currentYear.toLong(), currentYear.toLong(), currentYear.toLong(), currentYear.toLong(), currentYear.toLong(), collectionId) { c ->
        QuickTotals(c.int(0), c.int(1), c.int(2), c.int(3), c.int(4))
    }

    private fun SqlCursor.int(i: Int): Int = (getLong(i) ?: 0L).toInt()

    private fun <T> rows(sql: String, vararg args: Any?, map: (SqlCursor) -> T): List<T> =
        database.driver.executeQuery(null, sql, { c ->
            val out = ArrayList<T>()
            while (c.next().value) out += map(c)
            QueryResult.Value(out)
        }, args.size) { args.forEachIndexed { i, a -> bindAny(i, a) } }.value

    private fun <T> one(sql: String, vararg args: Any?, map: (SqlCursor) -> T): T = rows(sql, *args, map = map).single()
}
