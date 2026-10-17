package com.houseofvino.app.ui.journal

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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houseofvino.app.AppGraph
import com.houseofvino.app.ui.common.DateField
import com.houseofvino.app.ui.common.GraphViewModel
import com.houseofvino.app.ui.common.Load
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.common.userMessage
import com.houseofvino.app.ui.design.ButtonKind
import com.houseofvino.app.ui.design.EmptyState
import com.houseofvino.app.ui.design.ErrorState
import com.houseofvino.app.ui.design.Fact
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvButton
import com.houseofvino.app.ui.design.HvTextField
import com.houseofvino.app.ui.design.HvTopBar
import com.houseofvino.app.ui.design.LoadingRows
import com.houseofvino.app.ui.design.SectionHeader
import com.houseofvino.app.ui.design.vintageLabel
import com.houseofvino.app.ui.photos.PhotoImage
import com.houseofvino.core.model.Level
import com.houseofvino.core.model.RatingScale
import com.houseofvino.core.model.Ratings
import com.houseofvino.core.model.StructuredNotes
import com.houseofvino.core.model.Tasting
import com.houseofvino.core.search.SearchQuery
import com.houseofvino.core.search.SearchScope
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.db.Photo
import com.houseofvino.data.repo.OpenedBottle
import com.houseofvino.data.repo.PhotoOwner
import com.houseofvino.data.repo.TastingInput
import com.houseofvino.data.search.JournalRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// =================================================================================================
// Journal timeline

class JournalViewModel(graph: AppGraph) : GraphViewModel(graph) {
    private val _state = MutableStateFlow<Load<List<JournalRow>>>(Load.Loading)
    val state: StateFlow<Load<List<JournalRow>>> = _state.asStateFlow()
    val scale: RatingScale = graph.settings.preferences().ratingScale

    init {
        observe("tasting", "wine", "vintage") {
            _state.value = try {
                Load.Ready(io { graph.search.searchJournal(collectionId, SearchQuery(scope = SearchScope.JOURNAL), 0, 500).items })
            } catch (e: Exception) { Load.Failed(e.userMessage()) }
        }
    }
}

@Composable
fun JournalScreen(onOpen: (String) -> Unit, onAdd: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel { JournalViewModel(graph) }
    val state by vm.state.collectAsState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HvTopBar("Journal")
            when (val s = state) {
                Load.Loading -> LoadingRows()
                is Load.Failed -> ErrorState(s.message, null)
                is Load.Ready -> if (s.value.isEmpty()) {
                    EmptyState("No tasting notes yet", "Notes you write when you open a bottle, or about wines you taste elsewhere, appear here.",
                        action = "Write a note" to onAdd)
                } else {
                    val byMonth = s.value.groupBy { it.tastedOn.take(7) }
                    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), modifier = Modifier.testTag("journal-list")) {
                        byMonth.forEach { (month, rows) ->
                            item(key = "m-$month") { SectionHeader(monthLabel(month)) }
                            items(rows, key = { it.tastingId }) { r -> JournalRowItem(r, vm.scale) { onOpen(r.tastingId) } }
                        }
                    }
                }
            }
        }
        if ((state as? Load.Ready)?.value?.isNotEmpty() == true) {
            ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Write a note") },
                containerColor = Hv.colors.accent, contentColor = Hv.colors.onAccent, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
        }
    }
}

private val MONTHS = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
fun monthLabel(yyyyMm: String): String {
    val y = yyyyMm.take(4); val m = yyyyMm.drop(5).toIntOrNull() ?: return yyyyMm
    return "${MONTHS[(m - 1).coerceIn(0, 11)]} $y"
}

@Composable
private fun JournalRowItem(r: JournalRow, scale: RatingScale, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).semantics(mergeDescendants = true) {}.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(vintageLabel(r.vintage), style = Hv.type.vintageRow, color = Hv.colors.ink, modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.producer, style = Hv.type.label, color = Hv.colors.inkMuted)
                r.wineName?.let { Text(it, style = Hv.type.wineName, color = Hv.colors.ink, maxLines = 1) }
            }
            r.rating?.let { Text(Ratings.display(it, scale), style = Hv.type.figure, color = Hv.colors.ink) }
        }
        r.notesPreview?.let { Text(it, style = Hv.type.bodyDense, color = Hv.colors.ink, maxLines = 2) }
        Text(listOfNotNull(r.tastedOn, r.occasion).joinToString(" · "), style = Hv.type.caption, color = Hv.colors.inkMuted)
    }
    HorizontalDivider(Modifier.padding(start = 16.dp), color = Hv.colors.outline)
}

// =================================================================================================
// Tasting editor

data class TastingForm(
    val tastedOn: String,
    val rating: Int? = null,
    val structured: StructuredNotes = StructuredNotes(),
    val aromas: String = "",
    val notes: String = "",
    val occasion: String = "",
    val foodPairing: String = "",
    val companions: String = "",
    val place: String = "",
    val wouldBuyAgain: Boolean? = null,
    val showStructure: Boolean = false,
    val wineTitle: String = "",
    val photos: List<Photo> = emptyList(),
    val pendingPhotos: List<ByteArray> = emptyList(),
)

class TastingEditorViewModel(
    graph: AppGraph,
    private val vintageId: String?,
    private val tastingId: String?,
    private val opened: OpenedBottle?,
) : GraphViewModel(graph) {
    private val _form = MutableStateFlow(TastingForm(tastedOn = graph.platform.today().toString()))
    val form: StateFlow<TastingForm> = _form.asStateFlow()
    private val _saved = MutableStateFlow<String?>(null)
    val saved: StateFlow<String?> = _saved.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    val scale = graph.settings.preferences().ratingScale

    init {
        launchIo {
            val existing = tastingId?.let { io { graph.journal.tasting(it) } }
            val vid = existing?.vintageId ?: vintageId
            val title = vid?.let { io { graph.cellar.vintageDetail(it) } }?.let { "${vintageLabel(it.vintage.vintage)} ${it.wine.displayName}" }.orEmpty()
            val existingPhotos = existing?.let { io { graph.photos.forOwner(PhotoOwner.TASTING, it.id) } }.orEmpty()
            _form.update { f ->
                if (existing == null) f.copy(wineTitle = title) else f.copy(
                    tastedOn = existing.tastedOn.toString(), rating = existing.rating, structured = existing.structured,
                    aromas = existing.structured.aromas.joinToString(", "), notes = existing.notes.orEmpty(), occasion = existing.occasion.orEmpty(),
                    foodPairing = existing.foodPairing.orEmpty(), companions = existing.companions.orEmpty(), place = existing.place.orEmpty(),
                    wouldBuyAgain = existing.wouldBuyAgain, showStructure = !existing.structured.isEmpty, wineTitle = title,
                    photos = existingPhotos,
                )
            }
        }
    }

    fun edit(t: (TastingForm) -> TastingForm) { _form.update(t); _error.value = null }

    fun addPhoto(bytes: ByteArray) = _form.update { it.copy(pendingPhotos = it.pendingPhotos + bytes) }

    fun save() {
        val f = _form.value
        val date = IsoDate.parseOrNull(f.tastedOn) ?: run { _error.value = "Use YYYY-MM-DD"; return }
        val input = TastingInput(
            tastedOn = date, rating = f.rating,
            structured = f.structured.copy(aromas = f.aromas.split(',').map { it.trim() }.filter { it.isNotEmpty() }),
            notes = f.notes, occasion = f.occasion, foodPairing = f.foodPairing, companions = f.companions, place = f.place,
            wouldBuyAgain = f.wouldBuyAgain,
        )
        launchIo(onError = { _error.value = it.userMessage() }) {
            val id = io {
                if (tastingId != null) { graph.journal.update(tastingId, input); tastingId }
                else graph.journal.add(collectionId, requireNotNull(vintageId) { "Choose a wine first" }, input, opened)
            }
            for (bytes in f.pendingPhotos) io {
                val photoId = graph.photos.newPhotoId()
                val path = graph.platform.savePhoto(photoId, bytes)
                graph.photos.addWithId(photoId, collectionId, PhotoOwner.TASTING, id, path, 0, 0, bytes.size.toLong())
            }
            messenger.show(if (opened != null) "Bottle opened and note saved" else "Note saved")
            _saved.value = id
        }
    }
}

@Composable
fun TastingEditorScreen(vintageId: String?, tastingId: String?, lotId: String?, locationId: String?, consume: Boolean, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val graph = appGraph()
    val opened = if (consume && lotId != null) OpenedBottle(lotId, locationId) else null
    val vm = viewModel(key = "tasting-$vintageId-$tastingId-$lotId") { TastingEditorViewModel(graph, vintageId, tastingId, opened) }
    val f by vm.form.collectAsState()
    val saved by vm.saved.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(saved) { saved?.let(onSaved) }

    Column(Modifier.fillMaxSize()) {
        HvTopBar(if (tastingId != null) "Edit note" else if (opened != null) "Open and taste" else "Tasting note", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(f.wineTitle, style = Hv.type.wineName, color = Hv.colors.ink)
            if (opened != null) Text("Saving records one bottle as drunk and links it to this note.", style = Hv.type.caption, color = Hv.colors.inkMuted)
            DateField("Date", f.tastedOn, { v -> vm.edit { it.copy(tastedOn = v) } }, error?.takeIf { it.contains("YYYY") })
            RatingInput(f.rating, vm.scale) { r -> vm.edit { it.copy(rating = r) } }
            HvTextField(f.notes, { v -> vm.edit { it.copy(notes = v) } }, "Notes", singleLine = false, minLines = 4, modifier = Modifier.testTag("tasting-notes"))
            HvTextField(f.aromas, { v -> vm.edit { it.copy(aromas = v) } }, "Aromas and flavours", placeholder = "cassis, cedar, graphite")
            TextButton(onClick = { vm.edit { it.copy(showStructure = !it.showStructure) } }) {
                Text(if (f.showStructure) "Hide structured notes" else "Add structured notes (acidity, tannin, body…)")
            }
            if (f.showStructure) StructuredInput(f.structured) { s -> vm.edit { it.copy(structured = s) } }
            HvTextField(f.occasion, { v -> vm.edit { it.copy(occasion = v) } }, "Occasion")
            HvTextField(f.foodPairing, { v -> vm.edit { it.copy(foodPairing = v) } }, "Food")
            HvTextField(f.companions, { v -> vm.edit { it.copy(companions = v) } }, "With")
            HvTextField(f.place, { v -> vm.edit { it.copy(place = v) } }, "Where")
            Text("Would you buy it again?", style = Hv.type.label, color = Hv.colors.inkMuted)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(true to "Yes", false to "No", null to "Not sure").forEachIndexed { i, (v, label) ->
                    SegmentedButton(selected = f.wouldBuyAgain == v, onClick = { vm.edit { it.copy(wouldBuyAgain = v) } }, shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(label) }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                f.photos.forEach { p -> p.local_path?.let { PhotoImage(it, "Tasting photo", Modifier.size(72.dp).clip(RoundedCornerShape(6.dp))) } }
                if (f.pendingPhotos.isNotEmpty()) Text("${f.pendingPhotos.size} new photo(s)", style = Hv.type.caption, color = Hv.colors.inkMuted)
            }
            HvButton("Add photo", { graph.platform.pickPhoto { bytes -> bytes?.let(vm::addPhoto) } }, kind = ButtonKind.SECONDARY)
            error?.takeIf { !it.contains("YYYY") }?.let { Text(it, color = Hv.colors.danger, style = Hv.type.bodyDense) }
            HvButton("Save note", vm::save, Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag("save-tasting"))
        }
    }
}

/** Rating in the user's scale; stored on 0–100. */
@Composable
fun RatingInput(rating: Int?, scale: RatingScale, onChange: (Int?) -> Unit) {
    Column(Modifier.semantics { stateDescription = rating?.let { Ratings.display(it, scale) } ?: "Not rated" }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rating", style = Hv.type.label, color = Hv.colors.inkMuted, modifier = Modifier.weight(1f))
            Text(rating?.let { Ratings.display(it, scale) } ?: "Not rated", style = Hv.type.vintageRow, color = Hv.colors.ink)
            if (rating != null) TextButton(onClick = { onChange(null) }) { Text("Clear") }
        }
        val (steps, toRating, fromRating) = when (scale) {
            RatingScale.FIVE_STARS -> Triple(8, { v: Float -> (v * 20).toInt() }, { r: Int -> r / 20f })           // half stars
            RatingScale.TWENTY -> Triple(38, { v: Float -> (v * 5).toInt() }, { r: Int -> r / 5f })                  // half points
            RatingScale.HUNDRED -> Triple(49, { v: Float -> v.toInt() }, { r: Int -> r.toFloat() })
        }
        val range = when (scale) { RatingScale.FIVE_STARS -> 0.5f..5f; RatingScale.TWENTY -> 0.5f..20f; RatingScale.HUNDRED -> 50f..100f }
        Slider(
            value = rating?.let(fromRating)?.coerceIn(range) ?: range.endInclusive * 0.8f,
            onValueChange = { v -> onChange(toRating(snap(v, scale)).coerceIn(0, 100)) },
            valueRange = range, steps = steps,
            modifier = Modifier.semantics { contentDescription = "Rating" },
        )
    }
}

private fun snap(v: Float, scale: RatingScale): Float = when (scale) {
    RatingScale.HUNDRED -> kotlin.math.round(v)
    else -> kotlin.math.round(v * 2) / 2f
}

@Composable
private fun StructuredInput(s: StructuredNotes, onChange: (StructuredNotes) -> Unit) {
    @Composable
    fun row(label: String, value: Level?, set: (Level?) -> StructuredNotes) {
        Column {
            Text(label, style = Hv.type.label, color = Hv.colors.inkMuted)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Level.entries.forEachIndexed { i, l ->
                    SegmentedButton(selected = value == l, onClick = { onChange(set(if (value == l) null else l)) },
                        shape = SegmentedButtonDefaults.itemShape(i, Level.entries.size)) {
                        Text(when (l) { Level.LOW -> "Low"; Level.MEDIUM_MINUS -> "M−"; Level.MEDIUM -> "Med"; Level.MEDIUM_PLUS -> "M+"; Level.HIGH -> "High" }, style = Hv.type.caption)
                    }
                }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        row("Sweetness", s.sweetness) { s.copy(sweetness = it) }
        row("Acidity", s.acidity) { s.copy(acidity = it) }
        row("Tannin", s.tannin) { s.copy(tannin = it) }
        row("Alcohol", s.alcohol) { s.copy(alcohol = it) }
        row("Body", s.body) { s.copy(body = it) }
        row("Flavour intensity", s.flavourIntensity) { s.copy(flavourIntensity = it) }
        row("Finish", s.finish) { s.copy(finish = it) }
    }
}

// =================================================================================================
// Tasting detail

class TastingDetailViewModel(graph: AppGraph, private val tastingId: String) : GraphViewModel(graph) {
    private val _state = MutableStateFlow<Load<Pair<Tasting, String>>>(Load.Loading)
    val state: StateFlow<Load<Pair<Tasting, String>>> = _state.asStateFlow()
    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()
    val scale = graph.settings.preferences().ratingScale

    init {
        observe("tasting", "photo") {
            val t = io { graph.journal.tasting(tastingId) }
            _state.value = if (t == null) Load.Failed("This note was deleted.") else {
                val title = io { graph.cellar.vintageDetail(t.vintageId) }?.let { "${vintageLabel(it.vintage.vintage)} ${it.wine.displayName}" }.orEmpty()
                Load.Ready(t to title)
            }
            _photos.value = io { graph.photos.forOwner(PhotoOwner.TASTING, tastingId) }
        }
    }

    fun delete(onDone: () -> Unit) = launchIo { io { graph.journal.delete(tastingId) }; messenger.show("Note deleted"); onDone() }
}

@Composable
fun TastingDetailScreen(tastingId: String, onBack: () -> Unit, onEdit: () -> Unit, onOpenWine: (String) -> Unit) {
    val graph = appGraph()
    val vm = viewModel(key = "td-$tastingId") { TastingDetailViewModel(graph, tastingId) }
    val state by vm.state.collectAsState()
    val photos by vm.photos.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Tasting note", onBack = onBack, actions = { IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit note") } })
        when (val s = state) {
            Load.Loading -> LoadingRows(3)
            is Load.Failed -> ErrorState(s.message, null)
            is Load.Ready -> {
                val (t, title) = s.value
                Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
                    Text(title, style = Hv.type.title, color = Hv.colors.ink, modifier = Modifier.padding(horizontal = 16.dp).clickable { onOpenWine(t.vintageId) })
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(t.tastedOn.toString(), style = Hv.type.label, color = Hv.colors.inkMuted, modifier = Modifier.weight(1f))
                        t.rating?.let { Text(Ratings.display(it, vm.scale), style = Hv.type.vintageRow, color = Hv.colors.ink) }
                    }
                    t.notes?.let { Text(it, style = Hv.type.body, color = Hv.colors.ink, modifier = Modifier.padding(horizontal = 16.dp)) }
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        photos.forEach { p -> p.local_path?.let { PhotoImage(it, "Tasting photo", Modifier.size(120.dp).clip(RoundedCornerShape(6.dp))) } }
                    }
                    Fact("Aromas", t.structured.aromas.joinToString(", ").ifEmpty { null })
                    val st = t.structured
                    Fact("Structure", listOfNotNull(
                        st.sweetness?.let { "sweetness ${it.name.lowercase().replace('_', ' ')}" }, st.acidity?.let { "acidity ${it.name.lowercase().replace('_', ' ')}" },
                        st.tannin?.let { "tannin ${it.name.lowercase().replace('_', ' ')}" }, st.body?.let { "body ${it.name.lowercase().replace('_', ' ')}" },
                        st.finish?.let { "finish ${it.name.lowercase().replace('_', ' ')}" },
                    ).joinToString(", ").ifEmpty { null })
                    Fact("Occasion", t.occasion)
                    Fact("Food", t.foodPairing)
                    Fact("With", t.companions)
                    Fact("Where", t.place)
                    Fact("Buy again", t.wouldBuyAgain?.let { if (it) "Yes" else "No" })
                    Fact("Bottle", if (t.movementId != null) "Opened from the cellar" else null)
                    HvButton("Delete note", { confirm = true }, Modifier.padding(16.dp), kind = ButtonKind.QUIET)
                }
            }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Delete this note?") },
        text = { Text("The note is removed. If a bottle was opened with it, that stays recorded as drunk.") },
        confirmButton = { TextButton(onClick = { confirm = false; vm.delete(onBack) }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
    )
}
