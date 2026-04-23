package com.example.rabit.ui.security

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.security.NeuralAuditEngine
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityAuditorScreen(
    viewModel: SecurityAuditorViewModel,
    onBack: () -> Unit
) {
    val findings by viewModel.findings.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val progress by viewModel.scanProgress.collectAsState()

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEURAL AUDITOR", style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Black))
                        Text("ETHICAL COMPLIANCE ENGINE", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Surface0)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            
            // --- Tactical Briefing ---
            var showBriefing by remember { mutableStateOf(true) }
            if (showBriefing) {
                Surface(
                    color = SuccessGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("SECURITY BRIEFING", color = SuccessGreen, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { showBriefing = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Silver, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This tool looks for 'open windows' and weaknesses in any phone you connect. It tells you if the phone is safe or needs fixing.",
                            color = Platinum, fontSize = 13.sp, lineHeight = 18.sp
                        )
                    }
                }
            }

            var selectedTab by remember { mutableStateOf(0) }
            val tabs = listOf("LIVE AUDIT", "TACTICAL HISTORY")

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AccentBlue,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AccentBlue
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

            Spacer(Modifier.height(20.dp))

            if (selectedTab == 0) {
                LiveAuditContent(viewModel)
            } else {
                HistoryContent(viewModel)
            }
        }
    }
}

@Composable
fun LiveAuditContent(viewModel: SecurityAuditorViewModel) {
    val findings by viewModel.findings.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val progress by viewModel.scanProgress.collectAsState()

    Column {
        // --- Audit Control ---
        Surface(
            color = Surface1,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, null, tint = if (findings.isEmpty()) Silver else SuccessGreen, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Target Security Posture", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(if (isScanning) "Neural analysis in progress..." else "${findings.size} Security Findings Detected", color = TextSecondary, fontSize = 12.sp)
                    }
                    
                    if (findings.isNotEmpty() && !isScanning) {
                        var showReportDialog by remember { mutableStateOf(false) }
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Default.Description, "Report", tint = AccentBlue)
                        }

                        if (showReportDialog) {
                            val reportText = viewModel.getReportText()
                            val context = androidx.compose.ui.platform.LocalContext.current
                            AlertDialog(
                                onDismissRequest = { showReportDialog = false },
                                containerColor = Surface1,
                                title = { Text("TACTICAL SECURITY REPORT", color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.Black) },
                                text = {
                                    Surface(
                                        color = Color.Black,
                                        modifier = Modifier.height(300.dp).fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        LazyColumn(modifier = Modifier.padding(12.dp)) {
                                            item {
                                                Text(reportText, color = SuccessGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = { 
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_TEXT, reportText)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Exfiltrate Report"))
                                        showReportDialog = false 
                                    }, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) {
                                        Text("SHARE / EXFILTRATE")
                                    }
                                }
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                if (isScanning) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = AccentBlue,
                        trackColor = BorderColor
                    )
                } else {
                    Button(
                        onClick = { viewModel.runFullAudit() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Radar, null)
                        Spacer(Modifier.width(8.dp))
                        Text("COMMENCE FULL SECURITY AUDIT")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Findings List ---
        Text("VULNERABILITY REPORT", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(findings) { finding ->
                FindingCard(finding)
            }
            
            if (findings.isEmpty() && !isScanning) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No findings yet. Run a scan to identify risks.", color = TextTertiary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryContent(viewModel: SecurityAuditorViewModel) {
    val history by viewModel.auditHistory.collectAsState()

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("PAST TACTICAL AUDITS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            TextButton(onClick = { viewModel.clearTacticalHistory() }) {
                Text("CLEAR ALL", color = Color.Red, fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(history) { record ->
                HistoryRecordCard(record)
            }
            
            if (history.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No history found. Complete an audit to save it here.", color = TextTertiary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryRecordCard(record: com.example.rabit.data.security.TacticalStorageManager.AuditRecord) {
    Surface(
        color = Surface1,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.timestamp)),
                    color = Platinum,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("TARGET: ${record.targetName}", color = Silver, fontSize = 11.sp)
            Text("FINDINGS: ${record.findings.size}", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FindingCard(finding: NeuralAuditEngine.Finding) {
    val severityColor = when (finding.severity) {
        NeuralAuditEngine.Severity.CRITICAL -> Color(0xFFEF4444)
        NeuralAuditEngine.Severity.HIGH -> Color(0xFFF97316)
        NeuralAuditEngine.Severity.MEDIUM -> Color(0xFFEAB308)
        NeuralAuditEngine.Severity.LOW -> Color(0xFF3B82F6)
        NeuralAuditEngine.Severity.INFO -> SuccessGreen
    }

    Surface(
        color = Surface1,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(severityColor, RoundedCornerShape(2.dp))
                        .padding(top = 4.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(finding.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(finding.category.name, color = severityColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Text(finding.severity.name, color = severityColor.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(12.dp))
            Text(finding.description, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            
            Spacer(Modifier.height(16.dp))
            
            // Remediation Box
            Surface(
                color = Color.Black.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Handyman, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("REMEDIATION STEPS", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(finding.remediation, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}
