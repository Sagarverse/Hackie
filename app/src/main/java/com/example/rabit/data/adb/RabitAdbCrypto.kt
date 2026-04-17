package com.example.rabit.data.adb

import android.content.Context
import android.util.Base64
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbCrypto
import java.io.File

object RabitAdbCrypto {

    private class AndroidAdbBase64 : AdbBase64 {
        override fun encodeToString(data: ByteArray): String {
            return Base64.encodeToString(data, Base64.NO_WRAP)
        }
    }

    fun getCrypto(context: Context): AdbCrypto {
        val base64 = AndroidAdbBase64()
        val pubFile = File(context.filesDir, "adb_key.pub")
        val privFile = File(context.filesDir, "adb_key")

        if (pubFile.exists() && privFile.exists()) {
            try {
                return AdbCrypto.loadAdbKeyPair(base64, privFile, pubFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val crypto = AdbCrypto.generateAdbKeyPair(base64)
        crypto.saveAdbKeyPair(privFile, pubFile)
        return crypto
    }
}
