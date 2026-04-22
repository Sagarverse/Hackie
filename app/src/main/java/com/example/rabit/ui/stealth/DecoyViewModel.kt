package com.example.rabit.ui.stealth

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DecoyViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
    
    private val _isStealthMode = MutableStateFlow(prefs.getBoolean("stealth_mode_active", false))
    val isStealthMode = _isStealthMode.asStateFlow()

    private val _isSessionUnlocked = MutableStateFlow(false)
    val isSessionUnlocked = _isSessionUnlocked.asStateFlow()

    private val _calculatorDisplay = MutableStateFlow("0")
    val calculatorDisplay = _calculatorDisplay.asStateFlow()

    private var secretBuffer = ""

    fun onNumberClick(num: String) {
        if (_calculatorDisplay.value == "0") {
            _calculatorDisplay.value = num
        } else {
            _calculatorDisplay.value += num
        }
        secretBuffer += num
    }

    fun onOperatorClick(op: String) {
        if (op == "=") {
            checkSecretSequence()
        }
        _calculatorDisplay.value = "0"
    }

    fun onClear() {
        _calculatorDisplay.value = "0"
        secretBuffer = ""
    }

    private fun checkSecretSequence() {
        // SECRET UNLOCK CODE: 1337
        if (_calculatorDisplay.value == "1337") {
            unlockHackie()
        }
        secretBuffer = ""
    }

    fun unlockHackie() {
        _isSessionUnlocked.value = true
    }

    fun toggleStealthIcon(enable: Boolean) {
        val context = getApplication<Application>()
        val packageManager = context.packageManager
        
        val hackieComponent = ComponentName(context, "com.example.rabit.MainActivity")
        val decoyComponent = ComponentName(context, "com.example.rabit.DecoyActivity") // Activity Alias

        if (enable) {
            packageManager.setComponentEnabledSetting(hackieComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            packageManager.setComponentEnabledSetting(decoyComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        } else {
            packageManager.setComponentEnabledSetting(decoyComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            packageManager.setComponentEnabledSetting(hackieComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        }
        
        prefs.edit().putBoolean("stealth_mode_active", enable).apply()
        _isStealthMode.value = enable
    }
}
