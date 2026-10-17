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

object Spacing {
    const val XS = 4
    const val S = 8
    const val M = 12
    const val L = 16
    const val XL = 24
    const val XXL = 32
    const val XXXL = 48
}
