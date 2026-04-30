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
fun PortScannerScreen(viewModel: PortScannerViewModel, onBack: () -> Unit) {
    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PORT SCANNER", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("TCP SCAN + BANNER GRAB", color = SuccessGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Platinum) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            PortScannerContent(viewModel)
        }
    }
}

@Composable
fun PortScannerContent(viewModel: PortScannerViewModel) {
    val results by viewModel.results.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    var targetHost by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Input
        Surface(color = Surface1, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = targetHost, onValueChange = { targetHost = it.trim() },
                    placeholder = { Text("Target IP or hostname", color = Silver.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Platinum, unfocusedTextColor = Platinum, focusedBorderColor = SuccessGreen, unfocusedBorderColor = BorderColor),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace)
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.quickScan(targetHost) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(8.dp),
                        enabled = scanState !is PortScanState.Scanning && targetHost.isNotBlank()
                    ) {
                        Icon(Icons.Default.FlashOn, null, tint = Obsidian, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("QUICK SCAN", fontWeight = FontWeight.Black, color = Obsidian, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { viewModel.startScan(targetHost) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(8.dp),
                        enabled = scanState !is PortScanState.Scanning && targetHost.isNotBlank()
                    ) {
                        Icon(Icons.Default.Radar, null, tint = Obsidian, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("FULL SCAN", fontWeight = FontWeight.Black, color = Obsidian, fontSize = 12.sp)
                    }
                    if (scanState is PortScanState.Scanning) {
                        IconButton(onClick = { viewModel.stopScan() }, modifier = Modifier.size(44.dp).background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(8.dp))) {
                            Icon(Icons.Default.Stop, null, tint = Color.Red)
                        }
                    }
                }

                if (scanState is PortScanState.Scanning) {
                    val s = scanState as PortScanState.Scanning
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Scanning port ${s.currentPort}", color = Silver, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text("${(s.progress * 100).toInt()}%", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape), color = SuccessGreen, trackColor = Color.White.copy(alpha = 0.1f))
                }

                if (scanState is PortScanState.Completed) {
                    Spacer(Modifier.height(8.dp))
                    Text("${results.size} open port(s) found", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Results
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(results) { result ->
                Surface(
                    color = Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row {
                                Text("${result.port}", color = Platinum, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                                Spacer(Modifier.width(8.dp))
                                Text(result.service, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            if (result.banner.isNotBlank()) {
                                Text(result.banner, color = Silver, fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                            }
                        }
                        Text(result.state, color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
