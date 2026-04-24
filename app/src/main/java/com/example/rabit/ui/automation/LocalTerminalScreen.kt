package com.example.rabit.ui.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.BorderColor
import com.example.rabit.ui.theme.Graphite
import com.example.rabit.ui.theme.Obsidian
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver
import com.example.rabit.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalTerminalScreen(
    viewModel: LocalTerminalViewModel,
    apiKey: String,
    onBack: () -> Unit
) {
    val lines by viewModel.terminalLines.collectAsState()
    val input by viewModel.inputText.collectAsState()
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
                            "LOCAL SHELL",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text("TERMUX-LIKE EMULATOR", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Platinum)
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
    // --- Tactical Briefing ---
    var showBriefing by remember { mutableStateOf(true) }
    if (showBriefing) {
        Surface(
            color = AccentBlue.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("TACTICAL BRIEFING", color = AccentBlue, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showBriefing = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Silver, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "This is your own private Linux computer inside Hackie. You can install tools just like a pro hacker.",
                    color = Platinum, fontSize = 13.sp, lineHeight = 18.sp
                )
                Spacer(Modifier.height(8.dp))
                Text("HOW TO USE:", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("• Type 'hpkg install cmatrix' to get the falling green matrix effect.", color = Silver, fontSize = 12.sp)
                Text("• Type 'hpkg install busybox' to add 100+ new commands.", color = Silver, fontSize = 12.sp)
                Text("• Use 'clear' to clean the screen.", color = Silver, fontSize = 12.sp)
            }
        }
    }

    val screenBuffer by viewModel.screenBuffer.collectAsState()
    val isInteractiveMode by viewModel.isInteractiveMode.collectAsStateWithLifecycle()
    
    Surface(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        color = Color.Black,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f))
    ) {
        if (isInteractiveMode) {
            // High-Definition Matrix Grid
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                screenBuffer.forEach { row ->
                    Text(
                        text = String(row),
                        color = SuccessGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        letterSpacing = 0.sp
                    )
                }
            }
        } else {
            // Standard Tactical Log
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Top
            ) {
                items(lines) { line ->
                    Text(
                        line,
                        color = if (line.startsWith("$ ")) Platinum else SuccessGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
    
    // --- Terminal Control Bar ---
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val ctrlActive by viewModel.ctrlActive.collectAsStateWithLifecycle()
        val altActive by viewModel.altActive.collectAsStateWithLifecycle()
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ControlChip("CTRL", active = ctrlActive, onClick = { viewModel.toggleCtrl() })
            ControlChip("ALT", active = altActive, onClick = { viewModel.toggleAlt() })
            ControlChip("TAB", active = false, onClick = { viewModel.sendTab() })
            ControlChip("STOP", active = false, onClick = { viewModel.stopExecution() })
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { viewModel.sendArrow("up") }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowUpward, null, tint = Silver, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { viewModel.sendArrow("down") }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowDownward, null, tint = Silver, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { viewModel.sendArrow("left") }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowBack, null, tint = Silver, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { viewModel.sendArrow("right") }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowForward, null, tint = Silver, modifier = Modifier.size(16.dp))
            }
        }
    }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Quick Actions Row ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val actions = listOf("ls", "pwd", "whoami", "clear")
                actions.forEach { action ->
                    AssistChip(
                        onClick = { viewModel.sendCommand(action) },
                        label = { Text(action, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Graphite.copy(alpha = 0.5f),
                            labelColor = Silver
                        ),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
                    )
                }
            }

            // --- Input Row ---
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { viewModel.onInputChange(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("$ ", color = Silver.copy(alpha = 0.4f), fontSize = 14.sp, fontFamily = FontFamily.Monospace) },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Graphite.copy(alpha = 0.5f),
                        focusedContainerColor = Graphite.copy(alpha = 0.5f),
                        unfocusedBorderColor = BorderColor.copy(alpha = 0.3f),
                        focusedBorderColor = AccentBlue,
                        cursorColor = AccentBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Platinum, fontFamily = FontFamily.Monospace),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (input.isNotBlank()) {
                            viewModel.sendCommand(input)
                        }
                    })
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                val isGeneratingPrompt by viewModel.isGeneratingPrompt.collectAsState()
                
                IconButton(
                    onClick = {
                        if (input.isNotBlank() && !isGeneratingPrompt) {
                            viewModel.generateCommandFromPrompt(input, apiKey)
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(AccentBlue.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    if (isGeneratingPrompt) {
                        CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Generate", tint = AccentBlue)
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            viewModel.sendCommand(input)
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

@Composable
fun ControlChip(label: String, active: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (active) AccentBlue.copy(alpha = 0.3f) else Graphite.copy(alpha = 0.5f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (active) AccentBlue else BorderColor.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = if (active) AccentBlue else Platinum,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
