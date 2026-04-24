package com.example.rabit.ui.forensics

import android.app.Application
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream

data class ExifTag(
    val name: String,
    val value: String
)

class ExifForensicsViewModel(application: Application) : AndroidViewModel(application) {
    private val _tags = MutableStateFlow<List<ExifTag>>(emptyList())
    val tags = _tags.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val interestingTags = listOf(
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_IMAGE_WIDTH,
        ExifInterface.TAG_IMAGE_LENGTH,
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT
    )

    fun analyzeImage(uri: Uri) {
        _tags.value = emptyList()
        _error.value = null

        try {
            val inputStream: InputStream? = getApplication<Application>().contentResolver.openInputStream(uri)
            if (inputStream == null) {
                _error.value = "Could not read image file."
                return
            }

            val exif = ExifInterface(inputStream)
            val extractedTags = mutableListOf<ExifTag>()

            for (tag in interestingTags) {
                val value = exif.getAttribute(tag)
                if (!value.isNullOrBlank()) {
                    extractedTags.add(ExifTag(formatTagName(tag), value))
                }
            }

            if (extractedTags.isEmpty()) {
                _error.value = "No interesting EXIF metadata found. The image may have been stripped."
            } else {
                _tags.value = extractedTags
            }

            inputStream.close()
        } catch (e: Exception) {
            _error.value = "Forensic analysis failed: ${e.localizedMessage}"
        }
    }

    private fun formatTagName(rawTag: String): String {
        return rawTag.replace("([A-Z])".toRegex(), " $1").trim().uppercase()
    }
}
