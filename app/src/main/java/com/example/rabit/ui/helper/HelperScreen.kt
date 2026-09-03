package com.example.rabit.ui.helper

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.rabit.ui.components.AppCard
import com.example.rabit.ui.components.IconTile
import com.example.rabit.ui.components.InfoPill
import com.example.rabit.ui.components.PrimaryButton
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.components.SectionHeader
import com.example.rabit.ui.components.SettingsToggleRow
import com.example.rabit.ui.components.TonalButton
import com.example.rabit.ui.theme.HackieSpacing
import com.example.rabit.ui.theme.Success

@Composable
fun HelperScreen(
    viewModel: HelperViewModel,
    onBack: () -> Unit
) {
    val isConnected by viewModel.isHelperConnected.collectAsState()
    val helperName by viewModel.helperDeviceName.collectAsState()
    val helperBaseUrl by viewModel.helperBaseUrl.collectAsState()
    val helperConnectionStatus by viewModel.helperConnectionStatus.collectAsState()
    val helperIp by viewModel.helperDeviceIp.collectAsState()
    val helperMac by viewModel.helperDeviceMac.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    var helperUrlInput by remember(helperBaseUrl) { mutableStateOf(helperBaseUrl) }
    var terminalCommand by remember { mutableStateOf("") }
    var clipboardSyncEnabled by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.sendFileToHelper(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.ensurePhoneHelperReceiverRunning()
        viewModel.fetchHelperDeviceDetails()
        viewModel.setClipboardSyncState(clipboardSyncEnabled)
    }

    ScreenScaffold(
        title = "Helper",
        subtitle = if (isConnected) "Connected to $helperName"
        else "Not connected",
        onBack = onBack,
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = HackieSpacing.md),
            verticalArrangement = Arrangement.spacedBy(HackieSpacing.md),
        ) {
            // ── Status card ──────────────────────────────────────
            AppCard {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(HackieSpacing.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = HackieSpacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = if (isConnected) Success.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Devices
                            else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isConnected) Success
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isConnected) helperName.ifBlank { "Desktop linked" }
                            else "No desktop linked",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (isConnected) "Active on $helperIp"
                            else helperConnectionStatus.ifBlank { "Searching for a desktop helper…" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isConnected) Success
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                    ) {
                        InfoPill(label = "IP", value = helperIp.ifBlank { "—" })
                        InfoPill(label = "MAC", value = helperMac.take(17).ifBlank { "—" })
                    }
                }
            }

            // ── Manual URL override ──────────────────────────────
            SectionHeader(title = "Connection URL")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm)) {
                    Text(
                        text = "Override the auto-discovered endpoint. Useful for connecting over a specific network or VPN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = helperUrlInput,
                        onValueChange = { helperUrlInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("http://192.168.1.50:8080") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Uri,
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm)) {
                        OutlinedButton(
                            onClick = { viewModel.discoverHelperOnLocalWifi() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(HackieSpacing.xs))
                            Text("Auto-discover")
                        }
                        PrimaryButton(
                            text = "Save URL",
                            onClick = {
                                val target = helperUrlInput.trim()
                                if (target.isNotBlank()) {
                                    viewModel.setHelperBaseUrl(target)
                                }
                            },
                            enabled = helperUrlInput.isNotBlank() && helperUrlInput != helperBaseUrl,
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Save,
                        )
                    }
                }
            }

            // ── Send a file ──────────────────────────────────────
            SectionHeader(title = "Send to desktop")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm)) {
                    Text(
                        text = "Pick a file on this device and it lands on the desktop's working folder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PrimaryButton(
                        text = "Choose file",
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.Upload,
                    )
                }
            }

            // ── Remote shell ─────────────────────────────────────
            SectionHeader(title = "Remote shell")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm)) {
                    OutlinedTextField(
                        value = terminalCommand,
                        onValueChange = { terminalCommand = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter a command…") },
                        shape = MaterialTheme.shapes.large,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (terminalCommand.isNotBlank()) {
                                        viewModel.runRemoteShellCommand(terminalCommand)
                                        terminalCommand = ""
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = "Run",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Box(modifier = Modifier.padding(HackieSpacing.md)) {
                            Text(
                                text = terminalOutput.ifBlank { "Ready for input." },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                                color = if (terminalOutput.isBlank())
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                            )
                        }
                    }
                }
            }

            // ── Discovery & sync ─────────────────────────────────
            SectionHeader(title = "Discovery & sync")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(HackieSpacing.xs)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = HackieSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                    ) {
                        IconTile(
                            icon = Icons.Default.Lan,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scan local network",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Look for a desktop helper on the current Wi-Fi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TonalButton(
                            text = "Scan",
                            onClick = { viewModel.discoverHelperOnLocalWifi() },
                            icon = Icons.Default.Refresh,
                        )
                    }
                }
            }

            AppCard {
                SettingsToggleRow(
                    title = "Sync clipboard",
                    subtitle = "Auto-share copy buffers between devices",
                    leading = {
                        IconTile(
                            icon = Icons.Default.ContentPaste,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    checked = clipboardSyncEnabled,
                    onCheckedChange = {
                        clipboardSyncEnabled = it
                        viewModel.setClipboardSyncState(it)
                    },
                )
            }

            Spacer(Modifier.height(HackieSpacing.lg))
        }
    }
}
