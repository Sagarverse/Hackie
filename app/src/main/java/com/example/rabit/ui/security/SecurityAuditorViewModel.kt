package com.example.rabit.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.adb.RabitAdbClient
import com.example.rabit.data.security.NeuralAuditEngine
import com.example.rabit.data.security.TacticalStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SecurityAuditorViewModel(
    private val adbClient: RabitAdbClient?,
    private val storageManager: TacticalStorageManager
) : ViewModel() {
    
    private val auditor = adbClient?.let { NeuralAuditEngine(it) }
    
    private val _findings = MutableStateFlow<List<NeuralAuditEngine.Finding>>(emptyList())
    val findings = _findings.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()
    
    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress = _scanProgress.asStateFlow()

    private val _lastReport = MutableStateFlow<String?>(null)
    val lastReport = _lastReport.asStateFlow()

    private val _auditHistory = MutableStateFlow<List<TacticalStorageManager.AuditRecord>>(emptyList())
    val auditHistory = _auditHistory.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _auditHistory.value = storageManager.getHistory()
        }
    }

    fun runFullAudit() {
        val currentAuditor = auditor ?: return
        viewModelScope.launch {
            _isScanning.value = true
            _scanProgress.value = 0.1f
            
            // Simulate progress
            _scanProgress.value = 0.4f
            val results = currentAuditor.performAudit()
            _scanProgress.value = 0.8f
            
            _findings.value = results
            _scanProgress.value = 1.0f
            _isScanning.value = false
            
            // Auto-generate report text
            _lastReport.value = currentAuditor.generateReport(results)
            
            // Save to persistent history
            storageManager.saveAudit("Connected Device", results)
            loadHistory()
        }
    }

    fun getReportText(): String {
        return _lastReport.value ?: "No audit data available."
    }

    fun clearTacticalHistory() {
        viewModelScope.launch {
            storageManager.clearHistory()
            _auditHistory.value = emptyList()
        }
    }
}
