package com.example.rabit.ui.crypto

import android.util.Base64
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLDecoder
import java.net.URLEncoder

class EncoderDecoderViewModel : ViewModel() {
    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    private val _base64Output = MutableStateFlow("")
    val base64Output = _base64Output.asStateFlow()

    private val _hexOutput = MutableStateFlow("")
    val hexOutput = _hexOutput.asStateFlow()

    private val _urlOutput = MutableStateFlow("")
    val urlOutput = _urlOutput.asStateFlow()

    private val _binaryOutput = MutableStateFlow("")
    val binaryOutput = _binaryOutput.asStateFlow()

    private val _rot13Output = MutableStateFlow("")
    val rot13Output = _rot13Output.asStateFlow()

    var isEncodingMode = true
        private set

    fun setMode(encode: Boolean) {
        isEncodingMode = encode
        processText(_inputText.value)
    }

    fun updateInput(text: String) {
        _inputText.value = text
        processText(text)
    }

    private fun processText(text: String) {
        if (text.isEmpty()) {
            _base64Output.value = ""
            _hexOutput.value = ""
            _urlOutput.value = ""
            _binaryOutput.value = ""
            _rot13Output.value = ""
            return
        }

        if (isEncodingMode) {
            _base64Output.value = try { Base64.encodeToString(text.toByteArray(), Base64.NO_WRAP) } catch (e: Exception) { "Error" }
            _hexOutput.value = text.toByteArray().joinToString("") { "%02X".format(it) }
            _urlOutput.value = try { URLEncoder.encode(text, "UTF-8") } catch (e: Exception) { "Error" }
            _binaryOutput.value = text.toByteArray().joinToString(" ") { 
                String.format("%8s", Integer.toBinaryString(it.toInt() and 0xFF)).replace(' ', '0') 
            }
            _rot13Output.value = text.map {
                when {
                    it.isLetter() && it.isUpperCase() -> ((it - 'A' + 13) % 26 + 'A'.code).toChar()
                    it.isLetter() && it.isLowerCase() -> ((it - 'a' + 13) % 26 + 'a'.code).toChar()
                    else -> it
                }
            }.joinToString("")
        } else {
            // Decoding Mode (Try to decode the input assuming it's the respective format)
            _base64Output.value = try { String(Base64.decode(text, Base64.NO_WRAP)) } catch (e: Exception) { "Invalid Base64" }
            _hexOutput.value = try {
                val cleanHex = text.replace("\\s+".toRegex(), "")
                val bytes = ByteArray(cleanHex.length / 2)
                for (i in bytes.indices) {
                    bytes[i] = cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                }
                String(bytes)
            } catch (e: Exception) { "Invalid Hex" }
            _urlOutput.value = try { URLDecoder.decode(text, "UTF-8") } catch (e: Exception) { "Invalid URL Encoding" }
            _binaryOutput.value = try {
                val cleanBin = text.replace("\\s+".toRegex(), "")
                val bytes = ByteArray(cleanBin.length / 8)
                for (i in bytes.indices) {
                    bytes[i] = cleanBin.substring(i * 8, i * 8 + 8).toInt(2).toByte()
                }
                String(bytes)
            } catch (e: Exception) { "Invalid Binary" }
            // ROT13 decoding is the same as encoding
            _rot13Output.value = text.map {
                when {
                    it.isLetter() && it.isUpperCase() -> ((it - 'A' + 13) % 26 + 'A'.code).toChar()
                    it.isLetter() && it.isLowerCase() -> ((it - 'a' + 13) % 26 + 'a'.code).toChar()
                    else -> it
                }
            }.joinToString("")
        }
    }
}
