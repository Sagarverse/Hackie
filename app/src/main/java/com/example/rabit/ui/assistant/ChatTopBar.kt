package com.example.rabit.ui.assistant

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rabit.ui.theme.HackieSpacing
import com.example.rabit.ui.theme.Success

/**
 * Clean, Apple-style chat top bar.
 * Shows the active model, connection status, and a row of action icons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumChatTopBar(
    modelName: String,
    isThinking: Boolean,
    connectionState: com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState,
    onLeftPanelClick: () -> Unit,
    onRightPanelClick: () -> Unit,
    onClearChat: () -> Unit,
    onNewChat: () -> Unit,
    onExportChat: () -> Unit,
    onSettingsClick: () -> Unit,
    chatSessions: List<com.example.rabit.data.repository.ChatSession> = emptyList(),
    onSessionClick: (String) -> Unit = {},
    onDeleteSession: (String) -> Unit = {}
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
    val orbAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isThinking) AssistantMotion.PULSE_FAST else AssistantMotion.PULSE_IDLE,
                easing = EaseInOutSine,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbAlpha",
    )
    val accent = MaterialTheme.colorScheme.primary
    val orbColor by animateColorAsState(
        targetValue = accent,
        label = "orbColor",
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HackieSpacing.md, vertical = HackieSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
        ) {
            // Model avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accent.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, accent.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (modelName.contains("Gemini")) "G" else "M",
                    color = orbColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.alpha(if (isThinking) orbAlpha else 1f),
                )
            }

            // Title + status
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = when {
                                    isThinking -> accent
                                    connectionState is com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connected ->
                                        Success
                                    else -> MaterialTheme.colorScheme.outline
                                },
                                shape = CircleShape,
                            ),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = when {
                            isThinking -> "Thinking…"
                            connectionState is com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connected ->
                                "Connected"
                            else -> "Disconnected"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // New chat
            FilledTonalIconButton(
                onClick = onNewChat,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New chat",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Settings menu
            Box {
                FilledTonalIconButton(
                    onClick = { showSettingsMenu = true },
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                DropdownMenu(
                    expanded = showSettingsMenu,
                    onDismissRequest = { showSettingsMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Hardware monitor") },
                        leadingIcon = { Icon(Icons.Default.SettingsInputComponent, null) },
                        onClick = {
                            showSettingsMenu = false
                            onRightPanelClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Chat history") },
                        leadingIcon = { Icon(Icons.Default.History, null) },
                        onClick = {
                            showSettingsMenu = false
                            showHistoryDialog = true
                        },
                    )
                }
            }

            // More menu
            Box {
                FilledTonalIconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Export chat") },
                        leadingIcon = { Icon(Icons.Default.IosShare, null) },
                        onClick = {
                            showMoreMenu = false
                            onExportChat()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Clear chat") },
                        leadingIcon = { Icon(Icons.Default.DeleteSweep, null) },
                        onClick = {
                            showMoreMenu = false
                            onClearChat()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        leadingIcon = { Icon(Icons.Default.Settings, null) },
                        onClick = {
                            showMoreMenu = false
                            onSettingsClick()
                        },
                    )
                }
            }
        }
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = accent)
                    Spacer(Modifier.size(HackieSpacing.xs))
                    Text("Chat history")
                }
            },
            text = {
                if (chatSessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(HackieSpacing.lg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No saved sessions",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
                    ) {
                        items(chatSessions.size) { index ->
                            val session = chatSessions[index]
                            Surface(
                                onClick = {
                                    onSessionClick(session.id)
                                    showHistoryDialog = false
                                },
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(HackieSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${session.messageCount} messages · " +
                                                java.text.SimpleDateFormat(
                                                    "MMM dd, HH:mm",
                                                    java.util.Locale.getDefault(),
                                                ).format(session.lastModified),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = { onDeleteSession(session.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close")
                }
            },
        )
    }
}
