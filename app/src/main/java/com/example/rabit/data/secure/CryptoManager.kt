package com.example.rabit.data.secure

import android.util.Base64
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val SALT_LENGTH_BYTE = 16
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256

    /**
     * Derives a 256-bit AES key from a PIN and salt using PBKDF2.
     */
    fun deriveKey(pin: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    /**
     * Encrypts plaintext using AES-GCM with a key derived from the PIN.
     * Returns a Base64 string containing: salt(16) + iv(12) + ciphertext
     */
    fun encrypt(plaintext: String, pin: String): String? {
        return try {
            val salt = ByteArray(SALT_LENGTH_BYTE).apply { SecureRandom().nextBytes(this) }
            val iv = ByteArray(IV_LENGTH_BYTE).apply { SecureRandom().nextBytes(this) }
            
            val key = deriveKey(pin, salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BIT, iv))
            
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            
            val combined = ByteArray(salt.size + iv.size + ciphertext.size)
            System.arraycopy(salt, 0, combined, 0, salt.size)
            System.arraycopy(iv, 0, combined, salt.size, iv.size)
            System.arraycopy(ciphertext, 0, combined, salt.size + iv.size, ciphertext.size)
            
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decrypts a Base64 string encrypted with the above method.
     */
    fun decrypt(base64Ciphertext: String, pin: String): String? {
        return try {
            val combined = Base64.decode(base64Ciphertext, Base64.NO_WRAP)
            
            val salt = combined.copyOfRange(0, SALT_LENGTH_BYTE)
            val iv = combined.copyOfRange(SALT_LENGTH_BYTE, SALT_LENGTH_BYTE + IV_LENGTH_BYTE)
            val ciphertext = combined.copyOfRange(SALT_LENGTH_BYTE + IV_LENGTH_BYTE, combined.size)
            
            val key = deriveKey(pin, salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BIT, iv))
            
            val plaintext = cipher.doFinal(ciphertext)
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
