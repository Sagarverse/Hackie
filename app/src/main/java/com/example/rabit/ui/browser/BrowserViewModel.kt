package com.example.rabit.ui.browser

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("rabit_browser_prefs", Context.MODE_PRIVATE)

    private val _currentUrl = MutableStateFlow(prefs.getString("home_page", "https://www.google.com") ?: "https://www.google.com")
    val currentUrl = _currentUrl.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward = _canGoForward.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress = _progress.asStateFlow()

    fun updateUrl(url: String) {
        _currentUrl.value = url
    }

    fun setNavigationState(canBack: Boolean, canForward: Boolean) {
        _canGoBack.value = canBack
        _canGoForward.value = canForward
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setProgress(progress: Int) {
        _progress.value = progress
    }

    fun setHomePage(url: String) {
        prefs.edit().putString("home_page", url).apply()
    }

    fun getHomePage(): String {
        return prefs.getString("home_page", "https://www.google.com") ?: "https://www.google.com"
    }
}
