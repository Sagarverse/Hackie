package com.example.rabit.ui.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MacroAction {
    abstract val id: String
    
    data class Unlock(
        override val id: String = java.util.UUID.randomUUID().toString(),
        val os: String = "Mac"
    ) : MacroAction()

    data class LaunchApp(
        override val id: String = java.util.UUID.randomUUID().toString(),
        val appName: String = "",
        val os: String = "Mac"
    ) : MacroAction()

    data class TerminalCommand(
        override val id: String = java.util.UUID.randomUUID().toString(),
        val command: String = "",
        val os: String = "Mac"
    ) : MacroAction()

    data class Delay(
        override val id: String = java.util.UUID.randomUUID().toString(),
        val milliseconds: Long = 1000L
    ) : MacroAction()

    data class RawKeystroke(
        override val id: String = java.util.UUID.randomUUID().toString(),
        val text: String = "",
        val submit: Boolean = true
    ) : MacroAction()
}

class MacroBuilderViewModel : ViewModel() {
    private val _actions = MutableStateFlow<List<MacroAction>>(emptyList())
    val actions: StateFlow<List<MacroAction>> = _actions.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    fun addAction(action: MacroAction) {
        _actions.value = _actions.value + action
    }

    fun removeAction(id: String) {
        _actions.value = _actions.value.filter { it.id != id }
    }

    fun updateAction(updatedAction: MacroAction) {
        _actions.value = _actions.value.map {
            if (it.id == updatedAction.id) updatedAction else it
        }
    }

    fun clearActions() {
        _actions.value = emptyList()
    }

    fun executeSequence(mainViewModel: com.example.rabit.ui.MainViewModel) {
        if (_isExecuting.value) return
        _isExecuting.value = true

        viewModelScope.launch {
            try {
                for (action in _actions.value) {
                    when (action) {
                        is MacroAction.Unlock -> {
                            // Basic unlock wake up
                            mainViewModel.sendKey(40.toByte(), 0) // ENTER
                            delay(500)
                        }
                        is MacroAction.LaunchApp -> {
                            if (action.appName.isNotBlank()) {
                                // Spotlight / Search based on OS
                                val mod = if (action.os == "Mac") 0x08.toByte() else 0x08.toByte() // GUI/CMD
                                mainViewModel.sendKey(44.toByte(), mod) // SPACE
                                delay(600)
                                mainViewModel.sendText(action.appName)
                                delay(400)
                                mainViewModel.sendKey(40.toByte(), 0) // ENTER
                                delay(1000)
                            }
                        }
                        is MacroAction.TerminalCommand -> {
                            if (action.command.isNotBlank()) {
                                mainViewModel.sendText(action.command)
                                delay(200)
                                mainViewModel.sendKey(40.toByte(), 0) // ENTER
                                delay(500)
                            }
                        }
                        is MacroAction.Delay -> {
                            delay(action.milliseconds)
                        }
                        is MacroAction.RawKeystroke -> {
                            if (action.text.isNotBlank()) {
                                mainViewModel.sendText(action.text)
                                delay(200)
                                if (action.submit) {
                                    mainViewModel.sendKey(40.toByte(), 0) // ENTER
                                }
                                delay(300)
                            }
                        }
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }
}
