package com.example.rabit.ui.network

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkReconScreen(
    networkAuditorViewModel: NetworkAuditorViewModel,
    portScannerViewModel: PortScannerViewModel,
    pingTraceViewModel: PingTraceViewModel,
    bleViewModel: BleAuditorViewModel,
    wifiViewModel: WifiAttackerViewModel,
    bluetoothShadowViewModel: BluetoothShadowViewModel,
    apiKey: String,
    onBack: () -> Unit
) {
    var currentSubFeature by remember { mutableStateOf("auditor") } // "auditor", "scanner", "ping", "wifi", "bluetooth"
    val accentColor = Color(0xFF00F2FF)
    val scannerAccentColor = SuccessGreen
    val pingAccentColor = Color(0xFFF59E0B)
    val wifiAccentColor = Color(0xFFEAB308)
    val btAccentColor = AccentBlue
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
                            "ping" -> "PING & TRACEROUTE"
                            "wifi" -> "WIRELESS AUDITOR"
                            "bluetooth" -> "BLUETOOTH SHADOW"
                            else -> "NETWORK RECON"
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
                    "auditor" -> NetworkAuditorContent(networkAuditorViewModel, accentColor)
                    "scanner" -> PortScannerContent(portScannerViewModel)
                    "ping" -> PingTraceContent(pingTraceViewModel)
                    "wifi" -> WirelessAuditorContent(wifiViewModel, bleViewModel, apiKey)
                    "bluetooth" -> BluetoothShadowContent(bluetoothShadowViewModel)
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
                    MiniSidebarIcon(Icons.Default.Hub, currentSubFeature == "auditor", accentColor) { currentSubFeature = "auditor" }
                    MiniSidebarIcon(Icons.Default.Radar, currentSubFeature == "scanner", scannerAccentColor) { currentSubFeature = "scanner" }
                    MiniSidebarIcon(Icons.Default.NetworkPing, currentSubFeature == "ping", pingAccentColor) { currentSubFeature = "ping" }
                    MiniSidebarIcon(Icons.Default.Wifi, currentSubFeature == "wifi", wifiAccentColor) { currentSubFeature = "wifi" }
                    MiniSidebarIcon(Icons.Default.Bluetooth, currentSubFeature == "bluetooth", btAccentColor) { currentSubFeature = "bluetooth" }
                    
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
            Icon(icon, null, tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.3f), modifier = Modifier.size(24.dp))
            if (isSelected) {
                Box(modifier = Modifier.size(4.dp).align(Alignment.CenterEnd).padding(end = 4.dp).background(accentColor, CircleShape))
            }
        }
    }
}
