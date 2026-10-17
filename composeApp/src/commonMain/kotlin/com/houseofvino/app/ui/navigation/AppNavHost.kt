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
import com.houseofvino.app.ui.journal.JournalScreen
import com.houseofvino.app.ui.journal.TastingDetailScreen
import com.houseofvino.app.ui.journal.TastingEditorScreen
import com.houseofvino.app.ui.locations.LocationDetailScreen
import com.houseofvino.app.ui.locations.LocationEditorScreen
import com.houseofvino.app.ui.locations.LocationsScreen
import com.houseofvino.app.ui.scan.ScanScreen
import com.houseofvino.app.ui.search.SearchScreen
import com.houseofvino.app.ui.settings.ConflictsScreen
import com.houseofvino.app.ui.settings.EraseScreen
import com.houseofvino.app.ui.settings.FirstRunScreen
import com.houseofvino.app.ui.settings.MoreScreen
import com.houseofvino.app.ui.settings.PickWineScreen
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
    fun openVintage(id: String) = nav.navigate(VintageRoute(id))

    NavHost(nav, startDestination = start, modifier = modifier) {
        // ---- First run --------------------------------------------------------------------------
        composable<FirstRunRoute> {
            FirstRunScreen(
                onDone = { nav.navigate(CellarRoute) { popUpTo<FirstRunRoute> { inclusive = true } } },
                onJoin = { nav.navigate(CellarRoute) { popUpTo<FirstRunRoute> { inclusive = true } }; nav.navigate(JoinRoute) },
            )
        }

        // ---- Tabs -------------------------------------------------------------------------------
        composable<CellarRoute> {
            CellarScreen(onOpen = ::openVintage, onAdd = { nav.navigate(AddChooserRoute()) }, onSearch = { nav.navigate(SearchRoute) })
        }
        composable<SearchRoute> {
            SearchScreen(
                onOpenVintage = ::openVintage,
                onOpenWishlist = { nav.navigate(WishlistDetailRoute(it)) },
                onOpenTasting = { nav.navigate(TastingDetailRoute(it)) },
                onAddFromDiscover = { s -> nav.navigate(WineEditorRoute(prefill = graph.handoff.put(s))) },
            )
        }
        composable<JournalRoute> {
            JournalScreen(onOpen = { nav.navigate(TastingDetailRoute(it)) }, onAdd = { nav.navigate(PickWineRoute("tasting")) })
        }
        composable<WishlistRoute> {
            WishlistScreen(onOpen = { nav.navigate(WishlistDetailRoute(it)) }, onAdd = { nav.navigate(WishlistEditorRoute()) })
        }
        composable<MoreRoute> {
            MoreScreen(
                onLocations = { nav.navigate(LocationsRoute) }, onInsights = { nav.navigate(InsightsRoute) },
                onImport = { nav.navigate(ImportRoute) }, onExport = { nav.navigate(ExportRoute) },
                onSharing = { nav.navigate(SharingRoute) }, onSettings = { nav.navigate(SettingsRoute) },
            )
        }

        // ---- Cellar -----------------------------------------------------------------------------
        composable<AddChooserRoute> {
            AddChooserScreen(
                onBack = back,
                onManual = { nav.navigate(WineEditorRoute()) { popUpTo<AddChooserRoute> { inclusive = true } } },
                onScanBarcode = { nav.navigate(ScanRoute(ScanMode.BARCODE.name)) { popUpTo<AddChooserRoute> { inclusive = true } } },
                onScanLabel = { nav.navigate(ScanRoute(ScanMode.LABEL.name)) { popUpTo<AddChooserRoute> { inclusive = true } } },
                onImport = { nav.navigate(ImportRoute) { popUpTo<AddChooserRoute> { inclusive = true } } },
            )
        }
        composable<ScanRoute> { entry ->
            val r = entry.toRoute<ScanRoute>()
            ScanScreen(
                mode = ScanMode.valueOf(r.mode), onBack = back,
                onUseDetails = { key -> nav.navigate(WineEditorRoute(prefill = key)) { popUpTo<ScanRoute> { inclusive = true } } },
                onOpenExisting = { id -> nav.navigate(VintageRoute(id)) { popUpTo<ScanRoute> { inclusive = true } } },
                onNewVintageOf = { wineId, key -> nav.navigate(WineEditorRoute(wineId = wineId, prefill = key)) { popUpTo<ScanRoute> { inclusive = true } } },
                onManual = { nav.navigate(WineEditorRoute()) { popUpTo<ScanRoute> { inclusive = true } } },
            )
        }
        composable<VintageRoute> { entry ->
            val id = entry.toRoute<VintageRoute>().vintageId
            VintageDetailScreen(
                vintageId = id, onBack = back,
                onEdit = { nav.navigate(WineEditorRoute(editVintageId = it)) },
                onStockAction = { nav.navigate(StockActionRoute(id, it)) },
                onAddLot = { nav.navigate(AddLotRoute(id)) },
                onHistory = { nav.navigate(HistoryRoute(id)) },
                onAddTasting = { nav.navigate(TastingEditorRoute(vintageId = id)) },
                onOpenTasting = { nav.navigate(TastingDetailRoute(it)) },
                onValuations = { nav.navigate(ValuationsRoute(id)) },
                onOpenVintage = { nav.navigate(VintageRoute(it)) },
                onAddVintage = { wineId -> nav.navigate(WineEditorRoute(wineId = wineId)) },
                onAddToWishlist = { nav.navigate(WishlistEditorRoute(vintageId = id)) },
            )
        }
        composable<WineEditorRoute> { entry ->
            val r = entry.toRoute<WineEditorRoute>()
            WineEditorScreen(r.wineId, r.editVintageId, r.prefill, r.fromWishlistItemId, onBack = back, onSaved = { vintageId ->
                if (r.editVintageId != null) nav.popBackStack()
                else nav.navigate(VintageRoute(vintageId)) { popUpTo<WineEditorRoute> { inclusive = true } }
            })
        }
        composable<AddLotRoute> { entry ->
            val r = entry.toRoute<AddLotRoute>()
            AddLotScreen(r.vintageId, r.wishlistItemId, onBack = back, onSaved = back)
        }
        composable<StockActionRoute> { entry ->
            val r = entry.toRoute<StockActionRoute>()
            StockActionScreen(r.vintageId, r.action, onBack = back, onDone = back, onWriteNote = { lotId, locationId ->
                nav.navigate(TastingEditorRoute(vintageId = r.vintageId, lotId = lotId, locationId = locationId, consume = true)) {
                    popUpTo<StockActionRoute> { inclusive = true }
                }
            })
        }
        composable<HistoryRoute> { entry -> HistoryScreen(entry.toRoute<HistoryRoute>().vintageId, onBack = back) }
        composable<ValuationsRoute> { entry -> ValuationsScreen(entry.toRoute<ValuationsRoute>().vintageId, onBack = back) }

        // ---- Journal ----------------------------------------------------------------------------
        composable<PickWineRoute> {
            PickWineScreen(onBack = back,
                onPicked = { id -> nav.navigate(TastingEditorRoute(vintageId = id)) { popUpTo<PickWineRoute> { inclusive = true } } },
                onNewWine = { nav.navigate(WineEditorRoute()) { popUpTo<PickWineRoute> { inclusive = true } } })
        }
        composable<TastingEditorRoute> { entry ->
            val r = entry.toRoute<TastingEditorRoute>()
            TastingEditorScreen(r.vintageId, r.tastingId, r.lotId, r.locationId, r.consume, onBack = back, onSaved = { id ->
                if (r.tastingId != null) nav.popBackStack()
                else nav.navigate(TastingDetailRoute(id)) { popUpTo<TastingEditorRoute> { inclusive = true } }
            })
        }
        composable<TastingDetailRoute> { entry ->
            val id = entry.toRoute<TastingDetailRoute>().tastingId
            TastingDetailScreen(id, onBack = back, onEdit = { nav.navigate(TastingEditorRoute(tastingId = id)) }, onOpenWine = ::openVintage)
        }

        // ---- Wishlist ---------------------------------------------------------------------------
        composable<WishlistEditorRoute> { entry ->
            val r = entry.toRoute<WishlistEditorRoute>()
            WishlistEditorScreen(r.itemId, r.vintageId, onBack = back, onSaved = { id ->
                if (r.itemId != null) nav.popBackStack() else nav.navigate(WishlistDetailRoute(id)) { popUpTo<WishlistEditorRoute> { inclusive = true } }
            })
        }
        composable<WishlistDetailRoute> { entry ->
            val id = entry.toRoute<WishlistDetailRoute>().itemId
            WishlistDetailScreen(id, onBack = back, onEdit = { nav.navigate(WishlistEditorRoute(itemId = id)) }, onAddToCellar = { item ->
                val itemVintage = item.vintageId
                when {
                    itemVintage != null -> nav.navigate(AddLotRoute(itemVintage, wishlistItemId = item.id))
                    else -> {
                        val suggestion = WineSuggestion(
                            producer = item.producer?.let { Suggested(it, Confidence.HIGH, "wishlist") },
                            name = item.name?.let { Suggested(it, Confidence.HIGH, "wishlist") },
                            vintage = item.vintage?.let { Suggested(it, Confidence.HIGH, "wishlist") },
                            formatMl = item.formatMl?.let { Suggested(it, Confidence.HIGH, "wishlist") },
                        )
                        nav.navigate(WineEditorRoute(wineId = item.wineId, prefill = graph.handoff.put(suggestion), fromWishlistItemId = item.id))
                    }
                }
            })
        }

        // ---- More -------------------------------------------------------------------------------
        composable<LocationsRoute> {
            LocationsScreen(onBack = back, onOpen = { nav.navigate(LocationDetailRoute(it)) }, onAdd = { nav.navigate(LocationEditorRoute(parentId = it)) })
        }
        composable<LocationDetailRoute> { entry ->
            val id = entry.toRoute<LocationDetailRoute>().locationId
            LocationDetailScreen(id, onBack = back, onEdit = { nav.navigate(LocationEditorRoute(locationId = id)) },
                onAddChild = { nav.navigate(LocationEditorRoute(parentId = id)) }, onOpenVintage = ::openVintage)
        }
        composable<LocationEditorRoute> { entry ->
            val r = entry.toRoute<LocationEditorRoute>()
            LocationEditorScreen(r.locationId, r.parentId, onBack = back, onSaved = { back() })
        }
        composable<InsightsRoute> { InsightsScreen(onBack = back) }
        composable<ImportRoute> { ImportScreen(onBack = back, onDone = { nav.navigate(CellarRoute) { popUpTo<CellarRoute> { inclusive = false } } }) }
        composable<ExportRoute> { ExportScreen(onBack = back) }
        composable<SharingRoute> {
            SharingScreen(onBack = back, onInvite = { nav.navigate(InviteRoute) }, onJoin = { nav.navigate(JoinRoute) },
                onConflicts = { nav.navigate(ConflictsRoute) })
        }
        composable<InviteRoute> { InviteScreen(onBack = back) }
        composable<JoinRoute> {
            JoinScreen(onBack = back, onJoined = { nav.navigate(SharingRoute) { popUpTo<JoinRoute> { inclusive = true } } })
        }
        composable<ConflictsRoute> { ConflictsScreen(onBack = back, onOpenVintage = ::openVintage) }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = back, onSharing = { nav.navigate(SharingRoute) },
                onRates = { nav.navigate(RatesRoute) }, onPrivacy = { nav.navigate(PrivacyRoute) }, onExport = { nav.navigate(ExportRoute) },
                onConflicts = { nav.navigate(ConflictsRoute) }, onErase = { nav.navigate(EraseRoute) },
            )
        }
        composable<RatesRoute> { RatesScreen(onBack = back) }
        composable<PrivacyRoute> { PrivacyScreen(onBack = back) }
        composable<EraseRoute> {
            EraseScreen(onBack = back, onExport = { nav.navigate(ExportRoute) }, onErased = {
                nav.navigate(FirstRunRoute) { popUpTo(0) { inclusive = true } }
            })
        }
    }
}
