package com.houseofvino.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.houseofvino.app.ui.common.LocalAppGraph
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvIcons
import com.houseofvino.app.ui.design.HvTheme
import com.houseofvino.app.ui.design.LocalMoneyFormat
import com.houseofvino.app.ui.navigation.AppNavHost
import com.houseofvino.app.ui.navigation.CellarRoute
import com.houseofvino.app.ui.navigation.FirstRunRoute
import com.houseofvino.app.ui.navigation.JournalRoute
import com.houseofvino.app.ui.navigation.MoreRoute
import com.houseofvino.app.ui.navigation.SearchRoute
import com.houseofvino.app.ui.navigation.WishlistRoute
import com.houseofvino.data.repo.ThemePreference
import kotlin.reflect.KClass

private data class Tab(val route: Any, val type: KClass<*>, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(CellarRoute, CellarRoute::class, "Cellar", HvIcons.Cellar),
    Tab(SearchRoute, SearchRoute::class, "Search", Icons.Filled.Search),
    Tab(JournalRoute, JournalRoute::class, "Journal", HvIcons.Journal),
    Tab(WishlistRoute, WishlistRoute::class, "Wishlist", HvIcons.Wishlist),
    Tab(MoreRoute, MoreRoute::class, "More", Icons.Filled.MoreVert),
)

/** Root composable shared by Android and iOS. */
@Composable
fun App(graph: AppGraph) {
    var themeVersion by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        graph.db.changes.collect { if ("setting" in it) themeVersion++ }
    }
    val preference = remember(themeVersion) { graph.settings.preferences().theme }
    CompositionLocalProvider(LocalAppGraph provides graph, LocalMoneyFormat provides graph.platform.moneyFormat()) {
        HvTheme(preference = preference, reducedMotion = graph.platform.prefersReducedMotion()) {
            AppScaffold(graph)
        }
    }
}

@Composable
private fun AppScaffold(graph: AppGraph) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        graph.messenger.messages.collect { m ->
            val result = snackbar.showSnackbar(m.text, m.actionLabel, withDismissAction = m.actionLabel == null,
                duration = if (m.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) m.action?.invoke()
        }
    }
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val showTabs = tabs.any { t -> destination?.hierarchy?.any { it.hasRoute(t.type) } == true }

    Scaffold(
        containerColor = Hv.colors.bg,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showTabs) {
                NavigationBar(containerColor = Hv.colors.surface) {
                    tabs.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(tab.type) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label, style = Hv.type.caption) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Hv.colors.accent, selectedTextColor = Hv.colors.accent,
                                indicatorColor = Hv.colors.accentContainer, unselectedIconColor = Hv.colors.inkMuted,
                                unselectedTextColor = Hv.colors.inkMuted,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AppNavHost(nav, start = if (graph.settings.isOnboarded()) CellarRoute else FirstRunRoute, modifier = Modifier.weight(1f))
        }
    }
}
