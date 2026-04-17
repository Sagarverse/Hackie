package com.example.rabit.ui.automation

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.adb.RabitAdbClient
import com.example.rabit.data.adb.RabitAdbCrypto
import com.example.rabit.data.adb.UsbAdbManager
import com.example.rabit.domain.model.HidKeyCodes
import com.example.rabit.domain.model.*
import com.example.rabit.domain.repository.KeyboardRepository
import com.example.rabit.data.repository.KeyboardRepositoryImpl
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sign
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import kotlinx.coroutines.isActive


class AutomationViewModel(
    application: Application,
    private val repository: KeyboardRepository
) : AndroidViewModel(application) {

    private val gson = Gson()
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)

    // ADB Components
    val adbClient = RabitAdbClient(RabitAdbCrypto.getCrypto(application))
    val usbAdbManager = UsbAdbManager(application)

    // --- State Flows ---

    private val _customMacros = MutableStateFlow<List<CustomMacro>>(loadCustomMacros())
    val customMacros: StateFlow<List<CustomMacro>> = _customMacros.asStateFlow()

    private val _emergencyStatus = MutableStateFlow("Ready")
    val emergencyStatus: StateFlow<String> = _emergencyStatus.asStateFlow()

    private val _isTerminalScanning = MutableStateFlow(false)
    val isTerminalScanning: StateFlow<Boolean> = _isTerminalScanning.asStateFlow()

    // Auto Clicker
    enum class ClickTimeUnit { MS, SEC, MIN }
    private val _isAutoClicking = MutableStateFlow(false)
    val isAutoClicking: StateFlow<Boolean> = _isAutoClicking.asStateFlow()
    private val _autoClickInterval = MutableStateFlow(1000L)
    val autoClickInterval: StateFlow<Long> = _autoClickInterval.asStateFlow()
    private val _autoClickUnit = MutableStateFlow(ClickTimeUnit.MS)
    val autoClickUnit: StateFlow<ClickTimeUnit> = _autoClickUnit.asStateFlow()
    private val _autoClickLoops = MutableStateFlow(0)
    val autoClickLoops: StateFlow<Int> = _autoClickLoops.asStateFlow()
    private val _currentClickCount = MutableStateFlow(0)
    val currentClickCount: StateFlow<Int> = _currentClickCount.asStateFlow()
    private var autoClickJob: kotlinx.coroutines.Job? = null

    private val _terminalScanProgress = MutableStateFlow(0f)
    val terminalScanProgress: StateFlow<Float> = _terminalScanProgress.asStateFlow()

    private val _scannedTerminals = MutableStateFlow<List<TerminalDevice>>(emptyList())
    val scannedTerminals: StateFlow<List<TerminalDevice>> = _scannedTerminals.asStateFlow()

    private val _reverseShellLines = MutableStateFlow<List<String>>(emptyList())
    val reverseShellLines: StateFlow<List<String>> = _reverseShellLines.asStateFlow()

    private val _reverseShellStatus = MutableStateFlow("Listener Stopped")
    val reverseShellStatus: StateFlow<String> = _reverseShellStatus.asStateFlow()

    private val _reverseShellConnected = MutableStateFlow(false)
    val reverseShellConnected: StateFlow<Boolean> = _reverseShellConnected.asStateFlow()

    private val _isReverseShellListening = MutableStateFlow(false)
    val isReverseShellListening: StateFlow<Boolean> = _isReverseShellListening.asStateFlow()

    // WOL State
    private val _wolMacAddress = MutableStateFlow(prefs.getString("wol_mac_address", "") ?: "")
    val wolMacAddress: StateFlow<String> = _wolMacAddress.asStateFlow()
    
    private val _wolBroadcastIp = MutableStateFlow(prefs.getString("wol_broadcast_ip", "255.255.255.255") ?: "255.255.255.255")
    val wolBroadcastIp: StateFlow<String> = _wolBroadcastIp.asStateFlow()
    
    private val _wolPort = MutableStateFlow(prefs.getInt("wol_port", 9))
    val wolPort: StateFlow<Int> = _wolPort.asStateFlow()
    
    private val _wolStatus = MutableStateFlow("Idle")
    val wolStatus: StateFlow<String> = _wolStatus.asStateFlow()

    // --- Macro Management ---

    fun addCustomMacro(name: String, command: String) {
        val newList = _customMacros.value + CustomMacro(name = name, command = command)
        _customMacros.value = newList
        saveCustomMacros(newList)
    }

    fun deleteCustomMacro(macro: CustomMacro) {
        val newList = _customMacros.value.filter { it != macro }
        _customMacros.value = newList
        saveCustomMacros(newList)
    }

    private fun saveCustomMacros(macros: List<CustomMacro>) {
        val json = gson.toJson(macros)
        prefs.edit().putString("custom_macros_v2", json).apply()
    }

    private fun loadCustomMacros(): List<CustomMacro> {
        val json = prefs.getString("custom_macros_v2", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CustomMacro>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Macro Execution Engine ---

    fun runCustomMacro(macro: CustomMacro) {
        executeMacro2Script(macro.command, macro.cooldownMs)
    }

    fun executeMacro2Script(script: String, cooldownMs: Long = 0L) {
        val commands = script
            .split("\n", "&&")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        fun extractArg(cmd: String, keyword: String): String? {
            val normalized = cmd.trim()
            val prefix = "$keyword("
            if (!normalized.startsWith(prefix, ignoreCase = true) || !normalized.endsWith(")")) return null
            return normalized.substring(prefix.length, normalized.length - 1)
        }

        viewModelScope.launch {
            for (cmd in commands) {
                when {
                    extractArg(cmd, "WAIT") != null -> {
                        val ms = extractArg(cmd, "WAIT")?.trim()?.toLongOrNull() ?: 120L
                        delay(ms.coerceIn(0L, 60_000L))
                    }
                    extractArg(cmd, "TEXT") != null -> {
                        repository.sendText(extractArg(cmd, "TEXT") ?: "")
                    }
                    extractArg(cmd, "KEY") != null -> {
                        executeKeyCombo(extractArg(cmd, "KEY") ?: "")
                    }
                    extractArg(cmd, "MEDIA") != null -> {
                        executeSpecialKey(extractArg(cmd, "MEDIA") ?: "")
                    }
                    else -> {
                        repository.sendText(cmd)
                        repository.sendKey(HidKeyCodes.KEY_ENTER)
                    }
                }
                delay(120)
            }
            if (cooldownMs > 0) delay(cooldownMs)
        }
    }

    private fun executeKeyCombo(combo: String) {
        val keys = combo.split("+").map { it.trim().uppercase() }
        var modifiers = 0.toByte()
        var mainKey = HidKeyCodes.KEY_NONE

        keys.forEach { key ->
            when (key) {
                "GUI", "CMD", "WIN", "COMMAND", "SUPER" -> modifiers = (modifiers.toInt() or HidKeyCodes.MODIFIER_LEFT_GUI.toInt()).toByte()
                "SHIFT" -> modifiers = (modifiers.toInt() or HidKeyCodes.MODIFIER_LEFT_SHIFT.toInt()).toByte()
                "ALT", "OPTION", "OPT" -> modifiers = (modifiers.toInt() or HidKeyCodes.MODIFIER_LEFT_ALT.toInt()).toByte()
                "CTRL", "CONTROL" -> modifiers = (modifiers.toInt() or HidKeyCodes.MODIFIER_LEFT_CTRL.toInt()).toByte()
                "SPACE" -> mainKey = HidKeyCodes.KEY_SPACE
                "ENTER", "RETURN" -> mainKey = HidKeyCodes.KEY_ENTER
                "ESC", "ESCAPE" -> mainKey = HidKeyCodes.KEY_ESC
                "TAB" -> mainKey = HidKeyCodes.KEY_TAB
                "BACKSPACE", "DELETE", "DEL" -> mainKey = HidKeyCodes.KEY_BACKSPACE
                "CAPSLOCK", "CAPS" -> mainKey = HidKeyCodes.KEY_CAPS_LOCK
                "LEFT" -> mainKey = HidKeyCodes.KEY_LEFT
                "RIGHT" -> mainKey = HidKeyCodes.KEY_RIGHT
                "UP" -> mainKey = HidKeyCodes.KEY_UP
                "DOWN" -> mainKey = HidKeyCodes.KEY_DOWN
                "F1" -> mainKey = HidKeyCodes.KEY_F1
                "F2" -> mainKey = HidKeyCodes.KEY_F2
                "F3" -> mainKey = HidKeyCodes.KEY_F3
                "F4" -> mainKey = HidKeyCodes.KEY_F4
                "F5" -> mainKey = HidKeyCodes.KEY_F5
                "F6" -> mainKey = HidKeyCodes.KEY_F6
                "F7" -> mainKey = HidKeyCodes.KEY_F7
                "F8" -> mainKey = HidKeyCodes.KEY_F8
                "F9" -> mainKey = HidKeyCodes.KEY_F9
                "F10" -> mainKey = HidKeyCodes.KEY_F10
                "F11" -> mainKey = HidKeyCodes.KEY_F11
                "F12" -> mainKey = HidKeyCodes.KEY_F12
                else -> {
                    if (key.length == 1 && key[0] in 'A'..'Z') {
                        mainKey = (HidKeyCodes.KEY_A + (key[0] - 'A')).toByte()
                    } else if (key.length == 1 && key[0] in '0'..'9') {
                        mainKey = if (key[0] == '0') HidKeyCodes.KEY_0 else (HidKeyCodes.KEY_1 + (key[0] - '1')).toByte()
                    }
                }
            }
        }
        if (mainKey != HidKeyCodes.KEY_NONE || modifiers != 0.toByte()) {
            repository.sendKey(mainKey, modifiers)
        }
    }

    private fun executeSpecialKey(key: String) {
        val usageId = when (key.uppercase()) {
            "MUTE" -> HidKeyCodes.MEDIA_MUTE
            "VOL_UP" -> HidKeyCodes.MEDIA_VOL_UP
            "VOL_DOWN" -> HidKeyCodes.MEDIA_VOL_DOWN
            "PLAY", "PAUSE" -> HidKeyCodes.MEDIA_PLAY_PAUSE
            "BRIGHT_UP" -> HidKeyCodes.BRIGHTNESS_UP
            "BRIGHT_DOWN" -> HidKeyCodes.BRIGHTNESS_DOWN
            else -> 0.toShort()
        }
        if (usageId != 0.toShort()) {
            repository.sendConsumerKey(usageId)
        }
    }

    // --- Emergency Actions ---

    fun runEmergencyAction(action: EmergencyAction) {
        viewModelScope.launch {
            _emergencyStatus.value = "Executing ${action.name}..."
            when (action) {
                EmergencyAction.LOCK_MACHINE -> {
                    executeKeyCombo("CTRL+CMD+Q")
                }
                EmergencyAction.KILL_INTERNET_ADAPTER -> {
                    // This typically requires SSH or a helper agent, but we can try a macro
                    executeMacro2Script("KEY(CMD+SPACE) && WAIT(400) && TEXT(Terminal) && KEY(ENTER) && WAIT(1000) && TEXT(networksetup -setnetworkserviceenabled Wi-Fi off) && KEY(ENTER)")
                }
                EmergencyAction.STOP_AUDIO -> {
                    executeSpecialKey("MUTE")
                    executeSpecialKey("PAUSE")
                }
                EmergencyAction.CLEAR_CLIPBOARD -> {
                    repository.sendText(" ") // Simplistic clear
                }
                EmergencyAction.CLOSE_SENSITIVE_APPS -> {
                    executeMacro2Script("KEY(CMD+ALT+ESC) && WAIT(500) && KEY(ENTER)")
                }
            }
            delay(1000)
            _emergencyStatus.value = "Action Completed"
            delay(2000)
            _emergencyStatus.value = "Ready"
        }
    }

    fun runEmergencyAction(actionName: String) {
        val action = try { EmergencyAction.valueOf(actionName) } catch (e: Exception) { null }
        action?.let { runEmergencyAction(it) }
    }

    // --- Terminal Discovery ---

    fun scanTerminalDevices() {
        if (_isTerminalScanning.value) return
        _isTerminalScanning.value = true
        _terminalScanProgress.value = 0f
        _scannedTerminals.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            val foundDevices = mutableListOf<TerminalDevice>()
            val ports = listOf(22, 5555, 8080)
            
            // Simple subset scan behavior
            val baseIp = "192.168.1." 
            val totalIps = 20
            for (i in 1..totalIps) {
                val targetIp = baseIp + i
                for (port in ports) {
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(targetIp, port), 40)
                        socket.close()
                        val proto = when(port) {
                            22 -> "SSH"
                            5555 -> "ADB"
                            else -> "HTTP"
                        }
                        foundDevices.add(TerminalDevice(targetIp, port, proto))
                    } catch (e: Exception) { }
                }
                withContext(Dispatchers.Main) {
                    _terminalScanProgress.value = i / totalIps.toFloat()
                }
            }
            
            withContext(Dispatchers.Main) {
                _scannedTerminals.value = foundDevices
                _terminalScanProgress.value = 1f
                _isTerminalScanning.value = false
            }
        }
    }

    // --- Reverse Shell ---

    fun startReverseShellListener(port: Int) {
        if (_isReverseShellListening.value) return
        _isReverseShellListening.value = true
        _reverseShellStatus.value = "Listening on port $port..."
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Mock listener logic for now since we're refactoring/migrating state
                // In a real scenario, this would involve a ServerSocket
                delay(1000)
                withContext(Dispatchers.Main) {
                    _reverseShellStatus.value = "Awaiting payload on $port..."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _reverseShellStatus.value = "Error: ${e.message}"
                    _isReverseShellListening.value = false
                }
            }
        }
    }

    fun stopReverseShellListener() {
        _isReverseShellListening.value = false
        _reverseShellConnected.value = false
        _reverseShellStatus.value = "Listener Stopped"
    }

    fun sendReverseShellCommand(command: String) {
        val newLines = _reverseShellLines.value + "> $command"
        _reverseShellLines.value = newLines
    }

    fun exportMacrosJson(): String {
        return gson.toJson(_customMacros.value)
    }

    fun importMacrosJson(json: String): Boolean {
        return try {
            val type = object : TypeToken<List<CustomMacro>>() {}.type
            val imported: List<CustomMacro> = gson.fromJson(json, type)
            val merged = (_customMacros.value + imported).distinctBy { it.name }
            _customMacros.value = merged
            saveCustomMacros(merged)
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- Wake on LAN ---

    fun updateWolMac(mac: String) {
        _wolMacAddress.value = mac
        prefs.edit().putString("wol_mac_address", mac).apply()
    }

    fun updateWolBroadcast(ip: String) {
        _wolBroadcastIp.value = ip
        prefs.edit().putString("wol_broadcast_ip", ip).apply()
    }

    fun setWolPort(port: Int) {
        _wolPort.value = port.coerceIn(1, 65535)
        prefs.edit().putInt("wol_port", _wolPort.value).apply()
    }

    fun sendWakeOnLan() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _wolStatus.value = "Sending magic packet..."
                val mac = parseMacAddress(_wolMacAddress.value)
                val packetBytes = ByteArray(6 + 16 * mac.size)
                for (i in 0 until 6) packetBytes[i] = 0xFF.toByte()
                for (i in 6 until packetBytes.size step mac.size) {
                    mac.copyInto(packetBytes, i)
                }

                val targets = mutableListOf<InetAddress>()
                targets.add(InetAddress.getByName(_wolBroadcastIp.value.ifBlank { "255.255.255.255" }))
                
                NetworkInterface.getNetworkInterfaces()?.asSequence()?.forEach { iface ->
                    if (iface.isUp && !iface.isLoopback) {
                        iface.interfaceAddresses.mapNotNull { it.broadcast }.forEach { targets.add(it) }
                    }
                }

                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    targets.distinct().forEach { address ->
                        socket.send(DatagramPacket(packetBytes, packetBytes.size, address, _wolPort.value))
                    }
                }
                _wolStatus.value = "Packet sent successfully"
            } catch (e: Exception) {
                _wolStatus.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    private fun parseMacAddress(raw: String): ByteArray {
        val hex = raw.replace(Regex("[^0-9a-fA-F]"), "")
        if (hex.length != 12) throw IllegalArgumentException("Invalid MAC address")
        return ByteArray(6) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    // --- Auto Clicker ---

    fun setAutoClickInterval(interval: Long) {
        _autoClickInterval.value = interval.coerceAtLeast(1)
    }

    fun setAutoClickLoops(count: Int) {
        _autoClickLoops.value = count.coerceAtLeast(0)
    }

    fun setAutoClickUnit(unit: ClickTimeUnit) {
        _autoClickUnit.value = unit
    }

    fun startAutoClicker() {
        if (_isAutoClicking.value) return
        _isAutoClicking.value = true
        _currentClickCount.value = 0
        autoClickJob?.cancel()
        autoClickJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val maxLoops = _autoClickLoops.value
                val baseInterval = _autoClickInterval.value
                val unit = _autoClickUnit.value
                
                val intervalMs = when(unit) {
                    ClickTimeUnit.MS -> baseInterval
                    ClickTimeUnit.SEC -> baseInterval * 1000
                    ClickTimeUnit.MIN -> baseInterval * 60000
                }

                while (isActive && (maxLoops == 0 || _currentClickCount.value < maxLoops)) {
                    // Click down
                    repository.sendMouseMove(0f, 0f, 1, 0)
                    delay(50)
                    // Click up
                    repository.sendMouseMove(0f, 0f, 0, 0)
                    
                    _currentClickCount.value++
                    
                    if (maxLoops == 0 || _currentClickCount.value < maxLoops) {
                        delay(intervalMs.coerceAtLeast(10)) 
                    }
                }
            } finally {
                _isAutoClicking.value = false
            }
        }
    }

    fun stopAutoClicker() {
        autoClickJob?.cancel()
        _isAutoClicking.value = false
    }

    // --- Injector / DuckyScript ---

    fun executeDuckyScript(script: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val lines = script.lines().map { it.trim() }.filter { it.isNotBlank() && !it.uppercase().startsWith("REM ") }
            var defaultDelay = 100L
            for (line in lines) {
                val parts = line.split(" ", limit = 2)
                val cmd = parts[0].uppercase()
                val arg = if (parts.size > 1) parts[1] else ""

                when (cmd) {
                    "DEFAULTDELAY" -> defaultDelay = arg.toLongOrNull() ?: defaultDelay
                    "DELAY" -> delay(arg.toLongOrNull() ?: defaultDelay)
                    "STRING" -> repository.sendText(arg)?.join()
                    "ENTER" -> repository.sendKey(HidKeyCodes.KEY_ENTER)
                    "TAB" -> repository.sendKey(HidKeyCodes.KEY_TAB)
                    "SPACE" -> repository.sendKey(HidKeyCodes.KEY_SPACE)
                    "UP", "UPARROW" -> repository.sendKey(HidKeyCodes.KEY_UP)
                    "DOWN", "DOWNARROW" -> repository.sendKey(HidKeyCodes.KEY_DOWN)
                    "GUI", "WINDOWS", "COMMAND" -> {
                        val key = parseDuckyKey(arg)
                        repository.sendKey(key, HidKeyCodes.MODIFIER_LEFT_GUI)
                    }
                    "CTRL", "CONTROL" -> {
                        val key = parseDuckyKey(arg)
                        repository.sendKey(key, HidKeyCodes.MODIFIER_LEFT_CTRL)
                    }
                    "ALT" -> {
                        val key = parseDuckyKey(arg)
                        repository.sendKey(key, HidKeyCodes.MODIFIER_LEFT_ALT)
                    }
                    "SHIFT" -> {
                        val key = parseDuckyKey(arg)
                        repository.sendKey(key, HidKeyCodes.MODIFIER_LEFT_SHIFT)
                    }
                }
                delay(10)
            }
        }
    }

    private fun parseDuckyKey(arg: String): Byte {
        val uArg = arg.uppercase()
        return when (uArg) {
            "SPACE" -> HidKeyCodes.KEY_SPACE
            "ENTER" -> HidKeyCodes.KEY_ENTER
            "TAB" -> HidKeyCodes.KEY_TAB
            "ESC", "ESCAPE" -> HidKeyCodes.KEY_ESC
            else -> {
                if (uArg.length == 1 && uArg[0] in 'A'..'Z') {
                    (HidKeyCodes.KEY_A + (uArg[0] - 'A')).toByte()
                } else if (uArg.length == 1 && uArg[0] in '0'..'9') {
                    if (uArg[0] == '0') HidKeyCodes.KEY_0 else (HidKeyCodes.KEY_1 + (uArg[0] - '1')).toByte()
                } else HidKeyCodes.KEY_NONE
            }
        }
    }
}
