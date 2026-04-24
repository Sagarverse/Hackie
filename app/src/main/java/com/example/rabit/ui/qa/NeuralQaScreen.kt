package com.example.rabit.ui.qa

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralQaScreen(
    viewModel: NeuralQaViewModel,
    onBack: () -> Unit
) {
    val status by viewModel.status.collectAsState()
    val logs by viewModel.logs.collectAsState()
    var packageName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEURAL QA AUDITOR", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("AI-DRIVEN FUNCTIONAL TESTING", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
            if (status is QaAuditStatus.Idle) {
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Target Package Name (e.g. com.whatsapp)", color = Silver) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Platinum,
                        unfocusedTextColor = Platinum,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor
                    )
                )
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.startAudit(packageName) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp),
                    enabled = packageName.isNotBlank()
                ) {
                    Icon(Icons.Default.BugReport, null)
                    Spacer(Modifier.width(8.dp))
                    Text("START NEURAL AUDIT", fontWeight = FontWeight.Black)
                }
            } else {
                AuditDashboard(status, logs, onStop = { viewModel.stopAudit() })
            }
        }
    }
}

@Composable
fun AuditDashboard(status: QaAuditStatus, logs: List<QaLogEntry>, onStop: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Status Card
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (status) {
                        is QaAuditStatus.Crashed -> Color.Red
                        is QaAuditStatus.Finished -> SuccessGreen
                        is QaAuditStatus.Error -> Color.Red
                        else -> AccentBlue
                    }
                    Box(modifier = Modifier.size(8.dp).background(statusColor, RoundedCornerShape(4.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (status) {
                            is QaAuditStatus.Initializing -> "INITIALIZING PROBES..."
                            is QaAuditStatus.Scanning -> "STEP ${status.step}: ${status.action}"
                            is QaAuditStatus.Crashed -> "CRASH DETECTED!"
                            is QaAuditStatus.Finished -> "AUDIT COMPLETE"
                            is QaAuditStatus.Error -> "AUDIT FAILED"
                            else -> "IDLE"
                        },
                        color = Platinum,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                
                if (status is QaAuditStatus.Scanning) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = AccentBlue,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Log Terminal
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black, RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))) {
            LazyColumn(modifier = Modifier.padding(12.dp)) {
                items(logs) { log ->
                    LogItem(log)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (status is QaAuditStatus.Finished) {
            ReportViewer(status.report)
        } else if (status is QaAuditStatus.Crashed) {
            CrashViewer(status.stackTrace)
        }

        if (status !is QaAuditStatus.Finished && status !is QaAuditStatus.Error) {
            Button(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("TERMINATE AUDIT", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LogItem(log: QaLogEntry) {
    val color = when (log.type) {
        LogType.AI -> Color(0xFFD8B4FE)
        LogType.ACTION -> AccentBlue
        LogType.CRASH -> Color.Red
        else -> Silver
    }
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("[${log.tag}]", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.width(8.dp))
        Text(log.message, color = Platinum, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun ReportViewer(report: String) {
    Surface(
        color = SuccessGreen.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text("DETAILED FINDINGS", color = SuccessGreen, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(report, color = Platinum, fontSize = 11.sp)
        }
    }
}

@Composable
fun CrashViewer(stackTrace: String) {
    Surface(
        color = Color.Red.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text("CRASH STACKTRACE", color = Color.Red, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(stackTrace.take(500), color = Platinum, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
