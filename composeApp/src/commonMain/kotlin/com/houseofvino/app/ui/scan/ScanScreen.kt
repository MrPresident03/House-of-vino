package com.houseofvino.app.ui.scan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houseofvino.app.AppGraph
import com.houseofvino.app.platform.CameraScanner
import com.houseofvino.app.platform.ScanMode
import com.houseofvino.app.platform.ScanOutput
import com.houseofvino.app.ui.cellar.ScanPrefill
import com.houseofvino.app.ui.common.GraphViewModel
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.common.confidenceText
import com.houseofvino.app.ui.design.ButtonKind
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvButton
import com.houseofvino.app.ui.design.HvTopBar
import com.houseofvino.app.ui.design.LoadingRows
import com.houseofvino.app.ui.design.PermissionDenied
import com.houseofvino.app.ui.design.SectionHeader
import com.houseofvino.app.ui.design.vintageLabel
import com.houseofvino.core.csv.ExistingVintage
import com.houseofvino.core.model.BottleFormats
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.scan.BarcodeMapping
import com.houseofvino.core.scan.CandidateMatcher
import com.houseofvino.core.scan.LabelTextParser
import com.houseofvino.core.scan.MatchCandidate
import com.houseofvino.core.scan.Suggested
import com.houseofvino.core.scan.WineSuggestion
import com.houseofvino.data.integrations.LookupResult
import com.houseofvino.data.catalogue.CatalogueMatch
import com.houseofvino.data.catalogue.LwinCatalogue
import com.houseofvino.core.scan.Confidence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ScanState {
    data object Camera : ScanState
    data object PermissionDenied : ScanState
    data object Unavailable : ScanState
    data object Working : ScanState
    data class Results(
        val suggestion: WineSuggestion,
        val candidates: List<MatchCandidate>,
        val source: String,
        val note: String?,
        val photo: ByteArray?,
        /** What the label or barcode service gave, before any catalogue wine was applied. */
        val read: WineSuggestion = suggestion,
        val catalogue: List<CatalogueMatch> = emptyList(),
        val selectedLwin: Long? = null,
        val catalogueNote: String? = null,
    ) : ScanState
}

class ScanViewModel(graph: AppGraph) : GraphViewModel(graph) {
    private val _state = MutableStateFlow<ScanState>(ScanState.Camera)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    fun onPermissionDenied() { _state.value = ScanState.PermissionDenied }
    fun onUnavailable() { _state.value = ScanState.Unavailable }
    fun retry() { _state.value = ScanState.Camera }

    private var existing: List<ExistingVintage> = emptyList()
    private var mappings: List<BarcodeMapping> = emptyList()

    /** Applies a catalogue wine (or none) to what was read, and re-checks the cellar for it. */
    fun selectCatalogue(lwin: Long?) {
        val r = _state.value as? ScanState.Results ?: return
        val match = r.catalogue.firstOrNull { it.wine.lwin == lwin }
        launchIo {
            val suggestion = merge(r.read, match)
            val candidates = CandidateMatcher.match(suggestion, existing, mappings, formatsByVintage(existing))
            _state.value = r.copy(suggestion = suggestion, candidates = candidates, selectedLwin = match?.wine?.lwin)
        }
    }

    private suspend fun withCatalogue(r: ScanState.Results, text: String): ScanState.Results {
        val catalogue = graph.catalogue
        if (!catalogue.available || text.isBlank()) return r
        val year = (r.read.vintage?.value as? VintageYear.Year)?.year
        val matches = io { catalogue.match(text, year) }
        val auto = matches.firstOrNull()?.takeIf { it.confidence == Confidence.MEDIUM }
        val suggestion = merge(r.read, auto)
        val candidates = if (auto == null) r.candidates else CandidateMatcher.match(suggestion, existing, mappings, formatsByVintage(existing))
        val note = when {
            matches.isEmpty() -> "No match in the wine catalogue."
            auto != null -> "Filled from the wine catalogue. Check it's the right wine."
            else -> "Possible catalogue matches. Pick one to fill in the details."
        }
        return r.copy(suggestion = suggestion, candidates = candidates, catalogue = matches, selectedLwin = auto?.wine?.lwin, catalogueNote = note)
    }

    fun onResult(output: ScanOutput) {
        if (_state.value != ScanState.Camera) return
        _state.value = ScanState.Working
        launchIo {
            existing = io {
                db.catalog.identityIndex(collectionId).executeAsList().map {
                    ExistingVintage(it.vintage_id, it.wine_id, it.producer, it.name, VintageYear.fromStorage(it.vintage_kind, it.year?.toInt()))
                }
            }
            when (output) {
                is ScanOutput.Barcode -> {
                    mappings = io {
                        db.catalog.barcodesForCode(collectionId, output.value).executeAsList()
                            .map { BarcodeMapping(it.code, it.wine_id, it.vintage_id, it.format_ml?.toInt()) }
                    }
                    // Our own confirmed mapping first; the external database only fills gaps.
                    var productText = ""
                    val (suggestion, note) = when (val r = graph.barcodes.lookup(output.value)) {
                        is LookupResult.Found -> {
                            val p = r.products.first()
                            productText = listOfNotNull(p.brand, p.productName).joinToString(" ")
                            p.toSuggestion().copy(barcode = output.value) to r.attribution
                        }
                        LookupResult.NotFound -> WineSuggestion(barcode = output.value) to "This barcode isn't in Open Food Facts."
                        is LookupResult.Unavailable -> WineSuggestion(barcode = output.value) to r.reason
                    }
                    val candidates = CandidateMatcher.match(suggestion, existing, mappings, formatsByVintage(existing))
                    // A barcode you've already confirmed beats any catalogue guess.
                    val base = ScanState.Results(suggestion, candidates, "Barcode ${output.value}", note, null)
                    _state.value = if (candidates.any { it.vintageConfirmed }) base else withCatalogue(base, productText)
                }
                is ScanOutput.LabelText -> {
                    val suggestion = LabelTextParser.parse(output.lines, graph.platform.currentYear())
                    val candidates = CandidateMatcher.match(suggestion, existing, emptyList(), formatsByVintage(existing))
                    val base = ScanState.Results(suggestion, candidates, "Label",
                        if (suggestion.isEmpty) "Couldn't read much from the label. Try again in better light, or enter the details." else null, output.photo)
                    _state.value = withCatalogue(base, output.lines.joinToString("\n") { it.text })
                }
            }
        }
    }

    private suspend fun formatsByVintage(existing: List<ExistingVintage>): Map<String, Set<Int>> = io {
        existing.associate { e -> e.vintageId to db.inventory.lotsForVintage(e.vintageId).executeAsList().map { it.format_ml.toInt() }.toSet() }
    }

    private fun merge(read: WineSuggestion, match: CatalogueMatch?): WineSuggestion {
        if (match == null) return read
        val c = match.wine.toSuggestion(match.confidence)
        return read.copy(
            producer = c.producer ?: read.producer, name = c.name ?: read.name,
            color = c.color ?: read.color, style = c.style ?: read.style,
            country = c.country ?: read.country, region = c.region ?: read.region, appellation = c.appellation ?: read.appellation,
        )
    }

    fun prefillKey(r: ScanState.Results): String = graph.handoff.put(ScanPrefill(r.suggestion, r.photo))
}

@Composable
fun ScanScreen(
    mode: ScanMode,
    onBack: () -> Unit,
    onUseDetails: (prefillKey: String) -> Unit,
    onOpenExisting: (vintageId: String) -> Unit,
    onNewVintageOf: (wineId: String, prefillKey: String) -> Unit,
    onManual: () -> Unit,
) {
    val graph = appGraph()
    val vm = viewModel(key = "scan-$mode") { ScanViewModel(graph) }
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        HvTopBar(if (mode == ScanMode.BARCODE) "Scan barcode" else "Read label", onBack = onBack)
        when (val s = state) {
            ScanState.Camera -> Box(Modifier.fillMaxSize()) {
                CameraScanner(mode, vm::onResult, vm::onPermissionDenied, vm::onUnavailable, Modifier.fillMaxSize())
                Text(if (mode == ScanMode.BARCODE) "Point at the barcode on the back label" else "Fill the frame with the front label, then tap capture",
                    style = Hv.type.label, color = Hv.colors.ink, modifier = Modifier.padding(16.dp))
            }
            ScanState.PermissionDenied -> PermissionDenied("Camera", "House of Vino uses the camera only to read barcodes and labels, on this device.",
                graph.platform::openAppSettings, onManual)
            ScanState.Unavailable -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Scanning isn't available on this device.", style = Hv.type.body, color = Hv.colors.ink)
                HvButton("Enter details", onManual)
            }
            ScanState.Working -> LoadingRows(3)
            is ScanState.Results -> Results(s, vm, onUseDetails, onOpenExisting, onNewVintageOf)
        }
    }
}

@Composable
private fun Results(
    s: ScanState.Results,
    vm: ScanViewModel,
    onUseDetails: (String) -> Unit,
    onOpenExisting: (String) -> Unit,
    onNewVintageOf: (String, String) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Text(s.source, style = Hv.type.label, color = Hv.colors.inkMuted, modifier = Modifier.padding(horizontal = 16.dp))
        s.note?.let { Text(it, style = Hv.type.caption, color = Hv.colors.inkMuted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }

        if (s.candidates.isNotEmpty()) {
            SectionHeader("In your cellar?")
            s.candidates.forEach { c ->
                Column(Modifier.fillMaxWidth().clickable {
                    if (c.vintageConfirmed) onOpenExisting(c.existing.vintageId) else onNewVintageOf(c.existing.wineId, vm.prefillKey(s))
                }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text("${vintageLabel(c.existing.vintage)} ${c.existing.producer}${c.existing.name?.let { " $it" } ?: ""}", style = Hv.type.body, color = Hv.colors.ink)
                    Text(c.reasons.joinToString(" · "), style = Hv.type.caption, color = Hv.colors.inkMuted)
                    c.warnings.forEach { Text(it, style = Hv.type.caption, color = Hv.colors.drinkSoon) }
                    Text(if (c.vintageConfirmed) "Open this wine" else "Choose or add the vintage for this wine", style = Hv.type.label, color = Hv.colors.accent)
                }
                HorizontalDivider(Modifier.padding(start = 16.dp), color = Hv.colors.outline)
            }
        }

        if (s.catalogue.isNotEmpty() || s.catalogueNote != null) {
            SectionHeader("Wine catalogue")
            s.catalogueNote?.let { Text(it, style = Hv.type.caption, color = Hv.colors.inkMuted, modifier = Modifier.padding(horizontal = 16.dp)) }
            s.catalogue.forEach { m ->
                val selected = m.wine.lwin == s.selectedLwin
                Column(Modifier.fillMaxWidth().clickable { vm.selectCatalogue(if (selected) null else m.wine.lwin) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text((if (selected) "✓ " else "") + m.wine.displayName, style = Hv.type.body, color = if (selected) Hv.colors.accent else Hv.colors.ink)
                    Text(listOfNotNull(m.wine.colour, m.wine.subType, m.wine.region, m.wine.country).joinToString(" · "), style = Hv.type.caption, color = Hv.colors.inkMuted)
                    Text(m.reasons.joinToString(" · "), style = Hv.type.caption, color = Hv.colors.inkMuted)
                    m.warnings.forEach { Text(it, style = Hv.type.caption, color = Hv.colors.drinkSoon) }
                    Text(if (selected) "Using this wine. Tap to undo." else "Use this wine", style = Hv.type.label, color = Hv.colors.accent)
                }
                HorizontalDivider(Modifier.padding(start = 16.dp), color = Hv.colors.outline)
            }
            if (s.catalogue.isNotEmpty()) Text(LwinCatalogue.ATTRIBUTION, style = Hv.type.caption, color = Hv.colors.inkMuted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        SectionHeader("Details to use")
        val sg = s.suggestion
        Field("Producer", sg.producer)
        Field("Wine", sg.name)
        Field("Vintage", sg.vintage?.let { Suggested(vintageLabel(it.value), it.confidence, it.source) })
        Field("Size", sg.formatMl?.let { Suggested(BottleFormats.nameFor(it.value), it.confidence, it.source) })
        Field("Alcohol", sg.abvBasisPoints?.let { Suggested("${it.value / 100}.${(it.value % 100).toString().padStart(2, '0')}%", it.confidence, it.source) })
        Field("Appellation", sg.appellation)
        Field("Region", sg.region)
        Field("Country", sg.country)
        Field("Colour", sg.color?.let { Suggested(it.value.name.lowercase().replaceFirstChar(Char::uppercase), it.confidence, it.source) })
        Field("Style", sg.style?.let { Suggested(it.value.name.lowercase().replaceFirstChar(Char::uppercase), it.confidence, it.source) })
        if (sg.isEmpty) Text("Nothing readable yet.", style = Hv.type.bodyDense, color = Hv.colors.inkMuted, modifier = Modifier.padding(horizontal = 16.dp))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HvButton("Add as a new wine", { onUseDetails(vm.prefillKey(s)) }, Modifier.fillMaxWidth())
            HvButton("Scan again", vm::retry, Modifier.fillMaxWidth(), kind = ButtonKind.QUIET)
            Text("You'll check every field before saving. Nothing is added automatically.", style = Hv.type.caption, color = Hv.colors.inkMuted)
        }
    }
}

@Composable
private fun Field(label: String, value: Suggested<String>?) {
    if (value == null) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(label, style = Hv.type.caption, color = Hv.colors.inkMuted)
        Text(value.value, style = Hv.type.body, color = Hv.colors.ink)
        Text(confidenceText(value.confidence), style = Hv.type.caption, color = Hv.colors.inkMuted)
    }
}
