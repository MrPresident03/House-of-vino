package com.houseofvino.app

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** A transient message for the root snackbar host. Actions are named after what they do ("Undo"). */
data class UiMessage(val text: String, val actionLabel: String? = null, val action: (() -> Unit)? = null)

class Messenger {
    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    fun show(text: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
        _messages.tryEmit(UiMessage(text, actionLabel, action))
    }
}
