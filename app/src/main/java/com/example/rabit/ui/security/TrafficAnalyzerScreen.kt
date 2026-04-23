package com.example.rabit.ui.security

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.security.NeuralTrafficAnalyzer
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficAnalyzerScreen(
    viewModel: TrafficAnalyzerViewModel,
    onBack: () -> Unit
) {
    val packets by viewModel.packets.collectAsState()
    val isHacked by viewModel.isHacked.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEURAL PACKET INSPECTOR", style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Black))
                        Text("REAL-TIME TRAFFIC IDS", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Surface0)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            
            // --- Tactical Briefing ---
            var showBriefing by remember { mutableStateOf(true) }
            if (showBriefing) {
                Surface(
                    color = AccentBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("FORENSIC BRIEFING", color = AccentBlue, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { showBriefing = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Silver, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This tool 'listens' to everything the target phone is saying on the internet. It helps you catch spy apps and viruses in real-time.",
                            color = Platinum, fontSize = 13.sp, lineHeight = 18.sp
                        )
                    }
                }
            }

            var selectedTab by remember { mutableStateOf(0) }
            val tabs = listOf("LIVE INTERCEPT", "TRAFFIC HISTORY")

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AccentBlue,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AccentBlue
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            if (selectedTab == 0) {
                LiveTrafficContent(viewModel)
            } else {
                TrafficHistoryContent(viewModel)
            }
        }
    }
}

@Composable
fun LiveTrafficContent(viewModel: TrafficAnalyzerViewModel) {
    val packets by viewModel.packets.collectAsState()
    val isHacked by viewModel.isHacked.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    Column {
        // --- Status HUD ---
        Surface(
            color = if (isHacked == true) Color(0xFF450a0a) else Surface1,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isHacked == true) Color.Red else BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val statusText = when (isHacked) {
                    true -> "CRITICAL: COMPROMISE DETECTED"
                    false -> "SECURE: NO ANOMALIES FOUND"
                    else -> "AWAITING TELEMETRY..."
                }
                val statusColor = when (isHacked) {
                    true -> Color.Red
                    false -> SuccessGreen
                    else -> Silver
                }

                Icon(
                    if (isHacked == true) Icons.Default.Warning else Icons.Default.Shield, 
                    null, 
                    tint = statusColor, 
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(statusText, color = statusColor, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                Text(if (isHacked == true) "Anomalous C2 traffic patterns identified." else "Network state within ethical parameters.", color = TextSecondary, fontSize = 11.sp)
                
                Spacer(Modifier.height(20.dp))
                
                Button(
                    onClick = { if (isAnalyzing) viewModel.stopSniffing() else viewModel.startSniffing() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isAnalyzing) Color.Red.copy(alpha = 0.2f) else AccentBlue),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isAnalyzing) "TERMINATE SNIFFER" else "INITIALIZE PACKET INSPECTION")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Packet Stream ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("LIVE PACKET STREAM", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            if (isAnalyzing) {
                Text("SNIFFING...", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f)),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                items(packets) { packet ->
                    PacketItem(packet)
                }
                
                if (packets.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No traffic captured yet.", color = TextTertiary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrafficHistoryContent(viewModel: TrafficAnalyzerViewModel) {
    val history by viewModel.trafficHistory.collectAsState()

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("PAST NETWORK LOGS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            TextButton(onClick = { viewModel.clearTrafficHistory() }) {
                Text("WIPE HISTORY", color = Color.Red, fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(history) { record ->
                TrafficRecordCard(record)
            }
            
            if (history.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No saved captures. Run a scan to log traffic.", color = TextTertiary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TrafficRecordCard(record: com.example.rabit.data.security.TrafficStorageManager.TrafficRecord) {
    Surface(
        color = Surface1,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (record.wasCompromised) Color.Red else BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.timestamp)),
                    color = Platinum,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(Modifier.weight(1f))
                if (record.wasCompromised) {
                    Text("COMPROMISED", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("TARGET: ${record.targetName}", color = Silver, fontSize = 11.sp)
            Text("PACKETS CAPTURED: ${record.packets.size}", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PacketItem(packet: NeuralTrafficAnalyzer.Packet) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(0.5.dp, if (packet.isSuspicious) Color.Red.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(4.dp))
            .background(if (packet.isSuspicious) Color.Red.copy(alpha = 0.05f) else Color.Transparent)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(packet.protocol, color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.width(8.dp))
            Text(packet.source, color = Platinum, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Icon(Icons.Default.ArrowForward, null, tint = Silver, modifier = Modifier.size(10.dp).padding(horizontal = 4.dp))
            Text(packet.destination, color = if (packet.isSuspicious) Color.Red else Platinum, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        if (packet.isSuspicious) {
            Text("!! THREAT: ${packet.threatReason}", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
