package com.houseofvino.app.ui.cellar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houseofvino.app.AppGraph
import com.houseofvino.app.ui.common.GraphViewModel
import com.houseofvino.app.ui.common.Load
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.common.userMessage
import com.houseofvino.app.ui.design.BottleSilhouette
import com.houseofvino.app.ui.design.ButtonKind
import com.houseofvino.app.ui.design.ErrorState
import com.houseofvino.app.ui.design.Fact
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvButton
import com.houseofvino.app.ui.design.HvIcons
import com.houseofvino.app.ui.design.HvTopBar
import com.houseofvino.app.ui.design.LoadingRows
import com.houseofvino.app.ui.design.ProvenanceCaption
import com.houseofvino.app.ui.design.QuantityFigure
import com.houseofvino.app.ui.design.SectionHeader
import com.houseofvino.app.ui.design.WindowStatusPill
import com.houseofvino.app.ui.design.formatted
import com.houseofvino.app.ui.design.vintageLabel
import com.houseofvino.app.ui.photos.PhotoImage
import com.houseofvino.core.insights.DrinkingWindow
import com.houseofvino.core.model.BottleFormats
import com.houseofvino.core.model.Ratings
import com.houseofvino.core.model.Tasting
import com.houseofvino.core.model.Valuation
import com.houseofvino.core.search.WindowFilter
import com.houseofvino.data.repo.VintageDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VintageScreenData(val detail: VintageDetail, val tastings: List<Tasting>, val valuations: List<Valuation>)

class VintageDetailViewModel(graph: AppGraph, private val vintageId: String) : GraphViewModel(graph) {
    private val _state = MutableStateFlow<Load<VintageScreenData>>(Load.Loading)
    val state: StateFlow<Load<VintageScreenData>> = _state.asStateFlow()

    init {
        observe("wine", "vintage", "lot", "movement", "tasting", "valuation", "photo", "tag", "vintage_tag", "wine_grape", "location") { load() }
    }

    private suspend fun load() {
        _state.value = try {
            val d = io { graph.cellar.vintageDetail(vintageId) }
            if (d == null) Load.Failed("This wine has been removed.")
            else Load.Ready(VintageScreenData(d, io { graph.journal.forVintage(vintageId) }, io { graph.cellar.valuations(vintageId) }))
        } catch (e: Exception) { Load.Failed(e.userMessage()) }
    }

    fun delete(onDone: () -> Unit) = launchIo {
        io { graph.cellar.deleteVintage(vintageId) }
        messenger.show("Wine removed. Tasting notes are kept in your journal.")
        onDone()
    }
}

@Composable
fun VintageDetailScreen(
    vintageId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onStockAction: (String) -> Unit,
    onAddLot: () -> Unit,
    onHistory: () -> Unit,
    onAddTasting: () -> Unit,
    onOpenTasting: (String) -> Unit,
    onValuations: () -> Unit,
    onOpenVintage: (String) -> Unit,
    onAddVintage: (wineId: String) -> Unit,
    onAddToWishlist: () -> Unit,
) {
    val graph = appGraph()
    val vm = viewModel(key = "vintage-$vintageId") { VintageDetailViewModel(graph, vintageId) }
    val state by vm.state.collectAsState()
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val year = graph.platform.currentYear()

    Column(Modifier.fillMaxSize()) {
        HvTopBar("", onBack = onBack, actions = {
            IconButton(onClick = { onEdit(vintageId) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit wine") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More actions") }
                DropdownMenu(menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Movement history") }, onClick = { menu = false; onHistory() })
                    DropdownMenuItem(text = { Text("Valuations") }, onClick = { menu = false; onValuations() })
                    DropdownMenuItem(text = { Text("Add to wishlist") }, onClick = { menu = false; onAddToWishlist() })
                    DropdownMenuItem(text = { Text("Delete wine") }, onClick = { menu = false; confirmDelete = true })
                }
            }
        })
        when (val s = state) {
            Load.Loading -> LoadingRows(4)
            is Load.Failed -> ErrorState(s.message, null)
            is Load.Ready -> DetailBody(s.value, year, onStockAction, onAddLot, onAddTasting, onOpenTasting, onValuations, onOpenVintage, onAddVintage)
        }
    }
    if (confirmDelete) {
        val bottles = (state as? Load.Ready)?.value?.detail?.totalBottles ?: 0
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this wine?") },
            text = {
                Text(if (bottles > 0) "You still have $bottles bottle(s). Record them as drunk, sold or lost first, so your history stays accurate."
                     else "The wine is removed from your cellar. Tasting notes and purchase history are kept.")
            },
            confirmButton = { if (bottles == 0) TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(if (bottles > 0) "OK" else "Cancel") } },
        )
    }
}

@Composable
private fun DetailBody(
    data: VintageScreenData,
    year: Int,
    onStockAction: (String) -> Unit,
    onAddLot: () -> Unit,
    onAddTasting: () -> Unit,
    onOpenTasting: (String) -> Unit,
    onValuations: () -> Unit,
    onOpenVintage: (String) -> Unit,
    onAddVintage: (String) -> Unit,
) {
    val d = data.detail
    val w = d.wine
    val v = d.vintage
    val status = DrinkingWindow.status(v.drinkFrom, v.drinkUntil, year)
    val scale = appGraph().settings.preferences().ratingScale
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        // Header: the vintage numeral is the signature element.
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(vintageLabel(v.vintage), style = Hv.type.vintageDisplay, color = Hv.colors.ink,
                    modifier = Modifier.semantics { contentDescription = "Vintage ${vintageLabel(v.vintage)}" })
                Text(w.producer, style = Hv.type.label, color = Hv.colors.inkMuted)
                Text(w.name ?: w.appellation ?: w.producer, style = Hv.type.title, color = Hv.colors.ink, modifier = Modifier.semantics { heading() })
                Text(listOfNotNull(w.appellation.takeIf { w.name != null }, w.region, w.country).distinct().joinToString(", "),
                    style = Hv.type.bodyDense, color = Hv.colors.inkMuted)
            }
            Box(Modifier.size(96.dp, 144.dp).clip(RoundedCornerShape(6.dp))) {
                val labelPath = d.labelPhotoPath
                if (labelPath != null) PhotoImage(labelPath, "Label photo", Modifier.fillMaxSize())
                else BottleSilhouette(w.color, Modifier.fillMaxSize())
            }
        }

        // Stock and the primary actions.
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                QuantityFigure(d.totalBottles, Modifier.testTag("bottle-count"))
                Text(if (d.totalBottles == 1) "bottle" else "bottles", style = Hv.type.caption, color = Hv.colors.inkMuted)
            }
            if (status != WindowFilter.UNKNOWN) Column(horizontalAlignment = Alignment.End) {
                WindowStatusPill(status)
                Text(listOfNotNull(v.drinkFrom, v.drinkUntil).joinToString("–"), style = Hv.type.caption, color = Hv.colors.inkMuted)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HvButton("Drink", { onStockAction("CONSUME") }, Modifier.weight(1f), enabled = d.totalBottles > 0, icon = HvIcons.Glass)
            HvButton("Add purchase", onAddLot, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HvButton("Move", { onStockAction("TRANSFER") }, Modifier.weight(1f), kind = ButtonKind.QUIET, enabled = d.totalBottles > 0, icon = HvIcons.Transfer)
            HvButton("Sell or give", { onStockAction("SELL") }, Modifier.weight(1f), kind = ButtonKind.QUIET, enabled = d.totalBottles > 0)
            HvButton("Count", { onStockAction("ADJUST") }, Modifier.weight(1f), kind = ButtonKind.QUIET, enabled = d.lots.isNotEmpty())
        }

        if (d.stock.isNotEmpty()) {
            SectionHeader("Where it is")
            d.stock.forEach { line ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).semantics(mergeDescendants = true) {}) {
                    Column(Modifier.weight(1f)) {
                        Text(line.locationName ?: "Not put away", style = Hv.type.body, color = Hv.colors.ink)
                        Text(listOfNotNull(BottleFormats.nameFor(line.formatMl), line.acquiredOn?.let { "bought $it" }, line.source).joinToString(" · "),
                            style = Hv.type.caption, color = Hv.colors.inkMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(line.quantity.toString(), style = Hv.type.figure, color = if (line.quantity < 0) Hv.colors.danger else Hv.colors.ink)
                        line.unitPrice?.let { Text(it.formatted(), style = Hv.type.caption, color = Hv.colors.inkMuted) }
                    }
                }
                HorizontalDivider(Modifier.padding(start = 16.dp), color = Hv.colors.outline)
            }
            if (d.stock.any { it.quantity < 0 }) {
                Text("More bottles were recorded as removed than were in stock — probably changes made on two devices. Count the bottles and use Count to correct it.",
                    style = Hv.type.bodyDense, color = Hv.colors.danger, modifier = Modifier.padding(16.dp))
            }
        }

        SectionHeader("Tasting notes", trailing = { TextButton(onClick = onAddTasting) { Text("Add note") } })
        if (data.tastings.isEmpty()) {
            Text("No notes yet. When you open a bottle, “Drink” lets you write one.", style = Hv.type.bodyDense, color = Hv.colors.inkMuted,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        data.tastings.take(5).forEach { t ->
            Column(Modifier.fillMaxWidth().clickable { onOpenTasting(t.id) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row {
                    Text(t.tastedOn.toString(), style = Hv.type.label, color = Hv.colors.inkMuted, modifier = Modifier.weight(1f))
                    t.rating?.let { Text(Ratings.display(it, scale), style = Hv.type.figure, color = Hv.colors.ink) }
                }
                t.notes?.let { Text(it, style = Hv.type.body, color = Hv.colors.ink, maxLines = 3) }
                t.occasion?.let { Text(it, style = Hv.type.caption, color = Hv.colors.inkMuted) }
            }
        }

        SectionHeader("Value", trailing = { TextButton(onClick = onValuations) { Text("Add value") } })
        val latest = data.valuations.firstOrNull()
        if (latest == null) {
            Text("No estimate recorded. Add one with its source and date; the app never guesses market prices.",
                style = Hv.type.bodyDense, color = Hv.colors.inkMuted, modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("${latest.amount.formatted()} per ${BottleFormats.nameFor(latest.formatMl).lowercase()}", style = Hv.type.body, color = Hv.colors.ink)
                ProvenanceCaption(latest.source, latest.asOf.toString())
            }
        }

        SectionHeader("Details")
        Fact("Grapes", w.grapes.joinToString(", ") { g -> g.grape + (g.percent?.let { " $it%" } ?: "") }.ifEmpty { null })
        Fact("Colour", w.color?.name?.lowercase()?.replaceFirstChar { it.uppercase() })
        Fact("Style", w.style?.name?.lowercase()?.replaceFirstChar { it.uppercase() })
        Fact("Appellation", w.appellation)
        Fact("Classification", w.classification)
        Fact("Sub-region", w.subRegion)
        Fact("Alcohol", v.abvBasisPoints?.let { "${it / 100}.${(it % 100).toString().padStart(2, '0').trimEnd('0').ifEmpty { "0" }}%" })
        Fact("Window source", v.drinkWindowSource)
        Fact("Notes", listOfNotNull(w.notes, v.notes).joinToString("\n").ifEmpty { null })
        if (d.tags.isNotEmpty()) {
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                d.tags.forEach { AssistChip(onClick = {}, label = { Text(it.name) }) }
            }
        }

        SectionHeader("Other vintages", trailing = { TextButton(onClick = { onAddVintage(w.id) }) { Text("Add vintage") } })
        if (d.otherVintages.isEmpty()) {
            Text("Only this vintage so far.", style = Hv.type.bodyDense, color = Hv.colors.inkMuted, modifier = Modifier.padding(horizontal = 16.dp))
        }
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            d.otherVintages.forEach { ov -> AssistChip(onClick = { onOpenVintage(ov.id) }, label = { Text(vintageLabel(ov.vintage)) }) }
        }
        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.width(1.dp))
    }
}
