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
    private val _terminalLines = MutableStateFlow<List<String>>(emptyList())
    val terminalLines = _terminalLines.asStateFlow()

    private var process: Process? = null
    private var writer: PrintWriter? = null

    private val MAX_LINES = 1000
    private val linesBuffer = LinkedList<String>()

    init {
        startTerminal()
    }

    private fun startTerminal() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pb = ProcessBuilder("sh")
                pb.environment().apply {
                    put("TERM", "xterm")
                    put("HOME", getApplication<Application>().filesDir.absolutePath)
                }
                pb.directory(getApplication<Application>().filesDir)
                pb.redirectErrorStream(true) 
                
                val p = pb.start()
                process = p
                writer = PrintWriter(OutputStreamWriter(p.outputStream), true)
                
                appendLine(">> Android Native Shell Session Initiated")
                appendLine(">> Directory: ${getApplication<Application>().filesDir.absolutePath}")

                val reader = BufferedReader(InputStreamReader(p.inputStream))
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        val stripped = it.replace(Regex("\u001B\\[[;\\d]*m"), "")
                        appendLine(stripped)
                    }
                }
                
                p.waitFor()
                appendLine(">> Session Terminated. Exit value: ${p.exitValue()}")
                
            } catch (e: Exception) {
                appendLine(">> Shell error: ${e.message}")
            }
        }
    }

    fun sendCommand(command: String) {
        if (command.trim().equals("clear", ignoreCase = true)) {
            clearTerminal()
            return
        }
        
        // Don't show exit command to avoid confusion if we auto-restart
        if (command.trim().equals("exit", ignoreCase = true)) {
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

    fun clearTerminal() {
        viewModelScope.launch(Dispatchers.Main) {
            linesBuffer.clear()
            _terminalLines.value = emptyList()
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
