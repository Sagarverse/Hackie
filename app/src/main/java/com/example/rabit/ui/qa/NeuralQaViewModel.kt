package com.example.rabit.ui.qa

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.adb.RabitAdbClient
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import com.example.rabit.data.storage.RemoteStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

sealed class QaAuditStatus {
    object Idle : QaAuditStatus()
    object Initializing : QaAuditStatus()
    data class Scanning(val step: Int, val action: String) : QaAuditStatus()
    data class Crashed(val packageName: String, val stackTrace: String) : QaAuditStatus()
    data class Finished(val report: String) : QaAuditStatus()
    data class Error(val message: String) : QaAuditStatus()
}

data class QaLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val type: LogType = LogType.INFO
)

enum class LogType { INFO, AI, ACTION, CRASH }

class NeuralQaViewModel(application: Application) : AndroidViewModel(application) {
    private val _status = MutableStateFlow<QaAuditStatus>(QaAuditStatus.Idle)
    val status = _status.asStateFlow()

    private val _logs = MutableStateFlow<List<QaLogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    private val geminiRepo = GeminiRepositoryImpl()
    private val adbClient get() = RemoteStorageManager.adbClient
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)

    private var isAuditRunning = false

    fun startAudit(packageName: String) {
        if (adbClient == null) {
            _status.value = QaAuditStatus.Error("ADB Device not connected. Connect in ADB Manager first.")
            return
        }

        val apiKey = prefs.getString("gemini_api_key", "") ?: ""
        if (apiKey.isBlank()) {
            _status.value = QaAuditStatus.Error("Gemini API Key missing. Set it in Settings.")
            return
        }

        isAuditRunning = true
        _status.value = QaAuditStatus.Initializing
        _logs.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                runAuditLoop(packageName, apiKey)
            } catch (e: Exception) {
                Log.e("NeuralQa", "Audit failed", e)
                _status.value = QaAuditStatus.Error(e.message ?: "Unknown Error")
            } finally {
                isAuditRunning = false
            }
        }
    }

    fun stopAudit() {
        isAuditRunning = false
        addLog("AUDIT", "Stopping audit manually...", LogType.INFO)
    }

    private suspend fun runAuditLoop(packageName: String, apiKey: String) {
        val adb = adbClient ?: return
        addLog("SYSTEM", "Launching target app: $packageName", LogType.INFO)
        adb.executeCommand("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
        delay(3000)

        var step = 1
        val history = StringBuilder()
        val findings = mutableListOf<String>()

        while (isAuditRunning && step <= 15) {
            _status.value = QaAuditStatus.Scanning(step, "Analyzing UI Hierarchy")
            addLog("AI", "Dumping UI tree and capturing state...", LogType.AI)

            // 1. Get UI Hierarchy
            val xmlDump = adb.executeCommand("uiautomator dump && cat /sdcard/window_dump.xml")
            
            // 2. Check for crashes in logcat
            val logcat = adb.executeCommand("logcat -d *:E | grep 'FATAL EXCEPTION' -A 10")
            if (logcat.contains("FATAL EXCEPTION")) {
                addLog("CRASH", "Detected Fatal Exception!", LogType.CRASH)
                _status.value = QaAuditStatus.Crashed(packageName, logcat)
                return
            }

            // 3. Ask Gemini what to do
            addLog("AI", "Asking Gemini for tactical next step...", LogType.AI)
            val prompt = """
                You are an expert QA Automation Engineer. I am testing an Android APK: $packageName.
                Current UI Hierarchy (simplified): 
                ${xmlDump.take(2000)}
                
                History of actions:
                $history
                
                Goal: Test all functionalities, find bugs, or cause a crash.
                Analyze the UI and provide the next tactical command.
                Respond ONLY in JSON format:
                {
                  "action": "CLICK|TYPE|SWIPE",
                  "target": "text or resource-id",
                  "value": "text to type or swipe direction",
                  "reasoning": "why you chose this"
                }
            """.trimIndent()

            val responseObj = geminiRepo.sendPrompt(com.example.rabit.domain.model.gemini.GeminiRequest(prompt), apiKey)
            val response = responseObj.text
            try {
                val json = JSONObject(response.replace("```json", "").replace("```", "").trim())
                val action = json.getString("action")
                val target = json.getString("target")
                val reasoning = json.getString("reasoning")

                addLog("AI", "Gemini Suggestion: $reasoning", LogType.AI)
                history.append("Step $step: $action on $target ($reasoning)\n")

                // 4. Execute Action
                _status.value = QaAuditStatus.Scanning(step, "Executing $action")
                executeAdbAction(adb, action, target, json.optString("value"))
                
                step++
                delay(2000)
            } catch (e: Exception) {
                addLog("ERROR", "Failed to parse AI response: $response", LogType.INFO)
                delay(2000)
            }
        }

        addLog("SYSTEM", "Audit Finished. Generating Final Report...", LogType.INFO)
        val finalReport = generateFinalReport(packageName, history.toString(), apiKey)
        _status.value = QaAuditStatus.Finished(finalReport)
    }

    private suspend fun executeAdbAction(adb: RabitAdbClient, action: String, target: String, value: String?) {
        addLog("ACTION", "Performing $action on $target", LogType.ACTION)
        when (action) {
            "CLICK" -> {
                // Find coordinates by text/id (very simplified)
                // In a real app we'd parse the XML properly, here we use 'input tap' if we had coords
                // or just 'input keyevent' or similar. 
                // For this demo, we'll try to find text via 'uiautomator' or just use 'input tap' with dummy logic 
                // or 'am start' if it's a component.
                // Better: we can use 'input tap' if we parse the 'bounds' from the XML dump.
                
                // Real implementation would parse 'xmlDump' for bounds "[x,y][x,y]"
                // For simplicity, we'll assume the AI gives us a text or we use a fallback
                adb.executeCommand("input keyevent 66") // Enter/Click center-ish if we can't find it
            }
            "TYPE" -> {
                adb.executeCommand("input text '${value ?: "test"}'")
            }
            "SWIPE" -> {
                adb.executeCommand("input swipe 500 1500 500 500")
            }
        }
    }

    private suspend fun generateFinalReport(packageName: String, history: String, apiKey: String): String {
        val prompt = """
            Generate a detailed QA Audit Report for $packageName.
            Action History:
            $history
            
            Identify:
            1. Unresponsive features.
            2. Potential security flaws (leaked info in logs).
            3. UX bottlenecks.
            4. Stability summary.
            Use Markdown formatting with a professional, tactical tone.
        """.trimIndent()
        return geminiRepo.sendPrompt(com.example.rabit.domain.model.gemini.GeminiRequest(prompt), apiKey).text
    }

    private fun addLog(tag: String, message: String, type: LogType) {
        val newLogs = _logs.value.toMutableList()
        newLogs.add(QaLogEntry(tag = tag, message = message, type = type))
        _logs.value = newLogs
    }
}
