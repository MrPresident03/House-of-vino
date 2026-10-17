package com.houseofvino.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.houseofvino.app.AppGraph
import com.houseofvino.app.Messenger
import com.houseofvino.core.inventory.LedgerError
import com.houseofvino.data.repo.DuplicateWineException
import com.houseofvino.data.repo.InvalidStatusChangeException
import com.houseofvino.data.repo.LocationInUseException
import com.houseofvino.data.repo.StockRemainingException
import com.houseofvino.data.repo.VintageExistsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val LocalAppGraph = staticCompositionLocalOf<AppGraph> { error("AppGraph not provided") }

/** Loading → Content | Empty | Error, the four states every list screen renders. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
    data class Failed(val message: String) : Load<Nothing>
}

/** Turns exceptions into sentences for people (docs/DESIGN_SYSTEM.md, "Writing"). */
fun Throwable.userMessage(): String = when (this) {
    is LedgerError -> message ?: "That stock change isn't possible"
    is DuplicateWineException -> "You already have this wine"
    is VintageExistsException -> "This vintage already exists for the wine. Open it instead, or merge the two records."
    is StockRemainingException -> message ?: "Bottles are still in stock"
    is LocationInUseException -> message ?: "This location is in use"
    is InvalidStatusChangeException -> message ?: "That status change isn't allowed"
    is IllegalArgumentException -> message ?: "Check the highlighted fields"
    is IllegalStateException -> message ?: "Something went wrong"
    else -> message ?: "Something went wrong"
}

abstract class GraphViewModel(protected val graph: AppGraph) : ViewModel() {
    protected val db get() = graph.db
    protected val collectionId get() = graph.collectionId

    /** Runs blocking database work off the main thread. */
    protected suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    /** Launches [block]; reports failures through the messenger. */
    protected fun launchIo(onError: ((Throwable) -> Unit)? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (onError != null) onError(e) else messenger.show(e.userMessage())
            }
        }
    }

    /** Re-runs [refresh] whenever any of [tables] changes (debounced), and once now. */
    @OptIn(FlowPreview::class)
    protected fun observe(vararg tables: String, refresh: suspend () -> Unit) {
        val watched = tables.toSet()
        viewModelScope.launch { refresh() }
        viewModelScope.launch {
            db.changes.filter { changed -> watched.isEmpty() || changed.any { it in watched } }.debounce(120).collect { refresh() }
        }
    }

    protected val messenger: Messenger get() = graph.messenger
}

@Composable
fun appGraph(): AppGraph = LocalAppGraph.current
