package com.example.rabit.ui.websniper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
fun DirectoryScannerScreen(
    viewModel: WebSniperViewModel,
    onBack: () -> Unit
) {
    var targetDomain by remember { mutableStateOf("192.168.1.100") }
    val isScanning by viewModel.isScanningDirs.collectAsState()
    val progress by viewModel.dirProgress.collectAsState()
    val results by viewModel.dirResults.collectAsState()

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "DIR ENUMERATOR",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text("HIGH-SPEED ASYNC DISCOVERY", color = SuccessGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            OutlinedTextField(
                value = targetDomain,
                onValueChange = { targetDomain = it },
                label = { Text("Target Domain/IP") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Platinum,
                    unfocusedTextColor = Platinum,
                    focusedBorderColor = SuccessGreen,
                    unfocusedBorderColor = BorderColor
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (isScanning) viewModel.stopDirScanner()
                    else viewModel.startDirScanner(targetDomain, 10)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) Color(0xFFE11D48) else SuccessGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow, null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isScanning) "ABORT SCAN" else "LAUNCH ENUMERATION", 
                    color = Color.Black, 
                    fontWeight = FontWeight.Black
                )
            }
            
            if (isScanning || progress > 0) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = SuccessGreen,
                    trackColor = Graphite
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text("DISCOVERED PATHS", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                LazyColumn(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                    items(results) { result ->
                        val color = when (result.statusCode) {
                            200 -> SuccessGreen
                            403 -> Color(0xFFE11D48) // Red for forbidden (often juicy)
                            301, 302 -> AccentBlue
                            else -> Silver
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "/${result.path}",
                                color = Platinum,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                            Surface(
                                color = color.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, color)
                            ) {
                                Text(
                                    text = result.statusCode.toString(),
                                    color = color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
