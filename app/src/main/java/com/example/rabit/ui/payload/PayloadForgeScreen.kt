package com.example.rabit.ui.payload

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayloadForgeScreen(
    viewModel: PayloadForgeViewModel,
    onBack: () -> Unit,
    onExecuteHid: (String) -> Unit,
    onDeployAdb: (String) -> Unit
) {
    val payloadCode by viewModel.payloadCode.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()

    var targetOs by remember { mutableStateOf("Linux/Android") }
    var goal by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("Bash") }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEURAL PAYLOAD FORGE", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("AI-ASSISTANT EXPLOIT GENERATION", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
            // Configuration Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("TARGET PARAMETERS", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ForgeDropdown("OS: $targetOs", listOf("Linux/Android", "Windows", "macOS", "iOS", "Universal"), Modifier.weight(1f)) { targetOs = it }
                        ForgeDropdown("Format: $language", listOf("Bash", "PowerShell", "Python", "DuckyScript", "C++"), Modifier.weight(1f)) { language = it }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = goal,
                        onValueChange = { goal = it },
                        placeholder = { Text("Objective (e.g. Gather all WiFi passwords)", color = Silver.copy(alpha = 0.5f), fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor
                        ),
                        textStyle = TextStyle(fontSize = 14.sp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action Row
            Button(
                onClick = { viewModel.generatePayload(targetOs, goal, language) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(8.dp),
                enabled = !isGenerating && goal.isNotBlank()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Platinum, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Bolt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("FORGE PAYLOAD", fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Code Editor
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black, RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))) {
                BasicTextField(
                    value = payloadCode,
                    onValueChange = { viewModel.updateCode(it) },
                    modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
                    textStyle = TextStyle(color = Platinum, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp),
                    cursorBrush = SolidColor(AccentBlue)
                )
                
                if (payloadCode.isEmpty() && !isGenerating) {
                    Text("Awaiting generation sequence...", color = Silver.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.Center), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Deployment Options
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onExecuteHid(payloadCode) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Keyboard, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("VIA HID", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = { onDeployAdb(payloadCode) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Terminal, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("VIA ADB", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    
    if (error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("FORGE ERROR", color = Color.Red, fontWeight = FontWeight.Black) },
            text = { Text(error ?: "", color = Platinum) },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } },
            containerColor = Obsidian
        )
    }
}

@Composable
fun ForgeDropdown(label: String, options: List<String>, modifier: Modifier, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Platinum),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Text(label, fontSize = 11.sp, maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Surface0) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option, color = Platinum) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
