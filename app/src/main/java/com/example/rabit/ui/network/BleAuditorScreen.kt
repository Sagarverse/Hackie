package com.example.rabit.ui.network

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
fun BleAuditorScreen(viewModel: BleAuditorViewModel, apiKey: String, onBack: () -> Unit) {
    val devices by viewModel.devices.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "BLE SECURITY AUDITOR",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text("RF RECONNAISSANCE", color = AccentTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState is BleState.DeviceSelected) viewModel.reset()
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                    }
                },
                actions = {
                    if (uiState is BleState.Idle || uiState is BleState.Scanning) {
                        IconButton(onClick = { viewModel.startScan() }) {
                            Icon(Icons.Default.Refresh, null, tint = AccentTeal)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is BleState.Scanning, is BleState.Idle -> {
                    DeviceList(devices, onSelect = { viewModel.selectDevice(it) })
                }
                is BleState.DeviceSelected -> {
                    DeviceAnalysis(state, apiKey, onAnalyze = { viewModel.analyzeWithAi(apiKey) })
                }
            }
        }
    }
}

@Composable
fun DeviceList(devices: List<BleDevice>, onSelect: (BleDevice) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(devices) { device ->
            Surface(
                onClick = { onSelect(device) },
                color = Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(AccentTeal.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Bluetooth, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(device.name, color = Platinum, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(device.address, color = Silver.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${device.rssi} dBm", color = if (device.rssi > -70) SuccessGreen else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("RSSI", color = Color.Gray, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceAnalysis(state: BleState.DeviceSelected, apiKey: String, onAnalyze: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // --- Header ---
        Surface(
            color = AccentTeal.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BluetoothConnected, null, tint = AccentTeal, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(state.device.name, color = Platinum, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(state.device.address, color = AccentTeal, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- AI Analysis ---
        if (state.aiInsight != null) {
            Text("TACTICAL INTELLIGENCE", color = AccentTeal, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentTeal.copy(alpha = 0.3f))
            ) {
                Text(
                    text = state.aiInsight,
                    color = Platinum,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Services ---
        Text("DISCOVERED SERVICES", color = Silver.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(12.dp))
        
        if (state.services.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentTeal)
            }
        } else {
            state.services.forEach { service ->
                ServiceItem(service)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAnalyze,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = Obsidian),
            enabled = !state.isAnalyzing && apiKey.isNotBlank()
        ) {
            if (state.isAnalyzing) {
                CircularProgressIndicator(color = Obsidian, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI VULNERABILITY TRIAGE", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun ServiceItem(service: BleServiceInfo) {
    Surface(
        color = Color.White.copy(alpha = 0.02f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Service: ${service.uuid.take(8)}...", color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            service.characteristics.forEach { char ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(
                        Icons.Default.SettingsInputAntenna, 
                        null, 
                        tint = if (char.isDangerous) Color(0xFFFF3131) else Color.Gray, 
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(char.uuid.take(8) + "...", color = if (char.isDangerous) Color(0xFFFF3131) else Silver, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.weight(1f))
                    char.properties.forEach { prop ->
                        Text(
                            prop, 
                            color = if (prop == "WRITE") Color(0xFFFF3131) else Color.Gray, 
                            fontSize = 8.sp, 
                            modifier = Modifier.padding(start = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
