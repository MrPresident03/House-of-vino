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
// Sync status and conflicts

data class ConflictView(val raw: Sync_conflict, val title: String, val differences: List<Triple<String, String, String>>)

class SyncStatusViewModel(graph: AppGraph) : GraphViewModel(graph) {
    private val _conflicts = MutableStateFlow<List<ConflictView>?>(null)
    val conflicts: StateFlow<List<ConflictView>?> = _conflicts.asStateFlow()
    private val _summary = MutableStateFlow<List<Pair<String, Long>>>(emptyList())
    val summary: StateFlow<List<Pair<String, Long>>> = _summary.asStateFlow()
    private val json = Json { ignoreUnknownKeys = true }

    init {
        observe("sync_conflict", "sync_outbox") {
            _conflicts.value = io { db.local.openConflicts().executeAsList().map(::describe) }
            _summary.value = io { db.local.outboxSummary().executeAsList().map { it.state to it.ops } }
        }
    }

    private fun describe(c: Sync_conflict): ConflictView {
        if (c.entity == "stock") {
            val vintageId = runCatching { (json.parseToJsonElement(c.local_payload) as JsonObject)["vintage_id"]?.let { (it as JsonPrimitive).content } }.getOrNull()
            val title = vintageId?.let { graph.cellar.vintageDetail(it) }?.let { "${vintageLabel(it.vintage.vintage)} ${it.wine.displayName}" } ?: "A wine"
            return ConflictView(c, title, emptyList())
        }
        val local = runCatching { json.parseToJsonElement(c.local_payload) as JsonObject }.getOrNull() ?: JsonObject(emptyMap())
        val server = c.server_payload?.let { runCatching { json.parseToJsonElement(it) as JsonObject }.getOrNull() } ?: JsonObject(emptyMap())
        val ignore = setOf("rev", "updated_at", "created_at", "server_updated_at", "collection_id", "id", "producer_sort")
        val diffs = (local.keys + server.keys).filter { it !in ignore }.mapNotNull { k ->
            val a = (local[k] as? JsonPrimitive)?.content ?: ""
            val b = (server[k] as? JsonPrimitive)?.content ?: ""
            if (a == b) null else Triple(k.replace('_', ' '), a.ifEmpty { "—" }.takeIf { it != "null" } ?: "—", b.ifEmpty { "—" }.takeIf { it != "null" } ?: "—")
        }
        val title = (local["producer"] as? JsonPrimitive)?.content ?: (local["name"] as? JsonPrimitive)?.content ?: c.entity.replace('_', ' ')
        return ConflictView(c, title, diffs)
    }

    fun keepMine(id: String) = launchIo { io { graph.syncEngine?.resolveKeepLocal(id) }; graph.sync.syncNow() }
    fun keepTheirs(id: String) = launchIo { io { graph.syncEngine?.resolveKeepServer(id) ?: db.local.resolveConflict(db.now(), id) } }
    fun retryFailed() = launchIo { io { graph.syncEngine?.retryFailed() }; graph.sync.syncNow() }
}

@Composable
fun SyncStatusScreen(onBack: () -> Unit, onOpenVintage: (String) -> Unit, onAccount: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel { SyncStatusViewModel(graph) }
    val conflicts by vm.conflicts.collectAsState()
    val summary by vm.summary.collectAsState()
    val sync by graph.sync.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Sync", onBack = onBack)
        LazyColumn(Modifier.weight(1f)) {
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        !sync.configured -> Text("Sync isn't set up in this build. Your cellar is stored on this device only.", style = Hv.type.body, color = Hv.colors.ink)
                        !sync.signedIn -> {
                            Text("You're not signed in. Changes are kept on this device and will sync after you sign in.", style = Hv.type.body, color = Hv.colors.ink)
                            HvButton("Sign in", onAccount, kind = ButtonKind.SECONDARY)
                        }
                        sync.offline -> Text("Offline. Changes are saved here and will sync when you're back online.", style = Hv.type.body, color = Hv.colors.ink)
                        sync.engine.running -> Text("Syncing…", style = Hv.type.body, color = Hv.colors.ink)
                        else -> Text(sync.engine.lastSuccessAt?.let { "Up to date." } ?: "Not synced yet.", style = Hv.type.body, color = Hv.colors.ink)
                    }
                    val pending = summary.filter { it.first != "FAILED_PERMANENT" }.sumOf { it.second }
                    val failed = summary.filter { it.first == "FAILED_PERMANENT" }.sumOf { it.second }
                    Text("$pending change(s) waiting to upload", style = Hv.type.bodyDense, color = Hv.colors.inkMuted)
                    if (failed > 0) {
                        Text("$failed change(s) were refused by the server (for example, you can only view this household). ${sync.engine.lastError ?: ""}",
                            style = Hv.type.bodyDense, color = Hv.colors.danger)
                        HvButton("Try them again", vm::retryFailed, kind = ButtonKind.SECONDARY)
                    }
                    if (sync.configured && sync.signedIn) HvButton("Sync now", { graph.scope.launch { graph.sync.syncNow() } }, icon = HvIcons.Sync)
                }
            }
            item { SectionHeader("Conflicts") }
            val list = conflicts
            when {
                list == null -> item { LoadingRows(2) }
                list.isEmpty() -> item { Text("None. When the same wine is changed on two devices, you'll choose which version to keep here.",
                    style = Hv.type.bodyDense, color = Hv.colors.inkMuted, modifier = Modifier.padding(horizontal = 16.dp)) }
                else -> items(list, key = { it.raw.id }) { c -> ConflictItem(c, vm, onOpenVintage) }
            }
        }
    }
}

@Composable
private fun ConflictItem(c: ConflictView, vm: SyncStatusViewModel, onOpenVintage: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(c.title, style = Hv.type.body, color = Hv.colors.ink)
        Text(c.raw.reason, style = Hv.type.caption, color = Hv.colors.inkMuted)
        c.differences.forEach { (field, mine, theirs) ->
            Text("$field: this device “$mine”, other device “$theirs”", style = Hv.type.caption, color = Hv.colors.ink)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (c.raw.entity == "stock") {
                TextButton(onClick = { vm.keepTheirs(c.raw.id) }) { Text("I've counted the bottles") }
            } else {
                TextButton(onClick = { vm.keepMine(c.raw.id) }) { Text("Keep this device's") }
                TextButton(onClick = { vm.keepTheirs(c.raw.id) }) { Text("Keep the other") }
            }
        }
    }
    HorizontalDivider(color = Hv.colors.outline)
}

// =================================================================================================
// First run

@Composable
fun FirstRunScreen(onDone: () -> Unit, onSignIn: () -> Unit) {
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
        if (graph.account != null) HvButton("Sign in to restore or sync", { finish(onSignIn) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY)
        Text("Everything works offline. An account is only needed to back up and share.", style = Hv.type.caption, color = Hv.colors.inkMuted)
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

