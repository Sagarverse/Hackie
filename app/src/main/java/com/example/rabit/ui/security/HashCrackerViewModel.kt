package com.example.rabit.ui.security

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import com.example.rabit.domain.model.gemini.GeminiRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

sealed class HashCrackerState {
    object Idle : HashCrackerState()
    data class Cracking(val hashType: String, val progress: Float, val currentGuess: String) : HashCrackerState()
    data class Success(val hash: String, val plaintext: String, val method: String) : HashCrackerState()
    data class Failed(val reason: String) : HashCrackerState()
}

class HashCrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val geminiRepo = GeminiRepositoryImpl()

    private val _crackerState = MutableStateFlow<HashCrackerState>(HashCrackerState.Idle)
    val crackerState = _crackerState.asStateFlow()

    private var crackingJob: Job? = null

    // A small built-in dictionary for fast offline cracking.
    // In a real app, this would be loaded from a large text file in raw/assets.
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
            val md = MessageDigest.getInstance(algorithm)
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
                // Phase 1: Local Dictionary Attack
                for (i in dictionary.indices) {
                    if (!isActive) return@launch
                    val guess = dictionary[i]
                    
                    // Throttle UI updates so we don't overwhelm Compose
                    if (i % 2 == 0) {
                        _crackerState.value = HashCrackerState.Cracking(type, (i.toFloat() / dictionary.size) * 0.5f, guess)
                    }

                    val computed = hashString(guess, javaAlgorithm)
                    if (computed == cleanHash) {
                        _crackerState.value = HashCrackerState.Success(cleanHash, guess, "Local Dictionary")
                        return@launch
                    }
                    // Simulate delay to make it look like it's doing heavy work for the demo
                    delay(50)
                }

                // Phase 2: Neural Fallback Attack
                if (apiKey.isNotBlank()) {
                    _crackerState.value = HashCrackerState.Cracking(type, 0.6f, "Initiating Neural Lookup...")
                    
                    val req = GeminiRequest(
                        prompt = "You are an advanced password recovery engine. A user provided a $type hash: '$cleanHash'. Guess what the original plaintext password might be. If you recognize this hash from known rainbow tables or breaches, output ONLY the plaintext password. If you do not know, reply with 'UNKNOWN'. Do not include explanations.",
                        systemPrompt = "You are an expert hash cracking system.",
                        temperature = 0.1f
                    )
                    
                    val response = geminiRepo.sendPrompt(req, apiKey)
                    val result = response.text.trim()
                    
                    if (result.isNotBlank() && !result.equals("UNKNOWN", ignoreCase = true) && !result.contains("error", ignoreCase = true)) {
                        // Verify the AI's guess
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

    fun reset() {
        stopCracking()
    }
}
