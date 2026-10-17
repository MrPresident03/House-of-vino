package com.houseofvino.core.scan

import com.houseofvino.core.csv.ExistingVintage
import com.houseofvino.core.csv.ImportParser
import com.houseofvino.core.model.BottleFormats
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.model.WineStyle
import com.houseofvino.core.text.Fuzzy
import com.houseofvino.core.text.TextNormalizer

enum class Confidence { HIGH, MEDIUM, LOW }

/** A suggested value with how sure we are and where it came from. Always user-editable. */
data class Suggested<T>(val value: T, val confidence: Confidence, val source: String)

/** Everything a scan produced, before the user confirms anything. */
data class WineSuggestion(
    val producer: Suggested<String>? = null,
    val name: Suggested<String>? = null,
    val vintage: Suggested<VintageYear>? = null,
    val formatMl: Suggested<Int>? = null,
    val abvBasisPoints: Suggested<Int>? = null,
    val color: Suggested<WineColor>? = null,
    val style: Suggested<WineStyle>? = null,
    val country: Suggested<String>? = null,
    val region: Suggested<String>? = null,
    val appellation: Suggested<String>? = null,
    val barcode: String? = null,
    val rawText: String? = null,
) {
    val isEmpty: Boolean
        get() = listOf(producer, name, vintage, formatMl, abvBasisPoints, color, style, country, region, appellation).all { it == null }
}

/** One recognised text line. [confidence] is 0–100 when the engine reports it; [heightPermille] is the line's height relative to the image. */
data class OcrLine(val text: String, val confidence: Int?, val heightPermille: Int?)

/**
 * Extracts wine fields from label OCR text. It reports what it can read with honest
 * confidence and never fills a field it can't support from the text.
 */
object LabelTextParser {

    private val YEAR = Regex("""(?<![\d.,])(1[89]\d\d|20\d\d)(?![\d.,%])""")
    private val FOUNDING = Regex("""\b(since|depuis|founded|established|est\.?|fondee?|gegr|dal|desde|seit)\b""", RegexOption.IGNORE_CASE)
    private val ABV = Regex("""(\d{1,2}(?:[.,]\d{1,2})?)\s*%\s*(?:vol|alc|abv|by vol)?""", RegexOption.IGNORE_CASE)
    private val PRODUCER_WORDS = listOf("chateau", "domaine", "weingut", "bodega", "bodegas", "tenuta", "cantina", "maison",
        "estate", "winery", "clos", "quinta", "castello", "fattoria", "azienda", "cave", "caves", "schloss")
    private val APPELLATION = listOf(
        Regex("""appellation\s+(.+?)\s+(?:controlee|protegee|d.origine)""", RegexOption.IGNORE_CASE),
        Regex("""^(.+?)\s+(?:aoc|aop|docg|doca|doc|dop|ava|do|igt|igp)$""", RegexOption.IGNORE_CASE),
        Regex("""denominazione di origine (?:controllata|protetta)(?: e garantita)?\s+(.+)""", RegexOption.IGNORE_CASE),
    )
    private val COUNTRIES = mapOf(
        "france" to "France", "italy" to "Italy", "italia" to "Italy", "spain" to "Spain", "espana" to "Spain",
        "germany" to "Germany", "deutschland" to "Germany", "portugal" to "Portugal", "austria" to "Austria",
        "osterreich" to "Austria", "new zealand" to "New Zealand", "australia" to "Australia",
        "south africa" to "South Africa", "argentina" to "Argentina", "chile" to "Chile", "usa" to "United States",
        "united states" to "United States", "greece" to "Greece", "hungary" to "Hungary", "lebanon" to "Lebanon",
    )
    private val COUNTRY_CUES = listOf("product of", "produce of", "produit de", "prodotto in", "producto de", "wine of", "made in", "vin de")

    fun parse(lines: List<OcrLine>, currentYear: Int): WineSuggestion {
        val clean = lines.map { it.copy(text = it.text.trim()) }.filter { it.text.isNotEmpty() }
        if (clean.isEmpty()) return WineSuggestion()
        val raw = clean.joinToString("\n") { it.text }
        val lowConfidenceText = clean.mapNotNull { it.confidence }.let { c -> c.isNotEmpty() && c.average() < 60 }
        fun cap(c: Confidence) = if (lowConfidenceText && c == Confidence.HIGH) Confidence.MEDIUM else if (lowConfidenceText) Confidence.LOW else c

        // Vintage: plausible years, excluding "since 1855"-style founding dates.
        val years = clean.flatMap { line ->
            if (FOUNDING.containsMatchIn(line.text)) emptyList()
            else YEAR.findAll(line.text).map { it.value.toInt() }.filter { it in 1900..(currentYear + 1) }.toList()
        }.distinct()
        val vintage: Suggested<VintageYear>? = when {
            years.size == 1 -> Suggested(VintageYear.Year(years.single()), cap(Confidence.MEDIUM), "label text")
            years.size > 1 -> Suggested(VintageYear.Year(years.max()), Confidence.LOW, "label text (${years.size} years found)")
            NV_WORDS.any { raw.contains(it, ignoreCase = true) } -> Suggested(VintageYear.NonVintage, cap(Confidence.MEDIUM), "label text")
            else -> null
        }

        val abv = ABV.findAll(raw).mapNotNull { m ->
            val (w, f) = m.groupValues[1].replace(',', '.').split('.').let { it[0] to it.getOrElse(1) { "" } }
            val bp = (w.toIntOrNull() ?: return@mapNotNull null) * 100 + f.padEnd(2, '0').take(2).toInt()
            bp.takeIf { it in 500..2300 }
        }.firstOrNull()?.let { Suggested(it, cap(Confidence.HIGH), "label text") }

        val format = BottleFormats.parseVolume(raw)?.let { Suggested(it, cap(Confidence.HIGH), "label text") }

        val appellation = clean.firstNotNullOfOrNull { line ->
            APPELLATION.firstNotNullOfOrNull { rx -> rx.find(TextNormalizer.fold(line.text))?.groupValues?.get(1) }
                ?.let { original(line.text, it) }
        }?.let { Suggested(it, cap(Confidence.MEDIUM), "label text") }

        val folded = TextNormalizer.normalize(raw)
        val country = COUNTRY_CUES.firstNotNullOfOrNull { cue ->
            val idx = folded.indexOf(cue)
            if (idx < 0) null else COUNTRIES.entries.firstOrNull { folded.startsWith(it.key, idx + cue.length + 1) }?.value
        }?.let { Suggested(it, cap(Confidence.HIGH), "label text") }
            ?: COUNTRIES.entries.firstOrNull { Regex("\\b${it.key}\\b").containsMatchIn(folded) }
                ?.let { Suggested(it.value, Confidence.LOW, "label text") }

        val style = ImportParser.parseStyle(raw)?.let { Suggested(it, cap(Confidence.MEDIUM), "label text") }
        val color = colorWord(folded)?.let { Suggested(it, cap(Confidence.MEDIUM), "label text") }

        // Producer: a line naming an estate type, else the most prominent line.
        val producerLine = clean.firstOrNull { line -> TextNormalizer.tokens(line.text).any { it in PRODUCER_WORDS } }
        val prominent = clean.filter { it.heightPermille != null && it.text.any(Char::isLetter) && YEAR.find(it.text)?.value != it.text }
            .sortedByDescending { it.heightPermille }
        val producer = when {
            producerLine != null -> Suggested(titleCase(producerLine.text), cap(Confidence.MEDIUM), "estate word on label")
            prominent.isNotEmpty() -> Suggested(titleCase(prominent.first().text), Confidence.LOW, "largest text on label")
            else -> null
        }
        val producerSourceText = producerLine?.text ?: prominent.firstOrNull()?.text
        val name = prominent.firstOrNull { line ->
            line.text != producerSourceText &&
                (appellation == null || !line.text.contains(appellation.value, ignoreCase = true)) &&
                ABV.find(line.text) == null &&
                BottleFormats.parseVolume(line.text) == null
        }?.let { Suggested(titleCase(it.text), Confidence.LOW, "prominent text on label") }

        return WineSuggestion(
            producer = producer, name = name, vintage = vintage, formatMl = format, abvBasisPoints = abv,
            color = color, style = style, country = country, appellation = appellation, rawText = raw,
        )
    }

    private val NV_WORDS = listOf("non vintage", "non-vintage", "sans année", "sans annee", "cuvée nv", " nv ", "multi-vintage")

    private fun colorWord(folded: String): WineColor? {
        val tokens = folded.split(' ').toSet()
        return when {
            tokens.any { it in setOf("rose", "rosado", "rosato") } -> WineColor.ROSE
            tokens.any { it in setOf("rouge", "red", "rosso", "tinto", "rot") } -> WineColor.RED
            tokens.any { it in setOf("blanc", "white", "bianco", "blanco", "weiss", "weisswein") } -> WineColor.WHITE
            else -> null
        }
    }

    /** Recovers the original-cased substring for a match made on folded text. */
    private fun original(line: String, foldedMatch: String): String {
        val idx = TextNormalizer.fold(line).indexOf(foldedMatch)
        return if (idx >= 0 && idx + foldedMatch.length <= line.length) line.substring(idx, idx + foldedMatch.length).trim() else foldedMatch.trim()
    }

    private fun titleCase(s: String): String =
        if (s.any { it.isLowerCase() }) s.trim()
        else s.trim().lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }
}

/** A barcode → wine mapping the user previously confirmed (their own data). */
data class BarcodeMapping(val code: String, val wineId: String, val vintageId: String?, val formatMl: Int?)

data class MatchCandidate(
    val existing: ExistingVintage,
    val score: Int,
    val vintageConfirmed: Boolean,
    val formatConfirmed: Boolean,
    val reasons: List<String>,
    val warnings: List<String>,
)

/**
 * Matches a scan against the collection. A barcode is treated as evidence for a *wine*, never
 * as proof of vintage or bottle size: those are confirmed only by independent label evidence
 * or by the user.
 */
object CandidateMatcher {

    fun match(
        suggestion: WineSuggestion,
        existing: List<ExistingVintage>,
        barcodeMappings: List<BarcodeMapping>,
        formatsByVintage: Map<String, Set<Int>> = emptyMap(),
        limit: Int = 5,
    ): List<MatchCandidate> {
        val code = suggestion.barcode
        val barcodeWines = barcodeMappings.filter { code != null && it.code == code }
        val out = ArrayList<MatchCandidate>()
        for (e in existing) {
            var score = 0
            val reasons = ArrayList<String>()
            val warnings = ArrayList<String>()
            val hit = barcodeWines.firstOrNull { it.wineId == e.wineId }
            if (hit != null) { score += 50; reasons += "Barcode previously linked to this wine" }

            val producerSim = suggestion.producer?.let { Fuzzy.similarity(it.value, e.producer) }
            val nameSim = suggestion.name?.let { s -> e.name?.let { Fuzzy.similarity(s.value, it) } }
            if (producerSim != null) {
                if (producerSim >= 80) { score += 30; reasons += "Producer matches" }
                else if (producerSim >= 50) { score += 10; reasons += "Producer partly matches" }
                else if (hit != null) warnings += "Label producer \"${suggestion.producer.value}\" differs from ${e.producer}"
            }
            if (nameSim != null && nameSim >= 70) { score += 15; reasons += "Wine name matches" }

            val vintageConfirmed = suggestion.vintage?.value == e.vintage && suggestion.vintage.confidence != Confidence.LOW
            when {
                suggestion.vintage == null -> if (hit != null) warnings += "Vintage not readable — confirm it"
                vintageConfirmed -> { score += 15; reasons += "Vintage ${e.vintage} on label" }
                suggestion.vintage.value == e.vintage -> { score += 5; reasons += "Vintage ${e.vintage} possibly on label" }
                else -> { score -= 20; warnings += "Label says ${suggestion.vintage.value}, this record is ${e.vintage}" }
            }
            val knownFormats = formatsByVintage[e.vintageId].orEmpty()
            val formatConfirmed = suggestion.formatMl != null && suggestion.formatMl.value in knownFormats
            if (suggestion.formatMl != null && knownFormats.isNotEmpty() && !formatConfirmed) {
                warnings += "Label says ${BottleFormats.formatVolume(suggestion.formatMl.value)}, which you don't have for this wine"
            }
            if (hit?.formatMl != null && suggestion.formatMl != null && hit.formatMl != suggestion.formatMl.value) {
                warnings += "Barcode was linked to ${BottleFormats.formatVolume(hit.formatMl)}"
            }
            if (score >= 30) out += MatchCandidate(e, score.coerceAtMost(100), vintageConfirmed, formatConfirmed, reasons, warnings)
        }
        return out.sortedWith(compareByDescending<MatchCandidate> { it.score }.thenBy { it.existing.producer }).take(limit)
    }
}
