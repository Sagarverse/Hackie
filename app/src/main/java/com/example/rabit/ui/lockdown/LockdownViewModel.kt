package com.example.rabit.ui.lockdown

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.repository.KeyboardRepositoryImpl
import com.example.rabit.domain.repository.KeyboardRepository
import com.example.rabit.data.bluetooth.HidDeviceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LockdownViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: KeyboardRepository = KeyboardRepositoryImpl(application)
    
    private val _isLockdownActive = MutableStateFlow(false)
    val isLockdownActive: StateFlow<Boolean> = _isLockdownActive.asStateFlow()

    private var lockdownJob: Job? = null
    
    val connectionState = repository.connectionState

    fun toggleLockdown() {
        if (_isLockdownActive.value) {
            stopLockdown()
        } else {
            startLockdown()
        }
    }

    private fun startLockdown() {
        _isLockdownActive.value = true
        lockdownJob?.cancel()
        lockdownJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                // Input Saturation: Rapidly oscillate mouse reports
                // This makes the cursor jump in tiny jittering patterns, 
                // effectively freezing functional precision for the user.
                repository.sendMouseMove(2f, 2f)
                delay(10)
                repository.sendMouseMove(-2f, -2f)
                delay(10)
                
                // Periodically pulse MODIFIER_LEFT_GUI (Cmd/Win) to disrupt focus
                if (System.currentTimeMillis() % 1000 < 50) {
                    repository.setModifier(0x08.toByte(), true) // GUI
                    delay(20)
                    repository.setModifier(0x08.toByte(), false)
                }
            }
        }
    }

    private fun stopLockdown() {
        _isLockdownActive.value = false
        lockdownJob?.cancel()
        lockdownJob = null
        // Ensure neutral state
        repository.sendMouseMove(0f, 0f)
        repository.setModifier(0, false)
    }

    fun triggerMacLock() {
        repository.executeKeyCombo("CMD+CTRL+Q")
    }

    fun triggerWindowsLock() {
        repository.executeKeyCombo("GUI+L")
    }

    override fun onCleared() {
        super.onCleared()
        stopLockdown()
    }
}
