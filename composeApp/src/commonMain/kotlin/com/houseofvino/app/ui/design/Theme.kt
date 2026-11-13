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
import com.houseofvino.app.resources.cormorant_heading
import com.houseofvino.app.resources.cormorant_heading_italic
import com.houseofvino.app.resources.cormorant_heading_semibold
import com.houseofvino.app.resources.playfair_heading
import com.houseofvino.app.resources.playfair_heading_italic
import com.houseofvino.app.resources.playfair_heading_semibold
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
    style: ThemeStyle = ThemeStyle.BOLD,
    font: FontChoice = FontChoice.MODERN,
    content: @Composable () -> Unit,
) {
    val dark = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
    }
    val colors = (if (dark) style.dark else style.light).toColors(dark)

    // Only the chosen pairing is loaded.
    val fonts: FontSet = when (font) {
        FontChoice.CLASSIC -> FontSet(
            heading = FontFamily(
                Font(Res.font.bodoni_heading, FontWeight.Medium),
                Font(Res.font.bodoni_heading_semibold, FontWeight.SemiBold),
                Font(Res.font.bodoni_heading_italic, FontWeight.Medium, FontStyle.Italic),
            ),
            display = FontFamily(Font(Res.font.bodoni_display, FontWeight.Medium)),
            body = plexFamily(),
        )
        FontChoice.EDITORIAL -> FontFamily(
            Font(Res.font.playfair_heading, FontWeight.Medium),
            Font(Res.font.playfair_heading_semibold, FontWeight.SemiBold),
            Font(Res.font.playfair_heading_italic, FontWeight.Medium, FontStyle.Italic),
        ).let { FontSet(heading = it, display = it, body = plexFamily()) }
        // Cormorant has a small x-height, so its headings are set a little larger to read the same size.
        FontChoice.GARAMOND -> FontFamily(
            Font(Res.font.cormorant_heading, FontWeight.Medium),
            Font(Res.font.cormorant_heading_semibold, FontWeight.SemiBold),
            Font(Res.font.cormorant_heading_italic, FontWeight.Medium, FontStyle.Italic),
        ).let { FontSet(heading = it, display = it, body = plexFamily(), headingScale = 1.12f) }
        FontChoice.MODERN -> plexFamily().let { FontSet(heading = it, display = it, body = it) }
        FontChoice.SYSTEM -> FontSet(heading = FontFamily.Default, display = FontFamily.Default, body = FontFamily.Default)
    }
    val heading = fonts.heading
    val display = fonts.display
    val body = fonts.body
    fun h(size: Int) = (size * fonts.headingScale).sp

    val type = HvType(
        vintageDisplay = TextStyle(fontFamily = display, fontWeight = FontWeight.Medium, fontSize = h(56), lineHeight = h(60), fontFeatureSettings = "lnum, tnum"),
        title = TextStyle(fontFamily = heading, fontWeight = FontWeight.SemiBold, fontSize = h(28), lineHeight = h(34), letterSpacing = (-0.3).sp),
        wineName = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = h(20), lineHeight = h(26)),
        vintageRow = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = h(22), lineHeight = h(26), fontFeatureSettings = "lnum, tnum"),
        body = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyDense = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        label = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        caption = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        figure = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp),
        sectionTitle = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = h(20), lineHeight = h(26)),
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
        extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp),
    )

    CompositionLocalProvider(LocalHvColors provides colors, LocalHvType provides type, LocalReducedMotion provides reducedMotion) {
        MaterialTheme(colorScheme = scheme, typography = m3Type, shapes = shapes, content = content)
    }
}

/** `heading` carries Medium, SemiBold and Medium Italic; `display` is the big vintage numeral. */
private class FontSet(val heading: FontFamily, val display: FontFamily, val body: FontFamily, val headingScale: Float = 1f)

@Composable
private fun plexFamily(): FontFamily = FontFamily(
    Font(Res.font.plex_regular, FontWeight.Normal),
    Font(Res.font.plex_medium, FontWeight.Medium),
    Font(Res.font.plex_semibold, FontWeight.SemiBold),
)
