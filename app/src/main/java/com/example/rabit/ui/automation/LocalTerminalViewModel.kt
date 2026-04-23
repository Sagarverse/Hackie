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

class LocalTerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val pkgManager = com.example.rabit.data.terminal.HackiePkgManager(application)
    private val _terminalLines = MutableStateFlow<List<String>>(emptyList())
    val terminalLines = _terminalLines.asStateFlow()

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
    private var isInteractiveMode = false

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

    fun sendCommand(command: String) {
        val trimmed = command.trim()
        
        if (trimmed.equals("clear", ignoreCase = true)) {
            clearTerminal()
            return
        }

        // --- Hackie Package Manager Interceptor ---
        if (trimmed.startsWith("hpkg install ")) {
            val tool = trimmed.removePrefix("hpkg install ").trim()
            viewModelScope.launch {
                appendLine("\$ $command")
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
        
        if (trimmed.equals("exit", ignoreCase = true)) {
             viewModelScope.launch { appendLine("\$ $command") }
             viewModelScope.launch(Dispatchers.IO) {
                 try {
                     writer?.println(command)
                     writer?.flush()
                 } catch (e: Exception) {}
             }
             return
        }

        viewModelScope.launch { appendLine("\$ $command") }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                writer?.println(command)
                writer?.flush()
            } catch (e: Exception) {
                appendLine(">> Write error: ${e.message}")
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
        // Very basic ANSI parser for cursor movement and simple colors
        if (raw.contains("\u001B[")) {
            isInteractiveMode = true
            val parts = raw.split("\u001B[")
            parts.forEachIndexed { index, part ->
                if (index == 0 && part.isNotEmpty()) {
                    writeToBuffer(part)
                } else if (part.isNotEmpty()) {
                    handleAnsiSequence(part)
                }
            }
        } else {
            if (!isInteractiveMode) {
                val lines = raw.split("\n")
                lines.forEach { line ->
                    if (line.isNotEmpty()) viewModelScope.launch { appendLine(line) }
                }
            } else {
                writeToBuffer(raw)
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
            isInteractiveMode = false
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
