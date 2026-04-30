package com.example.rabit.ui.zero_touch

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalLabScreen(
    viewModel: SignalLabViewModel,
    onBack: () -> Unit
) {
    val pings by viewModel.pings.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    var targetInput by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SIGNAL LAB", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("GSM / SS7 STEALTH PINGING", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
            // --- Control Panel ---
            OutlinedTextField(
                value = targetInput,
                onValueChange = { targetInput = it },
                label = { Text("Target Phone Number", color = AccentBlue) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("+1 XXX XXX XXXX", color = Silver.copy(alpha = 0.3f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Platinum,
                    unfocusedTextColor = Platinum
                )
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.sendSilentSms(targetInput) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Radar, null)
                    Spacer(Modifier.width(8.dp))
                    Text("SILENT PING", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Button(
                    onClick = { /* HLR Lookup Action */ },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Graphite),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text("HLR LOOKUP", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(32.dp))

            Text("PING LOGS", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(pings) { ping ->
                    PingCard(ping)
                }
            }
        }
    }
}

@Composable
fun PingCard(ping: SignalPing) {
    Surface(
        color = Surface1.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CellTower, null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(ping.target, color = Platinum, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text("${ping.type} • ${ping.status}", color = SuccessGreen, fontSize = 10.sp)
                Text(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ping.timestamp)), color = Silver, fontSize = 9.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
        }
    }
}
