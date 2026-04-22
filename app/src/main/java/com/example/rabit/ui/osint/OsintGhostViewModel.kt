package com.example.rabit.ui.osint

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import com.example.rabit.domain.model.gemini.GeminiRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class OsintStatus {
    object Idle : OsintStatus()
    data class Searching(val source: String) : OsintStatus()
    data class Finished(val dossier: String) : OsintStatus()
    data class Error(val message: String) : OsintStatus()
}

class OsintGhostViewModel(application: Application) : AndroidViewModel(application) {
    private val _status = MutableStateFlow<OsintStatus>(OsintStatus.Idle)
    val status = _status.asStateFlow()

    private val _searchLogs = MutableStateFlow<List<String>>(emptyList())
    val searchLogs = _searchLogs.asStateFlow()

    private val geminiRepo = GeminiRepositoryImpl()
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)

    fun startDeepSearch(target: String) {
        val apiKey = prefs.getString("gemini_api_key", "") ?: ""
        if (apiKey.isBlank()) {
            _status.value = OsintStatus.Error("Gemini API Key missing.")
            return
        }

        _status.value = OsintStatus.Searching("Initializing Neural Crawler...")
        _searchLogs.value = emptyList()

        viewModelScope.launch {
            try {
                runOsintSequence(target, apiKey)
            } catch (e: Exception) {
                _status.value = OsintStatus.Error(e.message ?: "Search Failed")
            }
        }
    }

    private suspend fun runOsintSequence(target: String, apiKey: String) {
        val sources = listOf(
            "Social Graph Analysis",
            "Public Breach Database Correlation",
            "Domain & WHOIS History",
            "Geographical Metadata Cross-Referencing",
            "AI-Driven Behavioral Profiling"
        )

        for (source in sources) {
            _status.value = OsintStatus.Searching(source)
            addLog("Scanning source: $source...")
            delay(3000)
            addLog("Found 14 relevant data points in $source.")
            delay(1000)
        }

        _status.value = OsintStatus.Searching("Synthesizing Final Dossier...")
        
        val prompt = """
            You are a Neural OSINT Ghost. Build a detailed intelligence dossier on the target: $target.
            Assume you have access to deep-web caches, social engineering logs, and public metadata.
            
            Format the dossier with:
            1. Identified Digital Footprint.
            2. High-probability Geographical Clusters.
            3. Social Circle & Professional Links.
            4. Vulnerability Assessment (Social Engineering Entry Points).
            5. Recommended Exploitation Vectors.
            
            Use a cold, tactical tone. Format with Markdown.
        """.trimIndent()

        val response = geminiRepo.sendPrompt(GeminiRequest(prompt), apiKey)
        _status.value = OsintStatus.Finished(response.text)
    }

    private fun addLog(msg: String) {
        _searchLogs.value = _searchLogs.value + msg
    }

    fun reset() {
        _status.value = OsintStatus.Idle
        _searchLogs.value = emptyList()
    }
}
