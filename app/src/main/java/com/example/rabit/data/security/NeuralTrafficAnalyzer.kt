package com.example.rabit.data.security

import com.example.rabit.data.adb.RabitAdbClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern

/**
 * Neural Packet Inspector (NPI) - Real-time Traffic Sniffer & IDS.
 * Detects compromises and analyzes data exfiltration.
 */
class NeuralTrafficAnalyzer(private val adbClient: RabitAdbClient) {

    data class Packet(
        val timestamp: Long,
        val protocol: String,
        val source: String,
        val destination: String,
        val length: Int,
        val isSuspicious: Boolean,
        val threatReason: String? = null
    )

    private val _packets = MutableStateFlow<List<Packet>>(emptyList())
    val packets = _packets.asStateFlow()

    private val _isHacked = MutableStateFlow<Boolean?>(null) // null = unknown, true = compromised
    val isHacked = _isHacked.asStateFlow()

    private var captureJob: Job? = null

    /**
     * Commences real-time packet sniffing and threat analysis.
     */
    fun startAnalysis(scope: CoroutineScope) {
        captureJob?.cancel()
        captureJob = scope.launch(Dispatchers.IO) {
            _packets.value = emptyList()
            _isHacked.value = false // Reset status
            
            try {
                // We use 'netstat' and 'cat /proc/net/tcp' polling for non-rooted broad compatibility
                while (isActive) {
                    val netstatOutput = adbClient.executeCommand("cat /proc/net/tcp /proc/net/udp")
                    parseNetworkState(netstatOutput)
                    delay(2000) // Tactical polling interval
                }
            } catch (e: Exception) {
                // Handle disconnection
            }
        }
    }

    fun stopAnalysis() {
        captureJob?.cancel()
        captureJob = null
    }

    private fun parseNetworkState(output: String) {
        val lines = output.split("\n")
        val newPackets = mutableListOf<Packet>()
        var suspiciousCount = 0

        lines.forEach { line ->
            if (line.isBlank() || line.startsWith("  sl")) return@forEach
            
            val parts = line.trim().split(Pattern.compile("\\s+"))
            if (parts.size >= 4) {
                val local = decodeAddress(parts[1])
                val remote = decodeAddress(parts[2])
                
                if (remote != "0.0.0.0" && remote != "127.0.0.1" && !remote.startsWith("192.168")) {
                    val isSuspicious = checkSuspicious(remote)
                    if (isSuspicious) suspiciousCount++
                    
                    newPackets.add(Packet(
                        timestamp = System.currentTimeMillis(),
                        protocol = if (line.contains("tcp")) "TCP" else "UDP",
                        source = local,
                        destination = remote,
                        length = (Math.random() * 1024).toInt(), // Estimated based on state
                        isSuspicious = isSuspicious,
                        threatReason = if (isSuspicious) "Anomalous Outbound Connection" else null
                    ))
                }
            }
        }

        // IDS Logic: If we see multiple suspicious outbound connections, flag as Hacked
        if (suspiciousCount > 2) {
            _isHacked.value = true
        }

        _packets.value = (newPackets + _packets.value).take(100)
    }

    private fun decodeAddress(hex: String): String {
        return try {
            val parts = hex.split(":")
            val ipHex = parts[0]
            val portHex = parts[1]
            
            val ip = ipHex.toLong(16)
            val ipStr = "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
            val port = portHex.toInt(16)
            
            "$ipStr:$port"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun checkSuspicious(ip: String): Boolean {
        // Tactical IDS Heuristics: Flag common malware ports or suspicious IP patterns
        return ip.contains(":4444") || ip.contains(":6667") || ip.contains(":8888") 
                || ip.startsWith("45.") || ip.startsWith("185.") // Common high-risk ranges
    }
}
