package com.example.rabit.ui.helper

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.ui.ClipboardHelper
import com.example.rabit.data.bluetooth.BluetoothScanner
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.domain.model.RemoteFile
import com.example.rabit.data.storage.RemoteStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Properties
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import android.content.ComponentName
import android.content.pm.PackageManager
import android.provider.DocumentsContract
import android.util.Log
import com.example.rabit.domain.model.RemoteProcess
import com.example.rabit.domain.model.SystemStats
import kotlinx.coroutines.withContext

class HelperViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs: SharedPreferences = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
    private val clipboardManager = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private val _helperDeviceIp = MutableStateFlow("Unknown")
    val helperDeviceIp = _helperDeviceIp.asStateFlow()

    private val _helperDeviceMac = MutableStateFlow("Unknown")
    val helperDeviceMac = _helperDeviceMac.asStateFlow()

    private val _helperDeviceName = MutableStateFlow("Unknown")
    val helperDeviceName = _helperDeviceName.asStateFlow()

    private val _helperBaseUrl = MutableStateFlow(prefs.getString("helper_base_url", "") ?: "")
    val helperBaseUrl = _helperBaseUrl.asStateFlow()


    private val _isHelperConnected = MutableStateFlow(false)
    val isHelperConnected = _isHelperConnected.asStateFlow()

    private val _helperConnectionStatus = MutableStateFlow("Disconnected")
    val helperConnectionStatus = _helperConnectionStatus.asStateFlow()

    private val _terminalOutput = MutableStateFlow("")
    val terminalOutput = _terminalOutput.asStateFlow()

    private val _helperTransferEvents = MutableStateFlow<List<String>>(emptyList())
    val helperTransferEvents = _helperTransferEvents.asStateFlow()

    private val _helperAutoConnectStatus = MutableStateFlow("Auto-connect enabled")
    val helperAutoConnectStatus = _helperAutoConnectStatus.asStateFlow()

    private val _helperLastAutoDiscoverAt = MutableStateFlow("Never")
    val helperLastAutoDiscoverAt = _helperLastAutoDiscoverAt.asStateFlow()

    private val _helperRemoteFiles = MutableStateFlow<List<RemoteFile>>(emptyList())
    val helperRemoteFiles = _helperRemoteFiles.asStateFlow()

    private val _currentRemotePath = MutableStateFlow("/")
    val currentRemotePath = _currentRemotePath.asStateFlow()

    private val _localIp = MutableStateFlow("0.0.0.0")
    val localIp = _localIp.asStateFlow()

    // Native SSH Terminal
    private val _sshHost = MutableStateFlow(prefs.getString("ssh_host", "") ?: "")
    val sshHost = _sshHost.asStateFlow()
    private val _sshPort = MutableStateFlow(prefs.getInt("ssh_port", 22))
    val sshPort = _sshPort.asStateFlow()
    private val _sshUser = MutableStateFlow(prefs.getString("ssh_user", "") ?: "")
    val sshUser = _sshUser.asStateFlow()
    private val _sshPassword = MutableStateFlow(prefs.getString("ssh_password", "") ?: "")
    val sshPassword = _sshPassword.asStateFlow()
    private val _sshConnected = MutableStateFlow(false)
    val sshConnected = _sshConnected.asStateFlow()
    private val _sshTerminalLines = MutableStateFlow<List<String>>(listOf("Hackie SSH terminal ready."))
    val sshTerminalLines = _sshTerminalLines.asStateFlow()
    private val _sshStatus = MutableStateFlow("Disconnected")
    val sshStatus = _sshStatus.asStateFlow()

    private var sshSession: Session? = null
    private var sshChannel: ChannelShell? = null
    private var sshWriter: OutputStreamWriter? = null
    private var sshReaderJob: Job? = null
    private var sshCommandJob: Job? = null

    // Remote Explorer
    private val _remoteFiles = MutableStateFlow<List<RemoteFile>>(emptyList())
    val remoteFiles = _remoteFiles.asStateFlow()
    private val _isRemoteLoading = MutableStateFlow(false)
    val isRemoteLoading = _isRemoteLoading.asStateFlow()
    private val _remoteError = MutableStateFlow<String?>(null)
    val remoteError = _remoteError.asStateFlow()
    private val _isRemoteMounted = MutableStateFlow(false)
    val isRemoteMounted = _isRemoteMounted.asStateFlow()
    private val _remoteMountStatus = MutableStateFlow("")
    val remoteMountStatus = _remoteMountStatus.asStateFlow()
    private val _remoteSource = MutableStateFlow("SSH") // "SSH" or "ADB"
    val remoteSource = _remoteSource.asStateFlow()
    private val _filePreviewContent = MutableStateFlow<String?>(null)
    val filePreviewContent = _filePreviewContent.asStateFlow()
    private val _filePreviewName = MutableStateFlow("")
    val filePreviewName = _filePreviewName.asStateFlow()
    private val _downloadProgress = MutableStateFlow<String?>(null)
    val downloadProgress = _downloadProgress.asStateFlow()

    // Process Manager & Stats
    private val _remoteProcesses = MutableStateFlow<List<RemoteProcess>>(emptyList())
    val remoteProcesses = _remoteProcesses.asStateFlow()
    private val _isRefreshingProcesses = MutableStateFlow(false)
    val isRefreshingProcesses = _isRefreshingProcesses.asStateFlow()
    private val _systemStats = MutableStateFlow(SystemStats(0f, 0f, 0f, "Unknown"))
    val systemStats = _systemStats.asStateFlow()

    private var helperHealthPollJob: Job? = null
    private var helperAutoDiscoverJob: Job? = null
    private var clipboardSyncJob: Job? = null
    private var helperDiscoveryMulticastLock: WifiManager.MulticastLock? = null
    
    private var clipboardSyncEnabled = prefs.getBoolean("clipboard_sync_enabled", false)
    private var lastSentClipboardText = ""

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        if (!clipboardSyncEnabled) return@OnPrimaryClipChangedListener
        val clipboardContent = ClipboardHelper.getFromClipboard(getApplication())
        if (clipboardContent.isNotEmpty() && clipboardContent != lastSentClipboardText) {
            lastSentClipboardText = clipboardContent
            viewModelScope.launch(Dispatchers.IO) {
                pushClipboardToHelper(clipboardContent)
            }
        }
    }

    init {
        fetchHelperDeviceDetails()
        startHelperHealthPolling()
        startHelperAutoDiscoveryLoop()
        if (clipboardSyncEnabled) {
            startClipboardSyncLoop()
        }
    }

    private fun startHelperHealthPolling() {
        helperHealthPollJob?.cancel()
        helperHealthPollJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                fetchHelperDeviceDetails()
                delay(5000)
            }
        }
    }

    private fun startHelperAutoDiscoveryLoop() {
        helperAutoDiscoverJob?.cancel()
        helperAutoDiscoverJob = viewModelScope.launch(Dispatchers.IO) {
            delay(1500)
            while (isActive) {
                val baseMissing = _helperBaseUrl.value.isBlank()
                val disconnected = !_isHelperConnected.value
                if (baseMissing || disconnected) {
                    _helperLastAutoDiscoverAt.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(java.util.Date())
                    _helperAutoConnectStatus.value = "Auto-discovery: checking LAN beacon"
                    discoverHelperOnLocalWifi(fullScan = false, userInitiated = false)
                }
                delay(12000)
            }
        }
    }

    private fun normalizeHelperUrl(url: String): String {
        val trimmed = url.trim().removeSuffix("/")
        if (trimmed.isBlank()) return ""
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
    }

    private fun appendTransferEvent(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(java.util.Date())
        _helperTransferEvents.value = (_helperTransferEvents.value + "[$timestamp] $message").takeLast(40)
    }

    private fun postHelperCommand(command: String, payload: JSONObject = JSONObject()): JSONObject? {
        val base = _helperBaseUrl.value
        if (base.isBlank()) return null

        return try {
            payload.put("command", command)
            val conn = (URL("$base/command").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 2500
                readTimeout = 6000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            conn.outputStream.use { out ->
                out.write(payload.toString().toByteArray())
                out.flush()
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body)
        } catch (e: Exception) {
            _terminalOutput.value = "[Error] ${e.message ?: "Command failed"}"
            null
        }
    }

    fun setHelperBaseUrl(url: String) {
        val normalized = normalizeHelperUrl(url)
        _helperBaseUrl.value = normalized
        prefs.edit().putString("helper_base_url", normalized).apply()
        _helperConnectionStatus.value = if (normalized.isBlank()) "Disconnected" else "Endpoint saved"
    }


    fun resetHelperConnectionAndRescan() {
        _helperBaseUrl.value = ""
        prefs.edit().remove("helper_base_url").apply()
        _isHelperConnected.value = false
        _helperDeviceName.value = "Unknown"
        _helperDeviceIp.value = "Unknown"
        _helperDeviceMac.value = "Unknown"
        _helperConnectionStatus.value = "Reset complete, rescanning..."
        _helperAutoConnectStatus.value = "Manual reset: full LAN scan started"
        _helperLastAutoDiscoverAt.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(java.util.Date())
        discoverHelperOnLocalWifi(fullScan = true, userInitiated = true)
    }
    
    fun refreshLocalIp() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val intf = interfaces.nextElement()
                    val addrs = intf.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                            _localIp.value = addr.hostAddress ?: "0.0.0.0"
                            return@launch
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchHelperDeviceDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            val base = _helperBaseUrl.value
            if (base.isBlank()) {
                _isHelperConnected.value = false
                _helperDeviceName.value = "Unknown"
                _helperDeviceIp.value = "Unknown"
                _helperDeviceMac.value = "Unknown"
                _helperConnectionStatus.value = "No endpoint configured"
                return@launch
            }

            try {
                val conn = (URL("$base/info").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 4000
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                _helperDeviceName.value = json.optString("name", "Unknown")
                _helperDeviceIp.value = json.optString("ipAddress", "Unknown")
                _helperDeviceMac.value = json.optString("macAddress", "Unknown")
                _isHelperConnected.value = json.optString("status", "").equals("online", ignoreCase = true)
                _helperConnectionStatus.value = if (_isHelperConnected.value) "Connected" else "Endpoint reachable, helper offline"
            } catch (_: Exception) {
                _isHelperConnected.value = false
                _helperDeviceName.value = "Unknown"
                _helperDeviceIp.value = "Unknown"
                _helperDeviceMac.value = "Unknown"
                _helperConnectionStatus.value = "Connection failed"
            }
        }
    }

    fun discoverHelperOnLocalWifi(fullScan: Boolean = true, userInitiated: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val wifiManager = getApplication<Application>().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val local = _localIp.value
            if (local == "0.0.0.0" || !local.contains(".")) {
                refreshLocalIp()
                delay(100) // Wait briefly for IP to refresh
            }

            helperDiscoveryMulticastLock?.let {
                if (it.isHeld) it.release()
            }
            helperDiscoveryMulticastLock = wifiManager.createMulticastLock("rabit_helper_discovery").apply {
                setReferenceCounted(false)
            }

            try {
                helperDiscoveryMulticastLock?.acquire()
                try {
                    DatagramSocket(8766).use { socket ->
                        socket.soTimeout = 3000
                        socket.reuseAddress = true
                        val buf = ByteArray(2048)
                        val packet = DatagramPacket(buf, buf.size)
                        socket.receive(packet)
                        val jsonText = String(packet.data, 0, packet.length)
                        val payload = JSONObject(jsonText)
                        if (payload.optString("type") == "hackie_helper_beacon") {
                            val ip = payload.optString("ipAddress", packet.address.hostAddress ?: "")
                            val port = payload.optInt("port", 8765)
                            if (ip.isNotBlank()) {
                                val url = "http://$ip:$port"
                                setHelperBaseUrl(url)
                                fetchHelperDeviceDetails()
                                if (!userInitiated) {
                                    _helperAutoConnectStatus.value = "Auto-discovery: helper found via beacon"
                                }
                                if (userInitiated) {
                                    _terminalOutput.value = "Helper found via beacon: $url"
                                }
                                return@launch
                            }
                        }
                    }
                } catch (_: Exception) {}
            } finally {
                try {
                    helperDiscoveryMulticastLock?.release()
                } catch (_: Exception) {}
            }

            if (!fullScan) {
                if (!userInitiated) {
                    _helperAutoConnectStatus.value = "Auto-discovery: beacon not found"
                }
                if (userInitiated) {
                    _terminalOutput.value = "No beacon detected. Try full LAN scan."
                }
                return@launch
            }

            val currentLocalIp = _localIp.value
            val baseIp = currentLocalIp.substringBeforeLast('.', "")
            if (baseIp.isBlank() || currentLocalIp == "0.0.0.0") return@launch

            if (userInitiated) {
                _terminalOutput.value = "Scanning local network for Helper..."
            }
            var foundUrl: String? = null

            for (i in 1..254) {
                if (!isActive) break
                val host = "$baseIp.$i"
                val candidate = "http://$host:8765"
                try {
                    val conn = (URL("$candidate/health").openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 180
                        readTimeout = 250
                    }
                    val code = conn.responseCode
                    if (code == 200) {
                        foundUrl = candidate
                        break
                    }
                } catch (_: Exception) {}
            }

            if (foundUrl != null) {
                setHelperBaseUrl(foundUrl)
                fetchHelperDeviceDetails()
                if (!userInitiated) {
                    _helperAutoConnectStatus.value = "Auto-discovery: helper found by scan"
                }
                if (userInitiated) {
                    _terminalOutput.value = "Helper found on local Wi-Fi: $foundUrl"
                }
                appendTransferEvent("Helper discovered on LAN: $foundUrl")
            } else {
                if (!userInitiated) {
                    _helperAutoConnectStatus.value = "Auto-discovery: no helper found"
                }
                if (userInitiated) {
                    _terminalOutput.value = "No local helper found. Use internet URL manually (public IP/DDNS)."
                }
                _helperConnectionStatus.value = "LAN scan failed"
            }
        }
    }

    fun runRemoteShellCommand(cmd: String) {
        if (cmd.isBlank()) return
        _terminalOutput.value = "Running command..."
        viewModelScope.launch(Dispatchers.IO) {
            val response = postHelperCommand("run_shell", JSONObject().put("cmd", cmd)) ?: return@launch
            val stdout = response.optString("stdout", "")
            val stderr = response.optString("stderr", "")
            _terminalOutput.value = buildString {
                if (stdout.isNotBlank()) append(stdout)
                if (stderr.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append("[Error] $stderr")
                }
            }
            appendTransferEvent("Shell command executed")
        }
    }

    fun listRemoteFiles(path: String = _currentRemotePath.value) {
        val base = _helperBaseUrl.value
        if (base.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
                val conn = (URL("$base/files?path=$encodedPath").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 5000
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                _currentRemotePath.value = json.optString("path", path)
                val items = json.optJSONArray("items") ?: JSONArray()
                val files = mutableListOf<RemoteFile>()
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    files += RemoteFile(
                        name = item.optString("name", "Unknown"),
                        path = item.optString("path", "/"),
                        isDirectory = item.optBoolean("isDir", false)
                    )
                }
                _helperRemoteFiles.value = files.sortedWith(
                    compareByDescending<RemoteFile> { it.isDirectory }.thenBy { it.name.lowercase(Locale.getDefault()) }
                )
                _terminalOutput.value = "Loaded ${files.size} remote items"
                appendTransferEvent("Remote file list fetched")
            } catch (e: Exception) {
                _terminalOutput.value = "[Error] ${e.message ?: "File listing failed"}"
                _helperRemoteFiles.value = emptyList()
                appendTransferEvent("Remote file list failed")
            }
        }
    }

    fun listParentRemoteFiles() {
        val current = _currentRemotePath.value
        val normalized = current.trimEnd('/', '\\')
        if (normalized.isBlank() || normalized == "/") {
            listRemoteFiles("/")
            return
        }
        if (normalized.length <= 2 && normalized.endsWith(":")) {
            listRemoteFiles("$normalized\\")
            return
        }
        val slashIndex = maxOf(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\'))
        if (slashIndex <= 0) {
            listRemoteFiles("/")
            return
        }
        listRemoteFiles(normalized.substring(0, slashIndex + 1))
    }

    private fun queryDisplayName(uri: Uri): String? {
        var name: String? = null
        try {
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) name = cursor.getString(index)
                }
            }
        } catch (_: Exception) {}
        return name
    }

    fun sendFileToHelper(uri: Uri) {
        val base = _helperBaseUrl.value
        if (base.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val name = queryDisplayName(uri) ?: "shared_${System.currentTimeMillis()}"
                val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw java.lang.IllegalStateException("Cannot read selected file")

                val conn = (URL("$base/upload?name=${java.net.URLEncoder.encode(name, "UTF-8")}").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 3000
                    readTimeout = 15000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/octet-stream")
                    setRequestProperty("Content-Length", bytes.size.toString())
                }
                conn.outputStream.use { out ->
                    out.write(bytes)
                    out.flush()
                }
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                _terminalOutput.value = "Upload complete: $response"
                appendTransferEvent("Upload complete: $name")
            } catch (e: Exception) {
                _terminalOutput.value = "[Error] ${e.message ?: "Upload failed"}"
                appendTransferEvent("Upload failed")
            }
        }
    }

    fun setClipboardSyncState(sync: Boolean) {
        clipboardSyncEnabled = sync
        prefs.edit().putBoolean("clipboard_sync_enabled", sync).apply()

        if (sync) {
            startClipboardSyncLoop()
        } else {
            stopClipboardSyncLoop()
        }

        if (sync) {
            val clipboardContent = ClipboardHelper.getFromClipboard(getApplication())
            if (clipboardContent.isNotEmpty()) {
                lastSentClipboardText = clipboardContent
                viewModelScope.launch(Dispatchers.IO) {
                    pushClipboardToHelper(clipboardContent)
                }
            }
        }
    }

    private fun startClipboardSyncLoop() {
        clipboardSyncJob?.cancel()
        runCatching {
            clipboardManager.addPrimaryClipChangedListener(clipboardListener)
        }

        clipboardSyncJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val base = _helperBaseUrl.value
                if (base.isNotBlank() && _isHelperConnected.value) {
                    try {
                        val conn = (URL("$base/clipboard").openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 1500
                            readTimeout = 2000
                        }
                        if (conn.responseCode == 200) {
                            val body = conn.inputStream.bufferedReader().use { it.readText() }
                            val json = JSONObject(body)
                            val helperClipboard = json.optString("text", json.optString("content", ""))
                            if (helperClipboard.isNotBlank() && helperClipboard != lastSentClipboardText) {
                                val currentLocal = ClipboardHelper.getFromClipboard(getApplication())
                                if (helperClipboard != currentLocal) {
                                    lastSentClipboardText = helperClipboard
                                    ClipboardHelper.copyToClipboard(getApplication(), helperClipboard)
                                    appendTransferEvent("Copied from helper")
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                delay(3000)
            }
        }
    }

    private fun stopClipboardSyncLoop() {
        clipboardSyncJob?.cancel()
        clipboardSyncJob = null
        runCatching {
            clipboardManager.removePrimaryClipChangedListener(clipboardListener)
        }
    }

    private suspend fun pushClipboardToHelper(text: String) {
        val base = _helperBaseUrl.value
        if (base.isBlank()) return
        try {
            val payload = JSONObject().put("text", text)
            val conn = (URL("$base/clipboard").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 2000
                readTimeout = 3000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            conn.outputStream.use { out ->
                out.write(payload.toString().toByteArray())
                out.flush()
            }
            if (conn.responseCode == 200) {
                appendTransferEvent("Clipboard pushed to helper")
            }
        } catch (_: Exception) {}
    }
    
    fun getGuessedSshUser(host: String): String {
        val history = prefs.getString("ssh_history_json", "{}") ?: "{}"
        return try {
            val json = JSONObject(history)
            json.optString(host, prefs.getString("ssh_user", "") ?: "")
        } catch (e: Exception) {
            ""
        }
    }

    fun saveSshHistory(host: String, user: String) {
        val history = prefs.getString("ssh_history_json", "{}") ?: "{}"
        try {
            val json = JSONObject(history)
            json.put(host, user)
            prefs.edit().putString("ssh_history_json", json.toString()).apply()
            
            // Also update global defaults
            prefs.edit().putString("ssh_host", host).apply()
            prefs.edit().putString("ssh_user", user).apply()
        } catch (e: Exception) {
        }
    }

    fun setSshHost(value: String) {
        _sshHost.value = value
        prefs.edit().putString("ssh_host", value).apply()
    }

    fun setSshPort(value: Int) {
        val clamped = value.coerceIn(1, 65535)
        _sshPort.value = clamped
        prefs.edit().putInt("ssh_port", clamped).apply()
    }

    fun setSshUser(value: String) {
        _sshUser.value = value
        prefs.edit().putString("ssh_user", value).apply()
    }

    fun setSshPassword(value: String) {
        _sshPassword.value = value
        prefs.edit().putString("ssh_password", value).apply()
    }

    fun connectSsh() {
        if (_sshConnected.value) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appendTerminalLine("Connecting to ${_sshHost.value}:${_sshPort.value} ...")
                _sshStatus.value = "Connecting"
                val jsch = JSch()
                val session = jsch.getSession(_sshUser.value, _sshHost.value, _sshPort.value)
                session.setPassword(_sshPassword.value)
                val config = Properties().apply { put("StrictHostKeyChecking", "no") }
                session.setConfig(config)
                session.connect(10_000)
                
                // Track as successful host/user pair
                saveSshHistory(_sshHost.value, _sshUser.value)

                val channel = session.openChannel("shell") as ChannelShell
                channel.setPty(true)
                val input = channel.inputStream
                val writer = OutputStreamWriter(channel.outputStream)
                channel.connect(8_000)

                sshSession = session
                sshChannel = channel
                sshWriter = writer
                _sshConnected.value = true
                _sshStatus.value = "Connected"
                appendTerminalLine("Connected. Type commands below.")

                sshReaderJob?.cancel()
                sshReaderJob = viewModelScope.launch(Dispatchers.IO) {
                    val buffer = ByteArray(1024)
                    while (channel.isConnected) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        val chunk = String(buffer, 0, read)
                        chunk.lines().filter { it.isNotBlank() }.forEach { appendTerminalLine(it) }
                    }
                }
            } catch (e: Exception) {
                _sshStatus.value = "Connection failed"
                appendTerminalLine("SSH error: ${e.message}")
                disconnectSsh()
            }
        }
    }

    fun sendSshCommand(command: String) {
        if (command.isBlank()) return
        if (!_sshConnected.value) {
            appendTerminalLine("Not connected. Connect first.")
            return
        }
        sshCommandJob?.cancel()
        sshCommandJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                appendTerminalLine("$ $command")
                val session = sshSession
                if (session == null || !session.isConnected) {
                    appendTerminalLine("Session dropped. Reconnect SSH.")
                    _sshConnected.value = false
                    _sshStatus.value = "Disconnected"
                    return@launch
                }

                val exec = session.openChannel("exec") as ChannelExec
                exec.setCommand(command)
                exec.setPty(true)
                exec.inputStream = null
                val stderrBuffer = ByteArrayOutputStream()
                exec.setErrStream(stderrBuffer)
                val stdout = exec.inputStream
                exec.connect(8_000)

                val outText = stdout.readBytes().toString(Charsets.UTF_8)
                val errText = stderrBuffer.toString(Charsets.UTF_8.name())
                if (outText.isNotBlank()) {
                    outText.lines().filter { it.isNotBlank() }.forEach { appendTerminalLine(it) }
                }
                if (errText.isNotBlank()) {
                    errText.lines().filter { it.isNotBlank() }.forEach { appendTerminalLine("ERR: $it") }
                }
                appendTerminalLine("[exit ${exec.exitStatus}]")
                exec.disconnect()
            } catch (e: Exception) {
                appendTerminalLine("Send failed: ${e.message}")
            }
        }
    }

    fun navigateRemote(path: String) {
        _currentRemotePath.value = path
        refreshRemoteFiles()
    }

    fun refreshRemoteFiles() {
        val source = _remoteSource.value
        if (source == "ADB") {
            refreshAdbFiles()
            return
        }
        if (!_sshConnected.value) return
        _isRemoteLoading.value = true
        _remoteError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val path = _currentRemotePath.value
                val output = executeSshCommandSilent("ls -lAh \"$path\" 2>/dev/null")
                val lines = output.lines().filter { it.isNotBlank() && !it.startsWith("total ") }
                
                val files = lines.mapNotNull { line ->
                    // Parse ls -lAh output: drwxr-xr-x  2 user group  4.0K Jan 01 12:00 filename
                    val parts = line.split("\\s+".toRegex(), limit = 9)
                    if (parts.size < 9) return@mapNotNull null
                    val perms = parts[0]
                    val isDir = perms.startsWith("d")
                    val sizeStr = parts[4]
                    val name = parts[8]
                    if (name == "." || name == "..") return@mapNotNull null
                    
                    val size = parseHumanSize(sizeStr)
                    
                    RemoteFile(
                        name = name,
                        path = if (path.endsWith("/")) "$path$name" else "$path/$name",
                        isDirectory = isDir,
                        extension = if (!isDir) name.substringAfterLast(".", "") else "",
                        size = size,
                        modifiedTime = System.currentTimeMillis()
                    )
                }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                
                _remoteFiles.value = files
            } catch (e: Exception) {
                _remoteError.value = e.message
            } finally {
                _isRemoteLoading.value = false
            }
        }
    }

    private fun parseHumanSize(s: String): Long {
        val clean = s.uppercase().trim()
        return try {
            when {
                clean.endsWith("K") -> (clean.removeSuffix("K").toDouble() * 1024).toLong()
                clean.endsWith("M") -> (clean.removeSuffix("M").toDouble() * 1048576).toLong()
                clean.endsWith("G") -> (clean.removeSuffix("G").toDouble() * 1073741824).toLong()
                clean.endsWith("T") -> (clean.removeSuffix("T").toDouble() * 1099511627776).toLong()
                else -> clean.toLongOrNull() ?: 0L
            }
        } catch (_: Exception) { 0L }
    }

    fun downloadRemoteFile(file: RemoteFile, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _downloadProgress.value = "Downloading ${file.name}..."
                
                if (_remoteSource.value == "ADB") {
                    // ADB download via adb pull
                    val client = RemoteStorageManager.adbClient
                    if (client != null) {
                        val output = client.executeCommand("cat \"${file.path}\"")
                        if (output.isNotBlank()) {
                            val resolver = context.contentResolver
                            val values = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, file.name)
                                put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                                put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/Hackie")
                            }
                            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                            uri?.let { resolver.openOutputStream(it)?.use { out -> out.write(output.toByteArray()) } }
                            _downloadProgress.value = "✓ Saved to Downloads/Hackie/${file.name}"
                        }
                    }
                } else {
                    // SSH download via SCP/cat
                    val localFile = RemoteStorageManager.readFile(file.path)
                    if (localFile != null && localFile.exists()) {
                        val resolver = context.contentResolver
                        val values = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.Downloads.DISPLAY_NAME, file.name)
                            put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                            put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/Hackie")
                        }
                        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        if (uri != null) {
                            resolver.openOutputStream(uri)?.use { out ->
                                java.io.FileInputStream(localFile).use { fis -> fis.copyTo(out) }
                            }
                            _downloadProgress.value = "✓ Saved to Downloads/Hackie/${file.name}"
                        }
                    } else {
                        _downloadProgress.value = "✗ Download failed"
                    }
                }
                appendTerminalLine("Downloaded: ${file.name} → Downloads/Hackie/")
                delay(3000)
                _downloadProgress.value = null
            } catch (e: Exception) {
                _downloadProgress.value = "✗ ${e.message}"
                appendTerminalLine("Download error: ${e.message}")
                delay(3000)
                _downloadProgress.value = null
            }
        }
    }

    fun previewRemoteFile(file: RemoteFile) {
        _filePreviewContent.value = null
        _filePreviewName.value = file.name
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = if (_remoteSource.value == "ADB") {
                    val client = RemoteStorageManager.adbClient ?: throw Exception("ADB not connected")
                    client.executeCommand("cat \"${file.path}\"")
                } else {
                    executeSshCommandSilent("head -c 100000 \"${file.path}\"")
                }
                _filePreviewContent.value = content
            } catch (e: Exception) {
                _filePreviewContent.value = "[Error reading file: ${e.message}]"
            }
        }
    }

    fun closePreview() {
        _filePreviewContent.value = null
        _filePreviewName.value = ""
    }

    fun deleteRemoteFile(file: RemoteFile) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cmd = if (file.isDirectory) "rm -rf \"${file.path}\"" else "rm -f \"${file.path}\""
                if (_remoteSource.value == "ADB") {
                    RemoteStorageManager.adbClient?.executeCommand(cmd)
                } else {
                    executeSshCommandSilent(cmd)
                }
                appendTerminalLine("Deleted: ${file.name}")
                refreshRemoteFiles()
            } catch (e: Exception) {
                appendTerminalLine("Delete failed: ${e.message}")
            }
        }
    }

    fun renameRemoteFile(file: RemoteFile, newName: String) {
        if (newName.isBlank()) return
        val parentPath = file.path.substringBeforeLast("/")
        val newPath = "$parentPath/$newName"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cmd = "mv \"${file.path}\" \"$newPath\""
                if (_remoteSource.value == "ADB") {
                    RemoteStorageManager.adbClient?.executeCommand(cmd)
                } else {
                    executeSshCommandSilent(cmd)
                }
                appendTerminalLine("Renamed: ${file.name} → $newName")
                refreshRemoteFiles()
            } catch (e: Exception) {
                appendTerminalLine("Rename failed: ${e.message}")
            }
        }
    }

    fun createRemoteFolder(name: String) {
        if (name.isBlank()) return
        val path = _currentRemotePath.value
        val fullPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cmd = "mkdir -p \"$fullPath\""
                if (_remoteSource.value == "ADB") {
                    RemoteStorageManager.adbClient?.executeCommand(cmd)
                } else {
                    executeSshCommandSilent(cmd)
                }
                appendTerminalLine("Created folder: $name")
                refreshRemoteFiles()
            } catch (e: Exception) {
                appendTerminalLine("Mkdir failed: ${e.message}")
            }
        }
    }

    fun switchRemoteSource(source: String) {
        _remoteSource.value = source
        _remoteFiles.value = emptyList()
        _remoteError.value = null
        if (source == "ADB") {
            _currentRemotePath.value = "/sdcard"
        } else {
            _currentRemotePath.value = "/"
        }
        refreshRemoteFiles()
    }

    private fun refreshAdbFiles() {
        val client = RemoteStorageManager.adbClient
        if (client == null) {
            _remoteError.value = "ADB not connected. Connect via ADB Manager first."
            return
        }
        _isRemoteLoading.value = true
        _remoteError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val path = _currentRemotePath.value
                val output = client.executeCommand("ls -lAh \"$path\" 2>/dev/null")
                val lines = output.lines().filter { it.isNotBlank() && !it.startsWith("total ") }

                val files = lines.mapNotNull { line ->
                    val parts = line.split("\\s+".toRegex(), limit = 9)
                    if (parts.size < 9) return@mapNotNull null
                    val perms = parts[0]
                    val isDir = perms.startsWith("d")
                    val sizeStr = parts[4]
                    val name = parts[8]
                    if (name == "." || name == "..") return@mapNotNull null

                    RemoteFile(
                        name = name,
                        path = if (path.endsWith("/")) "$path$name" else "$path/$name",
                        isDirectory = isDir,
                        extension = if (!isDir) name.substringAfterLast(".", "") else "",
                        size = parseHumanSize(sizeStr),
                        modifiedTime = System.currentTimeMillis()
                    )
                }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

                _remoteFiles.value = files
            } catch (e: Exception) {
                _remoteError.value = "ADB Error: ${e.message}"
            } finally {
                _isRemoteLoading.value = false
            }
        }
    }

    fun goUpRemote() {
        val path = _currentRemotePath.value.trimEnd('/')
        if (path.isBlank() || path == "/" || path == "/sdcard") return
        val parent = path.substringBeforeLast("/")
        navigateRemote(if (parent.isBlank()) "/" else parent)
    }

    fun toggleRemoteMount() {
        if (_isRemoteMounted.value) {
            unmountRemoteStorage()
        } else {
            mountRemoteStorage()
        }
    }

    fun mountRemoteStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _remoteMountStatus.value = "Mounting..."
                val app = getApplication<Application>()

                val prefs = app.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("helper_base_url", _helperBaseUrl.value)
                    .apply()

                RemoteStorageManager.mount(app)

                val componentName = ComponentName(app, com.example.rabit.data.storage.RabitRemoteDocumentsProvider::class.java)
                app.packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )

                val rootsUri = DocumentsContract.buildRootsUri("${app.packageName}.remote.documents")
                app.contentResolver.notifyChange(rootsUri, null)

                _isRemoteMounted.value = true
                _remoteMountStatus.value = "Mounted — visible in Files app"
                appendTerminalLine("Remote storage mounted. Open Files app → Hackie Remote to browse.")
            } catch (e: Exception) {
                _remoteMountStatus.value = "Mount failed: ${e.message}"
                _isRemoteMounted.value = false
                appendTerminalLine("Mount failed: ${e.message}")
            }
        }
    }

    fun unmountRemoteStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()

                RemoteStorageManager.unmount()

                val componentName = ComponentName(app, com.example.rabit.data.storage.RabitRemoteDocumentsProvider::class.java)
                app.packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )

                val rootsUri = DocumentsContract.buildRootsUri("${app.packageName}.remote.documents")
                app.contentResolver.notifyChange(rootsUri, null)

                _isRemoteMounted.value = false
                _remoteMountStatus.value = ""
                appendTerminalLine("Remote storage unmounted.")
            } catch (e: Exception) {
                _remoteMountStatus.value = "Unmount error: ${e.message}"
            }
        }
    }

    suspend fun executeSshCommandSilent(command: String): String = withContext(Dispatchers.IO) {
        val session = sshSession ?: throw Exception("Not connected")
        if (!session.isConnected) throw Exception("Session disconnected")
        
        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(command)
        val input = channel.inputStream
        channel.connect(5000)
        
        val result = input.bufferedReader().readText()
        channel.disconnect()
        result
    }

    fun disconnectSsh() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                sshWriter?.apply {
                    write("exit\n")
                    flush()
                }
            }
            sshReaderJob?.cancel()
            sshReaderJob = null
            sshCommandJob?.cancel()
            sshCommandJob = null
            runCatching { sshChannel?.disconnect() }
            runCatching { sshSession?.disconnect() }
            sshChannel = null
            sshSession = null
            sshWriter = null
            _sshConnected.value = false
            _sshStatus.value = "Disconnected"
        }
    }

    fun clearSshTerminal() {
        _sshTerminalLines.value = listOf("Hackie SSH terminal cleared.")
    }

    fun appendTerminalLine(line: String) {
        val clean = line.trimEnd()
        if (clean.isBlank()) return
        val newList = (_sshTerminalLines.value + clean).takeLast(500)
        _sshTerminalLines.value = newList
    }
    
    fun fetchRemoteProcesses() {
        if (!_sshConnected.value) return
        _isRefreshingProcesses.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val output = executeSshCommandSilent("ps -eo pid,pcpu,pmem,user,comm -r | head -n 100")
                val lines = output.lines()
                val list = mutableListOf<RemoteProcess>()
                lines.drop(1).forEach { line ->
                    if (line.isBlank()) return@forEach
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size >= 5) {
                        runCatching {
                            list.add(RemoteProcess(
                                pid = parts[0].toInt(),
                                cpu = parts[1].toDouble(),
                                mem = parts[2].toDouble(),
                                user = parts[3],
                                command = parts.drop(4).joinToString(" ")
                            ))
                        }
                    }
                }
                _remoteProcesses.value = list
            } catch (e: Exception) {
                Log.e("HelperViewModel", "Failed to fetch processes: ${e.message}")
            } finally {
                _isRefreshingProcesses.value = false
            }
        }
    }

    fun killRemoteProcess(pid: Int) {
        viewModelScope.launch {
            executeSshCommandSilent("kill -9 $pid")
            delay(500)
            fetchRemoteProcesses()
        }
    }

    fun fetchSystemStats() {
        if (!_sshConnected.value) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cpu = executeSshCommandSilent("top -l 1 | grep 'CPU usage' | awk '{print $3}' | tr -d '%'").toDoubleOrNull() ?: 0.0
                val mem = executeSshCommandSilent("top -l 1 | grep 'PhysMem' | awk '{print $2}' | tr -d 'G'").toDoubleOrNull() ?: 0.0
                val disk = executeSshCommandSilent("df -h / | tail -1 | awk '{print $5}' | tr -d '%'").toDoubleOrNull() ?: 0.0
                val uptime = executeSshCommandSilent("uptime | awk -F'up ' '{print $2}' | awk -F',' '{print $1}'").trim()
                _systemStats.value = SystemStats(cpu.toFloat(), mem.toFloat(), disk.toFloat(), uptime)
            } catch (e: Exception) {
                Log.e("HelperVM", "Failed to fetch stats", e)
            }
        }
    }

    fun ensurePhoneHelperReceiverRunning() {
        // Stub — WebRTC manager lifecycle is owned by MainViewModel.
    }
    
    override fun onCleared() {
        super.onCleared()
        stopClipboardSyncLoop()
        helperHealthPollJob?.cancel()
        helperAutoDiscoverJob?.cancel()
    }
}
