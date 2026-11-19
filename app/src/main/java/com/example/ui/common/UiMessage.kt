package com.example.ui.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val isError: Boolean = false
)

class UiMessageHost {
    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    fun show(text: String, isError: Boolean = false) {
        if (text.isBlank()) return
        _message.value = UiMessage(text = text, isError = isError)
    }

    fun clear() {
        _message.value = null
    }
}
