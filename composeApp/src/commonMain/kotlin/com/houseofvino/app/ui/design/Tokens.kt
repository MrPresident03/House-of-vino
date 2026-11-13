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
    val Dark = Palette(
        bg = 0xFF1B1819, surface = 0xFF231F21, surfaceRaised = 0xFF2C2729, outline = 0xFF3D3639,
        ink = 0xFFEEE7E1, inkMuted = 0xFFA99F9A, accent = 0xFFD47A8A, onAccent = 0xFF1B1819,
        accentContainer = 0xFF4A1622, onAccentContainer = 0xFFF3D8DD, danger = 0xFFE07A6E,
        hold = 0xFF8C9BB0, ready = 0xFF8FB089, drinkSoon = 0xFFD2A55A, past = 0xFFB88A80,
    )

    val Light = Palette(
        bg = 0xFFF2EBDD, surface = 0xFFFAF6EE, surfaceRaised = 0xFFEFE6D6, outline = 0xFFD8CCBA,
        ink = 0xFF2A2023, inkMuted = 0xFF6E625F, accent = 0xFF7B1E34, onAccent = 0xFFFFFFFF,
        accentContainer = 0xFFF0D9DC, onAccentContainer = 0xFF4A1622, danger = 0xFFA3352A,
        hold = 0xFF4F5F75, ready = 0xFF3F6B3A, drinkSoon = 0xFF8A5A12, past = 0xFF7A4A40,
    )

    // Extra colour themes (Settings › Appearance). Each one passes the same contrast rules as the
    // original pair; DesignTokensTest checks every palette listed in ThemeStyle.

    val ChampagneLight = Palette(
        bg = 0xFFF6F1E4, surface = 0xFFFDFAF2, surfaceRaised = 0xFFEFE7D3, outline = 0xFFDCCFB0,
        ink = 0xFF26221A, inkMuted = 0xFF675F4F, accent = 0xFF7A5A12, onAccent = 0xFFFFFFFF,
        accentContainer = 0xFFF2E4BE, onAccentContainer = 0xFF3F2E06,
        danger = 0xFFA3352A,
        hold = 0xFF4F5F75, ready = 0xFF3F6B3A, drinkSoon = 0xFF8A5A12, past = 0xFF7A4A40,
    )

    val ChampagneDark = Palette(
        bg = 0xFF181612, surface = 0xFF211E18, surfaceRaised = 0xFF2B271F, outline = 0xFF3C3629,
        ink = 0xFFEFE8D8, inkMuted = 0xFFABA18C, accent = 0xFFD9B45F, onAccent = 0xFF1A1608,
        accentContainer = 0xFF4A3A12, onAccentContainer = 0xFFF5E6BB,
        danger = 0xFFE07A6E,
        hold = 0xFF8C9BB0, ready = 0xFF8FB089, drinkSoon = 0xFFD2A55A, past = 0xFFB88A80,
    )

    val VineyardLight = Palette(
        bg = 0xFFEDF0E6, surface = 0xFFF8FAF3, surfaceRaised = 0xFFE3E8D8, outline = 0xFFCBD3BC,
        ink = 0xFF1E241C, inkMuted = 0xFF586253, accent = 0xFF2F5D34, onAccent = 0xFFFFFFFF,
        accentContainer = 0xFFD7E6D2, onAccentContainer = 0xFF173019,
        danger = 0xFFA3352A,
        hold = 0xFF4F5F75, ready = 0xFF3F6B3A, drinkSoon = 0xFF8A5A12, past = 0xFF7A4A40,
    )

    val VineyardDark = Palette(
        bg = 0xFF141813, surface = 0xFF1B211A, surfaceRaised = 0xFF242B22, outline = 0xFF344031,
        ink = 0xFFE4EADF, inkMuted = 0xFF9DA898, accent = 0xFF8FC28A, onAccent = 0xFF10200F,
        accentContainer = 0xFF1F3E22, onAccentContainer = 0xFFD5EBD0,
        danger = 0xFFE07A6E,
        hold = 0xFF8C9BB0, ready = 0xFF8FB089, drinkSoon = 0xFFD2A55A, past = 0xFFB88A80,
    )

    val SlateLight = Palette(
        bg = 0xFFECEFF2, surface = 0xFFF8F9FB, surfaceRaised = 0xFFE1E6EB, outline = 0xFFC9D1D9,
        ink = 0xFF1C2229, inkMuted = 0xFF55606B, accent = 0xFF2B4C7E, onAccent = 0xFFFFFFFF,
        accentContainer = 0xFFD6E2F3, onAccentContainer = 0xFF14294A,
        danger = 0xFFA3352A,
        hold = 0xFF4F5F75, ready = 0xFF3F6B3A, drinkSoon = 0xFF8A5A12, past = 0xFF7A4A40,
    )

    val SlateDark = Palette(
        bg = 0xFF14171B, surface = 0xFF1B1F25, surfaceRaised = 0xFF252A31, outline = 0xFF363D47,
        ink = 0xFFE4E8ED, inkMuted = 0xFF9BA5B1, accent = 0xFF8DB4EA, onAccent = 0xFF0E1A2C,
        accentContainer = 0xFF1E3558, onAccentContainer = 0xFFD6E4F8,
        danger = 0xFFE07A6E,
        hold = 0xFF8C9BB0, ready = 0xFF8FB089, drinkSoon = 0xFFD2A55A, past = 0xFFB88A80,
    )

    val RoseLight = Palette(
        bg = 0xFFF8ECEA, surface = 0xFFFEF8F6, surfaceRaised = 0xFFF2E0DD, outline = 0xFFE2C9C5,
        ink = 0xFF2B1F20, inkMuted = 0xFF6E5C5B, accent = 0xFFA23B55, onAccent = 0xFFFFFFFF,
        accentContainer = 0xFFF7D6DB, onAccentContainer = 0xFF4F1424,
        danger = 0xFFA3352A,
        hold = 0xFF4F5F75, ready = 0xFF3F6B3A, drinkSoon = 0xFF8A5A12, past = 0xFF7A4A40,
    )

    val RoseDark = Palette(
        bg = 0xFF1C1617, surface = 0xFF251D1E, surfaceRaised = 0xFF2F2526, outline = 0xFF423436,
        ink = 0xFFF1E6E4, inkMuted = 0xFFB0A09E, accent = 0xFFEE9BAE, onAccent = 0xFF2A0D15,
        accentContainer = 0xFF5A1F30, onAccentContainer = 0xFFFBDDE3,
        danger = 0xFFE07A6E,
        hold = 0xFF8C9BB0, ready = 0xFF8FB089, drinkSoon = 0xFFD2A55A, past = 0xFFB88A80,
    )

    /** Wine colours for bottle silhouettes when there is no photo. */
    const val WINE_RED = 0xFF5E1A28
    const val WINE_WHITE = 0xFFD9C98E
    const val WINE_ROSE = 0xFFE2A1A0
    const val WINE_ORANGE = 0xFFC9803F
    const val WINE_UNKNOWN = 0xFF6B6466

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
    CLARET("Claret", Tokens.Light, Tokens.Dark),
    CHAMPAGNE("Champagne", Tokens.ChampagneLight, Tokens.ChampagneDark),
    VINEYARD("Vineyard", Tokens.VineyardLight, Tokens.VineyardDark),
    SLATE("Slate", Tokens.SlateLight, Tokens.SlateDark),
    ROSE("Rosé", Tokens.RoseLight, Tokens.RoseDark);

    companion object {
        /** Unknown or missing values fall back to the original theme. */
        fun of(name: String?): ThemeStyle = entries.firstOrNull { it.name == name } ?: CLARET
    }
}

/** Typeface pairing the user can pick. Body text is IBM Plex Sans except for [SYSTEM]. */
enum class FontChoice(val label: String, val description: String) {
    CLASSIC("Classic", "Bodoni Moda headings"),
    EDITORIAL("Editorial", "Playfair Display headings"),
    GARAMOND("Garamond", "Cormorant Garamond headings"),
    MODERN("Modern", "IBM Plex Sans throughout"),
    SYSTEM("Device", "Your phone's own font");

    companion object {
        fun of(name: String?): FontChoice = entries.firstOrNull { it.name == name } ?: CLASSIC
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
