package com.example.rabit.ui.osint

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun OsintGhostScreen(
    viewModel: OsintGhostViewModel,
    onBack: () -> Unit
) {
    val status by viewModel.status.collectAsState()
    val logs by viewModel.searchLogs.collectAsState()
    var targetInput by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEURAL OSINT GHOST", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("DEEP-WEB TARGET PROFILING ENGINE", color = Color.Magenta, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
            if (status is OsintStatus.Idle) {
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("Target Name / Email / Handle", color = Silver) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Platinum, unfocusedTextColor = Platinum, focusedBorderColor = Color.Magenta, unfocusedBorderColor = BorderColor)
                )
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.startDeepSearch(targetInput) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta),
                    shape = RoundedCornerShape(12.dp),
                    enabled = targetInput.isNotBlank()
                ) {
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text("INITIATE DEEP SEARCH", fontWeight = FontWeight.Black)
                }
            } else {
                OsintDashboard(viewModel, status, logs)
            }
        }
    }
}

@Composable
fun OsintDashboard(
    viewModel: OsintGhostViewModel,
    status: OsintStatus,
    logs: List<String>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Status Progress
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (status is OsintStatus.Searching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Magenta)
                    } else {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = when (status) {
                            is OsintStatus.Searching -> "CRAWLING: ${status.source}"
                            is OsintStatus.Finished -> "DOSSIER GENERATED"
                            is OsintStatus.Error -> "OSINT FAILED"
                            else -> ""
                        },
                        color = Platinum,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Search Feed / Results
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black, RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))) {
            if (status is OsintStatus.Finished) {
                Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                    Text(status.dossier, color = Platinum, fontSize = 13.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.reset() }, modifier = Modifier.fillMaxWidth()) {
                        Text("NEW SEARCH")
                    }
                }
            } else if (status is OsintStatus.Error) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(status.message, color = Color.Red, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { viewModel.reset() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta)) {
                        Text("TRY AGAIN", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.padding(12.dp), reverseLayout = true) {
                    items(logs.reversed()) { log ->
                        Text("> $log", color = Color.Magenta.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
            }
        }
    }
}
