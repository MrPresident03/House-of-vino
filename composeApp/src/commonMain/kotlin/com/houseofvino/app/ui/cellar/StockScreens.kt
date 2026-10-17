package com.houseofvino.app.ui.cellar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houseofvino.app.AppGraph
import com.houseofvino.app.ui.common.CurrencyDropdown
import com.houseofvino.app.ui.common.DateField
import com.houseofvino.app.ui.common.Dropdown
import com.houseofvino.app.ui.common.GraphViewModel
import com.houseofvino.app.ui.common.LocationDropdown
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.common.userMessage
import com.houseofvino.app.ui.design.ButtonKind
import com.houseofvino.app.ui.design.EmptyState
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvButton
import com.houseofvino.app.ui.design.HvIcons
import com.houseofvino.app.ui.design.HvTextField
import com.houseofvino.app.ui.design.HvTopBar
import com.houseofvino.app.ui.design.ProvenanceCaption
import com.houseofvino.app.ui.design.QuantityStepper
import com.houseofvino.app.ui.design.SectionHeader
import com.houseofvino.app.ui.design.formatted
import com.houseofvino.core.inventory.MovementKind
import com.houseofvino.core.model.BottleFormats
import com.houseofvino.core.model.Valuation
import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.Money
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.repo.LocationNode
import com.houseofvino.data.repo.LotInput
import com.houseofvino.data.repo.MovementLine
import com.houseofvino.data.repo.StockLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// =================================================================================================
// Stock actions: drink, sell/give/lose, move, count

data class StockActionState(
    val action: String,
    val kind: MovementKind = MovementKind.CONSUME,
    val title: String = "",
    val lines: List<StockLine> = emptyList(),
    val selected: Int = 0,
    val quantity: Int = 1,
    val toLocationId: String? = null,
    val locations: List<LocationNode> = emptyList(),
    val amount: String = "",
    val currency: CurrencyCode = CurrencyCode.EUR,
    val counterparty: String = "",
    val note: String = "",
    val writeNote: Boolean = true,
    val counted: Int = 0,
    val error: String? = null,
    val busy: Boolean = false,
    val done: Boolean = false,
    /** When drinking with a note, the editor takes over the consumption. */
    val openNoteFor: Pair<String, String?>? = null,
)

class StockActionViewModel(graph: AppGraph, private val vintageId: String, action: String) : GraphViewModel(graph) {
    private val _state = MutableStateFlow(StockActionState(
        action = action,
        kind = when (action) { "SELL" -> MovementKind.SELL; "ADJUST" -> MovementKind.ADJUST; "TRANSFER" -> MovementKind.TRANSFER_OUT; else -> MovementKind.CONSUME },
        currency = graph.settings.preferences().currency,
    ))
    val state: StateFlow<StockActionState> = _state.asStateFlow()

    init {
        launchIo {
            val lines = io { graph.cellar.stockLines(vintageId) }.ifEmpty {
                // Counting a lot with no stock left: offer each lot at "not put away".
                io { graph.cellar.vintageDetail(vintageId)?.lots.orEmpty().map { StockLine(it.id, null, null, 0, it.formatMl, it.acquiredOn, it.unitPrice, it.source) } }
            }
            val title = io { graph.cellar.vintageDetail(vintageId)?.let { "${it.wine.displayName} ${com.houseofvino.app.ui.design.vintageLabel(it.vintage.vintage)}" } }.orEmpty()
            val locations = io { graph.locations.tree(collectionId) }
            _state.update { it.copy(lines = lines, title = title, locations = locations, counted = lines.firstOrNull()?.quantity ?: 0) }
        }
    }

    fun edit(t: (StockActionState) -> StockActionState) = _state.update { t(it).copy(error = null) }

    fun select(index: Int) = _state.update { it.copy(selected = index, quantity = 1, counted = it.lines.getOrNull(index)?.quantity ?: 0) }

    fun submit() {
        val s = _state.value
        val line = s.lines.getOrNull(s.selected) ?: return
        if (s.action == "CONSUME" && s.writeNote) {
            // The tasting editor records the consumption and the note in one transaction.
            _state.update { it.copy(openNoteFor = line.lotId to line.locationId) }
            return
        }
        val amount = s.amount.takeIf { it.isNotBlank() }?.let { Money.parseOrNull(it, s.currency) ?: run {
            _state.update { st -> st.copy(error = "Enter an amount such as 120.00") }; return
        } }
        _state.update { it.copy(busy = true) }
        launchIo(onError = { e -> _state.update { it.copy(busy = false, error = e.userMessage()) } }) {
            val message = io {
                when (s.action) {
                    "TRANSFER" -> {
                        graph.cellar.transfer(line.lotId, line.locationId, s.toLocationId, s.quantity, s.note)
                        "Moved ${plural(s.quantity)}"
                    }
                    "ADJUST" -> {
                        val m = graph.cellar.adjust(line.lotId, line.locationId, s.counted, s.note.ifBlank { "Stock count" })
                        if (m == null) "Count matches — nothing to change" else "Stock corrected to ${s.counted}"
                    }
                    else -> {
                        val m = graph.cellar.depart(line.lotId, line.locationId, s.quantity, s.kind, amount, s.counterparty, s.note)
                        val verb = when (s.kind) { MovementKind.CONSUME -> "Drank"; MovementKind.SELL -> "Sold"; MovementKind.GIFT_OUT -> "Gave away"; else -> "Recorded loss of" }
                        undoable("$verb ${plural(s.quantity)}", m.id)
                        null
                    }
                }
            }
            message?.let { messenger.show(it) }
            _state.update { it.copy(busy = false, done = true) }
        }
    }

    private fun undoable(text: String, movementId: String) {
        // The screen closes after recording, so the undo runs in the app scope, not this view model's.
        messenger.show(text, "Undo") {
            graph.scope.launch {
                try {
                    graph.cellar.reverse(movementId, "Undone")
                    messenger.show("Undone")
                } catch (e: Exception) {
                    messenger.show(e.userMessage())
                }
            }
        }
    }

    private fun plural(n: Int) = if (n == 1) "1 bottle" else "$n bottles"
}

@Composable
fun StockActionScreen(vintageId: String, action: String, onBack: () -> Unit, onDone: () -> Unit, onWriteNote: (lotId: String, locationId: String?) -> Unit) {
    val graph = appGraph()
    val vm = viewModel(key = "stock-$vintageId-$action") { StockActionViewModel(graph, vintageId, action) }
    val s by vm.state.collectAsState()
    LaunchedEffect(s.done) { if (s.done) onDone() }
    LaunchedEffect(s.openNoteFor) { s.openNoteFor?.let { (lot, loc) -> vm.edit { it.copy(openNoteFor = null) }; onWriteNote(lot, loc) } }

    val title = when (action) { "TRANSFER" -> "Move bottles"; "ADJUST" -> "Count bottles"; "SELL" -> "Sell, give or write off"; else -> "Drink a bottle" }
    Column(Modifier.fillMaxSize()) {
        HvTopBar(title, onBack = onBack)
        if (s.lines.isEmpty()) {
            EmptyState("No bottles in stock", "Add a purchase first.")
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(s.title, style = Hv.type.wineName, color = Hv.colors.ink)
            if (s.lines.size > 1 || action == "ADJUST") {
                Text(if (action == "TRANSFER") "Move from" else "Which bottles", style = Hv.type.label, color = Hv.colors.inkMuted)
                s.lines.forEachIndexed { i, line ->
                    Row(Modifier.fillMaxWidth().clickable(role = Role.RadioButton) { vm.select(i) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = s.selected == i, onClick = { vm.select(i) })
                        Column(Modifier.weight(1f)) {
                            Text(line.locationName ?: "Not put away", style = Hv.type.body, color = Hv.colors.ink)
                            Text(listOfNotNull(BottleFormats.nameFor(line.formatMl), line.acquiredOn?.let { "bought $it" }).joinToString(" · "),
                                style = Hv.type.caption, color = Hv.colors.inkMuted)
                        }
                        Text(line.quantity.toString(), style = Hv.type.figure, color = Hv.colors.ink)
                    }
                }
            }
            val available = s.lines.getOrNull(s.selected)?.quantity ?: 0
            when (action) {
                "ADJUST" -> {
                    QuantityStepper(s.counted, { v -> vm.edit { it.copy(counted = v) } }, "Bottles actually there", min = 0)
                    Text("Recorded: $available. The difference is saved as a correction; history isn't rewritten.", style = Hv.type.caption, color = Hv.colors.inkMuted)
                }
                "TRANSFER" -> {
                    QuantityStepper(s.quantity, { v -> vm.edit { it.copy(quantity = v) } }, "Bottles", min = 1, max = available.coerceAtLeast(1))
                    LocationDropdown(s.locations, s.toLocationId, { id -> vm.edit { it.copy(toLocationId = id) } }, label = "Move to")
                }
                "SELL" -> {
                    Text("What happened", style = Hv.type.label, color = Hv.colors.inkMuted)
                    com.houseofvino.app.ui.design.ChoiceChips(listOf(MovementKind.SELL, MovementKind.GIFT_OUT, MovementKind.LOSS), setOf(s.kind),
                        { when (it) { MovementKind.SELL -> "Sold"; MovementKind.GIFT_OUT -> "Gift"; else -> "Broken or spoiled" } },
                        { k -> vm.edit { it.copy(kind = k) } })
                    QuantityStepper(s.quantity, { v -> vm.edit { it.copy(quantity = v) } }, "Bottles", min = 1, max = available.coerceAtLeast(1))
                    if (s.kind != MovementKind.LOSS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HvTextField(s.amount, { v -> vm.edit { it.copy(amount = v) } }, if (s.kind == MovementKind.SELL) "Total received" else "Value (optional)",
                                Modifier.weight(1.4f), keyboard = KeyboardType.Decimal)
                            CurrencyDropdown(s.currency, { c -> vm.edit { it.copy(currency = c) } }, Modifier.weight(1f))
                        }
                        HvTextField(s.counterparty, { v -> vm.edit { it.copy(counterparty = v) } }, if (s.kind == MovementKind.SELL) "Sold to" else "Given to")
                    }
                }
                else -> {
                    QuantityStepper(s.quantity, { v -> vm.edit { it.copy(quantity = v) } }, "Bottles", min = 1, max = available.coerceAtLeast(1))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { vm.edit { it.copy(writeNote = !it.writeNote) } }) {
                        Checkbox(s.writeNote && s.quantity == 1, { on -> vm.edit { it.copy(writeNote = on) } }, enabled = s.quantity == 1)
                        Text("Write a tasting note", style = Hv.type.body, color = Hv.colors.ink)
                    }
                }
            }
            HvTextField(s.note, { v -> vm.edit { it.copy(note = v) } }, "Note (optional)")
            s.error?.let { Text(it, style = Hv.type.bodyDense, color = Hv.colors.danger) }
            HvButton(
                when (action) { "TRANSFER" -> "Move"; "ADJUST" -> "Save count"; "SELL" -> "Record"; else -> if (s.writeNote && s.quantity == 1) "Continue to note" else "Drink" },
                { if (action == "CONSUME" && s.quantity > 1) vm.edit { it.copy(writeNote = false) }; vm.submit() },
                Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag("stock-submit"), enabled = !s.busy && (available > 0 || action == "ADJUST"),
            )
        }
    }
}

// =================================================================================================
// Add a purchase to an existing vintage

class AddLotViewModel(graph: AppGraph, private val vintageId: String, private val wishlistItemId: String?) : GraphViewModel(graph) {
    private val _form = MutableStateFlow(WineForm(currency = graph.settings.preferences().currency))
    val form: StateFlow<WineForm> = _form.asStateFlow()
    private val _errors = MutableStateFlow<Map<String, String>>(emptyMap())
    val errors: StateFlow<Map<String, String>> = _errors.asStateFlow()
    private val _locations = MutableStateFlow<List<LocationNode>>(emptyList())
    val locations: StateFlow<List<LocationNode>> = _locations.asStateFlow()
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        launchIo {
            _locations.value = io { graph.locations.tree(collectionId) }
            io { graph.cellar.stockLines(vintageId).firstOrNull() }?.let { last -> _form.update { it.copy(formatMl = last.formatMl, locationId = last.locationId) } }
        }
    }

    fun edit(t: (WineForm) -> WineForm) { _form.update(t); _errors.value = emptyMap() }

    fun save() {
        val f = _form.value
        val errors = buildMap {
            if (f.price.isNotBlank() && Money.parseOrNull(f.price, f.currency) == null) put("price", "Enter an amount such as 45.00")
            if (f.acquiredOn.isNotBlank() && IsoDate.parseOrNull(f.acquiredOn) == null) put("acquiredOn", "Use YYYY-MM-DD")
        }
        if (errors.isNotEmpty()) { _errors.value = errors; return }
        launchIo {
            io {
                graph.cellar.addLot(collectionId, vintageId, LotInput(
                    quantity = f.quantity, formatMl = f.formatMl, locationId = f.locationId, acquiredOn = IsoDate.parseOrNull(f.acquiredOn),
                    source = f.source, unitPrice = f.price.takeIf { it.isNotBlank() }?.let { Money.parse(it, f.currency) }, wishlistItemId = wishlistItemId,
                ))
                wishlistItemId?.let { graph.wishlist.markAcquiredIfOpen(it, vintageId) }
            }
            messenger.show("Added ${f.quantity} bottle${if (f.quantity == 1) "" else "s"}")
            _saved.value = true
        }
    }
}

@Composable
fun AddLotScreen(vintageId: String, wishlistItemId: String?, onBack: () -> Unit, onSaved: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel(key = "lot-$vintageId") { AddLotViewModel(graph, vintageId, wishlistItemId) }
    val f by vm.form.collectAsState()
    val errors by vm.errors.collectAsState()
    val locations by vm.locations.collectAsState()
    val saved by vm.saved.collectAsState()
    LaunchedEffect(saved) { if (saved) onSaved() }
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Add purchase", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp)) {
            PurchaseFields(f, errors, locations) { vm.edit(it) }
            HvButton("Add bottles", vm::save, Modifier.fillMaxWidth().padding(vertical = 16.dp))
        }
    }
}

// =================================================================================================
// Movement history with undo

class HistoryViewModel(graph: AppGraph, private val vintageId: String) : GraphViewModel(graph) {
    private val _lines = MutableStateFlow<List<MovementLine>?>(null)
    val lines: StateFlow<List<MovementLine>?> = _lines.asStateFlow()
    init { observe("movement", "lot", "location") { _lines.value = io { graph.cellar.movementHistory(vintageId) } } }

    fun undo(movementId: String) = launchIo {
        io { graph.cellar.reverse(movementId, "Undone from history") }
        messenger.show("Reversed. The original entry stays in the history.")
    }
}

@Composable
fun HistoryScreen(vintageId: String, onBack: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel(key = "history-$vintageId") { HistoryViewModel(graph, vintageId) }
    val lines by vm.lines.collectAsState()
    val offset = graph.platform.utcOffsetMinutes()
    Column(Modifier.fillMaxSize()) {
        HvTopBar("History", onBack = onBack)
        val list = lines
        when {
            list == null -> com.houseofvino.app.ui.design.LoadingRows()
            list.isEmpty() -> EmptyState("No movements yet", "Purchases, bottles drunk and moves appear here.")
            else -> LazyColumn {
                items(list, key = { it.movement.id }) { line ->
                    val m = line.movement
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(kindLabel(m.kind), style = Hv.type.body, color = if (line.reversed) Hv.colors.inkMuted else Hv.colors.ink)
                            Text(listOfNotNull(IsoDate.fromEpochMillis(m.occurredAt, offset).toString(), line.locationName ?: "not put away",
                                BottleFormats.nameFor(line.formatMl), m.counterparty, m.note, if (line.reversed) "reversed" else null).joinToString(" · "),
                                style = Hv.type.caption, color = Hv.colors.inkMuted)
                            m.amount?.let { Text(it.formatted(), style = Hv.type.caption, color = Hv.colors.inkMuted) }
                        }
                        Text((if (m.delta > 0) "+" else "") + m.delta, style = Hv.type.figure, color = Hv.colors.ink)
                        if (!line.reversed && m.kind != MovementKind.REVERSAL) {
                            TextButton(onClick = { vm.undo(m.id) }) { Text("Undo") }
                        }
                    }
                    HorizontalDivider(color = Hv.colors.outline)
                }
            }
        }
    }
}

fun kindLabel(k: MovementKind): String = when (k) {
    MovementKind.PURCHASE -> "Purchased"; MovementKind.OPENING -> "Added to cellar"; MovementKind.CONSUME -> "Drunk"
    MovementKind.SELL -> "Sold"; MovementKind.GIFT_OUT -> "Given away"; MovementKind.GIFT_IN -> "Received as a gift"
    MovementKind.LOSS -> "Broken or spoiled"; MovementKind.TRANSFER_OUT -> "Moved out"; MovementKind.TRANSFER_IN -> "Moved in"
    MovementKind.ADJUST -> "Count correction"; MovementKind.REVERSAL -> "Reversal"
}

// =================================================================================================
// Valuations

class ValuationsViewModel(graph: AppGraph, private val vintageId: String) : GraphViewModel(graph) {
    private val _list = MutableStateFlow<List<Valuation>>(emptyList())
    val list: StateFlow<List<Valuation>> = _list.asStateFlow()
    val amount = MutableStateFlow("")
    val currency = MutableStateFlow(graph.settings.preferences().currency)
    val source = MutableStateFlow("")
    val asOf = MutableStateFlow(graph.platform.today().toString())
    val formatMl = MutableStateFlow(750)
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { observe("valuation") { _list.value = io { graph.cellar.valuations(vintageId) } } }

    fun add() {
        val money = Money.parseOrNull(amount.value, currency.value) ?: run { _error.value = "Enter an amount such as 250.00"; return }
        if (source.value.isBlank()) { _error.value = "Say where the value comes from (e.g. auction result, merchant price)"; return }
        val date = IsoDate.parseOrNull(asOf.value) ?: run { _error.value = "Use YYYY-MM-DD"; return }
        _error.value = null
        launchIo {
            io { graph.cellar.addValuation(collectionId, vintageId, formatMl.value, money, source.value, date, null) }
            amount.value = ""; source.value = ""
            messenger.show("Value saved")
        }
    }

    fun delete(id: String) = launchIo { io { graph.cellar.deleteValuation(id) } }
}

@Composable
fun ValuationsScreen(vintageId: String, onBack: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel(key = "val-$vintageId") { ValuationsViewModel(graph, vintageId) }
    val list by vm.list.collectAsState()
    val amount by vm.amount.collectAsState()
    val currency by vm.currency.collectAsState()
    val source by vm.source.collectAsState()
    val asOf by vm.asOf.collectAsState()
    val formatMl by vm.formatMl.collectAsState()
    val error by vm.error.collectAsState()
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Value", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Record an estimate you trust, with where it came from. Insights show the source and date next to every total.",
                style = Hv.type.bodyDense, color = Hv.colors.inkMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HvTextField(amount, { vm.amount.value = it }, "Value per bottle", Modifier.weight(1.4f), keyboard = KeyboardType.Decimal)
                CurrencyDropdown(currency, { vm.currency.value = it }, Modifier.weight(1f))
            }
            Dropdown("Bottle size", listOf(375, 750, 1500, 3000), formatMl, { BottleFormats.nameFor(it) }, { vm.formatMl.value = it })
            HvTextField(source, { vm.source.value = it }, "Source", placeholder = "e.g. Auction result, merchant list price")
            DateField("As of", asOf, { vm.asOf.value = it }, null)
            error?.let { Text(it, color = Hv.colors.danger, style = Hv.type.bodyDense) }
            HvButton("Save value", vm::add, Modifier.fillMaxWidth())
            SectionHeader("History", Modifier.padding(start = 0.dp))
            list.forEach { v ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${v.amount.formatted()} · ${BottleFormats.nameFor(v.formatMl)}", style = Hv.type.body, color = Hv.colors.ink)
                        ProvenanceCaption(v.source, v.asOf.toString())
                    }
                    TextButton(onClick = { vm.delete(v.id) }) { Text("Remove") }
                }
            }
        }
    }
}

// =================================================================================================
// Add chooser

@Composable
fun AddChooserScreen(onBack: () -> Unit, onManual: () -> Unit, onScanBarcode: () -> Unit, onScanLabel: () -> Unit, onImport: () -> Unit) {
    val graph = appGraph()
    val scanning = graph.platform.scanningAvailable()
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Add wine", onBack = onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HvButton("Enter details", onManual, Modifier.fillMaxWidth(), icon = HvIcons.Bottle)
            HvButton("Scan barcode", onScanBarcode, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, enabled = scanning, icon = HvIcons.Barcode)
            HvButton("Read label", onScanLabel, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, enabled = scanning, icon = HvIcons.Label)
            HvButton("Import a spreadsheet", onImport, Modifier.fillMaxWidth(), kind = ButtonKind.QUIET)
            if (!scanning) Text("Scanning needs a camera. You can always enter details by hand.", style = Hv.type.caption, color = Hv.colors.inkMuted)
            Text("Scans only suggest details. You confirm the producer, vintage and bottle size before anything is saved.",
                style = Hv.type.caption, color = Hv.colors.inkMuted)
        }
    }
}
