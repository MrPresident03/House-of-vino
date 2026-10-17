package com.houseofvino.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.houseofvino.core.model.VintageYear
import com.houseofvino.core.model.WineColor
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
        waitUntil(timeout) { onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeUiTest.waitForText(text: String) = waitFor(hasText(text))

    private fun ComposeUiTest.waitForBottleCount(n: Int) =
        waitFor(hasText(n.toString()) and hasAnyAncestor(hasTestTag("bottle-count")))

    private fun AppGraph.onboarded() = apply { settings.put(SettingsRepository.KEY_ONBOARDED, "true") }

    private fun AppGraph.totalBottles(): Long = db.queryLong("SELECT coalesce(sum(quantity), 0) FROM stock_level") ?: 0L

    @Test
    fun firstRunAddWineWithBottlesThenDrinkOneWithANote() = runComposeUiTest {
        val graph = testGraph()
        setContent { App(graph) }

        onNodeWithText("Start with an empty cellar").performScrollTo().performClick()
        waitForText("Your cellar is empty")
        onNodeWithText("Add wine").performClick()
        waitForText("Enter details")
        onNodeWithText("Enter details").performClick()

        waitFor(hasTestTag("field-producer"))
        onNodeWithTag("field-producer").performTextInput("Château Test")
        onNodeWithText("Vintage year").performTextInput("2015")
        onNodeWithContentDescription("Add one").performScrollTo().performClick()
        onNodeWithContentDescription("Add one").performClick()
        onNodeWithTag("save-wine").performScrollTo().performClick()

        waitForBottleCount(3)
        assertEquals(3, graph.totalBottles())

        onNodeWithText("Drink").performClick()
        waitFor(hasTestTag("stock-submit"))
        onNodeWithTag("stock-submit").performScrollTo().performClick() // "Continue to note"
        waitFor(hasTestTag("save-tasting"))
        onNodeWithTag("tasting-notes").performTextInput("Cedar, graphite, still tight.")
        onNodeWithTag("save-tasting").performScrollTo().performClick()

        waitUntil(timeout) { graph.totalBottles() == 2L }
        assertEquals(1L, graph.db.queryLong("SELECT count(*) FROM tasting"))
        // The note and the departure are linked both ways, so history survives either being edited.
        assertEquals(1L, graph.db.queryLong("SELECT count(*) FROM movement m JOIN tasting t ON t.movement_id = m.id WHERE m.kind = 'CONSUME'"))
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
        onNodeWithText("Search").performClick()
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
        onNodeWithText("Search").performClick()
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
