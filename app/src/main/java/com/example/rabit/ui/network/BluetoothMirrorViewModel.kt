package com.example.rabit.ui.network

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import com.example.rabit.domain.model.gemini.GeminiRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ClonedProfile(
    val name: String,
    val address: String,
    val deviceClass: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class BluetoothMirrorViewModel(application: Application) : AndroidViewModel(application) {
    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices = _pairedDevices.asStateFlow()

    private val _clonedProfile = MutableStateFlow<ClonedProfile?>(null)
    val clonedProfile = _clonedProfile.asStateFlow()

    private val _isMirroring = MutableStateFlow(false)
    val isMirroring = _isMirroring.asStateFlow()

    private val _mirrorLog = MutableStateFlow<List<String>>(emptyList())
    val mirrorLog = _mirrorLog.asStateFlow()

    private val _isDeauthing = MutableStateFlow(false)
    val isDeauthing = _isDeauthing.asStateFlow()

    private val _selectedPayload = MutableStateFlow<String?>(null)
    val selectedPayload = _selectedPayload.asStateFlow()

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        val paired = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        _pairedDevices.value = paired
    }

    fun selectPayload(script: String) {
        _selectedPayload.value = script
        addLog("HOOK: Payload '${script.take(15)}...' armed and ready.")
    }

    @SuppressLint("MissingPermission")
    fun sendDeauthPulse() {
        val profile = _clonedProfile.value ?: return
        _isDeauthing.value = true
        addLog("PULSE: Sending high-intensity connection spam to ${profile.address}...")
        
        viewModelScope.launch {
            // Simulate a "Bluetooth Jamming" or "Connection Flood" sequence
            repeat(5) {
                addLog("PULSE: Disrupting link frequency... [Attempt ${it+1}]")
                delay(1500)
            }
            _isDeauthing.value = false
            addLog("SUCCESS: Deauth Pulse complete. Target connection should be unstable.")
        }
    }

    @SuppressLint("MissingPermission")
    fun triggerPayloadHook() {
        val profile = _clonedProfile.value ?: return
        val payload = _selectedPayload.value ?: "STRING Hello World\nENTER"
        
        _isMirroring.value = true
        addLog("MIRROR: Masking as ${profile.name}...")
        bluetoothAdapter?.name = profile.name
        
        addLog("HOOK: Initiating 'Firmware Update' social engineering prompt on target...")
        
        viewModelScope.launch {
            delay(3000)
            addLog("HIJACK: Connection accepted. Executing Payload Hook...")
            // In a real scenario, this would trigger the HidService.executeDuckyScript()
            addLog("SUCCESS: Payload injected via ${profile.name} profile.")
        }
    }

    @SuppressLint("MissingPermission")
    fun cloneDevice(device: BluetoothDevice) {
        val profile = ClonedProfile(
            name = device.name ?: "Unknown_Peripheral",
            address = device.address,
            deviceClass = device.bluetoothClass?.deviceClass ?: 0
        )
        _clonedProfile.value = profile
        addLog("CLONE: Extracted identity for ${profile.name} [${profile.address}]")
    }

    @SuppressLint("MissingPermission")
    fun initiateMirrorHijack(targetAddress: String) {
        val profile = _clonedProfile.value ?: return
        _isMirroring.value = true
        
        addLog("MIRROR: Masking adapter as ${profile.name}...")
        bluetoothAdapter?.name = profile.name
        
        addLog("HIJACK: Forging silent handshake with target $targetAddress...")
        
        viewModelScope.launch {
            // Simulate the heavy lifting of forging a background GATT probe
            delay(3000)
            addLog("SUCCESS: Mirror Link established. Target system accepts identity.")
            addLog("PROBE: Sniffing MacBook proximity pulses using speaker signature.")
        }
    }

    @SuppressLint("MissingPermission")
    fun reset() {
        _isMirroring.value = false
        _clonedProfile.value = null
        bluetoothAdapter?.name = "Hackie_Station" // Revert to default
        addLog("SYSTEM: Mirror Lab reset. Identity reverted.")
    }

    private fun addLog(msg: String) {
        _mirrorLog.value = (listOf("> $msg") + _mirrorLog.value).take(50)
    }
}
