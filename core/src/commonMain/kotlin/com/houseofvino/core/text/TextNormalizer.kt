package com.houseofvino.core.text

/**
 * Normalises text for search indexing and matching, identically on every platform:
 * lower-case, diacritics folded, ligatures expanded, punctuation split, common wine
 * abbreviations expanded. "Château d'Yquem" → "chateau d yquem"; "St-Émilion" → "saint emilion".
 */
object TextNormalizer {

    /** Normalised, space-separated tokens joined into one string. */
    fun normalize(text: String?): String = tokens(text).joinToString(" ")

    fun tokens(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        val folded = fold(text)
        val out = ArrayList<String>()
        val current = StringBuilder()
        fun flush() {
            if (current.isNotEmpty()) {
                val token = current.toString()
                out += ABBREVIATIONS[token] ?: token
                current.clear()
            }
        }
        for (c in folded) {
            if (c.isLetterOrDigit()) current.append(c) else flush()
        }
        flush()
        return out
    }

    /** Lower-cases and strips diacritics without tokenising. */
    fun fold(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val lower = ch.lowercaseChar()
            val mapped = FOLD[lower]
            if (mapped != null) sb.append(mapped)
            else if (lower in '̀'..'ͯ') continue // combining marks (decomposed input)
            else sb.append(lower)
        }
        return sb.toString()
    }

    /**
     * Whole-word abbreviations seen on labels and in exports. Applied to both indexed text and
     * queries, so "st emilion" and "saint emilion" match each other.
     */
    private val ABBREVIATIONS = mapOf(
        "st" to "saint", "ste" to "sainte", "mt" to "mount", "ch" to "chateau", "chteau" to "chateau",
        "dom" to "domaine", "cht" to "chateau",
    )

    private val FOLD: Map<Char, String> = buildMap {
        fun put(chars: String, replacement: String) = chars.forEach { put(it, replacement) }
        put("àáâãäåāăą", "a"); put("æ", "ae")
        put("çćĉċč", "c"); put("ďđð", "d")
        put("èéêëēĕėęě", "e"); put("ĝğġģ", "g"); put("ĥħ", "h")
        put("ìíîïĩīĭįı", "i"); put("ĵ", "j"); put("ķ", "k"); put("ĺļľŀł", "l")
        put("ñńņňŉ", "n"); put("òóôõöøōŏő", "o"); put("œ", "oe")
        put("ŕŗř", "r"); put("śŝşšș", "s"); put("ß", "ss"); put("ţťŧț", "t")
        put("ùúûüũūŭůűų", "u"); put("ŵ", "w"); put("ýÿŷ", "y"); put("źżž", "z"); put("þ", "th")
        // Typographic punctuation that should split tokens like ASCII equivalents.
        put("’‘`´ʼ", " "); put("–—‑", " ")
    }
}

/** Edit distance used for typo tolerance. */
object Fuzzy {

    /**
     * Optimal-string-alignment Damerau–Levenshtein distance, capped: returns [max] + 1 as soon as
     * the distance is known to exceed [max]. O(n·m) time, O(m) space.
     */
    fun distance(a: String, b: String, max: Int = Int.MAX_VALUE): Int {
        if (a == b) return 0
        if (kotlin.math.abs(a.length - b.length) > max) return max + 1
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prevPrev = IntArray(b.length + 1)
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    v = minOf(v, prevPrev[j - 2] + 1)
                }
                cur[j] = v
                if (v < rowMin) rowMin = v
            }
            if (rowMin > max) return max + 1
            val tmp = prevPrev
            prevPrev = prev
            prev = cur
            cur = tmp
        }
        return prev[b.length]
    }

    /** Typo budget by token length: none for very short tokens, 1 up to 5 chars, else 2. */
    fun allowedEdits(token: String): Int = when {
        token.length <= 3 -> 0
        token.length <= 5 -> 1
        else -> 2
    }

    data class Suggestion(val term: String, val distance: Int, val frequency: Int)

    /**
     * Picks the best replacements for [token] from [vocabulary] (term → document frequency).
     * Closer terms win; ties go to more frequent terms.
     */
    fun suggest(token: String, vocabulary: Map<String, Int>, limit: Int = 3): List<Suggestion> {
        val budget = allowedEdits(token)
        if (budget == 0) return emptyList()
        return vocabulary.entries.asSequence()
            .filter { kotlin.math.abs(it.key.length - token.length) <= budget && it.key != token }
            .map { Suggestion(it.key, distance(token, it.key, budget), it.value) }
            .filter { it.distance <= budget }
            .sortedWith(compareBy<Suggestion> { it.distance }.thenByDescending { it.frequency }.thenBy { it.term })
            .take(limit)
            .toList()
    }

    /** Token-set similarity in 0..100 used for duplicate detection (Jaccard over fuzzy-matched tokens). */
    fun similarity(a: String?, b: String?): Int {
        val ta = TextNormalizer.tokens(a).toSet()
        val tb = TextNormalizer.tokens(b).toSet()
        if (ta.isEmpty() && tb.isEmpty()) return 100
        if (ta.isEmpty() || tb.isEmpty()) return 0
        var matched = 0
        val remaining = tb.toMutableSet()
        for (t in ta) {
            val hit = remaining.firstOrNull { it == t } ?: remaining.firstOrNull { distance(t, it, allowedEdits(t)) <= allowedEdits(t) }
            if (hit != null) {
                matched++
                remaining.remove(hit)
            }
        }
        val union = ta.size + tb.size - matched
        return matched * 100 / union
    }
}
