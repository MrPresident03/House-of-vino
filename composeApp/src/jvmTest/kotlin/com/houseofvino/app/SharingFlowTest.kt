package com.houseofvino.app

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.houseofvino.app.platform.PickKind
import com.houseofvino.app.platform.PickedFile
import com.houseofvino.core.model.VintageYear
import com.houseofvino.data.repo.LotInput
import com.houseofvino.data.repo.SettingsRepository
import com.houseofvino.data.repo.VintageInput
import com.houseofvino.data.repo.WineInput
import com.houseofvino.data.repo.WineTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Pairing and swapping a sync file, driven through the Sharing screens. */
@OptIn(ExperimentalTestApi::class)
class SharingFlowTest {
    private val timeout = 10_000L

    private fun ComposeUiTest.waitFor(matcher: SemanticsMatcher) =
        waitUntil(timeoutMillis = timeout) { onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeUiTest.click(node: SemanticsNodeInteraction) {
        val target = node.fetchSemanticsNode()
        runOnUiThread { target.config[SemanticsActions.OnClick].action?.invoke() }
        waitForIdle()
    }

    private fun AppGraph.onboarded() = apply { settings.put(SettingsRepository.KEY_ONBOARDED, "true") }

    private fun AppGraph.bottles(): Long = db.queryLong("SELECT coalesce(sum(quantity), 0) FROM stock_level") ?: 0L

    private fun ComposeUiTest.openSharing() {
        waitFor(hasText("More"))
        click(onNodeWithText("More"))
        waitFor(hasText("Sharing"))
        click(onNodeWithText("Sharing"))
    }

    @Test
    fun sharingTheCellarShowsAnInviteCode() = runComposeUiTest {
        val graph = testGraph().onboarded()
        setContent { App(graph) }
        openSharing()
        waitFor(hasTestTag("share-cellar"))
        click(onNodeWithTag("share-cellar"))
        waitFor(hasTestTag("create-invite"))
        click(onNodeWithTag("create-invite"))
        waitFor(hasTestTag("invite-qr"))
        val info = assertNotNull(graph.household.info(graph.collectionId))
        assertEquals("Our cellar", info.name)
        waitFor(hasText("Safety code ${info.safetyCode}"))
    }

    @Test
    fun aPartnerJoinsWithThePastedInviteAndReceivesTheCellar() = runComposeUiTest {
        // Ana's phone, prepared directly: one wine, shared, and a sync file ready to send.
        val ana = testGraph().onboarded()
        ana.cellar.addWine(ana.collectionId, WineTarget.NewWine(WineInput("Château Musar")), VintageInput(VintageYear.Year(2012)), LotInput(4))
        val invite = ana.household.create(ana.collectionId, "Ana and Ben")
        val fileFromAna = ana.syncFiles.export(ana.collectionId, includePhotos = false) { null }

        // Ben's phone, driven through the UI. He already has a bottle of his own.
        val benPlatform = FakePlatform()
        val ben = testGraph(benPlatform).onboarded()
        ben.cellar.addWine(ben.collectionId, WineTarget.NewWine(WineInput("Ridge")), VintageInput(VintageYear.Year(2016)), LotInput(1))
        setContent { App(ben) }
        openSharing()
        waitFor(hasTestTag("join-cellar"))
        click(onNodeWithTag("join-cellar"))
        waitFor(hasTestTag("invite-field"))
        onNodeWithTag("invite-field").performTextInput(invite.encode())
        click(onNodeWithTag("invite-continue").performScrollTo())
        waitFor(hasText("Join Ana and Ben?"))
        click(onNodeWithTag("confirm-join"))

        waitFor(hasTestTag("receive-changes"))
        assertEquals(ana.collectionId, ben.collectionId)
        benPlatform.nextTextFile = PickedFile(fileFromAna.fileName, fileFromAna.text)
        click(onNodeWithTag("receive-changes").performScrollTo())
        waitFor(hasTestTag("sync-result"))
        assertEquals(PickKind.SYNC, benPlatform.pickedKinds.last())
        waitUntil(timeoutMillis = timeout) { ben.bottles() == 5L }

        // Ben sends his changes back; Ana ends up with both cellars.
        click(onNodeWithTag("send-changes").performScrollTo())
        waitUntil(timeoutMillis = timeout) { benPlatform.shared.isNotEmpty() }
        val (name, text) = benPlatform.shared.last()
        assertTrue(name.endsWith(".hovsync"))
        val received = ana.syncFiles.import(ana.collectionId, text) { id, _ -> "mem://$id" }
        assertTrue(received.applied > 0)
        assertEquals(5L, ana.bottles())
    }
}
