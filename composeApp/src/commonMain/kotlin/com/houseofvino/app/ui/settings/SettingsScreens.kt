package com.houseofvino.app.ui.settings

import com.houseofvino.data.catalogue.LwinCatalogue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houseofvino.app.AppGraph
import com.houseofvino.app.ui.common.CurrencyDropdown
import com.houseofvino.app.ui.common.DateField
import com.houseofvino.app.ui.common.GraphViewModel
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.common.userMessage
import com.houseofvino.app.ui.design.ButtonKind
import com.houseofvino.app.ui.design.ChoiceChips
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvButton
import com.houseofvino.app.ui.design.HvIcons
import com.houseofvino.app.ui.design.HvTextField
import com.houseofvino.app.ui.design.HvTopBar
import com.houseofvino.app.ui.design.SectionHeader
import com.houseofvino.core.model.RatingScale
import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.ExchangeRate
import com.houseofvino.core.time.DateOrder
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.repo.SettingsRepository
import com.houseofvino.data.repo.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// =================================================================================================
// More tab

@Composable
fun MoreScreen(onLocations: () -> Unit, onInsights: () -> Unit, onImport: () -> Unit, onExport: () -> Unit, onSharing: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HvTopBar("More")
        NavRow("Locations", "Cellars, fridges, racks and bottle positions", onLocations)
        NavRow("Insights", "Totals, value, drinking windows and trends", onInsights)
        NavRow("Import", "Bring in a CSV from another app", onImport)
        NavRow("Export", "Inventory, tasting history and financial records", onExport)
        NavRow("Sharing", "Keep one cellar with a partner's phone", onSharing)
        NavRow("Settings", "Currency, appearance, privacy", onSettings)
    }
}

@Composable
fun NavRow(title: String, subtitle: String?, onClick: () -> Unit, danger: Boolean = false) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(title, style = Hv.type.body, color = if (danger) Hv.colors.danger else Hv.colors.ink)
        subtitle?.let { Text(it, style = Hv.type.caption, color = Hv.colors.inkMuted) }
    }
    HorizontalDivider(color = Hv.colors.outline)
}

// =================================================================================================
// Settings

class SettingsViewModel(graph: AppGraph) : GraphViewModel(graph) {
    private val _prefs = MutableStateFlow(graph.settings.preferences())
    val prefs: StateFlow<com.houseofvino.data.repo.Preferences> = _prefs.asStateFlow()

    init { observe("setting") { _prefs.value = io { graph.settings.preferences() } } }

    fun set(key: String, value: String) = launchIo { io { graph.settings.put(key, value) } }

    fun setReminders(on: Boolean) = launchIo {
        io { graph.settings.put(SettingsRepository.KEY_WINDOW_REMINDERS, on.toString()) }
        val due = io { graph.insights.quickTotals(collectionId, graph.platform.currentYear()).drinkSoonBottles }
        graph.platform.setWindowReminders(on, due)
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit, onSharing: () -> Unit, onRates: () -> Unit, onPrivacy: () -> Unit,
    onExport: () -> Unit, onConflicts: () -> Unit, onErase: () -> Unit,
) {
    val graph = appGraph()
    val vm = viewModel { SettingsViewModel(graph) }
    val p by vm.prefs.collectAsState()
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Settings", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SectionHeader("Sharing")
            NavRow("Share with a partner", "No account or server needed", onSharing)

            SectionHeader("Preferences")
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CurrencyDropdown(p.currency, { vm.set(SettingsRepository.KEY_CURRENCY, it.code) }, label = "Main currency")
                Text("Appearance", style = Hv.type.label, color = Hv.colors.inkMuted)
                ChoiceChips(ThemePreference.entries, setOf(p.theme), { when (it) { ThemePreference.SYSTEM -> "Match device"; ThemePreference.DARK -> "Dark"; ThemePreference.LIGHT -> "Light" } },
                    { vm.set(SettingsRepository.KEY_THEME, it.name) })
                Text("Ratings", style = Hv.type.label, color = Hv.colors.inkMuted)
                ChoiceChips(RatingScale.entries, setOf(p.ratingScale), { when (it) { RatingScale.HUNDRED -> "100 points"; RatingScale.TWENTY -> "20 points"; RatingScale.FIVE_STARS -> "5 stars" } },
                    { vm.set(SettingsRepository.KEY_RATING_SCALE, it.name) })
                Text("Dates in imports", style = Hv.type.label, color = Hv.colors.inkMuted)
                ChoiceChips(DateOrder.entries, setOf(p.dateOrder), { when (it) { DateOrder.DMY -> "Day first"; DateOrder.MDY -> "Month first"; DateOrder.YMD -> "Year first" } },
                    { vm.set(SettingsRepository.KEY_DATE_ORDER, it.name) })
            }
            NavRow("Exchange rates", "Used only to show totals in one currency", onRates)

            SectionHeader("Notifications")
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Drinking-window reminders", style = Hv.type.body, color = Hv.colors.ink)
                    Text("A monthly note when wines reach the last year of their window", style = Hv.type.caption, color = Hv.colors.inkMuted)
                }
                Switch(p.windowReminders, vm::setReminders)
            }

            SectionHeader("Data")
            NavRow("Privacy", "What is stored, where, and who can see it", onPrivacy)
            NavRow("Backup and export",
                if (graph.platform.platformName == "Android") "Phone backups don't include your cellar. Download it as CSV" else "Download all your data as CSV",
                onExport)
            NavRow("Changed on both phones", "Review edits made on two phones at once", onConflicts)
            NavRow("Erase this phone", "Delete everything stored on this phone", onErase, danger = true)
            Text("House of Vino ${graph.platform.appVersion}", style = Hv.type.caption, color = Hv.colors.inkMuted, modifier = Modifier.padding(16.dp))
        }
    }
}

// =================================================================================================
// Exchange rates

class RatesViewModel(graph: AppGraph) : GraphViewModel(graph) {
    private val _rates = MutableStateFlow<List<Pair<String, ExchangeRate>>>(emptyList())
    val rates: StateFlow<List<Pair<String, ExchangeRate>>> = _rates.asStateFlow()
    val base = MutableStateFlow(CurrencyCode.USD)
    val quote = MutableStateFlow(graph.settings.preferences().currency)
    val rate = MutableStateFlow("")
    val source = MutableStateFlow("")
    val asOf = MutableStateFlow(graph.platform.today().toString())
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        observe("exchange_rate") {
            _rates.value = io { db.journal.ratesForCollection(collectionId).executeAsList().mapNotNull { r ->
                runCatching { r.id to ExchangeRate(CurrencyCode.of(r.base), CurrencyCode.of(r.quote), r.rate_e8, IsoDate.parse(r.as_of), r.source) }.getOrNull()
            } }
        }
    }

    fun add() {
        val r = runCatching { ExchangeRate.parseRate(rate.value) }.getOrNull() ?: run { _error.value = "Enter a rate such as 1.0825"; return }
        val d = IsoDate.parseOrNull(asOf.value) ?: run { _error.value = "Use YYYY-MM-DD"; return }
        val er = runCatching { ExchangeRate(base.value, quote.value, r, d, source.value) }.getOrElse { _error.value = it.userMessage(); return }
        _error.value = null
        launchIo { io { graph.settings.addRate(collectionId, er) }; rate.value = "" }
    }

    fun delete(id: String) = launchIo { io { graph.settings.deleteRate(id) } }
}

@Composable
fun RatesScreen(onBack: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel { RatesViewModel(graph) }
    val rates by vm.rates.collectAsState()
    val base by vm.base.collectAsState()
    val quote by vm.quote.collectAsState()
    val rate by vm.rate.collectAsState()
    val source by vm.source.collectAsState()
    val asOf by vm.asOf.collectAsState()
    val error by vm.error.collectAsState()
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Exchange rates", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Totals are always shown per currency. Add a rate if you also want an approximate total in your main currency; it is shown with its source and date.",
                style = Hv.type.bodyDense, color = Hv.colors.inkMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CurrencyDropdown(base, { vm.base.value = it }, Modifier.weight(1f), label = "1 ×")
                CurrencyDropdown(quote, { vm.quote.value = it }, Modifier.weight(1f), label = "equals")
            }
            HvTextField(rate, { vm.rate.value = it }, "Rate", keyboard = KeyboardType.Decimal, placeholder = "1.0825")
            HvTextField(source, { vm.source.value = it }, "Source", placeholder = "e.g. My bank, ECB reference rate")
            DateField("As of", asOf, { vm.asOf.value = it }, null)
            error?.let { Text(it, color = Hv.colors.danger, style = Hv.type.bodyDense) }
            HvButton("Add rate", vm::add, Modifier.fillMaxWidth())
            rates.forEach { (id, r) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("1 ${r.base.code} = ${r.rateText()} ${r.quote.code}", style = Hv.type.body, color = Hv.colors.ink)
                        Text("${r.source}, ${r.asOf}", style = Hv.type.caption, color = Hv.colors.inkMuted)
                    }
                    TextButton(onClick = { vm.delete(id) }) { Text("Remove") }
                }
            }
        }
    }
}

// =================================================================================================
// Privacy

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val platform = appGraph().platform
    val android = platform.platformName == "Android"
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Privacy", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            P("Your cellar is stored on this phone. There's no account, and House of Vino has no server: nothing is uploaded to us.")
            P("If you share with a partner, changes travel as files you send yourself. Each file is encrypted with a key that only your paired phones have, so the apps or services that carry it can't read it.")
            P("Label photos are read on your device. Text recognition doesn't send images anywhere.")
            if (android) {
                P("The scanner uses Google ML Kit, which runs on your device. ML Kit may send Google technical diagnostics (device and app details, how the scanner performed). It doesn't send camera images or the text it reads.")
            }
            P("Barcode lookups send only the barcode number to Open Food Facts, an open product database. Online discovery sends your search words to the same service.")
            P("Wine details suggested from a scan come from the LWIN catalogue built into the app, so matching a label sends nothing anywhere.")
            P("Photos you attach stay on this phone, and travel to your partner's phone inside the encrypted files if you choose to include them.")
            P("The app has no advertising and no analytics of its own.")
            P("You can export everything at any time, and Erase this phone deletes it from this phone.")
            Text("Data sources", style = Hv.type.label, color = Hv.colors.inkMuted)
            P(LwinCatalogue.ATTRIBUTION)
            TextButton(onClick = { platform.openUrl("https://creativecommons.org/licenses/by/4.0/") }) { Text("Read the CC BY 4.0 licence") }
            TextButton(onClick = { platform.openUrl("https://www.liv-ex.com/lwin/") }) { Text("About LWIN (Liv-ex)") }
            P("Barcode details: Open Food Facts, licensed ODbL.")
        }
    }
}

@Composable private fun P(text: String) = Text(text, style = Hv.type.body, color = Hv.colors.ink)

// =================================================================================================
// Erase this phone

@Composable
fun EraseScreen(onBack: () -> Unit, onExport: () -> Unit, onErased: () -> Unit) {
    val graph = appGraph()
    var phrase by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val shared = remember { graph.household.info(graph.collectionId)?.activePeers?.isNotEmpty() == true }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Erase this phone", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("This deletes the cellar, tasting notes, photos and history stored on this phone. It can't be undone.",
                style = Hv.type.body, color = Hv.colors.ink)
            Text(
                if (shared) "Your partner's phone keeps its copy. To carry on sharing after erasing, join again with a new invite."
                else "Nothing is stored anywhere else, so export first if you might want it back.",
                style = Hv.type.bodyDense, color = Hv.colors.inkMuted,
            )
            HvButton("Export my data first", onExport, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY)
            HvTextField(phrase, { phrase = it }, "Type ERASE to confirm")
            HvButton(if (busy) "Erasing…" else "Erase this phone", {
                busy = true
                scope.launch {
                    withContext(Dispatchers.IO) { graph.wipeLocalData() }
                    busy = false
                    onErased()
                }
            }, Modifier.fillMaxWidth(), kind = ButtonKind.DESTRUCTIVE, enabled = phrase.trim() == "ERASE" && !busy)
        }
    }
}
