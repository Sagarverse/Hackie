package com.example.rabit.ui.automation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.domain.model.HidKeyCodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class MacroAction {
    data class Type(val text: String) : MacroAction()
    data class Delay(val ms: Long) : MacroAction()
    data class KeyPress(val code: Byte, val modifier: Byte = 0) : MacroAction()
}

data class MacroTemplate(
    val name: String,
    val description: String,
    val actions: List<MacroAction>
)

class MacroOrchestratorViewModel(application: Application) : AndroidViewModel(application) {
    private val hidManager = HidDeviceManager.getInstance(application)

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting = _isExecuting.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private val _wordlistPreview = MutableStateFlow<List<String>>(emptyList())
    val wordlistPreview = _wordlistPreview.asStateFlow()

    val templates = listOf(
        MacroTemplate(
            "Quick Setup",
            "Types basic setup commands for a new terminal env",
            listOf(
                MacroAction.Type("sudo apt update && sudo apt upgrade -y\n"),
                MacroAction.Delay(500),
                MacroAction.Type("git --version\n")
            )
        ),
        MacroTemplate(
            "Hardware Info",
            "Triggers system info display via terminal",
            listOf(
                MacroAction.Type("neofetch || lscpu\n")
            )
        ),
        MacroTemplate(
            "Network Audit",
            "Runs local network connectivity check",
            listOf(
                MacroAction.Type("ping -c 4 8.8.8.8\n"),
                MacroAction.Delay(100),
                MacroAction.Type("ip route show\n")
            )
        )
    )

    fun executeMacro(template: MacroTemplate) {
        if (_isExecuting.value) return
        
        viewModelScope.launch {
            _isExecuting.value = true
            val total = template.actions.size
            template.actions.forEachIndexed { index, action ->
                _progress.value = (index + 1).toFloat() / total
                when (action) {
                    is MacroAction.Type -> hidManager.sendText(action.text)
                    is MacroAction.Delay -> delay(action.ms)
                    is MacroAction.KeyPress -> hidManager.sendKeyPress(action.code, action.modifier)
                }
                delay(50) // Small safety delay between actions
            }
            _isExecuting.value = false
            _progress.value = 0f
        }
    }

    fun loadWordlist(uri: Uri) {
        viewModelScope.launch {
            try {
                val lines = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().useLines { it.take(100).toList() }
                    } ?: emptyList()
                }
                _wordlistPreview.value = lines
            } catch (e: Exception) {
                _wordlistPreview.value = listOf("Error loading file: ${e.localizedMessage}")
            }
        }
    }

    fun stopExecution() {
        // Coroutine handles cancellation
        _isExecuting.value = false
    }
}
