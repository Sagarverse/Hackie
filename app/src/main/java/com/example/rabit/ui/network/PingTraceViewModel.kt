package com.example.rabit.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress

data class PingResult(
    val host: String,
    val reachable: Boolean,
    val latencyMs: Long
)

data class TraceHop(
    val hopNumber: Int,
    val hostName: String,
    val ip: String,
    val latencyMs: Long
)

sealed class PingTraceState {
    object Idle : PingTraceState()
    data class Pinging(val sent: Int, val received: Int) : PingTraceState()
    data class Tracing(val currentHop: Int) : PingTraceState()
    object Completed : PingTraceState()
}

class PingTraceViewModel : ViewModel() {
    private val _pingResults = MutableStateFlow<List<PingResult>>(emptyList())
    val pingResults = _pingResults.asStateFlow()

    private val _traceResults = MutableStateFlow<List<TraceHop>>(emptyList())
    val traceResults = _traceResults.asStateFlow()

    private val _state = MutableStateFlow<PingTraceState>(PingTraceState.Idle)
    val state = _state.asStateFlow()

    private var activeJob: Job? = null

    fun startPing(host: String, count: Int = 10) {
        val target = host.trim()
        if (target.isBlank() || activeJob?.isActive == true) return

        _pingResults.value = emptyList()
        _state.value = PingTraceState.Pinging(0, 0)

        activeJob = viewModelScope.launch(Dispatchers.IO) {
            val results = mutableListOf<PingResult>()
            var received = 0

            for (i in 1..count) {
                if (!isActive) break
                val start = System.currentTimeMillis()
                try {
                    val addr = InetAddress.getByName(target)
                    val reachable = addr.isReachable(2000)
                    val latency = System.currentTimeMillis() - start
                    if (reachable) received++
                    results.add(PingResult(addr.hostAddress ?: target, reachable, latency))
                } catch (e: Exception) {
                    results.add(PingResult(target, false, -1))
                }
                _pingResults.value = results.toList()
                _state.value = PingTraceState.Pinging(i, received)
                delay(500)
            }
            _state.value = PingTraceState.Completed
        }
    }

    fun startTrace(host: String) {
        val target = host.trim()
        if (target.isBlank() || activeJob?.isActive == true) return

        _traceResults.value = emptyList()
        _state.value = PingTraceState.Tracing(0)

        activeJob = viewModelScope.launch(Dispatchers.IO) {
            val hops = mutableListOf<TraceHop>()

            // Use Android's Process to run traceroute via ping with TTL
            for (ttl in 1..30) {
                if (!isActive) break
                _state.value = PingTraceState.Tracing(ttl)

                try {
                    val start = System.currentTimeMillis()
                    val process = Runtime.getRuntime().exec(arrayOf("ping", "-c", "1", "-t", "$ttl", "-W", "2", target))
                    val output = process.inputStream.bufferedReader().readText()
                    val error = process.errorStream.bufferedReader().readText()
                    val latency = System.currentTimeMillis() - start
                    process.waitFor()

                    // Parse the output
                    val fromRegex = Regex("from\\s+([\\w.-]+)\\s+\\(([\\d.]+)\\)")
                    val match = fromRegex.find(output + error)

                    if (match != null) {
                        val hopHost = match.groupValues[1]
                        val hopIp = match.groupValues[2]
                        hops.add(TraceHop(ttl, hopHost, hopIp, latency))
                        _traceResults.value = hops.toList()

                        // If we've reached the destination, stop
                        if (hopIp == target || hopHost == target) break
                        // Also check if we got a reply from the final destination
                        if (output.contains("ttl=") && process.exitValue() == 0) break
                    } else {
                        hops.add(TraceHop(ttl, "*", "* * *", -1))
                        _traceResults.value = hops.toList()
                    }
                } catch (e: Exception) {
                    hops.add(TraceHop(ttl, "Error", e.localizedMessage ?: "Unknown", -1))
                    _traceResults.value = hops.toList()
                }
            }
            _state.value = PingTraceState.Completed
        }
    }

    fun stop() {
        activeJob?.cancel()
        _state.value = PingTraceState.Completed
    }
}
