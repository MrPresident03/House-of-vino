package com.houseofvino.app.ui.locations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houseofvino.app.AppGraph
import com.houseofvino.app.ui.common.GraphViewModel
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.design.ButtonKind
import com.houseofvino.app.ui.design.ChoiceChips
import com.houseofvino.app.ui.design.EmptyState
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvButton
import com.houseofvino.app.ui.design.HvTopBar
import com.houseofvino.app.ui.design.LedgerRow
import com.houseofvino.app.ui.design.LoadingRows
import com.houseofvino.app.ui.design.SectionHeader
import com.houseofvino.core.model.Location
import com.houseofvino.data.repo.SlotView
import com.houseofvino.data.search.CellarRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RacksState(
    /** Null until the first load. Locations that have a grid of positions, in tree order. */
    val racks: List<Location>? = null,
    val selectedId: String? = null,
    val slots: List<SlotView> = emptyList(),
    val wines: List<CellarRow> = emptyList(),
    val placeable: List<Pair<String, String>> = emptyList(),
) {
    val selected: Location? get() = racks?.firstOrNull { it.id == selectedId }
    val filled: Int get() = slots.count { it.lotId != null }
}

/** The Racks tab: every location with a grid of positions, one tap from the bottom bar. */
class RacksViewModel(graph: AppGraph) : GraphViewModel(graph) {
    private val _state = MutableStateFlow(RacksState(selectedId = graph.settings.get(KEY_LAST_RACK)))
    val state: StateFlow<RacksState> = _state.asStateFlow()

    init {
        observe("location", "movement", "lot", "slot", "wine", "vintage", "photo") { reload() }
    }

    private suspend fun reload() {
        val racks = io { graph.locations.tree(collectionId).map { it.location }.filter { it.hasGrid } }
        // Keep the rack that was open; fall back to the first one if it has gone or lost its grid.
        val selectedId = _state.value.selectedId?.takeIf { id -> racks.any { it.id == id } } ?: racks.firstOrNull()?.id
        if (selectedId == null) {
            _state.value = RacksState(racks = racks)
            return
        }
        val slots = io { graph.locations.grid(selectedId) }
        val wines = io { winesIn(graph, collectionId, selectedId) }
        val placeable = io { placeableLots(graph, selectedId, wines) }
        // A different rack may have been chosen while this one was loading; that load wins.
        _state.update { current ->
            if (current.selectedId != null && current.selectedId != selectedId && racks.any { it.id == current.selectedId }) current.copy(racks = racks)
            else RacksState(racks, selectedId, slots, wines, placeable)
        }
    }

    fun select(id: String) {
        if (id == _state.value.selectedId) return
        _state.update { it.copy(selectedId = id, slots = emptyList(), wines = emptyList(), placeable = emptyList()) }
        launchIo {
            io { graph.settings.put(KEY_LAST_RACK, id) }
            reload()
        }
    }

    fun place(slot: SlotView, lotId: String) {
        val id = _state.value.selectedId ?: return
        launchIo { io { graph.locations.placeBottle(id, slot.row, slot.col, lotId) } }
    }

    fun clear(slot: SlotView) {
        val id = _state.value.selectedId ?: return
        launchIo { io { graph.locations.clearPosition(id, slot.row, slot.col) } }
    }

    companion object {
        /** Remembered on this phone only. */
        const val KEY_LAST_RACK = "racks_last"
    }
}

@Composable
fun RacksScreen(
    onOpenLocation: (String) -> Unit,
    onEditLocation: (String) -> Unit,
    onAddRack: () -> Unit,
    onManageLocations: () -> Unit,
    onOpenVintage: (String) -> Unit,
) {
    val graph = appGraph()
    val vm = viewModel { RacksViewModel(graph) }
    val state by vm.state.collectAsState()
    var picking by remember { mutableStateOf<SlotView?>(null) }
    val year = graph.platform.currentYear()
    val racks = state.racks
    val rack = state.selected

    Column(Modifier.fillMaxSize()) {
        HvTopBar("Racks", actions = {
            if (rack != null) IconButton(onClick = { onEditLocation(rack.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Change rows and positions") }
            IconButton(onClick = onAddRack) { Icon(Icons.Filled.Add, contentDescription = "Add a rack") }
        })
        when {
            racks == null -> LoadingRows()
            racks.isEmpty() || rack == null -> EmptyState(
                "No rack layout yet",
                "Give a rack, fridge or shelf rows and positions, and every bottle's place shows here.",
                modifier = Modifier.testTag("racks-empty"),
                action = "Set up a rack" to onAddRack,
                secondary = "All locations" to onManageLocations,
            )
            else -> LazyColumn(Modifier.weight(1f).testTag("racks"), contentPadding = PaddingValues(bottom = 32.dp)) {
                if (racks.size > 1) item {
                    ChoiceChips(racks, setOf(rack), { it.name }, { vm.select(it.id) }, Modifier.padding(horizontal = 16.dp))
                }
                item {
                    SectionHeader(if (racks.size > 1) rack.name else "${rack.name} · positions")
                    Text("${state.filled} of ${state.slots.size} positions filled", style = Hv.type.bodyDense, color = Hv.colors.inkMuted,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
                    SlotGrid(rack, state.slots) { picking = it }
                    Text("Tap a position to place or remove a bottle stored here.", style = Hv.type.caption, color = Hv.colors.inkMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HvButton("Open location", { onOpenLocation(rack.id) }, kind = ButtonKind.SECONDARY)
                        HvButton("All locations", onManageLocations, kind = ButtonKind.QUIET)
                    }
                }
                item { SectionHeader("Wines here") }
                if (state.wines.isEmpty()) item {
                    Text("No bottles here yet. Use Move on a wine to put bottles in this rack.", style = Hv.type.bodyDense, color = Hv.colors.inkMuted,
                        modifier = Modifier.padding(horizontal = 16.dp))
                }
                items(state.wines, key = { it.vintageId }) { r -> LedgerRow(r, year, onClick = { onOpenVintage(r.vintageId) }) }
            }
        }
    }
    picking?.let { slot ->
        SlotDialog(slot, state.placeable, onPlace = { lotId -> vm.place(slot, lotId) }, onClear = { vm.clear(slot) },
            onOpenVintage = onOpenVintage, onDismiss = { picking = null })
    }
}
