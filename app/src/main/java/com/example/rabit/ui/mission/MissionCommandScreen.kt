package com.example.rabit.ui.mission

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionCommandScreen(
    viewModel: MissionCommandViewModel,
    onNavigate: (String) -> Unit
) {
    val status by viewModel.missionStatus.collectAsState()
    var subjectInput by remember { mutableStateOf(status.subjectId) }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("MISSION COMMAND", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- Target Identification ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface1.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Adjust, "Target", tint = AccentBlue, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("TARGET IDENTIFICATION", color = Platinum, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = subjectInput,
                            onValueChange = { 
                                subjectInput = it
                                viewModel.updateSubjectId(it)
                            },
                            label = { Text("Subject Phone Number", color = Silver) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("+1 XXX XXX XXXX", color = Silver.copy(alpha = 0.3f)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Platinum, unfocusedTextColor = Platinum)
                        )
                    }
                }
            }

            // --- System Readiness ---
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusIndicatorCard("C2 TUNNEL", status.c2Status, SuccessGreen, Modifier.weight(1f))
                    StatusIndicatorCard("PHISH PORTAL", if (status.phishArmed) "ARMED" else "READY", AccentBlue, Modifier.weight(1f))
                }
            }

            // --- Rapid Deployment Hub ---
            item {
                Text("RAPID DEPLOYMENT HUB", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HubButton("PHISH PORTAL", "Social Engineering Delivery", Icons.Default.Radar, AccentBlue) { onNavigate("phish_portal") }
                    HubButton("SESSION CLONER", "Identity Hijack Lab", Icons.Default.Fingerprint, SuccessGreen) { onNavigate("session_cloner") }
                    HubButton("SIGNAL LAB", "GSM Stealth Pinging", Icons.Default.CellTower, Platinum) { onNavigate("signal_lab") }
                    HubButton("MEDIA EXPLOIT", "Zero-Click Asset Forge", Icons.Default.Science, Color.Red) { onNavigate("media_exploit") }
                }
            }

            // --- Live Intelligence Feed ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("INTELLIGENCE FEED", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${status.capturedLootCount} data points exfiltrated", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = { onNavigate("loot_viewer") },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen.copy(alpha = 0.1f), contentColor = SuccessGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("VIEW LOOT", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicatorCard(label: String, status: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = Surface1.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = Silver, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(8.dp))
                Text(status, color = color, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun HubButton(label: String, subLabel: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Surface1.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(label, color = Platinum, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(subLabel, color = Silver, fontSize = 10.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Silver, modifier = Modifier.size(20.dp))
        }
    }
}
