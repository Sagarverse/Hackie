package com.example.rabit.ui.automation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.components.AppCard
import com.example.rabit.ui.components.IconTile
import com.example.rabit.ui.components.LabelPill
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.components.SectionHeader
import com.example.rabit.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroOrchestratorScreen(
    viewModel: AutomationViewModel,
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val connectionState by mainViewModel.connectionState.collectAsState()
    val isConnected = connectionState is HidDeviceManager.ConnectionState.Connected

    val accent = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val successColor = Success

    val tacticalMacros = remember {
        listOf(
            MacroDefinition(
                "System recon",
                Icons.Default.Radar,
                accent,
                "KEY(GUI+SPACE) && WAIT(400) && TEXT(Terminal) && KEY(ENTER) && WAIT(1000) && TEXT(whoami; hostname; uname -a; uptime) && KEY(ENTER)"
            ),
            MacroDefinition(
                "Exfiltrate clipboard",
                Icons.Default.ContentPaste,
                accent,
                "KEY(GUI+C) && WAIT(200) && TEXT(Clipboard Exfiltrated) && KEY(ENTER)"
            ),
            MacroDefinition(
                "Network sweep",
                Icons.Default.Language,
                successColor,
                "KEY(GUI+SPACE) && WAIT(400) && TEXT(Terminal) && KEY(ENTER) && WAIT(1000) && TEXT(ifconfig | grep inet) && KEY(ENTER)"
            ),
            MacroDefinition(
                "Clear history",
                Icons.Default.DeleteSweep,
                errorColor,
                "TEXT(history -c; clear) && KEY(ENTER)"
            ),
        )
    }

    val accessMacros = remember {
        listOf(
            MacroDefinition(
                "Lock bypass (space)",
                Icons.Default.LockOpen,
                accent,
                "KEY(SPACE) && WAIT(200) && KEY(SPACE)"
            ),
            MacroDefinition(
                "Force sleep",
                Icons.Default.PowerSettingsNew,
                mutedColor,
                "KEY(CTRL+SHIFT+POWER)"
            ),
            MacroDefinition(
                "Lock Mac",
                Icons.Default.Lock,
                errorColor,
                "KEY(GUI+CTRL+Q)"
            ),
        )
    }

    val devMacros = remember {
        listOf(
            MacroDefinition(
                "New VS Code window",
                Icons.Default.Code,
                accent,
                "KEY(GUI+SPACE) && WAIT(400) && TEXT(Visual Studio Code) && KEY(ENTER) && WAIT(1500) && KEY(GUI+N)"
            ),
            MacroDefinition(
                "Git status check",
                Icons.Default.Commit,
                accent,
                "TEXT(git status) && KEY(ENTER)"
            ),
            MacroDefinition(
                "Activity Monitor",
                Icons.Default.Monitor,
                successColor,
                "KEY(GUI+SPACE) && WAIT(400) && TEXT(Activity Monitor) && KEY(ENTER)"
            ),
        )
    }

    ScreenScaffold(
        title = "Macros",
        subtitle = if (isConnected) "HID link active" else "Not connected",
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                ConnectionPill(isConnected)
            }
            item {
                DiagnosticCard(
                    title = "Test notification",
                    subtitle = "Verify the clipboard sync pop-up",
                    icon = Icons.Default.BugReport,
                    enabled = isConnected,
                    onClick = {
                        val context = mainViewModel.getApplication<android.app.Application>()
                        val intent = android.content.Intent(context, com.example.rabit.data.bluetooth.HidService::class.java).apply {
                            action = "SHOW_CLIPBOARD_NOTIFICATION"
                            putExtra("text", "TEST PAYLOAD: system notification diagnostic")
                        }
                        // Foreground service on Android 8+ — bare startService() throws.
                        runCatching {
                            ContextCompat.startForegroundService(context, intent)
                        }
                    },
                )
            }
            item {
                DiagnosticCard(
                    title = "Push phone clipboard to Mac",
                    subtitle = "Sync the current phone text to the host",
                    icon = Icons.Default.Upload,
                    enabled = isConnected,
                    onClick = {
                        val context = mainViewModel.getApplication<android.app.Application>()
                        val intent = android.content.Intent(context, com.example.rabit.data.bluetooth.HidService::class.java).apply {
                            action = "SYNC_CLIPBOARD"
                        }
                        runCatching {
                            ContextCompat.startForegroundService(context, intent)
                        }
                    },
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(title = "Tactical")
                    tacticalMacros.forEach { macro ->
                        MacroRow(
                            macro = macro,
                            enabled = isConnected,
                            onClick = { viewModel.executeMacro2Script(macro.command) },
                        )
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(title = "Access & control")
                    accessMacros.forEach { macro ->
                        MacroRow(
                            macro = macro,
                            enabled = isConnected,
                            onClick = { viewModel.executeMacro2Script(macro.command) },
                        )
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(title = "Developer")
                    devMacros.forEach { macro ->
                        MacroRow(
                            macro = macro,
                            enabled = isConnected,
                            onClick = { viewModel.executeMacro2Script(macro.command) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionPill(isConnected: Boolean) {
    val color = if (isConnected) Success else MaterialTheme.colorScheme.error
    AppCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconTile(
                icon = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                color = color,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isConnected) "HID link active" else "HID link disconnected",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (isConnected) "Macros will run on the target"
                    else "Pair a device to enable macros",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LabelPill(
                text = if (isConnected) "Ready" else "Offline",
                background = color.copy(alpha = 0.15f),
                foreground = color,
            )
        }
    }
}

@Composable
private fun DiagnosticCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    AppCard(
        onClick = if (enabled) onClick else null,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconTile(icon = icon, color = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MacroRow(
    macro: MacroDefinition,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    AppCard(
        onClick = if (enabled) onClick else null,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconTile(icon = macro.icon, color = macro.color)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = macro.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = macro.command,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}
