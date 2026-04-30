package com.example.rabit.ui.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingTraceScreen(viewModel: PingTraceViewModel, onBack: () -> Unit) {
    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PING & TRACEROUTE", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("NETWORK PATH ANALYSIS", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Platinum) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            PingTraceContent(viewModel)
        }
    }
}

@Composable
fun PingTraceContent(viewModel: PingTraceViewModel) {
    val pingResults by viewModel.pingResults.collectAsState()
    val traceResults by viewModel.traceResults.collectAsState()
    val state by viewModel.state.collectAsState()
    var targetHost by remember { mutableStateOf("") }
    var isPingMode by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = Surface1, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = targetHost, onValueChange = { targetHost = it.trim() },
                    placeholder = { Text("Target host or IP", color = Silver.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Platinum, unfocusedTextColor = Platinum, focusedBorderColor = AccentBlue, unfocusedBorderColor = BorderColor),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace)
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isRunning = state is PingTraceState.Pinging || state is PingTraceState.Tracing
                    Button(
                        onClick = { isPingMode = true; viewModel.startPing(targetHost) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isRunning && targetHost.isNotBlank()
                    ) {
                        Icon(Icons.Default.NetworkPing, null, tint = Obsidian, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PING", fontWeight = FontWeight.Black, color = Obsidian, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { isPingMode = false; viewModel.startTrace(targetHost) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isRunning && targetHost.isNotBlank()
                    ) {
                        Icon(Icons.Default.Route, null, tint = Obsidian, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("TRACEROUTE", fontWeight = FontWeight.Black, color = Obsidian, fontSize = 12.sp)
                    }
                    if (isRunning) {
                        IconButton(onClick = { viewModel.stop() }, modifier = Modifier.size(44.dp).background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(8.dp))) {
                            Icon(Icons.Default.Stop, null, tint = Color.Red)
                        }
                    }
                }

                if (state is PingTraceState.Pinging) {
                    val s = state as PingTraceState.Pinging
                    Spacer(Modifier.height(8.dp))
                    Text("Sent: ${s.sent}  |  Received: ${s.received}  |  Loss: ${if (s.sent > 0) ((s.sent - s.received) * 100 / s.sent) else 0}%", color = Silver, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                if (state is PingTraceState.Tracing) {
                    Spacer(Modifier.height(8.dp))
                    Text("Tracing hop ${(state as PingTraceState.Tracing).currentHop}...", color = Color(0xFFF59E0B), fontSize = 11.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isPingMode) {
                items(pingResults) { result ->
                    Surface(color = Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (result.reachable) SuccessGreen else Color.Red))
                            Spacer(Modifier.width(12.dp))
                            Text(result.host, color = Platinum, fontSize = 13.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                            Text(
                                if (result.reachable) "${result.latencyMs}ms" else "timeout",
                                color = if (result.reachable) SuccessGreen else Color.Red,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            } else {
                items(traceResults) { hop ->
                    Surface(color = Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${hop.hopNumber}", color = AccentBlue, fontWeight = FontWeight.Black, fontSize = 14.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(28.dp))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (hop.latencyMs >= 0) Color(0xFFF59E0B) else Color.Red))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(hop.hostName, color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                if (hop.ip != hop.hostName) Text(hop.ip, color = Silver, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            if (hop.latencyMs >= 0) {
                                Text("${hop.latencyMs}ms", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}
