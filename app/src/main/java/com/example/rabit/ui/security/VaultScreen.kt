package com.example.rabit.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

/**
 * Hidden Forensic Vault View.
 * Accessible via secret decoy code (9999).
 * Shows both Audits and Traffic Captures.
 */
@Composable
fun VaultScreen(
    auditorViewModel: SecurityAuditorViewModel,
    trafficViewModel: TrafficAnalyzerViewModel,
    onLock: () -> Unit
) {
    val auditHistory by auditorViewModel.auditHistory.collectAsState()
    val trafficHistory by trafficViewModel.trafficHistory.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("SECURITY AUDITS", "TRAFFIC LOGS")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Obsidian
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("FORENSIC VAULT", color = Platinum, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp)
                    Text("UNIFIED CLOAKED REPOSITORY", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onLock) {
                    Icon(Icons.Default.Close, null, tint = Silver)
                }
            }

            Spacer(Modifier.height(24.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = SuccessGreen,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = SuccessGreen
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

            Spacer(Modifier.height(24.dp))

            if (selectedTab == 0) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(auditHistory) { record ->
                        VaultAuditCard(record)
                    }
                    if (auditHistory.isEmpty()) {
                        item { EmptyVaultState("No security audits found.") }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(trafficHistory) { record ->
                        VaultTrafficCard(record)
                    }
                    if (trafficHistory.isEmpty()) {
                        item { EmptyVaultState("No traffic logs found.") }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyVaultState(message: String) {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(message, color = TextTertiary, fontSize = 13.sp)
    }
}

@Composable
fun VaultAuditCard(record: com.example.rabit.data.security.TacticalStorageManager.AuditRecord) {
    Surface(
        color = Surface1.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.timestamp)),
                    color = Platinum,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("TARGET: ${record.targetName}", color = Silver, fontSize = 11.sp)
            Text("FINDINGS: ${record.findings.size}", color = if (record.findings.isEmpty()) SuccessGreen else Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun VaultTrafficCard(record: com.example.rabit.data.security.TrafficStorageManager.TrafficRecord) {
    Surface(
        color = Surface1.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (record.wasCompromised) Color.Red else BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.timestamp)),
                    color = Platinum,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.weight(1f))
                if (record.wasCompromised) {
                    Text("COMPROMISED", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("TARGET: ${record.targetName}", color = Silver, fontSize = 11.sp)
            Text("PACKETS: ${record.packets.size}", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
