package com.example.rabit.ui.automation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.domain.model.RemoteProcess
import com.example.rabit.ui.helper.HelperViewModel
import com.example.rabit.ui.theme.*

@Composable
fun ProcessManagerContent(
    viewModel: HelperViewModel
) {
    val processes by viewModel.remoteProcesses.collectAsState()
    val isRefreshing by viewModel.isRefreshingProcesses.collectAsState()
    val connected by viewModel.sshConnected.collectAsState()

    var showKillConfirm by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(connected) {
        if (connected) {
            viewModel.fetchRemoteProcesses()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Kill Process",
                    color = Platinum,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    if (connected) "Connected to Mac" else "SSH Not Connected",
                    color = if (connected) AccentBlue else Color.Red,
                    fontSize = 12.sp
                )
            }
            
            IconButton(
                onClick = { viewModel.fetchRemoteProcesses() },
                enabled = connected && !isRefreshing
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Platinum
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!connected) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Please connect SSH first in Automation tab.", color = Silver)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(processes) { proc ->
                    ProcessItem(
                        process = proc,
                        onKill = { showKillConfirm = proc.pid }
                    )
                }
            }
        }
    }

    if (showKillConfirm != null) {
        AlertDialog(
            onDismissRequest = { showKillConfirm = null },
            containerColor = Graphite,
            title = { Text("Force Kill?", color = Platinum) },
            text = { Text("Are you sure you want to kill process PID ${showKillConfirm}?", color = Silver) },
            confirmButton = {
                TextButton(onClick = {
                    showKillConfirm?.let { viewModel.killRemoteProcess(it) }
                    showKillConfirm = null
                }) {
                    Text("KILL", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKillConfirm = null }) {
                    Text("CANCEL", color = Platinum)
                }
            }
        )
    }
}


@Composable
fun ProcessItem(
    process: RemoteProcess,
    onKill: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        modifier = Modifier.fillMaxWidth(),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    process.command.ifBlank { "Unknown Process" }.substringAfterLast("/"),
                    color = Platinum,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("PID: ${process.pid}", color = Silver, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("CPU: ${process.cpu}%", color = if (process.cpu > 50) Color.Red else Color.Green, fontSize = 11.sp)
                    Text("MEM: ${process.mem}%", color = AccentBlue, fontSize = 11.sp)
                }
            }
            
            IconButton(onClick = onKill) {
                Icon(
                    Icons.Default.StopCircle,
                    contentDescription = "Kill",
                    tint = Color.Red.copy(alpha = 0.8f)
                )
            }
        }
    }
}
