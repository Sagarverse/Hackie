package com.example.rabit.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import com.example.rabit.data.network.RabitNetworkServer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import android.content.Intent
import android.bluetooth.BluetoothAdapter
import androidx.core.content.ContextCompat
import com.example.rabit.data.voice.VoiceState
import com.example.rabit.data.airplay.AirPlayReceiverService

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
    val isBluetoothConnected: StateFlow<Boolean> = repository.connectionState.map { 
        it is HidDeviceManager.ConnectionState.Connected 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val discoveredDevices: StateFlow<Set<BluetoothDevice>> = repository.scannedDevices
    
    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising = _isAdvertising.asStateFlow()
    
    val isTextPushing = repository.isTextPushing
    val knownWorkstations = repository.knownWorkstations
    val activeModifiers: StateFlow<Byte> = repository.activeModifiers
    
    // Combine saved workstations with bonded system devices
    private val bluetoothAdapter: BluetoothAdapter? = application.getSystemService(android.bluetooth.BluetoothManager::class.java)?.adapter
    val savedDevices: StateFlow<List<Workstation>> = combine(
        repository.knownWorkstations,
        flow {
            while(true) {
                val devices = try {
                    bluetoothAdapter?.bondedDevices?.map { device ->
                        Workstation(
                            address = device.address,
                            name = device.name ?: "Unknown Device",
                            lastConnected = 0L
                        )
                    } ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                emit(devices)
                delay(5000)
            }
        }
    ) { known, bonded ->
        val merged = (known + bonded).distinctBy { it.address.lowercase() }
        merged.sortedByDescending { it.lastConnected }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _isHumanTypingEnabled = MutableStateFlow(prefs.getBoolean("human_typing_enabled", false))
    val isHumanTypingEnabled = _isHumanTypingEnabled.asStateFlow()

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

    // OPSEC & Anti-Forensics
    private val _isDecoyMode = MutableStateFlow(prefs.getBoolean("is_decoy_mode", false))
    val isDecoyMode = _isDecoyMode.asStateFlow()

    fun setDecoyMode(enabled: Boolean) {
        _isDecoyMode.value = enabled
        prefs.edit().putBoolean("is_decoy_mode", enabled).apply()
    }

    fun nukeData() {
        val editor = prefs.edit()
        // Wipe all saved custom macros
        editor.remove("rabit_macros")
        // Wipe AI keys
        editor.remove("gemini_api_key")
        // Wipe tactical history/settings (if any)
        editor.remove("recent_workstations")
        // Automatically hide tactical UI sections
        editor.putBoolean("feature_web_bridge_visible", false)
        editor.putBoolean("feature_automation_visible", false)
        editor.putBoolean("feature_ssh_terminal_visible", false)
        editor.apply()

        // Engage Decoy Mode immediately after nuking
        setDecoyMode(true)
    }

    fun startAdvertising() {
        _isAdvertising.value = true
        repository.requestDiscoverable()
        viewModelScope.launch {
            delay(300_000) // Discoverable duration
            _isAdvertising.value = false
        }
    }

    fun stopAdvertising() {
        _isAdvertising.value = false
    }


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

    init {
        // Handle Auto-Reconnect on startup
        viewModelScope.launch {
            if (_autoReconnectEnabled.value) {
                // Wait for device list to populate (bonded devices take a few moments to load)
                savedDevices.first { it.isNotEmpty() }
                
                // Attempt connection to the most recently used device
                val target = savedDevices.value.firstOrNull()
                target?.let { device ->
                    Log.d("MainViewModel", "Auto-reconnecting to ${device.name} (${device.address})")
                    bluetoothAdapter?.getRemoteDevice(device.address)?.let { btDevice ->
                        repository.connect(btDevice)
                    }
                }
            }
        }
    }

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

    private val _unlockSyncEnabled = MutableStateFlow(prefs.getBoolean("unlock_sync_enabled", false))
    val unlockSyncEnabled = _unlockSyncEnabled.asStateFlow()

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
    val isPushPaused = repository.isPushPaused

    // Compatibility Stubs & Functional States
    private val _helperBaseUrl = MutableStateFlow(prefs.getString("helper_base_url", "") ?: "")
    val helperBaseUrl = _helperBaseUrl.asStateFlow()

    private val _isHelperConnected = MutableStateFlow(false)
    val isHelperConnected = _isHelperConnected.asStateFlow()

    private val _helperDeviceName = MutableStateFlow("")
    val helperDeviceName = _helperDeviceName.asStateFlow()

    private val _helperDeviceMac = MutableStateFlow("")
    val helperDeviceMac = _helperDeviceMac.asStateFlow()

    private val _helperDeviceIp = MutableStateFlow("")
    val helperDeviceIp = _helperDeviceIp.asStateFlow()

    val p2pStatus = MutableStateFlow("Disconnected").asStateFlow()
    val helperConnectionStatus = MutableStateFlow("Ready").asStateFlow()
    val helperTransferEvents = MutableStateFlow<List<String>>(emptyList()).asStateFlow()
    
    private val _nowPlayingTitle = MutableStateFlow("Nothing playing")
    val nowPlayingTitle = _nowPlayingTitle.asStateFlow()

    private val _nowPlayingArtist = MutableStateFlow("")
    val nowPlayingArtist = _nowPlayingArtist.asStateFlow()

    private val _nowPlayingAlbum = MutableStateFlow("")
    val nowPlayingAlbum = _nowPlayingAlbum.asStateFlow()

    private val _nowPlayingArtworkBase64 = MutableStateFlow<String?>(null)
    val nowPlayingArtworkBase64 = _nowPlayingArtworkBase64.asStateFlow()

    private val _airPlayPacketStats = MutableStateFlow("0 pkt / 0 drop / 0 reorder")
    val airPlayPacketStats = _airPlayPacketStats.asStateFlow()
    private val _airPlayAutoFallbackEnabled = MutableStateFlow(prefs.getBoolean("airplay_auto_fallback_enabled", true))
    val airPlayAutoFallbackEnabled = _airPlayAutoFallbackEnabled.asStateFlow()
    private val _airPlayReceiverEnabled = MutableStateFlow(false)
    val airPlayReceiverEnabled = _airPlayReceiverEnabled.asStateFlow()
    private val _airPlayNativeReadiness = MutableStateFlow("LOW")
    val airPlayNativeReadiness = _airPlayNativeReadiness.asStateFlow()
    private val _airPlayLastRtspMethod = MutableStateFlow("N/A")
    val airPlayLastRtspMethod = _airPlayLastRtspMethod.asStateFlow()
    private val _airPlayServerPorts = MutableStateFlow("N/A")
    val airPlayServerPorts = _airPlayServerPorts.asStateFlow()
    private val _airPlayClientPorts = MutableStateFlow("N/A")
    val airPlayClientPorts = _airPlayClientPorts.asStateFlow()
    private val _airPlayEncryptionEnabled = MutableStateFlow(false)
    val airPlayEncryptionEnabled = _airPlayEncryptionEnabled.asStateFlow()
    private val _airPlayAudioLatency = MutableStateFlow(0L)
    val airPlayAudioLatency = _airPlayAudioLatency.asStateFlow()
    private val _airPlayBufferStatus = MutableStateFlow("Idle")
    val airPlayBufferStatus = _airPlayBufferStatus.asStateFlow()

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
        HidDeviceManager.getInstance(application).isHumanTypingEnabled = _isHumanTypingEnabled.value
        startProximityTelemetryRefresh()

        // Link Now Playing updates from RabitNetworkServer (Mac Companion Push)
        val existingReceiver = RabitNetworkServer.nowPlayingReceiver
        RabitNetworkServer.nowPlayingReceiver = { payload ->
            _nowPlayingTitle.value = payload.title
            _nowPlayingArtist.value = payload.artist
            _nowPlayingAlbum.value = payload.album
            _nowPlayingArtworkBase64.value = payload.artworkBase64
            existingReceiver?.invoke(payload)
        }
        
        // Listen to AirPlay State Bus
        viewModelScope.launch {
            AirPlayStateBus.status.collect { status ->
                _airPlayStatus.value = status
                val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
                _airPlayStatusLog.value = (listOf("[$ts] $status") + _airPlayStatusLog.value).take(24)
                _airPlayHandshakeStage.value = mapAirPlayStage(status)
                syncAirPlayTelemetry(status)
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
    fun startScan() = repository.startScanning()
    fun stopScan() = repository.stopScanning()
    
    fun connectToDevice(device: BluetoothDevice) = repository.connect(device)
    fun connectToDevice(address: String) {
        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device != null) {
            repository.connect(device)
        }
    }
    fun connect(device: BluetoothDevice) = repository.connect(device)
    fun connectWithRetry(device: BluetoothDevice) = repository.connect(device) // Simplified for now
    fun disconnectKeyboard() = repository.disconnect()

    fun sendText(text: String) = repository.sendText(text)
    fun sendKey(keyCode: Byte, modifiers: Byte = 0, useSticky: Boolean = true) = 
        repository.sendKey(keyCode, modifiers, useSticky)
    
    fun clearModifiers() {
        repository.setModifier(HidKeyCodes.MODIFIER_LEFT_CTRL, false)
        repository.setModifier(HidKeyCodes.MODIFIER_LEFT_ALT, false)
        repository.setModifier(HidKeyCodes.MODIFIER_LEFT_GUI, false)
        repository.setModifier(HidKeyCodes.MODIFIER_LEFT_SHIFT, false)
    }
    fun sendMouseMove(dx: Float, dy: Float, buttons: Int = 0, scroll: Int = 0) {
        repository.sendMouseMove(dx, dy, buttons, scroll)
    }
    
    fun setMouseLocked(locked: Boolean) = repository.setMouseLocked(locked)

    fun toggleModifier(modifier: Byte) {
        val current = activeModifiers.value
        val isActive = (current.toInt() and modifier.toInt()) != 0
        repository.setModifier(modifier, !isActive)
    }

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
            "Too Slow" -> 350L
            "Slow" -> 180L
            "Fast" -> 60L
            "Super Fast" -> 20L
            else -> 120L
        }
        HidDeviceManager.getInstance(getApplication()).typingDelay = delay
    }

    fun setHumanTypingEnabled(enabled: Boolean) {
        _isHumanTypingEnabled.value = enabled
        prefs.edit().putBoolean("human_typing_enabled", enabled).apply()
        HidDeviceManager.getInstance(getApplication()).isHumanTypingEnabled = enabled
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
            // Neutralize mouse state to stop any "background" movement or stuck buttons
            repository.sendMouseMove(0f, 0f, 0, 0)
            repository.resetMouseAccumulator()
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
            status.contains("ANNOUNCE", ignoreCase = true) -> "ANNOUNCED"
            status.contains("SETUP", ignoreCase = true) -> "SETUP"
            status.contains("RECORD", ignoreCase = true) -> "RECORDING"
            status.contains("Streaming", ignoreCase = true) || status.contains("delivered=", ignoreCase = true) -> "STREAMING"
            status.contains("stalled", ignoreCase = true) -> "STALLED"
            status.contains("failed", ignoreCase = true) || status.contains("unsupported", ignoreCase = true) -> "FAILED"
            status.contains("Handshake", ignoreCase = true) -> "HANDSHAKE"
            else -> "ACTIVE"
        }
    }

    private fun syncAirPlayTelemetry(status: String) {
        if (status.contains("Idle", ignoreCase = true)) {
            _airPlayReceiverEnabled.value = false
        }
        if (status.contains("Starting AirPlay receiver", ignoreCase = true) ||
            status.contains("AirPlay ready on port", ignoreCase = true) ||
            status.contains("RAOP advertised", ignoreCase = true) ||
            status.contains("RAOP client connected", ignoreCase = true)
        ) {
            _airPlayReceiverEnabled.value = true
        }

        if (status.contains("RTSP ", ignoreCase = true)) {
            _airPlayLastRtspMethod.value = status.substringAfter("RTSP ").substringBefore(' ').uppercase(Locale.getDefault())
        }

        if (status.contains("RAOP UDP ready", ignoreCase = true)) {
            _airPlayServerPorts.value = status.substringAfter("RAOP UDP ready ").trim()
        }

        if (status.contains("RAOP client ports", ignoreCase = true)) {
            _airPlayClientPorts.value = status.substringAfter("RAOP client ports ").trim()
        }

        if (status.contains("delivered=", ignoreCase = true)) {
            _airPlayPacketStats.value = status.substringAfter("RTP packets ").trim()
            _airPlayBufferStatus.value = "Flowing"
            _airPlayNativeReadiness.value = "MEDIUM"
        } else if (status.contains("Waiting for RTP packets", ignoreCase = true) ||
            status.contains("stalled", ignoreCase = true)
        ) {
            _airPlayBufferStatus.value = "Waiting"
        }

        if (status.contains("Encrypted RAOP session requested", ignoreCase = true)) {
            _airPlayEncryptionEnabled.value = true
        }
        if (status.contains("ALAC native decode pipeline armed", ignoreCase = true) ||
            status.contains("ALAC decode active", ignoreCase = true)
        ) {
            _airPlayNativeReadiness.value = "MEDIUM"
        }
        if (status.contains("decode unavailable", ignoreCase = true) ||
            status.contains("fallback required", ignoreCase = true)
        ) {
            _airPlayNativeReadiness.value = "LOW-MEDIUM"
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
    fun sendMediaPlayPause() {
        performHapticFeedback("Soft")
        executeHybridMediaCommand(HidKeyCodes.MEDIA_PLAY_PAUSE, "media_play_pause")
    }
    fun sendMediaVolumeUp() {
        performHapticFeedback("Sharp")
        executeHybridMediaCommand(HidKeyCodes.MEDIA_VOL_UP, "volume_up")
    }
    fun sendMediaVolumeDown() {
        performHapticFeedback("Sharp")
        executeHybridMediaCommand(HidKeyCodes.MEDIA_VOL_DOWN, "volume_down")
    }
    fun sendMediaNext() {
        performHapticFeedback("Soft")
        executeHybridMediaCommand(HidKeyCodes.MEDIA_NEXT, "media_next")
    }
    fun sendMediaPrev() {
        performHapticFeedback("Soft")
        executeHybridMediaCommand(HidKeyCodes.MEDIA_PREVIOUS, "media_prev")
    }
    fun sendMediaNextTrack() = sendMediaNext()
    fun sendMediaPreviousTrack() = sendMediaPrev()

    private fun executeHybridMediaCommand(hidKey: Short, helperCommand: String) {
        if (connectionState.value is HidDeviceManager.ConnectionState.Connected) {
            repository.sendConsumerKey(hidKey)
        } else if (_helperBaseUrl.value.isNotEmpty()) {
            sendHelperCommand(helperCommand)
        }
    }

    private fun sendHelperCommand(command: String, payload: JSONObject = JSONObject()) {
        val base = _helperBaseUrl.value
        if (base.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                payload.put("command", command)
                val conn = (URL("$base/command").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 2000
                    readTimeout = 2000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                conn.responseCode // Trigger request
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Helper command failed: $command", e)
            }
        }
    }

    fun sendConsumerKey(keyCode: Int) = repository.sendConsumerKey(keyCode.toShort())
    fun sendConsumerKey(keyCode: Short) = repository.sendConsumerKey(keyCode)
    fun sendKeyCombination(combo: String) = repository.executeKeyCombo(combo)
    fun sendKeyCombination(bytes: List<Byte>) { /* Stub for byte-based combo */ }

    fun generateSmartMacro(intent: String) { /* Stub */ }
    fun cancelMacro() { /* Stub */ }

    fun pauseTextPush() { repository.pauseTextPush() }
    fun resumeTextPush() { repository.resumeTextPush() }
    fun stopTextPush() { repository.stopTextPush() }

    // AirPlay Controls
    fun startAirPlayReceiver() {
        val context = getApplication<Application>()
        val intent = Intent(context, AirPlayReceiverService::class.java).apply {
            action = AirPlayReceiverService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
        _airPlayReceiverEnabled.value = true
    }

    fun stopAirPlayReceiver() {
        val context = getApplication<Application>()
        val intent = Intent(context, AirPlayReceiverService::class.java).apply {
            action = AirPlayReceiverService.ACTION_STOP
        }
        context.startService(intent)
        _airPlayReceiverEnabled.value = false
    }

    fun restartAirPlayReceiver() {
        stopAirPlayReceiver()
        viewModelScope.launch {
            delay(250)
            startAirPlayReceiver()
        }
    }

    fun setAirPlayAutoFallbackEnabled(enabled: Boolean) {
        _airPlayAutoFallbackEnabled.value = enabled
        prefs.edit().putBoolean("airplay_auto_fallback_enabled", enabled).apply()
    }

    fun refreshAirPlayDecoderCapability() {
        _airPlayAlacCapability.value = AlacFrameDecoder.capabilityLabel()
    }

    fun clearAirPlayStatusLog() {
        _airPlayStatusLog.value = emptyList()
    }

    fun playAirPlayTestTone() {
        val context = getApplication<Application>()
        val intent = Intent(context, AirPlayReceiverService::class.java).apply {
            action = AirPlayReceiverService.ACTION_TEST_TONE
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun requestNowPlayingFromHost() {
        val base = _helperBaseUrl.value
        if (base.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val conn = (URL("$base/now-playing").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 3000
                    readTimeout = 3000
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    _nowPlayingTitle.value = json.optString("title", "No track")
                    _nowPlayingArtist.value = json.optString("artist", "Unknown artist")
                    _nowPlayingAlbum.value = json.optString("album", "")
                    _nowPlayingArtworkBase64.value = json.optString("artworkBase64", null)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to fetch metadata", e)
            }
        }
    }
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

    fun setUnlockSyncEnabled(enabled: Boolean) {
        _unlockSyncEnabled.value = enabled
        prefs.edit().putBoolean("unlock_sync_enabled", enabled).apply()
        // Notify HidService to update notification actions
        val intent = Intent(getApplication(), HidService::class.java).apply {
            action = "REFRESH_MAIN_NOTIFICATION"
        }
        getApplication<Application>().startService(intent)
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
