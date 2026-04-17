package com.example.rabit.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.airplay.AirPlayStateBus
import com.example.rabit.data.airplay.AlacFrameDecoder
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.data.bluetooth.HidService
import com.example.rabit.data.gemini.LocalLlmManager
import com.example.rabit.data.repository.KeyboardRepositoryImpl
import com.example.rabit.data.secure.SecureStorage
import com.example.rabit.data.sensors.GyroscopeAirMouse
import com.example.rabit.data.sensors.SpatialPointerManager
import com.example.rabit.data.voice.VoiceAssistantManager
import com.example.rabit.domain.model.*
import com.example.rabit.domain.repository.KeyboardRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import android.content.Intent
import android.bluetooth.BluetoothAdapter
import com.example.rabit.data.voice.VoiceState

class MainViewModel(application: Application) : AndroidViewModel(application) {
    enum class SystemShortcut { MUTE, PLAY_PAUSE, NEXT, PREV, VOL_UP, VOL_DOWN, LOCK_SCREEN }

    private val repository: KeyboardRepository = KeyboardRepositoryImpl(application)
    private val localLlmManager = LocalLlmManager(application)
    private val spatialPointerManager = SpatialPointerManager(application)
    private val gyroAirMouse = GyroscopeAirMouse(application)
    private val voiceAssistantManager = VoiceAssistantManager(application)
    private val secureStorage = SecureStorage(application)
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)

    // HID & Connection State
    val connectionState: StateFlow<HidDeviceManager.ConnectionState> = repository.connectionState
    val scannedDevices: StateFlow<Set<BluetoothDevice>> = repository.scannedDevices
    val isScanning: StateFlow<Boolean> = repository.isScanning
    val isTextPushing = repository.isTextPushing
    val knownWorkstations = repository.knownWorkstations
    val activeModifiers: StateFlow<Byte> = MutableStateFlow(0.toByte()).asStateFlow()
    val savedDevices = repository.knownWorkstations
    val activeApp: StateFlow<String> = MutableStateFlow("").asStateFlow()

    // Settings & Configuration
    private val _vibrationEnabled = MutableStateFlow(prefs.getBoolean("vibration_enabled", true))
    val vibrationEnabled = _vibrationEnabled.asStateFlow()

    private val _hapticPreset = MutableStateFlow(prefs.getString("haptic_preset", "Mechanical") ?: "Mechanical")
    val hapticPreset = _hapticPreset.asStateFlow()

    private val _trackpadSensitivity = MutableStateFlow(prefs.getFloat("trackpad_sensitivity", 1.5f))
    val trackpadSensitivity = _trackpadSensitivity.asStateFlow()

    private val _airMouseEnabled = MutableStateFlow(false)
    val airMouseEnabled = _airMouseEnabled.asStateFlow()

    private val _airMouseSensitivity = MutableStateFlow(prefs.getFloat("air_mouse_sensitivity", 18f))
    val airMouseSensitivity = _airMouseSensitivity.asStateFlow()

    private val _typingSpeed = MutableStateFlow(prefs.getString("typing_speed", "Normal") ?: "Normal")
    val typingSpeed = _typingSpeed.asStateFlow()

    private val _autoReconnectEnabled = MutableStateFlow(prefs.getBoolean("auto_reconnect_enabled", false))
    val autoReconnectEnabled = _autoReconnectEnabled.asStateFlow()

    // Feature visibility toggles
    private val _featureWebBridgeVisible = MutableStateFlow(prefs.getBoolean("feature_web_bridge_visible", true))
    val featureWebBridgeVisible = _featureWebBridgeVisible.asStateFlow()
    private val _featureAutomationVisible = MutableStateFlow(prefs.getBoolean("feature_automation_visible", true))
    val featureAutomationVisible = _featureAutomationVisible.asStateFlow()
    private val _featureAssistantVisible = MutableStateFlow(prefs.getBoolean("feature_assistant_visible", true))
    val featureAssistantVisible = _featureAssistantVisible.asStateFlow()
    private val _featureSnippetsVisible = MutableStateFlow(prefs.getBoolean("feature_snippets_visible", true))
    val featureSnippetsVisible = _featureSnippetsVisible.asStateFlow()
    private val _featureWakeOnLanVisible = MutableStateFlow(prefs.getBoolean("feature_wake_on_lan_visible", true))
    val featureWakeOnLanVisible = _featureWakeOnLanVisible.asStateFlow()
    private val _featureSshTerminalVisible = MutableStateFlow(prefs.getBoolean("feature_ssh_terminal_visible", true))
    val featureSshTerminalVisible = _featureSshTerminalVisible.asStateFlow()

    // AirPlay State (for Global UI)
    private val _airPlayStatus = MutableStateFlow("Idle")
    val airPlayStatus = _airPlayStatus.asStateFlow()
    private val _airPlayStatusLog = MutableStateFlow<List<String>>(emptyList())
    val airPlayStatusLog = _airPlayStatusLog.asStateFlow()
    private val _airPlayHandshakeStage = MutableStateFlow("IDLE")
    val airPlayHandshakeStage = _airPlayHandshakeStage.asStateFlow()
    private val _airPlayAlacCapability = MutableStateFlow("UNKNOWN")
    val airPlayAlacCapability = _airPlayAlacCapability.asStateFlow()

    // Biometrics & Security
    private val _biometricRequests = MutableSharedFlow<CompletableDeferred<Boolean>>()
    val biometricRequests = _biometricRequests.asSharedFlow()
    private val _biometricLockEnabled = MutableStateFlow(prefs.getBoolean("biometric_lock_enabled", false))
    val biometricLockEnabled = _biometricLockEnabled.asStateFlow()
    
    // Voice Assistant State
    val voiceState: StateFlow<VoiceState> = voiceAssistantManager.state
    val voiceResult: StateFlow<String> = voiceAssistantManager.result
    
    // Proximity State
    private val _proximityLiveRssi = MutableStateFlow(-120)
    val proximityLiveRssi = _proximityLiveRssi.asStateFlow()
    
    private val _proximityLiveDistanceMeters = MutableStateFlow(0f)
    val proximityLiveDistanceMeters = _proximityLiveDistanceMeters.asStateFlow()

    private val _proximityAutoUnlockEnabled = MutableStateFlow(prefs.getBoolean("proximity_auto_unlock_enabled", false))
    val proximityAutoUnlockEnabled = _proximityAutoUnlockEnabled.asStateFlow()

    private val _proximityNearRssi = MutableStateFlow(prefs.getInt("proximity_near_rssi", -65))
    val proximityNearRssi = _proximityNearRssi.asStateFlow()

    private val _proximityFarRssi = MutableStateFlow(prefs.getInt("proximity_far_rssi", -85))
    val proximityFarRssi = _proximityFarRssi.asStateFlow()

    private val _proximityCooldownSec = MutableStateFlow(prefs.getInt("proximity_cooldown_sec", 10))
    val proximityCooldownSec = _proximityCooldownSec.asStateFlow()

    private val _proximityRequirePhoneUnlock = MutableStateFlow(prefs.getBoolean("proximity_require_phone_unlock", true))
    val proximityRequirePhoneUnlock = _proximityRequirePhoneUnlock.asStateFlow()

    private val _proximityTargetAddress = MutableStateFlow(prefs.getString("proximity_target_address", "") ?: "")
    val proximityTargetAddress = _proximityTargetAddress.asStateFlow()

    private val _proximityUnlockArmed = MutableStateFlow(true)
    val proximityUnlockArmed = _proximityUnlockArmed.asStateFlow()

    private val _proximityMacLockStateGuess = MutableStateFlow("UNKNOWN")
    val proximityMacLockStateGuess = _proximityMacLockStateGuess.asStateFlow()

    private val _notificationSyncEnabled = MutableStateFlow(prefs.getBoolean("notification_sync_enabled", false))
    val notificationSyncEnabled = _notificationSyncEnabled.asStateFlow()

    private val _autoPushEnabled = MutableStateFlow(prefs.getBoolean("auto_push_enabled", false))
    val autoPushEnabled = _autoPushEnabled.asStateFlow()

    private val _biometricMacAutofillEnabled = MutableStateFlow(prefs.getBoolean("biometric_mac_autofill_enabled", false))
    val biometricMacAutofillEnabled = _biometricMacAutofillEnabled.asStateFlow()
    
    private val _macAutofillPreEnter = MutableStateFlow(prefs.getBoolean("mac_autofill_pre_enter", false))
    val macAutofillPreEnter = _macAutofillPreEnter.asStateFlow()
    
    private val _macAutofillPostEnter = MutableStateFlow(prefs.getBoolean("mac_autofill_post_enter", false))
    val macAutofillPostEnter = _macAutofillPostEnter.asStateFlow()

    private val _stealthModeEnabled = MutableStateFlow(prefs.getBoolean("stealth_mode_enabled", false))
    val stealthModeEnabled = _stealthModeEnabled.asStateFlow()

    private val _shakeToDisconnectEnabled = MutableStateFlow(prefs.getBoolean("shake_to_disconnect_enabled", false))
    val shakeToDisconnectEnabled = _shakeToDisconnectEnabled.asStateFlow()

    private val _macPassword = MutableStateFlow(secureStorage.getMacPassword() ?: "")
    val macPassword = _macPassword.asStateFlow()

    private val _ttsPitch = MutableStateFlow(prefs.getFloat("tts_pitch", 1.0f))
    val ttsPitch = _ttsPitch.asStateFlow()

    private val _ttsSpeechRate = MutableStateFlow(prefs.getFloat("tts_speech_rate", 1.0f))
    val ttsSpeechRate = _ttsSpeechRate.asStateFlow()

    private val _featureShortcutsVisible = MutableStateFlow(prefs.getBoolean("feature_shortcuts_visible", true))
    val featureShortcutsVisible = _featureShortcutsVisible.asStateFlow()

    // Smart Macro Genie State
    sealed class GenieState {
        object Idle : GenieState()
        object Thinking : GenieState()
        data class Executing(val currentStep: String, val progress: Float) : GenieState()
        data class Success(val macroName: String) : GenieState()
        data class Error(val message: String) : GenieState()
    }
    private val _genieState = MutableStateFlow<GenieState>(GenieState.Idle)
    val genieState = _genieState.asStateFlow()

    // Text Push pausing
    private val _isPushPaused = MutableStateFlow(false)
    val isPushPaused = _isPushPaused.asStateFlow()

    // Compatibility Stubs for Migration
    val isHelperConnected = MutableStateFlow(false).asStateFlow()
    val helperDeviceName = MutableStateFlow("").asStateFlow()
    val helperDeviceMac = MutableStateFlow("").asStateFlow()
    val helperBaseUrl = MutableStateFlow("").asStateFlow()
    val helperDeviceIp = MutableStateFlow("").asStateFlow()
    val p2pStatus = MutableStateFlow("Disconnected").asStateFlow()
    val helperConnectionStatus = MutableStateFlow("Ready").asStateFlow()
    val helperTransferEvents = MutableStateFlow<List<String>>(emptyList()).asStateFlow()
    
    val nowPlayingTitle = MutableStateFlow("").asStateFlow()
    val nowPlayingArtist = MutableStateFlow("").asStateFlow()
    val nowPlayingAlbum = MutableStateFlow("").asStateFlow()
    val nowPlayingArtworkBase64 = MutableStateFlow<String?>(null).asStateFlow()

    // AirPlay Stubs
    val airPlayPacketStats = MutableStateFlow("0 pkt / 0 ms").asStateFlow()
    val airPlayAutoFallbackEnabled = MutableStateFlow(true).asStateFlow()
    val airPlayReceiverEnabled = MutableStateFlow(false).asStateFlow()
    val airPlayNativeReadiness = MutableStateFlow("Uninitialized").asStateFlow()
    val airPlayLastRtspMethod = MutableStateFlow("N/A").asStateFlow()
    val airPlayServerPorts = MutableStateFlow("N/A").asStateFlow()
    val airPlayClientPorts = MutableStateFlow("N/A").asStateFlow()
    val airPlayEncryptionEnabled = MutableStateFlow(false).asStateFlow()
    val airPlayAudioLatency = MutableStateFlow(0L).asStateFlow()
    val airPlayBufferStatus = MutableStateFlow("Empty").asStateFlow()

    private var proximityTelemetryJob: Job? = null

    // HID Actions (DuckyScript, etc)
    private var mouseJigglerJob: Job? = null
    private val _isMouseJigglerEnabled = MutableStateFlow(prefs.getBoolean("mouse_jiggler_enabled", false))
    val isMouseJigglerEnabled = _isMouseJigglerEnabled.asStateFlow()
    
    private val _isAirMouseCalibrating = MutableStateFlow(false)
    val isAirMouseCalibrating = _isAirMouseCalibrating.asStateFlow()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { shared, key ->
        when (key) {
            "auto_reconnect_enabled" -> _autoReconnectEnabled.value = shared.getBoolean(key, false)
            "typing_speed" -> {
                val speed = shared.getString(key, "Normal") ?: "Normal"
                _typingSpeed.value = speed
                updateRepositorySpeed(speed)
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        updateRepositorySpeed(_typingSpeed.value)
        startProximityTelemetryRefresh()
        
        // Listen to AirPlay State Bus
        viewModelScope.launch {
            AirPlayStateBus.status.collect { status ->
                _airPlayStatus.value = status
                val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
                _airPlayStatusLog.value = (listOf("[$ts] $status") + _airPlayStatusLog.value).take(24)
                _airPlayHandshakeStage.value = mapAirPlayStage(status)
            }
        }
        
        _airPlayAlacCapability.value = AlacFrameDecoder.capabilityLabel()

        // Handle Pointer Updates
        spatialPointerManager.onPointerUpdate = { dx, dy ->
            if (_airMouseEnabled.value) {
                repository.sendMouseMove(dx, dy)
            }
        }

        // Connection Auto-Save
        viewModelScope.launch {
            connectionState.collect { state ->
                updateMouseJiggler()
                if (state is HidDeviceManager.ConnectionState.Connected) {
                    saveDevice(state.deviceName, "") // Simplify address saving
                }
            }
        }
    }

    // Core Logic Methods
    fun startScanning() = repository.startScanning()
    fun stopScanning() = repository.stopScanning()
    fun connectToDevice(device: BluetoothDevice) = repository.connect(device)
    fun connect(device: BluetoothDevice) = repository.connect(device)
    fun disconnectKeyboard() = repository.disconnect()

    fun sendText(text: String) = repository.sendText(text)
    fun sendKey(keyCode: Byte, modifiers: Byte = 0) = repository.sendKey(keyCode, modifiers)
    fun sendMouseMove(dx: Float, dy: Float, buttons: Int = 0, scroll: Int = 0) {
        repository.sendMouseMove(dx, dy, buttons, scroll)
    }

    fun toggleModifier(modifier: Byte) = repository.setModifier(modifier, true)

    // Voice Actions
    fun startVoiceRecognition() = voiceAssistantManager.startListening()
    fun stopVoiceRecognition() = voiceAssistantManager.stopListening()
    fun resetVoiceState() = voiceAssistantManager.reset()

    fun setTypingSpeed(speed: String) {
        _typingSpeed.value = speed
        prefs.edit().putString("typing_speed", speed).apply()
        updateRepositorySpeed(speed)
    }

    private fun updateRepositorySpeed(speed: String) {
        val delay = when(speed) {
            "Slow" -> 180L
            "Fast" -> 60L
            "Super Fast" -> 20L
            else -> 120L
        }
        HidDeviceManager.getInstance(getApplication()).typingDelay = delay
    }

    fun setHapticPreset(preset: String) {
        _hapticPreset.value = preset
        prefs.edit().putString("haptic_preset", preset).apply()
        performHapticFeedback(preset)
    }

    private fun performHapticFeedback(preset: String) {
        if (!_vibrationEnabled.value) return
        val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            val duration = when(preset) {
                "Soft" -> 15L
                "Sharp" -> 40L
                else -> 25L
            }
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun setAirMouseEnabled(enabled: Boolean) {
        _airMouseEnabled.value = enabled
        if (enabled) {
            spatialPointerManager.start()
            gyroAirMouse.start()
        } else {
            spatialPointerManager.stop()
            gyroAirMouse.stop()
        }
    }
    
    fun setAirMouseSensitivity(value: Float) {
        _airMouseSensitivity.value = value
        prefs.edit().putFloat("air_mouse_sensitivity", value).apply()
    }
    
    fun setAirMouseCalibrating(value: Boolean) {
        _isAirMouseCalibrating.value = value
        if (value) {
            viewModelScope.launch {
                delay(2000)
                _isAirMouseCalibrating.value = false
            }
        }
    }

    fun executeDuckyScript(script: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val lines = script.lines().filter { it.isNotBlank() }
            for (line in lines) {
                val parts = line.split(" ", limit = 2)
                when (parts[0].uppercase()) {
                    "STRING" -> repository.sendText(parts.getOrNull(1) ?: "")
                    "DELAY" -> delay(parts.getOrNull(1)?.toLongOrNull() ?: 500)
                    "ENTER" -> repository.sendKey(HidKeyCodes.KEY_ENTER)
                }
            }
        }
    }

    private fun startProximityTelemetryRefresh() {
        proximityTelemetryJob?.cancel()
        proximityTelemetryJob = viewModelScope.launch {
            while (isActive) {
                val rssi = prefs.getInt("proximity_live_rssi", -120)
                _proximityLiveRssi.value = rssi
                // RSSI to Meters: d = 10^((MeasuredPower - RSSI) / (10 * N))
                // MeasuredPower approx -69, N approx 2.0
                if (rssi > -100) {
                   _proximityLiveDistanceMeters.value = 10.0.pow((-69 - rssi) / (10 * 2.0)).toFloat()
                } else {
                   _proximityLiveDistanceMeters.value = 0f
                }
                delay(2000)
            }
        }
    }

    private fun mapAirPlayStage(status: String): String {
        return when {
            status.contains("Idle", ignoreCase = true) -> "IDLE"
            status.contains("Streaming", ignoreCase = true) -> "STREAMING"
            status.contains("Handshake", ignoreCase = true) -> "HANDSHAKE"
            else -> "ACTIVE"
        }
    }

    private fun updateMouseJiggler() {
        if (_isMouseJigglerEnabled.value && connectionState.value is HidDeviceManager.ConnectionState.Connected) {
            if (mouseJigglerJob == null) {
                mouseJigglerJob = viewModelScope.launch(Dispatchers.IO) {
                    while (isActive) {
                        repository.sendMouseMove(1f, 0f)
                        delay(30_000)
                        repository.sendMouseMove(-1f, 0f)
                        delay(30_000)
                    }
                }
            }
        } else {
            mouseJigglerJob?.cancel()
            mouseJigglerJob = null
        }
    }

    // Media & System Shortcuts
    fun sendMediaPlayPause() = repository.sendConsumerKey(HidKeyCodes.MEDIA_PLAY_PAUSE)
    fun sendMediaVolumeUp() = repository.sendConsumerKey(HidKeyCodes.MEDIA_VOL_UP)
    fun sendMediaVolumeDown() = repository.sendConsumerKey(HidKeyCodes.MEDIA_VOL_DOWN)
    fun sendMediaNext() = repository.sendConsumerKey(HidKeyCodes.MEDIA_NEXT)
    fun sendMediaPrev() = repository.sendConsumerKey(HidKeyCodes.MEDIA_PREVIOUS)
    fun sendMediaNextTrack() = sendMediaNext()
    fun sendMediaPreviousTrack() = sendMediaPrev()

    fun sendConsumerKey(keyCode: Int) = repository.sendConsumerKey(keyCode.toShort())
    fun sendConsumerKey(keyCode: Short) = repository.sendConsumerKey(keyCode)
    fun sendKeyCombination(combo: String) = repository.executeKeyCombo(combo)
    fun sendKeyCombination(bytes: List<Byte>) { /* Stub for byte-based combo */ }

    fun generateSmartMacro(intent: String) { /* Stub */ }
    fun cancelMacro() { /* Stub */ }

    fun pauseTextPush() { _isPushPaused.value = true }
    fun resumeTextPush() { _isPushPaused.value = false }
    fun stopTextPush() { /* Stub */ }

    // AirPlay Controls
    fun startAirPlayReceiver() { /* Stub */ }
    fun stopAirPlayReceiver() { /* Stub */ }
    fun restartAirPlayReceiver() { /* Stub */ }
    fun setAirPlayAutoFallbackEnabled(enabled: Boolean) { /* Stub */ }
    fun refreshAirPlayDecoderCapability() { /* Stub */ }
    fun clearAirPlayStatusLog() { /* Stub */ }
    fun playAirPlayTestTone() { /* Stub */ }

    fun requestNowPlayingFromHost() { /* Stub */ }
    fun discoverHelperOnLocalWifi() { /* Stub */ }
    fun pingRemoteDevice() { /* Stub */ }

    fun sendSystemShortcut(shortcut: SystemShortcut) {
        when (shortcut) {
            SystemShortcut.MUTE -> repository.sendConsumerKey(HidKeyCodes.MEDIA_MUTE)
            SystemShortcut.PLAY_PAUSE -> repository.sendConsumerKey(HidKeyCodes.MEDIA_PLAY_PAUSE)
            SystemShortcut.NEXT -> repository.sendConsumerKey(HidKeyCodes.MEDIA_NEXT)
            SystemShortcut.PREV -> repository.sendConsumerKey(HidKeyCodes.MEDIA_PREVIOUS)
            SystemShortcut.VOL_UP -> repository.sendConsumerKey(HidKeyCodes.MEDIA_VOL_UP)
            SystemShortcut.VOL_DOWN -> repository.sendConsumerKey(HidKeyCodes.MEDIA_VOL_DOWN)
            SystemShortcut.LOCK_SCREEN -> repository.executeKeyCombo("CTRL+GUI+Q")
        }
    }

    fun unlockMac() {
        viewModelScope.launch {
            val password = secureStorage.getMacPassword() ?: ""
            if (password.isNotBlank()) {
                repository.executeKeyCombo("GUI+SPACE")
                delay(300)
                repository.sendText(password)
                delay(200)
                repository.sendKey(HidKeyCodes.KEY_ENTER)
            }
        }
    }
    
    fun markOnboardingCompleted() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
    }
    
    val onboardingCompleted: Boolean get() = prefs.getBoolean("onboarding_completed", false)

    fun requestEnableBluetooth(context: Context) {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun requestDiscoverable() {
        repository.requestDiscoverable()
    }

    private fun saveDevice(name: String, address: String) {
        // Simple logic for device saving
    }

    fun proximityNearDistanceMeters(): Float {
        val rssi = _proximityNearRssi.value.toFloat()
        return 10.0.pow((-69 - rssi) / (10 * 2.0)).toFloat()
    }

    // Setters for SettingsScreen
    fun setAutoReconnectEnabled(enabled: Boolean) {
        _autoReconnectEnabled.value = enabled
        prefs.edit().putBoolean("auto_reconnect_enabled", enabled).apply()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
    }

    fun setTrackpadSensitivity(value: Float) {
        _trackpadSensitivity.value = value
        prefs.edit().putFloat("trackpad_sensitivity", value).apply()
    }

    fun setAutoPushEnabled(enabled: Boolean) {
        _autoPushEnabled.value = enabled
        prefs.edit().putBoolean("auto_push_enabled", enabled).apply()
    }

    fun setNotificationSyncEnabled(enabled: Boolean) {
        _notificationSyncEnabled.value = enabled
        prefs.edit().putBoolean("notification_sync_enabled", enabled).apply()
    }

    fun setProximityAutoUnlockEnabled(enabled: Boolean) {
        _proximityAutoUnlockEnabled.value = enabled
        prefs.edit().putBoolean("proximity_auto_unlock_enabled", enabled).apply()
    }

    fun setProximityRequirePhoneUnlock(enabled: Boolean) {
        _proximityRequirePhoneUnlock.value = enabled
        prefs.edit().putBoolean("proximity_require_phone_unlock", enabled).apply()
    }

    fun setMouseJigglerEnabled(enabled: Boolean) {
        _isMouseJigglerEnabled.value = enabled
        prefs.edit().putBoolean("mouse_jiggler_enabled", enabled).apply()
        updateMouseJiggler()
    }

    fun setProximityUnlockDistanceMeters(meters: Float) {
        val rssi = (-69 - 10 * 2.0 * log10(meters.toDouble())).toInt()
        _proximityNearRssi.value = rssi
        prefs.edit().putInt("proximity_near_rssi", rssi).apply()
    }

    fun setProximityCooldownSec(seconds: Int) {
        _proximityCooldownSec.value = seconds
        prefs.edit().putInt("proximity_cooldown_sec", seconds).apply()
    }

    fun setProximityTargetAddress(address: String) {
        _proximityTargetAddress.value = address
        prefs.edit().putString("proximity_target_address", address).apply()
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        _biometricLockEnabled.value = enabled
        prefs.edit().putBoolean("biometric_lock_enabled", enabled).apply()
    }

    fun setStealthModeEnabled(enabled: Boolean) {
        _stealthModeEnabled.value = enabled
        prefs.edit().putBoolean("stealth_mode_enabled", enabled).apply()
    }

    fun setBiometricMacAutofillEnabled(enabled: Boolean) {
        _biometricMacAutofillEnabled.value = enabled
        prefs.edit().putBoolean("biometric_mac_autofill_enabled", enabled).apply()
    }

    fun setMacAutofillPreEnter(enabled: Boolean) {
        _macAutofillPreEnter.value = enabled
        prefs.edit().putBoolean("mac_autofill_pre_enter", enabled).apply()
    }

    fun setMacAutofillPostEnter(enabled: Boolean) {
        _macAutofillPostEnter.value = enabled
        prefs.edit().putBoolean("mac_autofill_post_enter", enabled).apply()
    }

    fun setShakeToDisconnectEnabled(enabled: Boolean) {
        _shakeToDisconnectEnabled.value = enabled
        prefs.edit().putBoolean("shake_to_disconnect_enabled", enabled).apply()
    }

    fun setTtsPitch(pitch: Float) {
        _ttsPitch.value = pitch
        prefs.edit().putFloat("tts_pitch", pitch).apply()
    }

    fun setTtsSpeechRate(rate: Float) {
        _ttsSpeechRate.value = rate
        prefs.edit().putFloat("tts_speech_rate", rate).apply()
    }

    fun setFeatureWebBridgeVisible(enabled: Boolean) {
        _featureWebBridgeVisible.value = enabled
        prefs.edit().putBoolean("feature_web_bridge_visible", enabled).apply()
    }

    fun setFeatureAutomationVisible(enabled: Boolean) {
        _featureAutomationVisible.value = enabled
        prefs.edit().putBoolean("feature_automation_visible", enabled).apply()
    }

    fun setFeatureAssistantVisible(enabled: Boolean) {
        _featureAssistantVisible.value = enabled
        prefs.edit().putBoolean("feature_assistant_visible", enabled).apply()
    }

    fun setFeatureSnippetsVisible(enabled: Boolean) {
        _featureSnippetsVisible.value = enabled
        prefs.edit().putBoolean("feature_snippets_visible", enabled).apply()
    }

    fun setFeatureWakeOnLanVisible(enabled: Boolean) {
        _featureWakeOnLanVisible.value = enabled
        prefs.edit().putBoolean("feature_wake_on_lan_visible", enabled).apply()
    }

    fun setFeatureSshTerminalVisible(enabled: Boolean) {
        _featureSshTerminalVisible.value = enabled
        prefs.edit().putBoolean("feature_ssh_terminal_visible", enabled).apply()
    }

    fun setMacPassword(pass: String) {
        secureStorage.saveMacPassword(pass)
        _macPassword.value = pass
    }

    fun sendMacPassword(password: String, preEnter: Boolean, postEnter: Boolean): String? {
        if (connectionState.value !is HidDeviceManager.ConnectionState.Connected) {
            return "Not connected to host"
        }
        viewModelScope.launch {
            repository.unlockMac(password, preEnter, postEnter)
        }
        return null
    }

    fun sendStoredMacPasswordToHost(): String? {
        val pass = _macPassword.value
        if (pass.isBlank()) return "No password stored"
        return sendMacPassword(pass, _macAutofillPreEnter.value, _macAutofillPostEnter.value)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        spatialPointerManager.stop()
        gyroAirMouse.stop()
        proximityTelemetryJob?.cancel()
    }
}
