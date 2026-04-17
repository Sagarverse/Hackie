package com.example.rabit.data.storage.adb_tls

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.conscrypt.Conscrypt
import java.security.Security

object AdbTlsPairingManager {
    private const val TAG = "AdbTlsPairingManager"

    init {
        // Essential: Install Conscrypt as the primary security provider for TLS 1.3 and ADB Exporting
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun pairDevice(
        context: Context,
        host: String,
        port: Int,
        pairingCode: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
            val adbKeyStore = PreferenceAdbKeyStore(prefs)
            val adbKey = AdbKey(adbKeyStore, Build.MODEL)
            
            val client = AdbPairingClient(host, port, pairingCode, adbKey)
            client.use {
                if (it.start()) {
                    Log.d(TAG, "Pairing successful for $host:$port")
                    Result.success(Unit)
                } else {
                    Log.e(TAG, "Pairing failed for $host:$port")
                    Result.failure(Exception("Pairing failed. Check if the code is correct."))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during pairing", e)
            Result.failure(e)
        }
    }
    
    /**
     * Parse the ADB QR code string: WIFI:T:ADB;S:name;P:password;;
     */
    fun parseQrCode(qrString: String): Triple<String, Int, String>? {
        // Actually, the QR code for ADB in Android settings contains a different format usually
        // But the common one is WIFI:T:ADB;S:<pairing_service_name>;P:<pairing_password>;;
        // However, the service name is discovered via mDNS.
        // Actually, for "direct" pairing via QR, we usually get the IP and Port from the mDNS name if we are lucky,
        // or the QR code itself might contain the ip:port if it is a custom implementation.
        // Google's standard QR code format is a bit more complex.
        
        // Simplified for our implementation if we use a specific scanner logic.
        // Standard Android Wireless Debugging QR code:
        // WIFI:T:ADB;S:adb-xxxxxx;P:xxxxxx;;
        
        if (!qrString.startsWith("WIFI:T:ADB;")) return null
        
        val parts = qrString.split(";")
        var serviceName = ""
        var password = ""
        
        for (part in parts) {
            if (part.startsWith("S:")) serviceName = part.substring(2)
            if (part.startsWith("P:")) password = part.substring(2)
        }
        
        if (password.isEmpty()) return null
        
        // Note: The serviceName 'adb-xxxxxx' is an mDNS service name.
        // To get the IP/Port, we actually need to resolve it via NsdManager.
        // For simplicity in this 'retry', we assume the user might enter IP/Port manually 
        // OR we try to extract it if the QR code is in a non-standard IP:PORT format.
        
        return Triple(serviceName, 0, password)
    }
}
