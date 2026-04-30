package com.example.rabit.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.data.voice.VoiceState
import com.example.rabit.domain.model.HidKeyCodes
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.helper.HelperViewModel
import com.example.rabit.ui.webbridge.WebBridgeViewModel
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.PulsingVoiceButton
import kotlinx.coroutines.flow.*
import androidx.compose.runtime.collectAsState

/**
 * KeyboardScreen — Premium remote control interface.
 * Manages input modules: Keyboard, Trackpad, Hub.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardScreen(
    viewModel: MainViewModel,
    helperViewModel: HelperViewModel,
    webBridgeViewModel: WebBridgeViewModel,
    onDisconnect: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToSnippets: () -> Unit = {},
    onNavigateToAutomation: () -> Unit = {},
    onNavigateToWebBridge: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val connectionState by viewModel.connectionState.collectAsState()
    var wasConnected by remember { mutableStateOf(false) }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            is HidDeviceManager.ConnectionState.Connected -> {
                wasConnected = true
            }
            is HidDeviceManager.ConnectionState.Disconnected -> {
                if (wasConnected) {
                    wasConnected = false
                    onDisconnect()
                }
            }
            else -> Unit
        }
    }

    // Disable Air Mouse and Lock Mouse when leaving the PAD tab or screen
    LaunchedEffect(selectedTab) {
        if (selectedTab != 1) {
            viewModel.setAirMouseEnabled(false)
            viewModel.setMouseLocked(true)
        } else {
            viewModel.setMouseLocked(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setAirMouseEnabled(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // ── Compact Header ──
        val deviceName = (connectionState as? HidDeviceManager.ConnectionState.Connected)?.deviceName ?: "OFFLINE"
        val isOnline = deviceName != "OFFLINE"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot + device name
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (isOnline) SuccessGreen else TextTertiary.copy(alpha = 0.5f),
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                deviceName.uppercase(),
                color = if (isOnline) TextSecondary else TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.W500,
                letterSpacing = 1.5.sp,
                modifier = Modifier.weight(1f)
            )

            // Action buttons
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(34.dp)
                    .background(Surface2, CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDisconnect,
                modifier = Modifier
                    .size(34.dp)
                    .background(Surface2, CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = TextSecondary, modifier = Modifier.size(14.dp))
            }
        }

        // ── Module Switcher (clean underline style) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            val tabs = listOf("Input", "Pad", "Hub")

            tabs.forEachIndexed { index, label ->
                val active = selectedTab == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = index }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        label.uppercase(),
                        color = if (active) TextPrimary else TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.W700 else FontWeight.W500,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (active) AccentBlue else Color.Transparent)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Dynamic Module Container ──
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> DualKeyboardTab(viewModel)
                1 -> TrackpadSection(viewModel)
                2 -> FileHubSection(helperViewModel, webBridgeViewModel, onNavigateToSnippets, onNavigateToAutomation, onNavigateToWebBridge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// PremiumHeader kept for backward compatibility but simplified
@Composable
fun PremiumHeader(
    connectionState: HidDeviceManager.ConnectionState,
    onNavigateToSettings: () -> Unit,
    onDisconnect: () -> Unit
) {
    // No-op — header is now inline in KeyboardScreen
}

@Composable
fun DualKeyboardTab(viewModel: MainViewModel) {
    var isSystemMode by remember { mutableStateOf(false) }

    Column {
        // Mode toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(Surface2, RoundedCornerShape(10.dp))
                .padding(3.dp)
        ) {
            listOf("CUSTOM" to false, "SYSTEM" to true).forEach { (label, system) ->
                val active = isSystemMode == system
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Surface3 else Color.Transparent)
                        .clickable { isSystemMode = system },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (active) TextPrimary else TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.W700,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isSystemMode) {
            MinimalSystemInput(viewModel)
        } else {
            val activeModifiers by viewModel.activeModifiers.collectAsState()
            PremiumKeyboardLayout(
                activeModifiers = activeModifiers,
                onModifierClick = { mod -> viewModel.toggleModifier(mod) },
                onKeyPress = { code -> viewModel.sendKey(code) }
            )
        }
    }
}

@Composable
fun MinimalSystemInput(viewModel: MainViewModel) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var batchText by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                val oldText = textFieldValue.text
                val newText = newValue.text

                if (newText.length < oldText.length) {
                    repeat(oldText.length - newText.length) {
                        viewModel.sendKey(HidKeyCodes.KEY_BACKSPACE, useSticky = false)
                    }
                } else if (newText.length > oldText.length) {
                    val diff = newText.substring(oldText.length)
                    viewModel.sendText(diff)
                }

                textFieldValue = newValue
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("Type here…", color = TextTertiary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = TextPrimary,
                cursorColor = AccentBlue
            ),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            trailingIcon = {
                val voiceState by viewModel.voiceState.collectAsState()
                val voiceResult by viewModel.voiceResult.collectAsState()

                LaunchedEffect(voiceResult, voiceState) {
                    if (voiceResult.isNotBlank() && voiceState == VoiceState.SUCCESS) {
                        val updatedText = textFieldValue.text + (if (textFieldValue.text.isNotEmpty()) " " else "") + voiceResult
                        textFieldValue = TextFieldValue(updatedText)
                        viewModel.resetVoiceState()
                    }
                }

                PulsingVoiceButton(
                    state = voiceState,
                    onClick = {
                        if (voiceState == VoiceState.LISTENING) {
                            viewModel.stopVoiceRecognition()
                        } else {
                            viewModel.startVoiceRecognition()
                        }
                    }
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("BATCH", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.W700, letterSpacing = 1.2.sp)
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = batchText,
            onValueChange = { batchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            placeholder = { Text("Paste long text and send", color = TextTertiary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SuccessGreen,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = TextPrimary,
                cursorColor = SuccessGreen
            ),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { viewModel.sendText(batchText); batchText = "" }) {
                Text("SEND", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.W700)
            }
            TextButton(onClick = { textFieldValue = TextFieldValue(""); batchText = "" }) {
                Text("RESET", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.W600)
            }
        }
    }
}

@Composable
fun PremiumKeyboardLayout(
    activeModifiers: Byte,
    onModifierClick: (Byte) -> Unit,
    onKeyPress: (Byte) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val rows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("Z", "X", "C", "V", "B", "N", "M", "Bksp"),
        listOf("Ctrl", "Opt", "Cmd", "Space", "Enter")
    )

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { label ->
                    val code = when(label) {
                        "Bksp" -> HidKeyCodes.KEY_BACKSPACE
                        "Space" -> HidKeyCodes.KEY_SPACE
                        "Enter" -> HidKeyCodes.KEY_ENTER
                        "Ctrl" -> HidKeyCodes.MODIFIER_LEFT_CTRL
                        "Opt" -> HidKeyCodes.MODIFIER_LEFT_ALT
                        "Cmd" -> HidKeyCodes.MODIFIER_LEFT_GUI
                        else -> {
                            val char = label[0].lowercaseChar()
                            HidKeyCodes.getHidCode(char).keyCode
                        }
                    }
                    val isMod = label in listOf("Ctrl", "Opt", "Cmd")
                    val isSelected = isMod && ((activeModifiers.toInt() and code.toInt()) != 0)

                    PremiumKey(
                        label = label,
                        modifier = Modifier.weight(if (label == "Space") 2.5f else if (label.length > 1) 1.3f else 1f),
                        accent = if (isSelected) SuccessGreen else if (isMod) AccentBlue else TextPrimary,
                        onPress = {
                            if (isMod) onModifierClick(code) else onKeyPress(code)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
            }
        }
    }
}
