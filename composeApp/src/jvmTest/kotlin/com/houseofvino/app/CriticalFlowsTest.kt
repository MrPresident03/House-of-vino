package com.houseofvino.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.houseofvino.app.ui.design.Appearance
import com.houseofvino.core.model.LocationKind
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.WineColor
import com.houseofvino.data.repo.LocationInput
import com.houseofvino.data.repo.LotInput
import com.houseofvino.data.repo.SettingsRepository
import com.houseofvino.data.repo.VintageInput
import com.houseofvino.data.repo.WineInput
import com.houseofvino.data.repo.WineTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end UI tests of the flows a collector relies on, run headlessly on the desktop JVM
 * against a real SQLite database. Device tests (camera, pickers, share sheet) are manual; see
 * docs/CHECKLIST.md.
 */
@OptIn(ExperimentalTestApi::class)
class CriticalFlowsTest {
    private val timeout = 10_000L

    private fun ComposeUiTest.waitFor(matcher: SemanticsMatcher) =
        waitUntil(timeoutMillis = timeout) { onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeUiTest.waitForText(text: String) = waitFor(hasText(text))

    /**
     * Clicks on the UI thread. Navigation updates lifecycles, which must happen on the main
     * thread; performClick() on desktop runs the handler on the test thread.
     */
    private fun ComposeUiTest.click(node: SemanticsNodeInteraction) {
        val target = node.fetchSemanticsNode()
        runOnUiThread { target.config[SemanticsActions.OnClick].action?.invoke() }
        waitForIdle()
    }

    private fun ComposeUiTest.waitForBottleCount(n: Int) =
        waitFor(hasText(n.toString()) and hasAnyAncestor(hasTestTag("bottle-count")))

    private fun AppGraph.onboarded() = apply { settings.put(SettingsRepository.KEY_ONBOARDED, "true") }

    private fun AppGraph.totalBottles(): Long = db.queryLong("SELECT coalesce(sum(quantity), 0) FROM stock_level") ?: 0L

    @Test
    fun firstRunAddWineWithBottlesThenDrinkOne() = runComposeUiTest {
        val graph = testGraph()
        setContent { App(graph) }

        click(onNodeWithText("Start with an empty cellar").performScrollTo())
        waitForText("Your cellar is empty")
        click(onNodeWithText("Add wine"))
        waitForText("Enter details")
        click(onNodeWithText("Enter details"))

        waitFor(hasTestTag("field-producer"))
        onNodeWithTag("field-producer").performTextInput("Château Test")
        onNodeWithText("Vintage year").performTextInput("2015")
        click(onNodeWithContentDescription("Add one").performScrollTo())
        click(onNodeWithContentDescription("Add one"))
        click(onNodeWithTag("save-wine").performScrollTo())

        waitForBottleCount(3)
        assertEquals(3, graph.totalBottles())

        click(onNodeWithText("Drink"))
        waitFor(hasTestTag("stock-submit"))
        click(onNodeWithTag("stock-submit").performScrollTo())

        waitUntil(timeoutMillis = timeout) { graph.totalBottles() == 2L }
        assertEquals(1L, graph.db.queryLong("SELECT count(*) FROM movement WHERE kind = 'CONSUME'"))
    }

    @Test
    fun racksTabOpensTheGridAndPlacesABottle() = runComposeUiTest {
        val graph = testGraph().onboarded()
        val fridge = graph.locations.create(graph.collectionId, LocationInput("Wine fridge", LocationKind.FRIDGE, gridRows = 2, gridCols = 3))
        graph.cellar.addWine(
            graph.collectionId, WineTarget.NewWine(WineInput("Château Margaux", color = WineColor.RED)),
            VintageInput(VintageYear.Year(2015)), LotInput(2, locationId = fridge),
        )
        setContent { App(graph) }

        waitForText("Racks")
        click(onNodeWithText("Racks"))
        waitFor(hasTestTag("racks"))
        waitForText("0 of 6 positions filled")

        click(onNode(hasContentDescription("Row 1 position 1, empty")))
        waitForText("2015 Château Margaux")
        click(onNodeWithText("2015 Château Margaux"))
        waitForText("1 of 6 positions filled")
        assertEquals(1L, graph.db.queryLong("SELECT count(*) FROM slot WHERE lot_id IS NOT NULL"))
    }

    @Test
    fun racksTabWithoutAGridOffersToSetOneUp() = runComposeUiTest {
        val graph = testGraph().onboarded()
        setContent { App(graph) }

        waitForText("Racks")
        click(onNodeWithText("Racks"))
        waitForText("No rack layout yet")
        click(onNodeWithText("Set up a rack"))
        // The editor opens with bottle positions already switched on.
        waitForText("Positions per row")
        onNodeWithText("Name").performTextInput("Rack A")
        onNodeWithText("Rows").performTextInput("3")
        onNodeWithText("Positions per row").performTextInput("4")
        click(onNodeWithText("Save").performScrollTo())

        waitForText("0 of 12 positions filled")
    }

    @Test
    fun varietalPickerFindsAGrapeBySynonymAndFillsTheColour() = runComposeUiTest {
        val graph = testGraph().onboarded()
        setContent { App(graph) }

        waitForText("Add wine")
        click(onNodeWithText("Add wine"))
        waitForText("Enter details")
        click(onNodeWithText("Enter details"))
        waitFor(hasTestTag("field-producer"))
        onNodeWithTag("field-producer").performTextInput("Penfolds")

        click(onNodeWithText("Choose varietal").performScrollTo())
        waitFor(hasTestTag("varietal-search"))
        onNodeWithTag("varietal-search").performTextInput("shiraz")
        waitFor(hasTestTag("varietal-row-Syrah"))
        click(onNodeWithTag("varietal-row-Syrah"))
        click(onNodeWithTag("varietal-done"))

        click(onNodeWithTag("save-wine").performScrollTo())
        waitUntil(timeoutMillis = timeout) { graph.db.queryLong("SELECT count(*) FROM wine_grape WHERE grape = 'Syrah'") == 1L }
        assertEquals(1L, graph.db.queryLong("SELECT count(*) FROM wine WHERE color = 'RED'"), "colour filled from the grape")
    }

    @Test
    fun colourThemeAndFontCanBeChangedAndAreRemembered() = runComposeUiTest {
        val graph = testGraph().onboarded()
        setContent { App(graph) }

        waitForText("More")
        click(onNodeWithText("More"))
        waitForText("Settings")
        click(onNodeWithText("Settings"))
        waitForText("Colour theme")

        click(onNodeWithText("Slate").performScrollTo())
        waitUntil(timeoutMillis = timeout) { graph.settings.get(Appearance.KEY_THEME_STYLE) == "SLATE" }
        // Every pairing must load its font files and still draw the screen.
        for ((label, stored) in listOf("Editorial" to "EDITORIAL", "Garamond" to "GARAMOND", "Modern" to "MODERN", "Device" to "SYSTEM", "Classic" to "CLASSIC")) {
            click(onNodeWithText(label).performScrollTo())
            waitUntil(timeoutMillis = timeout) { graph.settings.get(Appearance.KEY_FONT) == stored }
            waitForText("Colour theme")
        }
    }

    @Test
    fun searchFindsPartialWordsAndToleratesTypos() = runComposeUiTest {
        val graph = testGraph().onboarded()
        graph.cellar.addWine(
            graph.collectionId,
            WineTarget.NewWine(WineInput("Château Margaux", color = WineColor.RED, country = "France", region = "Bordeaux", appellation = "Margaux")),
            VintageInput(VintageYear.Year(2015)), LotInput(2),
        )
        graph.cellar.addWine(
            graph.collectionId,
            WineTarget.NewWine(WineInput("Domaine Leflaive", name = "Puligny-Montrachet", color = WineColor.WHITE, country = "France", region = "Burgundy")),
            VintageInput(VintageYear.Year(2019)), LotInput(6),
        )
        setContent { App(graph) }

        waitForText("Search")
        click(onNodeWithText("Search"))
        waitFor(hasTestTag("search-field"))

        onNodeWithTag("search-field").performTextInput("pulig")
        waitFor(hasContentDescription("Leflaive", substring = true))
        assertTrue(onAllNodes(hasContentDescription("Château Margaux", substring = true)).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun misspeltSearchStillFindsTheWine() = runComposeUiTest {
        val graph = testGraph().onboarded()
        graph.cellar.addWine(
            graph.collectionId,
            WineTarget.NewWine(WineInput("Château Margaux", color = WineColor.RED, country = "France", region = "Bordeaux", appellation = "Margaux")),
            VintageInput(VintageYear.Year(2015)), LotInput(2),
        )
        setContent { App(graph) }

        waitForText("Search")
        click(onNodeWithText("Search"))
        waitFor(hasTestTag("search-field"))
        onNodeWithTag("search-field").performTextInput("margeaux")
        waitFor(hasContentDescription("Château Margaux", substring = true))
    }

    @Test
    fun cellarListShowsSeededStockWithoutLoadingEverything() = runComposeUiTest {
        val graph = testGraph().onboarded()
        repeat(120) { i ->
            graph.cellar.addWine(
                graph.collectionId,
                WineTarget.NewWine(WineInput("Producer %03d".format(i), color = WineColor.RED)),
                VintageInput(VintageYear.Year(2000 + i % 20)), LotInput(1 + i % 3),
            )
        }
        setContent { App(graph) }
        waitFor(hasTestTag("cellar-list"))
        // Lazy list: only the visible rows are composed.
        val composed = onAllNodes(hasContentDescription("Producer", substring = true)).fetchSemanticsNodes().size
        assertTrue(composed in 1..40, "composed $composed rows")
    }
}
