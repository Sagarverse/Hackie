package com.example.rabit.ui.remotedeck

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteDeckScreen(viewModel: RemoteDeckViewModel) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val pin by viewModel.currentPin.collectAsState()
    val clientStats by viewModel.clientStats.collectAsState()
    val location by viewModel.location.collectAsState()
    val localIp by viewModel.localIp.collectAsState()
    
    val clipboardManager = LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }
    
    val accentColor = Color(0xFF00F2FF)
    val purpleColor = Color(0xFFBC13FE)
    val bgColor = Color(0xFF05050A)

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-100).dp)
                .blur(100.dp)
                .background(purpleColor.copy(alpha = 0.1f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Interaction Sandbox",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            Text(
                text = "Advanced Orchestration & C2 Lab",
                fontSize = 12.sp,
                color = accentColor.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // --- Session Control ---
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ACTIVE PIN", fontSize = 10.sp, color = Color.Gray)
                            Text(pin, fontSize = 32.sp, fontWeight = FontWeight.Black, color = accentColor, fontFamily = FontFamily.Monospace)
                        }
                        ConnectionBadge(status = connectionStatus)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val webUrl = "http://$localIp:8080/remote_deck.html"
                    Surface(
                        onClick = { clipboardManager.setText(AnnotatedString(webUrl)) },
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(webUrl, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = accentColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Sensor Dashboard ---
            SectionHeader("Remote Vitals")
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (clientStats.isEmpty()) {
                        Text("Awaiting neural uplink...", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        StatRow("OS / Platform", clientStats["os"] ?: "Unknown", Icons.Default.Computer)
                        StatRow("Hardware", "${clientStats["ram"]} RAM / ${clientStats["cores"]}", Icons.Default.SettingsSuggest)
                        StatRow("Network Adr", clientStats["ip"] ?: "N/A", Icons.Default.Public)
                        
                        location?.let { (lat, lon) ->
                            StatRow("GPS Coordinate", "$lat, $lon", Icons.Default.LocationOn)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- C2 Handlers ---
            SectionHeader("Tactical Controllers")
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Neural payload...", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColor
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.pushText(inputText); inputText = "" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                        ) {
                            Text("PUSH INTEL", fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { viewModel.triggerFocus() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = purpleColor)
                        ) {
                            Text("FULLSCREEN", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.triggerRemoteAlert("PROXIMITY WARNING") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3131))
                        ) {
                            Text("SEND ALERT", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
// Helpers from previous implementation
@Composable
fun GlassCard(content: @Composable () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun StatRow(label: String, value: String, icon: ImageVector) {
    Row(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFFBC13FE), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 9.sp, color = Color.Gray)
            Text(value, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ConnectionBadge(status: String) {
    val color = if (status.contains("Connected", true)) Color(0xFF39FF14) else Color(0xFFFF3131)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(status.uppercase(), fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold)
    }
}
