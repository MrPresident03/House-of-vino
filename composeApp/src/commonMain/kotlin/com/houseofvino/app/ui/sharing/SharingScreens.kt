package com.houseofvino.app.ui.sharing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houseofvino.app.AppGraph
import com.houseofvino.app.platform.CameraScanner
import com.houseofvino.app.platform.PickKind
import com.houseofvino.app.platform.ScanMode
import com.houseofvino.app.platform.ScanOutput
import com.houseofvino.app.ui.common.GraphViewModel
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.common.userMessage
import com.houseofvino.app.ui.design.ButtonKind
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvButton
import com.houseofvino.app.ui.design.HvIcons
import com.houseofvino.app.ui.design.HvTextField
import com.houseofvino.app.ui.design.HvTopBar
import com.houseofvino.app.ui.design.SectionHeader
import com.houseofvino.core.qr.QrCode
import com.houseofvino.data.sync.HouseholdInfo
import com.houseofvino.data.sync.Invite
import com.houseofvino.data.sync.PeerInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// =================================================================================================
// Sharing overview

class SharingViewModel(graph: AppGraph) : GraphViewModel(graph) {
    private val _info = MutableStateFlow<HouseholdInfo?>(null)
    val info: StateFlow<HouseholdInfo?> = _info.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()
    private val _conflicts = MutableStateFlow(0)
    val conflicts: StateFlow<Int> = _conflicts.asStateFlow()
    val busy = MutableStateFlow(false)
    val includePhotos = MutableStateFlow(true)
    private val _lastResult = MutableStateFlow<String?>(null)
    val lastResult: StateFlow<String?> = _lastResult.asStateFlow()

    init {
        observe("household_key", "peer", "collection", "setting", "sync_conflict") {
            _info.value = io { graph.household.info(collectionId) }
            _conflicts.value = io { graph.conflicts.open().size }
            _loaded.value = true
        }
    }

    fun create(name: String, onCreated: () -> Unit) = launchIo {
        io { graph.household.create(collectionId, name) }
        onCreated()
    }

    fun rename(name: String) = launchIo { io { graph.household.setDeviceName(name) } }

    fun send() = launchIo(onError = { busy.value = false; messenger.show(it.userMessage()) }) {
        busy.value = true
        val file = io { graph.syncFiles.export(collectionId, includePhotos.value, graph.platform::readPhoto) }
        busy.value = false
        graph.platform.shareText(file.fileName, com.houseofvino.data.sync.BundleCodec.MIME_TYPE, file.text)?.let { messenger.show(it) }
        _lastResult.value = when {
            file.changes == 0 && file.photos == 0 -> "Nothing new since your partner's last file. Sent anyway, so they know you're up to date."
            else -> "Sent ${plural(file.changes, "change")}${if (file.photos > 0) " and ${plural(file.photos, "photo")}" else ""} (${sizeText(file.sizeBytes)})."
        }
    }

    fun receive() = graph.platform.pickTextFile(PickKind.SYNC) { picked ->
        if (picked == null) return@pickTextFile
        launchIo(onError = { busy.value = false; _lastResult.value = it.userMessage() }) {
            busy.value = true
            val r = io { graph.syncFiles.import(collectionId, picked.text, graph.platform::savePhoto) }
            busy.value = false
            _lastResult.value = buildString {
                append(if (r.nothingNew) "Nothing new from ${r.fromName}." else "Received ${plural(r.applied, "change")} from ${r.fromName}.")
                if (r.photos > 0) append(" ${plural(r.photos, "photo")}.")
                if (r.conflicts > 0) append(" ${plural(r.conflicts, "item")} changed on both phones: review below.")
                if (r.stockConflicts > 0) append(" Stock went below zero for ${plural(r.stockConflicts, "wine")}: count those bottles.")
                if (r.incomplete) append(" Some changes need an earlier file from that phone; ask for a new one.")
            }
        }
    }

    fun removePartner(deviceId: String) = launchIo {
        io { graph.household.removePartner(collectionId, deviceId) }
        messenger.show("Removed. Your other phones need to join again with a new invite.")
    }

    fun stopSharing() = launchIo {
        io { graph.household.stopSharing(collectionId) }
        messenger.show("This phone no longer shares the cellar. Your bottles stay here.")
    }

    companion object {
        fun plural(n: Int, word: String) = "$n $word${if (n == 1) "" else "s"}"
        fun sizeText(bytes: Int) = when {
            bytes < 1_000 -> "$bytes bytes"
            bytes < 1_000_000 -> "${bytes / 1_000} KB"
            else -> "${bytes / 100_000 / 10.0} MB"
        }
    }
}

@Composable
fun SharingScreen(onBack: () -> Unit, onInvite: () -> Unit, onJoin: () -> Unit, onConflicts: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel { SharingViewModel(graph) }
    val info by vm.info.collectAsState()
    val loaded by vm.loaded.collectAsState()
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Sharing", onBack = onBack)
        if (!loaded) return@Column
        val household = info
        if (household == null) NotShared(vm, onInvite, onJoin) else Shared(household, vm, onInvite, onConflicts)
    }
}

@Composable
private fun NotShared(vm: SharingViewModel, onInvite: () -> Unit, onJoin: () -> Unit) {
    var naming by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("Our cellar") }
    Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Share one cellar with a partner", style = Hv.type.sectionTitle, color = Hv.colors.ink)
        P("Your phones keep the same list of bottles, purchases and tasting notes. There's no account and no server: changes go straight from one phone to the other.")
        P("You pair once, in person, by scanning a code. After that, tap Send changes and pass the file on with Messages, WhatsApp, email or AirDrop. Only your phones can open it.")
        if (!naming) {
            HvButton("Share this cellar", { naming = true }, Modifier.fillMaxWidth().testTag("share-cellar"))
        } else {
            HvTextField(name, { name = it.take(60) }, "Name for the shared cellar")
            HvButton("Create invite", { vm.create(name, onInvite) }, Modifier.fillMaxWidth().testTag("create-invite"), enabled = name.isNotBlank())
        }
        HvButton("Join a partner's cellar", onJoin, Modifier.fillMaxWidth().testTag("join-cellar"), kind = ButtonKind.SECONDARY)
    }
}

@Composable
private fun Shared(h: HouseholdInfo, vm: SharingViewModel, onInvite: () -> Unit, onConflicts: () -> Unit) {
    val busy by vm.busy.collectAsState()
    val photos by vm.includePhotos.collectAsState()
    val result by vm.lastResult.collectAsState()
    val conflicts by vm.conflicts.collectAsState()
    var removing by remember { mutableStateOf<PeerInfo?>(null) }
    var stopping by remember { mutableStateOf(false) }
    var deviceName by remember(h.deviceName) { mutableStateOf(h.deviceName) }
    Column(Modifier.verticalScroll(rememberScrollState()).imePadding()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(h.name, style = Hv.type.sectionTitle, color = Hv.colors.ink)
            Text("Safety code ${h.safetyCode}", style = Hv.type.caption, color = Hv.colors.inkMuted,
                modifier = Modifier.semantics { contentDescription = "Safety code ${h.safetyCode}" })
            Text("Every phone in the household shows the same code.", style = Hv.type.caption, color = Hv.colors.inkMuted)
        }
        SectionHeader("Send and receive")
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HvButton(if (busy) "Working…" else "Send changes", vm::send, Modifier.fillMaxWidth().testTag("send-changes"), enabled = !busy, icon = HvIcons.Sync)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Include photos", style = Hv.type.body, color = Hv.colors.ink, modifier = Modifier.weight(1f))
                Switch(photos, { vm.includePhotos.value = it })
            }
            HvButton("Receive changes", vm::receive, Modifier.fillMaxWidth().testTag("receive-changes"), kind = ButtonKind.SECONDARY, enabled = !busy)
            result?.let { Text(it, style = Hv.type.bodyDense, color = Hv.colors.ink, modifier = Modifier.testTag("sync-result")) }
            if (conflicts > 0) {
                HvButton("Review ${SharingViewModel.plural(conflicts, "change")} made on both phones", onConflicts, Modifier.fillMaxWidth(), kind = ButtonKind.QUIET)
            }
            Text("Send a file whenever you've added or drunk bottles, and open the one your partner sends you. Files only contain what the other phone hasn't seen yet.",
                style = Hv.type.caption, color = Hv.colors.inkMuted)
        }
        SectionHeader("Phones")
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HvTextField(deviceName, { deviceName = it.take(40) }, "This phone's name",
                trailing = { if (deviceName != h.deviceName && deviceName.isNotBlank()) TextButton(onClick = { vm.rename(deviceName) }) { Text("Save") } })
            if (h.activePeers.isEmpty()) {
                Text("No partner phone has sent a file yet.", style = Hv.type.bodyDense, color = Hv.colors.inkMuted)
            }
            h.peers.forEach { p -> PeerRow(p, onRemove = { removing = p }) }
            HvButton("Invite another phone", onInvite, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY)
        }
        HorizontalDivider(color = Hv.colors.outline, modifier = Modifier.padding(top = 16.dp))
        TextButton(onClick = { stopping = true }, modifier = Modifier.padding(8.dp)) { Text("Stop sharing on this phone", color = Hv.colors.danger) }
    }

    removing?.let { p ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove ${p.name}?") },
            text = { Text("Files from that phone will be refused from now on. What it already has stays on it. Any other phones in the household will need to join again.") },
            confirmButton = { TextButton(onClick = { vm.removePartner(p.deviceId); removing = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } },
        )
    }
    if (stopping) {
        AlertDialog(
            onDismissRequest = { stopping = false },
            title = { Text("Stop sharing?") },
            text = { Text("This phone keeps its bottles but stops sending and receiving changes. You can join again later with a new invite.") },
            confirmButton = { TextButton(onClick = { vm.stopSharing(); stopping = false }) { Text("Stop sharing") } },
            dismissButton = { TextButton(onClick = { stopping = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PeerRow(p: PeerInfo, onRemove: () -> Unit) {
    val graph = appGraph()
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(p.name, style = Hv.type.body, color = if (p.removed) Hv.colors.inkMuted else Hv.colors.ink)
            val heard = p.lastHeardAt
            Text(
                when {
                    p.removed -> "Removed"
                    heard == null -> "Hasn't sent a file yet"
                    else -> "Last file received ${ago(graph.db.now() - heard)}"
                },
                style = Hv.type.caption, color = Hv.colors.inkMuted,
            )
        }
        if (!p.removed) TextButton(onClick = onRemove) { Text("Remove") }
    }
}

private fun ago(ms: Long): String {
    val minutes = ms / 60_000
    return when {
        minutes < 2 -> "just now"
        minutes < 60 -> "$minutes minutes ago"
        minutes < 48 * 60 -> "${minutes / 60} hours ago"
        else -> "${minutes / (24 * 60)} days ago"
    }
}

@Composable private fun P(text: String) = Text(text, style = Hv.type.body, color = Hv.colors.ink)

// =================================================================================================
// Invite: QR code + copyable code

@Composable
fun InviteScreen(onBack: () -> Unit) {
    val graph = appGraph()
    val invite = remember { runCatching { graph.household.invite(graph.collectionId) }.getOrNull() }
    val info = remember { graph.household.info(graph.collectionId) }
    var showCode by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Invite a phone", onBack = onBack)
        if (invite == null || info == null) {
            Text("Share this cellar first.", style = Hv.type.body, color = Hv.colors.ink, modifier = Modifier.padding(16.dp))
            return@Column
        }
        val code = remember(invite) { invite.encode() }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("On your partner's phone, open House of Vino › Sharing › Join a partner's cellar and scan this code.",
                style = Hv.type.body, color = Hv.colors.ink)
            QrImage(code, Modifier.size(280.dp).testTag("invite-qr"))
            Text("Safety code ${info.safetyCode}", style = Hv.type.label, color = Hv.colors.ink)
            Text("The code is the key to your shared cellar. Show it in person rather than sending a screenshot.",
                style = Hv.type.caption, color = Hv.colors.inkMuted)
            if (!showCode) {
                TextButton(onClick = { showCode = true }) { Text("Can't scan? Show the invite as text") }
            } else {
                Text(code, style = Hv.type.caption, color = Hv.colors.ink, modifier = Modifier.testTag("invite-code"))
                Text("Type or paste this on the other phone. Anyone with it can read your cellar's changes.",
                    style = Hv.type.caption, color = Hv.colors.danger)
            }
        }
    }
}

/** Black on white regardless of theme, with the standard four-module quiet zone, so any camera can read it. */
@Composable
fun QrImage(text: String, modifier: Modifier = Modifier) {
    val qr = remember(text) { QrCode.encode(text, QrCode.Ecc.M) }
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(Color.White).semantics { contentDescription = "Invite QR code" }) {
        Canvas(Modifier.fillMaxSize()) {
            val quiet = 4
            val cells = qr.size + quiet * 2
            val cell = size.minDimension / cells
            for (r in 0 until qr.size) for (c in 0 until qr.size) {
                if (qr[r, c]) drawRect(Color.Black, Offset((c + quiet) * cell, (r + quiet) * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}

// =================================================================================================
// Join: scan or paste, then confirm

class JoinViewModel(graph: AppGraph) : GraphViewModel(graph) {
    val pasted = MutableStateFlow("")
    private val _invite = MutableStateFlow<Invite?>(null)
    val invite: StateFlow<Invite?> = _invite.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _myBottles = MutableStateFlow(0L)
    val myBottles: StateFlow<Long> = _myBottles.asStateFlow()

    init {
        launchIo { _myBottles.value = io { db.queryLong("SELECT coalesce(sum(quantity), 0) FROM stock_level") ?: 0L } }
    }

    fun read(text: String) {
        _error.value = null
        runCatching { Invite.decode(text) }
            .onSuccess { _invite.value = it }
            .onFailure { _error.value = it.userMessage() }
    }

    fun cancel() { _invite.value = null }

    fun join(onJoined: () -> Unit) {
        val inv = _invite.value ?: return
        launchIo(onError = { _error.value = it.userMessage(); _invite.value = null }) {
            val id = io { graph.household.join(inv, collectionId) }
            graph.useCollection(id)
            messenger.show("Joined ${inv.householdName}. Send changes so ${inv.inviterName} gets your bottles, and open the file they send you.")
            onJoined()
        }
    }
}

@Composable
fun JoinScreen(onBack: () -> Unit, onJoined: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel { JoinViewModel(graph) }
    val invite by vm.invite.collectAsState()
    val error by vm.error.collectAsState()
    val pasted by vm.pasted.collectAsState()
    val mine by vm.myBottles.collectAsState()
    var cameraProblem by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Join a partner's cellar", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("On your partner's phone, open Sharing › Invite another phone, then point this camera at the code.",
                style = Hv.type.body, color = Hv.colors.ink)
            if (invite == null && cameraProblem == null && graph.platform.scanningAvailable()) {
                Box(Modifier.fillMaxWidth().size(280.dp).clip(RoundedCornerShape(8.dp))) {
                    CameraScanner(
                        mode = ScanMode.QR,
                        onResult = { out -> if (out is ScanOutput.Barcode) vm.read(out.value) },
                        onPermissionDenied = { cameraProblem = "Camera access is off, so paste the invite instead." },
                        onUnavailable = { cameraProblem = "The camera isn't available, so paste the invite instead." },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            cameraProblem?.let { Text(it, style = Hv.type.bodyDense, color = Hv.colors.inkMuted) }
            HvTextField(pasted, { vm.pasted.value = it }, "Or paste the invite", singleLine = false, minLines = 2,
                modifier = Modifier.testTag("invite-field"))
            HvButton("Continue", { vm.read(pasted) }, Modifier.fillMaxWidth().testTag("invite-continue"), kind = ButtonKind.SECONDARY,
                enabled = pasted.isNotBlank())
            error?.let { Text(it, style = Hv.type.bodyDense, color = Hv.colors.danger) }
        }
    }
    invite?.let { inv ->
        AlertDialog(
            onDismissRequest = vm::cancel,
            title = { Text("Join ${inv.householdName}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Invited by ${inv.inviterName}. Check that both phones show safety code ${inv.key.safetyCode}.")
                    if (mine > 0) Text("The ${SharingViewModel.plural(mine.toInt(), "bottle")} on this phone will be added to the shared cellar and sent to ${inv.inviterName}. This can't be undone.")
                }
            },
            confirmButton = { TextButton(onClick = { vm.join(onJoined) }, modifier = Modifier.testTag("confirm-join")) { Text("Join") } },
            dismissButton = { TextButton(onClick = vm::cancel) { Text("Cancel") } },
        )
    }
}
