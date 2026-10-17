package com.houseofvino.app.ui.design

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** WCAG 2.x AA: 4.5:1 for text, 3:1 for large text and non-text UI (outlines, indicators). */
class DesignTokensTest {
    private val themes = mapOf("dark" to Tokens.Dark, "light" to Tokens.Light)

    private fun assertContrast(name: String, fg: Long, bg: Long, min: Double) {
        val ratio = Tokens.contrast(fg, bg)
        assertTrue(ratio >= min, "$name: ${(ratio * 100).toInt() / 100.0}:1 is below $min:1")
    }

    @Test fun contrastFormulaMatchesWcagReferencePoints() {
        assertEquals(21.0, Tokens.contrast(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, Tokens.contrast(0xFF777777, 0xFF777777), 0.0001)
        assertEquals(4.48, Tokens.contrast(0xFF777777, 0xFFFFFFFF), 0.01)
    }

    @Test fun bodyTextIsReadableOnEverySurface() = themes.forEach { (theme, p) ->
        for ((surfaceName, surface) in listOf("bg" to p.bg, "surface" to p.surface, "surfaceRaised" to p.surfaceRaised)) {
            assertContrast("$theme ink on $surfaceName", p.ink, surface, 4.5)
            assertContrast("$theme inkMuted on $surfaceName", p.inkMuted, surface, 4.5)
        }
    }

    @Test fun accentWorksAsTextAndAsAFill() = themes.forEach { (theme, p) ->
        assertContrast("$theme accent on bg", p.accent, p.bg, 4.5)
        assertContrast("$theme accent on surface", p.accent, p.surface, 4.5)
        assertContrast("$theme onAccent on accent", p.onAccent, p.accent, 4.5)
        assertContrast("$theme onAccentContainer on accentContainer", p.onAccentContainer, p.accentContainer, 4.5)
    }

    @Test fun statusColoursAreReadableAsCaptionText() = themes.forEach { (theme, p) ->
        for ((name, c) in listOf("danger" to p.danger, "hold" to p.hold, "ready" to p.ready, "drinkSoon" to p.drinkSoon, "past" to p.past)) {
            assertContrast("$theme $name on bg", c, p.bg, 4.5)
            assertContrast("$theme $name on surface", c, p.surface, 4.5)
        }
    }

    @Test fun outlinesAreDecorativeOnly() = themes.forEach { (theme, p) ->
        // Outlines separate rows; controls get their boundary from fills or text, so 3:1 isn't
        // required here. Guard against them disappearing entirely.
        assertTrue(Tokens.contrast(p.outline, p.surface) > 1.1, "$theme outline is invisible")
    }
}
