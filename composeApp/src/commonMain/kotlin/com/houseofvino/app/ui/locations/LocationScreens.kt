package com.houseofvino.app.ui.locations

import com.houseofvino.app.ui.photos.PhotoImage
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houseofvino.app.AppGraph
import com.houseofvino.app.ui.common.Dropdown
import com.houseofvino.app.ui.common.GraphViewModel
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.common.userMessage
import com.houseofvino.app.ui.design.ButtonKind
import com.houseofvino.app.ui.design.ChoiceChips
import com.houseofvino.app.ui.design.EmptyState
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvButton
import com.houseofvino.app.ui.design.HvTextField
import com.houseofvino.app.ui.design.HvTopBar
import com.houseofvino.app.ui.design.LedgerRow
import com.houseofvino.app.ui.design.SectionHeader
import com.houseofvino.app.ui.design.vintageLabel
import com.houseofvino.core.model.Location
import com.houseofvino.core.model.LocationKind
import com.houseofvino.core.search.SearchFilters
import com.houseofvino.core.search.SearchQuery
import com.houseofvino.core.search.SortOrder
import com.houseofvino.data.repo.LocationInput
import com.houseofvino.data.repo.LocationNode
import com.houseofvino.data.repo.SlotView
import com.houseofvino.data.search.CellarRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun kindLabel(k: LocationKind) = k.name.lowercase().replaceFirstChar(Char::uppercase)

class LocationsViewModel(graph: AppGraph) : GraphViewModel(graph) {
    private val _nodes = MutableStateFlow<List<LocationNode>?>(null)
    val nodes: StateFlow<List<LocationNode>?> = _nodes.asStateFlow()
    private val _unassigned = MutableStateFlow(0)
    val unassigned: StateFlow<Int> = _unassigned.asStateFlow()

    init {
        observe("location", "movement", "lot") {
            _nodes.value = io { graph.locations.tree(collectionId) }
            _unassigned.value = io { graph.locations.unassignedBottles(collectionId) }
        }
    }
}

@Composable
fun LocationsScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onAdd: (parentId: String?) -> Unit) {
    val graph = appGraph()
    val vm = viewModel { LocationsViewModel(graph) }
    val nodes by vm.nodes.collectAsState()
    val unassigned by vm.unassigned.collectAsState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HvTopBar("Locations", onBack = onBack)
            val list = nodes
            if (list != null && list.isEmpty()) {
                EmptyState("No locations yet", "Add your cellars, fridges and racks. Bottle positions are optional.", action = "Add location" to { onAdd(null) })
            } else if (list != null) LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(list, key = { it.location.id }) { n ->
                    Row(Modifier.fillMaxWidth().clickable { onOpen(n.location.id) }.padding(start = (16 + n.depth * 20).dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
                        .semantics(mergeDescendants = true) { contentDescription = "${n.location.name}, ${kindLabel(n.location.kind)}, ${n.bottlesTotal} bottles" },
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(n.location.name, style = Hv.type.body, color = Hv.colors.ink)
                            Text(listOfNotNull(kindLabel(n.location.kind), n.location.gridRows?.let { "${it} × ${n.location.gridCols} grid" }).joinToString(" · "),
                                style = Hv.type.caption, color = Hv.colors.inkMuted)
                        }
                        Text(n.bottlesTotal.toString(), style = Hv.type.figure, color = Hv.colors.ink)
                    }
                    HorizontalDivider(color = Hv.colors.outline)
                }
                if (unassigned > 0) item {
                    Text("$unassigned bottle(s) aren't in a location yet. Use Move on a wine to put them away.", style = Hv.type.bodyDense,
                        color = Hv.colors.inkMuted, modifier = Modifier.padding(16.dp))
                }
            }
        }
        ExtendedFloatingActionButton(onClick = { onAdd(null) }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Add location") },
            containerColor = Hv.colors.accent, contentColor = Hv.colors.onAccent, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}

// =================================================================================================

class LocationDetailViewModel(graph: AppGraph, private val locationId: String) : GraphViewModel(graph) {
    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()
    private val _path = MutableStateFlow("")
    val path: StateFlow<String> = _path.asStateFlow()
    private val _rows = MutableStateFlow<List<CellarRow>>(emptyList())
    val rows: StateFlow<List<CellarRow>> = _rows.asStateFlow()
    private val _grid = MutableStateFlow<List<SlotView>>(emptyList())
    val grid: StateFlow<List<SlotView>> = _grid.asStateFlow()
    /** Lots stored directly here, for placing into positions. */
    private val _placeable = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val placeable: StateFlow<List<Pair<String, String>>> = _placeable.asStateFlow()

    init {
        observe("location", "movement", "lot", "slot", "wine", "vintage") {
            _location.value = io { graph.locations.byId(locationId) }
            _path.value = io { graph.locations.pathName(locationId) }
            val page = io { graph.search.searchCellar(collectionId, SearchQuery(filters = SearchFilters(locationIds = setOf(locationId)), sort = SortOrder.PRODUCER), 0, 500, graph.platform.currentYear(), null) }
            _rows.value = page.items
            _grid.value = io { graph.locations.grid(locationId) }
            _placeable.value = io {
                page.items.flatMap { row ->
                    graph.cellar.stockLines(row.vintageId).filter { it.locationId == locationId && it.quantity > 0 }
                        .map { it.lotId to "${vintageLabel(row.vintage)} ${row.producer}${row.wineName?.let { n -> " $n" } ?: ""}" }
                }
            }
        }
    }

    fun place(row: Int, col: Int, lotId: String) = launchIo { io { graph.locations.placeBottle(locationId, row, col, lotId) } }
    fun clear(row: Int, col: Int) = launchIo { io { graph.locations.clearPosition(locationId, row, col) } }
    fun delete(onDone: () -> Unit) = launchIo { io { graph.locations.delete(locationId) }; messenger.show("Location deleted"); onDone() }
}

@Composable
fun LocationDetailScreen(locationId: String, onBack: () -> Unit, onEdit: () -> Unit, onAddChild: () -> Unit, onOpenVintage: (String) -> Unit) {
    val graph = appGraph()
    val vm = viewModel(key = "loc-$locationId") { LocationDetailViewModel(graph, locationId) }
    val location by vm.location.collectAsState()
    val path by vm.path.collectAsState()
    val rows by vm.rows.collectAsState()
    val grid by vm.grid.collectAsState()
    val placeable by vm.placeable.collectAsState()
    var picking by remember { mutableStateOf<SlotView?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val year = graph.platform.currentYear()

    Column(Modifier.fillMaxSize()) {
        HvTopBar(location?.name ?: "", onBack = onBack, actions = { IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit location") } })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Text(path, style = Hv.type.caption, color = Hv.colors.inkMuted, modifier = Modifier.padding(horizontal = 16.dp))
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HvButton("Add location inside", onAddChild, kind = ButtonKind.SECONDARY)
                    HvButton("Delete", { confirmDelete = true }, kind = ButtonKind.QUIET)
                }
            }
            if (grid.isNotEmpty()) item {
                SectionHeader("Positions")
                SlotGrid(location!!, grid) { picking = it }
                Text("Tap a position to place or remove a bottle stored here.", style = Hv.type.caption, color = Hv.colors.inkMuted, modifier = Modifier.padding(horizontal = 16.dp))
            }
            item { SectionHeader("Wines here and inside") }
            if (rows.isEmpty()) item { Text("No bottles here.", style = Hv.type.bodyDense, color = Hv.colors.inkMuted, modifier = Modifier.padding(horizontal = 16.dp)) }
            items(rows, key = { it.vintageId }) { r -> LedgerRow(r, year, onClick = { onOpenVintage(r.vintageId) }) }
        }
    }
    picking?.let { slot ->
        AlertDialog(
            onDismissRequest = { picking = null },
            title = { Text(slot.label ?: "Row ${slot.row}, position ${slot.col}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (slot.lotId != null) {
                        Text("Row ${slot.row}, position ${slot.col}", style = Hv.type.body)
                        slot.vintageId?.let { v -> TextButton(onClick = { picking = null; onOpenVintage(v) }) { Text("Open this wine") } }
                    }
                    else if (placeable.isEmpty()) Text("No bottles stored directly in this location. Move bottles here first.", style = Hv.type.body)
                    else placeable.distinctBy { it.first }.forEach { (lotId, label) ->
                        TextButton(onClick = { vm.place(slot.row, slot.col, lotId); picking = null }) { Text(label) }
                    }
                }
            },
            confirmButton = { if (slot.lotId != null) TextButton(onClick = { vm.clear(slot.row, slot.col); picking = null }) { Text("Remove from position") } },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Close") } },
        )
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete this location?") },
        text = { Text("Only empty locations can be deleted. Move bottles and inner locations first.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
    )
}

private fun slotColor(color: String?): Color = when (color) {
    "RED" -> Color(0xFF7A1F2B); "WHITE" -> Color(0xFFE6D89A); "ROSE" -> Color(0xFFE8A0A8); "ORANGE" -> Color(0xFFD98A3D)
    else -> Color(0xFF8A7A6A)
}

@Composable
private fun SlotGrid(location: Location, slots: List<SlotView>, onTap: (SlotView) -> Unit) {
    val cols = location.gridCols ?: return
    Column(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        slots.chunked(cols).forEach { rowSlots ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                rowSlots.forEach { s ->
                    val filled = s.lotId != null
                    // Filled positions show the bottle's label photo (or its colour), so you can see what's where.
                    Box(Modifier.size(48.dp).clip(CircleShape)
                        .background(if (!filled) Hv.colors.surfaceRaised else slotColor(s.color))
                        .border(if (filled) 2.dp else 1.dp, if (filled) Hv.colors.accent else Hv.colors.outline, CircleShape)
                        .clickable { onTap(s) }
                        .semantics { contentDescription = "Row ${s.row} position ${s.col}, ${s.label ?: if (filled) "bottle placed" else "empty"}" },
                        contentAlignment = Alignment.Center) {
                        if (s.photoPath != null) PhotoImage(s.photoPath, null, Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

// =================================================================================================

class LocationEditorViewModel(graph: AppGraph, private val locationId: String?, parentId: String?) : GraphViewModel(graph) {
    val name = MutableStateFlow("")
    val kind = MutableStateFlow(if (parentId == null) LocationKind.CELLAR else LocationKind.RACK)
    val parent = MutableStateFlow(parentId)
    val hasGrid = MutableStateFlow(false)
    val rows = MutableStateFlow("")
    val cols = MutableStateFlow("")
    val notes = MutableStateFlow("")
    private val _nodes = MutableStateFlow<List<LocationNode>>(emptyList())
    val nodes: StateFlow<List<LocationNode>> = _nodes.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _saved = MutableStateFlow<String?>(null)
    val saved: StateFlow<String?> = _saved.asStateFlow()

    init {
        launchIo {
            _nodes.value = io { graph.locations.tree(collectionId) }.filter { it.location.id != locationId }
            locationId?.let { id -> io { graph.locations.byId(id) } }?.let { l ->
                name.value = l.name; kind.value = l.kind; parent.value = l.parentId; hasGrid.value = l.hasGrid
                rows.value = l.gridRows?.toString().orEmpty(); cols.value = l.gridCols?.toString().orEmpty(); notes.value = l.notes.orEmpty()
            }
        }
    }

    fun save() {
        val r = if (hasGrid.value) rows.value.toIntOrNull() else null
        val c = if (hasGrid.value) cols.value.toIntOrNull() else null
        if (hasGrid.value && (r == null || c == null || r !in 1..200 || c !in 1..200)) { _error.value = "Rows and positions must be between 1 and 200"; return }
        val input = LocationInput(name.value, kind.value, parent.value, r, c, 0, notes.value)
        launchIo(onError = { _error.value = it.userMessage() }) {
            val id = io { if (locationId == null) graph.locations.create(collectionId, input) else { graph.locations.update(locationId, input); locationId } }
            _saved.value = id
        }
    }
}

@Composable
fun LocationEditorScreen(locationId: String?, parentId: String?, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val graph = appGraph()
    val vm = viewModel(key = "le-$locationId-$parentId") { LocationEditorViewModel(graph, locationId, parentId) }
    val name by vm.name.collectAsState()
    val kind by vm.kind.collectAsState()
    val parent by vm.parent.collectAsState()
    val hasGrid by vm.hasGrid.collectAsState()
    val rows by vm.rows.collectAsState()
    val cols by vm.cols.collectAsState()
    val notes by vm.notes.collectAsState()
    val nodes by vm.nodes.collectAsState()
    val error by vm.error.collectAsState()
    val saved by vm.saved.collectAsState()
    LaunchedEffect(saved) { saved?.let(onSaved) }
    Column(Modifier.fillMaxSize()) {
        HvTopBar(if (locationId == null) "Add location" else "Edit location", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HvTextField(name, { vm.name.value = it }, "Name", placeholder = "e.g. Main cellar, Rack A, Kitchen fridge")
            Text("Type", style = Hv.type.label, color = Hv.colors.inkMuted)
            ChoiceChips(LocationKind.entries, setOf(kind), ::kindLabel, { vm.kind.value = it })
            val options: List<LocationNode?> = listOf<LocationNode?>(null) + nodes
            Dropdown("Inside", options, options.firstOrNull { it?.location?.id == parent }, { n -> n?.let { "  ".repeat(it.depth) + it.location.name } ?: "Nothing (top level)" },
                { vm.parent.value = it?.location?.id })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Bottle positions", style = Hv.type.body, color = Hv.colors.ink)
                    Text("Optional: track individual slots in a grid", style = Hv.type.caption, color = Hv.colors.inkMuted)
                }
                Switch(hasGrid, { vm.hasGrid.value = it })
            }
            if (hasGrid) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HvTextField(rows, { vm.rows.value = it.filter(Char::isDigit).take(3) }, "Rows", Modifier.weight(1f), keyboard = KeyboardType.Number)
                HvTextField(cols, { vm.cols.value = it.filter(Char::isDigit).take(3) }, "Positions per row", Modifier.weight(1f), keyboard = KeyboardType.Number)
            }
            HvTextField(notes, { vm.notes.value = it }, "Notes", singleLine = false, minLines = 2)
            error?.let { Text(it, color = Hv.colors.danger, style = Hv.type.bodyDense) }
            HvButton("Save", vm::save, Modifier.fillMaxWidth().padding(vertical = 16.dp))
        }
    }
}

