package com.example.rabit.ui.home

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.components.AppCard
import com.example.rabit.ui.components.AppCardElevated
import com.example.rabit.ui.components.IconTile
import com.example.rabit.ui.components.InfoPill
import com.example.rabit.ui.components.LabelPill
import com.example.rabit.ui.components.MediaMiniPlayer
import com.example.rabit.ui.components.PrimaryButton
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.components.StatusDot
import com.example.rabit.ui.theme.HackieSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenHelper: () -> Unit,
    onOpenKeyboard: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenMacros: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenWebBridge: () -> Unit,
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val knownWorkstations by viewModel.knownWorkstations.collectAsState()
    val isHelperConnected by viewModel.isHelperConnected.collectAsState()
    val helperName by viewModel.helperDeviceName.collectAsState()
    val helperMac by viewModel.helperDeviceMac.collectAsState()
    val helperBaseUrl by viewModel.helperBaseUrl.collectAsState()
    val helperIp by viewModel.helperDeviceIp.collectAsState()
    val helperConnectionStatus by viewModel.helperConnectionStatus.collectAsState()
    val nowPlayingTitle by viewModel.nowPlayingTitle.collectAsState()
    val nowPlayingArtist by viewModel.nowPlayingArtist.collectAsState()
    val nowPlayingAlbum by viewModel.nowPlayingAlbum.collectAsState()
    val nowPlayingArtworkBase64 by viewModel.nowPlayingArtworkBase64.collectAsState()

    val artwork = remember(nowPlayingArtworkBase64) {
        try {
            nowPlayingArtworkBase64?.let {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }

    val bluetoothConnectedName =
        (connectionState as? HidDeviceManager.ConnectionState.Connected)?.deviceName
    val bluetoothConnectedMac = if (bluetoothConnectedName != null) {
        knownWorkstations.firstOrNull { it.name == bluetoothConnectedName }?.address
            ?: knownWorkstations.firstOrNull()?.address
    } else {
        null
    }

    val resolvedHelperHost = remember(helperBaseUrl, helperIp) {
        val fromEndpoint = runCatching {
            val host = Uri.parse(helperBaseUrl).host.orEmpty()
            host.ifBlank { null }
        }.getOrNull()
        fromEndpoint ?: helperIp.takeIf { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) }
    }

    val online = bluetoothConnectedName != null || isHelperConnected
    val deviceDisplayName = when {
        bluetoothConnectedName != null -> bluetoothConnectedName
        isHelperConnected -> helperName.ifBlank { "Desktop" }
        else -> null
    }

    ScreenScaffold(
        title = "Home",
        subtitle = if (online) "Connected" else "Not connected",
    ) { _ ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HackieSpacing.md),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = HackieSpacing.sm,
                bottom = HackieSpacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(HackieSpacing.md),
        ) {
            // ── Hero status card ─────────────────────────────────────
            item {
                HeroStatusCard(
                    online = online,
                    deviceName = deviceDisplayName,
                    helperStatus = helperConnectionStatus,
                    bluetoothName = bluetoothConnectedName,
                    helperHost = resolvedHelperHost,
                    bluetoothMac = bluetoothConnectedMac,
                    helperMac = helperMac,
                    onPrimaryAction = if (online) onOpenKeyboard else onOpenHelper,
                    primaryLabel = if (online) "Open keyboard" else "Connect a device",
                )
            }

            // ── Now playing mini bar ─────────────────────────────────
            if (online) {
                item {
                    AppCardElevated(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = HackieSpacing.xs,
                            vertical = HackieSpacing.xs,
                        ),
                    ) {
                        MediaMiniPlayer(
                            title = nowPlayingTitle.ifBlank { "Nothing playing" },
                            artist = nowPlayingArtist.ifBlank { null },
                            isPlaying = nowPlayingTitle.isNotBlank() && nowPlayingTitle != "Nothing playing",
                            onTogglePlay = { viewModel.sendMediaPlayPause() },
                            onNext = { viewModel.sendMediaNextTrack() },
                            onPrevious = { viewModel.sendMediaPreviousTrack() },
                        )
                    }
                }
            }

            // ── Shortcuts grid ───────────────────────────────────────
            item {
                Text(
                    text = "Shortcuts",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = HackieSpacing.xs, top = HackieSpacing.xs),
                )
            }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                ) {
                    ShortcutTile(
                        icon = Icons.Default.Bolt,
                        label = "Macros",
                        onClick = onOpenMacros,
                        modifier = Modifier.weight(1f),
                    )
                    ShortcutTile(
                        icon = Icons.Default.SmartToy,
                        label = "AI Assistant",
                        onClick = onOpenAssistant,
                        modifier = Modifier.weight(1f),
                    )
                    ShortcutTile(
                        icon = Icons.Default.AccountTree,
                        label = "Web Bridge",
                        onClick = onOpenWebBridge,
                        modifier = Modifier.weight(1f),
                    )
                    ShortcutTile(
                        icon = Icons.Default.ContentPaste,
                        label = "Vault",
                        onClick = onOpenVault,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Device details (when connected) ──────────────────────
            if (online) {
                item {
                    Text(
                        text = "Connection",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = HackieSpacing.xs, top = HackieSpacing.sm),
                    )
                }
                item {
                    AppCard {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                        ) {
                            InfoPill(
                                label = "IP",
                                value = when {
                                    isHelperConnected && !resolvedHelperHost.isNullOrBlank() -> resolvedHelperHost
                                    bluetoothConnectedName != null -> "Bluetooth"
                                    !resolvedHelperHost.isNullOrBlank() -> resolvedHelperHost
                                    else -> "—"
                                },
                            )
                            InfoPill(
                                label = "MAC",
                                value = (bluetoothConnectedMac ?: helperMac).take(17),
                            )
                            if (bluetoothConnectedName != null) {
                                LabelPill(
                                    text = "Bluetooth HID",
                                    background = MaterialTheme.colorScheme.secondaryContainer,
                                    foreground = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
            }

            // ── Diagnostic row (Helper, Rescan) ──────────────────────
            item {
                Spacer(Modifier.height(HackieSpacing.xs))
            }
            item {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        DiagnosticRow(
                            icon = Icons.Default.Devices,
                            title = "Helper",
                            subtitle = helperConnectionStatus,
                            onClick = onOpenHelper,
                        )
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(vertical = HackieSpacing.xxs),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        DiagnosticRow(
                            icon = Icons.Default.Sensors,
                            title = "Rescan",
                            subtitle = "Re-detect the helper on the local network",
                            onClick = { viewModel.discoverHelperOnLocalWifi() },
                        )
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(vertical = HackieSpacing.xxs),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        DiagnosticRow(
                            icon = Icons.Default.Wifi,
                            title = "Ping",
                            subtitle = "Send a ping to the connected device",
                            onClick = { viewModel.pingRemoteDevice() },
                        )
                    }
                }
            }
        }
    }
}

// ── Hero status card ─────────────────────────────────────────────────────

@Composable
private fun HeroStatusCard(
    online: Boolean,
    deviceName: String?,
    helperStatus: String,
    bluetoothName: String?,
    helperHost: String?,
    bluetoothMac: String?,
    helperMac: String?,
    onPrimaryAction: () -> Unit,
    primaryLabel: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(HackieSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HackieSpacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(
                    color = if (online) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.size(HackieSpacing.xs))
                Text(
                    text = if (online) "Connected" else "Not connected",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (online) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = deviceName ?: "No device linked",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = when {
                    online && bluetoothName != null -> "Paired over Bluetooth HID"
                    online -> helperStatus.ifBlank { "Helper connected" }
                    else -> "Pair a Mac, PC, or Android to start."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(HackieSpacing.xs))

            PrimaryButton(
                text = primaryLabel,
                onClick = onPrimaryAction,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            )
        }
    }
}

// ── Shortcut tile ────────────────────────────────────────────────────────

@Composable
private fun ShortcutTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(
        onClick = onClick,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = HackieSpacing.md,
            vertical = HackieSpacing.md,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
        ) {
            IconTile(icon = icon, size = 40.dp, iconSize = 20.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Diagnostic row (used in the bottom card) ─────────────────────────────

@Composable
private fun DiagnosticRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = HackieSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon = icon, size = 36.dp, iconSize = 18.dp)
        Spacer(Modifier.size(HackieSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
