package com.example.rabit.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardHelper {
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Hackie Remote Clipboard", text)
        clipboard?.setPrimaryClip(clip)
    }

    fun getFromClipboard(context: Context): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard?.hasPrimaryClip() == true && clipboard.primaryClip?.itemCount!! > 0) {
            return clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        }
        return ""
    }
}
