package com.houseofvino.app.ui.cellar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houseofvino.app.AppGraph
import com.houseofvino.app.ui.common.CurrencyDropdown
import com.houseofvino.app.ui.common.DateField
import com.houseofvino.app.ui.common.FormatPicker
import com.houseofvino.app.ui.common.GraphViewModel
import com.houseofvino.app.ui.common.LocationDropdown
import com.houseofvino.app.ui.common.VintageInput
import com.houseofvino.app.ui.common.VintageKindChoice
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.common.confidenceText
import com.houseofvino.app.ui.common.userMessage
import com.houseofvino.app.ui.design.ChoiceChips
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvButton
import com.houseofvino.app.ui.design.HvTextField
import com.houseofvino.app.ui.design.HvTopBar
import com.houseofvino.app.ui.design.QuantityStepper
import com.houseofvino.app.ui.design.SectionHeader
import com.houseofvino.core.csv.ImportParser
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.model.WineStyle
import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.money.Money
import com.houseofvino.core.scan.Confidence
import com.houseofvino.core.scan.WineSuggestion
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.repo.DuplicateWineException
import com.houseofvino.data.repo.LocationNode
import com.houseofvino.data.repo.LotInput
import com.houseofvino.data.repo.VintageExistsException
import com.houseofvino.data.repo.VintageInput
import com.houseofvino.data.repo.WineInput
import com.houseofvino.data.repo.WineTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class EditorMode { NEW_WINE, NEW_VINTAGE, EDIT }

/** What a scan hands to the editor: editable suggestions plus the label photo, if one was taken. */
class ScanPrefill(val suggestion: WineSuggestion, val labelPhoto: ByteArray?)

data class WineForm(
    val producer: String = "", val name: String = "",
    val vintageKind: VintageKindChoice = VintageKindChoice.YEAR, val year: String = "",
    val color: WineColor? = null, val style: WineStyle? = null,
    val country: String = "", val region: String = "", val subRegion: String = "", val appellation: String = "",
    val classification: String = "", val grapes: String = "",
    val drinkFrom: String = "", val drinkUntil: String = "", val windowSource: String = "", val abv: String = "",
    val notes: String = "", val tags: String = "",
    val addPurchase: Boolean = true, val quantity: Int = 1, val formatMl: Int = 750, val locationId: String? = null,
    val price: String = "", val currency: CurrencyCode = CurrencyCode.EUR, val acquiredOn: String = "", val source: String = "",
    /** Fields filled from a scan, with how sure the scan was. */
    val confidence: Map<String, Confidence> = emptyMap(),
    val barcode: String? = null,
)

data class EditorState(
    val mode: EditorMode,
    val form: WineForm,
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val duplicateOf: List<String> = emptyList(),
    val clashVintageId: String? = null,
    val locations: List<LocationNode> = emptyList(),
    val wineSummary: String? = null,
    val savedVintageId: String? = null,
)

class WineEditorViewModel(
    graph: AppGraph,
    private val wineId: String?,
    private val editVintageId: String?,
    prefillKey: String?,
    private val wishlistItemId: String?,
) : GraphViewModel(graph) {

    private val mode = when {
        editVintageId != null -> EditorMode.EDIT
        wineId != null -> EditorMode.NEW_VINTAGE
        else -> EditorMode.NEW_WINE
    }
    private val _state = MutableStateFlow(EditorState(mode, WineForm(currency = graph.settings.preferences().currency, addPurchase = mode != EditorMode.EDIT)))
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private var labelPhoto: ByteArray? = null

    init {
        when (val prefill = graph.handoff.take<Any>(prefillKey)) {
            is WineSuggestion -> applySuggestion(prefill)
            is ScanPrefill -> { applySuggestion(prefill.suggestion); labelPhoto = prefill.labelPhoto }
        }
        launchIo {
            val locations = io { graph.locations.tree(collectionId) }
            _state.update { it.copy(locations = locations) }
            when (mode) {
                EditorMode.EDIT -> io { loadExisting(editVintageId!!) }
                EditorMode.NEW_VINTAGE -> io {
                    val w = graph.cellar.wine(wineId!!)
                    _state.update { it.copy(wineSummary = w?.displayName) }
                }
                EditorMode.NEW_WINE -> Unit
            }
        }
    }

    private fun loadExisting(vintageId: String) {
        val d = graph.cellar.vintageDetail(vintageId) ?: return
        val w = d.wine
        val v = d.vintage
        _state.update {
            it.copy(form = it.form.copy(
                producer = w.producer, name = w.name.orEmpty(),
                vintageKind = when (v.vintage) { is VintageYear.Year -> VintageKindChoice.YEAR; VintageYear.NonVintage -> VintageKindChoice.NV; VintageYear.Unknown -> VintageKindChoice.UNKNOWN },
                year = v.vintage.yearOrNull?.toString().orEmpty(), color = w.color, style = w.style, country = w.country.orEmpty(),
                region = w.region.orEmpty(), subRegion = w.subRegion.orEmpty(), appellation = w.appellation.orEmpty(),
                classification = w.classification.orEmpty(),
                grapes = w.grapes.joinToString(", ") { g -> g.grape + (g.percent?.let { p -> " $p%" } ?: "") },
                drinkFrom = v.drinkFrom?.toString().orEmpty(), drinkUntil = v.drinkUntil?.toString().orEmpty(),
                windowSource = v.drinkWindowSource.orEmpty(), abv = v.abvBasisPoints?.let { bp -> formatAbv(bp) }.orEmpty(),
                notes = listOfNotNull(w.notes).joinToString(), tags = d.tags.joinToString(", ") { t -> t.name }, addPurchase = false,
            ))
        }
    }

    private fun applySuggestion(s: WineSuggestion) {
        val conf = HashMap<String, Confidence>()
        _state.update { st ->
            var f = st.form
            s.producer?.let { f = f.copy(producer = it.value); conf["producer"] = it.confidence }
            s.name?.let { f = f.copy(name = it.value); conf["name"] = it.confidence }
            s.vintage?.let { v ->
                f = when (val y = v.value) {
                    is VintageYear.Year -> f.copy(vintageKind = VintageKindChoice.YEAR, year = y.year.toString())
                    VintageYear.NonVintage -> f.copy(vintageKind = VintageKindChoice.NV)
                    VintageYear.Unknown -> f.copy(vintageKind = VintageKindChoice.UNKNOWN)
                }
                conf["vintage"] = v.confidence
            }
            s.formatMl?.let { f = f.copy(formatMl = it.value); conf["format"] = it.confidence }
            s.abvBasisPoints?.let { f = f.copy(abv = formatAbv(it.value)); conf["abv"] = it.confidence }
            s.color?.let { f = f.copy(color = it.value); conf["color"] = it.confidence }
            s.style?.let { f = f.copy(style = it.value); conf["style"] = it.confidence }
            s.country?.let { f = f.copy(country = it.value); conf["country"] = it.confidence }
            s.region?.let { f = f.copy(region = it.value); conf["region"] = it.confidence }
            s.appellation?.let { f = f.copy(appellation = it.value); conf["appellation"] = it.confidence }
            st.copy(form = f.copy(confidence = conf, barcode = s.barcode))
        }
    }

    fun edit(transform: (WineForm) -> WineForm) = _state.update { it.copy(form = transform(it.form), errors = emptyMap()) }

    private fun validate(f: WineForm): Map<String, String> = buildMap {
        if (mode != EditorMode.NEW_VINTAGE && f.producer.isBlank()) put("producer", "Enter the producer")
        if (f.vintageKind == VintageKindChoice.YEAR) {
            val y = f.year.toIntOrNull()
            if (y == null || y !in 1700..(graph.platform.currentYear() + 1)) put("year", "Enter a year such as 2015, or choose non-vintage")
        }
        val from = f.drinkFrom.takeIf { it.isNotBlank() }?.toIntOrNull()
        val until = f.drinkUntil.takeIf { it.isNotBlank() }?.toIntOrNull()
        if (f.drinkFrom.isNotBlank() && from == null) put("drinkFrom", "Enter a year")
        if (f.drinkUntil.isNotBlank() && until == null) put("drinkUntil", "Enter a year")
        if (from != null && until != null && from > until) put("drinkUntil", "Must be after the start year")
        if (f.abv.isNotBlank() && parseAbv(f.abv) == null) put("abv", "Enter alcohol as a percentage, e.g. 13.5")
        val grapes = runCatching { ImportParser.parseGrapes(f.grapes) }.getOrNull()
        if (grapes == null) put("grapes", "Use “Merlot 60%, Cabernet Franc 40%”")
        else if (grapes.mapNotNull { it.percent }.sum() > 100) put("grapes", "Percentages add up to more than 100%")
        if (f.addPurchase && mode != EditorMode.EDIT) {
            if (f.quantity < 1) put("quantity", "Add at least one bottle, or turn off “Add bottles now”")
            if (f.price.isNotBlank() && Money.parseOrNull(f.price, f.currency) == null) put("price", "Enter an amount such as 45.00")
            if (f.acquiredOn.isNotBlank() && IsoDate.parseOrNull(f.acquiredOn) == null) put("acquiredOn", "Use YYYY-MM-DD")
        }
    }

    private fun wineInput(f: WineForm) = WineInput(
        producer = f.producer, name = f.name, color = f.color, style = f.style, country = f.country, region = f.region,
        subRegion = f.subRegion, appellation = f.appellation, classification = f.classification,
        grapes = ImportParser.parseGrapes(f.grapes), notes = f.notes,
    )

    private fun vintageInput(f: WineForm) = VintageInput(
        vintage = when (f.vintageKind) {
            VintageKindChoice.YEAR -> VintageYear.Year(f.year.toInt())
            VintageKindChoice.NV -> VintageYear.NonVintage
            VintageKindChoice.UNKNOWN -> VintageYear.Unknown
        },
        drinkFrom = f.drinkFrom.toIntOrNull(), drinkUntil = f.drinkUntil.toIntOrNull(),
        drinkWindowSource = f.windowSource.ifBlank { if (f.drinkFrom.isNotBlank() || f.drinkUntil.isNotBlank()) "My estimate" else "" },
        abvBasisPoints = parseAbv(f.abv), notes = null,
    )

    private fun lotInput(f: WineForm): LotInput? = if (!f.addPurchase) null else LotInput(
        quantity = f.quantity, formatMl = f.formatMl, locationId = f.locationId,
        acquiredOn = IsoDate.parseOrNull(f.acquiredOn), source = f.source,
        unitPrice = f.price.takeIf { it.isNotBlank() }?.let { Money.parse(it, f.currency) },
        wishlistItemId = wishlistItemId,
    )

    /** Saves. [target] overrides the default when the user resolved a duplicate prompt. */
    fun save(target: WineTarget? = null, allowDuplicate: Boolean = false) {
        val f = _state.value.form
        val errors = validate(f)
        if (errors.isNotEmpty()) { _state.update { it.copy(errors = errors) }; return }
        _state.update { it.copy(saving = true, duplicateOf = emptyList(), clashVintageId = null) }
        launchIo(onError = { e ->
            when (e) {
                is DuplicateWineException -> _state.update { it.copy(saving = false, duplicateOf = e.existingWineIds) }
                is VintageExistsException -> _state.update { it.copy(saving = false, clashVintageId = e.existingVintageId) }
                else -> { _state.update { it.copy(saving = false) }; messenger.show(e.userMessage()) }
            }
        }) {
            val tags = f.tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val vintageId = io {
                when (mode) {
                    EditorMode.EDIT -> {
                        val v = graph.cellar.vintage(editVintageId!!)!!
                        graph.cellar.updateWine(v.wineId, wineInput(f))
                        graph.cellar.updateVintage(v.id, vintageInput(f).copy(notes = v.notes))
                        graph.cellar.setTags(collectionId, v.id, tags)
                        v.id
                    }
                    EditorMode.NEW_VINTAGE -> graph.cellar.addWine(collectionId, WineTarget.ExistingWine(wineId!!), vintageInput(f), lotInput(f), tags)
                    EditorMode.NEW_WINE -> graph.cellar.addWine(collectionId, target ?: WineTarget.NewWine(wineInput(f)), vintageInput(f), lotInput(f), tags, allowDuplicate)
                }
            }
            if (wishlistItemId != null && mode != EditorMode.EDIT) io { graph.wishlist.markAcquiredIfOpen(wishlistItemId, vintageId) }
            labelPhoto?.let { bytes ->
                io {
                    val photoId = graph.photos.newPhotoId()
                    val path = graph.platform.savePhoto(photoId, bytes)
                    graph.photos.addWithId(photoId, collectionId, com.houseofvino.data.repo.PhotoOwner.VINTAGE, vintageId, path, 0, 0, bytes.size.toLong())
                    graph.cellar.setLabelPhoto(vintageId, photoId)
                }
            }
            f.barcode?.let { code ->
                io { graph.cellar.vintage(vintageId)?.let { v -> graph.cellar.linkBarcode(collectionId, code, "EAN_13", v.wineId, vintageId, f.formatMl) } }
            }
            messenger.show(when (mode) { EditorMode.EDIT -> "Changes saved"; else -> if (f.addPurchase) "Added ${f.quantity} bottle${if (f.quantity == 1) "" else "s"}" else "Wine added" })
            _state.update { it.copy(saving = false, savedVintageId = vintageId) }
        }
    }

    fun dismissDuplicate() = _state.update { it.copy(duplicateOf = emptyList(), clashVintageId = null) }

    companion object {
        fun formatAbv(bp: Int): String = "${bp / 100}" + (bp % 100).let { r -> if (r == 0) "" else "." + r.toString().padStart(2, '0').trimEnd('0') }
        fun parseAbv(text: String): Int? {
            if (text.isBlank()) return null
            val t = text.trim().removeSuffix("%").trim().replace(',', '.')
            val parts = t.split('.')
            val whole = parts[0].toIntOrNull() ?: return null
            val frac = parts.getOrNull(1)?.take(2)?.padEnd(2, '0')?.toIntOrNull() ?: 0
            if (parts.size > 2) return null
            return (whole * 100 + frac).takeIf { it in 0..8000 }
        }
    }
}

@Composable
fun WineEditorScreen(
    wineId: String?,
    editVintageId: String?,
    prefillKey: String?,
    wishlistItemId: String?,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val graph = appGraph()
    val vm = viewModel(key = "editor-$wineId-$editVintageId-$prefillKey") { WineEditorViewModel(graph, wineId, editVintageId, prefillKey, wishlistItemId) }
    val state by vm.state.collectAsState()
    state.savedVintageId?.let { id -> androidx.compose.runtime.LaunchedEffect(id) { onSaved(id) } }
    val f = state.form
    val e = state.errors
    fun hint(key: String) = f.confidence[key]?.let { confidenceText(it) }

    Column(Modifier.fillMaxSize()) {
        HvTopBar(when (state.mode) { EditorMode.EDIT -> "Edit wine"; EditorMode.NEW_VINTAGE -> "Add vintage"; EditorMode.NEW_WINE -> "Add wine" }, onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.mode == EditorMode.NEW_VINTAGE) {
                Text(state.wineSummary ?: "", style = Hv.type.wineName, color = Hv.colors.ink)
            } else {
                HvTextField(f.producer, { v -> vm.edit { it.copy(producer = v) } }, "Producer", error = e["producer"], supporting = hint("producer"),
                    modifier = Modifier.testTag("field-producer"))
                HvTextField(f.name, { v -> vm.edit { it.copy(name = v) } }, "Wine or cuvée (optional)", supporting = hint("name") ?: "Leave blank for the producer's main wine")
            }
            VintageInput(f.vintageKind, f.year, { k -> vm.edit { it.copy(vintageKind = k) } }, { y -> vm.edit { it.copy(year = y) } }, e["year"], f.confidence["vintage"])

            if (state.mode != EditorMode.NEW_VINTAGE) {
                Text("Colour", style = Hv.type.label, color = Hv.colors.inkMuted)
                ChoiceChips(WineColor.entries, setOfNotNull(f.color), { it.name.lowercase().replaceFirstChar(Char::uppercase).replace("Rose", "Rosé") },
                    { c -> vm.edit { it.copy(color = if (it.color == c) null else c) } })
                Text("Style", style = Hv.type.label, color = Hv.colors.inkMuted)
                ChoiceChips(WineStyle.entries, setOfNotNull(f.style), { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                    { s -> vm.edit { it.copy(style = if (it.style == s) null else s) } })
                HvTextField(f.country, { v -> vm.edit { it.copy(country = v) } }, "Country", supporting = hint("country"))
                HvTextField(f.region, { v -> vm.edit { it.copy(region = v) } }, "Region", supporting = hint("region"))
                HvTextField(f.appellation, { v -> vm.edit { it.copy(appellation = v) } }, "Appellation", supporting = hint("appellation"))
                HvTextField(f.subRegion, { v -> vm.edit { it.copy(subRegion = v) } }, "Sub-region or vineyard")
                HvTextField(f.classification, { v -> vm.edit { it.copy(classification = v) } }, "Classification", placeholder = "e.g. Premier Grand Cru Classé")
                HvTextField(f.grapes, { v -> vm.edit { it.copy(grapes = v) } }, "Grapes", error = e["grapes"], placeholder = "Merlot 60%, Cabernet Franc 40%")
            }

            SectionHeader("Drinking window", Modifier.padding(start = 0.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HvTextField(f.drinkFrom, { v -> vm.edit { it.copy(drinkFrom = v.filter(Char::isDigit).take(4)) } }, "From", Modifier.weight(1f), error = e["drinkFrom"], keyboard = KeyboardType.Number)
                HvTextField(f.drinkUntil, { v -> vm.edit { it.copy(drinkUntil = v.filter(Char::isDigit).take(4)) } }, "Until", Modifier.weight(1f), error = e["drinkUntil"], keyboard = KeyboardType.Number)
            }
            HvTextField(f.windowSource, { v -> vm.edit { it.copy(windowSource = v) } }, "Window source", placeholder = "e.g. My estimate, producer, critic")
            HvTextField(f.abv, { v -> vm.edit { it.copy(abv = v) } }, "Alcohol %", error = e["abv"], supporting = hint("abv"), keyboard = KeyboardType.Decimal)
            HvTextField(f.tags, { v -> vm.edit { it.copy(tags = v) } }, "Tags", placeholder = "Separate with commas")
            if (state.mode != EditorMode.NEW_VINTAGE) {
                HvTextField(f.notes, { v -> vm.edit { it.copy(notes = v) } }, "Notes", singleLine = false, minLines = 2)
            }

            if (state.mode != EditorMode.EDIT) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Add bottles now", style = Hv.type.sectionTitle, color = Hv.colors.ink, modifier = Modifier.weight(1f))
                    Switch(f.addPurchase, { on -> vm.edit { it.copy(addPurchase = on) } })
                }
                if (f.addPurchase) PurchaseFields(f, e, state.locations) { t -> vm.edit(t) }
            }
            HvButton(if (state.saving) "Saving…" else "Save", { vm.save() }, Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag("save-wine"), enabled = !state.saving)
        }
    }

    if (state.duplicateOf.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = vm::dismissDuplicate,
            title = { Text("You already have this wine") },
            text = { Text("${f.producer}${if (f.name.isNotBlank()) " ${f.name}" else ""} is in your cellar. Add this vintage to it, or keep it as a separate wine?") },
            confirmButton = { TextButton(onClick = { vm.save(WineTarget.ExistingWine(state.duplicateOf.first())) }) { Text("Add to existing") } },
            dismissButton = { TextButton(onClick = { vm.save(allowDuplicate = true) }) { Text("Keep separate") } },
        )
    }
    state.clashVintageId?.let { clash ->
        AlertDialog(
            onDismissRequest = vm::dismissDuplicate,
            title = { Text("That vintage already exists") },
            text = { Text("Vintages are never merged automatically. Open the existing record instead, or choose a different year.") },
            confirmButton = { TextButton(onClick = { vm.dismissDuplicate(); onSaved(clash) }) { Text("Open it") } },
            dismissButton = { TextButton(onClick = vm::dismissDuplicate) { Text("Change year") } },
        )
    }
}

@Composable
fun PurchaseFields(f: WineForm, e: Map<String, String>, locations: List<LocationNode>, edit: ((WineForm) -> WineForm) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        QuantityStepper(f.quantity, { q -> edit { it.copy(quantity = q) } }, "Bottles", min = 1)
        e["quantity"]?.let { Text(it, color = Hv.colors.danger, style = Hv.type.caption) }
        FormatPicker(f.formatMl) { ml -> edit { it.copy(formatMl = ml) } }
        LocationDropdown(locations, f.locationId, { id -> edit { it.copy(locationId = id) } })
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HvTextField(f.price, { v -> edit { it.copy(price = v) } }, "Price per bottle", Modifier.weight(1.4f), error = e["price"], keyboard = KeyboardType.Decimal)
            CurrencyDropdown(f.currency, { c -> edit { it.copy(currency = c) } }, Modifier.weight(1f))
        }
        DateField("Purchase date", f.acquiredOn, { v -> edit { it.copy(acquiredOn = v) } }, e["acquiredOn"])
        HvTextField(f.source, { v -> edit { it.copy(source = v) } }, "Bought from", placeholder = "Merchant, auction, winery…")
    }
}
