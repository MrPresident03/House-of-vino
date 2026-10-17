package com.houseofvino.app.ui.design

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.houseofvino.core.insights.DrinkingWindow
import com.houseofvino.core.model.BottleFormats
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.WineColor
import com.houseofvino.core.money.Money
import com.houseofvino.core.money.MoneyFormatStyle
import com.houseofvino.core.search.WindowFilter
import com.houseofvino.data.search.CellarRow

/** Locale-specific money formatting, supplied by the platform. */
val LocalMoneyFormat = staticCompositionLocalOf { MoneyFormatStyle() }

@Composable
fun Money.formatted(): String = LocalMoneyFormat.current.format(this)

// -------------------------------------------------------------------------------------------------
// Bars and headers

@Composable
fun HvTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(title, style = Hv.type.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() })
        },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Hv.colors.bg, titleContentColor = Hv.colors.ink,
            navigationIconContentColor = Hv.colors.ink, actionIconContentColor = Hv.colors.ink),
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = Hv.type.sectionTitle, color = Hv.colors.ink, modifier = Modifier.weight(1f).semantics { heading() })
        trailing?.invoke()
    }
}

// -------------------------------------------------------------------------------------------------
// Ledger row: the signature list item

@Composable
fun LedgerRow(row: CellarRow, currentYear: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val status = DrinkingWindow.status(row.drinkFrom, row.drinkUntil, currentYear)
    val name = listOfNotNull(row.producer, row.wineName).joinToString(" ")
    val spoken = buildString {
        append(vintageLabel(row.vintage)).append(' ').append(name)
        row.appellation?.let { append(", ").append(it) } ?: row.region?.let { append(", ").append(it) }
        append(", ").append(row.quantity).append(if (row.quantity == 1) " bottle" else " bottles")
        if (status != WindowFilter.UNKNOWN) append(", ").append(DrinkingWindow.label(status).lowercase())
    }
    Column(modifier.fillMaxWidth().clickable(onClickLabel = "Open details", role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) { contentDescription = spoken }) {
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(vintageLabel(row.vintage), style = Hv.type.vintageRow, color = Hv.colors.ink, modifier = Modifier.width(64.dp), maxLines = 1)
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(row.producer, style = Hv.type.label, color = Hv.colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(row.wineName ?: row.appellation ?: row.region ?: row.producer, style = Hv.type.wineName, color = Hv.colors.ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                val meta = listOfNotNull(
                    if (row.wineName != null) (row.appellation ?: row.region) else row.region?.takeIf { it != row.appellation },
                    row.formatsMl.takeIf { it.isNotEmpty() && it != listOf(BottleFormats.STANDARD) }?.joinToString(", ") { BottleFormats.nameFor(it) },
                ).joinToString(" · ")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (status != WindowFilter.UNKNOWN) {
                        WindowStatusPill(status)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (meta.isNotEmpty()) Text(meta, style = Hv.type.caption, color = Hv.colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            QuantityFigure(row.quantity)
        }
        HorizontalDivider(Modifier.padding(start = 80.dp), color = Hv.colors.outline, thickness = 1.dp)
    }
}

fun vintageLabel(v: VintageYear): String = when (v) {
    is VintageYear.Year -> v.year.toString()
    VintageYear.NonVintage -> "NV"
    VintageYear.Unknown -> "—"
}

/** Quantity that rolls to its new value after a stock action (the one signature motion). */
@Composable
fun QuantityFigure(quantity: Int, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    AnimatedContent(
        targetState = quantity,
        transitionSpec = {
            if (reduced) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            else {
                val down = targetState < initialState
                (slideInVertically(tween(240)) { h -> if (down) -h else h } + fadeIn(tween(240))) togetherWith
                    (slideOutVertically(tween(240)) { h -> if (down) h else -h } + fadeOut(tween(160)))
            }
        },
        modifier = modifier,
        label = "quantity",
    ) { q ->
        Text(q.toString(), style = Hv.type.figure.copy(fontSize = Hv.type.vintageRow.fontSize), color = if (q < 0) Hv.colors.danger else Hv.colors.ink)
    }
}

@Composable
fun WindowStatusPill(status: WindowFilter, modifier: Modifier = Modifier) {
    val c = Hv.colors
    val color = when (status) {
        WindowFilter.HOLD -> c.hold
        WindowFilter.READY -> c.ready
        WindowFilter.DRINK_SOON -> c.drinkSoon
        WindowFilter.PAST -> c.past
        WindowFilter.UNKNOWN -> c.inkMuted
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(DrinkingWindow.label(status), style = Hv.type.caption, color = color)
    }
}

// -------------------------------------------------------------------------------------------------
// Bottle imagery

fun wineColorArgb(color: WineColor?): Long = when (color) {
    WineColor.RED -> Tokens.WINE_RED
    WineColor.WHITE -> Tokens.WINE_WHITE
    WineColor.ROSE -> Tokens.WINE_ROSE
    WineColor.ORANGE -> Tokens.WINE_ORANGE
    null -> Tokens.WINE_UNKNOWN
}

/** A Bordeaux-shouldered silhouette used when a wine has no photo. Decorative only. */
@Composable
fun BottleSilhouette(color: WineColor?, modifier: Modifier = Modifier) {
    val glass = Color(wineColorArgb(color))
    val label = Hv.colors.surface
    val outline = Hv.colors.outline
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val bw = minOf(w * 0.42f, h * 0.24f)
        val left = (w - bw) / 2
        val neckW = bw * 0.34f
        val neckLeft = (w - neckW) / 2
        val top = h * 0.06f
        val shoulder = h * 0.34f
        val path = Path().apply {
            moveTo(neckLeft, top)
            lineTo(neckLeft + neckW, top)
            lineTo(neckLeft + neckW, h * 0.22f)
            cubicTo(neckLeft + neckW, h * 0.28f, left + bw, h * 0.28f, left + bw, shoulder)
            lineTo(left + bw, h * 0.94f)
            quadraticTo(left + bw, h * 0.97f, left + bw - 4f, h * 0.97f)
            lineTo(left + 4f, h * 0.97f)
            quadraticTo(left, h * 0.97f, left, h * 0.94f)
            lineTo(left, shoulder)
            cubicTo(left, h * 0.28f, neckLeft, h * 0.28f, neckLeft, h * 0.22f)
            close()
        }
        drawPath(path, Brush.horizontalGradient(listOf(glass.copy(alpha = 0.85f), glass, glass.copy(alpha = 0.7f)), left, left + bw))
        drawPath(path, outline, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
        drawRect(label, topLeft = Offset(left + bw * 0.08f, h * 0.55f), size = Size(bw * 0.84f, h * 0.22f))
        drawRect(Color.White.copy(alpha = 0.10f), topLeft = Offset(left + bw * 0.16f, shoulder + 6f), size = Size(bw * 0.08f, h * 0.5f))
    }
}

@Composable
fun BottleTile(row: CellarRow, currentYear: Int, onClick: () -> Unit, modifier: Modifier = Modifier, photo: @Composable ((Modifier) -> Unit)? = null) {
    Column(modifier.clickable(onClick = onClick).semantics(mergeDescendants = true) {
        contentDescription = "${vintageLabel(row.vintage)} ${row.producer} ${row.wineName ?: ""}, ${row.quantity} bottles"
    }.padding(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(6.dp)).background(Hv.colors.surfaceRaised), contentAlignment = Alignment.Center) {
            if (photo != null) photo(Modifier.fillMaxSize()) else BottleSilhouette(row.color, Modifier.fillMaxSize().padding(12.dp))
            Box(Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(Hv.colors.accentContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(row.quantity.toString(), style = Hv.type.label, color = Hv.colors.onAccentContainer)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(vintageLabel(row.vintage), style = Hv.type.vintageRow, color = Hv.colors.ink)
        Text(row.producer, style = Hv.type.label, color = Hv.colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        row.wineName?.let { Text(it, style = Hv.type.bodyDense, color = Hv.colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    }
}

// -------------------------------------------------------------------------------------------------
// Money and provenance

@Composable
fun ProvenanceCaption(source: String, asOf: String, modifier: Modifier = Modifier) {
    Text("Source: $source, $asOf", style = Hv.type.caption, color = Hv.colors.inkMuted, modifier = modifier)
}

// -------------------------------------------------------------------------------------------------
// Buttons

enum class ButtonKind { PRIMARY, SECONDARY, DESTRUCTIVE, QUIET }

@Composable
fun HvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = Hv.type.label)
    }
    val shape = RoundedCornerShape(6.dp)
    val minH = Modifier.heightIn(min = 48.dp)
    when (kind) {
        ButtonKind.PRIMARY -> Button(onClick, modifier.then(minH), enabled, shape = shape, content = content)
        ButtonKind.DESTRUCTIVE -> Button(onClick, modifier.then(minH), enabled, shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = Hv.colors.danger, contentColor = Hv.colors.bg), content = content)
        ButtonKind.SECONDARY -> OutlinedButton(onClick, modifier.then(minH), enabled, shape = shape,
            border = BorderStroke(1.dp, Hv.colors.inkMuted), content = content)
        ButtonKind.QUIET -> TextButton(onClick, modifier.then(minH), enabled, shape = shape, content = content)
    }
}

// -------------------------------------------------------------------------------------------------
// Forms

@Composable
fun HvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supporting: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    placeholder: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = (error ?: supporting)?.let { msg ->
            { Text(msg, modifier = if (error != null) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        singleLine = singleLine,
        minLines = minLines,
        placeholder = placeholder?.let { { Text(it, color = Hv.colors.inkMuted) } },
        trailingIcon = trailing,
        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Hv.colors.surfaceRaised, unfocusedContainerColor = Hv.colors.surfaceRaised,
            errorContainerColor = Hv.colors.surfaceRaised, focusedIndicatorColor = Hv.colors.accent,
            unfocusedIndicatorColor = Hv.colors.inkMuted, focusedLabelColor = Hv.colors.accent,
        ),
    )
}

/** − n + stepper with large touch targets. */
@Composable
fun QuantityStepper(value: Int, onChange: (Int) -> Unit, label: String, min: Int = 0, max: Int = 9999, modifier: Modifier = Modifier) {
    Row(modifier.semantics(mergeDescendants = false) { contentDescription = "$label: $value" }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Hv.type.label, color = Hv.colors.inkMuted, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }, enabled = value > min,
            modifier = Modifier.size(56.dp), shape = RoundedCornerShape(6.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            border = BorderStroke(1.dp, Hv.colors.inkMuted)) { Text("−", style = Hv.type.vintageRow) }
        Text(value.toString(), style = Hv.type.vintageRow, color = Hv.colors.ink, modifier = Modifier.widthIn(min = 56.dp).padding(horizontal = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        OutlinedButton(onClick = { onChange((value + 1).coerceAtMost(max)) }, enabled = value < max,
            modifier = Modifier.size(56.dp), shape = RoundedCornerShape(6.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            border = BorderStroke(1.dp, Hv.colors.inkMuted)) { Icon(Icons.Filled.Add, contentDescription = "Add one") }
    }
}

@Composable
fun <T> ChoiceChips(options: List<T>, selected: Set<T>, label: (T) -> String, onToggle: (T) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { o ->
            FilterChip(
                selected = o in selected, onClick = { onToggle(o) }, label = { Text(label(o)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Hv.colors.accentContainer,
                    selectedLabelColor = Hv.colors.onAccentContainer),
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// States

@Composable
fun LoadingRows(count: Int = 6) {
    Column(Modifier.fillMaxWidth().semantics { contentDescription = "Loading" }) {
        repeat(count) {
            Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp, 22.dp).clip(RoundedCornerShape(4.dp)).background(Hv.colors.surfaceRaised))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Hv.colors.surfaceRaised))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.7f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(Hv.colors.surfaceRaised))
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier, action: Pair<String, () -> Unit>? = null, secondary: Pair<String, () -> Unit>? = null) {
    Column(modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(HvIcons.Bottle, contentDescription = null, tint = Hv.colors.inkMuted, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = Hv.type.sectionTitle, color = Hv.colors.ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = Hv.type.body, color = Hv.colors.inkMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(24.dp)); HvButton(action.first, action.second) }
        if (secondary != null) { Spacer(Modifier.height(8.dp)); HvButton(secondary.first, secondary.second, kind = ButtonKind.QUIET) }
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp).semantics { liveRegion = LiveRegionMode.Assertive }, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = Hv.colors.danger)
        Spacer(Modifier.height(12.dp))
        Text(message, style = Hv.type.body, color = Hv.colors.ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (onRetry != null) { Spacer(Modifier.height(16.dp)); HvButton("Try again", onRetry, kind = ButtonKind.SECONDARY) }
    }
}

@Composable
fun PermissionDenied(what: String, why: String, onOpenSettings: () -> Unit, onManual: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$what access is off", style = Hv.type.sectionTitle, color = Hv.colors.ink)
        Spacer(Modifier.height(8.dp))
        Text(why, style = Hv.type.body, color = Hv.colors.inkMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        HvButton("Open settings", onOpenSettings)
        if (onManual != null) { Spacer(Modifier.height(8.dp)); HvButton("Enter details manually", onManual, kind = ButtonKind.QUIET) }
    }
}

/** A labelled value in detail screens. */
@Composable
fun Fact(label: String, value: String?, modifier: Modifier = Modifier) {
    if (value.isNullOrBlank()) return
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).semantics(mergeDescendants = true) {}) {
        Text(label, style = Hv.type.bodyDense, color = Hv.colors.inkMuted, modifier = Modifier.width(120.dp))
        Text(value, style = Hv.type.bodyDense, color = Hv.colors.ink, modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(value, style = Hv.type.title, color = Hv.colors.ink)
        Text(label, style = Hv.type.caption, color = Hv.colors.inkMuted)
    }
}

@Composable
fun SheetColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

/** Horizontal bar for breakdowns; value in per-mille. */
@Composable
fun ShareBar(label: String, bottles: Int, permille: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).semantics(mergeDescendants = true) {
        contentDescription = "$label, $bottles bottles, ${permille / 10} percent"
    }) {
        Row {
            Text(label, style = Hv.type.bodyDense, color = Hv.colors.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(bottles.toString(), style = Hv.type.figure.copy(fontSize = Hv.type.bodyDense.fontSize), color = Hv.colors.inkMuted)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Hv.colors.surfaceRaised)) {
            Box(Modifier.fillMaxWidth((permille / 1000f).coerceIn(0.01f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Hv.colors.accent))
        }
    }
}
