package com.example.rabit.ui.automation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@Composable
fun MacroOrchestratorScreen(
    viewModel: MacroOrchestratorViewModel,
    onBack: () -> Unit
) {
    val isExecuting by viewModel.isExecuting.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val wordlist by viewModel.wordlistPreview.collectAsState()
    
    val wordlistLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.loadWordlist(it) }
    }

    Scaffold(
        containerColor = ChatSurface,
        topBar = {
            com.example.rabit.ui.assistant.PremiumChatTopBar(
                modelName = "MACRO ORCHESTRATOR",
                isThinking = isExecuting,
                connectionState = com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Disconnected,
                onLeftPanelClick = onBack,
                onRightPanelClick = {},
                onClearChat = {},
                onNewChat = {},
                onExportChat = {},
                onSettingsClick = {}
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Professional Automation Library",
                    color = Platinum,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Execute complex hardware interaction sequences",
                    color = Silver,
                    fontSize = 14.sp
                )
            }

            // Execution Progress
            if (isExecuting) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentBlue.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(24.dp),
                                color = AccentBlue,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Sequence in progress...", color = Platinum, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = AccentBlue,
                            trackColor = Graphite
                        )
                    }
                }
            }

            // Templates
            items(viewModel.templates) { template ->
                Surface(
                    onClick = { viewModel.executeMacro(template) },
                    enabled = !isExecuting,
                    color = Graphite.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(AccentBlue.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(template.name, color = Platinum, fontWeight = FontWeight.Bold)
                            Text(template.description, color = Silver, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Silver.copy(alpha = 0.5f))
                    }
                }
            }

            // Wordlist Validator
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Security Research: Data Validator",
                    color = Platinum,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Preview and validate research datasets/wordlists",
                    color = Silver,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    onClick = { wordlistLauncher.launch("*/*") },
                    color = Graphite.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = AccentBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Load Wordlist File", color = Platinum, fontWeight = FontWeight.SemiBold)
                        Text("Supports .txt, .csv, and generic payloads", color = Silver, fontSize = 12.sp)
                    }
                }
            }

            // Wordlist Preview
            if (wordlist.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Graphite.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text("PREVIEW (First 100 entries)", color = AccentBlue, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        wordlist.forEach { line ->
                            Text(line, color = Silver, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}
