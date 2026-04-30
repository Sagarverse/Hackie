package com.example.rabit.ui.opsec

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanicTerminalScreen(
    mainViewModel: MainViewModel,
    killSwitchViewModel: KillSwitchViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "PANIC TERMINAL",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Color(0xFFE11D48)
                            )
                        )
                        Text("EMERGENCY OPSEC PROTOCOLS", color = Color(0xFFE11D48).copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
        PanicTerminalContent(mainViewModel, killSwitchViewModel, modifier = Modifier.padding(padding))
    }
}

@Composable
fun PanicTerminalContent(
    viewModel: MainViewModel,
    killSwitchViewModel: KillSwitchViewModel,
    modifier: Modifier = Modifier
) {
    val wipeResults by killSwitchViewModel.wipeResults.collectAsState()
    val isWiping by killSwitchViewModel.isWiping.collectAsState()
    val totalBytesFreed by killSwitchViewModel.totalBytesFreed.collectAsState()
    
    var showNukeConfirm by remember { mutableStateOf(false) }
    var showWipeConfirm by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFE11D48), modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                "CRITICAL SECURITY OVERRIDE",
                color = Platinum,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                letterSpacing = 1.sp
            )
            Text(
                "Actions performed here are irreversible and immediate.",
                color = Silver,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }

        // TIER 1: STEALTH
        item {
            OPSECCard(
                title = "TIER 1: STEALTH DECOY",
                description = "Instantly hide tactical UI and switch to Decoy Mode. Requires secret knock to return.",
                icon = Icons.Default.VisibilityOff,
                buttonText = "ENGAGE DECOY",
                buttonColor = Color(0xFF1E88E5),
                onClick = { viewModel.setDecoyMode(true) }
            )
        }

        // TIER 2: EVIDENCE WIPE (KILL SWITCH)
        item {
            OPSECCard(
                title = "TIER 2: EVIDENCE WIPE",
                description = "Securely erase logs, caches, and forensic data. Frees storage and destroys traces.",
                icon = Icons.Default.CleaningServices,
                buttonText = if (isWiping) "WIPING..." else "EXECUTE WIPE",
                buttonColor = Color(0xFFEAB308),
                isLoading = isWiping,
                onClick = { showWipeConfirm = true }
            )
        }

        if (wipeResults.isNotEmpty()) {
            item {
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("WIPE REPORT: ${killSwitchViewModel.formatBytes(totalBytesFreed)} FREED", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        wipeResults.forEach { result ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(Icons.Default.Check, null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(result.category, color = Platinum, fontSize = 11.sp)
                                Spacer(Modifier.weight(1f))
                                Text("${result.filesDeleted} files", color = Silver, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // TIER 3: NUCLEAR OPTION
        item {
            OPSECCard(
                title = "TIER 3: NUCLEAR RESET",
                description = "Wipe ALL settings, API keys, and custom payloads. Factory reset Hackie Pro environment.",
                icon = Icons.Default.DeleteForever,
                buttonText = "NUKE SYSTEM",
                buttonColor = Color(0xFFE11D48),
                onClick = { showNukeConfirm = true }
            )
        }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            containerColor = Surface0,
            title = { Text("CONFIRM EVIDENCE WIPE", color = Color(0xFFEAB308), fontWeight = FontWeight.Black) },
            text = { Text("Forensic logs, cached payloads, and network traces will be permanently destroyed. Continue?", color = Platinum) },
            confirmButton = {
                Button(onClick = { showWipeConfirm = false; killSwitchViewModel.executeKillSwitch() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308))) {
                    Text("EXECUTE", color = Color.Black, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) { Text("ABORT", color = Silver) }
            }
        )
    }

    if (showNukeConfirm) {
        AlertDialog(
            onDismissRequest = { showNukeConfirm = false },
            containerColor = Surface0,
            title = { Text("INITIATE SELF DESTRUCT?", color = Color(0xFFE11D48), fontWeight = FontWeight.Black) },
            text = { Text("This will permanently delete ALL data, including keys and macros. The app will restart in Decoy Mode.", color = Platinum) },
            confirmButton = {
                Button(onClick = { showNukeConfirm = false; viewModel.nukeData() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))) {
                    Text("NUKE", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNukeConfirm = false }) { Text("CANCEL", color = Silver) }
            }
        )
    }
}

@Composable
fun OPSECCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonText: String,
    buttonColor: Color,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = buttonColor, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Text(title, color = Platinum, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(description, color = Silver, fontSize = 11.sp, lineHeight = 16.sp)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = if (buttonColor == Color.White) Color.Black else Color.White, strokeWidth = 2.dp)
                } else {
                    Text(buttonText, fontWeight = FontWeight.Black, color = if (buttonColor == Color(0xFFEAB308)) Color.Black else Color.White)
                }
            }
        }
    }
}
