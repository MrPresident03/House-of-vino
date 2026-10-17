package com.houseofvino.data.search

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.model.WineStyle
import com.houseofvino.core.search.QueryParser
import com.houseofvino.core.search.SearchFilters
import com.houseofvino.core.search.SearchQuery
import com.houseofvino.core.search.SearchScope
import com.houseofvino.core.search.SortOrder
import com.houseofvino.core.text.Fuzzy
import com.houseofvino.core.text.TextNormalizer
import com.houseofvino.data.db.LocalDatabase
import com.houseofvino.data.db.bindAny
import kotlin.math.ln
import kotlin.math.min

/** One vintage in a cellar list or search result. */
data class CellarRow(
    val vintageId: String,
    val wineId: String,
    val producer: String,
    val wineName: String?,
    val vintage: VintageYear,
    val color: WineColor?,
    val style: WineStyle?,
    val country: String?,
    val region: String?,
    val appellation: String?,
    val quantity: Int,
    val formatsMl: List<Int>,
    val drinkFrom: Int?,
    val drinkUntil: Int?,
    val photoPath: String?,
    val bestRating: Int?,
)

data class WishlistRow(
    val itemId: String,
    val producer: String?,
    val wineName: String?,
    val vintage: VintageYear?,
    val priority: String,
    val status: String,
    val desiredQuantity: Int,
    val targetPriceMinor: Long?,
    val targetPriceCurrency: String?,
    val bestPriceMinor: Long?,
)

data class JournalRow(
    val tastingId: String,
    val vintageId: String,
    val producer: String,
    val wineName: String?,
    val vintage: VintageYear,
    val tastedOn: String,
    val rating: Int?,
    val notesPreview: String?,
    val occasion: String?,
)

data class SearchPage<T>(
    val items: List<T>,
    val total: Int,
    val offset: Int,
    /** Typo corrections applied, e.g. "margeaux" → "margaux". Shown as "Showing results for …". */
    val corrections: Map<String, String> = emptyMap(),
)

/**
 * Executes searches entirely in SQLite: FTS4 for text, parameterised SQL for structured filters,
 * LIMIT/OFFSET paging. Only ids and scores of matching rows are held in memory for relevance
 * ranking — never the collection itself.
 */
class SearchEngine(private val database: LocalDatabase) {

    private val driver get() = database.driver

    fun searchCellar(collectionId: String, query: SearchQuery, offset: Int, limit: Int, currentYear: Int, currency: String?): SearchPage<CellarRow> =
        runWithTypoFallback(query) { q -> cellar(collectionId, q, offset, limit, currentYear, currency) }

    fun searchWishlist(collectionId: String, query: SearchQuery, statuses: Set<String>, offset: Int, limit: Int): SearchPage<WishlistRow> =
        runWithTypoFallback(query) { q -> wishlist(collectionId, q, statuses, offset, limit) }

    fun searchJournal(collectionId: String, query: SearchQuery, offset: Int, limit: Int): SearchPage<JournalRow> =
        runWithTypoFallback(query) { q -> journal(collectionId, q, offset, limit) }

    // ---------------------------------------------------------------------------------------------
    // Typo tolerance

    private fun <T> runWithTypoFallback(query: SearchQuery, run: (SearchQuery) -> SearchPage<T>): SearchPage<T> {
        val first = run(query)
        if (first.total > 0 || query.text.isBlank()) return first
        val parsed = QueryParser.parse(query.text)
        val corrections = LinkedHashMap<String, String>()
        for (token in parsed.tokens) {
            if (tokenHasHits(token)) continue
            val suggestion = Fuzzy.suggest(token, vocabularyNear(token), limit = 1).firstOrNull() ?: continue
            corrections[token] = suggestion.term
        }
        if (corrections.isEmpty()) return first
        val corrected = parsed.tokens.joinToString(" ") { corrections[it] ?: it }
        // Keep inline field filters ("vintage:2015") as typed; only the free-text tokens change.
        val fields = FIELD.findAll(query.text).joinToString(" ") { it.value }
        val second = run(query.copy(text = "$fields $corrected".trim()))
        return if (second.total > 0) second.copy(corrections = corrections) else first
    }

    private fun tokenHasHits(token: String): Boolean {
        val match = QueryParser.toFtsMatch(listOf(token)) ?: return true
        return (queryLong("SELECT count(*) FROM search_fts WHERE search_fts MATCH ?", match) ?: 0L) > 0
    }

    private fun vocabularyNear(token: String): Map<String, Int> {
        val budget = Fuzzy.allowedEdits(token)
        return driver.executeQuery(null,
            "SELECT term, documents FROM search_terms WHERE col = '*' AND length(term) BETWEEN ? AND ?", { c ->
                val out = HashMap<String, Int>()
                while (c.next().value) out[c.getString(0)!!] = c.getLong(1)!!.toInt()
                QueryResult.Value(out)
            }, 2) {
            bindLong(0, (token.length - budget).toLong())
            bindLong(1, (token.length + budget).toLong())
        }.value
    }

    // ---------------------------------------------------------------------------------------------
    // Cellar scope

    private fun cellar(collectionId: String, query: SearchQuery, offset: Int, limit: Int, currentYear: Int, currency: String?): SearchPage<CellarRow> {
        val parsed = QueryParser.parse(query.text)
        val sql = SqlBuilder()
        sql.append("""
            FROM vintage v JOIN wine w ON w.id = v.wine_id
            LEFT JOIN (
                SELECT l.vintage_id, sum(s.quantity) AS qty, group_concat(DISTINCT l.format_ml) AS formats,
                       max(CASE WHEN l.unit_price_currency = ? THEN l.unit_price_minor END) AS price
                FROM lot l JOIN stock_level s ON s.lot_id = l.id
                WHERE l.deleted_at IS NULL AND l.collection_id = ? AND s.quantity > 0
                GROUP BY l.vintage_id
            ) st ON st.vintage_id = v.id
            WHERE w.collection_id = ? AND v.deleted_at IS NULL AND w.deleted_at IS NULL
        """.trimIndent(), currency ?: query.filters.priceCurrency, collectionId, collectionId)

        val match = ftsMatch(parsed)
        if (match != null) {
            sql.append(" AND v.id IN (SELECT d.entity_id FROM search_doc d WHERE d.scope = 'CELLAR' AND d.docid IN (SELECT docid FROM search_fts WHERE search_fts MATCH ?))", match)
        }
        applyCellarFilters(sql, query.filters, parsed, currentYear)

        val ratingExpr = "(SELECT max(t.rating) FROM tasting t WHERE t.vintage_id = v.id AND t.deleted_at IS NULL)"
        val select = """
            SELECT v.id, w.id, w.producer, w.name, v.vintage_kind, v.year, w.color, w.style, w.country, w.region,
                   w.appellation, coalesce(st.qty, 0), st.formats, v.drink_from, v.drink_until,
                   (SELECT p.local_path FROM photo p WHERE p.id = v.label_photo_id AND p.deleted_at IS NULL),
                   $ratingExpr
        """.trimIndent()

        val relevance = query.sort == SortOrder.RELEVANCE && match != null
        if (relevance) {
            val ids = idList("SELECT v.id ${sql.text}", sql.args)
            val scores = scores("CELLAR", match!!)
            val ordered = ids.sortedWith(compareByDescending<String> { scores[it] ?: 0.0 }.thenBy { it })
            val page = ordered.drop(offset).take(limit)
            val rows = if (page.isEmpty()) emptyList() else {
                val byId = rowsFor("$select ${sql.text} AND v.id IN (${page.joinToString { "?" }})", sql.args + page, ::cellarRow)
                    .associateBy { it.vintageId }
                page.mapNotNull { byId[it] }
            }
            return SearchPage(rows, ids.size, offset)
        }
        val total = queryLong("SELECT count(*) ${sql.text}", *sql.args.toTypedArray())?.toInt() ?: 0
        val order = when (query.sort) {
            SortOrder.PRODUCER, SortOrder.RELEVANCE -> "w.producer_sort, w.name, v.year"
            SortOrder.VINTAGE_ASC -> "(v.vintage_kind <> 'YEAR'), v.year, w.producer_sort"
            SortOrder.VINTAGE_DESC -> "(v.vintage_kind <> 'YEAR'), v.year DESC, w.producer_sort"
            SortOrder.QUANTITY_DESC -> "coalesce(st.qty, 0) DESC, w.producer_sort"
            SortOrder.RECENTLY_ADDED -> "v.created_at DESC"
            SortOrder.PRICE_DESC -> "(st.price IS NULL), st.price DESC, w.producer_sort"
            SortOrder.PRICE_ASC -> "(st.price IS NULL), st.price, w.producer_sort"
            SortOrder.DRINK_BY -> "(v.drink_until IS NULL), v.drink_until, v.drink_from, w.producer_sort"
            SortOrder.RATING_DESC, SortOrder.TASTED_DESC -> "($ratingExpr IS NULL), $ratingExpr DESC, w.producer_sort"
        }
        val rows = rowsFor("$select ${sql.text} ORDER BY $order, v.id LIMIT ? OFFSET ?", sql.args + listOf(limit.toLong(), offset.toLong()), ::cellarRow)
        return SearchPage(rows, total, offset)
    }

    private fun applyCellarFilters(sql: SqlBuilder, f: SearchFilters, parsed: QueryParser.Parsed, currentYear: Int) {
        if (f.inStockOnly) sql.append(" AND coalesce(st.qty, 0) > 0")
        val colors = f.colors + parsed.colors
        if (colors.isNotEmpty()) sql.inList(" AND w.color", colors.map { it.name })
        val styles = f.styles + parsed.styles
        if (styles.isNotEmpty()) sql.inList(" AND w.style", styles.map { it.name })
        if (f.countries.isNotEmpty()) sql.inList(" AND w.country", f.countries.toList())
        if (f.regions.isNotEmpty()) sql.inList(" AND w.region", f.regions.toList())
        if (f.producers.isNotEmpty()) sql.inList(" AND w.producer_sort", f.producers.map { TextNormalizer.normalize(it) })
        if (f.grapes.isNotEmpty()) {
            sql.append(" AND EXISTS (SELECT 1 FROM wine_grape g WHERE g.wine_id = w.id AND g.deleted_at IS NULL")
            sql.inList(" AND lower(g.grape)", f.grapes.map { it.lowercase() })
            sql.append(")")
        }
        if (f.tagIds.isNotEmpty()) {
            sql.append(" AND EXISTS (SELECT 1 FROM vintage_tag vt WHERE vt.vintage_id = v.id AND vt.deleted_at IS NULL")
            sql.inList(" AND vt.tag_id", f.tagIds.toList())
            sql.append(")")
        }
        if (f.locationIds.isNotEmpty()) {
            val subtree = locationSubtree(f.locationIds)
            sql.append(" AND EXISTS (SELECT 1 FROM lot l2 JOIN stock_level s2 ON s2.lot_id = l2.id WHERE l2.vintage_id = v.id AND l2.deleted_at IS NULL AND s2.quantity > 0")
            sql.inList(" AND s2.location_key", subtree)
            sql.append(")")
        }
        if (f.formatsMl.isNotEmpty()) {
            sql.append(" AND EXISTS (SELECT 1 FROM lot l3 JOIN stock_level s3 ON s3.lot_id = l3.id WHERE l3.vintage_id = v.id AND l3.deleted_at IS NULL AND s3.quantity > 0")
            sql.inList(" AND l3.format_ml", f.formatsMl.map { it.toLong() })
            sql.append(")")
        }
        val from = parsed.vintageFrom ?: f.vintageFrom
        val to = parsed.vintageTo ?: f.vintageTo
        if (parsed.nonVintageOnly) {
            sql.append(" AND v.vintage_kind = 'NV'")
        } else if (from != null || to != null) {
            sql.append(" AND ((v.vintage_kind = 'YEAR' AND v.year BETWEEN ? AND ?)", (from ?: VintageYear.MIN_YEAR).toLong(), (to ?: VintageYear.MAX_YEAR).toLong())
            sql.append(if (f.includeNonVintage) " OR v.vintage_kind <> 'YEAR')" else ")")
        }
        if (f.windows.isNotEmpty()) {
            sql.append(" AND (CASE WHEN v.drink_from IS NULL AND v.drink_until IS NULL THEN 'UNKNOWN' " +
                "WHEN v.drink_from IS NOT NULL AND ? < v.drink_from THEN 'HOLD' " +
                "WHEN v.drink_until IS NOT NULL AND ? > v.drink_until THEN 'PAST' " +
                "WHEN v.drink_until = ? THEN 'DRINK_SOON' ELSE 'READY' END)", currentYear.toLong(), currentYear.toLong(), currentYear.toLong())
            sql.inList("", f.windows.map { it.name })
        }
        if (f.priceMinMinor != null) sql.append(" AND st.price >= ?", f.priceMinMinor)
        if (f.priceMaxMinor != null) sql.append(" AND st.price <= ?", f.priceMaxMinor)
        f.minRating?.let { min ->
            sql.append(" AND (SELECT max(t.rating) FROM tasting t WHERE t.vintage_id = v.id AND t.deleted_at IS NULL) >= ?", min.toLong())
        }
    }

    private fun cellarRow(c: SqlCursor) = CellarRow(
        vintageId = c.getString(0)!!, wineId = c.getString(1)!!, producer = c.getString(2)!!, wineName = c.getString(3),
        vintage = VintageYear.fromStorage(c.getString(4)!!, c.getLong(5)?.toInt()),
        color = c.getString(6)?.let { WineColor.valueOf(it) }, style = c.getString(7)?.let { WineStyle.valueOf(it) },
        country = c.getString(8), region = c.getString(9), appellation = c.getString(10),
        quantity = c.getLong(11)!!.toInt(),
        formatsMl = c.getString(12)?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.sorted().orEmpty(),
        drinkFrom = c.getLong(13)?.toInt(), drinkUntil = c.getLong(14)?.toInt(), photoPath = c.getString(15),
        bestRating = c.getLong(16)?.toInt(),
    )

    // ---------------------------------------------------------------------------------------------
    // Wishlist scope

    private fun wishlist(collectionId: String, query: SearchQuery, statuses: Set<String>, offset: Int, limit: Int): SearchPage<WishlistRow> {
        val parsed = QueryParser.parse(query.text)
        val sql = SqlBuilder()
        sql.append("""
            FROM wishlist_item i
            LEFT JOIN vintage v ON v.id = i.vintage_id
            LEFT JOIN wine w ON w.id = v.wine_id
            LEFT JOIN wine w2 ON w2.id = i.wine_id
            WHERE i.collection_id = ? AND i.deleted_at IS NULL
        """.trimIndent(), collectionId)
        if (statuses.isNotEmpty()) sql.inList(" AND i.status", statuses.toList())
        val match = ftsMatch(parsed)
        if (match != null) {
            sql.append(" AND i.id IN (SELECT d.entity_id FROM search_doc d WHERE d.scope = 'WISHLIST' AND d.docid IN (SELECT docid FROM search_fts WHERE search_fts MATCH ?))", match)
        }
        val total = queryLong("SELECT count(*) ${sql.text}", *sql.args.toTypedArray())?.toInt() ?: 0
        val select = """
            SELECT i.id, coalesce(w.producer, w2.producer, i.producer), coalesce(w.name, w2.name, i.name),
                   coalesce(v.vintage_kind, i.vintage_kind), coalesce(v.year, i.year), i.priority, i.status, i.desired_qty,
                   i.target_price_minor, i.target_price_currency,
                   (SELECT min(ml.last_price_minor) FROM merchant_link ml WHERE ml.wishlist_item_id = i.id
                       AND ml.deleted_at IS NULL AND ml.last_price_currency = i.target_price_currency)
        """.trimIndent()
        val order = "CASE i.priority WHEN 'MUST' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, i.updated_at DESC, i.id"
        val rows = rowsFor("$select ${sql.text} ORDER BY $order LIMIT ? OFFSET ?", sql.args + listOf(limit.toLong(), offset.toLong())) { c ->
            WishlistRow(
                itemId = c.getString(0)!!, producer = c.getString(1), wineName = c.getString(2),
                vintage = c.getString(3)?.let { VintageYear.fromStorage(it, c.getLong(4)?.toInt()) },
                priority = c.getString(5)!!, status = c.getString(6)!!, desiredQuantity = c.getLong(7)!!.toInt(),
                targetPriceMinor = c.getLong(8), targetPriceCurrency = c.getString(9), bestPriceMinor = c.getLong(10),
            )
        }
        return SearchPage(rows, total, offset)
    }

    // ---------------------------------------------------------------------------------------------
    // Journal scope

    private fun journal(collectionId: String, query: SearchQuery, offset: Int, limit: Int): SearchPage<JournalRow> {
        val parsed = QueryParser.parse(query.text)
        val sql = SqlBuilder()
        sql.append("""
            FROM tasting t JOIN vintage v ON v.id = t.vintage_id JOIN wine w ON w.id = v.wine_id
            WHERE t.collection_id = ? AND t.deleted_at IS NULL
        """.trimIndent(), collectionId)
        val match = ftsMatch(parsed)
        if (match != null) {
            sql.append(" AND t.id IN (SELECT d.entity_id FROM search_doc d WHERE d.scope = 'JOURNAL' AND d.docid IN (SELECT docid FROM search_fts WHERE search_fts MATCH ?))", match)
        }
        val f = query.filters
        val colors = f.colors + parsed.colors
        if (colors.isNotEmpty()) sql.inList(" AND w.color", colors.map { it.name })
        f.minRating?.let { min -> sql.append(" AND t.rating >= ?", min.toLong()) }
        val from = parsed.vintageFrom ?: f.vintageFrom
        val to = parsed.vintageTo ?: f.vintageTo
        if (from != null || to != null) {
            sql.append(" AND v.vintage_kind = 'YEAR' AND v.year BETWEEN ? AND ?", (from ?: VintageYear.MIN_YEAR).toLong(), (to ?: VintageYear.MAX_YEAR).toLong())
        }
        val total = queryLong("SELECT count(*) ${sql.text}", *sql.args.toTypedArray())?.toInt() ?: 0
        val order = when (query.sort) {
            SortOrder.RATING_DESC -> "(t.rating IS NULL), t.rating DESC, t.tasted_on DESC"
            SortOrder.PRODUCER -> "w.producer_sort, v.year"
            else -> "t.tasted_on DESC, t.created_at DESC"
        }
        val rows = rowsFor("""
            SELECT t.id, v.id, w.producer, w.name, v.vintage_kind, v.year, t.tasted_on, t.rating, substr(t.notes, 1, 160), t.occasion
            ${sql.text} ORDER BY $order, t.id LIMIT ? OFFSET ?
        """.trimIndent(), sql.args + listOf(limit.toLong(), offset.toLong())) { c ->
            JournalRow(
                tastingId = c.getString(0)!!, vintageId = c.getString(1)!!, producer = c.getString(2)!!, wineName = c.getString(3),
                vintage = VintageYear.fromStorage(c.getString(4)!!, c.getLong(5)?.toInt()), tastedOn = c.getString(6)!!,
                rating = c.getLong(7)?.toInt(), notesPreview = c.getString(8), occasion = c.getString(9),
            )
        }
        return SearchPage(rows, total, offset)
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers

    /** Combines free-text tokens and inline field filters into one FTS4 MATCH expression. */
    internal fun ftsMatch(parsed: QueryParser.Parsed): String? {
        val parts = ArrayList<String>()
        QueryParser.toFtsMatch(parsed.tokens)?.let(parts::add)
        fun column(col: String, values: Set<String>) = values.forEach { v ->
            TextNormalizer.tokens(v).forEach { t -> parts += "$col:$t*" }
        }
        column("producer", parsed.producers)
        column("place", parsed.regions + parsed.countries)
        column("grapes", parsed.grapes)
        column("tags", parsed.tags)
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    /**
     * Relevance per entity from matchinfo('pcxn'): hits in identity columns weigh most; rarer
     * terms weigh more (idf). Only rows matching the text are scored.
     */
    private fun scores(scope: String, match: String): Map<String, Double> =
        driver.executeQuery(null,
            "SELECT d.entity_id, matchinfo(search_fts, 'pcxn') FROM search_fts JOIN search_doc d ON d.docid = search_fts.docid " +
                "WHERE search_fts MATCH ? AND d.scope = ?", { c ->
                val out = HashMap<String, Double>()
                while (c.next().value) {
                    val id = c.getString(0)!!
                    val info = c.getBytes(1) ?: continue
                    out[id] = scoreMatchInfo(info)
                }
                QueryResult.Value(out)
            }, 2) { bindString(0, match); bindString(1, scope) }.value

    private fun scoreMatchInfo(blob: ByteArray): Double {
        fun int(i: Int): Long {
            val o = i * 4
            return (blob[o].toLong() and 0xff) or ((blob[o + 1].toLong() and 0xff) shl 8) or
                ((blob[o + 2].toLong() and 0xff) shl 16) or ((blob[o + 3].toLong() and 0xff) shl 24)
        }
        val phrases = int(0).toInt()
        val cols = int(1).toInt()
        val n = int(2 + phrases * cols * 3).coerceAtLeast(1)
        var score = 0.0
        for (p in 0 until phrases) {
            for (col in 0 until cols) {
                val base = 2 + (p * cols + col) * 3
                val hitsHere = int(base)
                val docsWithHits = int(base + 2).coerceAtLeast(1)
                if (hitsHere > 0) {
                    val idf = 1.0 + ln(n.toDouble() / docsWithHits.toDouble())
                    score += COLUMN_WEIGHTS.getOrElse(col) { 0.5 } * min(hitsHere, 3L) * idf
                }
            }
        }
        return score
    }

    private fun locationSubtree(roots: Set<String>): List<String> {
        val result = LinkedHashSet<String>(roots)
        var frontier = roots.toList()
        var guard = 0
        while (frontier.isNotEmpty() && guard++ < 32) {
            val next = idList("SELECT id FROM location WHERE deleted_at IS NULL AND parent_id IN (${frontier.joinToString { "?" }})", frontier)
            frontier = next.filter { result.add(it) }
        }
        return result.toList()
    }

    private fun idList(sql: String, args: List<Any?>): List<String> =
        driver.executeQuery(null, sql, { c ->
            val out = ArrayList<String>()
            while (c.next().value) c.getString(0)?.let(out::add)
            QueryResult.Value(out)
        }, args.size) { args.forEachIndexed { i, a -> bindAny(i, a) } }.value

    private fun <T> rowsFor(sql: String, args: List<Any?>, map: (SqlCursor) -> T): List<T> =
        driver.executeQuery(null, sql, { c ->
            val out = ArrayList<T>()
            while (c.next().value) out += map(c)
            QueryResult.Value(out)
        }, args.size) { args.forEachIndexed { i, a -> bindAny(i, a) } }.value

    private fun queryLong(sql: String, vararg args: Any?): Long? = database.queryLong(sql, *args)

    private class SqlBuilder {
        private val sb = StringBuilder()
        val args = ArrayList<Any?>()
        val text: String get() = sb.toString()

        fun append(fragment: String, vararg values: Any?) {
            sb.append(fragment)
            args.addAll(values)
        }

        fun inList(prefix: String, values: List<Any?>) {
            require(values.isNotEmpty())
            sb.append(prefix).append(" IN (").append(values.joinToString { "?" }).append(")")
            args.addAll(values)
        }
    }

    companion object {
        /** producer, name, vintage, place, grapes, tags, notes */
        private val COLUMN_WEIGHTS = listOf(4.0, 4.0, 2.5, 2.0, 1.5, 1.5, 0.5)
        private val FIELD = Regex("""\b(?:producer|p|region|r|appellation|country|grape|g|variety|varietal|tag|t|color|colour|style|vintage|v|year):(?:"[^"]*"|\S+)""", RegexOption.IGNORE_CASE)

        fun scopeOf(name: String): SearchScope = SearchScope.valueOf(name)
    }
}
