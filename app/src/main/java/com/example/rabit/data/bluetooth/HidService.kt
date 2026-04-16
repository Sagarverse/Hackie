package com.example.rabit.data.bluetooth

import android.app.*
import android.bluetooth.BluetoothDevice
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.rabit.data.secure.SecureStorage
import com.example.rabit.data.network.RabitNetworkServer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import com.example.rabit.domain.model.HidKeyCodes
import com.example.rabit.MainActivity

class HidService : Service() {

    private lateinit var hidDeviceManager: HidDeviceManager
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val channelId = "hid_service_channel"
    private val clipboardChannelId = "clipboard_channel"
    private val mediaChannelId = "media_playback_channel"
    private val mediaNotificationId = 3
    private lateinit var encryptionManager: com.example.rabit.data.secure.EncryptionManager
    private lateinit var proximitySmartLockManager: ProximitySmartLockManager
    
    companion object {
        const val ACTION_START_WEB_BRIDGE = "com.example.rabit.ACTION_START_WEB_BRIDGE"
        const val ACTION_STOP_WEB_BRIDGE = "com.example.rabit.ACTION_STOP_WEB_BRIDGE"
        const val ACTION_UPDATE_PROXIMITY_SMART_LOCK = "com.example.rabit.ACTION_UPDATE_PROXIMITY_SMART_LOCK"
        const val ACTION_UPDATE_NOW_PLAYING = "com.example.rabit.ACTION_UPDATE_NOW_PLAYING"
    }

    private lateinit var clipboard: ClipboardManager
    private var lastClipboardText: String? = null
    private var clipboardJob: Job? = null
    private var notificationStateTitle: String = "Disconnected"
    private var notificationStateText: String = "Bluetooth HID connection is inactive."
    private var nowPlayingTitle: String = "No track"
    private var nowPlayingArtist: String = "Connect companion for metadata"
    private var nowPlayingAlbum: String = ""
    private var nowPlayingArtworkBase64: String? = null
    private var playbackState: String = "paused"

    inner class LocalBinder : Binder() {
        fun getService(): HidService = this@HidService
    }

    override fun onCreate() {
        super.onCreate()
        hidDeviceManager = HidDeviceManager.getInstance(this)
        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Keep now-playing ingestion owned by the service so metadata continues
        // to update even when no UI ViewModel is active.
        RabitNetworkServer.nowPlayingReceiver = { payload ->
            updateNowPlayingState(
                title = payload.title,
                artist = payload.artist,
                album = payload.album,
                artworkBase64 = payload.artworkBase64,
                state = payload.playbackState
            )
        }

        encryptionManager = com.example.rabit.data.secure.EncryptionManager(this)
        proximitySmartLockManager = ProximitySmartLockManager(
            context = this,
            hidDeviceManager = hidDeviceManager,
            connectionState = hidDeviceManager.connectionState,
            scope = serviceScope
        )

        // Legacy network listeners removed for File Hub focus

        createNotificationChannels()
        loadNowPlayingFromPrefs()
        startForeground(1, buildNotification("Disconnected", "Bluetooth HID connection is inactive."))
        observeConnectionState()
        startClipboardObserver()
        
        // Check if Web Bridge should auto-start from preferences
        val prefs = getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("web_bridge_enabled", false)) {
            serviceScope.launch(Dispatchers.IO) {
                RabitNetworkServer.start(this@HidService, encryptionManager)
            }
        }
        if (prefs.getBoolean("proximity_auto_unlock_enabled", false)) {
            proximitySmartLockManager.start()
        }
    }

    private fun startClipboardObserver() {
        clipboardJob?.cancel()
        
        try {
            val currentClip = clipboard.primaryClip
            if (currentClip != null && currentClip.itemCount > 0) {
                lastClipboardText = currentClip.getItemAt(0).text?.toString()
            }
        } catch (e: Exception) { }

        clipboardJob = serviceScope.launch {
            while (isActive) {
                try {
                    val primaryClip = clipboard.primaryClip
                    if (primaryClip != null && primaryClip.itemCount > 0) {
                        val text = primaryClip.getItemAt(0).text?.toString()
                        if (text != lastClipboardText && !text.isNullOrBlank()) {
                            lastClipboardText = text
                            val prefs = getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
                            val isAutoPush = prefs.getBoolean("auto_push_enabled", false)
                            
                            if (isAutoPush) {
                                sendText(text)
                            } else {
                                showClipboardNotification(text)
                            }
                        }
                    }
                } catch (e: Exception) { }
                delay(3000)
            }
        }
    }

    private fun observeConnectionState() {
        serviceScope.launch {
            hidDeviceManager.connectionState.collect { state ->
                val (title, text) = when (state) {
                    is HidDeviceManager.ConnectionState.Connected -> {
                        "Connected" to "Active connection to ${state.deviceName}"
                    }
                    is HidDeviceManager.ConnectionState.Connecting ->
                        "Connecting" to "Attempting to connect..."
                    else -> {
                        "Disconnected" to "Bluetooth HID connection is inactive."
                    }
                }
                updateNotification(title, text)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_NOW_PLAYING -> {
                val title = intent.getStringExtra("title") ?: nowPlayingTitle
                val artist = intent.getStringExtra("artist") ?: nowPlayingArtist
                val album = intent.getStringExtra("album") ?: nowPlayingAlbum
                val artworkBase64 = intent.getStringExtra("artworkBase64")
                val state = intent.getStringExtra("state") ?: playbackState
                updateNowPlayingState(title, artist, album, artworkBase64, state)
            }
            ACTION_UPDATE_PROXIMITY_SMART_LOCK -> {
                val enabled = intent.getBooleanExtra("enabled", false)
                if (enabled) proximitySmartLockManager.start() else proximitySmartLockManager.stop()
            }
            ACTION_START_WEB_BRIDGE -> {
                if (!RabitNetworkServer.isRunning) {
                    serviceScope.launch(Dispatchers.IO) {
                        RabitNetworkServer.start(this@HidService, encryptionManager)
                    }
                    getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE).edit().putBoolean("web_bridge_enabled", true).apply()
                }
            }
            ACTION_STOP_WEB_BRIDGE -> {
                if (RabitNetworkServer.isRunning) {
                    RabitNetworkServer.stop()
                    getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE).edit().putBoolean("web_bridge_enabled", false).apply()
                }
            }
            "SEND_NOTIFICATION" -> {
                val content = intent.getStringExtra("content") ?: ""
                sendText(content + "\n")
            }
            "PUSH_CLIPBOARD" -> {
                val content = intent.getStringExtra("text") ?: ""
                sendText(content)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.cancel(2)
            }
            "TOGGLE_AUTO_PUSH" -> {
                val prefs = getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
                val current = prefs.getBoolean("auto_push_enabled", false)
                prefs.edit().putBoolean("auto_push_enabled", !current).apply()
                val status = if (!current) "Enabled" else "Disabled"
                Toast.makeText(this, "Auto-Push $status", Toast.LENGTH_SHORT).show()
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.cancel(2)
            }
            "DISMISS_CLIPBOARD" -> {
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.cancel(2)
            }
            "SHOW_CLIPBOARD_NOTIFICATION" -> {
                val text = intent.getStringExtra("text") ?: ""
                showClipboardNotification(text)
            }
            "LOCK_MAC" -> {
                val modifier = (HidKeyCodes.MODIFIER_LEFT_CTRL.toInt() or HidKeyCodes.MODIFIER_LEFT_GUI.toInt()).toByte()
                sendKey(HidKeyCodes.KEY_Q, modifier)
            }
            "SYNC_CLIPBOARD" -> {
                try {
                    val text = intent.getStringExtra("text")
                    if (!text.isNullOrBlank()) {
                        sendText(text)
                        Toast.makeText(this, "Phone clipboard synced to Mac", Toast.LENGTH_SHORT).show()
                    } else {
                        // Fallback pull (might work if service was recently foregrounded)
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val clipText = clip.getItemAt(0).text?.toString()
                            if (!clipText.isNullOrBlank()) {
                                sendText(clipText)
                                Toast.makeText(this, "Syncing current clipboard...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HidService", "Manual sync failed", e)
                }
            }
            "UNLOCK_MAC" -> {
                val macPass = SecureStorage(applicationContext).getMacPassword() ?: ""
                if (macPass.isNotEmpty()) {
                    unlockMac(macPass)
                } else {
                    Toast.makeText(this, "Set Mac Password in Settings first", Toast.LENGTH_SHORT).show()
                }
            }
            "MEDIA_PREV" -> sendConsumerKey(HidKeyCodes.MEDIA_PREVIOUS)
            "MEDIA_PLAY_PAUSE" -> sendConsumerKey(HidKeyCodes.MEDIA_PLAY_PAUSE)
            "MEDIA_NEXT" -> sendConsumerKey(HidKeyCodes.MEDIA_NEXT)
            "MEDIA_VOL_UP" -> sendConsumerKey(HidKeyCodes.MEDIA_VOL_UP)
            "MEDIA_VOL_DOWN" -> sendConsumerKey(HidKeyCodes.MEDIA_VOL_DOWN)
            "STOP_APP" -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun showClipboardNotification(text: String) {
        val prefs = getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
        val isAutoPush = prefs.getBoolean("auto_push_enabled", false)

        val pushIntent = Intent(this, HidService::class.java).apply {
            action = "PUSH_CLIPBOARD"
            putExtra("text", text)
        }
        val pushPendingIntent = PendingIntent.getService(this, 0, pushIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val toggleAutoPushIntent = Intent(this, HidService::class.java).apply {
            action = "TOGGLE_AUTO_PUSH"
        }
        val toggleAutoPushPendingIntent = PendingIntent.getService(this, 2, toggleAutoPushIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val dismissIntent = Intent(this, HidService::class.java).apply {
            action = "DISMISS_CLIPBOARD"
        }
        val dismissPendingIntent = PendingIntent.getService(this, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, clipboardChannelId)
            .setSmallIcon(android.R.drawable.ic_menu_set_as)
            .setContentTitle("Clipboard Detected")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_send, "Push to Mac", pushPendingIntent)
            .addAction(
                if (isAutoPush) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background,
                if (isAutoPush) "Auto-Push: ON" else "Auto-Push: OFF",
                toggleAutoPushPendingIntent
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(2, notification)
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep service running even if app is swiped away
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            
            val serviceChannel = NotificationChannel(
                channelId, "Hackie Background Service", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Bluetooth connection active while app is in use."
            }
            manager.createNotificationChannel(serviceChannel)

            val clipboardChannel = NotificationChannel(
                clipboardChannelId, "Clipboard Sync", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for clipboard sync features."
            }
            manager.createNotificationChannel(clipboardChannel)

            val mediaChannel = NotificationChannel(
                mediaChannelId, "Now Playing Controls", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media controls shown only while a song is playing."
            }
            manager.createNotificationChannel(mediaChannel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        notificationStateTitle = title
        notificationStateText = text

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val unlockIntent = Intent(this, HidService::class.java).apply { action = "UNLOCK_MAC" }
        val unlockPending = PendingIntent.getService(this, 20, unlockIntent, PendingIntent.FLAG_IMMUTABLE)

        val syncIntent = Intent(this, com.example.rabit.ui.components.ClipboardSyncActivity::class.java)
        val syncPending = PendingIntent.getActivity(this, 22, syncIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, HidService::class.java).apply { action = "STOP_APP" }
        val stopPending = PendingIntent.getService(this, 12, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val lockIntent = Intent(this, HidService::class.java).apply { action = "LOCK_MAC" }
        val lockPending = PendingIntent.getService(this, 10, lockIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Hackie Pro Hub: $title")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_lock_lock, "Lock Mac", lockPending)
            .addAction(android.R.drawable.ic_menu_set_as, "Sync", syncPending)
            .addAction(android.R.drawable.ic_lock_idle_lock, "Unlock", unlockPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
            
        return builder.build()
    }

    private fun isSongPlaying(): Boolean {
        val title = nowPlayingTitle.trim()
        val isPaused = playbackState.equals("paused", ignoreCase = true) || playbackState.equals("stopped", ignoreCase = true)
        return title.isNotEmpty() && !title.equals("No track", ignoreCase = true) && !isPaused
    }

    private fun buildNowPlayingNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 30, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, HidService::class.java).apply { action = "MEDIA_PREV" }
        val playPauseIntent = Intent(this, HidService::class.java).apply { action = "MEDIA_PLAY_PAUSE" }
        val nextIntent = Intent(this, HidService::class.java).apply { action = "MEDIA_NEXT" }

        val prevPending = PendingIntent.getService(this, 31, prevIntent, PendingIntent.FLAG_IMMUTABLE)
        val playPausePending = PendingIntent.getService(this, 32, playPauseIntent, PendingIntent.FLAG_IMMUTABLE)
        val nextPending = PendingIntent.getService(this, 33, nextIntent, PendingIntent.FLAG_IMMUTABLE)

        val detailLine = buildString {
            append(nowPlayingArtist.ifBlank { "Unknown artist" })
            if (nowPlayingAlbum.isNotBlank()) {
                append(" • ")
                append(nowPlayingAlbum)
            }
        }

        val isPlaying = !playbackState.equals("paused", ignoreCase = true)
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        return NotificationCompat.Builder(this, mediaChannelId)
            .setContentTitle(nowPlayingTitle.ifBlank { "Now Playing" })
            .setContentText(detailLine.ifBlank { "Unknown artist" })
            .setSmallIcon(if (isPlaying) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause)
            .setLargeIcon(decodeArtwork(nowPlayingArtworkBase64))
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_previous, "Prev", prevPending)
            .addAction(playPauseIcon, if (isPlaying) "Pause" else "Play", playPausePending)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
            .build()
    }

    private fun updateNowPlayingNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (!isSongPlaying()) {
            notificationManager.cancel(mediaNotificationId)
            return
        }
        notificationManager.notify(mediaNotificationId, buildNowPlayingNotification())
    }

    private fun updateNotification(title: String, text: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, buildNotification(title, text))
    }

    private fun updateNowPlayingState(title: String, artist: String, album: String, artworkBase64: String?, state: String) {
        nowPlayingTitle = title.ifBlank { "No track" }
        nowPlayingArtist = artist.ifBlank { "Unknown artist" }
        nowPlayingAlbum = album
        nowPlayingArtworkBase64 = artworkBase64
        playbackState = state.ifBlank { "paused" }

        getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("now_playing_title", nowPlayingTitle)
            .putString("now_playing_artist", nowPlayingArtist)
            .putString("now_playing_album", nowPlayingAlbum)
            .putString("now_playing_artwork_base64", nowPlayingArtworkBase64)
            .putString("playback_state", playbackState)
            .apply()

        updateNowPlayingNotification()
    }

    private fun loadNowPlayingFromPrefs() {
        val prefs = getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
        nowPlayingTitle = prefs.getString("now_playing_title", "No track") ?: "No track"
        nowPlayingArtist = prefs.getString("now_playing_artist", "Connect companion for metadata") ?: "Connect companion for metadata"
        nowPlayingAlbum = prefs.getString("now_playing_album", "") ?: ""
        nowPlayingArtworkBase64 = prefs.getString("now_playing_artwork_base64", null)
        playbackState = prefs.getString("playback_state", "paused") ?: "paused"
        updateNowPlayingNotification()
    }

    private fun decodeArtwork(base64: String?): android.graphics.Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    val connectionState: StateFlow<HidDeviceManager.ConnectionState> get() = hidDeviceManager.connectionState

    fun connect(device: BluetoothDevice) = hidDeviceManager.connect(device)
    fun disconnect() = hidDeviceManager.disconnect()
    fun sendKey(keyCode: Byte, modifier: Byte) = hidDeviceManager.sendKeyPress(keyCode, modifier)
    fun sendConsumerKey(usageId: Short) = hidDeviceManager.sendConsumerKey(usageId)
    fun sendText(text: String) = hidDeviceManager.sendText(text)
    fun unlockMac(password: String) = hidDeviceManager.unlockMac(password)

    override fun onDestroy() {
        proximitySmartLockManager.stop()
        RabitNetworkServer.nowPlayingReceiver = null
        getSystemService(NotificationManager::class.java).cancel(mediaNotificationId)
        RabitNetworkServer.stop()
        serviceScope.cancel()
        hidDeviceManager.unregister()
        super.onDestroy()
    }
}
