package com.example.rabit.data.adb

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.tananaev.adblib.AdbStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class AdbMirrorStreamer(
    private val adbClient: RabitAdbClient,
    private val scope: CoroutineScope
) {
    private var decoder: MediaCodec? = null
    private var streamingJob: Job? = null
    private var adbStream: AdbStream? = null

    companion object {
        private const val TAG = "AdbMirrorStreamer"
        private const val VIDEO_MIME = "video/avc"
    }

    fun startMirroring(surface: Surface, width: Int = 720, height: Int = 1280, bitRate: Int = 4_000_000) {
        stopMirroring()
        
        streamingJob = scope.launch(Dispatchers.IO) {
            try {
                // Configure Decoder
                val format = MediaFormat.createVideoFormat(VIDEO_MIME, width, height)
                decoder = MediaCodec.createDecoderByType(VIDEO_MIME)
                decoder?.configure(format, surface, null, 0)
                decoder?.start()

                Log.d(TAG, "Starting screenrecord stream...")
                // time-limit 0 for infinite, but we'll stop when stream closes
                val command = "screenrecord --output-format=h264 --size ${width}x${height} --bit-rate $bitRate --time-limit 0 -"
                adbStream = adbClient.openStream(command)

                val bufferInfo = MediaCodec.BufferInfo()
                
                while (isActive && adbStream?.isClosed == false) {
                    val rawData = adbStream?.read() ?: break
                    if (rawData.isEmpty()) continue

                    // Feed data to decoder
                    val inputIndex = decoder?.dequeueInputBuffer(10_000) ?: -1
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder?.getInputBuffer(inputIndex)
                        inputBuffer?.clear()
                        inputBuffer?.put(rawData)
                        decoder?.queueInputBuffer(inputIndex, 0, rawData.size, System.nanoTime() / 1000, 0)
                    }

                    // Release output buffers to surface
                    var outputIndex = decoder?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
                    while (outputIndex >= 0) {
                        decoder?.releaseOutputBuffer(outputIndex, true)
                        outputIndex = decoder?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Mirroring error", e)
            } finally {
                cleanup()
            }
        }
    }

    fun stopMirroring() {
        streamingJob?.cancel()
        streamingJob = null
        cleanup()
    }

    private fun cleanup() {
        try { adbStream?.close() } catch (e: Exception) {}
        try {
            decoder?.stop()
            decoder?.release()
        } catch (e: Exception) {}
        decoder = null
        adbStream = null
    }
}
