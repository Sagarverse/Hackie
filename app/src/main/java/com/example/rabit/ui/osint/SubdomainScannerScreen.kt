package com.example.rabit.ui.osint

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TravelExplore
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
fun SubdomainScannerScreen(viewModel: SubdomainScannerViewModel, onBack: () -> Unit) {
    val results by viewModel.results.collectAsState()
    val scannerState by viewModel.scannerState.collectAsState()
    var domainInput by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SUBDOMAIN SCANNER", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("DNS ENUMERATION LAB", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (scannerState is ScannerState.Scanning) viewModel.stopScan()
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Platinum)
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
            // Input Header
            Surface(
                color = Surface1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = domainInput,
                            onValueChange = { domainInput = it.trim() },
                            placeholder = { Text("target.com", color = Silver.copy(alpha = 0.5f)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Platinum,
                                unfocusedTextColor = Platinum,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        if (scannerState is ScannerState.Scanning) {
                            IconButton(
                                onClick = { viewModel.stopScan() },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Default.Stop, null, tint = Color.Red)
                            }
                        } else {
                            IconButton(
                                onClick = { viewModel.startScan(domainInput) },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(AccentBlue, RoundedCornerShape(12.dp)),
                                enabled = domainInput.isNotBlank() && domainInput.contains(".")
                            ) {
                                Icon(Icons.Default.TravelExplore, null, tint = Obsidian)
                            }
                        }
                    }

                    if (scannerState is ScannerState.Scanning) {
                        val scanState = scannerState as ScannerState.Scanning
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("SCANNING:", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(scanState.currentTarget, color = Platinum, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                            Text("${(scanState.progress * 100).toInt()}%", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { scanState.progress },
                            modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                            color = AccentBlue,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            // Results List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (results.isEmpty() && scannerState !is ScannerState.Scanning) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                            Text("Ready to enumerate subdomains.", color = Silver.copy(alpha = 0.5f))
                        }
                    }
                }

                items(results) { result ->
                    Surface(
                        color = Color.White.copy(alpha = 0.02f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Dns, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(result.subdomain, color = Platinum, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SuccessGreen))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(result.ipAddress, color = Silver, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
