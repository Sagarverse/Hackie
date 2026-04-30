package com.example.rabit.ui.home

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenHelper: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val knownWorkstations by viewModel.knownWorkstations.collectAsState()
    val isHelperConnected by viewModel.isHelperConnected.collectAsState()
    val helperName by viewModel.helperDeviceName.collectAsState()
    val helperMac by viewModel.helperDeviceMac.collectAsState()
    val helperBaseUrl by viewModel.helperBaseUrl.collectAsState()
    val helperIp by viewModel.helperDeviceIp.collectAsState()
    val p2pStatus by viewModel.p2pStatus.collectAsState()
    val helperConnectionStatus by viewModel.helperConnectionStatus.collectAsState()
    val transfers by viewModel.helperTransferEvents.collectAsState()
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

    val bluetoothConnectedName = (connectionState as? HidDeviceManager.ConnectionState.Connected)?.deviceName
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface0)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // ── Compact Status Header ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (online) "Connected" else "Offline",
                    color = if (online) SuccessGreen else TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W600,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    deviceDisplayName ?: "No device linked",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.W300,
                    letterSpacing = (-0.3).sp
                )
            }
            // Status indicator dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (online) SuccessGreen else TextTertiary.copy(alpha = 0.4f),
                        CircleShape
                    )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Connection Details Card ──────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Surface1,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(AccentBlue.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Device Link", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.W600)
                        Text(
                            helperConnectionStatus,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (online) {
                        StatusPill(text = "Online", color = SuccessGreen)
                    }
                }

                if (online) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = BorderColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoChip(
                            label = "IP",
                            value = when {
                                isHelperConnected && !resolvedHelperHost.isNullOrBlank() -> resolvedHelperHost
                                bluetoothConnectedName != null -> "Bluetooth"
                                !resolvedHelperHost.isNullOrBlank() -> resolvedHelperHost
                                else -> "—"
                            }
                        )
                        InfoChip(label = "MAC", value = (bluetoothConnectedMac ?: helperMac).take(17))
                        InfoChip(label = "P2P", value = p2pStatus)
                    }

                    if (bluetoothConnectedName != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(4.dp).background(AccentTeal, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bluetooth HID", color = AccentTeal, fontSize = 11.sp, fontWeight = FontWeight.W500)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Now Playing ─────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Surface1,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Compact header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Now Playing", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.W600, letterSpacing = 0.5.sp)
                    }
                    IconButton(onClick = { viewModel.requestNowPlayingFromHost() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextTertiary, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Song info + controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Album art
                    if (artwork != null) {
                        Image(
                            bitmap = artwork,
                            contentDescription = "Album art",
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Surface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            nowPlayingTitle.ifBlank { "Nothing playing" },
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (nowPlayingArtist.isNotBlank()) {
                            Text(
                                nowPlayingArtist,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Inline controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MediaControl(icon = Icons.Default.Remove, onClick = { viewModel.sendMediaVolumeDown() })
                    MediaControl(icon = Icons.Default.FastRewind, onClick = { viewModel.sendMediaPreviousTrack() })
                    
                    // Play/Pause button — primary accent
                    IconButton(
                        onClick = { viewModel.sendMediaPlayPause() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(AccentBlue, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (nowPlayingTitle.isNotBlank() && nowPlayingTitle != "Nothing playing") Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    MediaControl(icon = Icons.Default.FastForward, onClick = { viewModel.sendMediaNextTrack() })
                    MediaControl(icon = Icons.Default.Add, onClick = { viewModel.sendMediaVolumeUp() })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Quick Actions Grid ──────────────────────────────────────────────
        Text(
            "QUICK ACTIONS",
            color = TextTertiary,
            fontSize = 9.sp,
            fontWeight = FontWeight.W700,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Devices,
                label = "Helper",
                onClick = onOpenHelper
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Wifi,
                label = "Rescan",
                onClick = { viewModel.discoverHelperOnLocalWifi() }
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.NetworkPing,
                label = "Ping",
                onClick = { viewModel.pingRemoteDevice() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun StatusPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.W600)
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label.uppercase(), color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.W700, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.W500, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MediaControl(icon: ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .background(Surface2, CircleShape)
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Surface1,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.W500)
        }
    }
}
