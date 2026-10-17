package com.houseofvino.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.houseofvino.app.ui.design.ChoiceChips
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvTextField
import com.houseofvino.core.model.BottleFormats
import com.houseofvino.core.money.CurrencyCode
import com.houseofvino.core.scan.Confidence
import com.houseofvino.core.time.IsoDate
import com.houseofvino.data.repo.LocationNode

/** A dropdown over a fixed list of options. */
@Composable
fun <T> Dropdown(
    label: String,
    options: List<T>,
    selected: T?,
    display: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Choose",
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
        HvTextField(
            value = selected?.let(display) ?: "",
            onValueChange = {},
            label = label,
            placeholder = placeholder,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            trailing = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(display(o)) }, onClick = { onSelect(o); open = false })
            }
        }
    }
}

@Composable
fun CurrencyDropdown(selected: CurrencyCode, onSelect: (CurrencyCode) -> Unit, modifier: Modifier = Modifier, label: String = "Currency") {
    Dropdown(label, CurrencyCode.supported, selected, { it.code }, onSelect, modifier)
}

/** Location picker over the tree, indented by depth, with "Not put away". */
@Composable
fun LocationDropdown(nodes: List<LocationNode>, selectedId: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier, label: String = "Location") {
    val options: List<LocationNode?> = listOf<LocationNode?>(null) + nodes
    Dropdown(label, options, options.firstOrNull { it?.location?.id == selectedId },
        { n -> if (n == null) "Not put away" else "  ".repeat(n.depth) + n.location.name },
        { onSelect(it?.location?.id) }, modifier)
}

/** ISO date entry with a picker. Typing is allowed; invalid input shows an error. */
@Composable
fun DateField(label: String, value: String, onValueChange: (String) -> Unit, error: String?, modifier: Modifier = Modifier) {
    var picking by remember { mutableStateOf(false) }
    HvTextField(value, onValueChange, label, modifier, error = error, placeholder = "YYYY-MM-DD", keyboard = KeyboardType.Number,
        trailing = { IconButton(onClick = { picking = true }) { Icon(Icons.Filled.DateRange, contentDescription = "Choose date") } })
    if (picking) {
        val initial = IsoDate.parseOrNull(value)?.let { it.epochDay * 86_400_000L }
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onValueChange(IsoDate.fromEpochMillis(it).toString()) }
                    picking = false
                }) { Text("Choose") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
}

enum class VintageKindChoice(val label: String) { YEAR("Year"), NV("Non-vintage"), UNKNOWN("Not known") }

@Composable
fun VintageInput(kind: VintageKindChoice, year: String, onKind: (VintageKindChoice) -> Unit, onYear: (String) -> Unit, error: String?, confidence: Confidence? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            VintageKindChoice.entries.forEachIndexed { i, k ->
                SegmentedButton(selected = kind == k, onClick = { onKind(k) }, shape = SegmentedButtonDefaults.itemShape(i, VintageKindChoice.entries.size)) {
                    Text(k.label)
                }
            }
        }
        if (kind == VintageKindChoice.YEAR) {
            HvTextField(year, { onYear(it.filter(Char::isDigit).take(4)) }, "Vintage year", keyboard = KeyboardType.Number, error = error,
                supporting = confidence?.let { confidenceText(it) })
        }
    }
}

@Composable
fun FormatPicker(formatMl: Int, onFormat: (Int) -> Unit) {
    val common = listOf(375, 750, 1500, 3000)
    var other by remember { mutableStateOf(if (formatMl in common) "" else formatMl.toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Bottle size", style = Hv.type.label, color = Hv.colors.inkMuted)
        ChoiceChips(common, setOf(formatMl), { BottleFormats.nameFor(it) }, { onFormat(it); other = "" })
        HvTextField(other, { t -> other = t.filter(Char::isDigit).take(5); other.toIntOrNull()?.takeIf { it in 50..30000 }?.let(onFormat) },
            "Other size (ml)", keyboard = KeyboardType.Number)
    }
}

fun confidenceText(c: Confidence): String = when (c) {
    Confidence.HIGH -> "Read from the label — check it"
    Confidence.MEDIUM -> "Probably right — please check"
    Confidence.LOW -> "A guess — please check"
}

@Composable
fun FieldRow(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) { content() }
}

@Composable
fun Gap() { androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp)) }
