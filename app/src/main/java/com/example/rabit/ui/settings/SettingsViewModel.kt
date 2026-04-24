package com.example.rabit.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.rabit.data.secure.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.pow
import kotlin.math.log10

data class VaultEntry(
    val id: String,
    val appName: String,
    val username: String,
    val password: String,
    val notes: String,
    val updatedAtMs: Long
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
    private val secureStorage = SecureStorage(application)

    private val _unlockPassword = MutableStateFlow(secureStorage.getUnlockPassword() ?: "")
    val unlockPassword = _unlockPassword.asStateFlow()
    private val _hasUnlockPassword = MutableStateFlow(_unlockPassword.value.isNotBlank())
    val hasUnlockPassword = _hasUnlockPassword.asStateFlow()
    private val _macPassword = MutableStateFlow(secureStorage.getMacPassword() ?: "")
    val macPassword = _macPassword.asStateFlow()
    private val _passwordVaultEntries = MutableStateFlow(loadPasswordVaultEntries())
    val passwordVaultEntries = _passwordVaultEntries.asStateFlow()

    private val _autoReconnectEnabled = MutableStateFlow(prefs.getBoolean("auto_reconnect_enabled", false))
    val autoReconnectEnabled = _autoReconnectEnabled.asStateFlow()
    
    private val _typingSpeed = MutableStateFlow(prefs.getString("typing_speed", "Normal") ?: "Normal")
    val typingSpeed = _typingSpeed.asStateFlow()
    
    private val _notificationSyncEnabled = MutableStateFlow(prefs.getBoolean("notification_sync_enabled", false))
    val notificationSyncEnabled = _notificationSyncEnabled.asStateFlow()
    
    private val _autoPushEnabled = MutableStateFlow(prefs.getBoolean("auto_push_enabled", false))
    val autoPushEnabled = _autoPushEnabled.asStateFlow()
    
    private val _vibrationEnabled = MutableStateFlow(prefs.getBoolean("vibration_enabled", true))
    val vibrationEnabled = _vibrationEnabled.asStateFlow()
    
    private val _trackpadSensitivity = MutableStateFlow(prefs.getFloat("trackpad_sensitivity", 1.5f))
    val trackpadSensitivity = _trackpadSensitivity.asStateFlow()

    private val _proximityAutoUnlockEnabled = MutableStateFlow(prefs.getBoolean("proximity_auto_unlock_enabled", false))
    val proximityAutoUnlockEnabled = _proximityAutoUnlockEnabled.asStateFlow()
    
    private val _proximityNearRssi = MutableStateFlow(prefs.getInt("proximity_near_rssi", -62))
    val proximityNearRssi = _proximityNearRssi.asStateFlow()
    
    private val _proximityFarRssi = MutableStateFlow(prefs.getInt("proximity_far_rssi", -80))
    val proximityFarRssi = _proximityFarRssi.asStateFlow()
    
    private val _proximityCooldownSec = MutableStateFlow(prefs.getInt("proximity_cooldown_sec", 12))
    val proximityCooldownSec = _proximityCooldownSec.asStateFlow()
    
    private val _proximityRequirePhoneUnlock = MutableStateFlow(prefs.getBoolean("proximity_require_phone_unlock", true))
    val proximityRequirePhoneUnlock = _proximityRequirePhoneUnlock.asStateFlow()
    
    private val _proximityTargetAddress = MutableStateFlow(prefs.getString("proximity_target_address", "") ?: "")
    val proximityTargetAddress = _proximityTargetAddress.asStateFlow()
    
    private val _proximityLiveRssi = MutableStateFlow(prefs.getInt("proximity_live_rssi", -120))
    val proximityLiveRssi = _proximityLiveRssi.asStateFlow()
    
    private val _proximityLiveDistanceMeters = MutableStateFlow(prefs.getFloat("proximity_live_distance_m", -1f))
    val proximityLiveDistanceMeters = _proximityLiveDistanceMeters.asStateFlow()
    
    private val _proximityUnlockArmed = MutableStateFlow(prefs.getBoolean("proximity_unlock_armed", true))
    val proximityUnlockArmed = _proximityUnlockArmed.asStateFlow()
    
    private val _proximityMacLockStateGuess = MutableStateFlow(prefs.getString("proximity_mac_lock_state_guess", "UNKNOWN") ?: "UNKNOWN")
    val proximityMacLockStateGuess = _proximityMacLockStateGuess.asStateFlow()
    
    private val _isMouseJigglerEnabled = MutableStateFlow(prefs.getBoolean("mouse_jiggler_enabled", false))
    val isMouseJigglerEnabled = _isMouseJigglerEnabled.asStateFlow()

    private val _biometricMacAutofillEnabled = MutableStateFlow(prefs.getBoolean("biometric_mac_autofill_enabled", false))
    val biometricMacAutofillEnabled = _biometricMacAutofillEnabled.asStateFlow()
    
    private val _macAutofillPreEnter = MutableStateFlow(prefs.getBoolean("mac_autofill_pre_enter", false))
    val macAutofillPreEnter = _macAutofillPreEnter.asStateFlow()
    
    private val _macAutofillPostEnter = MutableStateFlow(prefs.getBoolean("mac_autofill_post_enter", false))
    val macAutofillPostEnter = _macAutofillPostEnter.asStateFlow()

    val geminiApiKey: String get() = prefs.getString("gemini_api_key", "") ?: ""

    // Passwords Vault
    private fun loadPasswordVaultEntries(): List<VaultEntry> {
        val jsonString = secureStorage.getPasswordVaultJson()
        val list = mutableListOf<VaultEntry>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(VaultEntry(
                    id = obj.getString("id"),
                    appName = obj.optString("appName", "App"),
                    username = obj.optString("username", ""),
                    password = obj.optString("password", ""),
                    notes = obj.optString("notes", ""),
                    updatedAtMs = obj.optLong("updatedAtMs", System.currentTimeMillis())
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
    
    private fun savePasswordVaultEntries(entries: List<VaultEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("appName", entry.appName)
                put("username", entry.username)
                put("password", entry.password)
                put("notes", entry.notes)
                put("updatedAtMs", entry.updatedAtMs)
            }
            array.put(obj)
        }
        secureStorage.savePasswordVaultJson(array.toString())
        _passwordVaultEntries.value = entries
    }

    fun addVaultEntry(appName: String, username: String, pass: String, notes: String) {
        val newEntry = VaultEntry(UUID.randomUUID().toString(), appName, username, pass, notes, System.currentTimeMillis())
        savePasswordVaultEntries(_passwordVaultEntries.value + newEntry)
    }

    fun removeVaultEntry(id: String) {
        savePasswordVaultEntries(_passwordVaultEntries.value.filter { it.id != id })
    }

    fun clearVault() {
        savePasswordVaultEntries(emptyList())
    }

    fun setUnlockPassword(pass: String) {
        secureStorage.saveUnlockPassword(pass)
        _unlockPassword.value = pass
        _hasUnlockPassword.value = pass.isNotBlank()
    }
    
    fun setMacPassword(pass: String) {
        secureStorage.saveMacPassword(pass)
        _macPassword.value = pass
    }

    // Setters for standard preferences
    fun setAutoReconnectEnabled(enabled: Boolean) {
        _autoReconnectEnabled.value = enabled
        prefs.edit().putBoolean("auto_reconnect_enabled", enabled).apply()
    }

    fun setTypingSpeed(speed: String) {
        _typingSpeed.value = speed
        prefs.edit().putString("typing_speed", speed).apply()
    }

    fun setNotificationSyncEnabled(enabled: Boolean) {
        _notificationSyncEnabled.value = enabled
        prefs.edit().putBoolean("notification_sync_enabled", enabled).apply()
    }

    fun setAutoPushEnabled(enabled: Boolean) {
        _autoPushEnabled.value = enabled
        prefs.edit().putBoolean("auto_push_enabled", enabled).apply()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
    }

    fun setTrackpadSensitivity(sensitivity: Float) {
        _trackpadSensitivity.value = sensitivity
        prefs.edit().putFloat("trackpad_sensitivity", sensitivity).apply()
    }
    
    fun setMouseJigglerEnabled(enabled: Boolean) {
        _isMouseJigglerEnabled.value = enabled
        prefs.edit().putBoolean("mouse_jiggler_enabled", enabled).apply()
    }

    fun setBiometricMacAutofillEnabled(enabled: Boolean) {
        _biometricMacAutofillEnabled.value = enabled
        prefs.edit().putBoolean("biometric_mac_autofill_enabled", enabled).apply()
    }

    fun setMacAutofillPreEnter(enabled: Boolean) {
        _macAutofillPreEnter.value = enabled
        prefs.edit().putBoolean("mac_autofill_pre_enter", enabled).apply()
    }

    fun setMacAutofillPostEnter(enabled: Boolean) {
        _macAutofillPostEnter.value = enabled
        prefs.edit().putBoolean("mac_autofill_post_enter", enabled).apply()
    }

    // Proximity
    fun setProximityAutoUnlockEnabled(enabled: Boolean) {
        _proximityAutoUnlockEnabled.value = enabled
        prefs.edit().putBoolean("proximity_auto_unlock_enabled", enabled).apply()
    }

    fun proximityNearDistanceMeters(): Float {
        val rssi = _proximityNearRssi.value
        return 10.0f.pow((-59 - rssi) / (10 * 3.0f))
    }

    fun setProximityUnlockDistanceMeters(distance: Float) {
        val calculatedRssi = -59 - (10 * 3.0 * log10(distance.toDouble())).toInt()
        val finalRssi = calculatedRssi.coerceIn(-100, -30)
        _proximityNearRssi.value = finalRssi
        prefs.edit().putInt("proximity_near_rssi", finalRssi).apply()
    }

    fun setProximityCooldownSec(cooldown: Int) {
        _proximityCooldownSec.value = cooldown
        prefs.edit().putInt("proximity_cooldown_sec", cooldown).apply()
    }

    fun setProximityRequirePhoneUnlock(enabled: Boolean) {
        _proximityRequirePhoneUnlock.value = enabled
        prefs.edit().putBoolean("proximity_require_phone_unlock", enabled).apply()
    }

    fun setProximityTargetAddress(address: String) {
        _proximityTargetAddress.value = address
        prefs.edit().putString("proximity_target_address", address).apply()
    }
}
