package com.houseofvino.app.ui.importexport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houseofvino.app.AppGraph
import com.houseofvino.app.ui.common.CurrencyDropdown
import com.houseofvino.app.ui.common.Dropdown
import com.houseofvino.app.ui.common.GraphViewModel
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.common.userMessage
import com.houseofvino.app.ui.design.ButtonKind
import com.houseofvino.app.ui.design.ChoiceChips
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvButton
import com.houseofvino.app.ui.design.HvTopBar
import com.houseofvino.app.ui.design.SectionHeader
import com.houseofvino.app.ui.design.vintageLabel
import com.houseofvino.core.csv.ColumnMapping
import com.houseofvino.core.csv.DuplicateAssessment
import com.houseofvino.core.csv.ImportDecision
import com.houseofvino.core.csv.ImportField
import com.houseofvino.core.csv.ImportOptions
import com.houseofvino.core.csv.Severity
import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.time.DateOrder
import com.houseofvino.data.io.ImportPreview
import com.houseofvino.data.io.ImportReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ImportStep { PICK, MAP, REVIEW, IMPORTING, DONE }

data class ImportState(
    val step: ImportStep = ImportStep.PICK,
    val fileName: String? = null,
    val text: String? = null,
    val header: List<String> = emptyList(),
    val mapping: ColumnMapping? = null,
    val currency: CurrencyCode = CurrencyCode.EUR,
    val dateOrder: DateOrder = DateOrder.DMY,
    val decimalComma: Boolean = false,
    val preview: ImportPreview? = null,
    val decisions: Map<Int, ImportDecision> = emptyMap(),
    val progress: Float = 0f,
    val report: ImportReport? = null,
    val error: String? = null,
)

class ImportViewModel(graph: AppGraph) : GraphViewModel(graph) {
    private val _state = MutableStateFlow(ImportState(currency = graph.settings.preferences().currency, dateOrder = graph.settings.preferences().dateOrder))
    val state: StateFlow<ImportState> = _state.asStateFlow()

    fun pick() = graph.platform.pickTextFile { file ->
        if (file == null) return@pickTextFile
        launchIo(onError = { e -> _state.update { it.copy(error = "Couldn't read the file. ${e.userMessage()}") } }) {
            val table = io { com.houseofvino.core.csv.Csv.read(file.text) }
            if (table.header.isEmpty()) { _state.update { it.copy(error = "That file has no rows.") }; return@launchIo }
            _state.update { it.copy(step = ImportStep.MAP, fileName = file.name, text = file.text, header = table.header, mapping = ColumnMapping.suggest(table.header), error = null) }
        }
    }

    fun setColumn(field: ImportField, column: Int?) = _state.update { s ->
        val cols = s.mapping?.columns.orEmpty().toMutableMap()
        if (column == null) cols.remove(field) else { cols.entries.removeAll { it.value == column }; cols[field] = column }
        s.copy(mapping = ColumnMapping(cols))
    }

    fun setOptions(t: (ImportState) -> ImportState) = _state.update(t)

    fun review() {
        val s = _state.value
        launchIo(onError = { e -> _state.update { it.copy(error = e.userMessage()) } }) {
            val preview = io {
                graph.importer.preview(collectionId, s.text!!, s.mapping, ImportOptions(s.currency, s.dateOrder, if (s.decimalComma) ',' else '.'))
            }
            _state.update { it.copy(step = ImportStep.REVIEW, preview = preview, decisions = emptyMap(), error = null) }
        }
    }

    fun decide(row: Int, d: ImportDecision) = _state.update { it.copy(decisions = it.decisions + (row to d)) }

    fun commit() {
        val s = _state.value
        val preview = s.preview ?: return
        _state.update { it.copy(step = ImportStep.IMPORTING, progress = 0f) }
        launchIo(onError = { e -> _state.update { it.copy(step = ImportStep.REVIEW, error = e.userMessage()) } }) {
            val report = io {
                graph.importer.commit(collectionId, preview, s.decisions) { done, total ->
                    _state.update { it.copy(progress = if (total == 0) 1f else done.toFloat() / total) }
                }
            }
            _state.update { it.copy(step = ImportStep.DONE, report = report) }
        }
    }

    fun restart() = _state.update { ImportState(currency = it.currency, dateOrder = it.dateOrder) }
}

@Composable
fun ImportScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel { ImportViewModel(graph) }
    val s by vm.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Import", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            s.error?.let { Text(it, color = Hv.colors.danger, style = Hv.type.bodyDense) }
            when (s.step) {
                ImportStep.PICK -> {
                    Text("Import a CSV exported from CellarTracker, Vivino, a spreadsheet or another app. You'll match the columns and review duplicates before anything is saved.",
                        style = Hv.type.body, color = Hv.colors.ink)
                    HvButton("Choose a CSV file", vm::pick, Modifier.fillMaxWidth())
                }
                ImportStep.MAP -> MapStep(s, vm)
                ImportStep.REVIEW -> ReviewStep(s, vm)
                ImportStep.IMPORTING -> {
                    Text("Importing…", style = Hv.type.sectionTitle, color = Hv.colors.ink)
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                }
                ImportStep.DONE -> s.report?.let { r ->
                    Text("Import finished", style = Hv.type.title, color = Hv.colors.ink)
                    Text("${r.bottlesAdded} bottles added in ${r.lotsCreated} purchases. ${r.winesCreated} new wines, ${r.vintagesCreated} new vintages. ${r.skipped} rows skipped.",
                        style = Hv.type.body, color = Hv.colors.ink)
                    if (r.failed.isNotEmpty()) {
                        Text("${r.failed.size} row(s) couldn't be imported:", style = Hv.type.body, color = Hv.colors.danger)
                        r.failed.take(50).forEach { Text("Row ${it.row}: ${it.message}", style = Hv.type.caption, color = Hv.colors.inkMuted) }
                    }
                    HvButton("Done", onDone, Modifier.fillMaxWidth())
                    HvButton("Import another file", vm::restart, Modifier.fillMaxWidth(), kind = ButtonKind.QUIET)
                }
            }
        }
    }
}

@Composable
private fun MapStep(s: ImportState, vm: ImportViewModel) {
    val header = s.header
    Text(s.fileName ?: "", style = Hv.type.label, color = Hv.colors.inkMuted)
    Text("Match columns", style = Hv.type.sectionTitle, color = Hv.colors.ink)
    Text("Columns were matched by name. Check them; only Producer is required.", style = Hv.type.bodyDense, color = Hv.colors.inkMuted)
    val options: List<Int?> = listOf<Int?>(null) + header.indices
    ImportField.entries.forEach { field ->
        Dropdown(field.label + if (field.required) " *" else "", options, s.mapping?.get(field), { i -> i?.let { header[it] } ?: "Not in file" }, { vm.setColumn(field, it) })
    }
    SectionHeader("How values are written", Modifier.padding(start = 0.dp))
    CurrencyDropdown(s.currency, { c -> vm.setOptions { it.copy(currency = c) } }, label = "Currency when the file doesn't say")
    Text("Dates like 03/04/2021 mean", style = Hv.type.label, color = Hv.colors.inkMuted)
    ChoiceChips(listOf(DateOrder.DMY, DateOrder.MDY, DateOrder.YMD), setOf(s.dateOrder),
        { when (it) { DateOrder.DMY -> "3 April (day first)"; DateOrder.MDY -> "March 4 (month first)"; DateOrder.YMD -> "Year first only" } },
        { o -> vm.setOptions { it.copy(dateOrder = o) } })
    ChoiceChips(listOf(false, true), setOf(s.decimalComma), { if (it) "Decimal comma (12,50)" else "Decimal point (12.50)" }, { c -> vm.setOptions { it.copy(decimalComma = c) } })
    HvButton("Check rows", vm::review, Modifier.fillMaxWidth().padding(vertical = 8.dp), enabled = s.mapping?.get(ImportField.PRODUCER) != null)
}

@Composable
private fun ReviewStep(s: ImportState, vm: ImportViewModel) {
    val p = s.preview ?: return
    Text("${p.validRows} rows ready · ${p.bottles} bottles", style = Hv.type.sectionTitle, color = Hv.colors.ink)
    if (p.errorRows > 0) Text("${p.errorRows} row(s) have errors and will be skipped:", style = Hv.type.body, color = Hv.colors.danger)
    p.parse.issues.filter { it.severity == Severity.ERROR }.take(30).forEach { Text("Row ${it.row}: ${it.message}", style = Hv.type.caption, color = Hv.colors.inkMuted) }
    val warnings = p.parse.issues.filter { it.severity == Severity.WARNING }
    if (warnings.isNotEmpty()) {
        Text("${warnings.size} warning(s)", style = Hv.type.label, color = Hv.colors.inkMuted)
        warnings.take(15).forEach { Text("Row ${it.row}: ${it.message}", style = Hv.type.caption, color = Hv.colors.inkMuted) }
    }
    val review = p.assessments.filter { it.needsReview }
    if (review.isNotEmpty()) {
        SectionHeader("Check ${review.size} possible duplicate(s)", Modifier.padding(start = 0.dp))
        review.forEach { a -> DuplicateItem(a, s.decisions[a.record.row] ?: a.defaultDecision) { vm.decide(a.record.row, it) } }
    }
    HvButton("Import ${p.validRows - p.assessments.count { (s.decisions[it.record.row] ?: it.defaultDecision) == ImportDecision.Skip }} rows",
        vm::commit, Modifier.fillMaxWidth().padding(vertical = 8.dp), enabled = p.validRows > 0)
}

@Composable
private fun DuplicateItem(a: DuplicateAssessment, decision: ImportDecision, onDecide: (ImportDecision) -> Unit) {
    val r = a.record
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("Row ${r.row}: ${vintageLabel(r.vintage)} ${r.producer}${r.name?.let { " $it" } ?: ""} · ${r.quantity} bottle(s)", style = Hv.type.body, color = Hv.colors.ink)
        Text(a.explanation, style = Hv.type.caption, color = Hv.colors.inkMuted)
        val options = buildList {
            add(ImportDecision.CreateNew to "Keep separate")
            a.candidates.filter { it.vintage == r.vintage }.take(2).forEach { add(ImportDecision.AddToVintage(it.vintageId) to "Add to ${vintageLabel(it.vintage)} ${it.producer}") }
            a.candidates.filter { it.vintage != r.vintage }.distinctBy { it.wineId }.take(1).forEach { add(ImportDecision.AddVintageToWine(it.wineId) to "File under ${it.producer}") }
            add(ImportDecision.Skip to "Skip")
        }
        ChoiceChips(options.map { it.first }, setOf(decision), { d -> options.first { it.first == d }.second }, onDecide)
    }
    HorizontalDivider(color = Hv.colors.outline)
}

// =================================================================================================

class ExportViewModel(graph: AppGraph) : GraphViewModel(graph) {
    fun export(kind: String) = launchIo {
        val today = graph.platform.today()
        val csv = io {
            when (kind) {
                "inventory" -> graph.exporter.inventoryCsv(collectionId)
                "tastings" -> graph.exporter.tastingsCsv(collectionId)
                else -> graph.exporter.financialCsv(collectionId)
            }
        }
        val error = graph.platform.shareText(graph.exporter.suggestedFileName(kind, today), "text/csv", csv)
        if (error != null) messenger.show(error)
    }
}

@Composable
fun ExportScreen(onBack: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel { ExportViewModel(graph) }
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Export", onBack = onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ExportOption("Inventory", "Every bottle in stock with location, purchase and drinking window. Re-imports cleanly.") { vm.export("inventory") }
            ExportOption("Tasting history", "All your notes and ratings, including wines no longer in the cellar.") { vm.export("tastings") }
            ExportOption("Financial records", "Purchases, sales, gifts and valuations in their original currencies.") { vm.export("financial") }
            Text("Files are CSV (UTF-8). Spreadsheet formulas in text are neutralised for safety.", style = Hv.type.caption, color = Hv.colors.inkMuted)
        }
    }
}

@Composable
private fun ExportOption(title: String, description: String, onExport: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = Hv.type.body, color = Hv.colors.ink)
            Text(description, style = Hv.type.caption, color = Hv.colors.inkMuted)
        }
        HvButton("Export", onExport, kind = ButtonKind.SECONDARY)
    }
}
