package com.houseofvino.app.ui.cellar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvButton
import com.houseofvino.app.ui.design.HvTextField
import com.houseofvino.app.ui.design.SectionHeader
import com.houseofvino.app.ui.design.wineColorArgb
import com.houseofvino.core.csv.ImportParser
import com.houseofvino.core.model.GrapeShare
import com.houseofvino.core.model.GrapeVariety
import com.houseofvino.core.model.GrapeVarieties
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.text.TextNormalizer

// -------------------------------------------------------------------------------------------------
// Grapes string helpers. WineForm.grapes ("Merlot 60%, Cabernet Franc 40%") stays the single source of truth.

private val RESERVED_GRAPE_CHARS = ",;/|+%()".toSet()

private fun parseGrapesOrEmpty(text: String): List<GrapeShare> =
    runCatching { ImportParser.parseGrapes(text) }.getOrDefault(emptyList())

private fun formatGrapes(shares: List<GrapeShare>): String =
    shares.joinToString(", ") { g -> g.grape + (g.percent?.let { p -> " $p%" } ?: "") }

/** Same grape under any of its names ("Shiraz" and "Syrah") compares equal. */
private fun grapeKey(name: String): String =
    TextNormalizer.normalize(GrapeVarieties.find(name)?.name ?: name)

/** Typed text made safe for the grapes field: no characters the parser splits on or reads as percentages. */
private fun cleanCustomGrape(text: String): String =
    text.filterNot { it in RESERVED_GRAPE_CHARS }.trim().replace(Regex("\\s+"), " ")

/** Adds [name] to the grapes string, or removes it if present; other grapes keep their order and percentages. */
internal fun toggleGrape(grapes: String, name: String): String {
    val key = grapeKey(name)
    val shares = parseGrapesOrEmpty(grapes)
    val next = if (shares.any { grapeKey(it.grape) == key }) shares.filterNot { grapeKey(it.grape) == key }
    else if (name.isBlank()) shares
    else shares + GrapeShare(name.trim(), null)
    return formatGrapes(next)
}

/** The colour shared by every selected grape, or null when there are none, one is unknown, or they differ. */
internal fun inferredGrapeColor(grapes: String): WineColor? {
    val shares = parseGrapesOrEmpty(grapes)
    if (shares.isEmpty()) return null
    val colors = shares.map { GrapeVarieties.find(it.grape)?.color ?: return null }.toSet()
    return colors.singleOrNull()
}

/** Toggles a grape and re-infers the colour while it is empty or was itself inferred. Never overrides a chosen colour. */
internal fun WineForm.withGrapeToggled(name: String): WineForm {
    val updated = toggleGrape(grapes, name)
    if (color != null && !colorInferred) return copy(grapes = updated)
    val inferred = inferredGrapeColor(updated)
    return copy(grapes = updated, color = inferred, colorInferred = inferred != null)
}

private fun selectedGrapeNames(grapes: String): List<String> = parseGrapesOrEmpty(grapes).map { it.grape }

// -------------------------------------------------------------------------------------------------
// UI

/** The tappable "Varietal" field in the quick form. */
@Composable
internal fun VarietalField(grapes: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val names = selectedGrapeNames(grapes)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Varietal", style = Hv.type.label, color = Hv.colors.inkMuted)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(6.dp))
                .border(1.dp, Hv.colors.outline, RoundedCornerShape(6.dp))
                .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (names.isEmpty()) "Choose varietal" else names.joinToString(", "),
                style = Hv.type.body,
                color = if (names.isEmpty()) Hv.colors.inkMuted else Hv.colors.ink,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Hv.colors.inkMuted)
        }
    }
}

/** Full-screen searchable list of grape varieties. Tapping a row calls [onToggle] with the grape's name. */
@Composable
internal fun VarietalPicker(grapes: String, onToggle: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val selected = selectedGrapeNames(grapes)
    val selectedKeys = selected.map { grapeKey(it) }.toSet()
    val trimmed = query.trim()
    val results = remember(trimmed) { GrapeVarieties.search(trimmed) }
    val custom = cleanCustomGrape(trimmed)
    val offerCustom = custom.isNotEmpty() && GrapeVarieties.find(custom) == null && grapeKey(custom) !in selectedKeys

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Hv.colors.bg) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Varietal", style = Hv.type.title, color = Hv.colors.ink, modifier = Modifier.weight(1f))
                    HvButton("Done", onDismiss, Modifier.testTag("varietal-done"))
                }
                HvTextField(
                    query, { query = it }, "Search grapes",
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag("varietal-search"),
                    placeholder = "e.g. Syrah, Shiraz, Grüner",
                )
                if (selected.isNotEmpty()) {
                    FlowRow(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        selected.forEach { name ->
                            FilterChip(
                                selected = true, onClick = { onToggle(name) }, label = { Text(name) },
                                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove $name", modifier = Modifier.size(18.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Hv.colors.accentContainer, selectedLabelColor = Hv.colors.onAccentContainer),
                                modifier = Modifier.testTag("varietal-chip-$name"),
                            )
                        }
                    }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("varietal-list")) {
                    if (offerCustom) {
                        item(key = "custom") {
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onToggle(custom); query = "" }
                                    .padding(horizontal = 16.dp, vertical = 8.dp).testTag("varietal-add-custom"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Add “$custom”", style = Hv.type.body, color = Hv.colors.accent)
                            }
                        }
                    }
                    if (trimmed.isEmpty()) {
                        item(key = "h-common") { SectionHeader("Common") }
                        items(GrapeVarieties.common, key = { "c-" + it.name }) { v ->
                            VarietyRow(v, grapeKey(v.name) in selectedKeys) { onToggle(v.name) }
                        }
                        item(key = "h-all") { SectionHeader("All varieties") }
                        items(results, key = { "a-" + it.name }) { v ->
                            VarietyRow(v, grapeKey(v.name) in selectedKeys) { onToggle(v.name) }
                        }
                    } else {
                        items(results, key = { "r-" + it.name }) { v ->
                            VarietyRow(v, grapeKey(v.name) in selectedKeys) { onToggle(v.name) }
                        }
                        if (results.isEmpty() && !offerCustom) {
                            item(key = "none") {
                                Text("No matching grapes", style = Hv.type.caption, color = Hv.colors.inkMuted, modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VarietyRow(variety: GrapeVariety, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp).testTag("varietal-row-${variety.name}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(12.dp).clip(CircleShape).background(Color(wineColorArgb(variety.color)))
                .border(1.dp, Hv.colors.outline, CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(variety.name, style = Hv.type.body, color = Hv.colors.ink)
            if (variety.synonyms.isNotEmpty()) {
                Text(variety.synonyms.joinToString(", "), style = Hv.type.caption, color = Hv.colors.inkMuted)
            }
        }
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Hv.colors.accent)
    }
}
