package com.example.rabit.ui.network

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothShadowScreen(
    viewModel: BluetoothShadowViewModel,
    onBack: () -> Unit
) {
    val devices by viewModel.discoveredDevices.collectAsState()
    val isScanning by viewModel.isShadowScanning.collectAsState()
    val logs by viewModel.shadowLog.collectAsState()

    val isGhosting by viewModel.isGhosting.collectAsState()
    val activeIdentity by viewModel.activeIdentity.collectAsState()
    var selectedGhostIdentity by remember { mutableStateOf("Sony WH-1000XM4") }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BLUETOOTH SHADOW", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("STEALTH LINK & GATT HIJACKER", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Ghost Identity Spoofer
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isGhosting) SuccessGreen.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isGhosting) SuccessGreen.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("GHOST IDENTITY SPOOFER", color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(if (isGhosting) "ACTIVE AS: $activeIdentity" else "SYSTEM ID VISIBLE", color = if (isGhosting) SuccessGreen else Silver, fontSize = 9.sp)
                    }
                    
                    if (!isGhosting) {
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            Text(
                                selectedGhostIdentity,
                                color = AccentBlue,
                                fontSize = 10.sp,
                                modifier = Modifier.clickable { expanded = true }.padding(8.dp).border(1.dp, AccentBlue.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(4.dp)
                            )
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("Sony WH-1000XM4", "Apple Watch S9", "HP LaserJet Pro", "Tesla Model 3").forEach { id ->
                                    DropdownMenuItem(text = { Text(id) }, onClick = { selectedGhostIdentity = id; expanded = false })
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                    
                    Switch(
                        checked = isGhosting,
                        onCheckedChange = { viewModel.toggleGhostMode(selectedGhostIdentity) },
                        colors = SwitchDefaults.colors(checkedThumbColor = SuccessGreen, checkedTrackColor = SuccessGreen.copy(alpha = 0.3f))
                    )
                }
            }

            // BLE Disruptor (Spam)
            val isSpamming by viewModel.isSpamming.collectAsState()
            var selectedSpamProfile by remember { mutableStateOf("Apple_Popup_Flood") }

            Card(
                colors = CardDefaults.cardColors(containerColor = if (isSpamming) Color.Red.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSpamming) Color.Red.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("BLE DISRUPTOR (SPAM)", color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(if (isSpamming) "BROADCASTING: $selectedSpamProfile" else "SPAM ENGINE STANDBY", color = if (isSpamming) Color.Red else Silver, fontSize = 9.sp)
                    }
                    
                    if (!isSpamming) {
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            Text(
                                selectedSpamProfile.replace("_", " "),
                                color = AccentBlue,
                                fontSize = 10.sp,
                                modifier = Modifier.clickable { expanded = true }.padding(8.dp).border(1.dp, AccentBlue.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(4.dp)
                            )
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("Apple_Popup_Flood", "Android_Fast_Pair", "Windows_Swift_Pair").forEach { id ->
                                    DropdownMenuItem(text = { Text(id.replace("_", " ")) }, onClick = { selectedSpamProfile = id; expanded = false })
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = { viewModel.toggleBleSpam(selectedSpamProfile) },
                        modifier = Modifier.background(if (isSpamming) Color.Red else Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(if (isSpamming) Icons.Default.WifiTetheringError else Icons.Default.Campaign, null, tint = if (isSpamming) Color.Black else Color.Red)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Radar Visualizer
            Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                ShadowRadar(isScanning)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isScanning) "SCANNING FREQUENCIES" else "STANDBY", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text("${devices.size} SIGNALS DETECTED", color = Silver, fontSize = 9.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action Row
            Button(
                onClick = { if (isScanning) viewModel.stopShadowScan() else viewModel.startShadowScan() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isScanning) Color.Red.copy(alpha = 0.2f) else AccentBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(if (isScanning) Icons.Default.Stop else Icons.Default.Radar, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isScanning) "ABORT MISSION" else "INITIATE SHADOW SCAN", fontWeight = FontWeight.Black)
            }

            Spacer(Modifier.height(16.dp))

            // Main Content: Split List & Logs
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Device List
                LazyColumn(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    items(devices) { device ->
                        ShadowDeviceItem(device) { viewModel.initiateShadowLink(device) }
                    }
                }

                // Shadow Logs
                Box(modifier = Modifier.weight(0.8f).fillMaxHeight().background(Color.Black, RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(logs) { log ->
                            Text(log, color = AccentBlue.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShadowRadar(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse)
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart)
    )

    Canvas(modifier = Modifier.size(160.dp)) {
        drawCircle(AccentBlue.copy(alpha = 0.1f), radius = size.minDimension / 2)
        drawCircle(AccentBlue.copy(alpha = 0.2f), radius = size.minDimension / 3, style = Stroke(width = 1.dp.toPx()))
        drawCircle(AccentBlue.copy(alpha = 0.3f), radius = size.minDimension / 4, style = Stroke(width = 1.dp.toPx()))
        
        if (isActive) {
            drawCircle(AccentBlue.copy(alpha = 1f - scale), radius = (size.minDimension / 2) * scale, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
fun ShadowDeviceItem(device: ShadowDevice, onClick: () -> Unit) {
    var showVuln by remember { mutableStateOf(false) }

    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth().clickable { showVuln = !showVuln },
        border = androidx.compose.foundation.BorderStroke(1.dp, if (device.status.contains("Link")) SuccessGreen.copy(alpha = 0.3f) else Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bluetooth, null, tint = if (device.status.contains("Link")) SuccessGreen else AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(device.name, color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(device.address, color = Silver, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
                Text("${device.rssi}dBm", color = Silver, fontSize = 10.sp)
            }
            
            if (device.vulnerability != null && showVuln) {
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(8.dp)) {
                    Text(device.vulnerability, color = AccentBlue, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("ESTABLISH SHADOW LINK", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
