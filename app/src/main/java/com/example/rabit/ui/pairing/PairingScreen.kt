package com.example.rabit.ui.pairing

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.domain.model.Workstation
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.network.BluetoothMirrorViewModel
import com.example.rabit.ui.network.BluetoothShadowViewModel
import com.example.rabit.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    viewModel: MainViewModel,
    mirrorViewModel: BluetoothMirrorViewModel,
    shadowViewModel: BluetoothShadowViewModel,
    automationViewModel: com.example.rabit.ui.automation.AutomationViewModel,
    onConnected: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isScanning by viewModel.isScanning.collectAsState()
    val isBluetoothConnected by viewModel.isBluetoothConnected.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val savedDevices by viewModel.savedDevices.collectAsState()
    val shadowDevices by shadowViewModel.discoveredDevices.collectAsState()
    val isShadowScanning by shadowViewModel.isShadowScanning.collectAsState()
    val isGhosting by shadowViewModel.isGhosting.collectAsState()
    val activeIdentity by shadowViewModel.activeIdentity.collectAsState()
    val isSpamming by shadowViewModel.isSpamming.collectAsState()
    val spamProfile by shadowViewModel.spamProfile.collectAsState()

    var showIdentityLab by remember { mutableStateOf(false) }
    var manualName by remember { mutableStateOf("") }
    var manualMac by remember { mutableStateOf("") }
    val autoReconnect by viewModel.autoReconnectEnabled.collectAsState()

    LaunchedEffect(isBluetoothConnected) {
        if (isBluetoothConnected) onConnected()
    }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "CONTROL HUB",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text("TACTICAL CONNECTION INTERFACE", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = Silver)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── QUICK ACTIONS ──
            item {
                QuickActionPanel(
                    isJamming = isSpamming,
                    isScanning = isScanning || isShadowScanning,
                    onToggleJam = { shadowViewModel.toggleBleSpam("Apple_Popup_Flood") },
                    onToggleScan = { 
                        if (isScanning || isShadowScanning) {
                            viewModel.stopScan()
                            shadowViewModel.stopShadowScan()
                        } else {
                            viewModel.startScan()
                            shadowViewModel.startShadowScan()
                        }
                    }
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Graphite.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (autoReconnect) AccentBlue.copy(alpha = 0.15f) else SoftGrey.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                tint = if (autoReconnect) AccentBlue else Silver,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AUTO RECONNECT", color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Auto-link to last known device", color = Silver.copy(alpha = 0.7f), fontSize = 10.sp)
                        }
                        Switch(
                            checked = autoReconnect,
                            onCheckedChange = { viewModel.setAutoReconnectEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentBlue,
                                checkedTrackColor = AccentBlue.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // ── ACTIVE IDENTITY ──
            item {
                ActiveIdentityCard(
                    identity = activeIdentity,
                    isGhosting = isGhosting,
                    onReset = { shadowViewModel.stopGhosting() }
                )
            }

            // ── SAVED & RECENT TARGETS ──
            item {
                SectionHeader("SAVED & RECENT TARGETS", Icons.Default.History)
            }
            
            if (savedDevices.isEmpty() && discoveredDevices.isEmpty()) {
                item {
                    EmptyStateCard("Awaiting Target Signature...", "Start scanning to detect nearby nodes")
                }
            } else {
                items(savedDevices) { device ->
                    TargetDeviceCard(
                        name = device.name,
                        address = device.address,
                        isPaired = true,
                        onClick = { 
                            shadowViewModel.stopGhosting()
                            shadowViewModel.stopSpamming()
                            viewModel.connectToDevice(device.address) 
                        }
                    )
                }
                items(discoveredDevices.toList()) { device ->
                    if (savedDevices.none { it.address == device.address }) {
                        TargetDeviceCard(
                            name = device.name ?: "Unknown Device",
                            address = device.address,
                            isPaired = false,
                            onClick = { 
                                shadowViewModel.stopGhosting()
                                shadowViewModel.stopSpamming()
                                viewModel.connectToDevice(device.address) 
                            }
                        )
                    }
                }
            }

            // ── IDENTITY LAB (TACTICAL CUSTOMIZATION) ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("IDENTITY LAB", Icons.Default.Science)
                    TextButton(onClick = { showIdentityLab = !showIdentityLab }) {
                        Text(if (showIdentityLab) "CLOSE LAB" else "OPEN CUSTOMIZATION", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showIdentityLab) {
                item {
                    IdentityLabPanel(
                        name = manualName,
                        mac = manualMac,
                        onNameChange = { manualName = it },
                        onMacChange = { manualMac = it },
                        onDeploy = { shadowViewModel.toggleGhostMode(manualName) },
                        shadowDevices = shadowDevices,
                        onClone = { device -> 
                            manualName = device.name
                            manualMac = device.address
                            shadowViewModel.toggleGhostMode(device.name)
                        },
                        isJamming = isSpamming,
                        onToggleJam = { shadowViewModel.toggleBleSpam("Apple_Popup_Flood") }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun QuickActionPanel(
    isJamming: Boolean,
    isScanning: Boolean,
    onToggleJam: () -> Unit,
    onToggleScan: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionCard(
            title = "PULSE SCAN",
            subtitle = if (isScanning) "ACTIVE" else "READY",
            icon = Icons.Default.Radar,
            isActive = isScanning,
            color = AccentBlue,
            onClick = onToggleScan,
            modifier = Modifier.weight(1f)
        )
        ActionCard(
            title = "SIGNAL JAM",
            subtitle = if (isJamming) "FLOODING" else "DORMANT",
            icon = Icons.Default.CellTower,
            isActive = isJamming,
            color = Color.Red,
            onClick = onToggleJam,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) color.copy(alpha = 0.15f) else Graphite.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isActive) color.copy(alpha = 0.5f) else BorderColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (isActive) color else Silver,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = if (isActive) color else Silver, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActiveIdentityCard(
    identity: String,
    isGhosting: Boolean,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SoftGrey.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if (isGhosting) AccentBlue.copy(alpha = 0.2f) else Graphite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isGhosting) Icons.Default.Fingerprint else Icons.Default.Hardware,
                    contentDescription = null,
                    tint = if (isGhosting) AccentBlue else Silver
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("CURRENT IDENTITY", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (isGhosting) identity else "Original Hardware (Native)",
                    color = if (isGhosting) AccentBlue else Platinum,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (isGhosting) {
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = Silver)
                }
            }
        }
    }
}

@Composable
fun TargetDeviceCard(
    name: String,
    address: String,
    isPaired: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Graphite.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (name.lowercase().contains("mac") || name.lowercase().contains("laptop")) Icons.Default.Laptop 
                else if (name.lowercase().contains("phone")) Icons.Default.Smartphone
                else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = if (isPaired) AccentBlue else Silver,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(address, color = Silver, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            if (isPaired) {
                Surface(
                    color = AccentBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "KNOWN",
                        color = AccentBlue,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Silver.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun ShadowCloneCard(
    name: String,
    address: String,
    status: String,
    onClone: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SoftGrey.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(address, color = Silver, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(status, color = if (status.contains("Active")) AccentBlue else Silver.copy(alpha = 0.6f), fontSize = 10.sp)
            }
            Button(
                onClick = onClone,
                colors = ButtonDefaults.buttonColors(containerColor = Graphite),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("SHADOW", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun IdentityLabPanel(
    name: String,
    mac: String,
    onNameChange: (String) -> Unit,
    onMacChange: (String) -> Unit,
    onDeploy: () -> Unit,
    shadowDevices: List<com.example.rabit.ui.network.ShadowDevice>,
    onClone: (com.example.rabit.ui.network.ShadowDevice) -> Unit,
    isJamming: Boolean,
    onToggleJam: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Graphite.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Section 1: Signal Disruption
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("SIGNAL DISRUPTION", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text("DISCONNECT OTHER NODES", color = Silver.copy(alpha = 0.6f), fontSize = 9.sp)
                }
                Switch(
                    checked = isJamming,
                    onCheckedChange = { onToggleJam() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Red, checkedTrackColor = Color.Red.copy(alpha = 0.3f))
                )
            }

            HorizontalDivider(color = BorderColor.copy(alpha = 0.2f))

            // Section 2: Identity Shadowing (Nearby Clones)
            Text("NEARBY IDENTITIES", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
            
            if (shadowDevices.isEmpty()) {
                Text("Searching for nearby signatures...", color = Silver.copy(alpha = 0.4f), fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(shadowDevices) { device ->
                        Surface(
                            onClick = { onClone(device) },
                            shape = RoundedCornerShape(12.dp),
                            color = Surface1,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Devices, null, tint = Silver, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.height(4.dp))
                                Text(device.name.take(10), color = Platinum, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(device.address.take(8), color = Silver, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderColor.copy(alpha = 0.2f))

            // Section 3: Manual Identity Override
            Text("MANUAL OVERRIDE", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
            
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("EMULATED DEVICE NAME", fontSize = 9.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Platinum
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )
                OutlinedTextField(
                    value = mac,
                    onValueChange = onMacChange,
                    label = { Text("EMULATED MAC ADDRESS", fontSize = 9.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Platinum
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )
                Button(
                    onClick = onDeploy,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("DEPLOY IDENTITY", color = Obsidian, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Silver, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = Silver,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun EmptyStateCard(title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.02f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Silver, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Silver.copy(alpha = 0.5f), fontSize = 11.sp)
        }
    }
}
