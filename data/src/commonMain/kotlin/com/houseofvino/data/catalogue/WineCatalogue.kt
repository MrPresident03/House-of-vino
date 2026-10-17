package com.houseofvino.data.catalogue

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.model.WineStyle
import com.houseofvino.core.scan.Confidence
import com.houseofvino.core.scan.Suggested
import com.houseofvino.core.scan.WineSuggestion
import com.houseofvino.core.text.Fuzzy
import com.houseofvino.core.text.TextNormalizer

/** One wine in the bundled reference catalogue (an LWIN record). Vintage-independent. */
data class CatalogueWine(
    val lwin: Long,
    val producerTitle: String?,
    val producerName: String,
    val wine: String?,
    val country: String?,
    val region: String?,
    val subRegion: String?,
    val site: String?,
    val parcel: String?,
    val colour: String?,
    val type: String?,
    val subType: String?,
    val designation: String?,
    val classification: String?,
    val firstVintage: Int?,
    val finalVintage: Int?,
) {
    /** Producer as printed on most labels: "Chateau" + "Margaux", or just the name. */
    val producer: String get() = listOfNotNull(producerTitle, producerName).joinToString(" ")

    /** LWIN's display order: producer, wine, classification, place. */
    val displayName: String get() = buildString {
        append(producer)
        val rest = listOfNotNull(wine, classification, subRegion?.takeIf { it != region }, site, parcel)
        if (rest.isNotEmpty()) append(", ").append(rest.joinToString(", "))
    }

    val color: WineColor? get() = when (colour) {
        "Red" -> WineColor.RED
        "White" -> WineColor.WHITE
        "Rose" -> WineColor.ROSE
        else -> null
    }

    val style: WineStyle? get() = when {
        type == "Fortified Wine" || subType in FORTIFIED_SUBTYPES -> WineStyle.FORTIFIED
        subType == "Sparkling" -> WineStyle.SPARKLING
        subType in SWEET_SUBTYPES -> WineStyle.SWEET
        subType == "Still" -> WineStyle.STILL
        else -> null
    }

    /**
     * Fields for the wine editor. Only what LWIN states is filled; vintage, size, alcohol and
     * everything else stay with the label or the user.
     */
    fun toSuggestion(confidence: Confidence): WineSuggestion {
        fun s(v: String?) = v?.takeIf { it.isNotBlank() }?.let { Suggested(it, confidence, SOURCE) }
        return WineSuggestion(
            producer = s(producer),
            name = s(listOfNotNull(wine, classification, site, parcel).joinToString(" ").ifBlank { null }),
            color = color?.let { Suggested(it, confidence, SOURCE) },
            style = style?.let { Suggested(it, confidence, SOURCE) },
            country = s(country),
            region = s(region),
            appellation = s(subRegion),
        )
    }

    companion object {
        const val SOURCE = "LWIN catalogue"
        private val FORTIFIED_SUBTYPES = setOf("Port", "Madeira", "Sherry", "Vin Doux Naturel", "Marsala")
        private val SWEET_SUBTYPES = setOf("Sweet", "Dessert")
    }
}

/** A catalogue wine that fits what was scanned, with the evidence for it. */
data class CatalogueMatch(
    val wine: CatalogueWine,
    val score: Int,
    val confidence: Confidence,
    val reasons: List<String>,
    val warnings: List<String> = emptyList(),
)

/** The bundled wine reference (ADR-14). Read-only, on the phone, no network. */
interface WineCatalogue {
    val available: Boolean
    /** Credit line required by the data licence. */
    val attribution: String
    val size: Int

    /** Catalogue wines whose producer appears in [text] (label OCR or a product name), best first. */
    fun match(text: String, year: Int? = null, limit: Int = 5): List<CatalogueMatch>

    fun byLwin(lwin: Long): CatalogueWine?
}

object UnavailableCatalogue : WineCatalogue {
    override val available = false
    override val attribution = ""
    override val size = 0
    override fun match(text: String, year: Int?, limit: Int) = emptyList<CatalogueMatch>()
    override fun byLwin(lwin: Long): CatalogueWine? = null
}

/**
 * Matching against the LWIN pack built by [CataloguePack].
 *
 * A wine is a candidate only if every distinctive word of its producer is in the text; the
 * producer is the one thing every label prints. Candidates are then ranked by how much of the
 * cuvée name, site and classification also appear. The best match is reported at MEDIUM
 * confidence at most: the catalogue is a strong hint, never a confirmed identity.
 */
class LwinCatalogue(private val driver: SqlDriver) : WineCatalogue {
    override val available = true
    override val attribution = ATTRIBUTION
    override val size: Int by lazy { count() }

    override fun match(text: String, year: Int?, limit: Int): List<CatalogueMatch> {
        // Every word counts as evidence (“Block 3” needs the 3); only longer words are searched for.
        val present = TextNormalizer.tokens(text).toSet()
        val queryTerms = present.filter { it !in GENERIC && !it.all(Char::isDigit) && it.length >= 3 }.sortedByDescending { it.length }.take(MAX_QUERY_TERMS)
        if (queryTerms.isEmpty()) return emptyList()

        val ids = queryLongs(
            "SELECT docid FROM wine_fts WHERE wine_fts MATCH ? LIMIT $MAX_CANDIDATES",
            queryTerms.joinToString(" OR ") { "producer:$it" },
        )
        if (ids.isEmpty()) return emptyList()
        val fuzzyVocab = present.filter { it.length >= 5 }

        fun has(token: String): Boolean = token in present ||
            (token.length >= 6 && fuzzyVocab.any { kotlin.math.abs(it.length - token.length) <= 1 && Fuzzy.distance(it, token, 1) <= 1 })

        val scored = ArrayList<CatalogueMatch>()
        for (w in winesByIds(ids)) {
            val producerAll = TextNormalizer.tokens(w.producer)
            val producerTokens = producerAll.filter { it !in GENERIC }.ifEmpty { producerAll }
            if (producerTokens.isEmpty() || !producerTokens.all(::has)) continue

            val wineTokens = TextNormalizer.tokens(w.wine).filter { it !in GENERIC }
            val wineHits = wineTokens.count(::has)
            val wineCover = if (wineTokens.isEmpty()) 0.5 else wineHits.toDouble() / wineTokens.size
            val placeTokens = TextNormalizer.tokens(listOfNotNull(w.classification, w.subRegion, w.site, w.parcel).joinToString(" ")).filter { it !in GENERIC }
            val placeHits = placeTokens.count(::has)

            var score = 30 + 8 * minOf(producerTokens.size, 3) + (45 * wineCover).toInt() + 3 * wineHits + 6 * placeHits - 3 * (placeTokens.size - placeHits)
            val reasons = mutableListOf("Producer “${w.producer}” is on the label")
            if (wineTokens.isNotEmpty() && wineHits == wineTokens.size) reasons += "Wine name “${w.wine}” matches"
            else if (wineHits > 0) reasons += "Part of the wine name matches"
            if (placeHits > 0) reasons += "Place or classification matches"

            val warnings = mutableListOf<String>()
            if (year != null) {
                if (w.firstVintage != null && year < w.firstVintage || w.finalVintage != null && year > w.finalVintage) {
                    score -= 15
                    warnings += "LWIN lists this wine for ${w.firstVintage ?: "?"}–${w.finalVintage ?: "now"}, not $year"
                }
            }
            scored += CatalogueMatch(w, score, Confidence.LOW, reasons, warnings)
        }
        val ranked = scored.sortedWith(compareByDescending<CatalogueMatch> { it.score }.thenBy { it.wine.lwin }).take(limit)
        if (ranked.isEmpty()) return ranked
        // MEDIUM only when the cuvée fully matched and nothing else comes close.
        val top = ranked.first()
        val wineFull = top.reasons.any { it.startsWith("Wine name") }
        val clear = ranked.size == 1 || ranked[1].score <= top.score - 3
        return if (wineFull && clear && top.warnings.isEmpty()) listOf(top.copy(confidence = Confidence.MEDIUM)) + ranked.drop(1) else ranked
    }

    override fun byLwin(lwin: Long): CatalogueWine? = winesByIds(listOf(lwin)).firstOrNull()

    private fun count(): Int = queryLongs("SELECT count(*) FROM wine", null).firstOrNull()?.toInt() ?: 0

    private fun winesByIds(ids: List<Long>): List<CatalogueWine> {
        val out = ArrayList<CatalogueWine>(ids.size)
        for (chunk in ids.chunked(500)) {
            val sql = "SELECT $COLUMNS FROM wine WHERE lwin IN (${chunk.joinToString(",")})"
            driver.executeQuery(null, sql, { c ->
                while (c.next().value) out += c.toWine()
                QueryResult.Unit
            }, 0)
        }
        return out
    }

    private fun queryLongs(sql: String, arg: String?): List<Long> {
        val out = ArrayList<Long>()
        driver.executeQuery(null, sql, { c ->
            while (c.next().value) c.getLong(0)?.let(out::add)
            QueryResult.Unit
        }, if (arg == null) 0 else 1) { if (arg != null) bindString(0, arg) }
        return out
    }

    private fun SqlCursor.toWine(): CatalogueWine {
        fun s(i: Int) = getString(i)?.takeIf { it.isNotEmpty() }
        return CatalogueWine(
            lwin = getLong(0)!!, producerTitle = s(1), producerName = s(2) ?: "", wine = s(3), country = s(4), region = s(5),
            subRegion = s(6), site = s(7), parcel = s(8), colour = s(9), type = s(10), subType = s(11), designation = s(12),
            classification = s(13), firstVintage = getLong(14)?.toInt(), finalVintage = getLong(15)?.toInt(),
        )
    }

    companion object {
        const val ATTRIBUTION = "Wine catalogue: LWIN by Liv-ex, licensed CC BY 4.0. Adapted for search (spirits and retired codes removed)."
        internal const val COLUMNS = "lwin, producer_title, producer_name, wine, country, region, sub_region, site, parcel, colour, type, sub_type, designation, classification, first_vintage, final_vintage"
        private const val MAX_QUERY_TERMS = 30
        private const val MAX_CANDIDATES = 4000

        /** Words too common on labels and in producer names to identify anyone. */
        internal val GENERIC = setOf(
            "chateau", "domaine", "maison", "weingut", "bodega", "bodegas", "tenuta", "cantina", "cantine", "quinta", "casa", "castello",
            "schloss", "azienda", "agricola", "cave", "caves", "clos", "mas", "podere", "fattoria", "villa", "herdade", "cellier",
            "de", "du", "des", "la", "le", "les", "di", "del", "della", "dei", "da", "do", "dos", "das", "the", "and", "et", "y", "e",
            "von", "van", "der", "den", "zu", "of", "et", "fils", "pere", "freres", "family", "famille", "estate", "estates", "winery",
            "wines", "wine", "vineyard", "vineyards", "cellars", "vignobles", "vins", "vin", "vino", "vini", "wein", "saint", "sainte",
            "red", "white", "rose", "rouge", "blanc", "rosso", "bianco", "tinto", "blanco",
        )
    }
}
