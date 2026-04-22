package com.example.rabit.ui.automation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.domain.model.TerminalDevice
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScannerScreen(
    viewModel: AutomationViewModel,
    onBack: () -> Unit,
    onConnect: (String, Int, String) -> Unit
) {
    val devices by viewModel.scannedTerminals.collectAsState()
    val progress by viewModel.terminalScanProgress.collectAsState()
    val isScanning by viewModel.isTerminalScanning.collectAsState()
    
    val scanningText = remember(progress) {
        if (progress >= 1f) "SCAN COMPLETE" 
        else "PROBING SUBNET [${(progress * 100).toInt()}%]"
    }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "TERMINAL LAB",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            color = Platinum
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Scanner Status / Terminal Header
            TerminalHeader(scanningText, isScanning, onStartScan = { viewModel.scanTerminalDevices() })

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = AccentTeal,
                trackColor = Platinum.copy(alpha = 0.05f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "DISCOVERED NODES",
                color = Silver.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!isScanning && devices.isEmpty()) {
                HelpCard()
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (devices.isEmpty() && !isScanning) {
                EmptyDiscoveryState()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    items(devices) { device ->
                        DiscoveredDeviceItem(device, viewModel, onConnect)
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalHeader(text: String, isScanning: Boolean, onStartScan: () -> Unit) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Platinum.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isScanning) Color.Yellow else SuccessGreen, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text,
                        color = Platinum,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    "Scanning full /24 subnet (Ports: 21, 22, 23, 80, 443, 5555)",
                    color = Silver.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onStartScan,
                enabled = !isScanning,
                modifier = Modifier
                    .background(
                        if (isScanning) Platinum.copy(alpha = 0.05f) else AccentTeal.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    if (isScanning) Icons.Default.Refresh else Icons.Default.Radar,
                    null,
                    tint = if (isScanning) Silver else AccentTeal
                )
            }
        }
    }
}

@Composable
private fun DiscoveredDeviceItem(
    device: TerminalDevice,
    viewModel: AutomationViewModel,
    onConnect: (String, Int, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when(device.protocol) {
                                    "SSH" -> Icons.Default.Terminal
                                    "ADB" -> Icons.Default.Android
                                    "Telnet" -> Icons.Default.SettingsEthernet
                                    "HTTP", "HTTPS" -> Icons.Default.Language
                                    else -> Icons.Default.Devices
                                },
                                null,
                                tint = when(device.riskLevel) {
                                    "CRITICAL" -> Color.Red
                                    "HIGH" -> Color(0xFFFF4500)
                                    "MEDIUM" -> Color.Yellow
                                    "LOW" -> SuccessGreen
                                    else -> Platinum.copy(alpha = 0.8f)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            device.ip,
                            color = Platinum,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${device.protocol} : ${device.port}",
                                color = Silver.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                            if (device.riskLevel != "UNKNOWN") {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "[${device.riskLevel}]",
                                    color = when(device.riskLevel) {
                                        "CRITICAL" -> Color.Red
                                        "HIGH" -> Color(0xFFFF4500)
                                        "MEDIUM" -> Color.Yellow
                                        else -> SuccessGreen
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.analyzeHostVulnerabilities(device) },
                        modifier = Modifier.background(AccentTeal.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConnect(device.ip, device.port, device.protocol) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("PWN", color = AccentTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    if (device.banner != null) {
                        Text("BANNER", color = Silver.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            device.banner,
                            color = SuccessGreen.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    if (device.aiInsight != null) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, null, tint = AccentTeal, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI TACTICAL REPORT", color = AccentTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    device.aiInsight,
                                    color = Platinum.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            "Tap the AI icon to analyze vulnerabilities",
                            color = Silver.copy(alpha = 0.3f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpOutline, null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("LAB INTELLIGENCE", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text(
                "• Radar Scan: Probes every IP on your current Wi-Fi subnet.\n" +
                "• Target Ports: 22 (SSH), 23 (Telnet), 5555 (ADB Wireless).\n" +
                "• Quick Login: Tap any discovered node to instantly open a terminal session.\n" +
                "• Requirements: Target device must have 'Sharing' or 'ADB' enabled.",
                color = Silver.copy(alpha = 0.6f),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun EmptyDiscoveryState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.WifiTetheringOff,
            null,
            tint = Silver.copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Signals Quiet",
            color = Silver.copy(alpha = 0.4f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Initiate radar scan to find nodes",
            color = Silver.copy(alpha = 0.3f),
            fontSize = 13.sp
        )
    }
}
