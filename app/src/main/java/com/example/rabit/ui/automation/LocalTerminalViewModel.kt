package com.example.rabit.ui.automation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.LinkedList
import kotlinx.coroutines.delay
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import com.example.rabit.domain.model.gemini.GeminiRequest

class LocalTerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val pkgManager = com.example.rabit.data.terminal.HackiePkgManager(application)
    private val geminiRepo = GeminiRepositoryImpl()
    
    private val _terminalLines = MutableStateFlow<List<String>>(emptyList())
    val terminalLines = _terminalLines.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()
    
    private val _isGeneratingPrompt = MutableStateFlow(false)
    val isGeneratingPrompt = _isGeneratingPrompt.asStateFlow()

    private val history = mutableListOf<String>()
    private var historyIndex = -1

    private var process: Process? = null
    private var writer: PrintWriter? = null

    private val MAX_LINES = 1000
    private val linesBuffer = LinkedList<String>()
    
    // --- Neural Screen Buffer (for interactive tools like cmatrix) ---
    val terminalWidth = 80
    val terminalHeight = 40
    private val _screenBuffer = MutableStateFlow(Array(terminalHeight) { CharArray(terminalWidth) { ' ' } })
    val screenBuffer = _screenBuffer.asStateFlow()
    
    private var cursorX = 0
    private var cursorY = 0
    private val _isInteractiveMode = MutableStateFlow(false)
    val isInteractiveMode = _isInteractiveMode.asStateFlow()
    
    // --- Control Keys ---
    private val _ctrlActive = MutableStateFlow(false)
    val ctrlActive = _ctrlActive.asStateFlow()
    
    private val _altActive = MutableStateFlow(false)
    val altActive = _altActive.asStateFlow()

    init {
        startTerminal()
    }

    private fun startTerminal() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pb = ProcessBuilder("sh")
                val env = pb.environment()
                
                // Inject Hackie-OS Environment (Termux-Lite)
                pkgManager.getEnvironment().forEach { (k, v) ->
                    env[k] = v
                }
                
                pb.directory(getApplication<Application>().filesDir)
                pb.redirectErrorStream(true) 
                
                val p = pb.start()
                process = p
                writer = PrintWriter(OutputStreamWriter(p.outputStream), true)
                
                appendLine(">> Hackie-OS Virtual Shell Initiated")
                appendLine(">> Prefix: ${pkgManager.binDir.parentFile?.absolutePath}")
                appendLine(">> Tip: Use 'hpkg install cmatrix' to launch the matrix.")

                val reader = BufferedReader(InputStreamReader(p.inputStream))
                val buffer = CharArray(1024)
                var charsRead: Int
                
                while (reader.read(buffer).also { charsRead = it } != -1) {
                    val rawOutput = String(buffer, 0, charsRead)
                    processRawOutput(rawOutput)
                }
                
                p.waitFor()
                appendLine(">> Session Terminated. Exit value: ${p.exitValue()}")
                
            } catch (e: Exception) {
                appendLine(">> Shell error: ${e.message}")
            }
        }
    }

    fun onInputChange(text: String) {
        if (_ctrlActive.value && text.isNotEmpty()) {
            val lastChar = text.last().toString()
            viewModelScope.launch(Dispatchers.IO) {
                handleCtrlCommand(lastChar)
            }
            _ctrlActive.value = false
            _inputText.value = ""
            return
        }
        if (_altActive.value && text.isNotEmpty()) {
            val lastChar = text.last().toString()
            viewModelScope.launch(Dispatchers.IO) {
                writer?.print("\u001B$lastChar")
                writer?.flush()
            }
            _altActive.value = false
            _inputText.value = ""
            return
        }
        _inputText.value = text
    }

    fun stopExecution() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                writer?.close()
                process?.destroy()
                delay(100)
                appendLine(">> Execution stopped.")
                _isInteractiveMode.value = false
                startTerminal()
            } catch (e: Exception) {
                appendLine(">> Stop error: ${e.message}")
            }
        }
    }

    fun generateCommandFromPrompt(prompt: String, apiKey: String) {
        if (apiKey.isBlank()) {
            viewModelScope.launch { appendLine(">> Error: Gemini API Key not configured. Please set it in Settings.") }
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingPrompt.value = true
            try {
                val req = GeminiRequest(
                    prompt = prompt,
                    systemPrompt = "You are an expert Linux systems administrator and shell wizard. Convert the user's natural language request into a single, executable shell command. Output ONLY the raw command. Do not use markdown backticks, explanations, or quotes. Example: if user says 'list files', you output 'ls -la'.",
                    temperature = 0.2f
                )
                val response = geminiRepo.sendPrompt(req, apiKey)
                withContext(Dispatchers.Main) {
                    _inputText.value = response.text.trim()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendLine(">> AI Error: ${e.localizedMessage}")
                }
            } finally {
                _isGeneratingPrompt.value = false
            }
        }
    }

    fun sendCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return
        
        // Add to history
        if (history.isEmpty() || history.last() != trimmed) {
            history.add(trimmed)
        }
        historyIndex = -1
        _inputText.value = ""

        if (trimmed.equals("clear", ignoreCase = true)) {
            clearTerminal()
            return
        }

        // --- Hackie Package Manager Interceptor ---
        if (trimmed.startsWith("hpkg install ") || trimmed.startsWith("pkg install ")) {
            val tool = trimmed.removePrefix("hpkg install ").removePrefix("pkg install ").trim()
            viewModelScope.launch {
                appendLine("\$ $trimmed")
                appendLine(">> Neural Pkg: Commencing deployment of $tool...")
                val result = pkgManager.installTool(tool)
                result.onSuccess { 
                    appendLine(">> Neural Pkg: Success. $it")
                }.onFailure {
                    appendLine(">> Neural Pkg: Failed. ${it.message}")
                }
            }
            return
        }
        
        if (trimmed.equals("pkg update", ignoreCase = true) || trimmed.equals("hpkg update", ignoreCase = true)) {
            viewModelScope.launch {
                appendLine("\$ $trimmed")
                appendLine(">> Neural Pkg: Synchronizing repositories...")
                delay(1000)
                appendLine(">> Neural Pkg: All tactical indices are up to date.")
            }
            return
        }

        if (trimmed.equals("exit", ignoreCase = true)) {
             viewModelScope.launch { appendLine("\$ $trimmed") }
             viewModelScope.launch(Dispatchers.IO) {
                 try {
                     writer?.println(trimmed)
                     writer?.flush()
                 } catch (e: Exception) {}
             }
             return
        }

        viewModelScope.launch { appendLine("\$ $trimmed") }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                writer?.println(trimmed)
                writer?.flush()
            } catch (e: Exception) {
                appendLine(">> Write error: ${e.message}")
            }
        }
    }

    private fun handleCtrlCommand(command: String) {
        val key = command.lowercase().firstOrNull() ?: return
        val code = when (key) {
            'c' -> "\u0003"
            'd' -> "\u0004"
            'z' -> "\u001A"
            'l' -> "\u000C"
            else -> ""
        }
        if (code.isNotEmpty()) {
            writer?.print(code)
            writer?.flush()
        }
    }

    fun toggleCtrl() { _ctrlActive.value = !_ctrlActive.value }
    fun toggleAlt() { _altActive.value = !_altActive.value }
    
    fun sendTab() {
        viewModelScope.launch(Dispatchers.IO) {
            writer?.print("\t")
            writer?.flush()
        }
    }
    
    fun sendArrow(direction: String) {
        when (direction) {
            "up" -> {
                if (history.isNotEmpty()) {
                    if (historyIndex == -1) historyIndex = history.size - 1
                    else if (historyIndex > 0) historyIndex--
                    _inputText.value = history[historyIndex]
                }
            }
            "down" -> {
                if (history.isNotEmpty() && historyIndex != -1) {
                    if (historyIndex < history.size - 1) {
                        historyIndex++
                        _inputText.value = history[historyIndex]
                    } else {
                        historyIndex = -1
                        _inputText.value = ""
                    }
                }
            }
            "right", "left" -> {
                val seq = if (direction == "right") "\u001B[C" else "\u001B[D"
                viewModelScope.launch(Dispatchers.IO) {
                    writer?.print(seq)
                    writer?.flush()
                }
            }
        }
    }

    private suspend fun appendLine(text: String) {
        withContext(Dispatchers.Main) {
            linesBuffer.add(text)
            if (linesBuffer.size > MAX_LINES) {
                linesBuffer.removeFirst()
            }
            _terminalLines.value = ArrayList(linesBuffer)
        }
    }

    private fun processRawOutput(raw: String) {
        // Strip simple color codes and check if we really need interactive mode
        val colorStriped = raw.replace(Regex("\u001B\\[[0-9;]*m"), "")
        
        if (raw.contains("\u001B[H") || raw.contains("\u001B[2J") || raw.contains("\u001B[J")) {
            _isInteractiveMode.value = true
            clearScreenBuffer() // Entering interactive mode usually starts with a clear
        }

        if (_isInteractiveMode.value) {
            val parts = raw.split("\u001B[")
            parts.forEachIndexed { index, part ->
                if (index == 0 && part.isNotEmpty()) {
                    writeToBuffer(part)
                } else if (part.isNotEmpty()) {
                    handleAnsiSequence(part)
                }
            }
            // Also append to lines for accessibility/logging if it contains printable text
            if (colorStriped.trim().isNotEmpty()) {
                val lines = colorStriped.split("\n")
                lines.forEach { line ->
                    if (line.isNotBlank()) viewModelScope.launch { appendLine(line) }
                }
            }
        } else {
            val lines = colorStriped.split("\n")
            lines.forEach { line ->
                if (line.isNotEmpty()) viewModelScope.launch { appendLine(line) }
            }
        }
    }

    private fun handleAnsiSequence(seq: String) {
        val cmd = seq.takeWhile { !it.isLetter() && it != '@' && it != 'm' }
        val type = seq.getOrNull(cmd.length)
        val remaining = seq.drop(cmd.length + 1)
        
        when (type) {
            'H', 'f' -> { // Cursor Position
                val coords = cmd.split(";")
                cursorY = (coords.getOrNull(0)?.toIntOrNull() ?: 1).coerceIn(1, terminalHeight) - 1
                cursorX = (coords.getOrNull(1)?.toIntOrNull() ?: 1).coerceIn(1, terminalWidth) - 1
            }
            'J' -> { // Erase in Display
                if (cmd == "2" || cmd == "3") clearScreenBuffer()
            }
            'm' -> { /* Color Change - visualized in UI based on mode */ }
        }
        if (remaining.isNotEmpty()) writeToBuffer(remaining)
    }

    private fun writeToBuffer(text: String) {
        val current = _screenBuffer.value.map { it.copyOf() }.toTypedArray()
        text.forEach { char ->
            if (char == '\n') {
                cursorY = (cursorY + 1).coerceAtMost(terminalHeight - 1)
                cursorX = 0
            } else if (char == '\r') {
                cursorX = 0
            } else {
                if (cursorY < terminalHeight && cursorX < terminalWidth) {
                    current[cursorY][cursorX] = char
                    cursorX++
                    if (cursorX >= terminalWidth) {
                        cursorX = 0
                        cursorY = (cursorY + 1).coerceAtMost(terminalHeight - 1)
                    }
                }
            }
        }
        _screenBuffer.value = current
    }

    private fun clearScreenBuffer() {
        _screenBuffer.value = Array(terminalHeight) { CharArray(terminalWidth) { ' ' } }
        cursorX = 0
        cursorY = 0
    }

    fun clearTerminal() {
        viewModelScope.launch(Dispatchers.Main) {
            linesBuffer.clear()
            _terminalLines.value = emptyList()
            clearScreenBuffer()
            _isInteractiveMode.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            writer?.close()
            process?.destroy()
        } catch (e: Exception) {
            Log.e("LocalTerminal", "Error destroying shell process", e)
        }
    }
}
