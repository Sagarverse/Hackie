package com.example.rabit.ui.automation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroOrchestratorScreen(
    viewModel: AutomationViewModel,
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val connectionState by mainViewModel.connectionState.collectAsState()
    val isConnected = connectionState is com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connected
    
    val tacticalMacros = remember {
        listOf(
            MacroDefinition(
                "System Recon", 
                Icons.Default.Radar, 
                AccentBlue, 
                "KEY(GUI+SPACE) && WAIT(400) && TEXT(Terminal) && KEY(ENTER) && WAIT(1000) && TEXT(whoami; hostname; uname -a; uptime) && KEY(ENTER)"
            ),
            MacroDefinition(
                "Exfiltrate Clipboard", 
                Icons.Default.ContentPaste, 
                AccentTeal, 
                "KEY(GUI+C) && WAIT(200) && TEXT(Clipboard Exfiltrated) && KEY(ENTER)"
            ),
            MacroDefinition(
                "Network Sweep", 
                Icons.Default.Language, 
                SuccessGreen, 
                "KEY(GUI+SPACE) && WAIT(400) && TEXT(Terminal) && KEY(ENTER) && WAIT(1000) && TEXT(ifconfig | grep inet) && KEY(ENTER)"
            ),
            MacroDefinition(
                "Clear History", 
                Icons.Default.DeleteSweep, 
                ErrorRed, 
                "TEXT(history -c; clear) && KEY(ENTER)"
            )
        )
    }

    val bypassMacros = remember {
        listOf(
            MacroDefinition(
                "Lock Bypass (Space)", 
                Icons.Default.LockOpen, 
                WarningYellow, 
                "KEY(SPACE) && WAIT(200) && KEY(SPACE)"
            ),
            MacroDefinition(
                "Force Sleep", 
                Icons.Default.PowerSettingsNew, 
                Silver, 
                "KEY(CTRL+SHIFT+POWER)"
            )
        )
    }

    val devMacros = remember {
        listOf(
            MacroDefinition(
                "New VS Code Window", 
                Icons.Default.Code, 
                AccentBlue, 
                "KEY(GUI+SPACE) && WAIT(400) && TEXT(Visual Studio Code) && KEY(ENTER) && WAIT(1500) && KEY(GUI+N)"
            ),
            MacroDefinition(
                "Git Status Check", 
                Icons.Default.Commit, 
                AccentPurple, 
                "TEXT(git status) && KEY(ENTER)"
            )
        )
    }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "MACRO ORCHESTRATOR",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text("MISSION-CRITICAL AUTOMATION", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            item {
                // Connection Status Card
                Surface(
                    color = if (isConnected) SuccessGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isConnected) SuccessGreen.copy(alpha = 0.3f) else ErrorRed.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            tint = if (isConnected) SuccessGreen else ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (isConnected) "HID LINK ACTIVE — Target: Connected" else "HID LINK DISCONNECTED — Check Pairing",
                            color = if (isConnected) SuccessGreen else ErrorRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            item {
                MacroCategory(
                    title = "TACTICAL RECON",
                    icon = Icons.Default.Security,
                    macros = tacticalMacros,
                    onMacroClick = { viewModel.executeMacro2Script(it.command) },
                    enabled = isConnected
                )
            }

            item {
                MacroCategory(
                    title = "ACCESS & CONTROL",
                    icon = Icons.Default.Lock,
                    macros = bypassMacros,
                    onMacroClick = { viewModel.executeMacro2Script(it.command) },
                    enabled = isConnected
                )
            }

            item {
                MacroCategory(
                    title = "DEVELOPER OPS",
                    icon = Icons.Default.Terminal,
                    macros = devMacros,
                    onMacroClick = { viewModel.executeMacro2Script(it.command) },
                    enabled = isConnected
                )
            }

            item {
                // Info footer
                Surface(
                    color = Graphite.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = Silver, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Macros use HID injection patterns. Ensure the target OS matches your keyboard layout settings.",
                            color = Silver,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
