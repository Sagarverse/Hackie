package com.example.rabit.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for user-level preferences (theme, haptics,
 * tracking sensitivity, etc). Backed by a private SharedPreferences file.
 *
 * The values are read with simple static accessors so any callsite —
 * Composables, ViewModels, services — can read the current value without
 * having to thread a ViewModel through. Writes are also static; the
 * underlying SharedPreferences is process-safe.
 */
object UserPreferences {

    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    private const val PREFS_NAME = "hackie_user_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    private const val KEY_TRACKPAD_SENSITIVITY = "trackpad_sensitivity"
    private const val KEY_AIR_MOUSE_SENSITIVITY = "air_mouse_sensitivity"

    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences {
        cachedPrefs?.let { return it }
        return synchronized(this) {
            cachedPrefs ?: context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .also { cachedPrefs = it }
        }
    }

    // ── Theme mode ─────────────────────────────────────────────────
    //
    // The mode is exposed as a StateFlow so the activity can observe it
    // and re-apply HackieTheme the instant the user picks a different
    // value. Writes go through [setThemeMode] which both updates the
    // flow and persists to SharedPreferences, so any new Composable
    // observing the flow will see the new value immediately.
    private val _themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow.asStateFlow()

    fun themeMode(context: Context): ThemeMode {
        val raw = prefs(context).getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        val parsed = runCatching { ThemeMode.valueOf(raw ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
        // Keep the flow in sync with the persisted value the first time
        // we read it. After init, writes are the only source of truth.
        if (_themeModeFlow.value == ThemeMode.SYSTEM && parsed != ThemeMode.SYSTEM) {
            _themeModeFlow.value = parsed
        }
        return parsed
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeModeFlow.value = mode
    }

    // ── Vibration (haptics) ────────────────────────────────────────
    fun vibrationEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_VIBRATION_ENABLED, true)

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }

    // ── Trackpad sensitivity ───────────────────────────────────────
    fun trackpadSensitivity(context: Context): Float =
        prefs(context).getFloat(KEY_TRACKPAD_SENSITIVITY, 1.0f)

    fun setTrackpadSensitivity(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_TRACKPAD_SENSITIVITY, value).apply()
    }

    // ── Air mouse sensitivity ──────────────────────────────────────
    fun airMouseSensitivity(context: Context): Float =
        prefs(context).getFloat(KEY_AIR_MOUSE_SENSITIVITY, 1.0f)

    fun setAirMouseSensitivity(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_AIR_MOUSE_SENSITIVITY, value).apply()
    }
}

/**
 * Composition local that exposes the current [ThemeMode] to composables.
 * Provided at the top of the activity and updated when the user changes
 * the theme in Settings.
 */
val LocalThemeMode = compositionLocalOf { UserPreferences.ThemeMode.SYSTEM }
