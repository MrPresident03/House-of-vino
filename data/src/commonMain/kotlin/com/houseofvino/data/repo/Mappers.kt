package com.houseofvino.data.repo

import com.houseofvino.core.inventory.Movement
import com.houseofvino.core.inventory.MovementKind
import com.houseofvino.core.model.GrapeShare
import com.houseofvino.core.model.Level
import com.houseofvino.core.model.Location
import com.houseofvino.core.model.LocationKind
import com.houseofvino.core.model.Lot
import com.houseofvino.core.model.MerchantLink
import com.houseofvino.core.model.Packaging
import com.houseofvino.core.model.StructuredNotes
import com.houseofvino.core.model.Tag
import com.houseofvino.core.model.Tasting
import com.houseofvino.core.model.Valuation
import com.houseofvino.core.model.Vintage
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.Wine
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.model.WineStyle
import com.houseofvino.core.model.WishlistItem
import com.houseofvino.core.model.WishlistPriority
import com.houseofvino.core.model.WishlistStatus
import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.ExchangeRate
import com.houseofvino.core.money.Money
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.db.Exchange_rate
import com.houseofvino.data.db.Wine_grape
import com.houseofvino.data.db.Wishlist_item
import com.houseofvino.data.db.Location as LocationRow
import com.houseofvino.data.db.Lot as LotRow
import com.houseofvino.data.db.Merchant_link as MerchantLinkRow
import com.houseofvino.data.db.Movement as MovementRow
import com.houseofvino.data.db.Tag as TagRow
import com.houseofvino.data.db.Tasting as TastingRow
import com.houseofvino.data.db.Valuation as ValuationRow
import com.houseofvino.data.db.Vintage as VintageRow
import com.houseofvino.data.db.Wine as WineRow

internal fun money(minor: Long?, currency: String?): Money? =
    if (minor == null || currency == null) null else Money(minor, CurrencyCode.ofOrNull(currency) ?: CurrencyCode.of(currency, 2))

internal fun WineRow.toDomain(grapes: List<Wine_grape>) = Wine(
    id = id, collectionId = collection_id, producer = producer, name = name,
    color = color?.let { WineColor.valueOf(it) }, style = style?.let { WineStyle.valueOf(it) },
    country = country, region = region, subRegion = sub_region, appellation = appellation,
    classification = classification, grapes = grapes.map { GrapeShare(it.grape, it.percent?.toInt()) },
    notes = notes, createdAt = created_at, updatedAt = updated_at,
)

internal fun VintageRow.toDomain() = Vintage(
    id = id, collectionId = collection_id, wineId = wine_id,
    vintage = VintageYear.fromStorage(vintage_kind, year?.toInt()),
    drinkFrom = drink_from?.toInt(), drinkUntil = drink_until?.toInt(), drinkWindowSource = drink_window_source,
    abvBasisPoints = abv_bp?.toInt(), notes = notes, labelPhotoId = label_photo_id,
    createdAt = created_at, updatedAt = updated_at,
)

internal fun LotRow.toDomain() = Lot(
    id = id, collectionId = collection_id, vintageId = vintage_id, formatMl = format_ml.toInt(),
    acquiredOn = IsoDate.parseOrNull(acquired_on), source = source, orderRef = order_ref,
    unitPrice = money(unit_price_minor, unit_price_currency), packaging = packaging?.let { Packaging.valueOf(it) },
    bottlesPerCase = bottles_per_case?.toInt(), wishlistItemId = wishlist_item_id, notes = notes,
    createdAt = created_at, updatedAt = updated_at,
)

internal fun LocationRow.toDomain() = Location(
    id = id, collectionId = collection_id, parentId = parent_id, name = name, kind = LocationKind.valueOf(kind),
    gridRows = grid_rows?.toInt(), gridCols = grid_cols?.toInt(), sortOrder = sort_order.toInt(), notes = notes,
    createdAt = created_at, updatedAt = updated_at,
)

internal fun MovementRow.toDomain() = Movement(
    id = id, collectionId = collection_id, lotId = lot_id, locationId = location_id, delta = delta.toInt(),
    kind = MovementKind.valueOf(kind), occurredAt = occurred_at, transferGroupId = transfer_group_id,
    reversalOf = reversal_of, amount = money(amount_minor, amount_currency), counterparty = counterparty,
    note = note, deviceId = device_id, tastingId = tasting_id, createdAt = created_at,
)

internal fun TastingRow.toDomain() = Tasting(
    id = id, collectionId = collection_id, vintageId = vintage_id, tastedOn = IsoDate.parse(tasted_on),
    rating = rating?.toInt(),
    structured = StructuredNotes(
        appearance = appearance.level(), noseIntensity = nose_intensity.level(), sweetness = sweetness.level(),
        acidity = acidity.level(), tannin = tannin.level(), alcohol = alcohol.level(), body = body.level(),
        flavourIntensity = flavour_intensity.level(), finish = finish.level(),
        aromas = aromas?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
    ),
    notes = notes, occasion = occasion, foodPairing = food_pairing, companions = companions, place = place,
    formatMl = format_ml?.toInt(), movementId = movement_id, wouldBuyAgain = would_buy_again?.let { it != 0L },
    createdAt = created_at, updatedAt = updated_at,
)

private fun String?.level(): Level? = this?.let { runCatching { Level.valueOf(it) }.getOrNull() }

internal fun Wishlist_item.toDomain() = WishlistItem(
    id = id, collectionId = collection_id, vintageId = vintage_id, wineId = wine_id, producer = producer, name = name,
    vintage = vintage_kind?.let { VintageYear.fromStorage(it, year?.toInt()) }, formatMl = format_ml?.toInt(),
    desiredQuantity = desired_qty.toInt(), targetPrice = money(target_price_minor, target_price_currency),
    priority = WishlistPriority.valueOf(priority), status = WishlistStatus.valueOf(status), notes = notes,
    acquiredLotId = acquired_lot_id, createdAt = created_at, updatedAt = updated_at,
)

internal fun MerchantLinkRow.toDomain() = MerchantLink(
    id = id, wishlistItemId = wishlist_item_id, merchant = merchant, url = url,
    lastPrice = money(last_price_minor, last_price_currency), checkedOn = IsoDate.parseOrNull(checked_on),
)

internal fun ValuationRow.toDomain() = Valuation(
    id = id, collectionId = collection_id, vintageId = vintage_id, formatMl = format_ml.toInt(),
    amount = money(amount_minor, amount_currency)!!, source = source, asOf = IsoDate.parse(as_of), note = note,
)

internal fun TagRow.toDomain() = Tag(id = id, collectionId = collection_id, name = name, color = color)

internal fun Exchange_rate.toDomain(): ExchangeRate? = runCatching {
    ExchangeRate(CurrencyCode.of(base), CurrencyCode.of(quote), rate_e8, IsoDate.parse(as_of), source)
}.getOrNull()
