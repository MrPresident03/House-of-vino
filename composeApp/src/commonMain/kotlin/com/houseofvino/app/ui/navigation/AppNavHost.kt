package com.houseofvino.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.houseofvino.app.platform.ScanMode
import com.houseofvino.app.ui.cellar.AddChooserScreen
import com.houseofvino.app.ui.cellar.AddLotScreen
import com.houseofvino.app.ui.cellar.CellarScreen
import com.houseofvino.app.ui.cellar.HistoryScreen
import com.houseofvino.app.ui.cellar.StockActionScreen
import com.houseofvino.app.ui.cellar.ValuationsScreen
import com.houseofvino.app.ui.cellar.VintageDetailScreen
import com.houseofvino.app.ui.cellar.WineEditorScreen
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.importexport.ExportScreen
import com.houseofvino.app.ui.importexport.ImportScreen
import com.houseofvino.app.ui.insights.InsightsScreen
import com.houseofvino.app.ui.locations.LocationDetailScreen
import com.houseofvino.app.ui.locations.LocationEditorScreen
import com.houseofvino.app.ui.locations.LocationsScreen
import com.houseofvino.app.ui.locations.RacksScreen
import com.houseofvino.app.ui.scan.ScanScreen
import com.houseofvino.app.ui.search.SearchScreen
import com.houseofvino.app.ui.settings.ConflictsScreen
import com.houseofvino.app.ui.settings.EraseScreen
import com.houseofvino.app.ui.settings.FirstRunScreen
import com.houseofvino.app.ui.settings.MoreScreen
import com.houseofvino.app.ui.settings.PrivacyScreen
import com.houseofvino.app.ui.settings.RatesScreen
import com.houseofvino.app.ui.settings.SettingsScreen
import com.houseofvino.app.ui.sharing.InviteScreen
import com.houseofvino.app.ui.sharing.JoinScreen
import com.houseofvino.app.ui.sharing.SharingScreen
import com.houseofvino.app.ui.wishlist.WishlistDetailScreen
import com.houseofvino.app.ui.wishlist.WishlistEditorScreen
import com.houseofvino.app.ui.wishlist.WishlistScreen
import com.houseofvino.core.scan.Confidence
import com.houseofvino.core.scan.Suggested
import com.houseofvino.core.scan.WineSuggestion

@Composable
fun AppNavHost(nav: NavHostController, start: Any, modifier: Modifier = Modifier) {
    val graph = appGraph()
    val back: () -> Unit = { nav.popBackStack() }
    fun openVintage(id: String) = nav.go(VintageRoute(id))

    NavHost(nav, startDestination = start, modifier = modifier) {
        // ---- First run --------------------------------------------------------------------------
        composable<FirstRunRoute> {
            FirstRunScreen(
                onDone = { nav.navigate(CellarRoute) { launchSingleTop = true; popUpTo<FirstRunRoute> { inclusive = true } } },
                onJoin = { nav.navigate(CellarRoute) { launchSingleTop = true; popUpTo<FirstRunRoute> { inclusive = true } }; nav.go(JoinRoute) },
            )
        }

        // ---- Tabs -------------------------------------------------------------------------------
        composable<CellarRoute> {
            CellarScreen(onOpen = ::openVintage, onAdd = { nav.go(AddChooserRoute()) }, onSearch = { nav.go(SearchRoute) })
        }
        composable<SearchRoute> {
            SearchScreen(
                onOpenVintage = ::openVintage,
                onOpenWishlist = { nav.go(WishlistDetailRoute(it)) },
                onAddFromDiscover = { s -> nav.go(WineEditorRoute(prefill = graph.handoff.put(s))) },
            )
        }
        composable<RacksRoute> {
            RacksScreen(
                onOpenLocation = { nav.go(LocationDetailRoute(it)) },
                onEditLocation = { nav.go(LocationEditorRoute(locationId = it)) },
                onAddRack = { nav.go(LocationEditorRoute(withGrid = true)) },
                onManageLocations = { nav.go(LocationsRoute) },
                onOpenVintage = ::openVintage,
            )
        }
        composable<WishlistRoute> {
            WishlistScreen(onOpen = { nav.go(WishlistDetailRoute(it)) }, onAdd = { nav.go(WishlistEditorRoute()) })
        }
        composable<MoreRoute> {
            MoreScreen(
                onLocations = { nav.go(LocationsRoute) }, onInsights = { nav.go(InsightsRoute) },
                onImport = { nav.go(ImportRoute) }, onExport = { nav.go(ExportRoute) },
                onSharing = { nav.go(SharingRoute) }, onSettings = { nav.go(SettingsRoute) },
            )
        }

        // ---- Cellar -----------------------------------------------------------------------------
        composable<AddChooserRoute> {
            AddChooserScreen(
                onBack = back,
                onManual = { nav.navigate(WineEditorRoute()) { launchSingleTop = true; popUpTo<AddChooserRoute> { inclusive = true } } },
                onScanBarcode = { nav.navigate(ScanRoute(ScanMode.BARCODE.name)) { launchSingleTop = true; popUpTo<AddChooserRoute> { inclusive = true } } },
                onScanLabel = { nav.navigate(ScanRoute(ScanMode.LABEL.name)) { launchSingleTop = true; popUpTo<AddChooserRoute> { inclusive = true } } },
                onImport = { nav.navigate(ImportRoute) { launchSingleTop = true; popUpTo<AddChooserRoute> { inclusive = true } } },
            )
        }
        composable<ScanRoute> { entry ->
            val r = entry.toRoute<ScanRoute>()
            ScanScreen(
                mode = ScanMode.valueOf(r.mode), onBack = back,
                onUseDetails = { key -> nav.navigate(WineEditorRoute(prefill = key)) { launchSingleTop = true; popUpTo<ScanRoute> { inclusive = true } } },
                onOpenExisting = { id -> nav.navigate(VintageRoute(id)) { launchSingleTop = true; popUpTo<ScanRoute> { inclusive = true } } },
                onNewVintageOf = { wineId, key -> nav.navigate(WineEditorRoute(wineId = wineId, prefill = key)) { launchSingleTop = true; popUpTo<ScanRoute> { inclusive = true } } },
                onManual = { nav.navigate(WineEditorRoute()) { launchSingleTop = true; popUpTo<ScanRoute> { inclusive = true } } },
            )
        }
        composable<VintageRoute> { entry ->
            val id = entry.toRoute<VintageRoute>().vintageId
            VintageDetailScreen(
                vintageId = id, onBack = back,
                onEdit = { nav.go(WineEditorRoute(editVintageId = it)) },
                onStockAction = { nav.go(StockActionRoute(id, it)) },
                onAddLot = { nav.go(AddLotRoute(id)) },
                onHistory = { nav.go(HistoryRoute(id)) },
                onValuations = { nav.go(ValuationsRoute(id)) },
                // Plain navigate: related vintages must stack (singleTop would replace the current vintage).
                onOpenVintage = { nav.navigate(VintageRoute(it)) },
                onAddVintage = { wineId -> nav.go(WineEditorRoute(wineId = wineId)) },
                onAddToWishlist = { nav.go(WishlistEditorRoute(vintageId = id)) },
            )
        }
        composable<WineEditorRoute> { entry ->
            val r = entry.toRoute<WineEditorRoute>()
            WineEditorScreen(r.wineId, r.editVintageId, r.prefill, r.fromWishlistItemId, onBack = back, onSaved = { vintageId ->
                if (r.editVintageId != null) nav.popBackStack()
                else nav.navigate(VintageRoute(vintageId)) { launchSingleTop = true; popUpTo<WineEditorRoute> { inclusive = true } }
            })
        }
        composable<AddLotRoute> { entry ->
            val r = entry.toRoute<AddLotRoute>()
            AddLotScreen(r.vintageId, r.wishlistItemId, onBack = back, onSaved = back)
        }
        composable<StockActionRoute> { entry ->
            val r = entry.toRoute<StockActionRoute>()
            StockActionScreen(r.vintageId, r.action, onBack = back, onDone = back)
        }
        composable<HistoryRoute> { entry -> HistoryScreen(entry.toRoute<HistoryRoute>().vintageId, onBack = back) }
        composable<ValuationsRoute> { entry -> ValuationsScreen(entry.toRoute<ValuationsRoute>().vintageId, onBack = back) }

        // ---- Wishlist ---------------------------------------------------------------------------
        composable<WishlistEditorRoute> { entry ->
            val r = entry.toRoute<WishlistEditorRoute>()
            WishlistEditorScreen(r.itemId, r.vintageId, onBack = back, onSaved = { id ->
                if (r.itemId != null) nav.popBackStack() else nav.navigate(WishlistDetailRoute(id)) { launchSingleTop = true; popUpTo<WishlistEditorRoute> { inclusive = true } }
            })
        }
        composable<WishlistDetailRoute> { entry ->
            val id = entry.toRoute<WishlistDetailRoute>().itemId
            WishlistDetailScreen(id, onBack = back, onEdit = { nav.go(WishlistEditorRoute(itemId = id)) }, onAddToCellar = { item ->
                val itemVintage = item.vintageId
                when {
                    itemVintage != null -> nav.go(AddLotRoute(itemVintage, wishlistItemId = item.id))
                    else -> {
                        val suggestion = WineSuggestion(
                            producer = item.producer?.let { Suggested(it, Confidence.HIGH, "wishlist") },
                            name = item.name?.let { Suggested(it, Confidence.HIGH, "wishlist") },
                            vintage = item.vintage?.let { Suggested(it, Confidence.HIGH, "wishlist") },
                            formatMl = item.formatMl?.let { Suggested(it, Confidence.HIGH, "wishlist") },
                        )
                        nav.go(WineEditorRoute(wineId = item.wineId, prefill = graph.handoff.put(suggestion), fromWishlistItemId = item.id))
                    }
                }
            })
        }

        // ---- More -------------------------------------------------------------------------------
        composable<LocationsRoute> {
            LocationsScreen(onBack = back, onOpen = { nav.go(LocationDetailRoute(it)) }, onAdd = { nav.go(LocationEditorRoute(parentId = it)) })
        }
        composable<LocationDetailRoute> { entry ->
            val id = entry.toRoute<LocationDetailRoute>().locationId
            LocationDetailScreen(id, onBack = back, onEdit = { nav.go(LocationEditorRoute(locationId = id)) },
                onAddChild = { nav.go(LocationEditorRoute(parentId = id)) }, onOpenVintage = ::openVintage)
        }
        composable<LocationEditorRoute> { entry ->
            val r = entry.toRoute<LocationEditorRoute>()
            LocationEditorScreen(r.locationId, r.parentId, onBack = back, onSaved = { back() }, withGrid = r.withGrid)
        }
        composable<InsightsRoute> { InsightsScreen(onBack = back) }
        composable<ImportRoute> { ImportScreen(onBack = back, onDone = { if (!nav.popBackStack(CellarRoute, inclusive = false)) nav.go(CellarRoute) }) }
        composable<ExportRoute> { ExportScreen(onBack = back) }
        composable<SharingRoute> {
            SharingScreen(onBack = back, onInvite = { nav.go(InviteRoute) }, onJoin = { nav.go(JoinRoute) },
                onConflicts = { nav.go(ConflictsRoute) })
        }
        composable<InviteRoute> { InviteScreen(onBack = back) }
        composable<JoinRoute> {
            JoinScreen(onBack = back, onJoined = { nav.navigate(SharingRoute) { launchSingleTop = true; popUpTo<JoinRoute> { inclusive = true } } })
        }
        composable<ConflictsRoute> { ConflictsScreen(onBack = back, onOpenVintage = ::openVintage) }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = back, onSharing = { nav.go(SharingRoute) },
                onRates = { nav.go(RatesRoute) }, onPrivacy = { nav.go(PrivacyRoute) }, onExport = { nav.go(ExportRoute) },
                onConflicts = { nav.go(ConflictsRoute) }, onErase = { nav.go(EraseRoute) },
            )
        }
        composable<RatesRoute> { RatesScreen(onBack = back) }
        composable<PrivacyRoute> { PrivacyScreen(onBack = back) }
        composable<EraseRoute> {
            EraseScreen(onBack = back, onExport = { nav.go(ExportRoute) }, onErased = {
                nav.navigate(FirstRunRoute) { launchSingleTop = true; popUpTo(0) { inclusive = true } }
            })
        }
    }
}

/** Navigates with launchSingleTop so a double tap doesn't push the same screen twice. */
private fun NavHostController.go(route: Any) = navigate(route) { launchSingleTop = true }
