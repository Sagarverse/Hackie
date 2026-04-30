package com.example.rabit.ui.automation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun C2TunnelScreen(
    viewModel: AutomationViewModel,
    onBack: () -> Unit
) {
    val isTunnelActive by viewModel.isTunnelActive.collectAsState()
    val globalAddress by viewModel.globalC2Address.collectAsState()
    var tunnelInput by remember { mutableStateOf(globalAddress) }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("GLOBAL C2 TUNNEL", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("CROSS-NETWORK INFILTRATION", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            // --- Tunnel Status ---
            Surface(
                color = if (isTunnelActive) SuccessGreen.copy(alpha = 0.05f) else Graphite.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isTunnelActive) SuccessGreen.copy(alpha = 0.3f) else BorderColor.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Language, 
                        null, 
                        tint = if (isTunnelActive) SuccessGreen else Silver, 
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (isTunnelActive) "GLOBAL LINK ESTABLISHED" else "LOCAL ISOLATION ACTIVE",
                        color = if (isTunnelActive) SuccessGreen else Silver,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (isTunnelActive) {
                        Text(globalAddress, color = Platinum, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- Automated Ngrok Controller ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface1.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoMode, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("AUTO-TUNNEL (NGROK)", color = Platinum, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    var authToken by remember { mutableStateOf("") }
                    val ngrokStatus by viewModel.ngrokStatus.collectAsState()
                    val ngrokLog by viewModel.ngrokLog.collectAsState()

                    OutlinedTextField(
                        value = authToken,
                        onValueChange = { authToken = it },
                        label = { Text("Ngrok Auth Token", color = Silver) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Paste token from ngrok.com", color = Silver.copy(alpha = 0.3f)) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Platinum, unfocusedTextColor = Platinum),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { 
                            if (ngrokStatus == "CONNECTED") viewModel.stopAutoTunnel() 
                            else viewModel.startAutoTunnel(authToken) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ngrokStatus == "CONNECTED") Color.Red.copy(alpha = 0.7f) else AccentBlue
                        )
                    ) {
                        Text(if (ngrokStatus == "CONNECTED") "STOP AUTO-TUNNEL" else "START AUTO-TUNNEL", fontWeight = FontWeight.Black)
                    }

                    if (ngrokLog.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            LazyColumn {
                                items(ngrokLog) { log ->
                                    Text("> $log", color = SuccessGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- Manual Configuration ---
            Text("MANUAL OVERRIDE", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            
            OutlinedTextField(
                value = tunnelInput,
                onValueChange = { tunnelInput = it },
                placeholder = { Text("e.g. 0.tcp.ngrok.io:12345", color = Silver.copy(alpha = 0.3f)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("External URL", color = Silver) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Platinum, unfocusedTextColor = Platinum)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { viewModel.toggleTunnel(!isTunnelActive, tunnelInput) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Graphite)
            ) {
                Text(if (isTunnelActive) "STOP MANUAL TUNNEL" else "USE MANUAL URL", fontWeight = FontWeight.Black)
            }
        }
    }
}
