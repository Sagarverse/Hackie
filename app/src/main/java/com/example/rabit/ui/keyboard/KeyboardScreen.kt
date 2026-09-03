package com.example.rabit.ui.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.data.voice.VoiceState
import com.example.rabit.domain.model.HidKeyCodes
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.components.AppCard
import com.example.rabit.ui.components.IconTile
import com.example.rabit.ui.components.PulsingVoiceButton
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.components.SettingsToggleRow
import com.example.rabit.ui.helper.HelperViewModel
import com.example.rabit.ui.theme.HackieSpacing
import com.example.rabit.ui.webbridge.WebBridgeViewModel
import kotlinx.coroutines.flow.*

/**
 * KeyboardScreen — remote control surface for the paired device.
 * Hosts the Input, Trackpad, and File Hub modules.
 */
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
    var selectedTab by remember { mutableIntStateOf(0) }
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

    val isTextPushing by viewModel.isTextPushing.collectAsState()
    val isPushPaused by viewModel.isPushPaused.collectAsState()
    val deviceName = (connectionState as? HidDeviceManager.ConnectionState.Connected)?.deviceName
    val isOnline = deviceName != null

    ScreenScaffold(
        title = deviceName ?: "Not connected",
        subtitle = when {
            isTextPushing && isPushPaused -> "Typing paused"
            isTextPushing -> "Injecting text…"
            isOnline -> "Connected"
            else -> "Connect a device to start"
        },
        onBack = onDisconnect,
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HackieSpacing.md),
        ) {
            // ── Tab strip ──────────────────────────────────────
            ModuleTabStrip(
                selected = selectedTab,
                onSelect = { selectedTab = it },
            )

            Spacer(Modifier.height(HackieSpacing.sm))

            // ── Dynamic module container ───────────────────────
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> DualKeyboardTab(viewModel)
                    1 -> TrackpadSection(viewModel)
                    2 -> FileHubSection(
                        viewModel = helperViewModel,
                        webBridgeViewModel = webBridgeViewModel,
                        onNavigateToSnippets = onNavigateToSnippets,
                        onNavigateToAutomation = onNavigateToAutomation,
                        onNavigateToWebBridge = onNavigateToWebBridge,
                    )
                }
            }

            Spacer(Modifier.height(HackieSpacing.md))
        }
    }
}

@Composable
private fun ModuleTabStrip(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("Input", "Pad", "Hub")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val active = selected == index
            val container = if (active) MaterialTheme.colorScheme.primary
            else Color.Transparent
            val content = if (active) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(container)
                    .clickable { onSelect(index) }
                    .padding(vertical = HackieSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                )
            }
        }
    }
}

@Composable
fun DualKeyboardTab(viewModel: MainViewModel) {
    var isSystemMode by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(HackieSpacing.md)) {
        // Mode toggle
        val modeLabels = listOf("Custom" to false, "System" to true)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(4.dp),
        ) {
            modeLabels.forEach { (label, system) ->
                val active = isSystemMode == system
                val container = if (active) MaterialTheme.colorScheme.surfaceContainerHigh
                else Color.Transparent
                val content = if (active) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(container)
                        .clickable { isSystemMode = system }
                        .padding(vertical = HackieSpacing.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = label, style = MaterialTheme.typography.labelLarge, color = content)
                }
            }
        }

        AppCard {
            val isHumanTyping by viewModel.isHumanTypingEnabled.collectAsState()
            SettingsToggleRow(
                title = "Human mode",
                subtitle = "Inject typos and natural backspacing",
                leading = {
                    IconTile(
                        icon = Icons.Default.Psychology,
                        color = if (isHumanTyping) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                checked = isHumanTyping,
                onCheckedChange = { viewModel.setHumanTypingEnabled(it) },
            )
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
                ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Injection speed",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                val typingSpeed by viewModel.typingSpeed.collectAsState()
                SpeedSegmented(
                    current = typingSpeed,
                    onPick = { viewModel.setTypingSpeed(it) },
                )
            }
        }

        if (isSystemMode) {
            MinimalSystemInput(viewModel)
        } else {
            val activeModifiers by viewModel.activeModifiers.collectAsState()
            PremiumKeyboardLayout(
                activeModifiers = activeModifiers,
                onModifierClick = { mod -> viewModel.toggleModifier(mod) },
                onKeyPress = { code -> viewModel.sendKey(code) },
            )
        }
    }
}

@Composable
private fun SpeedSegmented(current: String, onPick: (String) -> Unit) {
    val speeds = listOf("Too Slow", "Slow", "Normal", "Fast", "Super Fast")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp),
    ) {
        speeds.forEach { speed ->
            val active = speed == current
            val shortName = when (speed) {
                "Too Slow" -> "Min"
                "Super Fast" -> "Max"
                else -> speed
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onPick(speed) }
                    .padding(vertical = HackieSpacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = shortName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun MinimalSystemInput(viewModel: MainViewModel) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var batchText by remember { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
            placeholder = { Text("Type here…") },
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            trailingIcon = {
                val voiceState by viewModel.voiceState.collectAsState()
                val voiceResult by viewModel.voiceResult.collectAsState()

                LaunchedEffect(voiceResult, voiceState) {
                    if (voiceResult.isNotBlank() && voiceState == VoiceState.SUCCESS) {
                        val updatedText = textFieldValue.text +
                            (if (textFieldValue.text.isNotEmpty()) " " else "") + voiceResult
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
                    },
                )
            },
        )

        Text(
            text = "Batch",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = HackieSpacing.xs),
        )

        OutlinedTextField(
            value = batchText,
            onValueChange = { batchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            placeholder = { Text("Paste long text and send") },
            shape = MaterialTheme.shapes.large,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { textFieldValue = TextFieldValue(""); batchText = "" }) {
                Text("Clear", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { viewModel.sendText(batchText); batchText = "" }) {
                Text("Send", color = MaterialTheme.colorScheme.primary)
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

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { label ->
                    val code = when (label) {
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
                        modifier = Modifier.weight(
                            when (label) {
                                "Space" -> 2.5f
                                "Bksp", "Enter" -> 1.4f
                                "Ctrl", "Opt", "Cmd" -> 1.2f
                                else -> 1f
                            }
                        ),
                        accent = if (isSelected) MaterialTheme.colorScheme.primary
                        else if (isMod) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurface,
                        onPress = {
                            if (isMod) onModifierClick(code) else onKeyPress(code)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                    )
                }
            }
        }
    }
}
