package com.houseofvino.core.model

import com.houseofvino.core.money.Money
import com.houseofvino.core.time.IsoDate

enum class WineColor { RED, WHITE, ROSE, ORANGE }

enum class WineStyle { STILL, SPARKLING, FORTIFIED, SWEET }

/** The identity of a wine independent of vintage: producer plus cuvée. */
data class Wine(
    val id: String,
    val collectionId: String,
    val producer: String,
    val name: String?,
    val color: WineColor?,
    val style: WineStyle?,
    val country: String?,
    val region: String?,
    val subRegion: String?,
    val appellation: String?,
    val classification: String?,
    val grapes: List<GrapeShare>,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    /** "Château Margaux" or "Domaine Leflaive Puligny-Montrachet Les Pucelles". */
    val displayName: String get() = if (name.isNullOrBlank()) producer else "$producer $name"
}

data class GrapeShare(val grape: String, val percent: Int?) {
    init {
        require(grape.isNotBlank()) { "Grape name is required" }
        require(percent == null || percent in 0..100) { "Grape percent must be 0–100" }
    }
}

/**
 * Vintage designation. "Unknown" (the year exists but nobody recorded it) is distinct from
 * "Non-vintage" (a blend of years by design, e.g. most Champagne).
 */
sealed interface VintageYear : Comparable<VintageYear> {
    data class Year(val year: Int) : VintageYear {
        init { require(year in MIN_YEAR..MAX_YEAR) { "Vintage year out of range: $year" } }
        override fun toString() = year.toString()
    }
    data object NonVintage : VintageYear { override fun toString() = "NV" }
    data object Unknown : VintageYear { override fun toString() = "Unknown" }

    /** Sort order: years ascending, then NV, then Unknown. */
    override fun compareTo(other: VintageYear): Int = sortKey().compareTo(other.sortKey())

    val kind: String get() = when (this) { is Year -> "YEAR"; NonVintage -> "NV"; Unknown -> "UNKNOWN" }
    val yearOrNull: Int? get() = (this as? Year)?.year

    companion object {
        const val MIN_YEAR = 1700
        const val MAX_YEAR = 2200

        fun fromStorage(kind: String, year: Int?): VintageYear = when (kind) {
            "YEAR" -> Year(requireNotNull(year) { "YEAR vintage without year" })
            "NV" -> NonVintage
            "UNKNOWN" -> Unknown
            else -> throw IllegalArgumentException("Unknown vintage kind: $kind")
        }

        /** Parses user/CSV text: "2015", "NV", "N.V.", "non-vintage", "" (unknown). */
        fun parse(text: String?): VintageYear? {
            val t = text?.trim().orEmpty()
            if (t.isEmpty() || t == "?" || t.equals("unknown", true)) return Unknown
            val compact = t.replace(".", "").replace("-", "").replace(" ", "").lowercase()
            if (compact == "nv" || compact == "nonvintage" || compact == "sa" /* sans année */ || compact == "mv") {
                return NonVintage
            }
            val year = t.toIntOrNull() ?: return null
            return if (year in MIN_YEAR..MAX_YEAR) Year(year) else null
        }
    }
}

private fun VintageYear.sortKey(): Int = when (this) {
    is VintageYear.Year -> year
    VintageYear.NonVintage -> VintageYear.MAX_YEAR + 1
    VintageYear.Unknown -> VintageYear.MAX_YEAR + 2
}

/** One vintage of a wine. Tastings, valuations and stock all attach here. */
data class Vintage(
    val id: String,
    val collectionId: String,
    val wineId: String,
    val vintage: VintageYear,
    val drinkFrom: Int?,
    val drinkUntil: Int?,
    val drinkWindowSource: String?,
    val abvBasisPoints: Int?,
    val notes: String?,
    val labelPhotoId: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    init {
        require(drinkFrom == null || drinkUntil == null || drinkFrom <= drinkUntil) {
            "Drinking window must start before it ends"
        }
        require(abvBasisPoints == null || abvBasisPoints in 0..8000) { "ABV out of range" }
    }
}

/** Standard bottle sizes. Any positive millilitre value is allowed; these get names. */
object BottleFormats {
    data class Format(val ml: Int, val name: String)

    val STANDARD = 750

    val named: List<Format> = listOf(
        Format(187, "Piccolo"),
        Format(375, "Half bottle"),
        Format(500, "50 cl"),
        Format(620, "Clavelin"),
        Format(750, "Bottle"),
        Format(1000, "Litre"),
        Format(1500, "Magnum"),
        Format(3000, "Double magnum"),
        Format(4500, "Rehoboam"),
        Format(5000, "Jeroboam (5 L)"),
        Format(6000, "Imperial"),
        Format(9000, "Salmanazar"),
        Format(12000, "Balthazar"),
        Format(15000, "Nebuchadnezzar"),
    )

    fun nameFor(ml: Int): String = named.firstOrNull { it.ml == ml }?.name ?: formatVolume(ml)

    fun formatVolume(ml: Int): String = when {
        ml >= 1000 && ml % 100 == 0 -> {
            val litres = ml / 1000
            val tenths = (ml % 1000) / 100
            if (tenths == 0) "$litres L" else "$litres.$tenths L"
        }
        ml % 10 == 0 -> "${ml / 10} cl"
        else -> "$ml ml"
    }

    /** Bottle-equivalents relative to 750 ml, in hundredths (a magnum = 200). */
    fun standardEquivalentsCentis(ml: Int, quantity: Int): Long = ml.toLong() * quantity * 100 / STANDARD

    private val VOLUME = Regex("""(\d+(?:[.,]\d+)?)\s*(ml|cl|l|ltr|litre|liter|litres|liters)\b""", RegexOption.IGNORE_CASE)

    /** Extracts a bottle volume from text like "750 ml", "75cl", "1,5 L". */
    fun parseVolume(text: String): Int? {
        val m = VOLUME.find(text) ?: return null
        val numberText = m.groupValues[1].replace(',', '.')
        val unit = m.groupValues[2].lowercase()
        val (whole, frac) = numberText.split('.').let { it[0] to it.getOrElse(1) { "" } }
        val wholeN = whole.toIntOrNull() ?: return null
        val fracDigits = frac.take(3).padEnd(3, '0').toIntOrNull() ?: 0
        val millis = wholeN * 1000 + fracDigits // value × 1000
        val ml = when (unit) {
            "ml" -> millis / 1000
            "cl" -> millis / 100
            else -> millis // litres → ml
        }
        return ml.takeIf { it in 50..30000 }
    }
}

enum class Packaging { LOOSE, OWC, OC, CARTON }

/** An acquisition batch: what was bought, when, from whom and at what unit price. */
data class Lot(
    val id: String,
    val collectionId: String,
    val vintageId: String,
    val formatMl: Int,
    val acquiredOn: IsoDate?,
    val source: String?,
    val orderRef: String?,
    val unitPrice: Money?,
    val packaging: Packaging?,
    val bottlesPerCase: Int?,
    val wishlistItemId: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    init {
        require(formatMl > 0) { "Bottle format must be positive" }
        require(unitPrice == null || !unitPrice.isNegative) { "Unit price cannot be negative" }
        require(bottlesPerCase == null || bottlesPerCase > 0) { "Bottles per case must be positive" }
    }
}

enum class LocationKind { CELLAR, FRIDGE, RACK, SHELF, BIN, CASE, BOX, OTHER }

data class Location(
    val id: String,
    val collectionId: String,
    val parentId: String?,
    val name: String,
    val kind: LocationKind,
    val gridRows: Int?,
    val gridCols: Int?,
    val sortOrder: Int,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    init {
        require(name.isNotBlank()) { "Location name is required" }
        require((gridRows == null) == (gridCols == null)) { "Grid needs both rows and columns" }
        require(gridRows == null || (gridRows in 1..200 && gridCols!! in 1..200)) { "Grid size out of range" }
    }
    val hasGrid: Boolean get() = gridRows != null
}

/** Utilities for location trees (parent links). */
object LocationTree {
    /** Returns true if making [newParentId] the parent of [locationId] would create a cycle. */
    fun wouldCreateCycle(locationId: String, newParentId: String?, parentOf: (String) -> String?): Boolean {
        var cursor = newParentId
        var guard = 0
        while (cursor != null) {
            if (cursor == locationId) return true
            cursor = parentOf(cursor)
            if (++guard > 10_000) return true
        }
        return false
    }

    /** "Main cellar › Rack A › Shelf 3". */
    fun path(locationId: String, byId: (String) -> Location?): List<Location> {
        val out = ArrayList<Location>()
        var cursor: String? = locationId
        var guard = 0
        while (cursor != null && guard++ < 64) {
            val loc = byId(cursor) ?: break
            out += loc
            cursor = loc.parentId
        }
        return out.asReversed()
    }
}
