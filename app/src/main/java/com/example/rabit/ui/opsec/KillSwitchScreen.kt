package com.example.rabit.ui.opsec

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KillSwitchScreen(viewModel: KillSwitchViewModel, onBack: () -> Unit) {
    val wipeResults by viewModel.wipeResults.collectAsState()
    val isWiping by viewModel.isWiping.collectAsState()
    val totalBytesFreed by viewModel.totalBytesFreed.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("KILL SWITCH", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Color.Red))
                        Text("ANTI-FORENSICS PROTOCOL", color = Color.Red.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Platinum) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (wipeResults.isEmpty()) {
                // Pre-wipe state
                Spacer(Modifier.weight(1f))

                Icon(Icons.Default.DeleteForever, null, tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(96.dp))
                Spacer(Modifier.height(24.dp))
                Text("EMERGENCY EVIDENCE WIPE", color = Platinum, fontSize = 18.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    "This will permanently destroy all cached data, operation logs, cracked hashes, PCAP captures, steganography files, forensic databases, and sensitive preferences stored by Hackie Pro.",
                    color = Silver, fontSize = 12.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text("THIS ACTION CANNOT BE UNDONE.", color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isWiping
                ) {
                    if (isWiping) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Platinum, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.DeleteForever, null)
                        Spacer(Modifier.width(12.dp))
                        Text("EXECUTE KILL SWITCH", fontWeight = FontWeight.Black)
                    }
                }
            } else {
                // Post-wipe results
                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("EVIDENCE DESTROYED", color = SuccessGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("${viewModel.formatBytes(totalBytesFreed)} freed", color = Silver, fontSize = 14.sp)
                Spacer(Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(wipeResults) { result ->
                        Surface(
                            color = Color.White.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (result.filesDeleted > 0) Color.Red else Silver))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(result.category, color = Platinum, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${result.filesDeleted} files • ${viewModel.formatBytes(result.bytesFreed)}", color = Silver, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                Icon(Icons.Default.Check, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onBack, modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Platinum),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Text("RETURN TO BASE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("CONFIRM KILL SWITCH", color = Color.Red, fontWeight = FontWeight.Black) },
            text = { Text("All forensic data will be permanently destroyed. Proceed?", color = Platinum) },
            confirmButton = {
                TextButton(onClick = { showConfirmDialog = false; viewModel.executeKillSwitch() }) {
                    Text("EXECUTE", color = Color.Red, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("ABORT", color = Silver)
                }
            },
            containerColor = Surface0
        )
    }
}
