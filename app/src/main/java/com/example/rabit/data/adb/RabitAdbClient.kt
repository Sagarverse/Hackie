package com.example.rabit.data.adb

import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import com.tananaev.adblib.AdbStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.Socket
import android.util.Log

class RabitAdbClient(private val crypto: AdbCrypto) {
    private var connection: AdbConnection? = null
    private var socket: Socket? = null

    companion object {
        private const val TAG = "RabitAdbClient"
    }
    suspend fun connectWifi(ip: String, port: Int) = withContext(Dispatchers.IO) {
        try {
            socket = Socket(ip, port)
            connection = AdbConnection.create(socket!!, crypto)
            connection!!.connect()
            Log.i(TAG, "Connected to ADB at $ip:$port")
        } catch (e: Exception) {
            Log.e(TAG, "ADB Connection failed: ${e.message}")
            disconnect()
            throw e
        }
    }

    suspend fun connectSocket(customSocket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket = customSocket
            connection = AdbConnection.create(socket!!, crypto)
            connection!!.connect()
            Log.i(TAG, "Connected to ADB via custom socket (USB)")
        } catch (e: Exception) {
            Log.e(TAG, "ADB Connection failed: ${e.message}")
            disconnect()
            throw e
        }
    }

    fun disconnect() {
        try { connection?.close() } catch (e: Exception) {}
        try { socket?.close() } catch (e: Exception) {}
        connection = null
        socket = null
    }

    suspend fun executeCommand(command: String): String = withContext(Dispatchers.IO) {
        val conn = connection ?: throw IllegalStateException("ADB Not Connected")
        val stream: AdbStream = conn.open("shell:$command")
        val output = ByteArrayOutputStream()
        
        try {
            while (!stream.isClosed) {
                val bytes = stream.read()
                if (bytes != null && bytes.isNotEmpty()) {
                    output.write(bytes)
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ADB Shell Stream error", e)
        }
        
        output.toByteArray().toString(Charsets.UTF_8).trimEnd()
    }

    suspend fun downloadFileBytes(remotePath: String): ByteArray = withContext(Dispatchers.IO) {
        val conn = connection ?: throw IllegalStateException("ADB Not Connected")
        // Request base64 so pseudo-terminal (if any) doesn't corrupt raw binary bytes with CRLF
        val stream: AdbStream = conn.open("shell:base64 '$remotePath'")
        val output = ByteArrayOutputStream()
        
        try {
            while (!stream.isClosed) {
                val bytes = stream.read()
                if (bytes != null && bytes.isNotEmpty()) {
                    output.write(bytes)
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ADB Shell Stream error", e)
        }
        
        // Decode base64 to get raw binary
        val base64String = output.toString(Charsets.UTF_8.name()).replace("\r", "").replace("\n", "")
        android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
    }

    suspend fun uploadFileBytes(remotePath: String, data: ByteArray) = withContext(Dispatchers.IO) {
        val conn = connection ?: throw IllegalStateException("ADB Not Connected")
        // base64 decode on target
        val base64Str = android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
        
        val stream: AdbStream = conn.open("shell:base64 -d > '$remotePath'")
        try {
            // Write to the stream
            val payload = base64Str.toByteArray(Charsets.UTF_8)
            // write in chunks? AdbStream has write(byte[])
            stream.write(payload)
            // close stream so process exits
            stream.close()
        } catch (e: Exception) {
            Log.e(TAG, "ADB Shell Stream write error", e)
            try { stream.close() } catch (_: Exception) {}
            throw e
        }
    }

    suspend fun openStream(command: String): AdbStream = withContext(Dispatchers.IO) {
        val conn = connection ?: throw IllegalStateException("ADB Not Connected")
        conn.open("shell:$command")
    }
}
