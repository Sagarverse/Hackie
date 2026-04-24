package com.example.rabit.ui.osint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress

data class SubdomainResult(
    val subdomain: String,
    val ipAddress: String,
    val status: String
)

sealed class ScannerState {
    object Idle : ScannerState()
    data class Scanning(val progress: Float, val currentTarget: String) : ScannerState()
    object Completed : ScannerState()
}

class SubdomainScannerViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<SubdomainResult>>(emptyList())
    val results = _results.asStateFlow()

    private val _scannerState = MutableStateFlow<ScannerState>(ScannerState.Idle)
    val scannerState = _scannerState.asStateFlow()

    private var scanJob: Job? = null

    // A small built-in list for fast mobile enumeration.
    private val commonSubdomains = listOf(
        "www", "mail", "remote", "blog", "webmail", "server", "ns1", "ns2",
        "smtp", "secure", "vpn", "api", "dev", "staging", "test", "ftp",
        "admin", "portal", "support", "cpanel", "git", "cloud", "app"
    )

    fun startScan(domain: String) {
        val cleanDomain = domain.trim().lowercase().removePrefix("http://").removePrefix("https://").removeSuffix("/")
        if (cleanDomain.isBlank() || !cleanDomain.contains(".")) return

        if (scanJob?.isActive == true) return

        _results.value = emptyList()
        _scannerState.value = ScannerState.Scanning(0f, "Initializing...")

        scanJob = viewModelScope.launch(Dispatchers.IO) {
            val foundList = mutableListOf<SubdomainResult>()

            // First, scan the root domain
            try {
                _scannerState.value = ScannerState.Scanning(0f, cleanDomain)
                val addresses = InetAddress.getAllByName(cleanDomain)
                if (addresses.isNotEmpty()) {
                    foundList.add(SubdomainResult(cleanDomain, addresses[0].hostAddress ?: "Unknown", "ACTIVE"))
                    _results.value = foundList.toList()
                }
            } catch (e: Exception) {
                // Root domain might not resolve, that's okay, we keep checking subdomains
            }

            // Then iterate over common subdomains
            for (i in commonSubdomains.indices) {
                if (!coroutineContext[kotlinx.coroutines.Job]!!.isActive) break
                val prefix = commonSubdomains[i]
                val target = "$prefix.$cleanDomain"
                
                _scannerState.value = ScannerState.Scanning((i.toFloat() / commonSubdomains.size), target)
                
                try {
                    val addresses = InetAddress.getAllByName(target)
                    if (addresses.isNotEmpty()) {
                        foundList.add(SubdomainResult(target, addresses[0].hostAddress ?: "Unknown", "ACTIVE"))
                        _results.value = foundList.toList()
                    }
                } catch (e: Exception) {
                    // Host not found, ignore
                }
            }

            _scannerState.value = ScannerState.Completed
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        if (_scannerState.value is ScannerState.Scanning) {
            _scannerState.value = ScannerState.Completed
        }
    }
}
