package com.example.rabit.data.repository

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.example.rabit.data.bluetooth.BluetoothScanner
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.domain.model.HidKeyCodes
import com.example.rabit.domain.repository.KeyboardRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

class KeyboardRepositoryImpl(context: Context) : KeyboardRepository {
    private val hidDeviceManager = HidDeviceManager.getInstance(context)
    private val bluetoothScanner = BluetoothScanner(context)
    private val deviceRepository = com.example.rabit.data.repository.DeviceRepositoryImpl(context)

    override val connectionState: StateFlow<HidDeviceManager.ConnectionState> = hidDeviceManager.connectionState
    override val scannedDevices: StateFlow<Set<BluetoothDevice>> = bluetoothScanner.scannedDevices
    override val isScanning: StateFlow<Boolean> = bluetoothScanner.isScanning
    override val isPushPaused: StateFlow<Boolean> = hidDeviceManager.isPushPaused
    override val isTextPushing: StateFlow<Boolean> = hidDeviceManager.isTextPushing
    override val knownWorkstations: StateFlow<List<com.example.rabit.domain.model.Workstation>> = deviceRepository.knownWorkstations
    override val activeModifiers: StateFlow<Byte> = hidDeviceManager.activeModifiers
    
    override var isPulseModeEnabled: Boolean
        get() = hidDeviceManager.isPulseModeEnabled
        set(value) { hidDeviceManager.isPulseModeEnabled = value }

    override var onShakeDetected: (() -> Unit)? = null

    override fun startScanning() {
        bluetoothScanner.startScanning()
    }

    override fun stopScanning() {
        bluetoothScanner.stopScanning()
    }

    override fun removeWorkstation(address: String) {
        deviceRepository.removeWorkstation(address)
    }

    override fun updateWorkstationNickname(address: String, nickname: String) {
        deviceRepository.updateNickname(address, nickname)
    }

    override fun requestDiscoverable() {
        hidDeviceManager.requestDiscoverable()
    }

    override fun connect(device: BluetoothDevice) {
        hidDeviceManager.connect(device)
    }

    override fun connectWithRetry(device: BluetoothDevice, maxRetries: Int, retryDelayMs: Long) {
        hidDeviceManager.connectWithRetry(device, maxRetries, retryDelayMs)
    }

    override fun disconnect() {
        hidDeviceManager.disconnect()
    }

    override fun sendKey(keyCode: Byte, modifier: Byte, useSticky: Boolean) {
        hidDeviceManager.sendKeyPress(keyCode, modifier, useSticky)
    }

    override fun setModifier(modifier: Byte, active: Boolean) {
        hidDeviceManager.setModifier(modifier, active)
    }

    override fun sendConsumerKey(usageId: Short) {
        hidDeviceManager.sendConsumerKey(usageId)
    }

    override fun sendText(text: String): kotlinx.coroutines.Job? {
        return hidDeviceManager.sendText(text)
    }

    override fun sendMouseMove(dx: Float, dy: Float, buttons: Int, wheel: Int) {
        hidDeviceManager.sendMouseMove(dx, dy, buttons, wheel)
    }

    override fun sendDigitizerInput(x: Int, y: Int, isPressed: Boolean, inRange: Boolean) {
        hidDeviceManager.sendDigitizerInput(x, y, isPressed, inRange)
    }

    override fun resetMouseAccumulator() {
        hidDeviceManager.resetMouseAccumulator()
    }

    override fun stopTextPush() {
        hidDeviceManager.stopTextPush()
    }

    override fun pauseTextPush() {
        hidDeviceManager.pauseTextPush()
    }

    override fun resumeTextPush() {
        hidDeviceManager.resumeTextPush()
    }

    override fun unlockMac(
        password: String,
        pressEnterBefore: Boolean,
        pressEnterAfter: Boolean,
        preTypeDelayMs: Long,
        postTypeDelayMs: Long
    ) {
        hidDeviceManager.unlockMac(password, pressEnterBefore, pressEnterAfter, preTypeDelayMs, postTypeDelayMs)
    }

    override fun executeKeyCombo(combo: String) {
        val parts = combo.split("+").map { it.trim().uppercase() }
        var mod = 0
        var key: Byte = 0
        
        parts.forEach { part ->
            when (part) {
                "CTRL" -> mod = mod or HidKeyCodes.MODIFIER_LEFT_CTRL.toInt()
                "GUI", "CMD", "WIN" -> mod = mod or HidKeyCodes.MODIFIER_LEFT_GUI.toInt()
                "ALT", "OPT" -> mod = mod or HidKeyCodes.MODIFIER_LEFT_ALT.toInt()
                "SHIFT" -> mod = mod or HidKeyCodes.MODIFIER_LEFT_SHIFT.toInt()
                else -> {
                    // Try to get key code from Char if it's single char, or from HidKeyCodes
                    if (part.length == 1) {
                        key = HidKeyCodes.getHidCode(part[0]).keyCode
                    } else {
                        // Handle special keys like ENTER, SPACE in combos
                        when (part) {
                            "ENTER" -> key = HidKeyCodes.KEY_ENTER
                            "SPACE" -> key = HidKeyCodes.KEY_SPACE
                            "TAB" -> key = HidKeyCodes.KEY_TAB
                            "ESC" -> key = HidKeyCodes.KEY_ESC
                            "Q" -> key = HidKeyCodes.KEY_Q // Common for lock combo
                        }
                    }
                }
            }
        }
        if (key != 0.toByte() || mod != 0) {
            hidDeviceManager.sendKeyPress(key, mod.toByte(), useSticky = false)
        }
    }

    override fun executeSpecialKey(key: String) {
        when (key.uppercase()) {
            "VOL_UP" -> hidDeviceManager.sendConsumerKey(HidKeyCodes.MEDIA_VOL_UP)
            "VOL_DOWN" -> hidDeviceManager.sendConsumerKey(HidKeyCodes.MEDIA_VOL_DOWN)
            "MUTE" -> hidDeviceManager.sendConsumerKey(HidKeyCodes.MEDIA_MUTE)
            "PLAY", "PLAY_PAUSE" -> hidDeviceManager.sendConsumerKey(HidKeyCodes.MEDIA_PLAY_PAUSE)
            "NEXT" -> hidDeviceManager.sendConsumerKey(HidKeyCodes.MEDIA_NEXT)
            "PREV" -> hidDeviceManager.sendConsumerKey(HidKeyCodes.MEDIA_PREVIOUS)
            "BRIGHT_UP" -> hidDeviceManager.sendConsumerKey(HidKeyCodes.MEDIA_BRIGHT_UP)
            "BRIGHT_DOWN" -> hidDeviceManager.sendConsumerKey(HidKeyCodes.MEDIA_BRIGHT_DOWN)
        }
    }

    override fun updateIdentity(name: String, provider: String, description: String) {
        hidDeviceManager.updateIdentity(name, provider, description)
    }

    override fun setMouseLocked(locked: Boolean) {
        hidDeviceManager.isMouseLocked = locked
    }
}
