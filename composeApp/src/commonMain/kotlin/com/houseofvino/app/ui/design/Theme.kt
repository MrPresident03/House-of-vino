package com.houseofvino.app.ui.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.houseofvino.app.resources.Res
import com.houseofvino.app.resources.bodoni_display
import com.houseofvino.app.resources.bodoni_heading
import com.houseofvino.app.resources.bodoni_heading_italic
import com.houseofvino.app.resources.bodoni_heading_semibold
import com.houseofvino.app.resources.plex_medium
import com.houseofvino.app.resources.plex_regular
import com.houseofvino.app.resources.plex_semibold
import com.houseofvino.data.repo.ThemePreference
import org.jetbrains.compose.resources.Font

/** Semantic colours beyond Material's scheme. */
@Immutable
data class HvColors(
    val bg: Color, val surface: Color, val surfaceRaised: Color, val outline: Color,
    val ink: Color, val inkMuted: Color, val accent: Color, val onAccent: Color,
    val accentContainer: Color, val onAccentContainer: Color, val danger: Color,
    val hold: Color, val ready: Color, val drinkSoon: Color, val past: Color,
    val isDark: Boolean,
)

/** Typographic roles from docs/DESIGN_SYSTEM.md. */
@Immutable
data class HvType(
    val vintageDisplay: TextStyle,
    val title: TextStyle,
    val wineName: TextStyle,
    val vintageRow: TextStyle,
    val body: TextStyle,
    val bodyDense: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val figure: TextStyle,
    val sectionTitle: TextStyle,
)

private fun Palette.toColors(dark: Boolean) = HvColors(
    Color(bg), Color(surface), Color(surfaceRaised), Color(outline), Color(ink), Color(inkMuted), Color(accent),
    Color(onAccent), Color(accentContainer), Color(onAccentContainer), Color(danger), Color(hold), Color(ready),
    Color(drinkSoon), Color(past), dark,
)

val LocalHvColors = staticCompositionLocalOf { Tokens.Dark.toColors(true) }
val LocalHvType = staticCompositionLocalOf<HvType> { error("HvTheme not set") }
/** True when the OS asks for reduced motion; animations check this. */
val LocalReducedMotion = staticCompositionLocalOf { false }

object Hv {
    val colors: HvColors @Composable get() = LocalHvColors.current
    val type: HvType @Composable get() = LocalHvType.current
}

@Composable
fun HvTheme(
    preference: ThemePreference = ThemePreference.SYSTEM,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
    }
    val colors = (if (dark) Tokens.Dark else Tokens.Light).toColors(dark)

    val bodoni = FontFamily(
        Font(Res.font.bodoni_heading, FontWeight.Medium),
        Font(Res.font.bodoni_heading_semibold, FontWeight.SemiBold),
        Font(Res.font.bodoni_heading_italic, FontWeight.Medium, FontStyle.Italic),
    )
    val bodoniDisplay = FontFamily(Font(Res.font.bodoni_display, FontWeight.Medium))
    val plex = FontFamily(
        Font(Res.font.plex_regular, FontWeight.Normal),
        Font(Res.font.plex_medium, FontWeight.Medium),
        Font(Res.font.plex_semibold, FontWeight.SemiBold),
    )

    val type = HvType(
        vintageDisplay = TextStyle(fontFamily = bodoniDisplay, fontWeight = FontWeight.Medium, fontSize = 56.sp, lineHeight = 60.sp, fontFeatureSettings = "lnum, tnum"),
        title = TextStyle(fontFamily = bodoni, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
        wineName = TextStyle(fontFamily = bodoni, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
        vintageRow = TextStyle(fontFamily = bodoni, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 26.sp, fontFeatureSettings = "lnum, tnum"),
        body = TextStyle(fontFamily = plex, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyDense = TextStyle(fontFamily = plex, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        label = TextStyle(fontFamily = plex, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        caption = TextStyle(fontFamily = plex, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        figure = TextStyle(fontFamily = plex, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp),
        sectionTitle = TextStyle(fontFamily = bodoni, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
    )

    val scheme: ColorScheme = if (dark) darkColorScheme(
        primary = colors.accent, onPrimary = colors.onAccent, primaryContainer = colors.accentContainer,
        onPrimaryContainer = colors.onAccentContainer, secondary = colors.inkMuted, onSecondary = colors.bg,
        background = colors.bg, onBackground = colors.ink, surface = colors.surface, onSurface = colors.ink,
        surfaceVariant = colors.surfaceRaised, onSurfaceVariant = colors.inkMuted, surfaceContainer = colors.surface,
        surfaceContainerHigh = colors.surfaceRaised, surfaceContainerHighest = colors.surfaceRaised,
        surfaceContainerLow = colors.surface, surfaceContainerLowest = colors.bg,
        outline = colors.inkMuted, outlineVariant = colors.outline, error = colors.danger, onError = colors.bg,
        secondaryContainer = colors.accentContainer, onSecondaryContainer = colors.onAccentContainer,
    ) else lightColorScheme(
        primary = colors.accent, onPrimary = colors.onAccent, primaryContainer = colors.accentContainer,
        onPrimaryContainer = colors.onAccentContainer, secondary = colors.inkMuted, onSecondary = colors.surface,
        background = colors.bg, onBackground = colors.ink, surface = colors.surface, onSurface = colors.ink,
        surfaceVariant = colors.surfaceRaised, onSurfaceVariant = colors.inkMuted, surfaceContainer = colors.surface,
        surfaceContainerHigh = colors.surfaceRaised, surfaceContainerHighest = colors.surfaceRaised,
        surfaceContainerLow = colors.surface, surfaceContainerLowest = colors.bg,
        outline = colors.inkMuted, outlineVariant = colors.outline, error = colors.danger, onError = colors.surface,
        secondaryContainer = colors.accentContainer, onSecondaryContainer = colors.onAccentContainer,
    )

    val m3Type = Typography(
        displayLarge = type.vintageDisplay, headlineMedium = type.title, headlineSmall = type.wineName,
        titleLarge = type.sectionTitle, titleMedium = type.label.copy(fontSize = 16.sp), titleSmall = type.label,
        bodyLarge = type.body, bodyMedium = type.bodyDense, bodySmall = type.caption,
        labelLarge = type.label, labelMedium = type.caption.copy(fontWeight = FontWeight.Medium), labelSmall = type.caption,
    )
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(6.dp), medium = RoundedCornerShape(6.dp),
        large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(16.dp),
    )

    CompositionLocalProvider(LocalHvColors provides colors, LocalHvType provides type, LocalReducedMotion provides reducedMotion) {
        MaterialTheme(colorScheme = scheme, typography = m3Type, shapes = shapes, content = content)
    }
}
