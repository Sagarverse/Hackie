package com.example.rabit.ui.network

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkAuditorScreen(
    viewModel: NetworkAuditorViewModel,
    portScannerViewModel: PortScannerViewModel,
    pingTraceViewModel: PingTraceViewModel,
    onBack: () -> Unit
) {
    var currentSubFeature by remember { mutableStateOf("auditor") } // "auditor", "scanner", "ping"
    val accentColor = Color(0xFF00F2FF)
    val scannerAccentColor = SuccessGreen
    val pingAccentColor = Color(0xFFF59E0B)
    val bgColor = Color(0xFF05050A)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentSubFeature) {
                            "auditor" -> "NETWORK AUDITOR"
                            "scanner" -> "PORT SCANNER"
                            else -> "PING & TRACEROUTE"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                when (currentSubFeature) {
                    "auditor" -> {
                        NetworkAuditorContent(viewModel, accentColor)
                    }
                    "scanner" -> {
                        PortScannerContent(portScannerViewModel)
                    }
                    "ping" -> {
                        PingTraceContent(pingTraceViewModel)
                    }
                }
            }

            // Right-side Mini Sidebar
            Surface(
                color = Color.White.copy(alpha = 0.03f),
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    MiniSidebarIcon(
                        icon = Icons.Default.Hub,
                        isSelected = currentSubFeature == "auditor",
                        accentColor = accentColor,
                        onClick = { currentSubFeature = "auditor" }
                    )
                    MiniSidebarIcon(
                        icon = Icons.Default.Radar,
                        isSelected = currentSubFeature == "scanner",
                        accentColor = scannerAccentColor,
                        onClick = { currentSubFeature = "scanner" }
                    )
                    MiniSidebarIcon(
                        icon = Icons.Default.NetworkPing,
                        isSelected = currentSubFeature == "ping",
                        accentColor = pingAccentColor,
                        onClick = { currentSubFeature = "ping" }
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniSidebarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                null,
                tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .background(accentColor, CircleShape)
                )
            }
        }
    }
}

@Composable
fun NetworkAuditorContent(viewModel: NetworkAuditorViewModel, accentColor: Color) {
    val isScanning by viewModel.isScanning.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val progress by viewModel.scanProgress.collectAsState()
    val aiResult by viewModel.aiAnalysisResult.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Scanning Status Bar
            if (isScanning || progress > 0f) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = accentColor,
                    trackColor = Color.White.copy(alpha = 0.05f)
                )
            }

            if (devices.isEmpty() && !isScanning) {
                EmptyState(onScan = { viewModel.startScan() })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isScanning) "SCANNING INFRASTRUCTURE..." else "LAN TOPOLOGY MAPPED",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (!isScanning) {
                                TextButton(onClick = { viewModel.startScan() }) {
                                    Text("RESCAN", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                    items(devices) { device ->
                        DeviceCard(device)
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
        
        // --- Floating Action Button (Neural Analyst) ---
        if (devices.isNotEmpty() && !isScanning) {
            FloatingActionButton(
                onClick = { viewModel.reviewTopologyWithAi() },
                containerColor = Color(0xFFBC13FE),
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.AutoAwesome, "Neural Analyst")
                }
            }
        }

        // --- AI Analysis Result Board ---
        if (aiResult != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                color = Color(0xFF1A1A2E).copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFBC13FE).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFBC13FE), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("NEURAL ANALYST INSIGHTS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearAiAnalysis() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = aiResult ?: "",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceCard(device: NetworkAuditorViewModel.NetworkDevice) {
    val accentColor = Color(0xFF00F2FF)
    
    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            device.services.any { it.contains("SSH") } -> Icons.Default.Terminal
                            device.services.any { it.contains("HTTP") } -> Icons.Default.Language
                            device.services.any { it.contains("AirPlay") } -> Icons.Default.Cast
                            else -> Icons.Default.Dns
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = device.hostname, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = device.ip, fontSize = 12.sp, color = accentColor, fontFamily = FontFamily.Monospace)
                        Text(text = "•", fontSize = 12.sp, color = Color.Gray)
                        Text(text = device.macAddress, fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.Gray
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("INFERRED USER", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(text = device.userName, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("MANUFACTURER", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(text = device.manufacturer, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
            
            if (device.services.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("OPEN PORTS / SERVICES", fontSize = 9.sp, color = Color(0xFFBC13FE), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    device.services.forEach { service ->
                        ServiceTag(service)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceTag(name: String) {
    Surface(
        color = Color(0xFFBC13FE).copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, Color(0xFFBC13FE).copy(alpha = 0.3f))
    ) {
        Text(
            text = name,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFBC13FE),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EmptyState(onScan: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.3f))
            Spacer(Modifier.height(16.dp))
            Text("Ready for Network Audit", color = Color.Gray)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onScan,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F2FF), contentColor = Color.Black)
            ) {
                Text("START SCAN", fontWeight = FontWeight.Bold)
            }
        }
    }
}
