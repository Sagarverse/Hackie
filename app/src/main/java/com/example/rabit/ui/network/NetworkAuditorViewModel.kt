package com.example.rabit.ui.network

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import com.example.rabit.domain.model.gemini.GeminiRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import org.json.JSONArray
import org.json.JSONObject

class NetworkAuditorViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _devices = MutableStateFlow<List<NetworkDevice>>(emptyList())
    val devices: StateFlow<List<NetworkDevice>> = _devices.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _aiAnalysisResult = MutableStateFlow<String?>(null)
    val aiAnalysisResult: StateFlow<String?> = _aiAnalysisResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private var scanJob: Job? = null
    private val geminiRepo = GeminiRepositoryImpl()
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)

    data class NetworkDevice(
        val ip: String,
        val hostname: String = "Unknown",
        val userName: String = "Unknown",
        val macAddress: String = "Unknown",
        val services: List<String> = emptyList(),
        val manufacturer: String = "Generic Device",
        val isReachable: Boolean = true
    )

    fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        _devices.value = emptyList()
        _scanProgress.value = 0f
        
        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val subnet = getLocalSubnet() ?: return@launch
                val activeDevices = mutableListOf<NetworkDevice>()
                
                val jobs = mutableListOf<Job>()
                for (i in 1..254) {
                    val ip = "$subnet.$i"
                    jobs.add(launch {
                        if (InetAddress.getByName(ip).isReachable(500)) {
                            val hostname = resolveHostname(ip)
                            val userName = extractUserNameFromHostname(hostname)
                            val macAddress = getMacAddressFromArp(ip)
                            val services = probeCommonServices(ip)
                            val device = NetworkDevice(
                                ip = ip,
                                hostname = hostname,
                                userName = userName,
                                macAddress = macAddress,
                                services = services,
                                manufacturer = "Station Node"
                            )
                            synchronized(activeDevices) { activeDevices.add(device) }
                        }
                        synchronized(_scanProgress) {
                            _scanProgress.value += (1f / 254f)
                        }
                    })
                }
                jobs.joinAll()
                _devices.value = activeDevices.sortedBy { it.ip.split(".").last().toInt() }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
                _scanProgress.value = 1f
            }
        }
    }

    private fun getLocalSubnet(): String? {
        val wm = getApplication<Application>().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectionInfo = wm.connectionInfo
        val ipAddress = connectionInfo.ipAddress
        if (ipAddress == 0) return null
        
        return String.format(
            "%d.%d.%d",
            ipAddress and 0xff,
            ipAddress shr 8 and 0xff,
            ipAddress shr 16 and 0xff
        )
    }

    private fun resolveHostname(ip: String): String {
        return try {
            val addr = InetAddress.getByName(ip)
            val name = addr.canonicalHostName
            if (name == ip) "Unidentified Node" else name
        } catch (_: Exception) {
            "Unidentified Node"
        }
    }

    private fun extractUserNameFromHostname(hostname: String): String {
        if (hostname == "Unidentified Node" || hostname.isBlank()) return "Unknown"
        // Common patterns: "Sagars-MacBook-Pro", "Sagar-PC", "iPhone-von-Sagar"
        val regex = Regex("(?i)^([a-z0-9]+)s?-(?:macbook|pc|iphone|ipad|laptop|desktop|imac|macmini)", RegexOption.IGNORE_CASE)
        val match = regex.find(hostname)
        if (match != null) {
            val rawName = match.groupValues[1]
            if (rawName.endsWith("s", ignoreCase = true)) {
                 return rawName.dropLast(1).replaceFirstChar { it.uppercase() }
            }
            return rawName.replaceFirstChar { it.uppercase() }
        }
        
        // Split by dashes and take the first reasonably sized part as a guess
        val parts = hostname.split("-")
        if (parts.isNotEmpty() && parts[0].length > 2 && !parts[0].equals("android", ignoreCase = true)) {
            val name = parts[0]
            if (name.endsWith("s", ignoreCase = true)) {
                return name.dropLast(1).replaceFirstChar { it.uppercase() }
            }
            return name.replaceFirstChar { it.uppercase() }
        }

        return "Unknown"
    }

    private fun getMacAddressFromArp(ip: String): String {
        var mac = "Unknown"
        try {
            java.io.BufferedReader(java.io.FileReader("/proc/net/arp")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val splitted = line!!.split(Regex(" +"))
                    if (splitted.size >= 4 && ip == splitted[0]) {
                        val potentialMac = splitted[3]
                        if (potentialMac.matches(Regex("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}")) && potentialMac != "00:00:00:00:00:00") {
                            mac = potentialMac.uppercase()
                        }
                        break
                    }
                }
            }
        } catch (e: Exception) {
            // /proc/net/arp is restricted on Android 10+, this might fail gracefully and return "Unknown"
        }
        return mac
    }

    private fun probeCommonServices(ip: String): List<String> {
        val services = mutableListOf<String>()
        val ports = mapOf(
            22 to "SSH",
            80 to "HTTP",
            443 to "HTTPS",
            445 to "SMB/CIFS",
            5000 to "UPnP/DLNA",
            7000 to "AirPlay",
            8080 to "Web Hub",
            5555 to "ADB"
        )

        ports.forEach { (port, name) ->
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 100)
                    services.add("$port ($name)")
                }
            } catch (_: Exception) {}
        }
        return services
    }

    fun stopScan() {
        scanJob?.cancel()
        _isScanning.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }

    fun reviewTopologyWithAi() {
        if (_devices.value.isEmpty()) return
        
        viewModelScope.launch {
            _isAnalyzing.value = true
            _aiAnalysisResult.value = null
            
            try {
                val apiKey = prefs.getString("gemini_api_key", "") ?: ""
                if (apiKey.isBlank()) {
                    _aiAnalysisResult.value = "Error: Gemini API Key missing in Settings"
                    return@launch
                }

                val topologyInfo = _devices.value.joinToString("\n") { device ->
                    "- ${device.ip} (${device.hostname}): [${device.services.joinToString(", ")}]"
                }

                val request = GeminiRequest(
                    prompt = "Analyze this local network topology and identify high-value targets or likely vulnerabilities. Suggest tactical next steps or HID payloads if applicable.\n\nTopology:\n$topologyInfo",
                    systemPrompt = "You are a professional Network Security Analyst (Neural Auditor). Analyze the JSON topology and provide concise, high-impact tactical insights.",
                    temperature = 0.5f
                )

                val response = geminiRepo.sendPrompt(request, apiKey)
                if (response.error != null) {
                    _aiAnalysisResult.value = "Error: ${response.error.message}"
                } else {
                    _aiAnalysisResult.value = response.text
                }
            } catch (e: Exception) {
                _aiAnalysisResult.value = "Exception: ${e.message}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun clearAiAnalysis() {
        _aiAnalysisResult.value = null
    }
}
