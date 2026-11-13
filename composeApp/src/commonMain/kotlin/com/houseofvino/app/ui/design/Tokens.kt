package com.houseofvino.app.ui.design

/**
 * Raw colour tokens as ARGB longs, kept free of Compose types so contrast rules can be unit-tested
 * on any platform (see DesignTokensTest). Values documented in docs/DESIGN_SYSTEM.md.
 */
data class Palette(
    val bg: Long,
    val surface: Long,
    val surfaceRaised: Long,
    val outline: Long,
    val ink: Long,
    val inkMuted: Long,
    val accent: Long,
    val onAccent: Long,
    val accentContainer: Long,
    val onAccentContainer: Long,
    val danger: Long,
    val hold: Long,
    val ready: Long,
    val drinkSoon: Long,
    val past: Long,
)

object Tokens {
    // Colour themes (Settings › Preferences). DesignTokensTest checks every palette listed in ThemeStyle.

    /** Bold, dark (the default): near-black with a bright wine accent. */
    val Dark = Palette(
        bg = 0xFF0B0B0E, surface = 0xFF1A1A20, surfaceRaised = 0xFF24242C, outline = 0xFF34343E,
        ink = 0xFFF5F5F7, inkMuted = 0xFF9E9EAA, accent = 0xFFFF4D6D, onAccent = 0xFF1A0208,
        accentContainer = 0xFF4A1220, onAccentContainer = 0xFFFFD6DE, danger = 0xFFFF7A6B,
        hold = 0xFF6EA8FF, ready = 0xFF3DDC84, drinkSoon = 0xFFFFB547, past = 0xFFC9A29A,
    )

    /** Bold, light. */
    val Light = Palette(
        bg = 0xFFFFFFFF, surface = 0xFFF4F4F6, surfaceRaised = 0xFFEAEAEE, outline = 0xFFD9D9DF,
        ink = 0xFF111114, inkMuted = 0xFF5E5E69, accent = 0xFFC8103E, onAccent = 0xFFFFFFFF,
        accentContainer = 0xFFFFDCE3, onAccentContainer = 0xFF5C0018, danger = 0xFFB3261E,
        hold = 0xFF2F5FC4, ready = 0xFF16753F, drinkSoon = 0xFF8F5600, past = 0xFF7A4A40,
    )

    /** Clean, light: white with a deep wine accent. */
    val CleanLight = Palette(
        bg = 0xFFFFFFFF, surface = 0xFFF4F4F6, surfaceRaised = 0xFFEAEAEE, outline = 0xFFD9D9DF,
        ink = 0xFF111114, inkMuted = 0xFF5E5E69, accent = 0xFFB0123E, onAccent = 0xFFFFFFFF,
        accentContainer = 0xFFF9DCE3, onAccentContainer = 0xFF5A0A20, danger = 0xFFB3261E,
        hold = 0xFF2F5FC4, ready = 0xFF16753F, drinkSoon = 0xFF8F5600, past = 0xFF7A4A40,
    )

    /** Clean, dark. */
    val CleanDark = Palette(
        bg = 0xFF121214, surface = 0xFF1C1C1F, surfaceRaised = 0xFF26262A, outline = 0xFF38383E,
        ink = 0xFFF2F2F4, inkMuted = 0xFFA0A0AA, accent = 0xFFF07A93, onAccent = 0xFF23040C,
        accentContainer = 0xFF4F1626, onAccentContainer = 0xFFFCD9E1, danger = 0xFFFF7A6B,
        hold = 0xFF7EB0FF, ready = 0xFF52D68A, drinkSoon = 0xFFF5B04A, past = 0xFFC9A29A,
    )

    /** Colour-coded, light: warm off-white with an indigo accent. */
    val VividLight = Palette(
        bg = 0xFFF6F5F2, surface = 0xFFFFFFFF, surfaceRaised = 0xFFECEAE4, outline = 0xFFD8D4CA,
        ink = 0xFF1A1A1A, inkMuted = 0xFF5F5B53, accent = 0xFF4B3FD1, onAccent = 0xFFFFFFFF,
        accentContainer = 0xFFE3E0FF, onAccentContainer = 0xFF221A7A, danger = 0xFFB3261E,
        hold = 0xFF2457C5, ready = 0xFF0F7A47, drinkSoon = 0xFF9A5200, past = 0xFF7A4A40,
    )

    /** Colour-coded, dark. */
    val VividDark = Palette(
        bg = 0xFF0F0F14, surface = 0xFF1A1A22, surfaceRaised = 0xFF25252F, outline = 0xFF363645,
        ink = 0xFFF4F4F8, inkMuted = 0xFFA2A2B2, accent = 0xFF9C94FF, onAccent = 0xFF14104A,
        accentContainer = 0xFF2E2873, onAccentContainer = 0xFFE3E0FF, danger = 0xFFFF7A6B,
        hold = 0xFF7EB0FF, ready = 0xFF52D68A, drinkSoon = 0xFFFFB547, past = 0xFFC9A29A,
    )

    /** Wine colours for bottle silhouettes when there is no photo. */
    const val WINE_RED = 0xFFD12E4E
    const val WINE_WHITE = 0xFFF2C94C
    const val WINE_ROSE = 0xFFF58FA8
    const val WINE_ORANGE = 0xFFF08A3C
    const val WINE_UNKNOWN = 0xFF8A8A96

    /** WCAG 2.x contrast ratio between two opaque colours. */
    fun contrast(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else kotlin.math.exp(2.4 * kotlin.math.ln((c + 0.055) / 1.055))
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}

/** A colour theme the user can pick: one palette for light mode and one for dark. */
enum class ThemeStyle(val label: String, val light: Palette, val dark: Palette) {
    BOLD("Bold", Tokens.Light, Tokens.Dark),
    CLEAN("Clean", Tokens.CleanLight, Tokens.CleanDark),
    VIVID("Colour-coded", Tokens.VividLight, Tokens.VividDark);

    companion object {
        /** Unknown, missing or retired values (the 0.4 previews) fall back to Bold. */
        fun of(name: String?): ThemeStyle = entries.firstOrNull { it.name == name } ?: BOLD
    }
}

/** Typeface pairing the user can pick. Body text is IBM Plex Sans except for [SYSTEM]. */
enum class FontChoice(val label: String, val description: String) {
    MODERN("Modern", "IBM Plex Sans throughout"),
    CLASSIC("Classic", "Bodoni Moda headings"),
    EDITORIAL("Editorial", "Playfair Display headings"),
    GARAMOND("Garamond", "Cormorant Garamond headings"),
    SYSTEM("Device", "Your phone's own font");

    companion object {
        fun of(name: String?): FontChoice = entries.firstOrNull { it.name == name } ?: MODERN
    }
}

/** Setting keys for appearance. They are stored on this phone only and never synced. */
object Appearance {
    const val KEY_THEME_STYLE = "theme_style"
    const val KEY_FONT = "font_choice"
}

object Spacing {
    const val XS = 4
    const val S = 8
    const val M = 12
    const val L = 16
    const val XL = 24
    const val XXL = 32
    const val XXXL = 48
}
