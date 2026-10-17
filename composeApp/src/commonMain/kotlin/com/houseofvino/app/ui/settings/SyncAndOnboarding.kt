package com.houseofvino.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houseofvino.app.AppGraph
import com.houseofvino.app.ui.common.CurrencyDropdown
import com.houseofvino.app.ui.common.GraphViewModel
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.common.userMessage
import com.houseofvino.app.ui.design.ButtonKind
import com.houseofvino.app.ui.design.EmptyState
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvButton
import com.houseofvino.app.ui.design.HvIcons
import com.houseofvino.app.ui.design.HvTopBar
import com.houseofvino.app.ui.design.LoadingRows
import com.houseofvino.app.ui.design.SectionHeader
import com.houseofvino.app.ui.design.vintageLabel
import com.houseofvino.data.db.Sync_conflict
import com.houseofvino.data.sync.ConflictField
import com.houseofvino.data.repo.SettingsRepository
import com.houseofvino.data.repo.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// =================================================================================================
// Conflicts: changes made on two phones before they synced

data class ConflictView(val raw: Sync_conflict, val title: String, val differences: List<ConflictField>)

class ConflictsViewModel(graph: AppGraph) : GraphViewModel(graph) {
    private val _conflicts = MutableStateFlow<List<ConflictView>?>(null)
    val conflicts: StateFlow<List<ConflictView>?> = _conflicts.asStateFlow()
    private val json = Json { ignoreUnknownKeys = true }

    init {
        observe("sync_conflict", "vintage", "wine") {
            _conflicts.value = io { graph.conflicts.open().map(::describe) }
        }
    }

    private fun describe(c: Sync_conflict): ConflictView {
        val vintageId = when (c.entity) {
            "stock" -> runCatching { (json.parseToJsonElement(c.local_payload) as JsonObject)["vintage_id"]?.let { (it as JsonPrimitive).content } }.getOrNull()
            "vintage" -> c.entity_id
            else -> null
        }
        val title = vintageId?.let { graph.cellar.vintageDetail(it) }?.let { "${vintageLabel(it.vintage.vintage)} ${it.wine.displayName}" }
            ?: (if (c.entity == "wine") graph.cellar.wine(c.entity_id)?.displayName else null)
            ?: c.entity.replace('_', ' ').replaceFirstChar { it.uppercase() }
        return ConflictView(c, title, graph.conflicts.fields(c).filter { it.column !in HIDDEN })
    }

    fun keepCurrent(id: String) = launchIo { io { graph.conflicts.keepCurrent(id) } }
    fun useOther(id: String) = launchIo { io { graph.conflicts.useOther(id) }; messenger.show("Changed. Send changes so your partner gets it too.") }

    private companion object { val HIDDEN = setOf("updated_at", "created_at", "producer_sort", "collection_id", "id") }
}

@Composable
fun ConflictsScreen(onBack: () -> Unit, onOpenVintage: (String) -> Unit) {
    val graph = appGraph()
    val vm = viewModel { ConflictsViewModel(graph) }
    val conflicts by vm.conflicts.collectAsState()
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Changed on both phones", onBack = onBack)
        LazyColumn(Modifier.weight(1f)) {
            item {
                Text("When the same detail is changed on two phones before they swap files, the later change is kept on both and the other is listed here so you can choose.",
                    style = Hv.type.bodyDense, color = Hv.colors.inkMuted, modifier = Modifier.padding(16.dp))
            }
            val list = conflicts
            when {
                list == null -> item { LoadingRows(2) }
                list.isEmpty() -> item { EmptyState("Nothing to review", "Both phones agree on everything they've seen.") }
                else -> items(list, key = { it.raw.id }) { c -> ConflictItem(c, vm, onOpenVintage) }
            }
        }
    }
}

@Composable
private fun ConflictItem(c: ConflictView, vm: ConflictsViewModel, onOpenVintage: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(c.title, style = Hv.type.body, color = Hv.colors.ink,
            modifier = if (c.raw.entity == "vintage") Modifier.clickable { onOpenVintage(c.raw.entity_id) } else Modifier)
        Text(c.raw.reason, style = Hv.type.caption, color = Hv.colors.inkMuted)
        c.differences.forEach { d ->
            Text("${d.column.replace('_', ' ')}: now “${d.current ?: "—"}”, other phone “${d.other ?: "—"}”", style = Hv.type.caption, color = Hv.colors.ink)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (c.raw.entity == "stock") {
                TextButton(onClick = { vm.keepCurrent(c.raw.id) }) { Text("I've counted the bottles") }
            } else {
                TextButton(onClick = { vm.keepCurrent(c.raw.id) }) { Text("Keep it as it is") }
                TextButton(onClick = { vm.useOther(c.raw.id) }) { Text("Use the other phone's") }
            }
        }
    }
    HorizontalDivider(color = Hv.colors.outline)
}

// =================================================================================================
// First run

@Composable
fun FirstRunScreen(onDone: () -> Unit, onJoin: () -> Unit) {
    val graph = appGraph()
    var currency by remember { mutableStateOf(graph.settings.preferences().currency) }
    var theme by remember { mutableStateOf(ThemePreference.SYSTEM) }
    fun finish(then: () -> Unit) {
        graph.scope.launch {
            graph.settings.put(SettingsRepository.KEY_CURRENCY, currency.code)
            graph.settings.put(SettingsRepository.KEY_THEME, theme.name)
            graph.settings.put(SettingsRepository.KEY_ONBOARDED, "true")
        }
        then()
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("House of Vino", style = Hv.type.vintageDisplay, color = Hv.colors.ink)
        Text("A cellar book for serious collections: every bottle, where it is, what you paid and how it tasted.", style = Hv.type.body, color = Hv.colors.ink)
        CurrencyDropdown(currency, { currency = it }, label = "Your main currency")
        Text("Appearance", style = Hv.type.label, color = Hv.colors.inkMuted)
        com.houseofvino.app.ui.design.ChoiceChips(ThemePreference.entries, setOf(theme),
            { when (it) { ThemePreference.SYSTEM -> "Match device"; ThemePreference.DARK -> "Dark"; ThemePreference.LIGHT -> "Light" } }, { theme = it })
        HvButton("Start with an empty cellar", { finish(onDone) }, Modifier.fillMaxWidth())
        HvButton("Join a partner's cellar", { finish(onJoin) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY)
        Text("Everything stays on your phone. There's no account; you can share with a partner's phone at any time.", style = Hv.type.caption, color = Hv.colors.inkMuted)
    }
}

// =================================================================================================
// Pick a wine (for tasting notes about wines from the cellar or anywhere)

class PickWineViewModel(graph: AppGraph) : GraphViewModel(graph) {
    val text = MutableStateFlow("")
    private val _rows = MutableStateFlow<List<com.houseofvino.data.search.CellarRow>>(emptyList())
    val rows: StateFlow<List<com.houseofvino.data.search.CellarRow>> = _rows.asStateFlow()

    init { search("") }

    fun search(t: String) {
        text.value = t
        launchIo {
            _rows.value = io {
                graph.search.searchCellar(collectionId, com.houseofvino.core.search.SearchQuery(t, filters = com.houseofvino.core.search.SearchFilters(inStockOnly = false),
                    sort = if (t.isBlank()) com.houseofvino.core.search.SortOrder.RECENTLY_ADDED else com.houseofvino.core.search.SortOrder.RELEVANCE),
                    0, 100, graph.platform.currentYear(), null).items
            }
        }
    }
}

@Composable
fun PickWineScreen(onBack: () -> Unit, onPicked: (String) -> Unit, onNewWine: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel { PickWineViewModel(graph) }
    val text by vm.text.collectAsState()
    val rows by vm.rows.collectAsState()
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Which wine?", onBack = onBack)
        com.houseofvino.app.ui.design.HvTextField(text, vm::search, "Search your wines", Modifier.padding(horizontal = 16.dp))
        TextButton(onClick = onNewWine, modifier = Modifier.padding(horizontal = 8.dp)) { Text("A wine not in my cellar…") }
        if (rows.isEmpty()) EmptyState("No matches", "Add the wine first; it doesn't need to have bottles in stock.", action = "Add wine" to onNewWine)
        LazyColumn {
            items(rows, key = { it.vintageId }) { r ->
                Row(Modifier.fillMaxWidth().clickable { onPicked(r.vintageId) }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(vintageLabel(r.vintage), style = Hv.type.vintageRow, color = Hv.colors.ink, modifier = Modifier.padding(end = 12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.producer, style = Hv.type.label, color = Hv.colors.inkMuted)
                        r.wineName?.let { Text(it, style = Hv.type.body, color = Hv.colors.ink) }
                    }
                    Text(r.quantity.toString(), style = Hv.type.figure, color = Hv.colors.inkMuted)
                }
                HorizontalDivider(Modifier.padding(start = 16.dp), color = Hv.colors.outline)
            }
        }
    }
}

