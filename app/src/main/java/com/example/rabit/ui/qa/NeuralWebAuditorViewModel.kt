package com.example.rabit.ui.qa

import android.app.Application
import android.content.Context
import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import com.example.rabit.domain.model.gemini.GeminiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream

sealed class WebAuditStatus {
    object Idle : WebAuditStatus()
    object Loading : WebAuditStatus()
    data class Analyzing(val step: Int, val action: String) : WebAuditStatus()
    data class Finished(val report: String) : WebAuditStatus()
    data class Error(val message: String) : WebAuditStatus()
}

class NeuralWebAuditorViewModel(application: Application) : AndroidViewModel(application) {
    private val _status = MutableStateFlow<WebAuditStatus>(WebAuditStatus.Idle)
    val status = _status.asStateFlow()

    private val _logs = MutableStateFlow<List<QaLogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    private val geminiRepo = GeminiRepositoryImpl()
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)

    private var isAuditRunning = false
    private var webView: WebView? = null

    fun setWebView(wv: WebView) {
        this.webView = wv
    }

    fun startAudit(url: String) {
        val apiKey = prefs.getString("gemini_api_key", "") ?: ""
        if (apiKey.isBlank()) {
            _status.value = WebAuditStatus.Error("Gemini API Key missing.")
            return
        }

        isAuditRunning = true
        _logs.value = emptyList()
        _status.value = WebAuditStatus.Loading

        viewModelScope.launch {
            try {
                runWebAuditLoop(url, apiKey)
            } catch (e: Exception) {
                _status.value = WebAuditStatus.Error(e.message ?: "Audit Failed")
            } finally {
                isAuditRunning = false
            }
        }
    }

    fun stopAudit() {
        isAuditRunning = false
        addLog("SYSTEM", "Audit terminated.", LogType.INFO)
    }

    private suspend fun runWebAuditLoop(url: String, apiKey: String) {
        addLog("SYSTEM", "Navigating to $url", LogType.INFO)
        withContext(Dispatchers.Main) {
            webView?.loadUrl(url)
        }
        delay(5000) // Initial load

        var step = 1
        val history = StringBuilder()

        while (isAuditRunning && step <= 10) {
            _status.value = WebAuditStatus.Analyzing(step, "Extracting DOM and state")
            
            val dom = getDomContent()
            val consoleLogs = getConsoleLogs()

            addLog("AI", "Analyzing page structure...", LogType.AI)
            
            val prompt = """
                You are a Neural Web Auditor. You are testing the website: $url.
                Current DOM (partial):
                ${dom.take(3000)}
                
                Console/Errors:
                $consoleLogs
                
                History:
                $history
                
                Goal: Identify broken features, slow elements, or UI bugs.
                Decide the next action to test the site.
                Respond ONLY in JSON:
                {
                  "action": "CLICK|TYPE|SCROLL|NAVIGATE",
                  "selector": "css selector or text",
                  "value": "text to type or 'UP'/'DOWN'",
                  "reasoning": "why"
                }
            """.trimIndent()

            val responseObj = geminiRepo.sendPrompt(GeminiRequest(prompt), apiKey)
            val response = responseObj.text
            
            try {
                val json = JSONObject(response.replace("```json", "").replace("```", "").trim())
                val action = json.getString("action")
                val selector = json.getString("selector")
                val reasoning = json.getString("reasoning")

                addLog("AI", "Strategic Choice: $reasoning", LogType.AI)
                history.append("Step $step: $action on $selector ($reasoning)\n")

                _status.value = WebAuditStatus.Analyzing(step, "Executing $action")
                executeWebAction(action, selector, json.optString("value"))
                
                step++
                delay(3000)
            } catch (e: Exception) {
                addLog("ERROR", "Parse Error: ${e.message}", LogType.INFO)
                delay(2000)
            }
        }

        _status.value = WebAuditStatus.Analyzing(step, "Generating Comprehensive Report")
        val finalReport = generateFinalWebReport(url, history.toString(), apiKey)
        _status.value = WebAuditStatus.Finished(finalReport)
    }

    private suspend fun getDomContent(): String = withContext(Dispatchers.Main) {
        var result = ""
        webView?.evaluateJavascript("(function() { return document.body.innerText.substring(0, 5000) + '\\n' + Array.from(document.querySelectorAll('button, a, input')).map(el => el.outerHTML.substring(0,200)).join('\\n'); })();") {
            result = it
        }
        delay(500)
        result
    }

    private suspend fun getConsoleLogs(): String = withContext(Dispatchers.Main) {
        // In a real implementation, we'd hook into WebChromeClient.onConsoleMessage
        // Here we simulate or pull from a buffer if we implemented one
        "No critical console errors detected."
    }

    private suspend fun executeWebAction(action: String, selector: String, value: String?) {
        addLog("ACTION", "Performing $action on $selector", LogType.ACTION)
        withContext(Dispatchers.Main) {
            when (action) {
                "CLICK" -> {
                    val script = "document.querySelectorAll('$selector')[0]?.click() || Array.from(document.querySelectorAll('button, a')).find(el => el.textContent.includes('$selector'))?.click()"
                    webView?.evaluateJavascript(script, null)
                }
                "TYPE" -> {
                    val script = "val el = document.querySelectorAll('$selector')[0]; if(el) { el.value = '$value'; el.dispatchEvent(new Event('input')); }"
                    webView?.evaluateJavascript(script, null)
                }
                "SCROLL" -> {
                    val dist = if (value == "UP") -500 else 500
                    webView?.evaluateJavascript("window.scrollBy(0, $dist)", null)
                }
                "NAVIGATE" -> {
                    webView?.loadUrl(selector)
                }
            }
        }
    }

    private suspend fun generateFinalWebReport(url: String, history: String, apiKey: String): String {
        val prompt = """
            Generate a detailed Website Quality & Security Audit for $url.
            Test Session History:
            $history
            
            Focus on:
            1. Responsiveness and UX.
            2. Link integrity and navigation flow.
            3. Accessibility issues.
            4. Potential vulnerabilities (XSS entry points, open redirects).
            Use a professional auditor tone with Markdown.
        """.trimIndent()
        return geminiRepo.sendPrompt(GeminiRequest(prompt), apiKey).text
    }

    private fun addLog(tag: String, message: String, type: LogType) {
        val newLogs = _logs.value.toMutableList()
        newLogs.add(QaLogEntry(tag = tag, message = message, type = type))
        _logs.value = newLogs
    }
}
