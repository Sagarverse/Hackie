package com.example.rabit.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.rabit.data.prefs.UserPreferences

/**
 * Centralized haptic dispatch.
 *
 * Every callsite in the app should go through these helpers, not `Vibrator`
 * directly, so the user's "Vibration enabled" preference in Settings is
 * actually respected. The six previously-direct callsites
 * (`RabitAppScaffold`, `AutoClickerScreen`, `TrackpadSection`, `ChatBubble`,
 * `AssistantNotifier`, plus the original `MainViewModel.performHapticFeedback`)
 * are routed through here now.
 *
 * Three semantic levels cover everything the app needs:
 *  - [tick]     — small tactile click, e.g. a drawer item tap
 *  - [confirm]  — slightly stronger double pulse, e.g. AI response arrived
 *  - [doubleTap]— strongest, e.g. an action completed / pushed to host
 */
object Haptics {

    fun tick(context: Context) = dispatch(context, EFFECT_TICK)
    fun confirm(context: Context) = dispatch(context, EFFECT_CONFIRM)
    fun doubleTap(context: Context) = dispatch(context, EFFECT_DOUBLE_TAP)

    private const val EFFECT_TICK = 1
    private const val EFFECT_CONFIRM = 2
    private const val EFFECT_DOUBLE_TAP = 3

    private fun dispatch(context: Context, effect: Int) {
        // Honor the user's preference. If vibration is off, do nothing.
        if (!UserPreferences.vibrationEnabled(context)) return

        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        val (durationMs, amplitude) = when (effect) {
            EFFECT_TICK -> 12 to 90
            EFFECT_CONFIRM -> 28 to 160
            EFFECT_DOUBLE_TAP -> 18 to 200  // double-tap fires twice
            else -> return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (effect == EFFECT_DOUBLE_TAP) {
                // Two quick pulses, 80ms apart, so the user feels "tap-tap"
                val timings = longArrayOf(0, 18, 80, 18)
                val amplitudes = intArrayOf(0, 200, 0, 200)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), amplitude))
            }
        } else {
            @Suppress("DEPRECATION")
            if (effect == EFFECT_DOUBLE_TAP) {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 18, 80, 18), -1)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs.toLong())
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
