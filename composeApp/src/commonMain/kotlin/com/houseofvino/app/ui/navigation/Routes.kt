package com.houseofvino.app.ui.navigation

import kotlinx.serialization.Serializable

// Top-level tabs ----------------------------------------------------------------------------------
@Serializable data object CellarRoute
@Serializable data object SearchRoute
@Serializable data object RacksRoute
@Serializable data object WishlistRoute
@Serializable data object MoreRoute

// Cellar --------------------------------------------------------------------------------------------
@Serializable data class VintageRoute(val vintageId: String)

/** New wine (all ids null), new vintage of a wine ([wineId]), or edit ([editVintageId]). [prefill] is a JSON WineSuggestion. */
@Serializable data class WineEditorRoute(
    val wineId: String? = null,
    val editVintageId: String? = null,
    val prefill: String? = null,
    val fromWishlistItemId: String? = null,
)

@Serializable data class AddLotRoute(val vintageId: String, val wishlistItemId: String? = null)
@Serializable data class StockActionRoute(val vintageId: String, val action: String)
@Serializable data class HistoryRoute(val vintageId: String)
@Serializable data class ValuationsRoute(val vintageId: String)
@Serializable data class AddChooserRoute(val unused: Int = 0)
@Serializable data class ScanRoute(val mode: String)

// Wishlist ------------------------------------------------------------------------------------------
@Serializable data class WishlistEditorRoute(val itemId: String? = null, val vintageId: String? = null)
@Serializable data class WishlistDetailRoute(val itemId: String)

// More ----------------------------------------------------------------------------------------------
@Serializable data object LocationsRoute
@Serializable data class LocationDetailRoute(val locationId: String)
/** [withGrid] starts a new location with rows and positions switched on (from the Racks tab). */
@Serializable data class LocationEditorRoute(val locationId: String? = null, val parentId: String? = null, val withGrid: Boolean = false)
@Serializable data object InsightsRoute
@Serializable data object ImportRoute
@Serializable data object ExportRoute
@Serializable data object SettingsRoute
@Serializable data object SharingRoute
@Serializable data object InviteRoute
@Serializable data object JoinRoute
@Serializable data object ConflictsRoute
@Serializable data object RatesRoute
@Serializable data object PrivacyRoute
@Serializable data object EraseRoute
@Serializable data object FirstRunRoute
