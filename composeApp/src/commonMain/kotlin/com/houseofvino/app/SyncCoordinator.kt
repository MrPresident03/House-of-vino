package com.houseofvino.app

import com.houseofvino.data.repo.SettingsRepository
import com.houseofvino.data.sync.AuthRequiredException
import com.houseofvino.data.sync.SyncStatus
import com.houseofvino.data.sync.TransientSyncException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** UI-facing sync state. */
data class SyncUiState(
    val configured: Boolean,
    val signedIn: Boolean,
    val offline: Boolean = false,
    val engine: SyncStatus = SyncStatus(),
)

/**
 * Decides *when* to sync: shortly after local changes (debounced), when the network returns, on
 * app start and periodically while the app is open. The engine decides *how*.
 */
class SyncCoordinator(private val graph: AppGraph) {
    private val _state = MutableStateFlow(SyncUiState(configured = graph.syncEngine != null, signedIn = graph.account?.currentUser() != null))
    val state: StateFlow<SyncUiState> = _state.asStateFlow()
    private var periodic: Job? = null
    private var started = false

    /** Idempotent: the Android activity (and so [App]) can be recreated many times per process. */
    @OptIn(FlowPreview::class)
    fun start() {
        val engine = graph.syncEngine ?: return
        if (started) return
        started = true
        graph.scope.launch {
            graph.db.changes
                .filter { tables -> tables.any { it != "setting" && it != "recent_query" && it != "sync_outbox" && it != "sync_conflict" } }
                .debounce(3_000)
                .collect { syncNow() }
        }
        graph.scope.launch { engine.status.collect { s -> _state.value = _state.value.copy(engine = s) } }
        graph.account?.let { account ->
            graph.scope.launch {
                account.user.collect { user ->
                    _state.value = _state.value.copy(signedIn = user != null)
                    if (user != null) syncNow()
                }
            }
        }
        graph.platform.onNetworkAvailable { graph.scope.launch { syncNow() } }
        periodic = graph.scope.launch {
            while (isActive) {
                delay(15 * 60_000L)
                syncNow()
            }
        }
    }

    /** Runs one sync if signed in. Safe to call often; the engine serialises runs. */
    suspend fun syncNow(): Boolean {
        val engine = graph.syncEngine ?: return false
        val account = graph.account ?: return false
        if (account.currentUser() == null) return false
        return try {
            account.bootstrap(graph.collectionId, "My cellar")
            engine.sync(graph.collectionId)
            graph.photoStorage?.let { storage ->
                graph.photos.syncFiles(storage, readLocal = graph.platform::readPhoto, writeLocal = { id, bytes -> graph.platform.savePhoto(id, bytes) })
            }
            graph.settings.put(SettingsRepository.KEY_LAST_SYNC, graph.db.now().toString())
            _state.value = _state.value.copy(offline = false)
            true
        } catch (e: TransientSyncException) {
            _state.value = _state.value.copy(offline = true)
            false
        } catch (e: AuthRequiredException) {
            _state.value = _state.value.copy(signedIn = false)
            false
        } catch (e: Exception) {
            engine.refreshStatus()
            false
        }
    }
}
