package com.example.rabit.ui.remotedeck

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.network.LanIpResolver
import com.example.rabit.data.network.WebRtcManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class RemoteDeckViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
    private val webRtcManager = WebRtcManager(application)
    
    private val _connectionStatus = webRtcManager.connectionStatus
    val connectionStatus: StateFlow<String> = _connectionStatus

    private val _currentPin = MutableStateFlow("")
    val currentPin: StateFlow<String> = _currentPin.asStateFlow()

    private val _clientStats = MutableStateFlow<Map<String, String>>(emptyMap())
    val clientStats: StateFlow<Map<String, String>> = _clientStats.asStateFlow()

    private val _location = MutableStateFlow<Pair<String, String>?>(null)
    val location: StateFlow<Pair<String, String>?> = _location.asStateFlow()

    private val _exfiltratedFiles = MutableStateFlow<List<File>>(emptyList())
    val exfiltratedFiles: StateFlow<List<File>> = _exfiltratedFiles.asStateFlow()

    private val _localIp = MutableStateFlow("0.0.0.0")
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    init {
        val pin = String.format("%04d", (1000..9999).random())
        _currentPin.value = pin
        
        refreshLocalIp()
        startP2P(pin)
        
        viewModelScope.launch(Dispatchers.IO) {
            webRtcManager.incomingDataFlow.collect { (type, data) ->
                if (type == "METADATA" && data is String) {
                    handleIncomingMessage(data)
                }
            }
        }
    }

    private fun refreshLocalIp() {
        val ip = LanIpResolver.preferredLanIpv4String(getApplication()) ?: "0.0.0.0"
        _localIp.value = ip
    }

    private fun startP2P(pin: String) {
        val roomId = "rabit_p2p_$pin"
        webRtcManager.start(roomId)
    }

    private fun handleIncomingMessage(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            when (json.optString("type")) {
                "CLIENT_STATS" -> {
                    val stats = mutableMapOf<String, String>()
                    val keys = listOf("os", "ram", "cores", "ip")
                    keys.forEach { key ->
                        stats[key] = json.optString(key, "N/A")
                    }
                    _clientStats.value = stats
                }
                "LOCATION_UPDATE" -> {
                    val lat = json.optString("lat")
                    val lon = json.optString("lon")
                    _location.value = lat to lon
                }
                "REMOTE_FILE_PUSH" -> {
                    val fileName = json.optString("name")
                    val dataB64 = json.optString("dataBase64")
                    saveRemoteFile(fileName, dataB64)
                }
            }
        } catch (e: Exception) {
            Log.e("RemoteDeckVM", "Error parsing incoming JSON: $jsonStr", e)
        }
    }

    private fun refreshExfiltratedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val hackieDir = File(downloadsDir, "Hackie/RemoteDeck")
            if (hackieDir.exists()) {
                val files = hackieDir.listFiles()?.toList() ?: emptyList()
                _exfiltratedFiles.value = files.sortedByDescending { it.lastModified() }
            }
        }
    }

    private fun saveRemoteFile(name: String, base64: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val hackieDir = File(downloadsDir, "Hackie/RemoteDeck")
                if (!hackieDir.exists()) hackieDir.mkdirs()
                
                val file = File(hackieDir, "Remote_${System.currentTimeMillis()}_$name")
                FileOutputStream(file).use { it.write(data) }
                Log.d("RemoteDeckVM", "Saved remote file to ${file.absolutePath}")
                refreshExfiltratedFiles()
            } catch (e: Exception) {
                Log.e("RemoteDeckVM", "Failed to save remote file", e)
            }
        }
    }

    fun pushText(text: String) {
        if (text.isBlank()) return
        val payload = JSONObject().apply {
            put("type", "push_text")
            put("text", text)
        }.toString()
        webRtcManager.sendData(payload)
    }

    fun triggerRemoteAlert(text: String) {
        val payload = JSONObject().apply {
            put("type", "remote_alert")
            put("text", text)
        }.toString()
        webRtcManager.sendData(payload)
    }

    fun triggerFocus() {
        val payload = JSONObject().apply {
            put("type", "trigger_focus")
        }.toString()
        webRtcManager.sendData(payload)
    }

    fun regenerateSession() {
        webRtcManager.stop()
        val pin = String.format("%04d", (1000..9999).random())
        _currentPin.value = pin
        startP2P(pin)
    }

    override fun onCleared() {
        super.onCleared()
        webRtcManager.stop()
    }
}
