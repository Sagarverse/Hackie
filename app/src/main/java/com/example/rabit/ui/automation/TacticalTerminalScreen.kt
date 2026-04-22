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
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalTerminalScreen(
    viewModel: com.example.rabit.ui.webbridge.WebBridgeViewModel,
    onBack: () -> Unit
) {
    val lines by viewModel.terminalLines.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "TACTICAL TERMINAL",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text("HID COMMAND & C2 FEED", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // --- Terminal Monitor ---
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f))
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    items(lines) { line ->
                        Text(
                            line,
                            color = if (line.startsWith(">")) AccentBlue else SuccessGreen.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val isPulseModeEnabled by viewModel.isPulseModeEnabled.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Waves, null, tint = if (isPulseModeEnabled) Color(0xFFBC13FE) else Silver, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PULSE MASKING", color = if (isPulseModeEnabled) Platinum else Silver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Switch(
                    checked = isPulseModeEnabled,
                    onCheckedChange = { viewModel.togglePulseMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFBC13FE),
                        uncheckedThumbColor = Silver,
                        uncheckedTrackColor = Color.Black.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Agent Launcher Row ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AccentPurple.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, AccentPurple.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("C2 Uplink Agent", color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Deploy host relay script via HID", color = Silver, fontSize = 10.sp)
                    }
                    TextButton(onClick = {
                        viewModel.sendHidCommand("GUI SPACE")
                        viewModel.sendHidCommand("DELAY 400")
                        viewModel.sendHidCommand("STRING terminal")
                        viewModel.sendHidCommand("ENTER")
                        // Additional payload logic would go here
                    }) {
                        Text("DEPLOY", color = AccentPurple, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Input Row ---
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter shell command...", color = Silver.copy(alpha = 0.4f), fontSize = 14.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Graphite.copy(alpha = 0.5f),
                        focusedContainerColor = Graphite.copy(alpha = 0.5f),
                        unfocusedBorderColor = BorderColor.copy(alpha = 0.3f),
                        focusedBorderColor = AccentBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Platinum, fontFamily = FontFamily.Monospace),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (input.isNotBlank()) {
                            viewModel.sendHidCommand(input)
                            input = ""
                        }
                    })
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            viewModel.sendHidCommand(input)
                            input = ""
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = AccentBlue)
                }
            }
        }
    }
}
