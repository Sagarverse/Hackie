package com.example.rabit.ui.components

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.example.rabit.data.bluetooth.HidService

/**
 * A transparent activity used to gain focus and bypass Android 10+ background 
 * clipboard restrictions. When tapped from a notification, it pulls the current 
 * clipboard and sends it to HidService for syncing.
 */
class ClipboardSyncActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set transparent layout if not already handled by theme
    }

    override fun onResume() {
        super.onResume()
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank()) {
                    Log.d("ClipboardSyncActivity", "Clipboard pulled successfully")
                    val intent = Intent(this, HidService::class.java).apply {
                        action = "SYNC_CLIPBOARD"
                        putExtra("text", text)
                    }
                    startService(intent)
                }
            }
        } catch (e: Exception) {
            Log.e("ClipboardSyncActivity", "Failed to pull clipboard", e)
        }
        finish()
    }
}
