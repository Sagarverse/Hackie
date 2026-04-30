package com.example.rabit.ui.network

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WirelessAuditorScreen(
    bleViewModel: BleAuditorViewModel,
    wifiViewModel: WifiAttackerViewModel,
    apiKey: String,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("WIFI AUDITOR", "BLE AUDITOR")
    val colors = listOf(Color(0xFFEAB308), AccentTeal)

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "WIRELESS AUDITOR",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    color = Platinum
                                )
                            )
                            Text("RF SPECTRUM ANALYSIS", color = colors[selectedTab], fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
                
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = colors[selectedTab],
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = colors[selectedTab]
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    title, 
                                    fontSize = 11.sp, 
                                    fontWeight = if (selectedTab == index) FontWeight.Black else FontWeight.Normal,
                                    color = if (selectedTab == index) Platinum else Silver.copy(alpha = 0.5f)
                                ) 
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> {
                    val networks by wifiViewModel.networks.collectAsState()
                    val attackState by wifiViewModel.attackState.collectAsState()
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (attackState is WifiAttackState.Scanning) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = colors[0])
                        }
                        
                        when (val state = attackState) {
                            is WifiAttackState.Idle, is WifiAttackState.Scanning -> {
                                WifiNetworkList(networks, wifiViewModel, onAttack = { wifiViewModel.startAttack(it, apiKey) })
                            }
                            is WifiAttackState.Attacking -> AttackProgress(state)
                            is WifiAttackState.Success -> AttackSuccess(state, onReset = { wifiViewModel.stopAttack() })
                            is WifiAttackState.Failed -> AttackFailed(state, onReset = { wifiViewModel.stopAttack() })
                        }
                    }
                    
                    if (attackState == WifiAttackState.Idle || attackState == WifiAttackState.Scanning) {
                        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomEnd) {
                            FloatingActionButton(
                                onClick = { wifiViewModel.startScan() },
                                containerColor = colors[0],
                                contentColor = Obsidian,
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Wifi, null)
                            }
                        }
                    }
                }
                1 -> {
                    val devices by bleViewModel.devices.collectAsState()
                    val uiState by bleViewModel.uiState.collectAsState()
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        when (val state = uiState) {
                            is BleState.Scanning, is BleState.Idle -> {
                                DeviceList(devices, onSelect = { bleViewModel.selectDevice(it) })
                            }
                            is BleState.DeviceSelected -> {
                                DeviceAnalysis(state, apiKey, onAnalyze = { bleViewModel.analyzeWithAi(apiKey) })
                            }
                            else -> {}
                        }
                    }
                    
                    if (uiState is BleState.Idle || uiState is BleState.Scanning) {
                        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomEnd) {
                            FloatingActionButton(
                                onClick = { bleViewModel.startScan() },
                                containerColor = colors[1],
                                contentColor = Obsidian,
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Refresh, null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WifiNetworkList(
    networks: List<com.example.rabit.ui.network.WifiNetwork>,
    viewModel: WifiAttackerViewModel,
    onAttack: (com.example.rabit.ui.network.WifiNetwork) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(16.dp)) {
        items(networks) { net ->
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wifi, null, tint = Color(0xFFEAB308), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(net.ssid, color = Platinum, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(net.bssid, color = Silver, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = { onAttack(net) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("ATTACK", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Obsidian)
                    }
                }
            }
        }
    }
}

@Composable
fun AttackProgress(state: WifiAttackState.Attacking) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("CRACKING: ${state.network.ssid}", color = Color(0xFFEAB308), fontWeight = FontWeight.Black, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text("Attempting with payload: ${state.currentPassword}", color = Silver, fontSize = 11.sp)
        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Color(0xFFEAB308), trackColor = Color.White.copy(alpha = 0.1f))
    }
}

@Composable
fun AttackSuccess(state: WifiAttackState.Success, onReset: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("TARGET COMPROMISED", color = SuccessGreen, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(state.network.ssid, color = Platinum, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RECOVERED KEY", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(state.password, color = Platinum, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onReset, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))) {
            Text("RETURN TO SCANNER", color = Platinum)
        }
    }
}

@Composable
fun AttackFailed(state: WifiAttackState.Failed, onReset: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("ATTACK FAILED", color = Color.Red, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(state.reason, color = Silver, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onReset, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))) {
            Text("RETRY", color = Platinum)
        }
    }
}

@Composable
fun DeviceList(devices: List<com.example.rabit.ui.network.BleDevice>, onSelect: (com.example.rabit.ui.network.BleDevice) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(16.dp)) {
        items(devices) { dev ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(dev) },
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bluetooth, null, tint = AccentTeal, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(dev.name, color = Platinum, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(dev.address, color = Silver, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceAnalysis(state: BleState.DeviceSelected, apiKey: String, onAnalyze: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("DEVICE PROFILE: ${state.device.name}", color = AccentTeal, fontWeight = FontWeight.Black, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        
        if (state.aiInsight != null) {
            Text(state.aiInsight, color = Platinum, fontSize = 13.sp, lineHeight = 20.sp)
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (state.isAnalyzing) {
                    CircularProgressIndicator(color = AccentTeal)
                } else {
                    Button(onClick = onAnalyze, colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)) {
                        Text("GENERATE AI RISK REPORT", color = Obsidian, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
