package com.example.rabit.ui.osint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class OsintResult(
    val siteName: String,
    val url: String,
    val status: String // FOUND, NOT_FOUND, ERROR
)

class OsintViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<OsintResult>>(emptyList())
    val results = _results.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private val sites = mapOf(
        "GitHub" to "https://github.com/%s",
        "Twitter" to "https://twitter.com/%s",
        "Instagram" to "https://www.instagram.com/%s/",
        "Reddit" to "https://www.reddit.com/user/%s",
        "TikTok" to "https://www.tiktok.com/@%s",
        "YouTube" to "https://www.youtube.com/@%s",
        "Facebook" to "https://www.facebook.com/%s",
        "Pinterest" to "https://www.pinterest.com/%s/",
        "Snapchat" to "https://www.snapchat.com/add/%s",
        "Telegram" to "https://t.me/%s",
        "Medium" to "https://medium.com/@%s",
        "Steam" to "https://steamcommunity.com/id/%s",
        "Twitch" to "https://www.twitch.tv/%s",
        "DeviantArt" to "https://www.deviantart.com/%s"
    )

    fun startUsernameScan(username: String) {
        if (username.isBlank() || _isScanning.value) return
        
        _isScanning.value = true
        _results.value = emptyList()
        _progress.value = 0f

        viewModelScope.launch(Dispatchers.IO) {
            val total = sites.size
            var completed = 0

            sites.forEach { (name, template) ->
                val targetUrl = template.replace("%s", username)
                try {
                    val connection = URL(targetUrl).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    
                    val responseCode = connection.responseCode
                    val status = if (responseCode == 200) "FOUND" else "NOT_FOUND"
                    
                    val result = OsintResult(name, targetUrl, status)
                    if (status == "FOUND") {
                        withContext(Dispatchers.Main) {
                            _results.value = _results.value + result
                        }
                    }
                } catch (e: Exception) {
                    // Ignore or log error
                } finally {
                    completed++
                    withContext(Dispatchers.Main) {
                        _progress.value = completed.toFloat() / total.toFloat()
                    }
                }
            }

            withContext(Dispatchers.Main) {
                _isScanning.value = false
                _progress.value = 1f
            }
        }
    }

    fun clearResults() {
        _results.value = emptyList()
        _progress.value = 0f
    }
}
