package com.example.rabit.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.voice.JarvisCommandEngine
import com.example.rabit.ui.theme.*

/**
 * JarvisOverlay — Floating Jarvis-style overlay that sits on top of all screens.
 *
 * States:
 * - LISTENING_WAKE: Subtle pulsing dot at bottom-center
 * - WAKE_DETECTED: Expanding arc animation with "Listening..."
 * - LISTENING_COMMAND: Waveform animation with real-time transcription
 * - CONFIRMING: Command card with Confirm/Cancel buttons
 * - EXECUTING: Brief flash with command name
 */
@Composable
fun JarvisOverlay(
    state: JarvisCommandEngine.JarvisState,
    lastRecognizedText: String,
    pendingCommand: JarvisCommandEngine.VoiceCommand?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    // Don't render anything when idle
    if (state == JarvisCommandEngine.JarvisState.IDLE) return

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        when (state) {
            JarvisCommandEngine.JarvisState.LISTENING_WAKE -> {
                WakeWordIndicator()
            }
            JarvisCommandEngine.JarvisState.WAKE_DETECTED -> {
                WakeDetectedAnimation()
            }
            JarvisCommandEngine.JarvisState.LISTENING_COMMAND -> {
                CommandListeningCard(recognizedText = lastRecognizedText)
            }
            JarvisCommandEngine.JarvisState.THINKING -> {
                ThinkingAnimation(recognizedText = lastRecognizedText)
            }
            JarvisCommandEngine.JarvisState.CONFIRMING -> {
                ConfirmationCard(
                    command = pendingCommand,
                    onConfirm = onConfirm,
                    onCancel = onCancel
                )
            }
            JarvisCommandEngine.JarvisState.EXECUTING -> {
                ExecutingFlash(commandText = lastRecognizedText)
            }
            JarvisCommandEngine.JarvisState.ERROR -> {
                ErrorIndicator()
            }
            else -> { /* IDLE handled above */ }
        }
    }
}

/**
 * Subtle pulsing dot — shown while waiting for "Hey Sagar" wake word.
 */
@Composable
private fun WakeWordIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "wake_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wake_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wake_alpha"
    )

    Box(
        modifier = Modifier
            .padding(bottom = 24.dp)
            .size(12.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(AccentTeal.copy(alpha = alpha))
            .border(0.5.dp, AccentTeal.copy(alpha = alpha * 0.5f), CircleShape)
    )
}

/**
 * Expanding arc when wake word is detected.
 */
@Composable
private fun WakeDetectedAnimation() {
    val transition = rememberInfiniteTransition(label = "wake_detected")
    val ringScale by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale"
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha"
    )

    Column(
        modifier = Modifier.padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Expanding ring
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .border(2.dp, AccentTeal.copy(alpha = ringAlpha), CircleShape)
            )
            // Center dot
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(AccentTeal)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Listening...",
            color = AccentTeal,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Command listening card with live transcription and waveform.
 */
@Composable
private fun CommandListeningCard(recognizedText: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Obsidian.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(AccentTeal.copy(alpha = 0.5f), AccentBlue.copy(alpha = 0.5f)))
        ),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Waveform bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(32.dp)
            ) {
                for (i in 0..6) {
                    val barHeight by infiniteTransition.animateFloat(
                        initialValue = 8f,
                        targetValue = 28f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 300 + (i * 80),
                                easing = FastOutSlowInEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bar_$i"
                    )
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(barHeight.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(AccentTeal, AccentBlue)
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "🎤 Speak your command",
                color = Platinum,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            if (recognizedText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "\"$recognizedText\"",
                    color = AccentTeal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Animated Gemini sparkle effect shown while the LLM processes a command.
 */
@Composable
private fun ThinkingAnimation(recognizedText: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Obsidian.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(Color(0xFFBC13FE).copy(alpha = 0.5f), AccentBlue.copy(alpha = 0.5f)))
        ),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "Gemini Sparkle" placeholder - a pulsing gradient orb
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFBC13FE).copy(alpha = alpha),
                                AccentBlue.copy(alpha = alpha * 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Thinking...",
                color = Color(0xFFBC13FE),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            if (recognizedText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "\"$recognizedText\"",
                    color = Platinum.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Confirmation card with the detected command and Confirm/Cancel buttons.
 */
@Composable
private fun ConfirmationCard(
    command: JarvisCommandEngine.VoiceCommand?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    if (command == null) return

    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Obsidian.copy(alpha = 0.97f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(AccentGold.copy(alpha = 0.6f), AccentOrange.copy(alpha = 0.6f)))
        ),
        shadowElevation = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Command icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = AccentGold.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f))
            ) {
                Icon(
                    imageVector = commandIcon(command.commandType),
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Execute Command?",
                color = Platinum,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "\"${command.rawText}\"",
                color = AccentGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Text(
                command.commandType.name.replace("_", " "),
                color = Silver,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            if (command.argument.isNotBlank()) {
                Text(
                    "→ ${command.argument}",
                    color = AccentTeal,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Silver.copy(alpha = 0.3f))
                ) {
                    Text("Cancel", color = Silver)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirm", color = Obsidian, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Brief flash overlay when a command is being executed.
 */
@Composable
private fun ExecutingFlash(commandText: String) {
    val alpha by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(1200),
        label = "executing_fade"
    )

    Surface(
        modifier = Modifier
            .padding(horizontal = 48.dp, vertical = 48.dp),
        shape = RoundedCornerShape(16.dp),
        color = SuccessGreen.copy(alpha = 0.15f + alpha * 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f + alpha * 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Executing: $commandText",
                color = SuccessGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Error indicator.
 */
@Composable
private fun ErrorIndicator() {
    Surface(
        modifier = Modifier
            .padding(horizontal = 48.dp, vertical = 48.dp),
        shape = RoundedCornerShape(16.dp),
        color = ErrorRed.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Voice recognition error",
                color = ErrorRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun commandIcon(type: JarvisCommandEngine.CommandType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        JarvisCommandEngine.CommandType.LOCK -> Icons.Default.Lock
        JarvisCommandEngine.CommandType.UNLOCK -> Icons.Default.LockOpen
        JarvisCommandEngine.CommandType.SCREENSHOT,
        JarvisCommandEngine.CommandType.SCREENSHOT_AREA -> Icons.Default.Screenshot
        JarvisCommandEngine.CommandType.COPY -> Icons.Default.ContentCopy
        JarvisCommandEngine.CommandType.PASTE -> Icons.Default.ContentPaste
        JarvisCommandEngine.CommandType.CUT -> Icons.Default.ContentCut
        JarvisCommandEngine.CommandType.UNDO -> Icons.Default.Undo
        JarvisCommandEngine.CommandType.REDO -> Icons.Default.Redo
        JarvisCommandEngine.CommandType.CLOSE_WINDOW -> Icons.Default.Close
        JarvisCommandEngine.CommandType.SWITCH_APP -> Icons.Default.SwapHoriz
        JarvisCommandEngine.CommandType.MUTE -> Icons.Default.VolumeOff
        JarvisCommandEngine.CommandType.VOLUME_UP -> Icons.Default.VolumeUp
        JarvisCommandEngine.CommandType.VOLUME_DOWN -> Icons.Default.VolumeDown
        JarvisCommandEngine.CommandType.PLAY_PAUSE -> Icons.Default.PlayArrow
        JarvisCommandEngine.CommandType.OPEN_APP -> Icons.Default.OpenInNew
        JarvisCommandEngine.CommandType.TYPE_TEXT -> Icons.Default.Keyboard
        JarvisCommandEngine.CommandType.SLEEP -> Icons.Default.Bedtime
        JarvisCommandEngine.CommandType.FORCE_QUIT -> Icons.Default.PowerSettingsNew
        JarvisCommandEngine.CommandType.SAVE -> Icons.Default.Save
        JarvisCommandEngine.CommandType.FIND -> Icons.Default.Search
        JarvisCommandEngine.CommandType.SELECT_ALL -> Icons.Default.SelectAll
        JarvisCommandEngine.CommandType.NEW_TAB,
        JarvisCommandEngine.CommandType.CLOSE_TAB -> Icons.Default.Tab
        JarvisCommandEngine.CommandType.SHOW_DESKTOP -> Icons.Default.DesktopWindows
        JarvisCommandEngine.CommandType.MISSION_CONTROL -> Icons.Default.GridView
        JarvisCommandEngine.CommandType.BRIGHTNESS_UP,
        JarvisCommandEngine.CommandType.BRIGHTNESS_DOWN -> Icons.Default.BrightnessHigh
        JarvisCommandEngine.CommandType.UNKNOWN -> Icons.Default.HelpOutline
    }
}
