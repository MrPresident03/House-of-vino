package com.houseofvino.app.ui.cellar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houseofvino.app.AppGraph
import com.houseofvino.app.ui.common.GraphViewModel
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.common.userMessage
import com.houseofvino.app.ui.design.BottleTile
import com.houseofvino.app.ui.design.ChoiceChips
import com.houseofvino.app.ui.design.EmptyState
import com.houseofvino.app.ui.design.ErrorState
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvIcons
import com.houseofvino.app.ui.design.HvTopBar
import com.houseofvino.app.ui.design.LedgerRow
import com.houseofvino.app.ui.design.LoadingRows
import com.houseofvino.app.ui.design.StatBlock
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.model.WineStyle
import com.houseofvino.core.search.SearchFilters
import com.houseofvino.core.search.SearchQuery
import com.houseofvino.core.search.SortOrder
import com.houseofvino.core.search.WindowFilter
import com.houseofvino.data.repo.InsightsRepository
import com.houseofvino.data.repo.SettingsRepository
import com.houseofvino.data.search.CellarRow
import com.houseofvino.app.ui.photos.PhotoImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QuickFilter(val label: String) { RED("Red"), WHITE("White"), SPARKLING("Sparkling"), READY("Ready"), HOLD("Hold"), PAST("Past window") }

data class CellarState(
    val rows: List<CellarRow> = emptyList(),
    val total: Int = 0,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val sort: SortOrder = SortOrder.PRODUCER,
    val quick: Set<QuickFilter> = emptySet(),
    val grid: Boolean = false,
    val summary: InsightsRepository.QuickTotals? = null,
    val currentYear: Int = 2026,
) {
    val endReached: Boolean get() = rows.size >= total
}

class CellarViewModel(graph: AppGraph) : GraphViewModel(graph) {
    private val _state = MutableStateFlow(CellarState(
        grid = graph.settings.preferences().cellarView == "grid",
        currentYear = graph.platform.currentYear(),
    ))
    val state: StateFlow<CellarState> = _state.asStateFlow()

    init {
        observe("wine", "vintage", "lot", "movement", "photo", "tasting", "wine_grape", "vintage_tag") { reload() }
    }

    private fun query(s: CellarState): SearchQuery {
        val q = s.quick
        val filters = SearchFilters(
            colors = buildSet { if (QuickFilter.RED in q) add(WineColor.RED); if (QuickFilter.WHITE in q) add(WineColor.WHITE) },
            styles = if (QuickFilter.SPARKLING in q) setOf(WineStyle.SPARKLING) else emptySet(),
            windows = buildSet {
                if (QuickFilter.READY in q) { add(WindowFilter.READY); add(WindowFilter.DRINK_SOON) }
                if (QuickFilter.HOLD in q) add(WindowFilter.HOLD)
                if (QuickFilter.PAST in q) add(WindowFilter.PAST)
            },
        )
        return SearchQuery(filters = filters, sort = s.sort)
    }

    private suspend fun reload() {
        val s = _state.value
        try {
            val keep = s.rows.size.coerceAtLeast(PAGE)
            val page = io { graph.search.searchCellar(collectionId, query(s), 0, keep, s.currentYear, graph.settings.preferences().currency.code) }
            val summary = io { graph.insights.quickTotals(collectionId, s.currentYear) }
            _state.update { it.copy(rows = page.items, total = page.total, loading = false, error = null, summary = summary) }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = "Couldn't load your cellar. ${e.userMessage()}") }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.endReached) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            val page = io { graph.search.searchCellar(collectionId, query(s), s.rows.size, PAGE, s.currentYear, graph.settings.preferences().currency.code) }
            _state.update { it.copy(rows = it.rows + page.items, total = page.total, loadingMore = false) }
        }
    }

    fun setSort(sort: SortOrder) { _state.update { it.copy(sort = sort, rows = emptyList(), loading = true) }; viewModelScope.launch { reload() } }

    fun toggleQuick(f: QuickFilter) {
        _state.update { s -> s.copy(quick = if (f in s.quick) s.quick - f else s.quick + f, rows = emptyList(), loading = true) }
        viewModelScope.launch { reload() }
    }

    fun toggleGrid() {
        val grid = !_state.value.grid
        _state.update { it.copy(grid = grid) }
        launchIo { io { graph.settings.put(SettingsRepository.KEY_CELLAR_VIEW, if (grid) "grid" else "list") } }
    }

    fun retry() { _state.update { it.copy(loading = true, error = null) }; viewModelScope.launch { reload() } }

    companion object { const val PAGE = 60 }
}

private val sortLabels = listOf(
    SortOrder.PRODUCER to "Producer", SortOrder.VINTAGE_DESC to "Vintage, newest", SortOrder.VINTAGE_ASC to "Vintage, oldest",
    SortOrder.DRINK_BY to "Drink by", SortOrder.QUANTITY_DESC to "Most bottles", SortOrder.PRICE_DESC to "Price, highest",
    SortOrder.RATING_DESC to "My rating", SortOrder.RECENTLY_ADDED to "Recently added",
)

@Composable
fun CellarScreen(onOpen: (String) -> Unit, onAdd: () -> Unit, onSearch: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel { CellarViewModel(graph) }
    val state by vm.state.collectAsState()
    var sortMenu by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HvTopBar("Cellar", actions = {
                IconButton(onClick = vm::toggleGrid) {
                    Icon(if (state.grid) HvIcons.Rows else HvIcons.Grid, contentDescription = if (state.grid) "Show as list" else "Show as grid")
                }
                Box {
                    IconButton(onClick = { sortMenu = true }) { Icon(HvIcons.Filter, contentDescription = "Sort") }
                    DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                        sortLabels.forEach { (s, label) ->
                            DropdownMenuItem(text = { Text(if (s == state.sort) "✓  $label" else label) }, onClick = { sortMenu = false; vm.setSort(s) })
                        }
                    }
                }
            })
            when {
                state.loading -> LoadingRows()
                state.error != null -> ErrorState(state.error!!, vm::retry)
                state.rows.isEmpty() && state.quick.isEmpty() -> EmptyState(
                    title = "Your cellar is empty",
                    message = "Add a wine by hand, scan a label or import a spreadsheet from another app.",
                    action = "Add wine" to onAdd,
                )
                else -> CellarList(state, onOpen, vm)
            }
        }
        if (state.rows.isNotEmpty() || state.quick.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onAdd, icon = { Icon(Icons.Filled.Add, contentDescription = null) }, text = { Text("Add wine") },
                containerColor = Hv.colors.accent, contentColor = Hv.colors.onAccent,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag("add-wine"),
            )
        }
    }
}

@Composable
private fun CellarHeader(state: CellarState, vm: CellarViewModel) {
    Column(Modifier.fillMaxWidth()) {
        state.summary?.let { s ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                StatBlock(s.bottles.toString(), if (s.bottles == 1) "bottle" else "bottles")
                StatBlock(s.vintages.toString(), if (s.vintages == 1) "wine" else "wines")
                StatBlock((s.readyBottles + s.drinkSoonBottles).toString(), "ready to drink")
            }
        }
        ChoiceChips(QuickFilter.entries, state.quick, { it.label }, vm::toggleQuick, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
    }
}

@Composable
private fun CellarList(state: CellarState, onOpen: (String) -> Unit, vm: CellarViewModel) {
    if (state.grid) {
        val grid = rememberLazyGridState()
        val nearEnd by remember { derivedStateOf { (grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) > state.rows.size - 12 } }
        LaunchedEffect(nearEnd, state.rows.size) { if (nearEnd) vm.loadMore() }
        LazyVerticalGrid(GridCells.Adaptive(160.dp), state = grid, contentPadding = PaddingValues(bottom = 96.dp), modifier = Modifier.testTag("cellar-grid")) {
            item(span = { GridItemSpan(maxLineSpan) }) { CellarHeader(state, vm) }
            if (state.rows.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { NoMatches() }
            items(state.rows, key = { it.vintageId }) { row -> BottleTile(row, state.currentYear, onClick = { onOpen(row.vintageId) }, photo = labelPhoto(row)) }
        }
    } else {
        val list = rememberLazyListState()
        val nearEnd by remember { derivedStateOf { (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) > state.rows.size - 15 } }
        LaunchedEffect(nearEnd, state.rows.size) { if (nearEnd) vm.loadMore() }
        LazyColumn(state = list, contentPadding = PaddingValues(bottom = 96.dp), modifier = Modifier.testTag("cellar-list")) {
            item { CellarHeader(state, vm) }
            if (state.rows.isEmpty()) item { NoMatches() }
            items(state.rows, key = { it.vintageId }) { row -> LedgerRow(row, state.currentYear, onClick = { onOpen(row.vintageId) }, photo = labelPhoto(row)) }
            if (state.loadingMore) item { LoadingRows(2) }
        }
    }
}

/** The bottle's label photo, if it has one. */
private fun labelPhoto(row: CellarRow): (@Composable (Modifier) -> Unit)? =
    row.photoPath?.let { path -> { m -> PhotoImage(path, "Label photo", m) } }

@Composable
private fun NoMatches() {
    Text("No wines match these filters.", style = Hv.type.body, color = Hv.colors.inkMuted, modifier = Modifier.padding(16.dp))
}
