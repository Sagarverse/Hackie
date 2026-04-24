package com.example.rabit.ui.opsec

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanicTerminalScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var showNukeConfirm by remember { mutableStateOf(false) }
    
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
                        Text("ANTI-FORENSICS & DECOY", color = Silver, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFE11D48), modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(24.dp))
            
            Text(
                "CRITICAL OPSEC CONTROLS",
                color = Platinum,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Use these controls in emergency situations. Actions performed here are irreversible.",
                color = Silver,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(Modifier.height(48.dp))

            // Decoy Mode Button
            Button(
                onClick = { viewModel.setDecoyMode(true) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.VisibilityOff, null, tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Text("ENGAGE DECOY MODE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            
            Spacer(Modifier.height(12.dp))
            Text(
                "Instantly hides tactical UI. Secret Knock: Type '1337' in note title and long-press header.",
                color = Silver.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // Nuke Button
            Button(
                onClick = { showNukeConfirm = true },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteForever, null, tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Text("NUKE ALL PAYLOADS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
        
        if (showNukeConfirm) {
            AlertDialog(
                onDismissRequest = { showNukeConfirm = false },
                containerColor = Graphite,
                title = { Text("INITIATE SELF DESTRUCT?", color = Color(0xFFE11D48), fontWeight = FontWeight.Black) },
                text = { 
                    Text("This will permanently delete all custom macros, API keys, and tactical history. The app will immediately restart in Decoy Mode.", color = Platinum) 
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.nukeData()
                            showNukeConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                    ) {
                        Text("NUKE", fontWeight = FontWeight.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNukeConfirm = false }) {
                        Text("CANCEL", color = Silver)
                    }
                }
            )
        }
    }
}
