package com.example.rabit.ui.forensics

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import com.example.rabit.domain.model.gemini.GeminiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dongliu.apk.parser.ApkFile
import java.io.File

data class InstalledApp(
    val appName: String,
    val packageName: String,
    val sourceDir: String,
    val isSystemApp: Boolean
)

sealed class ForensicsState {
    object Idle : ForensicsState()
    object LoadingApps : ForensicsState()
    data class AppsLoaded(val apps: List<InstalledApp>) : ForensicsState()
    data class AppSelected(
        val app: InstalledApp,
        val manifestXml: String,
        val strings: List<String>,
        val aiInsight: String? = null,
        val isAnalyzing: Boolean = false
    ) : ForensicsState()
    data class Error(val message: String) : ForensicsState()
}

class ForensicsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ForensicsState>(ForensicsState.Idle)
    val uiState = _uiState.asStateFlow()

    private val geminiRepo = GeminiRepositoryImpl()

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = ForensicsState.LoadingApps
            try {
                val pm = getApplication<Application>().packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                
                val apps = packages.map { info ->
                    val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    InstalledApp(
                        appName = info.loadLabel(pm).toString(),
                        packageName = info.packageName,
                        sourceDir = info.sourceDir,
                        isSystemApp = isSystem
                    )
                }.sortedBy { it.appName }
                
                // Show user apps first, then system apps
                val sortedApps = apps.filter { !it.isSystemApp } + apps.filter { it.isSystemApp }
                
                _uiState.value = ForensicsState.AppsLoaded(sortedApps)
            } catch (e: Exception) {
                _uiState.value = ForensicsState.Error(e.localizedMessage ?: "Failed to load apps")
            }
        }
    }

    fun selectAppForAnalysis(app: InstalledApp) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apkFile = ApkFile(File(app.sourceDir))
                val manifestXml = apkFile.manifestXml ?: "Could not extract manifest"
                apkFile.close()
                
                // Advanced extraction: Read raw classes.dex and find high-value strings
                val strings = extractHighValueStrings(app.sourceDir)

                _uiState.value = ForensicsState.AppSelected(
                    app = app,
                    manifestXml = manifestXml,
                    strings = strings
                )
            } catch (e: Exception) {
                 _uiState.value = ForensicsState.Error(e.localizedMessage ?: "Failed to parse APK")
            }
        }
    }

    private fun extractHighValueStrings(sourceDir: String): List<String> {
        val strings = mutableListOf<String>()
        try {
            val apk = ApkFile(File(sourceDir))
            val dexBytes = apk.getFileData("classes.dex")
            apk.close()
            
            if (dexBytes == null) return listOf("classes.dex not found")

            val minLength = 6
            var currentStr = StringBuilder()
            
            for (byte in dexBytes) {
                val char = byte.toInt().toChar()
                if (char in ' '..'~') {
                    currentStr.append(char)
                } else {
                    if (currentStr.length >= minLength) {
                        val s = currentStr.toString()
                        if (isHighValueString(s)) strings.add(s)
                    }
                    currentStr.clear()
                }
            }
            if (currentStr.length >= minLength) {
                val s = currentStr.toString()
                if (isHighValueString(s)) strings.add(s)
            }
        } catch (e: Exception) {
            strings.add("Error extracting strings: ${e.localizedMessage}")
        }
        return strings.distinct()
    }

    private fun isHighValueString(s: String): Boolean {
        val isUrl = s.startsWith("http://") || s.startsWith("https://")
        val isAwsKey = s.startsWith("AKIA") && s.length == 20
        val isFirebaseKey = s.startsWith("AIza") && s.length > 30
        val isJwt = s.startsWith("eyJ") && s.length > 50
        val isGoogleOauth = s.endsWith("apps.googleusercontent.com")
        
        return isUrl || isAwsKey || isFirebaseKey || isJwt || isGoogleOauth
    }

    fun analyzeManifestWithAi(manifestXml: String, strings: List<String>, apiKey: String) {
        val currentState = _uiState.value as? ForensicsState.AppSelected ?: return
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(isAnalyzing = true)
            try {
                if (apiKey.isBlank()) {
                    _uiState.value = currentState.copy(isAnalyzing = false, aiInsight = "Error: Gemini API Key missing in Settings")
                    return@launch
                }

                // Truncate manifest if too long, focus on core components
                val truncatedManifest = if (manifestXml.length > 30000) {
                    manifestXml.take(30000) + "\n...[TRUNCATED]..."
                } else {
                    manifestXml
                }
                
                val secretsList = if (strings.isEmpty()) "None detected." else strings.joinToString("\n")

                val request = GeminiRequest(
                    prompt = "Analyze this AndroidManifest.xml and the extracted hardcoded strings from classes.dex. Look for hardcoded API keys (like Google Maps, AWS, Firebase), exported activities/services without permissions, debuggable flags, cleartext traffic enabled, and other security misconfigurations. Provide a concise, bulleted tactical report.\n\n--- EXTRACTED SECRETS ---\n$secretsList\n\n--- MANIFEST ---\n$truncatedManifest",
                    systemPrompt = "You are an elite mobile security researcher. Output a highly technical, tactical vulnerability assessment. Use [CRITICAL], [HIGH], [LOW] tags.",
                    temperature = 0.3f
                )

                val response = geminiRepo.sendPrompt(request, apiKey)
                if (response.error != null) {
                    _uiState.value = currentState.copy(isAnalyzing = false, aiInsight = "Error: ${response.error.message}")
                } else {
                    _uiState.value = currentState.copy(isAnalyzing = false, aiInsight = response.text)
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(isAnalyzing = false, aiInsight = "Error: ${e.localizedMessage}")
            }
        }
    }

    fun backToList() {
        loadInstalledApps()
    }
}
