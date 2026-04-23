package com.example.rabit.data.security

import android.content.Context
import com.example.rabit.data.security.NeuralAuditEngine.Finding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

/**
 * Tactical Persistence Layer for Hackie Pro.
 * Handles storage and retrieval of forensic audit history.
 */
class TacticalStorageManager(private val context: Context) {
    private val gson = Gson()
    private val historyFile = File(context.filesDir, "tactical_audit_history.json")

    data class AuditRecord(
        val timestamp: Long,
        val targetName: String,
        val findings: List<Finding>
    )

    /**
     * Saves a new audit record to the history.
     */
    suspend fun saveAudit(targetName: String, findings: List<Finding>) = withContext(Dispatchers.IO) {
        val records = getHistory().toMutableList()
        records.add(0, AuditRecord(Date().time, targetName, findings))
        
        // Keep only last 50 audits to prevent bloat
        val limitedRecords = records.take(50)
        historyFile.writeText(gson.toJson(limitedRecords))
    }

    /**
     * Retrieves the entire audit history.
     */
    suspend fun getHistory(): List<AuditRecord> = withContext(Dispatchers.IO) {
        if (!historyFile.exists()) return@withContext emptyList()
        try {
            val json = historyFile.readText()
            val type = object : TypeToken<List<AuditRecord>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Clears all tactical history.
     */
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        if (historyFile.exists()) historyFile.delete()
    }
}
