package com.example.rabit.ui.qa

import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralWebAuditorScreen(
    viewModel: NeuralWebAuditorViewModel,
    onBack: () -> Unit
) {
    val status by viewModel.status.collectAsState()
    val logs by viewModel.logs.collectAsState()
    var urlInput by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEURAL WEB AUDITOR", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("AUTONOMOUS WEBSITE QUALITY ANALYSIS", color = SuccessGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
            if (status is WebAuditStatus.Idle) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Target Website URL (e.g. google.com)", color = Silver) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Platinum,
                        unfocusedTextColor = Platinum,
                        focusedBorderColor = SuccessGreen,
                        unfocusedBorderColor = BorderColor
                    )
                )
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.startAudit(if (urlInput.startsWith("http")) urlInput else "https://$urlInput") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(12.dp),
                    enabled = urlInput.isNotBlank()
                ) {
                    Icon(Icons.Default.Public, null)
                    Spacer(Modifier.width(8.dp))
                    Text("INITIATE NEURAL WEB SCAN", fontWeight = FontWeight.Black)
                }
            } else {
                WebAuditDashboard(viewModel, status, logs, onStop = { viewModel.stopAudit() })
            }
        }
    }
}

@Composable
fun WebAuditDashboard(
    viewModel: NeuralWebAuditorViewModel,
    status: WebAuditStatus,
    logs: List<QaLogEntry>,
    onStop: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Status Card
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(SuccessGreen, RoundedCornerShape(4.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (status) {
                            is WebAuditStatus.Loading -> "LOADING TARGET PAGE..."
                            is WebAuditStatus.Analyzing -> "AUDIT STEP ${status.step}: ${status.action}"
                            is WebAuditStatus.Finished -> "WEB AUDIT COMPLETE"
                            is WebAuditStatus.Error -> "AUDIT CRITICAL FAILURE"
                            else -> "IDLE"
                        },
                        color = Platinum,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Split View: Web Preview + Logs
        Box(modifier = Modifier.weight(1f)) {
            Column {
                // Mini Browser View
                Box(modifier = Modifier.weight(0.6f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = WebViewClient()
                                viewModel.setWebView(this)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Scanned Overlay
                    Box(modifier = Modifier.fillMaxSize().background(SuccessGreen.copy(alpha = 0.05f)))
                }

                Spacer(Modifier.height(12.dp))

                // Log Terminal
                Box(modifier = Modifier.weight(0.4f).fillMaxWidth().background(Color.Black, RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(logs) { log ->
                            LogItem(log)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (status is WebAuditStatus.Finished) {
            ReportViewer(status.report)
            Spacer(Modifier.height(8.dp))
        }

        if (status !is WebAuditStatus.Finished && status !is WebAuditStatus.Error) {
            Button(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("TERMINATE WEB SCAN", fontWeight = FontWeight.Bold)
            }
        }
    }
}
