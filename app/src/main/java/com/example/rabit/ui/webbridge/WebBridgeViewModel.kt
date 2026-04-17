package com.example.rabit.ui.webbridge

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.network.RabitNetworkServer
import com.example.rabit.data.network.WebRtcManager
import com.example.rabit.data.repository.KeyboardRepositoryImpl
import com.example.rabit.domain.repository.KeyboardRepository
import com.example.rabit.domain.model.SharedTransferItem
import com.example.rabit.domain.model.TransferQueueStatus
import com.example.rabit.domain.model.BridgeNote
import com.example.rabit.data.bluetooth.HidService
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import com.example.rabit.data.db.NoteDatabase
import com.example.rabit.data.db.NoteEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class WebBridgeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
    private val webRtcManager = WebRtcManager(application)
    private val noteDatabase = NoteDatabase.getDatabase(application)
    private val noteDao = noteDatabase.noteDao()
    private val repository: KeyboardRepository = KeyboardRepositoryImpl(application)
    private val wifiAudioSink = com.example.rabit.data.airplay.AudioTrackPcmSink()
    
    private val _webBridgeRunning = MutableStateFlow(RabitNetworkServer.isRunning)
    val isWebBridgeRunning: StateFlow<Boolean> = _webBridgeRunning.asStateFlow()

    private val _webBridgePin = MutableStateFlow(RabitNetworkServer.currentPin)
    val webBridgePin: StateFlow<String> = _webBridgePin.asStateFlow()

    private val _webBridgeSelfTestStatus = MutableStateFlow("Not tested")
    val webBridgeSelfTestStatus: StateFlow<String> = _webBridgeSelfTestStatus.asStateFlow()

    private val _webBridgeSelfTestInProgress = MutableStateFlow(false)
    val webBridgeSelfTestInProgress: StateFlow<Boolean> = _webBridgeSelfTestInProgress.asStateFlow()

    private val _localIp = MutableStateFlow("0.0.0.0")
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _sharedFiles = MutableStateFlow<List<Uri>>(emptyList())
    val sharedFiles: StateFlow<List<Uri>> = _sharedFiles.asStateFlow()

    private val _receivedFiles = MutableStateFlow<List<File>>(emptyList())
    val receivedFiles: StateFlow<List<File>> = _receivedFiles.asStateFlow()

    private val _p2pEnabled = MutableStateFlow(false)
    val p2pEnabled: StateFlow<Boolean> = _p2pEnabled.asStateFlow()

    val p2pStatus: StateFlow<String> = webRtcManager.connectionStatus
    val p2pPeerId: StateFlow<String?> = webRtcManager.peerId

    private val _activeSessions = MutableStateFlow<List<RabitNetworkServer.TrustedSession>>(emptyList())
    val activeSessions: StateFlow<List<RabitNetworkServer.TrustedSession>> = _activeSessions.asStateFlow()

    val bridgeNotes: StateFlow<List<BridgeNote>> = noteDao.getAllNotes()
        .map { entities ->
            entities.map {
                BridgeNote(it.id, it.text, it.createdAtMs, it.source)
            }.sortedByDescending { it.createdAtMs }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Media / Now Playing
    private val _nowPlayingTitle = MutableStateFlow("No track")
    val nowPlayingTitle = _nowPlayingTitle.asStateFlow()
    private val _nowPlayingArtist = MutableStateFlow("Connect companion for metadata")
    val nowPlayingArtist = _nowPlayingArtist.asStateFlow()
    private val _nowPlayingAlbum = MutableStateFlow("")
    val nowPlayingAlbum = _nowPlayingAlbum.asStateFlow()
    private val _nowPlayingArtworkBase64 = MutableStateFlow<String?>(null)
    val nowPlayingArtworkBase64 = _nowPlayingArtworkBase64.asStateFlow()

    private val _wifiAudioStatus = MutableStateFlow("Idle")
    val wifiAudioStatus = _wifiAudioStatus.asStateFlow()
    private val _wifiAudioStreamActive = MutableStateFlow(false)
    val wifiAudioStreamActive = _wifiAudioStreamActive.asStateFlow()

    init {
        refreshLocalIp()
        refreshWebBridgeData()
        
        // Setup Providers for RabitNetworkServer
        setupServerProviders()
    }

    private fun setupServerProviders() {
        val context = getApplication<Application>()
        val clipSvc = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

        RabitNetworkServer.sharedFilesProvider = {
            _sharedFiles.value.map { uri ->
                val metadata = resolveSharedFileMetadata(uri)
                RabitNetworkServer.SharedFile(
                    id = uri.toString().hashCode().toString(),
                    name = metadata.first,
                    size = metadata.second,
                    type = context.contentResolver.getType(uri) ?: "application/octet-stream"
                )
            }
        }

        RabitNetworkServer.fileDownloadProvider = { id ->
            _sharedFiles.value.find { it.toString().hashCode().toString() == id }
        }

        RabitNetworkServer.clipboardProvider = {
            clipSvc.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        }

        RabitNetworkServer.clipboardReceiver = { text ->
            if (text.isNotEmpty()) {
                val clip = android.content.ClipData.newPlainText("Hackie Universal", text)
                clipSvc.setPrimaryClip(clip)
            }
        }

        RabitNetworkServer.nowPlayingReceiver = { payload ->
            _nowPlayingTitle.value = payload.title.ifBlank { "No track" }
            _nowPlayingArtist.value = payload.artist.ifBlank { "Unknown Artist" }
            _nowPlayingAlbum.value = payload.album.ifBlank { "" }
            _nowPlayingArtworkBase64.value = payload.artworkBase64
        }

        RabitNetworkServer.audioStreamStartReceiver = { payload ->
            wifiAudioSink.configure(
                sampleRate = payload.sampleRate.coerceIn(8_000, 96_000),
                channels = payload.channels.coerceIn(1, 2)
            )
            _wifiAudioStreamActive.value = true
            _wifiAudioStatus.value = "Streaming from ${payload.source} (${payload.sampleRate} Hz)"
        }

        RabitNetworkServer.audioStreamChunkReceiver = { payload ->
            try {
                val pcm = android.util.Base64.decode(payload.pcm16leBase64, android.util.Base64.DEFAULT)
                if (pcm.isNotEmpty()) {
                    wifiAudioSink.writePcm16le(pcm)
                    _wifiAudioStreamActive.value = true
                }
            } catch (_: Exception) {
                _wifiAudioStatus.value = "Audio chunk decode error"
            }
        }

        RabitNetworkServer.audioStreamStopReceiver = { payload ->
            wifiAudioSink.stop()
            _wifiAudioStreamActive.value = false
            _wifiAudioStatus.value = "Stopped (${payload.reason})"
        }

        RabitNetworkServer.systemActionReceiver = { action ->
            viewModelScope.launch {
                when (action) {
                    "vol_up" -> repository.executeSpecialKey("VOL_UP")
                    "vol_down" -> repository.executeSpecialKey("VOL_DOWN")
                    "play_pause" -> repository.executeSpecialKey("PLAY")
                    "brightness_up" -> repository.executeSpecialKey("BRIGHT_UP")
                    "brightness_down" -> repository.executeSpecialKey("BRIGHT_DOWN")
                    "lock" -> repository.executeKeyCombo("CTRL+GUI+Q")
                }
            }
        }
    }

    private fun generateRandomPin(): String {
        return String.format("%04d", (0..9999).random())
    }

    private fun refreshLocalIp() {
        val wm = getApplication<Application>().getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wm != null) {
            try {
                val ipAddress = wm.connectionInfo.ipAddress
                if (ipAddress != 0) {
                    val ip = String.format(
                        Locale.US, "%d.%d.%d.%d",
                        ipAddress and 0xff,
                        ipAddress shr 8 and 0xff,
                        ipAddress shr 16 and 0xff,
                        ipAddress shr 24 and 0xff
                    )
                    _localIp.value = ip
                } else {
                    val fallback = java.net.InetAddress.getLocalHost().hostAddress
                    if (fallback != null && fallback != "127.0.0.1") {
                        _localIp.value = fallback
                    }
                }
            } catch (e: Exception) {
                _localIp.value = "0.0.0.0"
            }
        }
    }

    fun startWebBridge() {
        val pin = generateRandomPin()
        RabitNetworkServer.currentPin = pin
        _webBridgePin.value = pin
        
        refreshLocalIp()
        
        val intent = Intent(getApplication<Application>(), HidService::class.java).apply {
            action = HidService.ACTION_START_WEB_BRIDGE
        }
        getApplication<Application>().startService(intent)
        prefs.edit().putBoolean("web_bridge_enabled", true).apply()
        
        viewModelScope.launch {
            delay(500)
            _webBridgeRunning.value = RabitNetworkServer.isRunning
            _webBridgePin.value = pin
            _webBridgeSelfTestStatus.value = "Not tested"
            startP2PHosting(forceRestart = true)
        }
    }

    fun stopWebBridge() {
        val intent = Intent(getApplication<Application>(), HidService::class.java).apply {
            action = HidService.ACTION_STOP_WEB_BRIDGE
        }
        getApplication<Application>().startService(intent)
        prefs.edit().putBoolean("web_bridge_enabled", false).apply()
        _webBridgeRunning.value = false
        _webBridgeSelfTestStatus.value = "Bridge stopped"
        clearSharedFiles()
        stopP2PHosting()
    }

    fun regenerateWebBridgePin() {
        val newPin = generateRandomPin()
        RabitNetworkServer.currentPin = newPin
        _webBridgePin.value = newPin
        startP2PHosting(forceRestart = true)
    }

    fun refreshWebBridgeData() {
        _webBridgeRunning.value = RabitNetworkServer.isRunning
        _webBridgePin.value = RabitNetworkServer.currentPin
        refreshLocalIp()
        _activeSessions.value = RabitNetworkServer.getActiveSessions()
        refreshReceivedFiles()
    }

    fun refreshReceivedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val hackieDir = File(downloadsDir, "Hackie")
            val files = mutableListOf<File>()
            downloadsDir.listFiles()?.filter { it.isFile && it.name.startsWith("Hackie_") }?.let { files.addAll(it) }
            if (hackieDir.exists() && hackieDir.isDirectory) {
                hackieDir.listFiles()?.filter { it.isFile }?.let { files.addAll(it) }
            }
            _receivedFiles.value = files.sortedByDescending { it.lastModified() }
        }
    }

    fun revokeActiveSession(token: String) {
        RabitNetworkServer.revokeSession(token)
        refreshWebBridgeData()
    }

    fun addSharedFile(uri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        _sharedFiles.value = (_sharedFiles.value + uri).distinct()
    }

    fun removeSharedFile(uri: Uri) {
        _sharedFiles.value = _sharedFiles.value - uri
    }

    fun clearSharedFiles() {
        _sharedFiles.value = emptyList()
    }

    fun deleteReceivedFile(file: File) {
        if (file.exists()) {
            file.delete()
            refreshReceivedFiles()
        }
    }

    private fun resolveSharedFileMetadata(uri: Uri): Pair<String, Long> {
        return try {
            var name = "File"
            var size = 0L
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) name = cursor.getString(nameIndex)
                    if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
                }
            }
            name to size
        } catch (e: Exception) {
            "File" to 0L
        }
    }

    fun runWebBridgeConnectivitySelfTest() {
        if (_webBridgeSelfTestInProgress.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _webBridgeSelfTestInProgress.value = true
            try {
                _webBridgeRunning.value = RabitNetworkServer.isRunning
                if (!_webBridgeRunning.value) {
                    _webBridgeSelfTestStatus.value = "FAIL: Web Bridge is not running"
                    return@launch
                }

                refreshLocalIp()
                delay(200)
                val host = _localIp.value
                if (host == "0.0.0.0") {
                    _webBridgeSelfTestStatus.value = "FAIL: Could not resolve phone Wi-Fi IP"
                    return@launch
                }

                val rootUrl = "http://$host:8080/"
                val rootCode = runCatching {
                    val conn = (URL(rootUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 2500
                        readTimeout = 2500
                    }
                    conn.responseCode.also { conn.disconnect() }
                }.getOrElse {
                    _webBridgeSelfTestStatus.value = "FAIL: Port 8080 unreachable (${it.message ?: "network error"})"
                    return@launch
                }

                if (rootCode !in 200..399) {
                    _webBridgeSelfTestStatus.value = "FAIL: Unexpected bridge root response ($rootCode)"
                    return@launch
                }

                val pin = _webBridgePin.value.ifBlank { RabitNetworkServer.currentPin }
                val authCode = runCatching {
                    val conn = (URL("http://$host:8080/auth").openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 3000
                        readTimeout = 3000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("X-Device-Id", "rabit-self-test")
                    }
                    val payload = "{\"pin\":\"$pin\"}".toByteArray(Charsets.UTF_8)
                    conn.outputStream.use { it.write(payload) }
                    conn.responseCode.also { conn.disconnect() }
                }.getOrElse {
                    _webBridgeSelfTestStatus.value = "FAIL: /auth probe failed (${it.message ?: "network error"})"
                    return@launch
                }

                val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
                _webBridgeSelfTestStatus.value = if (authCode == 200) {
                    "PASS ($ts): Local HTTP API works properly"
                } else {
                    "FAIL: /auth returned $authCode (expected 200)"
                }

            } catch (e: Exception) {
                _webBridgeSelfTestStatus.value = "ERROR: ${e.message}"
            } finally {
                _webBridgeSelfTestInProgress.value = false
            }
        }
    }

    private fun signalingRoomForPin(pin: String): String {
        return "rabit_p2p_$pin"
    }

    fun startP2PHosting(forceRestart: Boolean = false) {
        val desiredRoom = signalingRoomForPin(_webBridgePin.value)
        if (!forceRestart && _p2pEnabled.value && webRtcManager.peerId.value == desiredRoom) {
            return
        }

        stopP2PHosting()
        _p2pEnabled.value = true
        webRtcManager.start(desiredRoom)
    }

    fun stopP2PHosting() {
        _p2pEnabled.value = false
        webRtcManager.stop()
    }

    fun addBridgeNote(text: String, source: String = "Hackie"): BridgeNote? {
        val normalized = text.trim()
        if (normalized.isBlank()) return null

        val note = BridgeNote(
            id = UUID.randomUUID().toString(),
            text = normalized,
            createdAtMs = System.currentTimeMillis(),
            source = source
        )
        
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.insertNote(NoteEntity(
                id = note.id,
                text = note.text,
                source = note.source,
                createdAtMs = note.createdAtMs
            ))
            sendNotesListToPeer()
        }
        return note
    }

    fun updateBridgeNote(noteId: String, text: String, source: String? = null): Boolean {
        val normalizedId = noteId.trim()
        val normalizedText = text.trim()
        if (normalizedId.isBlank() || normalizedText.isBlank()) return false

        viewModelScope.launch(Dispatchers.IO) {
            val existing = bridgeNotes.value.find { it.id == normalizedId }
            if (existing != null) {
                noteDao.updateNote(NoteEntity(
                    id = normalizedId,
                    text = normalizedText,
                    source = source?.takeIf { it.isNotBlank() } ?: existing.source,
                    createdAtMs = existing.createdAtMs
                ))
                sendNotesListToPeer()
            }
        }
        return true
    }

    fun deleteBridgeNote(noteId: String): Boolean {
        val normalizedId = noteId.trim()
        if (normalizedId.isBlank()) return false

        viewModelScope.launch(Dispatchers.IO) {
            noteDao.deleteById(normalizedId)
            sendNotesListToPeer()
        }
        return true
    }

    private fun sendNotesListToPeer() {
        viewModelScope.launch {
            val notes = bridgeNotes.value
            val array = JSONArray()
            notes.forEach { note ->
                array.put(JSONObject().apply {
                    put("id", note.id)
                    put("text", note.text)
                    put("createdAtMs", note.createdAtMs)
                    put("source", note.source)
                })
            }
            webRtcManager.sendData(JSONObject().apply {
                put("type", "NOTES_LIST")
                put("notes", array)
            }.toString())
        }
    }

    override fun onCleared() {
        super.onCleared()
        webRtcManager.stop()
    }
}
