package com.example.rabit.data.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.rabit.domain.model.OsKeyMappings
import com.example.rabit.domain.model.TargetOs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * JarvisCommandEngine — "Hey Sagar" wake word + voice command execution.
 *
 * State machine:
 *   IDLE → LISTENING_WAKE → WAKE_DETECTED → LISTENING_COMMAND → EXECUTING / CONFIRMING
 *
 * Uses Android's SpeechRecognizer in a continuous loop. After each recognition
 * result or timeout, the recognizer is restarted to simulate always-listening.
 *
 * Commands are parsed from natural speech and mapped to HID actions using
 * OsKeyMappings for the selected target OS.
 */
class JarvisCommandEngine(private val context: Context) {

    companion object {
        private const val TAG = "JarvisEngine"

        // Wake word variations (fuzzy matching)
        private val WAKE_PHRASES = listOf(
            "hey sagar", "hey sugar", "hey sager", "hey saagar",
            "he sagar", "a sagar", "hey saga", "hey sagar.",
            "hey saggar", "hey saugar", "hey saagor"
        )

        // Maximum silence before restarting recognizer (ms)
        private const val RESTART_DELAY_MS = 500L

        // How long to wait for a command after wake word (ms)
        private const val COMMAND_TIMEOUT_MS = 6000L
    }

    enum class JarvisMode { BASIC, SMART }

    enum class JarvisState {
        IDLE,
        LISTENING_WAKE,
        WAKE_DETECTED,
        LISTENING_COMMAND,
        THINKING,
        CONFIRMING,
        EXECUTING,
        ERROR
    }

    /**
     * Recognized voice command ready for execution.
     */
    data class VoiceCommand(
        val rawText: String,
        val commandType: CommandType,
        val argument: String = ""
    )

    enum class CommandType {
        LOCK, UNLOCK, SCREENSHOT, SCREENSHOT_AREA,
        COPY, PASTE, CUT, UNDO, REDO,
        CLOSE_WINDOW, SWITCH_APP, SELECT_ALL,
        FIND, NEW_TAB, CLOSE_TAB, SAVE,
        SLEEP, FORCE_QUIT,
        MUTE, VOLUME_UP, VOLUME_DOWN, PLAY_PAUSE,
        OPEN_APP,
        TYPE_TEXT,
        SHOW_DESKTOP, MISSION_CONTROL,
        BRIGHTNESS_UP, BRIGHTNESS_DOWN,
        UNKNOWN
    }

    private val _state = MutableStateFlow(JarvisState.IDLE)
    val state: StateFlow<JarvisState> = _state.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    private val _pendingCommand = MutableStateFlow<VoiceCommand?>(null)
    val pendingCommand: StateFlow<VoiceCommand?> = _pendingCommand.asStateFlow()

    private val _lastExecutedCommand = MutableStateFlow("")
    val lastExecutedCommand: StateFlow<String> = _lastExecutedCommand.asStateFlow()

    private val _isConfirmationRequired = MutableStateFlow(false)
    val isConfirmationRequired: StateFlow<Boolean> = _isConfirmationRequired.asStateFlow()

    private val _wakeWordDetectedCount = MutableStateFlow(0L)
    val wakeWordDetectedCount: StateFlow<Long> = _wakeWordDetectedCount.asStateFlow()

    private val _commandsExecutedCount = MutableStateFlow(0L)
    val commandsExecutedCount: StateFlow<Long> = _commandsExecutedCount.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var commandTimeoutJob: Job? = null
    private var restartJob: Job? = null

    var confirmationRequired: Boolean
        get() = _isConfirmationRequired.value
        set(value) { _isConfirmationRequired.value = value }

    var mode: JarvisMode = JarvisMode.BASIC
    var smartEngine: JarvisSmartEngine? = null

    // Callback to execute HID commands — set by MainViewModel
    var onExecuteCommand: ((VoiceCommand) -> Unit)? = null

    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        // Extend silence detection to allow natural command phrasing
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
    }

    /**
     * Start the Jarvis always-listening engine.
     */
    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = JarvisState.ERROR
            Log.e(TAG, "Speech recognition not available on this device")
            return
        }
        _state.value = JarvisState.LISTENING_WAKE
        startListeningForWakeWord()
    }

    /**
     * Stop the Jarvis engine completely.
     */
    fun stop() {
        commandTimeoutJob?.cancel()
        restartJob?.cancel()
        destroyRecognizer()
        _state.value = JarvisState.IDLE
        _pendingCommand.value = null
        _lastRecognizedText.value = ""
    }

    /**
     * Confirm the pending command (when confirmation mode is ON).
     */
    fun confirmPendingCommand() {
        val cmd = _pendingCommand.value ?: return
        _state.value = JarvisState.EXECUTING
        executeCommand(cmd)
        _pendingCommand.value = null

        // Return to wake word listening
        scope.launch {
            delay(800)
            _state.value = JarvisState.LISTENING_WAKE
            startListeningForWakeWord()
        }
    }

    /**
     * Cancel the pending command.
     */
    fun cancelPendingCommand() {
        _pendingCommand.value = null
        _state.value = JarvisState.LISTENING_WAKE
        startListeningForWakeWord()
    }

    private fun startListeningForWakeWord() {
        destroyRecognizer()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(WakeWordListener())
            startListening(recognizerIntent)
        }
    }

    fun startListeningForCommand() {
        destroyRecognizer()
        _state.value = JarvisState.LISTENING_COMMAND

        // Play a short beep to signal command mode
        playActivationTone()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(CommandListener())
            startListening(recognizerIntent)
        }

        // Timeout: if no command recognized within COMMAND_TIMEOUT_MS, go back to wake listening
        commandTimeoutJob?.cancel()
        commandTimeoutJob = scope.launch {
            delay(COMMAND_TIMEOUT_MS)
            if (_state.value == JarvisState.LISTENING_COMMAND) {
                Log.d(TAG, "Command timeout — returning to wake word listening")
                _state.value = JarvisState.LISTENING_WAKE
                startListeningForWakeWord()
            }
        }
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying recognizer", e)
        }
        speechRecognizer = null
    }

    private fun playActivationTone() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            scope.launch {
                delay(200)
                toneGen.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not play activation tone", e)
        }
    }

    /**
     * Parse a recognized text string into a VoiceCommand.
     */
    fun parseCommand(text: String): VoiceCommand {
        val lower = text.lowercase().trim()

        return when {
            // Lock / Unlock
            lower == "lock" || lower == "lock screen" || lower == "lock the screen" ->
                VoiceCommand(text, CommandType.LOCK)
            lower == "unlock" || lower == "unlock screen" || lower == "unlock the screen" ->
                VoiceCommand(text, CommandType.UNLOCK)

            // Screenshot
            lower == "screenshot" || lower == "take screenshot" || lower == "take a screenshot" ->
                VoiceCommand(text, CommandType.SCREENSHOT)
            lower.contains("screenshot") && (lower.contains("area") || lower.contains("region")) ->
                VoiceCommand(text, CommandType.SCREENSHOT_AREA)

            // Clipboard
            lower == "copy" || lower == "copy that" ->
                VoiceCommand(text, CommandType.COPY)
            lower == "paste" || lower == "paste that" || lower == "paste it" ->
                VoiceCommand(text, CommandType.PASTE)
            lower == "cut" || lower == "cut that" ->
                VoiceCommand(text, CommandType.CUT)

            // Edit
            lower == "undo" -> VoiceCommand(text, CommandType.UNDO)
            lower == "redo" -> VoiceCommand(text, CommandType.REDO)
            lower == "select all" || lower == "select everything" ->
                VoiceCommand(text, CommandType.SELECT_ALL)
            lower == "find" || lower == "search" ->
                VoiceCommand(text, CommandType.FIND)
            lower == "save" || lower == "save file" || lower == "save it" ->
                VoiceCommand(text, CommandType.SAVE)

            // Window / App
            lower == "close" || lower == "close window" || lower == "close this" ->
                VoiceCommand(text, CommandType.CLOSE_WINDOW)
            lower == "switch" || lower == "switch app" || lower == "switch application" || lower == "switch window" ->
                VoiceCommand(text, CommandType.SWITCH_APP)
            lower == "new tab" || lower == "open new tab" ->
                VoiceCommand(text, CommandType.NEW_TAB)
            lower == "close tab" ->
                VoiceCommand(text, CommandType.CLOSE_TAB)
            lower == "show desktop" || lower == "desktop" ->
                VoiceCommand(text, CommandType.SHOW_DESKTOP)
            lower == "mission control" || lower == "overview" || lower == "task view" ->
                VoiceCommand(text, CommandType.MISSION_CONTROL)

            // System
            lower == "sleep" || lower == "sleep mode" || lower == "put to sleep" ->
                VoiceCommand(text, CommandType.SLEEP)
            lower == "force quit" || lower == "force close" || lower == "kill app" ->
                VoiceCommand(text, CommandType.FORCE_QUIT)

            // Media
            lower == "mute" || lower == "mute audio" || lower == "silence" ->
                VoiceCommand(text, CommandType.MUTE)
            lower == "volume up" || lower == "louder" || lower == "turn up" ->
                VoiceCommand(text, CommandType.VOLUME_UP)
            lower == "volume down" || lower == "quieter" || lower == "turn down" ->
                VoiceCommand(text, CommandType.VOLUME_DOWN)
            lower == "play" || lower == "pause" || lower == "play pause" || lower == "resume" ->
                VoiceCommand(text, CommandType.PLAY_PAUSE)
            lower == "brightness up" || lower == "brighter" ->
                VoiceCommand(text, CommandType.BRIGHTNESS_UP)
            lower == "brightness down" || lower == "dimmer" || lower == "dim" ->
                VoiceCommand(text, CommandType.BRIGHTNESS_DOWN)

            // Open app: "open Safari", "open Chrome", etc.
            lower.startsWith("open ") -> {
                val appName = text.substringAfter("open ", "").substringAfter("Open ", "").trim()
                VoiceCommand(text, CommandType.OPEN_APP, appName)
            }
            lower.startsWith("launch ") -> {
                val appName = text.substringAfter("launch ", "").substringAfter("Launch ", "").trim()
                VoiceCommand(text, CommandType.OPEN_APP, appName)
            }

            // Type text: "type hello world"
            lower.startsWith("type ") -> {
                val textToType = text.substringAfter("type ", "").substringAfter("Type ", "")
                VoiceCommand(text, CommandType.TYPE_TEXT, textToType)
            }

            else -> VoiceCommand(text, CommandType.UNKNOWN)
        }
    }

    private fun executeCommand(command: VoiceCommand) {
        _lastExecutedCommand.value = command.rawText
        _commandsExecutedCount.value++
        onExecuteCommand?.invoke(command)
    }

    /**
     * Check if text contains the wake word "hey sagar" (with fuzzy matching).
     * Returns the remainder of the text after the wake word, or null if not found.
     */
    private fun extractWakeWord(text: String): String? {
        val lower = text.lowercase().trim()
        for (wake in WAKE_PHRASES) {
            val idx = lower.indexOf(wake)
            if (idx >= 0) {
                return text.substring(idx + wake.length).trim()
            }
        }
        return null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Recognition Listeners
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Listens for the "Hey Sagar" wake word. On detection, transitions
     * to command listening mode.
     */
    private inner class WakeWordListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Wake listener ready")
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            Log.d(TAG, "Wake listener error: $error")
            // Restart listening after a brief delay (error codes 6, 7, 8 are common timeouts)
            if (_state.value == JarvisState.LISTENING_WAKE) {
                restartJob?.cancel()
                restartJob = scope.launch {
                    delay(RESTART_DELAY_MS)
                    if (_state.value == JarvisState.LISTENING_WAKE) {
                        startListeningForWakeWord()
                    }
                }
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                for (match in matches) {
                    _lastRecognizedText.value = match
                    val remainder = extractWakeWord(match)
                    if (remainder != null) {
                        _wakeWordDetectedCount.value++
                        _state.value = JarvisState.WAKE_DETECTED
                        Log.i(TAG, "Wake word detected! Remainder: '$remainder'")

                        // If the user said the command in the same breath as the wake word
                        if (remainder.isNotBlank()) {
                            handleCommandText(remainder)
                        } else {
                            startListeningForCommand()
                        }
                        return
                    }
                }
            }

            // No wake word found — restart listening
            if (_state.value == JarvisState.LISTENING_WAKE) {
                restartJob?.cancel()
                restartJob = scope.launch {
                    delay(RESTART_DELAY_MS)
                    if (_state.value == JarvisState.LISTENING_WAKE) {
                        startListeningForWakeWord()
                    }
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _lastRecognizedText.value = matches[0]
                // Check partial results for wake word too (faster response)
                val remainder = extractWakeWord(matches[0])
                if (remainder != null && remainder.isNotBlank()) {
                    _wakeWordDetectedCount.value++
                    _state.value = JarvisState.WAKE_DETECTED
                    Log.i(TAG, "Wake word in partial results! Command: '$remainder'")
                    handleCommandText(remainder)
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /**
     * Listens for a command after the wake word has been detected.
     */
    private inner class CommandListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Command listener ready")
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            Log.d(TAG, "Command listener error: $error")
            // Return to wake word listening on error
            if (_state.value == JarvisState.LISTENING_COMMAND) {
                _state.value = JarvisState.LISTENING_WAKE
                restartJob?.cancel()
                restartJob = scope.launch {
                    delay(RESTART_DELAY_MS)
                    startListeningForWakeWord()
                }
            }
        }

        override fun onResults(results: Bundle?) {
            commandTimeoutJob?.cancel()
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                handleCommandText(matches[0])
            } else {
                // No result — go back to wake listening
                _state.value = JarvisState.LISTENING_WAKE
                startListeningForWakeWord()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _lastRecognizedText.value = matches[0]
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /**
     * Process recognized command text and either execute or queue for confirmation.
     */
    private fun handleCommandText(text: String) {
        commandTimeoutJob?.cancel()
        _lastRecognizedText.value = text
        
        if (mode == JarvisMode.SMART && smartEngine != null) {
            _state.value = JarvisState.THINKING
            smartEngine?.processCommand(text)
            return
        }

        val command = parseCommand(text)

        if (command.commandType == CommandType.UNKNOWN) {
            Log.w(TAG, "Unknown command: '$text'")
            _state.value = JarvisState.LISTENING_WAKE
            scope.launch {
                delay(RESTART_DELAY_MS)
                startListeningForWakeWord()
            }
            return
        }

        if (_isConfirmationRequired.value) {
            // Queue for confirmation
            _pendingCommand.value = command
            _state.value = JarvisState.CONFIRMING
            destroyRecognizer() // Stop listening while showing confirmation
        } else {
            // Execute immediately
            _state.value = JarvisState.EXECUTING
            executeCommand(command)

            // Return to wake listening
            scope.launch {
                delay(800)
                _state.value = JarvisState.LISTENING_WAKE
                startListeningForWakeWord()
            }
        }
    }
    
    fun resetToListening() {
        _state.value = JarvisState.LISTENING_WAKE
        startListeningForWakeWord()
    }

    fun destroy() {
        stop()
        scope.cancel()
    }
}
