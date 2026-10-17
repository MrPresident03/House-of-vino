package com.houseofvino.core.search

import com.houseofvino.core.model.WineColor
import com.houseofvino.core.model.WineStyle
import com.houseofvino.core.text.TextNormalizer

enum class SearchScope { CELLAR, WISHLIST, JOURNAL, DISCOVER }

enum class SortOrder {
    RELEVANCE, PRODUCER, VINTAGE_ASC, VINTAGE_DESC, QUANTITY_DESC, RECENTLY_ADDED,
    PRICE_DESC, PRICE_ASC, DRINK_BY, RATING_DESC, TASTED_DESC,
}

enum class WindowFilter { HOLD, READY, DRINK_SOON, PAST, UNKNOWN }

/** Structured filters. Empty sets mean "no constraint". */
data class SearchFilters(
    val colors: Set<WineColor> = emptySet(),
    val styles: Set<WineStyle> = emptySet(),
    val countries: Set<String> = emptySet(),
    val regions: Set<String> = emptySet(),
    val grapes: Set<String> = emptySet(),
    val producers: Set<String> = emptySet(),
    val tagIds: Set<String> = emptySet(),
    val locationIds: Set<String> = emptySet(),
    val formatsMl: Set<Int> = emptySet(),
    val vintageFrom: Int? = null,
    val vintageTo: Int? = null,
    /** With a vintage range set, also include NV and unknown-vintage wines. */
    val includeNonVintage: Boolean = false,
    val inStockOnly: Boolean = true,
    val windows: Set<WindowFilter> = emptySet(),
    val priceMinMinor: Long? = null,
    val priceMaxMinor: Long? = null,
    val priceCurrency: String? = null,
    val minRating: Int? = null,
) {
    val activeCount: Int
        get() = listOf(colors, styles, countries, regions, grapes, producers, tagIds, locationIds, formatsMl, windows)
            .count { it.isNotEmpty() } +
            (if (vintageFrom != null || vintageTo != null) 1 else 0) +
            (if (priceMinMinor != null || priceMaxMinor != null) 1 else 0) +
            (if (minRating != null) 1 else 0) +
            (if (!inStockOnly) 1 else 0)
}

data class SearchQuery(
    val text: String = "",
    val scope: SearchScope = SearchScope.CELLAR,
    val filters: SearchFilters = SearchFilters(),
    val sort: SortOrder = SortOrder.RELEVANCE,
)

/**
 * Parses inline field syntax in the search box into structured filters plus free text:
 * `producer:roulot`, `grape:"pinot noir"`, `region:barolo`, `country:italy`, `tag:gift`,
 * `vintage:2010`, `vintage:2010-2016`, `vintage:nv`, `color:red`, `style:sparkling`.
 * Unknown fields stay in the free text so nothing the user typed is dropped.
 */
object QueryParser {

    data class Parsed(
        val freeText: String,
        val tokens: List<String>,
        val producers: Set<String>,
        val regions: Set<String>,
        val countries: Set<String>,
        val grapes: Set<String>,
        val tags: Set<String>,
        val colors: Set<WineColor>,
        val styles: Set<WineStyle>,
        val vintageFrom: Int?,
        val vintageTo: Int?,
        val nonVintageOnly: Boolean,
    )

    private val FIELD = Regex("""(\w+):(?:"([^"]*)"|(\S+))""")
    private val RANGE = Regex("""^(\d{4})\s*(?:-|\.\.)\s*(\d{4})$""")

    fun parse(input: String): Parsed {
        val producers = linkedSetOf<String>()
        val regions = linkedSetOf<String>()
        val countries = linkedSetOf<String>()
        val grapes = linkedSetOf<String>()
        val tags = linkedSetOf<String>()
        val colors = linkedSetOf<WineColor>()
        val styles = linkedSetOf<WineStyle>()
        var from: Int? = null
        var to: Int? = null
        var nvOnly = false
        val leftover = StringBuilder()
        var last = 0
        for (m in FIELD.findAll(input)) {
            val field = m.groupValues[1].lowercase()
            val value = m.groupValues[2].ifEmpty { m.groupValues[3] }
            val handled = when (field) {
                "producer", "p" -> producers.add(value)
                "region", "r", "appellation" -> regions.add(value)
                "country" -> countries.add(value)
                "grape", "g", "variety", "varietal" -> grapes.add(value)
                "tag", "t" -> tags.add(value)
                "color", "colour" -> colorOf(value)?.let { colors.add(it) } != null
                "style" -> styleOf(value)?.let { styles.add(it) } != null
                "vintage", "v", "year" -> {
                    val range = RANGE.matchEntire(value)
                    when {
                        value.equals("nv", true) -> { nvOnly = true; true }
                        range != null -> {
                            val a = range.groupValues[1].toInt(); val b = range.groupValues[2].toInt()
                            from = minOf(a, b); to = maxOf(a, b); true
                        }
                        value.length == 4 && value.toIntOrNull() != null -> {
                            from = value.toInt(); to = value.toInt(); true
                        }
                        else -> false
                    }
                }
                else -> false
            }
            leftover.append(input, last, m.range.first)
            if (!handled) leftover.append(m.value)
            last = m.range.last + 1
        }
        leftover.append(input, last, input.length)
        val free = leftover.toString().trim().replace(Regex("\\s+"), " ")
        return Parsed(
            freeText = free,
            tokens = TextNormalizer.tokens(free),
            producers = producers, regions = regions, countries = countries, grapes = grapes, tags = tags,
            colors = colors, styles = styles, vintageFrom = from, vintageTo = to, nonVintageOnly = nvOnly,
        )
    }

    private fun colorOf(v: String): WineColor? = when (TextNormalizer.normalize(v)) {
        "red", "rouge", "rosso", "tinto" -> WineColor.RED
        "white", "blanc", "bianco", "blanco" -> WineColor.WHITE
        "rose", "rosado", "rosato" -> WineColor.ROSE
        "orange", "amber" -> WineColor.ORANGE
        else -> null
    }

    private fun styleOf(v: String): WineStyle? = when (TextNormalizer.normalize(v)) {
        "still" -> WineStyle.STILL
        "sparkling", "champagne", "mousseux", "spumante", "cava" -> WineStyle.SPARKLING
        "fortified", "port", "sherry", "madeira" -> WineStyle.FORTIFIED
        "sweet", "dessert", "liquoreux" -> WineStyle.SWEET
        else -> null
    }

    /**
     * Builds an FTS4 MATCH expression from normalised tokens: every token is a prefix term and
     * all must match. Tokens are alphanumeric after normalisation, so no escaping is needed;
     * anything else is dropped defensively.
     */
    fun toFtsMatch(tokens: List<String>): String? {
        val safe = tokens.map { t -> t.filter { it.isLetterOrDigit() } }.filter { it.isNotEmpty() }
        if (safe.isEmpty()) return null
        return safe.joinToString(" ") { "$it*" }
    }
}
