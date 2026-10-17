package com.houseofvino.app.ui.wishlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houseofvino.app.AppGraph
import com.houseofvino.app.ui.common.CurrencyDropdown
import com.houseofvino.app.ui.common.DateField
import com.houseofvino.app.ui.common.GraphViewModel
import com.houseofvino.app.ui.common.Load
import com.houseofvino.app.ui.common.VintageInput
import com.houseofvino.app.ui.common.VintageKindChoice
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.common.userMessage
import com.houseofvino.app.ui.design.ButtonKind
import com.houseofvino.app.ui.design.ChoiceChips
import com.houseofvino.app.ui.design.EmptyState
import com.houseofvino.app.ui.design.ErrorState
import com.houseofvino.app.ui.design.Fact
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvButton
import com.houseofvino.app.ui.design.HvTextField
import com.houseofvino.app.ui.design.HvTopBar
import com.houseofvino.app.ui.design.LoadingRows
import com.houseofvino.app.ui.design.QuantityStepper
import com.houseofvino.app.ui.design.SectionHeader
import com.houseofvino.app.ui.design.formatted
import com.houseofvino.app.ui.design.vintageLabel
import com.houseofvino.core.model.BottleFormats
import com.houseofvino.core.model.MerchantLink
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.WishlistItem
import com.houseofvino.core.model.WishlistPriority
import com.houseofvino.core.model.WishlistStatus
import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.Money
import com.houseofvino.core.search.SearchQuery
import com.houseofvino.core.search.SearchScope
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.repo.MerchantLinkInput
import com.houseofvino.data.repo.WishlistInput
import com.houseofvino.data.search.WishlistRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private val OPEN = setOf("WANTED", "WATCHING", "ORDERED")

class WishlistViewModel(graph: AppGraph) : GraphViewModel(graph) {
    private val _state = MutableStateFlow<Load<List<WishlistRow>>>(Load.Loading)
    val state: StateFlow<Load<List<WishlistRow>>> = _state.asStateFlow()
    val showClosed = MutableStateFlow(false)

    init { observe("wishlist_item", "merchant_link", "wine", "vintage") { reload() } }

    suspend fun reload() {
        val statuses = if (showClosed.value) OPEN + setOf("ACQUIRED", "CANCELLED") else OPEN
        _state.value = try {
            Load.Ready(io { graph.search.searchWishlist(collectionId, SearchQuery(scope = SearchScope.WISHLIST), statuses, 0, 1000).items })
        } catch (e: Exception) { Load.Failed(e.userMessage()) }
    }

    fun toggleClosed() { showClosed.update { !it }; launchIo { reload() } }
}

fun priorityLabel(p: String) = when (p) { "MUST" -> "Must have"; "HIGH" -> "High"; "MEDIUM" -> "Medium"; else -> "Low" }
fun statusLabel(s: String) = when (s) { "WANTED" -> "Wanted"; "WATCHING" -> "Watching price"; "ORDERED" -> "Ordered"; "ACQUIRED" -> "In cellar"; else -> "Cancelled" }

@Composable
fun WishlistScreen(onOpen: (String) -> Unit, onAdd: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel { WishlistViewModel(graph) }
    val state by vm.state.collectAsState()
    val showClosed by vm.showClosed.collectAsState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HvTopBar("Wishlist", actions = { TextButton(onClick = vm::toggleClosed) { Text(if (showClosed) "Hide closed" else "Show closed") } })
            when (val s = state) {
                Load.Loading -> LoadingRows()
                is Load.Failed -> ErrorState(s.message, null)
                is Load.Ready -> if (s.value.isEmpty()) {
                    EmptyState("Nothing on your wishlist", "Keep track of wines you want, with target prices and where to buy them.", action = "Add a wine" to onAdd)
                } else LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(s.value, key = { it.itemId }) { row -> WishlistRowItem(row) { onOpen(row.itemId) } }
                }
            }
        }
        if ((state as? Load.Ready)?.value?.isNotEmpty() == true) {
            ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Add") },
                containerColor = Hv.colors.accent, contentColor = Hv.colors.onAccent, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
        }
    }
}

@Composable
private fun WishlistRowItem(row: WishlistRow, onClick: () -> Unit) {
    val target = row.targetPriceMinor?.let { m -> row.targetPriceCurrency?.let { c -> CurrencyCode.ofOrNull(c)?.let { Money(m, it) } } }
    val best = row.bestPriceMinor?.let { m -> target?.let { Money(m, it.currency) } }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).semantics(mergeDescendants = true) {}.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(row.vintage?.let { vintageLabel(it) } ?: "—", style = Hv.type.vintageRow, color = Hv.colors.ink, modifier = Modifier.padding(end = 12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.producer ?: "", style = Hv.type.label, color = Hv.colors.inkMuted)
            row.wineName?.let { Text(it, style = Hv.type.wineName, color = Hv.colors.ink, maxLines = 1) }
            Text(listOfNotNull(priorityLabel(row.priority), statusLabel(row.status), "×${row.desiredQuantity}").joinToString(" · "),
                style = Hv.type.caption, color = Hv.colors.inkMuted)
        }
        Column(horizontalAlignment = Alignment.End) {
            target?.let { Text("Target ${it.formatted()}", style = Hv.type.caption, color = Hv.colors.inkMuted) }
            best?.let { Text("Seen ${it.formatted()}", style = Hv.type.label, color = if (it <= target!!) Hv.colors.ready else Hv.colors.ink) }
        }
    }
    HorizontalDivider(Modifier.padding(start = 16.dp), color = Hv.colors.outline)
}

// =================================================================================================
// Editor

data class WishlistForm(
    val producer: String = "", val name: String = "",
    val vintageKind: VintageKindChoice = VintageKindChoice.YEAR, val year: String = "",
    val formatMl: Int? = null, val quantity: Int = 1, val price: String = "", val currency: CurrencyCode = CurrencyCode.EUR,
    val priority: WishlistPriority = WishlistPriority.MEDIUM, val notes: String = "",
    val linkedTitle: String? = null,
)

class WishlistEditorViewModel(graph: AppGraph, private val itemId: String?, private val vintageId: String?) : GraphViewModel(graph) {
    private val _form = MutableStateFlow(WishlistForm(currency = graph.settings.preferences().currency))
    val form: StateFlow<WishlistForm> = _form.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _saved = MutableStateFlow<String?>(null)
    val saved: StateFlow<String?> = _saved.asStateFlow()
    private var existing: WishlistItem? = null

    init {
        launchIo {
            existing = itemId?.let { io { graph.wishlist.item(it) } }
            val vid = existing?.vintageId ?: vintageId
            val title = vid?.let { io { graph.cellar.vintageDetail(it) } }?.let { "${vintageLabel(it.vintage.vintage)} ${it.wine.displayName}" }
            existing?.let { e ->
                _form.update {
                    it.copy(producer = e.producer.orEmpty(), name = e.name.orEmpty(),
                        vintageKind = when (e.vintage) { is VintageYear.Year -> VintageKindChoice.YEAR; VintageYear.NonVintage -> VintageKindChoice.NV; else -> VintageKindChoice.UNKNOWN },
                        year = e.vintage?.yearOrNull?.toString().orEmpty(), formatMl = e.formatMl, quantity = e.desiredQuantity,
                        price = e.targetPrice?.toPlainString().orEmpty(), currency = e.targetPrice?.currency ?: it.currency,
                        priority = e.priority, notes = e.notes.orEmpty())
                }
            }
            _form.update { it.copy(linkedTitle = title) }
        }
    }

    fun edit(t: (WishlistForm) -> WishlistForm) { _form.update(t); _error.value = null }

    fun save() {
        val f = _form.value
        val linked = existing?.vintageId ?: vintageId
        if (linked == null && f.producer.isBlank()) { _error.value = "Enter at least the producer"; return }
        val price = f.price.takeIf { it.isNotBlank() }?.let { Money.parseOrNull(it, f.currency) ?: run { _error.value = "Enter a target such as 60.00"; return } }
        val vintage = when (f.vintageKind) {
            VintageKindChoice.YEAR -> f.year.toIntOrNull()?.takeIf { it in 1700..2200 }?.let { VintageYear.Year(it) }
            VintageKindChoice.NV -> VintageYear.NonVintage
            VintageKindChoice.UNKNOWN -> null
        }
        val input = WishlistInput(vintageId = linked, wineId = existing?.wineId, producer = f.producer.takeIf { linked == null },
            name = f.name.takeIf { linked == null }, vintage = if (linked == null) vintage else null, formatMl = f.formatMl,
            desiredQuantity = f.quantity, targetPrice = price, priority = f.priority, notes = f.notes)
        launchIo(onError = { _error.value = it.userMessage() }) {
            val id = io { if (itemId != null) { graph.wishlist.update(itemId, input); itemId } else graph.wishlist.add(collectionId, input) }
            messenger.show("Saved to wishlist")
            _saved.value = id
        }
    }
}

@Composable
fun WishlistEditorScreen(itemId: String?, vintageId: String?, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val graph = appGraph()
    val vm = viewModel(key = "wle-$itemId-$vintageId") { WishlistEditorViewModel(graph, itemId, vintageId) }
    val f by vm.form.collectAsState()
    val error by vm.error.collectAsState()
    val saved by vm.saved.collectAsState()
    LaunchedEffect(saved) { saved?.let(onSaved) }
    Column(Modifier.fillMaxSize()) {
        HvTopBar(if (itemId == null) "Add to wishlist" else "Edit wishlist item", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (f.linkedTitle != null) {
                Text(f.linkedTitle!!, style = Hv.type.wineName, color = Hv.colors.ink)
            } else {
                HvTextField(f.producer, { v -> vm.edit { it.copy(producer = v) } }, "Producer")
                HvTextField(f.name, { v -> vm.edit { it.copy(name = v) } }, "Wine (optional)")
                VintageInput(f.vintageKind, f.year, { k -> vm.edit { it.copy(vintageKind = k) } }, { y -> vm.edit { it.copy(year = y) } }, null)
            }
            QuantityStepper(f.quantity, { q -> vm.edit { it.copy(quantity = q) } }, "Bottles wanted", min = 1)
            Text("Bottle size", style = Hv.type.label, color = Hv.colors.inkMuted)
            ChoiceChips(listOf(375, 750, 1500), setOfNotNull(f.formatMl), { BottleFormats.nameFor(it) }, { ml -> vm.edit { it.copy(formatMl = if (it.formatMl == ml) null else ml) } })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HvTextField(f.price, { v -> vm.edit { it.copy(price = v) } }, "Target price per bottle", Modifier.weight(1.4f), keyboard = KeyboardType.Decimal)
                CurrencyDropdown(f.currency, { c -> vm.edit { it.copy(currency = c) } }, Modifier.weight(1f))
            }
            Text("Priority", style = Hv.type.label, color = Hv.colors.inkMuted)
            ChoiceChips(WishlistPriority.entries, setOf(f.priority), { priorityLabel(it.name) }, { p -> vm.edit { it.copy(priority = p) } })
            HvTextField(f.notes, { v -> vm.edit { it.copy(notes = v) } }, "Notes", singleLine = false, minLines = 2)
            error?.let { Text(it, color = Hv.colors.danger, style = Hv.type.bodyDense) }
            HvButton("Save", vm::save, Modifier.fillMaxWidth().padding(vertical = 16.dp))
        }
    }
}

// =================================================================================================
// Detail: status, merchant links, add to cellar

data class WishlistDetail(val item: WishlistItem, val title: String, val links: List<MerchantLink>)

class WishlistDetailViewModel(graph: AppGraph, private val itemId: String) : GraphViewModel(graph) {
    private val _state = MutableStateFlow<Load<WishlistDetail>>(Load.Loading)
    val state: StateFlow<Load<WishlistDetail>> = _state.asStateFlow()

    init {
        observe("wishlist_item", "merchant_link") {
            val item = io { graph.wishlist.item(itemId) }
            _state.value = if (item == null || item.updatedAt < 0) Load.Failed("This item was removed.") else {
                val title = item.vintageId?.let { io { graph.cellar.vintageDetail(it) } }?.let { "${vintageLabel(it.vintage.vintage)} ${it.wine.displayName}" }
                    ?: listOfNotNull(item.vintage?.let(::vintageLabel), item.producer, item.name).joinToString(" ")
                Load.Ready(WishlistDetail(item, title, io { graph.wishlist.links(itemId) }))
            }
        }
    }

    fun setStatus(s: WishlistStatus) = launchIo { io { graph.wishlist.setStatus(itemId, s) } }
    fun delete(onDone: () -> Unit) = launchIo { io { graph.wishlist.delete(itemId) }; onDone() }
    fun addLink(merchant: String, url: String, price: String, currency: CurrencyCode, checked: String) = launchIo {
        val money = price.takeIf { it.isNotBlank() }?.let { Money.parse(it, currency) }
        io { graph.wishlist.addLink(collectionId, itemId, MerchantLinkInput(merchant, url.ifBlank { null }, money, IsoDate.parseOrNull(checked))) }
    }
    fun deleteLink(id: String) = launchIo { io { graph.wishlist.deleteLink(id) } }
}

@Composable
fun WishlistDetailScreen(itemId: String, onBack: () -> Unit, onEdit: () -> Unit, onAddToCellar: (WishlistItem) -> Unit) {
    val graph = appGraph()
    val vm = viewModel(key = "wld-$itemId") { WishlistDetailViewModel(graph, itemId) }
    val state by vm.state.collectAsState()
    var addingLink by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Wishlist", onBack = onBack, actions = { IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit") } })
        when (val s = state) {
            Load.Loading -> LoadingRows(3)
            is Load.Failed -> ErrorState(s.message, null)
            is Load.Ready -> {
                val d = s.value
                val item = d.item
                Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
                    Text(d.title, style = Hv.type.title, color = Hv.colors.ink, modifier = Modifier.padding(horizontal = 16.dp))
                    Fact("Status", statusLabel(item.status.name))
                    Fact("Priority", priorityLabel(item.priority.name))
                    Fact("Wanted", "${item.desiredQuantity} × ${item.formatMl?.let { BottleFormats.nameFor(it) } ?: "any size"}")
                    Fact("Target", item.targetPrice?.formatted())
                    Fact("Notes", item.notes)
                    if (item.status.isOpen) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HvButton("Add to cellar", { onAddToCellar(item) }, Modifier.weight(1f))
                            HvButton(if (item.status == WishlistStatus.ORDERED) "Not ordered" else "Mark ordered",
                                { vm.setStatus(if (item.status == WishlistStatus.ORDERED) WishlistStatus.WANTED else WishlistStatus.ORDERED) },
                                Modifier.weight(1f), kind = ButtonKind.SECONDARY)
                        }
                        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (item.status != WishlistStatus.ORDERED) HvButton(if (item.status == WishlistStatus.WATCHING) "Stop watching" else "Watch price",
                                { vm.setStatus(if (item.status == WishlistStatus.WATCHING) WishlistStatus.WANTED else WishlistStatus.WATCHING) }, kind = ButtonKind.QUIET)
                            HvButton("Cancel", { vm.setStatus(WishlistStatus.CANCELLED) }, kind = ButtonKind.QUIET)
                        }
                    } else if (item.status == WishlistStatus.CANCELLED || item.status == WishlistStatus.ACQUIRED) {
                        HvButton("Want it again", { vm.setStatus(WishlistStatus.WANTED) }, Modifier.padding(16.dp), kind = ButtonKind.SECONDARY)
                    }
                    SectionHeader("Where to buy", trailing = { TextButton(onClick = { addingLink = true }) { Text("Add") } })
                    if (d.links.isEmpty()) Text("Add merchants and the price you saw, with the date you checked.", style = Hv.type.bodyDense,
                        color = Hv.colors.inkMuted, modifier = Modifier.padding(horizontal = 16.dp))
                    d.links.forEach { link ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable(enabled = link.url != null) { link.url?.let(graph.platform::openUrl) }) {
                                Text(link.merchant, style = Hv.type.body, color = if (link.url != null) Hv.colors.accent else Hv.colors.ink)
                                Text(listOfNotNull(link.lastPrice?.formatted(), link.checkedOn?.let { "checked $it" },
                                    if (link.meetsTarget(item.targetPrice)) "at or below target" else null).joinToString(" · "),
                                    style = Hv.type.caption, color = Hv.colors.inkMuted)
                            }
                            TextButton(onClick = { vm.deleteLink(link.id) }) { Text("Remove") }
                        }
                    }
                    HvButton("Delete from wishlist", { confirmDelete = true }, Modifier.padding(16.dp), kind = ButtonKind.QUIET)
                }
            }
        }
    }
    if (addingLink) LinkDialog(graph.settings.preferences().currency, graph.platform.today().toString(), onDismiss = { addingLink = false }) { m, u, p, c, d ->
        addingLink = false; vm.addLink(m, u, p, c, d)
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete from wishlist?") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
    )
}

@Composable
private fun LinkDialog(defaultCurrency: CurrencyCode, today: String, onDismiss: () -> Unit, onSave: (String, String, String, CurrencyCode, String) -> Unit) {
    var merchant by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var checked by remember { mutableStateOf(today) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add merchant") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HvTextField(merchant, { merchant = it }, "Merchant")
                HvTextField(url, { url = it }, "Link (optional)", keyboard = KeyboardType.Uri, placeholder = "https://")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HvTextField(price, { price = it }, "Price seen", Modifier.weight(1.3f), keyboard = KeyboardType.Decimal)
                    CurrencyDropdown(currency, { currency = it }, Modifier.weight(1f))
                }
                DateField("Checked on", checked, { checked = it }, null)
                error?.let { Text(it, color = Hv.colors.danger, style = Hv.type.caption) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    merchant.isBlank() -> "Enter the merchant"
                    url.isNotBlank() && !(url.startsWith("https://") || url.startsWith("http://")) -> "Links start with https://"
                    price.isNotBlank() && Money.parseOrNull(price, currency) == null -> "Enter a price such as 55.00"
                    else -> null
                }
                if (error == null) onSave(merchant, url, price, currency, checked)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
