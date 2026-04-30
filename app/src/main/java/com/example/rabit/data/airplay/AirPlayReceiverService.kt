package com.example.rabit.data.airplay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.provider.Settings
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sagar.rabit.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.text.SimpleDateFormat
import kotlin.random.Random
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * AirPlayReceiverService (experimental):
 * - Advertises Hackie as a RAOP service (_raop._tcp) on local Wi-Fi via NSD/mDNS.
 * - Keeps a foreground service alive for receiver lifecycle.
 * - Socket acceptance is currently a transport scaffold; full ALAC/RAOP decode requires
 *   a dedicated RAOP implementation/library.
 */
class AirPlayReceiverService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var nsdManager: NsdManager? = null
    private var raopRegistrationListener: NsdManager.RegistrationListener? = null
    private var airplayRegistrationListener: NsdManager.RegistrationListener? = null
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private val audioSink: RaopAudioSink by lazy { AudioTrackPcmSink(this) }
    private lateinit var raopEngine: RaopEngine

    override fun onCreate() {
        super.onCreate()
        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("rabit_airplay_lock").apply {
            setReferenceCounted(false)
        }
        raopEngine = StubRaopEngine(status = { updateRuntimeStatus(it) }, audioSink = audioSink)
        ensureChannel()
        AirPlayStateBus.publish("Idle")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TEST_TONE -> {
                scope.launch {
                    audioSink.playTestTone(1300)
                    updateRuntimeStatus("Audio pipeline test tone played")
                }
            }
            ACTION_STOP -> {
                stopReceiver()
                stopSelf()
            }
            else -> {
                updateRuntimeStatus("Starting AirPlay receiver")
                startReceiver()
            }
        }
        return START_STICKY
    }

    private fun startReceiver() {
        if (serverSocket != null) return

        scope.launch {
            try {
                multicastLock?.acquire()
                serverSocket = runCatching { ServerSocket(PREFERRED_RAOP_PORT) }
                    .getOrElse { ServerSocket(0) }
                val port = serverSocket?.localPort ?: return@launch
                
                val raopId = buildRaopId()
                val serviceDisplayName = "Hackie"
                
                registerRaopService(port, raopId, serviceDisplayName)
                registerAirPlayCompanionService(port, serviceDisplayName, raopId)
                
                updateRuntimeStatus("AirPlay ready on port $port")

                acceptJob = launch {
                    while (isActive) {
                        val socket = try {
                            serverSocket?.accept()
                        } catch (_: Exception) {
                            null
                        }
                        socket?.let {
                            launch { handleRtspClient(it) }
                        }
                    }
                }
            } catch (_: Exception) {
                updateRuntimeStatus("AirPlay start failed")
            }
        }
    }

    private fun stopReceiver() {
        try {
            unregisterRaopService()
        } catch (_: Exception) {
        }
        try {
            acceptJob?.cancel()
            acceptJob = null
        } catch (_: Exception) {
        }
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (_: Exception) {
        }
        try {
            multicastLock?.release()
        } catch (_: Exception) {
        }
        updateRuntimeStatus("Idle")
    }

    private fun handleRtspClient(socket: Socket) {
        val remote = "${socket.inetAddress?.hostAddress}:${socket.port}"
        updateRuntimeStatus("Client connected: $remote")
        raopEngine.onClientConnected(remote)

        try {
            socket.soTimeout = 60_000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = OutputStreamWriter(socket.getOutputStream())

            while (true) {
                val requestLine = reader.readLine() ?: break
                if (requestLine.isBlank()) continue

                val headers = mutableMapOf<String, String>()
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) break
                    val idx = line.indexOf(':')
                    if (idx > 0) {
                        headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
                    }
                }

                val parts = requestLine.split(' ')
                val method = parts.firstOrNull()?.uppercase() ?: "UNKNOWN"
                val uri = parts.getOrNull(1) ?: "*"
                val version = parts.getOrNull(2) ?: "RTSP/1.0"
                val cseq = headers["cseq"] ?: "1"
                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                var body = ""
                if (contentLength > 0) {
                    val discard = CharArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val n = reader.read(discard, read, contentLength - read)
                        if (n <= 0) break
                        read += n
                    }
                    body = String(discard, 0, read)
                }

                val request = RtspRequest(method = method, uri = uri, version = version, headers = headers, body = body)
                updateRuntimeStatus("RTSP $method from $remote")
                val response = raopEngine.handleRtsp(request)
                writer.write(buildRawRtspResponse(cseq, response))
                writer.flush()

                if (method == "TEARDOWN") {
                    break
                }
            }
        } catch (_: Exception) {
            updateRuntimeStatus("RTSP session ended")
        } finally {
            raopEngine.onClientDisconnected(remote)
            runCatching { socket.close() }
        }
    }

    private fun buildRawRtspResponse(cseq: String, response: RtspResponse): String {
        val gmt = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }.format(Date())
        val common = StringBuilder()
            .append("RTSP/1.0 ${response.statusCode} ${response.reason}\r\n")
            .append("CSeq: $cseq\r\n")
            .append("Date: $gmt\r\n")
            .append("Server: HackieRAOP/0.1\r\n")

        response.headers.forEach { (k, v) ->
            common.append("$k: $v\r\n")
        }

        if (response.body.isNotBlank()) {
            if ("Content-Type" !in response.headers && "content-type" !in response.headers) {
                common.append("Content-Type: text/parameters\r\n")
            }
            common.append("Content-Length: ${response.body.toByteArray().size}\r\n")
        }

        common.append("\r\n")
        if (response.body.isNotBlank()) {
            common.append(response.body)
        }
        return common.toString()
    }

    private fun registerRaopService(port: Int, raopId: String, serviceDisplayName: String) {
        val raopServiceName = "$raopId@$serviceDisplayName"
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = raopServiceName
            serviceType = "_raop._tcp"
            setPort(port)
            setAttribute("txtvers", "1")
            setAttribute("cn", "0,1")
            setAttribute("ch", "2")
            setAttribute("sr", "44100")
            setAttribute("ss", "16")
            setAttribute("tp", "UDP")
            setAttribute("vn", "65537")
            setAttribute("md", "0,1,2")
            setAttribute("sf", "0x4")
            setAttribute("am", "Hackie,1")
            setAttribute("da", "true")
            setAttribute("pw", "false")
            setAttribute("et", "0,1") // Indicate we can handle unencrypted (0) or encrypted (1) handshakes
            setAttribute("vs", "220.68")
            setAttribute("pk", "0")
        }

        raopRegistrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                updateRuntimeStatus("RAOP advertised as ${nsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                updateRuntimeStatus("RAOP advertise failed (error $errorCode)")
                if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE || errorCode == NsdManager.FAILURE_MAX_LIMIT) {
                    retryRaopRegistration(port, raopId, serviceDisplayName)
                }
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }

        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, raopRegistrationListener)
    }

    private fun registerAirPlayCompanionService(port: Int, serviceDisplayName: String, raopId: String) {
        val companionInfo = NsdServiceInfo().apply {
            serviceName = serviceDisplayName
            serviceType = "_airplay._tcp"
            setPort(port)
            setAttribute("deviceid", raopId.chunked(2).joinToString(":"))
            setAttribute("features", "0x445F8A00,0x1C340")
            setAttribute("flags", "0x4")
            setAttribute("model", "AppleTV3,2")
            setAttribute("manufacturer", "Hackie")
            setAttribute("serialNumber", raopId)
            setAttribute("srcvers", "220.68")
            setAttribute("pi", raopId.lowercase())
            setAttribute("pw", "false")
            setAttribute("vv", "2")
        }

        airplayRegistrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                updateRuntimeStatus("AirPlay companion advertised as ${nsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                updateRuntimeStatus("AirPlay companion advertise failed (error $errorCode)")
                if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE || errorCode == NsdManager.FAILURE_MAX_LIMIT) {
                    retryAirPlayRegistration(port, serviceDisplayName, raopId)
                }
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }

        nsdManager?.registerService(companionInfo, NsdManager.PROTOCOL_DNS_SD, airplayRegistrationListener)
    }

    private fun retryRaopRegistration(port: Int, raopId: String, serviceDisplayName: String) {
        val fallbackName = "$raopId@${serviceDisplayName}-${Random.nextInt(100, 999)}"
        val retryInfo = NsdServiceInfo().apply {
            serviceName = fallbackName
            serviceType = "_raop._tcp"
            setPort(port)
            setAttribute("txtvers", "1")
            setAttribute("cn", "0,1")
            setAttribute("ch", "2")
            setAttribute("sr", "44100")
            setAttribute("ss", "16")
            setAttribute("tp", "UDP")
            setAttribute("vn", "65537")
            setAttribute("md", "0,1,2")
            setAttribute("sf", "0x4")
            setAttribute("am", "Hackie,1")
            setAttribute("da", "true")
            setAttribute("pw", "false")
            setAttribute("et", "0,1")
            setAttribute("vs", "220.68")
            setAttribute("pk", "0")
        }
        runCatching {
            raopRegistrationListener?.let { nsdManager?.unregisterService(it) }
        }
        nsdManager?.registerService(retryInfo, NsdManager.PROTOCOL_DNS_SD, raopRegistrationListener)
        updateRuntimeStatus("Retrying RAOP advertisement as $fallbackName")
    }

    private fun retryAirPlayRegistration(port: Int, serviceDisplayName: String, raopId: String) {
        val fallbackName = "$serviceDisplayName-${Random.nextInt(100, 999)}"
        val retryInfo = NsdServiceInfo().apply {
            serviceName = fallbackName
            serviceType = "_airplay._tcp"
            setPort(port)
            setAttribute("deviceid", raopId.chunked(2).joinToString(":"))
            setAttribute("features", "0x445F8A00,0x1C340")
            setAttribute("flags", "0x4")
            setAttribute("model", "AppleTV3,2")
            setAttribute("manufacturer", "Hackie")
            setAttribute("serialNumber", raopId)
            setAttribute("srcvers", "220.68")
            setAttribute("pi", raopId.lowercase())
            setAttribute("pw", "false")
            setAttribute("vv", "2")
        }
        runCatching {
            airplayRegistrationListener?.let { nsdManager?.unregisterService(it) }
        }
        nsdManager?.registerService(retryInfo, NsdManager.PROTOCOL_DNS_SD, airplayRegistrationListener)
        updateRuntimeStatus("Retrying AirPlay companion advertisement as $fallbackName")
    }

    private fun unregisterRaopService() {
        raopRegistrationListener?.let { listener ->
            runCatching { nsdManager?.unregisterService(listener) }
        }
        raopRegistrationListener = null

        airplayRegistrationListener?.let { listener ->
            runCatching { nsdManager?.unregisterService(listener) }
        }
        airplayRegistrationListener = null
    }

    private fun buildRaopId(): String {
        val androidId = runCatching {
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        }.getOrDefault("")

        val hexSeed = androidId.uppercase().filter { it in "0123456789ABCDEF" }
        if (hexSeed.length >= 12) return hexSeed.take(12)

        val fallback = MessageDigest.getInstance("SHA-1")
            .digest((Build.MODEL + Build.DEVICE + Build.ID).toByteArray())
            .joinToString(separator = "") { "%02X".format(it) }
        return fallback.take(12)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, "Hackie AirPlay", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Hackie AirPlay Receiver")
            .setContentText(content)
            .setOngoing(true)
            .build()
    }

    private fun updateRuntimeStatus(content: String) {
        Log.d("RabitAirPlay", content)
        AirPlayStateBus.publish(content)
        startForeground(NOTIFICATION_ID, buildNotification(content))
    }

    override fun onDestroy() {
        stopReceiver()
        audioSink.release()
        raopEngine.shutdown()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.rabit.airplay.START"
        const val ACTION_STOP = "com.example.rabit.airplay.STOP"
        const val ACTION_TEST_TONE = "com.example.rabit.airplay.TEST_TONE"
        private const val PREFERRED_RAOP_PORT = 5000
        private const val CHANNEL_ID = "rabit_airplay_receiver"
        private const val NOTIFICATION_ID = 77
    }
}
