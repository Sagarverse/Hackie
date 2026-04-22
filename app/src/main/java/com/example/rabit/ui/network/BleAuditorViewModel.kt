package com.example.rabit.ui.network

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import com.example.rabit.domain.model.gemini.GeminiRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

data class BleDevice(
    val device: BluetoothDevice,
    val rssi: Int,
    val scanRecord: ByteArray?,
    val name: String = "Unknown",
    val address: String = ""
)

data class BleServiceInfo(
    val uuid: String,
    val characteristics: List<BleCharacteristicInfo>
)

data class BleCharacteristicInfo(
    val uuid: String,
    val properties: List<String>,
    val isDangerous: Boolean
)

sealed class BleState {
    object Idle : BleState()
    object Scanning : BleState()
    data class DeviceSelected(
        val device: BleDevice,
        val services: List<BleServiceInfo>,
        val aiInsight: String? = null,
        val isAnalyzing: Boolean = false
    ) : BleState()
}

@SuppressLint("MissingPermission")
class BleAuditorViewModel(application: Application) : AndroidViewModel(application) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scanner = bluetoothAdapter?.bluetoothLeScanner
    private val geminiRepo = GeminiRepositoryImpl()

    private val _devices = MutableStateFlow<List<BleDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _uiState = MutableStateFlow<BleState>(BleState.Idle)
    val uiState = _uiState.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val newDevice = BleDevice(
                device = device,
                rssi = result.rssi,
                scanRecord = result.scanRecord?.bytes,
                name = device.name ?: "Unknown",
                address = device.address
            )
            
            val currentList = _devices.value.toMutableList()
            val index = currentList.indexOfFirst { it.address == newDevice.address }
            if (index != -1) {
                currentList[index] = newDevice
            } else {
                currentList.add(newDevice)
            }
            _devices.value = currentList.sortedByDescending { it.rssi }
        }
    }

    fun startScan() {
        if (_uiState.value is BleState.Scanning) return
        _devices.value = emptyList()
        _uiState.value = BleState.Scanning
        scanner?.startScan(scanCallback)
        
        // Auto-stop after 15 seconds
        viewModelScope.launch {
            delay(15000)
            stopScan()
        }
    }

    fun stopScan() {
        if (_uiState.value is BleState.Scanning) {
            scanner?.stopScan(scanCallback)
            _uiState.value = BleState.Idle
        }
    }

    fun selectDevice(bleDevice: BleDevice) {
        stopScan()
        _uiState.value = BleState.DeviceSelected(bleDevice, emptyList())
        
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val services = gatt.services.map { service ->
                        BleServiceInfo(
                            uuid = service.uuid.toString(),
                            characteristics = service.characteristics.map { char ->
                                val props = mutableListOf<String>()
                                if (char.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) props.add("READ")
                                if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) props.add("WRITE")
                                if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) props.add("WRITE_NO_RESP")
                                if (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) props.add("NOTIFY")
                                if (char.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) props.add("INDICATE")
                                
                                val isDangerous = props.contains("WRITE") || props.contains("WRITE_NO_RESP")
                                
                                BleCharacteristicInfo(char.uuid.toString(), props, isDangerous)
                            }
                        )
                    }
                    
                    _uiState.value = (_uiState.value as BleState.DeviceSelected).copy(services = services)
                }
            }
        }
        
        bleDevice.device.connectGatt(getApplication(), false, gattCallback)
    }

    fun analyzeWithAi(apiKey: String) {
        val currentState = _uiState.value as? BleState.DeviceSelected ?: return
        if (apiKey.isBlank()) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isAnalyzing = true)
            try {
                val serviceSummary = currentState.services.joinToString("\n") { svc ->
                    "Service: ${svc.uuid}\n" + svc.characteristics.joinToString("\n") { char ->
                        "  - Char: ${char.uuid} | Props: ${char.properties.joinToString(",")}"
                    }
                }

                val prompt = """
                    Analyze this Bluetooth Low Energy (BLE) device for security vulnerabilities.
                    Device Name: ${currentState.device.name}
                    MAC Address: ${currentState.device.address}
                    
                    --- SERVICES & CHARACTERISTICS ---
                    $serviceSummary
                    
                    Identify the likely device type (e.g. Smart Lock, Health Tracker, HID Keyboard). 
                    Look for 'Exposed Write' characteristics which might allow unauthorized control.
                    Provide a tactical risk assessment and potential attack vectors (e.g. GATT Replay, Unauthorized Command Injection).
                """.trimIndent()

                val request = GeminiRequest(
                    prompt = prompt,
                    systemPrompt = "You are an expert in Bluetooth/RF security. Provide a technical, tactical vulnerability assessment.",
                    temperature = 0.3f
                )

                val response = geminiRepo.sendPrompt(request, apiKey)
                _uiState.value = currentState.copy(
                    isAnalyzing = false,
                    aiInsight = response.text ?: response.error?.message ?: "Analysis failed."
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(isAnalyzing = false, aiInsight = "Error: ${e.localizedMessage}")
            }
        }
    }

    fun reset() {
        stopScan()
        _devices.value = emptyList()
        _uiState.value = BleState.Idle
    }
}
