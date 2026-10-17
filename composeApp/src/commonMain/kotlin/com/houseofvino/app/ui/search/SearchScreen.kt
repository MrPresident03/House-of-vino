package com.houseofvino.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houseofvino.app.AppGraph
import com.houseofvino.app.ui.common.GraphViewModel
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.common.userMessage
import com.houseofvino.app.ui.design.ButtonKind
import com.houseofvino.app.ui.design.ChoiceChips
import com.houseofvino.app.ui.design.ErrorState
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvButton
import com.houseofvino.app.ui.design.HvIcons
import com.houseofvino.app.ui.design.HvTextField
import com.houseofvino.app.ui.design.LedgerRow
import com.houseofvino.app.ui.design.LoadingRows
import com.houseofvino.app.ui.design.SectionHeader
import com.houseofvino.app.ui.design.vintageLabel
import com.houseofvino.core.model.BottleFormats
import com.houseofvino.core.model.Ratings
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.model.WineStyle
import com.houseofvino.core.scan.WineSuggestion
import com.houseofvino.core.search.SearchFilters
import com.houseofvino.core.search.SearchQuery
import com.houseofvino.core.search.SearchScope
import com.houseofvino.core.search.SortOrder
import com.houseofvino.core.search.WindowFilter
import com.houseofvino.data.integrations.ExternalProduct
import com.houseofvino.data.integrations.LookupResult
import com.houseofvino.data.repo.LocationNode
import com.houseofvino.data.repo.SavedSearch
import com.houseofvino.data.search.CellarRow
import com.houseofvino.data.search.JournalRow
import com.houseofvino.data.search.WishlistRow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FacetOptions(
    val countries: List<String> = emptyList(),
    val regions: List<String> = emptyList(),
    val grapes: List<String> = emptyList(),
    val tags: List<Pair<String, String>> = emptyList(),
    val locations: List<LocationNode> = emptyList(),
)

data class SearchState(
    val text: String = "",
    val scope: SearchScope = SearchScope.CELLAR,
    val filters: SearchFilters = SearchFilters(),
    val sort: SortOrder = SortOrder.RELEVANCE,
    val searching: Boolean = false,
    val cellar: List<CellarRow> = emptyList(),
    val wishlist: List<WishlistRow> = emptyList(),
    val journal: List<JournalRow> = emptyList(),
    val discover: List<ExternalProduct> = emptyList(),
    val discoverNote: String? = null,
    val total: Int = 0,
    val corrections: Map<String, String> = emptyMap(),
    val recent: List<String> = emptyList(),
    val saved: List<SavedSearch> = emptyList(),
    val facets: FacetOptions = FacetOptions(),
    val error: String? = null,
)

class SearchViewModel(graph: AppGraph) : GraphViewModel(graph) {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()
    private var job: Job? = null
    private val year = graph.platform.currentYear()

    init {
        observe("saved_search", "recent_query") {
            val scope = _state.value.scope
            _state.update { it.copy(recent = io { graph.settings.recentQueries(scope) }, saved = io { graph.settings.savedSearches(collectionId) }) }
        }
        observe("wine", "vintage", "lot", "movement", "tasting", "wishlist_item", "tag", "vintage_tag") { if (hasQuery()) run(debounce = false) }
        launchIo { loadFacets() }
    }

    private suspend fun loadFacets() {
        val f = io {
            FacetOptions(
                countries = db.catalog.countries(collectionId).executeAsList().mapNotNull { it.country },
                regions = db.catalog.regions(collectionId).executeAsList().mapNotNull { it.region },
                grapes = db.catalog.allGrapes(collectionId).executeAsList().map { it.grape },
                tags = graph.cellar.allTags(collectionId).map { it.first.id to it.first.name },
                locations = graph.locations.tree(collectionId),
            )
        }
        _state.update { it.copy(facets = f) }
    }

    private fun hasQuery() = _state.value.text.isNotBlank() || _state.value.filters.activeCount > 0

    fun setText(t: String) { _state.update { it.copy(text = t) }; run(debounce = true) }
    fun setScope(s: SearchScope) {
        _state.update { it.copy(scope = s, recent = graph.settings.recentQueries(s)) }
        run(debounce = false)
    }
    fun setFilters(f: SearchFilters) { _state.update { it.copy(filters = f) }; run(debounce = false) }
    fun setSort(s: SortOrder) { _state.update { it.copy(sort = s) }; run(debounce = false) }
    fun clear() { _state.update { it.copy(text = "", filters = SearchFilters(), cellar = emptyList(), wishlist = emptyList(), journal = emptyList(), discover = emptyList(), total = 0, corrections = emptyMap()) } }

    fun applySaved(s: SavedSearch) {
        _state.update { it.copy(text = s.query.text, scope = s.query.scope, filters = s.query.filters, sort = s.query.sort) }
        run(debounce = false)
    }

    fun save(name: String) = launchIo {
        val s = _state.value
        io { graph.settings.saveSearch(collectionId, name, SearchQuery(s.text, s.scope, s.filters, s.sort)) }
        messenger.show("Search saved")
    }

    fun deleteSaved(id: String) = launchIo { io { graph.settings.deleteSavedSearch(id) } }

    fun commitRecent() {
        val s = _state.value
        if (s.text.isNotBlank()) launchIo { io { graph.settings.recordRecentQuery(s.text, s.scope) } }
    }

    /** Discover only reaches the network on an explicit request: Open Food Facts allows ~10 searches a minute. */
    fun searchOnline() {
        commitRecent()
        run(debounce = false, online = true)
    }

    private fun run(debounce: Boolean, online: Boolean = false) {
        job?.cancel()
        job = viewModelScope.launch {
            if (debounce) delay(180)
            val s = _state.value
            if (s.text.isBlank() && s.filters.activeCount == 0 && s.scope != SearchScope.WISHLIST && s.scope != SearchScope.JOURNAL) {
                _state.update { it.copy(cellar = emptyList(), discover = emptyList(), total = 0, corrections = emptyMap(), searching = false) }
                return@launch
            }
            _state.update { it.copy(searching = true, error = null) }
            try {
                val q = SearchQuery(s.text, s.scope, s.filters, s.sort)
                when (s.scope) {
                    SearchScope.CELLAR -> {
                        val page = io { graph.search.searchCellar(collectionId, q, 0, 200, year, graph.settings.preferences().currency.code) }
                        _state.update { it.copy(cellar = page.items, total = page.total, corrections = page.corrections, searching = false) }
                    }
                    SearchScope.WISHLIST -> {
                        val page = io { graph.search.searchWishlist(collectionId, q, setOf("WANTED", "WATCHING", "ORDERED", "ACQUIRED"), 0, 200) }
                        _state.update { it.copy(wishlist = page.items, total = page.total, corrections = page.corrections, searching = false) }
                    }
                    SearchScope.JOURNAL -> {
                        val page = io { graph.search.searchJournal(collectionId, q, 0, 200) }
                        _state.update { it.copy(journal = page.items, total = page.total, corrections = page.corrections, searching = false) }
                    }
                    SearchScope.DISCOVER -> {
                        if (!online) {
                            _state.update { it.copy(searching = false, total = it.discover.size) }
                            return@launch
                        }
                        when (val r = graph.barcodes.search(s.text)) {
                            is LookupResult.Found -> _state.update { it.copy(discover = r.products, total = r.products.size, discoverNote = r.attribution, searching = false) }
                            LookupResult.NotFound -> _state.update { it.copy(discover = emptyList(), total = 0, discoverNote = null, searching = false) }
                            is LookupResult.Unavailable -> _state.update { it.copy(discover = emptyList(), total = 0, discoverNote = r.reason, searching = false) }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update { it.copy(searching = false, error = e.userMessage()) }
            }
        }
    }
}

private val scopes = listOf(SearchScope.CELLAR to "Cellar", SearchScope.WISHLIST to "Wishlist", SearchScope.JOURNAL to "Journal", SearchScope.DISCOVER to "Discover")

@Composable
fun SearchScreen(
    onOpenVintage: (String) -> Unit,
    onOpenWishlist: (String) -> Unit,
    onOpenTasting: (String) -> Unit,
    onAddFromDiscover: (WineSuggestion) -> Unit,
) {
    val graph = appGraph()
    val vm = viewModel { SearchViewModel(graph) }
    val s by vm.state.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val year = graph.platform.currentYear()

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            HvTextField(s.text, vm::setText, "Search producer, wine, region, grape…", Modifier.weight(1f).testTag("search-field"),
                trailing = {
                    if (s.text.isNotEmpty()) IconButton(onClick = vm::clear) { Icon(Icons.Filled.Clear, "Clear search") }
                    else Icon(Icons.Filled.Search, null)
                })
            if (s.scope != SearchScope.DISCOVER) {
                BadgedBox(badge = { if (s.filters.activeCount > 0) Badge { Text(s.filters.activeCount.toString()) } }) {
                    IconButton(onClick = { showFilters = true }) { Icon(HvIcons.Filter, "Filters") }
                }
            }
        }
        PrimaryTabRow(selectedTabIndex = scopes.indexOfFirst { it.first == s.scope }, containerColor = Hv.colors.bg) {
            scopes.forEach { (scope, label) -> Tab(selected = s.scope == scope, onClick = { vm.setScope(scope) }, text = { Text(label) }) }
        }
        if (s.scope == SearchScope.DISCOVER) {
            HvButton("Search Open Food Facts", vm::searchOnline, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                kind = ButtonKind.SECONDARY, enabled = !s.searching && s.text.trim().length >= 3)
        }
        val hasResults = s.cellar.isNotEmpty() || s.wishlist.isNotEmpty() || s.journal.isNotEmpty() || s.discover.isNotEmpty()
        if (s.text.isNotBlank() || s.filters.activeCount > 0 || hasResults) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (s.searching) "Searching…" else "${s.total} result${if (s.total == 1) "" else "s"}", style = Hv.type.caption, color = Hv.colors.inkMuted, modifier = Modifier.weight(1f))
                if (s.scope == SearchScope.CELLAR) {
                    TextButton(onClick = { sortMenu = true }) { Text("Sort: ${sortLabel(s.sort)}") }
                    DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                        listOf(SortOrder.RELEVANCE, SortOrder.PRODUCER, SortOrder.VINTAGE_DESC, SortOrder.VINTAGE_ASC, SortOrder.DRINK_BY,
                            SortOrder.QUANTITY_DESC, SortOrder.PRICE_DESC, SortOrder.RATING_DESC).forEach { o ->
                            DropdownMenuItem(text = { Text(sortLabel(o)) }, onClick = { sortMenu = false; vm.setSort(o) })
                        }
                    }
                }
                if (s.scope != SearchScope.DISCOVER) TextButton(onClick = { saving = true }) { Text("Save") }
            }
        }
        if (s.corrections.isNotEmpty()) {
            Text("Showing results for “${s.corrections.entries.joinToString(" ") { it.value }}”", style = Hv.type.bodyDense, color = Hv.colors.ink,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        when {
            s.error != null -> ErrorState(s.error!!, null)
            s.searching && !hasResults -> LoadingRows(4)
            s.text.isBlank() && s.filters.activeCount == 0 && !hasResults -> SuggestionsPanel(s, vm)
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), modifier = Modifier.testTag("search-results")) {
                when (s.scope) {
                    SearchScope.CELLAR -> items(s.cellar, key = { it.vintageId }) { row ->
                        LedgerRow(row, year, onClick = { vm.commitRecent(); onOpenVintage(row.vintageId) })
                    }
                    SearchScope.WISHLIST -> items(s.wishlist, key = { it.itemId }) { row ->
                        SimpleResult(row.vintage?.let(::vintageLabel) ?: "—", row.producer.orEmpty(), row.wineName) { vm.commitRecent(); onOpenWishlist(row.itemId) }
                    }
                    SearchScope.JOURNAL -> items(s.journal, key = { it.tastingId }) { row ->
                        SimpleResult(vintageLabel(row.vintage), row.producer, listOfNotNull(row.wineName, row.tastedOn,
                            row.rating?.let { Ratings.display(it, graph.settings.preferences().ratingScale) }).joinToString(" · ")) {
                            vm.commitRecent(); onOpenTasting(row.tastingId)
                        }
                    }
                    SearchScope.DISCOVER -> {
                        items(s.discover, key = { (it.code ?: "") + (it.productName ?: "") }) { p ->
                            SimpleResult("+", p.brand.orEmpty(), listOfNotNull(p.productName, p.quantityText).joinToString(" · ")) { onAddFromDiscover(p.toSuggestion()) }
                        }
                        item { s.discoverNote?.let { Text(it, style = Hv.type.caption, color = Hv.colors.inkMuted, modifier = Modifier.padding(16.dp)) } }
                    }
                }
                if (!s.searching && s.total == 0) item {
                    Text(if (s.scope == SearchScope.DISCOVER) s.discoverNote ?: "Type a producer or wine, then search. Online lookups are limited to a few a minute." else "Nothing matches. Try fewer words or clear the filters.",
                        style = Hv.type.body, color = Hv.colors.inkMuted, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }

    if (showFilters) FiltersSheet(s.filters, s.facets, s.scope, onDismiss = { showFilters = false }, onApply = { vm.setFilters(it); showFilters = false })
    if (saving) SaveSearchDialog(onDismiss = { saving = false }) { name -> saving = false; vm.save(name) }
}

private fun sortLabel(s: SortOrder) = when (s) {
    SortOrder.RELEVANCE -> "Best match"; SortOrder.PRODUCER -> "Producer"; SortOrder.VINTAGE_DESC -> "Newest vintage"; SortOrder.VINTAGE_ASC -> "Oldest vintage"
    SortOrder.DRINK_BY -> "Drink by"; SortOrder.QUANTITY_DESC -> "Most bottles"; SortOrder.PRICE_DESC -> "Highest price"; SortOrder.PRICE_ASC -> "Lowest price"
    SortOrder.RATING_DESC -> "My rating"; SortOrder.RECENTLY_ADDED -> "Recently added"; SortOrder.TASTED_DESC -> "Recently tasted"
}

@Composable
private fun SimpleResult(lead: String, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(lead, style = Hv.type.vintageRow, color = Hv.colors.ink, modifier = Modifier.padding(end = 12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Hv.type.body, color = Hv.colors.ink)
            subtitle?.takeIf { it.isNotBlank() }?.let { Text(it, style = Hv.type.caption, color = Hv.colors.inkMuted) }
        }
    }
    HorizontalDivider(Modifier.padding(start = 16.dp), color = Hv.colors.outline)
}

@Composable
private fun SuggestionsPanel(s: SearchState, vm: SearchViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (s.scope == SearchScope.DISCOVER) {
            Text("Search Open Food Facts for wines you don't own yet. Results are suggestions you can edit before adding.",
                style = Hv.type.bodyDense, color = Hv.colors.inkMuted, modifier = Modifier.padding(16.dp))
            return@Column
        }
        if (s.recent.isNotEmpty()) {
            SectionHeader("Recent")
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                s.recent.forEach { q -> AssistChip(onClick = { vm.setText(q) }, label = { Text(q) }) }
            }
        }
        if (s.saved.isNotEmpty()) {
            SectionHeader("Saved searches")
            s.saved.forEach { saved ->
                Row(Modifier.fillMaxWidth().clickable { vm.applySaved(saved) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(saved.name, style = Hv.type.body, color = Hv.colors.ink)
                        Text(listOfNotNull(saved.query.text.ifBlank { null }, saved.query.filters.activeCount.takeIf { it > 0 }?.let { "$it filter(s)" })
                            .joinToString(" · "), style = Hv.type.caption, color = Hv.colors.inkMuted)
                    }
                    TextButton(onClick = { vm.deleteSaved(saved.id) }) { Text("Remove") }
                }
            }
        }
        SectionHeader("Tips")
        Text("Type part of any word: “marg 2015”. Narrow with producer:, region:, grape:, tag:, vintage:2010-2016, color:white.",
            style = Hv.type.bodyDense, color = Hv.colors.inkMuted, modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun SaveSearchDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save this search") },
        text = { HvTextField(name, { name = it }, "Name", placeholder = "e.g. Ready Burgundy") },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun FiltersSheet(initial: SearchFilters, facets: FacetOptions, scope: SearchScope, onDismiss: () -> Unit, onApply: (SearchFilters) -> Unit) {
    var f by remember { mutableStateOf(initial) }
    var from by remember { mutableStateOf(initial.vintageFrom?.toString().orEmpty()) }
    var to by remember { mutableStateOf(initial.vintageTo?.toString().orEmpty()) }
    var minRating by remember { mutableStateOf(initial.minRating?.toString().orEmpty()) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Hv.colors.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Filters", style = Hv.type.title, color = Hv.colors.ink)
            if (scope == SearchScope.CELLAR) Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Only wines in stock", style = Hv.type.body, color = Hv.colors.ink, modifier = Modifier.weight(1f))
                Switch(f.inStockOnly, { f = f.copy(inStockOnly = it) })
            }
            Text("Colour", style = Hv.type.label, color = Hv.colors.inkMuted)
            ChoiceChips(WineColor.entries, f.colors, { it.name.lowercase().replaceFirstChar(Char::uppercase).replace("Rose", "Rosé") }, { c ->
                f = f.copy(colors = if (c in f.colors) f.colors - c else f.colors + c)
            })
            if (scope == SearchScope.CELLAR) {
                Text("Style", style = Hv.type.label, color = Hv.colors.inkMuted)
                ChoiceChips(WineStyle.entries, f.styles, { it.name.lowercase().replaceFirstChar(Char::uppercase) }, { st ->
                    f = f.copy(styles = if (st in f.styles) f.styles - st else f.styles + st)
                })
                Text("Drinking window", style = Hv.type.label, color = Hv.colors.inkMuted)
                ChoiceChips(listOf(WindowFilter.READY, WindowFilter.DRINK_SOON, WindowFilter.HOLD, WindowFilter.PAST, WindowFilter.UNKNOWN), f.windows,
                    { com.houseofvino.core.insights.DrinkingWindow.label(it) }, { w -> f = f.copy(windows = if (w in f.windows) f.windows - w else f.windows + w) })
                if (facets.countries.isNotEmpty()) {
                    Text("Country", style = Hv.type.label, color = Hv.colors.inkMuted)
                    ChoiceChips(facets.countries, f.countries, { it }, { c -> f = f.copy(countries = if (c in f.countries) f.countries - c else f.countries + c) })
                }
                if (facets.regions.isNotEmpty()) {
                    Text("Region", style = Hv.type.label, color = Hv.colors.inkMuted)
                    ChoiceChips(facets.regions.take(40), f.regions, { it }, { r -> f = f.copy(regions = if (r in f.regions) f.regions - r else f.regions + r) })
                }
                if (facets.grapes.isNotEmpty()) {
                    Text("Grape", style = Hv.type.label, color = Hv.colors.inkMuted)
                    ChoiceChips(facets.grapes.take(40), f.grapes, { it }, { g -> f = f.copy(grapes = if (g in f.grapes) f.grapes - g else f.grapes + g) })
                }
                Text("Bottle size", style = Hv.type.label, color = Hv.colors.inkMuted)
                ChoiceChips(listOf(375, 750, 1500, 3000), f.formatsMl, { BottleFormats.nameFor(it) }, { ml ->
                    f = f.copy(formatsMl = if (ml in f.formatsMl) f.formatsMl - ml else f.formatsMl + ml)
                })
                if (facets.locations.isNotEmpty()) {
                    Text("Location", style = Hv.type.label, color = Hv.colors.inkMuted)
                    ChoiceChips(facets.locations.filter { it.depth <= 1 }.map { it.location.id }, f.locationIds,
                        { id -> facets.locations.first { it.location.id == id }.location.name },
                        { id -> f = f.copy(locationIds = if (id in f.locationIds) f.locationIds - id else f.locationIds + id) })
                }
                if (facets.tags.isNotEmpty()) {
                    Text("Tags", style = Hv.type.label, color = Hv.colors.inkMuted)
                    ChoiceChips(facets.tags.map { it.first }, f.tagIds, { id -> facets.tags.first { it.first == id }.second },
                        { id -> f = f.copy(tagIds = if (id in f.tagIds) f.tagIds - id else f.tagIds + id) })
                }
            }
            Text("Vintage", style = Hv.type.label, color = Hv.colors.inkMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HvTextField(from, { from = it.filter(Char::isDigit).take(4) }, "From", Modifier.weight(1f), keyboard = KeyboardType.Number)
                HvTextField(to, { to = it.filter(Char::isDigit).take(4) }, "To", Modifier.weight(1f), keyboard = KeyboardType.Number)
            }
            if (scope == SearchScope.CELLAR) Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Include non-vintage", style = Hv.type.body, color = Hv.colors.ink, modifier = Modifier.weight(1f))
                Switch(f.includeNonVintage, { f = f.copy(includeNonVintage = it) })
            }
            HvTextField(minRating, { minRating = it.filter(Char::isDigit).take(3) }, "Minimum rating (0–100)", keyboard = KeyboardType.Number)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HvButton("Clear all", { f = SearchFilters(); from = ""; to = ""; minRating = "" }, Modifier.weight(1f), kind = com.houseofvino.app.ui.design.ButtonKind.SECONDARY)
                HvButton("Show results", {
                    onApply(f.copy(vintageFrom = from.toIntOrNull(), vintageTo = to.toIntOrNull(), minRating = minRating.toIntOrNull()?.coerceIn(0, 100)))
                }, Modifier.weight(1f).testTag("apply-filters"))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
