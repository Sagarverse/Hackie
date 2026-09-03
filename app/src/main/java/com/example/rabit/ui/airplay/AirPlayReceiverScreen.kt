package com.example.rabit.ui.airplay

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.network.LanIpResolver
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.components.LabelPill
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.components.StatusDot
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.Graphite
import com.example.rabit.ui.theme.HackieSpacing
import com.example.rabit.ui.theme.Obsidian
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver
import com.example.rabit.ui.theme.Success
import com.example.rabit.ui.theme.Warning

@Composable
fun AirPlayReceiverScreen(
    viewModel: MainViewModel,
    webBridgeViewModel: com.example.rabit.ui.webbridge.WebBridgeViewModel,
    onBack: () -> Unit,
) {
    // Kept for API compatibility with existing navigation wiring.
    @Suppress("UNUSED_VARIABLE")
    val keepArgs = webBridgeViewModel to onBack

    val context = LocalContext.current
    val enabled by viewModel.airPlayReceiverEnabled.collectAsState()
    val status by viewModel.airPlayStatus.collectAsState()
    val statusLog by viewModel.airPlayStatusLog.collectAsState()

    val isStreaming = status.contains("ALAC decode active", ignoreCase = true) ||
        status.contains("RTP packets delivered=", ignoreCase = true)
    val waitingForClient = enabled && !isStreaming &&
        (status.contains("Waiting for", ignoreCase = true) ||
            status.contains("ready on port", ignoreCase = true) ||
            status.contains("advertised as", ignoreCase = true))
    val fallbackRequired = status.contains("fallback required", ignoreCase = true) ||
        status.contains("Unsupported RAOP codec", ignoreCase = true) ||
        status.contains("Encrypted RAOP session requested", ignoreCase = true)

    val lanIp = remember(enabled) {
        if (enabled) LanIpResolver.preferredLanIpv4String(context) else null
    }

    ScreenScaffold(
        title = "AirPlay receiver",
        subtitle = "Stream Mac audio to this device",
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(HackieSpacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(HackieSpacing.md),
        ) {
            // ── Status hero card ───────────────────────────────────
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(HackieSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(HackieSpacing.md),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentBlue.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speaker,
                                contentDescription = null,
                                tint = AccentBlue,
                            )
                        }
                        Spacer(Modifier.size(HackieSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hackie",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (enabled) "Appears as an AirPlay speaker on your Mac" else "Turn on to start receiving audio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                if (it) viewModel.startAirPlayReceiver()
                                else viewModel.stopAirPlayReceiver()
                            },
                        )
                    }

                    // Status pill row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                    ) {
                        val (dot, label, color) = when {
                            isStreaming -> Triple(Success, "Streaming", Success)
                            fallbackRequired -> Triple(Warning, "Fallback needed", Warning)
                            waitingForClient -> Triple(AccentBlue, "Listening", AccentBlue)
                            enabled -> Triple(Silver, "Starting…", Silver)
                            else -> Triple(Silver, "Idle", Silver)
                        }
                        StatusDot(color = color, size = 10.dp)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = color,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (isStreaming) {
                            LabelPill(
                                text = "ALAC",
                                background = AccentBlue.copy(alpha = 0.18f),
                                foreground = AccentBlue,
                            )
                        }
                    }

                    // Connection target (what the Mac will see)
                    if (enabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "On your Mac",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "System Settings → Sound → Output → Hackie",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace,
                            )
                            if (lanIp != null) {
                                Text(
                                    text = "Or AirPlay menu bar icon → Hackie ($lanIp)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (fallbackRequired) {
                        Text(
                            text = "Tip: the Mac may want to negotiate encryption that this receiver can't decode. Toggle AirPlay off and back on, or play from a different app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── Transport controls ─────────────────────────────────
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(HackieSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                ) {
                    Text(
                        text = "Mac media",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "These buttons send media keys to the connected Mac.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.sendMediaPreviousTrack() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.KeyboardDoubleArrowLeft, contentDescription = null)
                        }
                        Button(
                            onClick = { viewModel.sendMediaPlayPause() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        }
                        OutlinedButton(
                            onClick = { viewModel.sendMediaNextTrack() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.KeyboardDoubleArrowRight, contentDescription = null)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.sendMediaVolumeDown() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.VolumeDown, contentDescription = null)
                            Spacer(Modifier.size(4.dp))
                            Text("Vol −")
                        }
                        OutlinedButton(
                            onClick = { viewModel.sendMediaVolumeUp() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null)
                            Spacer(Modifier.size(4.dp))
                            Text("Vol +")
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.restartAirPlayReceiver() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text("Restart receiver")
                    }
                }
            }

            // ── Status log (collapsed by default) ──────────────────
            if (statusLog.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(HackieSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Status log",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                        statusLog.take(8).forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(HackieSpacing.lg))
        }
    }
}
