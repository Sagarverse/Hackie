package com.example.rabit.ui.steganography

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

sealed class StegoState {
    object Idle : StegoState()
    object Processing : StegoState()
    data class Encoded(val message: String) : StegoState()
    data class Decoded(val hiddenMessage: String) : StegoState()
    data class Error(val reason: String) : StegoState()
}

class SteganographyViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<StegoState>(StegoState.Idle)
    val state = _state.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri = _selectedImageUri.asStateFlow()

    // Magic marker to detect end of hidden message
    private val END_MARKER = "##END##"

    fun selectImage(uri: Uri) {
        _selectedImageUri.value = uri
        _state.value = StegoState.Idle
    }

    fun encodeMessage(message: String) {
        val uri = _selectedImageUri.value ?: run {
            _state.value = StegoState.Error("No image selected.")
            return
        }
        if (message.isBlank()) {
            _state.value = StegoState.Error("Message cannot be empty.")
            return
        }

        _state.value = StegoState.Processing

        try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri) ?: throw Exception("Cannot read image")
            val bitmap = BitmapFactory.decodeStream(inputStream).copy(Bitmap.Config.ARGB_8888, true)
            inputStream.close()

            val fullMessage = message + END_MARKER
            val messageBits = fullMessage.toByteArray().flatMap { byte ->
                (7 downTo 0).map { bit -> (byte.toInt() shr bit) and 1 }
            }

            val maxBits = bitmap.width * bitmap.height * 3 // 3 channels (RGB)
            if (messageBits.size > maxBits) {
                _state.value = StegoState.Error("Message too large for this image. Max ~${maxBits / 8} bytes.")
                return
            }

            var bitIndex = 0
            outer@ for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    if (bitIndex >= messageBits.size) break@outer
                    var pixel = bitmap.getPixel(x, y)

                    val a = (pixel shr 24) and 0xFF
                    var r = (pixel shr 16) and 0xFF
                    var g = (pixel shr 8) and 0xFF
                    var b = pixel and 0xFF

                    if (bitIndex < messageBits.size) { r = (r and 0xFE) or messageBits[bitIndex]; bitIndex++ }
                    if (bitIndex < messageBits.size) { g = (g and 0xFE) or messageBits[bitIndex]; bitIndex++ }
                    if (bitIndex < messageBits.size) { b = (b and 0xFE) or messageBits[bitIndex]; bitIndex++ }

                    bitmap.setPixel(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
                }
            }

            // Save encoded image
            val outStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)

            // Save to public Download folder using MediaStore
            val fileName = "stego_encoded_${System.currentTimeMillis()}.png"
            val resolver = getApplication<Application>().contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val imageUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    outputStream.write(outStream.toByteArray())
                }
                _state.value = StegoState.Encoded("Message hidden! Image saved to Downloads folder as $fileName")
            } else {
                _state.value = StegoState.Error("Failed to create file in Downloads.")
            }
        } catch (e: Exception) {
            _state.value = StegoState.Error("Encoding failed: ${e.localizedMessage}")
        }
    }

    fun decodeMessage() {
        val uri = _selectedImageUri.value ?: run {
            _state.value = StegoState.Error("No image selected.")
            return
        }

        _state.value = StegoState.Processing

        try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri) ?: throw Exception("Cannot read image")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val bits = mutableListOf<Int>()
            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    val pixel = bitmap.getPixel(x, y)
                    bits.add((pixel shr 16) and 1) // R LSB
                    bits.add((pixel shr 8) and 1)  // G LSB
                    bits.add(pixel and 1)           // B LSB
                }
            }

            // Convert bits to bytes
            val sb = StringBuilder()
            for (i in 0 until bits.size - 7 step 8) {
                var byte = 0
                for (j in 0..7) {
                    byte = (byte shl 1) or bits[i + j]
                }
                sb.append(byte.toChar())

                // Check for end marker
                if (sb.endsWith(END_MARKER)) {
                    val hidden = sb.toString().removeSuffix(END_MARKER)
                    _state.value = StegoState.Decoded(hidden)
                    return
                }
            }

            _state.value = StegoState.Error("No hidden message detected in this image.")
        } catch (e: Exception) {
            _state.value = StegoState.Error("Decoding failed: ${e.localizedMessage}")
        }
    }

    fun reset() {
        _state.value = StegoState.Idle
        _selectedImageUri.value = null
    }
}
