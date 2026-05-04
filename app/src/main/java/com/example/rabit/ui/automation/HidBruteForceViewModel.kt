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
        stopCracking()
        try {
            getApplication<Application>().unregisterReceiver(actionReceiver)
        } catch (e: Exception) {}
    }

    // --- Hash Cracker Logic (Merged) ---
    private val geminiRepo = com.example.rabit.data.gemini.GeminiRepositoryImpl()
    
    private val _crackerState = MutableStateFlow<HashCrackerState>(HashCrackerState.Idle)
    val crackerState = _crackerState.asStateFlow()

    private var crackingJob: Job? = null

    private val dictionary = listOf(
        "123456", "password", "12345678", "qwerty", "123456789", "12345", "1234", "111111",
        "1234567", "dragon", "123123", "baseball", "monkey", "letmein", "admin", "admin123",
        "football", "shadow", "mustang", "superman", "1234567890", "michael", "jessica",
        "qwertyuiop", "iloveyou", "princess", "ashley", "daniel", "joshua", "andrew", "cookie",
        "secret", "hacker", "hackme", "root", "toor", "test", "test1234", "P@ssword", "Password123"
    )

    fun determineHashType(hash: String): String {
        return when (hash.length) {
            32 -> "MD5"
            40 -> "SHA-1"
            64 -> "SHA-256"
            128 -> "SHA-512"
            else -> "Unknown"
        }
    }

    private fun hashString(input: String, algorithm: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance(algorithm)
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun startCracking(targetHash: String, apiKey: String) {
        val cleanHash = targetHash.trim().lowercase()
        if (cleanHash.isBlank()) {
            _crackerState.value = HashCrackerState.Failed("Please enter a valid hash.")
            return
        }

        val type = determineHashType(cleanHash)
        if (type == "Unknown") {
            _crackerState.value = HashCrackerState.Failed("Unsupported hash length. Must be MD5, SHA-1, SHA-256, or SHA-512.")
            return
        }

        val javaAlgorithm = when (type) {
            "MD5" -> "MD5"
            "SHA-1" -> "SHA-1"
            "SHA-256" -> "SHA-256"
            "SHA-512" -> "SHA-512"
            else -> "MD5"
        }

        if (crackingJob?.isActive == true) return

        crackingJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                for (i in dictionary.indices) {
                    if (!isActive) return@launch
                    val guess = dictionary[i]
                    if (i % 2 == 0) {
                        _crackerState.value = HashCrackerState.Cracking(type, (i.toFloat() / dictionary.size) * 0.5f, guess)
                    }
                    val computed = hashString(guess, javaAlgorithm)
                    if (computed == cleanHash) {
                        _crackerState.value = HashCrackerState.Success(cleanHash, guess, "Local Dictionary")
                        return@launch
                    }
                    delay(50)
                }

                if (apiKey.isNotBlank()) {
                    _crackerState.value = HashCrackerState.Cracking(type, 0.6f, "Initiating Neural Lookup...")
                    val req = com.example.rabit.domain.model.gemini.GeminiRequest(
                        prompt = "You are an advanced password recovery engine. A user provided a $type hash: '$cleanHash'. Guess what the original plaintext password might be. If you recognize this hash from known rainbow tables or breaches, output ONLY the plaintext password. If you do not know, reply with 'UNKNOWN'. Do not include explanations.",
                        systemPrompt = "You are an expert hash cracking system.",
                        temperature = 0.1f
                    )
                    val response = geminiRepo.sendPrompt(req, apiKey)
                    val result = response.text.trim()
                    if (result.isNotBlank() && !result.equals("UNKNOWN", ignoreCase = true) && !result.contains("error", ignoreCase = true)) {
                        val verify = hashString(result, javaAlgorithm)
                        if (verify == cleanHash) {
                            _crackerState.value = HashCrackerState.Success(cleanHash, result, "Neural Lookup")
                            return@launch
                        }
                    }
                }
                _crackerState.value = HashCrackerState.Failed("Hash not found in dictionary or neural tables.")
            } catch (e: Exception) {
                _crackerState.value = HashCrackerState.Failed(e.localizedMessage ?: "Cracking failed")
            }
        }
    }

    fun stopCracking() {
        crackingJob?.cancel()
        _crackerState.value = HashCrackerState.Idle
    }

    fun resetCracker() {
        stopCracking()
    }
}

sealed class HashCrackerState {
    object Idle : HashCrackerState()
    data class Cracking(val hashType: String, val progress: Float, val currentGuess: String) : HashCrackerState()
    data class Success(val hash: String, val plaintext: String, val method: String) : HashCrackerState()
    data class Failed(val reason: String) : HashCrackerState()
}
