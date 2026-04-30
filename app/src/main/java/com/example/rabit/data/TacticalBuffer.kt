package com.example.rabit.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object TacticalBuffer {
    private val _keystrokes = MutableStateFlow<List<String>>(emptyList())
    val keystrokes = _keystrokes.asStateFlow()

    fun addKeystroke(key: String) {
        _keystrokes.value = (listOf(key) + _keystrokes.value).take(500)
    }

    fun clear() {
        _keystrokes.value = emptyList()
    }
}
