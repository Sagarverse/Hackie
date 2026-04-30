package com.example.rabit.ui.airplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.Graphite
import com.example.rabit.ui.theme.Obsidian
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver

@Composable
fun AirPlayReceiverScreen(
    viewModel: MainViewModel,
    webBridgeViewModel: com.example.rabit.ui.webbridge.WebBridgeViewModel,
    onBack: () -> Unit
) {
    // Kept for API compatibility with existing navigation wiring.
    @Suppress("UNUSED_VARIABLE")
    val keepArgs = webBridgeViewModel to onBack
    val enabled by viewModel.airPlayReceiverEnabled.collectAsState()
    val status by viewModel.airPlayStatus.collectAsState()
    val isStreaming = status.contains("ALAC decode active", ignoreCase = true) ||
        status.contains("RTP packets delivered=", ignoreCase = true)
    val fallbackRequired = status.contains("fallback required", ignoreCase = true) ||
        status.contains("Unsupported RAOP codec", ignoreCase = true) ||
        status.contains("Encrypted RAOP session requested", ignoreCase = true)

    Scaffold(containerColor = Obsidian) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Obsidian)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Graphite.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speaker, contentDescription = null, tint = AccentBlue)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("AirPlay Receiver", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Text(
                        if (enabled) "Mac audio routes to your phone speaker." else "Turn on to receive audio from Mac.",
                        color = Silver,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Receiver", color = Platinum, fontWeight = FontWeight.SemiBold)
                            Text(if (enabled) "Running" else "Stopped", color = Silver, fontSize = 12.sp)
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { if (it) viewModel.startAirPlayReceiver() else viewModel.stopAirPlayReceiver() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Platinum, checkedTrackColor = AccentBlue)
                        )
                    }
                    Text(
                        when {
                            !enabled -> "Status: Idle"
                            fallbackRequired -> "Status: Encryption/Codec fallback needed"
                            isStreaming -> "Status: Streaming to phone speaker"
                            else -> "Status: Waiting for Mac (Ensure same Wi-Fi)"
                        },
                        color = if (fallbackRequired) Color.Yellow else Silver.copy(alpha = 0.88f),
                        fontSize = 12.sp
                    )
                    if (fallbackRequired) {
                        Text(
                            "Tip: AirPlay 1 (RAOP) unencrypted mode works best. If Mac asks for code or fails, check network.",
                            color = Silver.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Graphite.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Playback Controls", color = Platinum, fontWeight = FontWeight.Bold)
                    Text(
                        "Controls your Mac media while connected.",
                        color = Silver,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.sendMediaPreviousTrack() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Graphite)
                        ) {
                            Icon(Icons.Default.KeyboardDoubleArrowLeft, contentDescription = null)
                        }
                        Button(
                            onClick = { viewModel.sendMediaPlayPause() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        }
                        Button(
                            onClick = { viewModel.sendMediaNextTrack() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Graphite)
                        ) {
                            Icon(Icons.Default.KeyboardDoubleArrowRight, contentDescription = null)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.sendMediaVolumeDown() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Graphite)
                        ) {
                            Icon(Icons.Default.VolumeDown, contentDescription = null)
                            Spacer(modifier = Modifier.padding(2.dp))
                            Text("Vol -")
                        }
                        Button(
                            onClick = { viewModel.sendMediaVolumeUp() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Graphite)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null)
                            Spacer(modifier = Modifier.padding(2.dp))
                            Text("Vol +")
                        }
                    }

                    Button(
                        onClick = { viewModel.restartAirPlayReceiver() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Reset Receiver", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
