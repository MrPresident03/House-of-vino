package com.houseofvino.app.ui.settings

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
import com.houseofvino.data.remote.Membership
import com.houseofvino.data.repo.SettingsRepository
import com.houseofvino.data.repo.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// =================================================================================================
// More tab

@Composable
fun MoreScreen(onLocations: () -> Unit, onInsights: () -> Unit, onImport: () -> Unit, onExport: () -> Unit, onSync: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HvTopBar("More")
        NavRow("Locations", "Cellars, fridges, racks and bottle positions", onLocations)
        NavRow("Insights", "Totals, value, drinking windows and trends", onInsights)
        NavRow("Import", "Bring in a CSV from another app", onImport)
        NavRow("Export", "Inventory, tasting history and financial records", onExport)
        NavRow("Sync", "Status, pending changes and conflicts", onSync)
        NavRow("Settings", "Account, household, currency, privacy", onSettings)
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
    onBack: () -> Unit, onAccount: () -> Unit, onHousehold: () -> Unit, onRates: () -> Unit, onPrivacy: () -> Unit,
    onExport: () -> Unit, onSync: () -> Unit, onDeleteAccount: () -> Unit,
) {
    val graph = appGraph()
    val vm = viewModel { SettingsViewModel(graph) }
    val p by vm.prefs.collectAsState()
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Settings", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SectionHeader("Account")
            val signedIn = graph.account?.currentUser()
            NavRow(if (signedIn != null) "Signed in as ${signedIn.email ?: "you"}" else "Sign in to sync",
                if (graph.account == null) "Sync isn't set up in this build; everything stays on this device." else "Back up and use on other devices", onAccount)
            if (graph.account != null) NavRow("Household", "Share your cellar with family or a partner", onHousehold)

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
            NavRow("Sync status", null, onSync)
            if (graph.account != null) NavRow("Delete account", "Permanently delete your account and synced data", onDeleteAccount, danger = true)
            Text("House of Vino ${graph.platform.appVersion}", style = Hv.type.caption, color = Hv.colors.inkMuted, modifier = Modifier.padding(16.dp))
        }
    }
}

// =================================================================================================
// Account: sign in / sign out

class AccountViewModel(graph: AppGraph) : GraphViewModel(graph) {
    val email = MutableStateFlow("")
    val password = MutableStateFlow("")
    val code = MutableStateFlow("")
    val codeSent = MutableStateFlow(false)
    val busy = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private fun run(block: suspend () -> Unit) {
        busy.value = true; _error.value = null
        launchIo(onError = { e -> busy.value = false; _error.value = e.userMessage() }) { block(); busy.value = false }
    }

    fun signIn() = run { graph.account!!.signIn(email.value, password.value); messenger.show("Signed in. Syncing…"); graph.sync.syncNow() }
    fun signUp() = run { graph.account!!.signUp(email.value, password.value); messenger.show("Check your email to confirm your account") }
    fun sendCode() = run { graph.account!!.sendCode(email.value); codeSent.value = true }
    fun verifyCode() = run { graph.account!!.verifyCode(email.value, code.value); messenger.show("Signed in. Syncing…"); graph.sync.syncNow() }
    fun signOut() = run { graph.account!!.signOut(); messenger.show("Signed out. Your cellar stays on this device.") }
}

@Composable
fun AccountScreen(onBack: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel { AccountViewModel(graph) }
    val email by vm.email.collectAsState()
    val password by vm.password.collectAsState()
    val code by vm.code.collectAsState()
    val codeSent by vm.codeSent.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val sync by graph.sync.state.collectAsState()
    var usePassword by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Account", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (graph.account == null) {
                Text("This build has no sync server configured, so everything is stored only on this device. Export regularly to keep a backup.",
                    style = Hv.type.body, color = Hv.colors.ink)
                return@Column
            }
            val user = graph.account.currentUser()
            if (user != null && sync.signedIn) {
                Text("Signed in as ${user.email}", style = Hv.type.body, color = Hv.colors.ink)
                HvButton("Sync now", { graph.scope.launch { graph.sync.syncNow() } }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, icon = HvIcons.Sync)
                HvButton("Sign out", vm::signOut, Modifier.fillMaxWidth(), kind = ButtonKind.QUIET, enabled = !busy)
                Text("Signing out keeps your cellar on this device. Changes made while signed out sync next time you sign in.",
                    style = Hv.type.caption, color = Hv.colors.inkMuted)
                return@Column
            }
            Text("Sign in to back up your cellar and use it on your other devices. You can keep using the app without an account.",
                style = Hv.type.body, color = Hv.colors.ink)
            HvTextField(email, { vm.email.value = it }, "Email", keyboard = KeyboardType.Email)
            if (usePassword) {
                androidx.compose.material3.TextField(password, { vm.password.value = it }, label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                HvButton("Sign in", vm::signIn, Modifier.fillMaxWidth(), enabled = !busy && email.isNotBlank() && password.length >= 8)
                HvButton("Create account", vm::signUp, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, enabled = !busy && email.isNotBlank() && password.length >= 8)
                Text("Passwords need at least 8 characters.", style = Hv.type.caption, color = Hv.colors.inkMuted)
            } else if (!codeSent) {
                HvButton("Email me a sign-in code", vm::sendCode, Modifier.fillMaxWidth(), enabled = !busy && email.contains('@'))
            } else {
                HvTextField(code, { vm.code.value = it.filter(Char::isDigit).take(8) }, "Code from the email", keyboard = KeyboardType.Number)
                HvButton("Sign in", vm::verifyCode, Modifier.fillMaxWidth(), enabled = !busy && code.length >= 6)
            }
            TextButton(onClick = { usePassword = !usePassword }) { Text(if (usePassword) "Use an email code instead" else "Use a password instead") }
            error?.let { Text(it, color = Hv.colors.danger, style = Hv.type.bodyDense) }
        }
    }
}

// =================================================================================================
// Household

class HouseholdViewModel(graph: AppGraph) : GraphViewModel(graph) {
    private val _members = MutableStateFlow<List<Membership>>(emptyList())
    val members: StateFlow<List<Membership>> = _members.asStateFlow()
    val inviteEmail = MutableStateFlow("")
    val inviteRole = MutableStateFlow("editor")
    val token = MutableStateFlow<String?>(null)
    val acceptToken = MutableStateFlow("")
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { refresh() }

    fun refresh() = launchIo(onError = { _error.value = it.userMessage() }) { _members.value = graph.account?.memberships().orEmpty() }

    fun invite() = launchIo(onError = { _error.value = it.userMessage() }) {
        token.value = graph.account!!.invite(collectionId, inviteEmail.value, inviteRole.value)
    }

    fun accept() = launchIo(onError = { _error.value = it.userMessage() }) {
        val cid = graph.account!!.acceptInvite(acceptToken.value)
        messenger.show("You've joined the household. Its wines will appear after the next sync.")
        io { graph.settings.put("joined_collection_$cid", "true") }
        refresh()
    }
}

@Composable
fun HouseholdScreen(onBack: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel { HouseholdViewModel(graph) }
    val members by vm.members.collectAsState()
    val email by vm.inviteEmail.collectAsState()
    val role by vm.inviteRole.collectAsState()
    val token by vm.token.collectAsState()
    val accept by vm.acceptToken.collectAsState()
    val error by vm.error.collectAsState()
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Household", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (graph.account?.currentUser() == null) {
                Text("Sign in first to share your cellar.", style = Hv.type.body, color = Hv.colors.ink)
                return@Column
            }
            Text("Your collections", style = Hv.type.sectionTitle, color = Hv.colors.ink)
            members.forEach { m -> Text("${m.name} · ${m.role}", style = Hv.type.body, color = Hv.colors.ink) }
            SectionHeader("Invite someone", Modifier.padding(start = 0.dp))
            Text("Editors can add and drink bottles; viewers can only look. Invites expire after 7 days and only work for the email you enter.",
                style = Hv.type.bodyDense, color = Hv.colors.inkMuted)
            HvTextField(email, { vm.inviteEmail.value = it }, "Their email", keyboard = KeyboardType.Email)
            ChoiceChips(listOf("editor", "viewer"), setOf(role), { if (it == "editor") "Editor" else "Viewer" }, { vm.inviteRole.value = it })
            HvButton("Create invite", vm::invite, Modifier.fillMaxWidth(), enabled = email.contains('@'))
            token?.let {
                Text("Send them this invite code. It is shown only once:", style = Hv.type.bodyDense, color = Hv.colors.ink)
                Text(it, style = Hv.type.figure, color = Hv.colors.accent)
                HvButton("Share code", { graph.scope.launch { graph.platform.shareText("house-of-vino-invite.txt", "text/plain", "Join my cellar on House of Vino with this code: $it") } },
                    kind = ButtonKind.SECONDARY)
            }
            SectionHeader("Join a household", Modifier.padding(start = 0.dp))
            HvTextField(accept, { vm.acceptToken.value = it.trim() }, "Invite code")
            HvButton("Join", vm::accept, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, enabled = accept.length >= 32)
            error?.let { Text(it, color = Hv.colors.danger, style = Hv.type.bodyDense) }
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
    val android = appGraph().platform.platformName == "Android"
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Privacy", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            P("Your cellar is stored on this device first. Nothing leaves it unless you sign in.")
            P("When you sign in, your data is synced to a private database. Only you, and people you invite to your household, can read it. This is enforced by the server, not just the app.")
            P("Label photos are read on your device. Text recognition doesn't send images anywhere.")
            if (android) {
                P("The scanner uses Google ML Kit, which runs on your device. ML Kit may send Google technical diagnostics (device and app details, how the scanner performed). It doesn't send camera images or the text it reads.")
            }
            P("Barcode lookups send only the barcode number to Open Food Facts, an open product database. Online discovery sends your search words to the same service.")
            P("Photos you attach are uploaded to private storage when you're signed in, and are visible only to your household.")
            P("The app has no advertising and no analytics of its own.")
            P("You can export everything at any time, and deleting your account permanently removes your synced data.")
        }
    }
}

@Composable private fun P(text: String) = Text(text, style = Hv.type.body, color = Hv.colors.ink)

// =================================================================================================
// Delete account

@Composable
fun DeleteAccountScreen(onBack: () -> Unit, onExport: () -> Unit, onDeleted: () -> Unit) {
    val graph = appGraph()
    var phrase by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var wipeLocal by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Delete account", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("This permanently deletes your account, every collection you own (including shared households), their photos and history. It can't be undone.",
                style = Hv.type.body, color = Hv.colors.ink)
            Text("People you've shared a household with lose access to it. Households you joined are not affected.", style = Hv.type.bodyDense, color = Hv.colors.inkMuted)
            HvButton("Export my data first", onExport, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Also erase this device's copy", style = Hv.type.body, color = Hv.colors.ink, modifier = Modifier.weight(1f))
                Switch(wipeLocal, { wipeLocal = it })
            }
            HvTextField(phrase, { phrase = it }, "Type DELETE to confirm")
            error?.let { Text(it, color = Hv.colors.danger, style = Hv.type.bodyDense) }
            HvButton(if (busy) "Deleting…" else "Delete my account", {
                busy = true; error = null
                scope.launch {
                    try {
                        graph.account!!.deleteAccount()
                        if (wipeLocal) graph.wipeLocalData()
                        onDeleted()
                    } catch (e: Exception) {
                        error = "Couldn't delete the account. ${e.userMessage()} You need to be online; nothing was deleted on this device."
                    } finally { busy = false }
                }
            }, Modifier.fillMaxWidth(), kind = ButtonKind.DESTRUCTIVE, enabled = phrase == "DELETE" && !busy)
        }
    }
}
