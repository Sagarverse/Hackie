package com.example.rabit.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.adb.RabitAdbClient
import com.example.rabit.data.security.NeuralTrafficAnalyzer
import com.example.rabit.data.security.TrafficStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrafficAnalyzerViewModel(
    private val adbClient: RabitAdbClient,
    private val storageManager: TrafficStorageManager
) : ViewModel() {
    
    private val analyzer = NeuralTrafficAnalyzer(adbClient)
    
    val packets = analyzer.packets
    val isHacked = analyzer.isHacked
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    private val _trafficHistory = MutableStateFlow<List<TrafficStorageManager.TrafficRecord>>(emptyList())
    val trafficHistory = _trafficHistory.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _trafficHistory.value = storageManager.getHistory()
        }
    }

    fun startSniffing() {
        _isAnalyzing.value = true
        analyzer.startAnalysis(viewModelScope)
    }

    fun stopSniffing() {
        _isAnalyzing.value = false
        val currentPackets = packets.value
        val compromised = isHacked.value ?: false
        
        analyzer.stopAnalysis()
        
        // Save capture to history if not empty
        if (currentPackets.isNotEmpty()) {
            viewModelScope.launch {
                storageManager.saveTraffic("Connected Device", currentPackets, compromised)
                loadHistory()
            }
        }
    }

    fun clearTrafficHistory() {
        viewModelScope.launch {
            storageManager.clearHistory()
            _trafficHistory.value = emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        analyzer.stopAnalysis()
    }
}
