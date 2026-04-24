package com.example.rabit.ui.opsec

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class WipeResult(
    val category: String,
    val filesDeleted: Int,
    val bytesFreed: Long
)

class KillSwitchViewModel(application: Application) : AndroidViewModel(application) {
    private val _wipeResults = MutableStateFlow<List<WipeResult>>(emptyList())
    val wipeResults = _wipeResults.asStateFlow()

    private val _isWiping = MutableStateFlow(false)
    val isWiping = _isWiping.asStateFlow()

    private val _totalBytesFreed = MutableStateFlow(0L)
    val totalBytesFreed = _totalBytesFreed.asStateFlow()

    fun executeKillSwitch() {
        if (_isWiping.value) return
        _isWiping.value = true
        _wipeResults.value = emptyList()
        _totalBytesFreed.value = 0L

        val context = getApplication<Application>()
        val results = mutableListOf<WipeResult>()

        // 1. Wipe app cache
        val cacheResult = wipeDirectory(context.cacheDir, "App Cache")
        results.add(cacheResult)

        // 2. Wipe external cache
        context.externalCacheDir?.let { dir ->
            val extCacheResult = wipeDirectory(dir, "External Cache")
            results.add(extCacheResult)
        }

        // 3. Wipe stego files
        val stegoFiles = context.cacheDir.listFiles { f -> f.name.startsWith("stego_") }
        var stegoCount = 0
        var stegoBytes = 0L
        stegoFiles?.forEach { f ->
            stegoBytes += f.length()
            f.delete()
            stegoCount++
        }
        results.add(WipeResult("Steganography Files", stegoCount, stegoBytes))

        // 4. Wipe shared preferences for sensitive data
        val prefsDir = File(context.filesDir.parentFile, "shared_prefs")
        val sensitivePrefs = listOf("scan_history", "cracked_hashes", "osint_results", "network_logs")
        var prefsDeleted = 0
        var prefsBytes = 0L
        sensitivePrefs.forEach { name ->
            val pref = File(prefsDir, "$name.xml")
            if (pref.exists()) {
                prefsBytes += pref.length()
                pref.delete()
                prefsDeleted++
            }
        }
        results.add(WipeResult("Sensitive Preferences", prefsDeleted, prefsBytes))

        // 5. Wipe databases (except Room which is critical)
        val dbDir = File(context.filesDir.parentFile, "databases")
        val sensitiveDbPatterns = listOf("forensic", "pcap", "exploit", "hash")
        var dbDeleted = 0
        var dbBytes = 0L
        dbDir.listFiles()?.forEach { f ->
            if (sensitiveDbPatterns.any { pattern -> f.name.contains(pattern, ignoreCase = true) }) {
                dbBytes += f.length()
                f.delete()
                dbDeleted++
            }
        }
        results.add(WipeResult("Forensic Databases", dbDeleted, dbBytes))

        // 6. Wipe internal files directory for logs
        val logFiles = context.filesDir.listFiles { f -> f.name.endsWith(".log") || f.name.endsWith(".pcap") || f.name.endsWith(".txt") }
        var logCount = 0
        var logBytes = 0L
        logFiles?.forEach { f ->
            logBytes += f.length()
            f.delete()
            logCount++
        }
        results.add(WipeResult("Operation Logs", logCount, logBytes))

        _wipeResults.value = results
        _totalBytesFreed.value = results.sumOf { it.bytesFreed }
        _isWiping.value = false
    }

    private fun wipeDirectory(dir: File, label: String): WipeResult {
        var count = 0
        var bytes = 0L
        dir.listFiles()?.forEach { f ->
            if (f.isDirectory) {
                val sub = wipeDirectory(f, "")
                count += sub.filesDeleted
                bytes += sub.bytesFreed
                f.delete()
            } else {
                bytes += f.length()
                f.delete()
                count++
            }
        }
        return WipeResult(label, count, bytes)
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_048_576 -> "%.2f MB".format(bytes / 1_048_576.0)
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
