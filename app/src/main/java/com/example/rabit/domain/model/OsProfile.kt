package com.example.rabit.domain.model

import androidx.annotation.DrawableRes
import com.sagar.rabit.R

/**
 * OsProfile — Target operating system profiles with hardcoded key mappings.
 *
 * Each OS maps voice/system commands to the correct HID keyboard shortcuts
 * so that Hackie works seamlessly across macOS, Windows, and Linux.
 */
enum class TargetOs(val displayName: String, @DrawableRes val iconRes: Int) {
    MAC_OS("macOS", R.drawable.ic_os_apple),
    WINDOWS("Windows", R.drawable.ic_os_windows),
    LINUX("Linux", R.drawable.ic_os_linux);

    companion object {
        fun fromString(value: String): TargetOs = when (value.uppercase()) {
            "WINDOWS" -> WINDOWS
            "LINUX" -> LINUX
            else -> MAC_OS
        }
    }
}

/**
 * Complete key mapping for a target OS.
 * Key combo strings use the format recognized by KeyboardRepository.executeKeyCombo(),
 * e.g. "CTRL+GUI+Q", "ALT+TAB", "GUI+L".
 */
data class OsKeyMapping(
    val lockScreen: String,
    val unlockPreamble: String,   // Key to press before typing password (wake screen)
    val openAppLauncher: String,  // Spotlight / Start / Activities
    val screenshot: String,
    val screenshotArea: String,
    val copy: String,
    val paste: String,
    val cut: String,
    val undo: String,
    val redo: String,
    val closeWindow: String,
    val switchApp: String,
    val selectAll: String,
    val find: String,
    val newTab: String,
    val closeTab: String,
    val save: String,
    val sleep: String,
    val forceQuit: String,
    val missionControl: String,
    val showDesktop: String,
)

object OsKeyMappings {

    private val macOs = OsKeyMapping(
        lockScreen = "CTRL+GUI+Q",
        unlockPreamble = "SPACE",          // Wake the lock screen
        openAppLauncher = "GUI+SPACE",     // Spotlight
        screenshot = "GUI+SHIFT+3",
        screenshotArea = "GUI+SHIFT+4",
        copy = "GUI+C",
        paste = "GUI+V",
        cut = "GUI+X",
        undo = "GUI+Z",
        redo = "GUI+SHIFT+Z",
        closeWindow = "GUI+W",
        switchApp = "GUI+TAB",
        selectAll = "GUI+A",
        find = "GUI+F",
        newTab = "GUI+T",
        closeTab = "GUI+W",
        save = "GUI+S",
        sleep = "GUI+ALT+EJECT",
        forceQuit = "GUI+ALT+ESC",
        missionControl = "CTRL+UP",
        showDesktop = "F11",
    )

    private val windows = OsKeyMapping(
        lockScreen = "GUI+L",
        unlockPreamble = "SPACE",
        openAppLauncher = "GUI",            // Start menu
        screenshot = "GUI+SHIFT+S",
        screenshotArea = "GUI+SHIFT+S",
        copy = "CTRL+C",
        paste = "CTRL+V",
        cut = "CTRL+X",
        undo = "CTRL+Z",
        redo = "CTRL+Y",
        closeWindow = "ALT+F4",
        switchApp = "ALT+TAB",
        selectAll = "CTRL+A",
        find = "CTRL+F",
        newTab = "CTRL+T",
        closeTab = "CTRL+W",
        save = "CTRL+S",
        sleep = "GUI+X",                   // Opens power menu
        forceQuit = "CTRL+SHIFT+ESC",      // Task Manager
        missionControl = "GUI+TAB",        // Task View
        showDesktop = "GUI+D",
    )

    private val linux = OsKeyMapping(
        lockScreen = "SUPER+L",
        unlockPreamble = "SPACE",
        openAppLauncher = "SUPER",          // GNOME Activities
        screenshot = "PRINT",
        screenshotArea = "SHIFT+PRINT",
        copy = "CTRL+C",
        paste = "CTRL+V",
        cut = "CTRL+X",
        undo = "CTRL+Z",
        redo = "CTRL+SHIFT+Z",
        closeWindow = "ALT+F4",
        switchApp = "ALT+TAB",
        selectAll = "CTRL+A",
        find = "CTRL+F",
        newTab = "CTRL+T",
        closeTab = "CTRL+W",
        save = "CTRL+S",
        sleep = "SUPER+L",                 // Most Linux DEs just lock
        forceQuit = "CTRL+ALT+DELETE",
        missionControl = "SUPER",
        showDesktop = "SUPER+D",
    )

    fun forOs(os: TargetOs): OsKeyMapping = when (os) {
        TargetOs.MAC_OS -> macOs
        TargetOs.WINDOWS -> windows
        TargetOs.LINUX -> linux
    }
}
