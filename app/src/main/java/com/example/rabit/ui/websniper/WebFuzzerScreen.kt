package com.example.rabit.ui.websniper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Storage
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebFuzzerScreen(
    viewModel: WebSniperViewModel,
    onBack: () -> Unit
) {
    var targetUrl by remember { mutableStateOf("http://192.168.1.100/vulnerable.php?id=") }
    var extractionMode by remember { mutableStateOf(false) }
    val isFuzzing by viewModel.isFuzzing.collectAsState()
    val logs by viewModel.fuzzerLogs.collectAsState()
    val results by viewModel.fuzzerResults.collectAsState()
    
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("rabit_prefs", android.content.Context.MODE_PRIVATE)

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "NEURAL FUZZER",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text("DYNAMIC AI PAYLOADS", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                value = targetUrl,
                onValueChange = { targetUrl = it },
                label = { Text("Target URL Parameter") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Platinum,
                    unfocusedTextColor = Platinum,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderColor
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = if (extractionMode) Color(0xFFFF3131) else Silver, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Database Extraction", color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Focus on dumping table data (SQLi)", color = Silver.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }
                Switch(
                    checked = extractionMode,
                    onCheckedChange = { extractionMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFFF3131),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }

            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (isFuzzing) viewModel.stopFuzzing()
                    else {
                        val apiKey = prefs.getString("gemini_api_key", "") ?: ""
                        if (extractionMode) {
                            viewModel.startSqlInjection(targetUrl, apiKey)
                        } else {
                            viewModel.startFuzzing(targetUrl, apiKey)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFuzzing) Color(0xFFE11D48) else if (extractionMode) Color(0xFFFF3131) else AccentBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (isFuzzing) Icons.Default.Stop else Icons.Default.AutoAwesome, null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isFuzzing) "ABORT ATTACK" else if (extractionMode) "INITIALIZE SQLi DUMP" else "GENERATE & INJECT PAYLOADS", 
                    color = Color.Black, 
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text("EXECUTION LOGS", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                LazyColumn(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                    items(logs) { log ->
                        val color = when {
                            log.startsWith("[+]") -> SuccessGreen
                            log.startsWith("[-]") -> Color.Red
                            log.startsWith("[!]") -> Color(0xFFE11D48)
                            else -> Silver
                        }
                        Text(
                            text = log,
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text("VULNERABILITIES FOUND: ${results.count { it.reflectionFound || it.dbErrorFound }}", color = Color(0xFFE11D48), fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}
