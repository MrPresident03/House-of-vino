package com.houseofvino.core.model

import com.houseofvino.core.money.Money
import com.houseofvino.core.time.IsoDate

/** Five-step structured scale (WSET-style) for tasting attributes. */
enum class Level { LOW, MEDIUM_MINUS, MEDIUM, MEDIUM_PLUS, HIGH }

data class StructuredNotes(
    val appearance: Level? = null,
    val noseIntensity: Level? = null,
    val sweetness: Level? = null,
    val acidity: Level? = null,
    val tannin: Level? = null,
    val alcohol: Level? = null,
    val body: Level? = null,
    val flavourIntensity: Level? = null,
    val finish: Level? = null,
    val aromas: List<String> = emptyList(),
) {
    val isEmpty: Boolean
        get() = listOf(appearance, noseIntensity, sweetness, acidity, tannin, alcohol, body, flavourIntensity, finish)
            .all { it == null } && aromas.isEmpty()
}

/** Personal rating, stored on a 0–100 scale; displayed in the user's preferred scale. */
enum class RatingScale { HUNDRED, TWENTY, FIVE_STARS }

object Ratings {
    fun validate(rating: Int?) {
        require(rating == null || rating in 0..100) { "Rating must be 0–100" }
    }

    /** Display text: "94", "17.5/20" or "4.5★". Five-star uses half stars (10 points each). */
    fun display(rating: Int, scale: RatingScale): String = when (scale) {
        RatingScale.HUNDRED -> rating.toString()
        RatingScale.TWENTY -> {
            val tenths = rating * 2 // rating/5 → in tenths: rating*10/5
            val whole = tenths / 10
            val frac = tenths % 10
            (if (frac == 0) "$whole" else "$whole.$frac") + "/20"
        }
        RatingScale.FIVE_STARS -> {
            val halfStars = (rating + 5) / 10 // nearest half star
            val whole = halfStars / 2
            (if (halfStars % 2 == 0) "$whole" else "$whole.5") + "★"
        }
    }

    /** Converts a value entered in [scale] units ×10 (e.g. 175 for 17.5/20, 45 for 4.5★) to 0–100. */
    fun fromScaleTenths(valueTimesTen: Int, scale: RatingScale): Int = when (scale) {
        RatingScale.HUNDRED -> valueTimesTen / 10
        RatingScale.TWENTY -> valueTimesTen / 2 // (v/10)*5
        RatingScale.FIVE_STARS -> valueTimesTen * 2 // (v/10)*20
    }.coerceIn(0, 100)
}

data class Tasting(
    val id: String,
    val collectionId: String,
    val vintageId: String,
    val tastedOn: IsoDate,
    val rating: Int?,
    val structured: StructuredNotes,
    val notes: String?,
    val occasion: String?,
    val foodPairing: String?,
    val companions: String?,
    val place: String?,
    val formatMl: Int?,
    val movementId: String?,
    val wouldBuyAgain: Boolean?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    init { Ratings.validate(rating) }
}

enum class WishlistPriority { LOW, MEDIUM, HIGH, MUST }

enum class WishlistStatus {
    WANTED, WATCHING, ORDERED, ACQUIRED, CANCELLED;

    val isOpen: Boolean get() = this == WANTED || this == WATCHING || this == ORDERED

    fun canTransitionTo(next: WishlistStatus): Boolean = when (this) {
        WANTED, WATCHING -> next != this
        ORDERED -> next == ACQUIRED || next == CANCELLED || next == WANTED
        ACQUIRED -> next == WANTED // re-open, e.g. "buy again"
        CANCELLED -> next == WANTED || next == WATCHING
    }
}

/**
 * Something the user wants to buy. It can point at a known vintage, at a wine (any vintage),
 * or be free-form text for a wine not yet in the collection.
 */
data class WishlistItem(
    val id: String,
    val collectionId: String,
    val vintageId: String?,
    val wineId: String?,
    val producer: String?,
    val name: String?,
    val vintage: VintageYear?,
    val formatMl: Int?,
    val desiredQuantity: Int,
    val targetPrice: Money?,
    val priority: WishlistPriority,
    val status: WishlistStatus,
    val notes: String?,
    val acquiredLotId: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    init {
        require(vintageId != null || wineId != null || !producer.isNullOrBlank()) {
            "A wishlist item needs a wine or at least a producer"
        }
        require(desiredQuantity >= 1) { "Desired quantity must be at least 1" }
        require(targetPrice == null || !targetPrice.isNegative) { "Target price cannot be negative" }
    }
}

data class MerchantLink(
    val id: String,
    val wishlistItemId: String,
    val merchant: String,
    val url: String?,
    val lastPrice: Money?,
    val checkedOn: IsoDate?,
) {
    init {
        require(merchant.isNotBlank()) { "Merchant name is required" }
        require(url == null || url.startsWith("https://") || url.startsWith("http://")) { "Links must be http(s) URLs" }
    }

    /** True when the last seen price is at or below the target (same currency only). */
    fun meetsTarget(target: Money?): Boolean =
        target != null && lastPrice != null && lastPrice.currency == target.currency && lastPrice <= target
}

/** An estimated market value. Source, currency and date are mandatory (spec §4). */
data class Valuation(
    val id: String,
    val collectionId: String,
    val vintageId: String,
    val formatMl: Int,
    val amount: Money,
    val source: String,
    val asOf: IsoDate,
    val note: String?,
) {
    init {
        require(source.isNotBlank()) { "Valuation source is required" }
        require(!amount.isNegative) { "Valuation cannot be negative" }
    }
}

data class Tag(val id: String, val collectionId: String, val name: String, val color: String?) {
    init { require(name.isNotBlank() && name.length <= 40) { "Tag names are 1–40 characters" } }
}
