package com.example.rabit.data.security

import android.content.Context
import com.example.rabit.data.security.NeuralTrafficAnalyzer.Packet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

/**
 * Traffic Persistence Layer for Hackie Pro.
 * Handles storage of forensic network logs.
 */
class TrafficStorageManager(private val context: Context) {
    private val gson = Gson()
    private val trafficFile = File(context.filesDir, "tactical_traffic_history.json")

    data class TrafficRecord(
        val timestamp: Long,
        val targetName: String,
        val packets: List<Packet>,
        val wasCompromised: Boolean
    )

    /**
     * Saves a new traffic record.
     */
    suspend fun saveTraffic(targetName: String, packets: List<Packet>, wasCompromised: Boolean) = withContext(Dispatchers.IO) {
        val records = getHistory().toMutableList()
        records.add(0, TrafficRecord(Date().time, targetName, packets, wasCompromised))
        
        // Keep last 30 traffic captures
        val limitedRecords = records.take(30)
        trafficFile.writeText(gson.toJson(limitedRecords))
    }

    /**
     * Retrieves the entire traffic history.
     */
    suspend fun getHistory(): List<TrafficRecord> = withContext(Dispatchers.IO) {
        if (!trafficFile.exists()) return@withContext emptyList()
        try {
            val json = trafficFile.readText()
            val type = object : TypeToken<List<TrafficRecord>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Clears all traffic history.
     */
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        if (trafficFile.exists()) trafficFile.delete()
    }
}
