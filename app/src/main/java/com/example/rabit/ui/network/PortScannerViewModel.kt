package com.example.rabit.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

data class PortResult(
    val port: Int,
    val state: String, // OPEN, CLOSED
    val service: String,
    val banner: String
)

sealed class PortScanState {
    object Idle : PortScanState()
    data class Scanning(val progress: Float, val currentPort: Int) : PortScanState()
    object Completed : PortScanState()
}

class PortScannerViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<PortResult>>(emptyList())
    val results = _results.asStateFlow()

    private val _scanState = MutableStateFlow<PortScanState>(PortScanState.Idle)
    val scanState = _scanState.asStateFlow()

    private var scanJob: Job? = null

    // Common ports with their well-known services
    private val commonPorts = mapOf(
        21 to "FTP", 22 to "SSH", 23 to "Telnet", 25 to "SMTP",
        53 to "DNS", 80 to "HTTP", 110 to "POP3", 111 to "RPCbind",
        135 to "MSRPC", 139 to "NetBIOS", 143 to "IMAP", 443 to "HTTPS",
        445 to "SMB", 993 to "IMAPS", 995 to "POP3S", 1433 to "MSSQL",
        1521 to "Oracle", 2049 to "NFS", 3306 to "MySQL", 3389 to "RDP",
        5432 to "PostgreSQL", 5900 to "VNC", 5985 to "WinRM", 6379 to "Redis",
        8080 to "HTTP-Proxy", 8443 to "HTTPS-Alt", 8888 to "Alt-HTTP",
        9200 to "Elasticsearch", 27017 to "MongoDB"
    )

    fun startScan(targetHost: String, startPort: Int = 1, endPort: Int = 1024, timeout: Int = 500) {
        val host = targetHost.trim()
        if (host.isBlank()) return
        if (scanJob?.isActive == true) return

        _results.value = emptyList()
        _scanState.value = PortScanState.Scanning(0f, startPort)

        scanJob = viewModelScope.launch(Dispatchers.IO) {
            val found = mutableListOf<PortResult>()
            val totalPorts = endPort - startPort + 1

            for (port in startPort..endPort) {
                if (!isActive) break

                if (port % 20 == 0) {
                    _scanState.value = PortScanState.Scanning(
                        (port - startPort).toFloat() / totalPorts, port
                    )
                }

                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(host, port), timeout)

                    // Try banner grabbing
                    var banner = ""
                    try {
                        socket.soTimeout = 1000
                        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                        if (reader.ready()) {
                            banner = reader.readLine()?.take(120) ?: ""
                        }
                    } catch (_: Exception) {}

                    socket.close()

                    val service = commonPorts[port] ?: "Unknown"
                    found.add(PortResult(port, "OPEN", service, banner))
                    _results.value = found.toList()
                } catch (_: Exception) {
                    // Port closed or filtered — skip
                }
            }

            _scanState.value = PortScanState.Completed
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _scanState.value = PortScanState.Completed
    }

    fun quickScan(targetHost: String) {
        val host = targetHost.trim()
        if (host.isBlank()) return
        if (scanJob?.isActive == true) return

        _results.value = emptyList()
        _scanState.value = PortScanState.Scanning(0f, 0)

        scanJob = viewModelScope.launch(Dispatchers.IO) {
            val found = mutableListOf<PortResult>()
            val ports = commonPorts.keys.toList().sorted()

            for (i in ports.indices) {
                if (!isActive) break
                val port = ports[i]
                _scanState.value = PortScanState.Scanning((i.toFloat() / ports.size), port)

                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(host, port), 800)
                    var banner = ""
                    try {
                        socket.soTimeout = 1500
                        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                        if (reader.ready()) { banner = reader.readLine()?.take(120) ?: "" }
                    } catch (_: Exception) {}
                    socket.close()
                    found.add(PortResult(port, "OPEN", commonPorts[port] ?: "Unknown", banner))
                    _results.value = found.toList()
                } catch (_: Exception) {}
            }
            _scanState.value = PortScanState.Completed
        }
    }
}
