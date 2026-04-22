package com.example.rabit.ui.network

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothMirrorScreen(
    viewModel: BluetoothMirrorViewModel,
    onBack: () -> Unit
) {
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val clonedProfile by viewModel.clonedProfile.collectAsState()
    val isMirroring by viewModel.isMirroring.collectAsState()
    val logs by viewModel.mirrorLog.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshPairedDevices()
    }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEURAL MIRROR LAB", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("IDENTITY CLONING & HIJACKING", color = Color.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Platinum)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.reset() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Stage 1: Clone Selection
            Text("STEP 1: SELECT SOURCE DEVICE TO CLONE", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Black, RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))) {
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(pairedDevices) { device ->
                        Surface(
                            color = if (clonedProfile?.address == device.address) Color.Yellow.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth().clickable { viewModel.cloneDevice(device) },
                            border = if (clonedProfile?.address == device.address) androidx.compose.foundation.BorderStroke(1.dp, Color.Yellow) else null
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SettingsInputComponent, null, tint = if (clonedProfile?.address == device.address) Color.Yellow else Silver, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(device.name ?: "Unknown", color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(device.address, color = Silver, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Stage 2: Mirror Dashboard
            if (clonedProfile != null) {
                val isDeauthing by viewModel.isDeauthing.collectAsState()
                
                Text("STEP 2: INITIATE TACTICAL MIRROR", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isMirroring) Color.Yellow.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, null, tint = Color.Yellow)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CLONED IDENTITY: ${clonedProfile?.name}", color = Platinum, fontWeight = FontWeight.Black)
                                Text("PROFILE: Audio_Peripheral_v2", color = Color.Yellow, fontSize = 9.sp)
                            }
                            
                            // Deauth Button
                            IconButton(
                                onClick = { viewModel.sendDeauthPulse() },
                                enabled = !isDeauthing,
                                modifier = Modifier.background(if (isDeauthing) Color.Red else Color.Transparent, CircleShape)
                            ) {
                                Icon(Icons.Default.WifiTetheringError, null, tint = if (isDeauthing) Color.Black else Color.Red)
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.initiateMirrorHijack("MACBOOK_TARGET_AUTO") },
                                modifier = Modifier.weight(1f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Platinum),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("SHADOW LINK", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                            
                            Button(
                                onClick = { viewModel.triggerPayloadHook() },
                                modifier = Modifier.weight(1f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Bolt, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("PAYLOAD HOOK", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Stage 3: Real-time Terminal
            Text("TACTICAL LOG", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black, RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))) {
                LazyColumn(modifier = Modifier.padding(12.dp)) {
                    items(logs) { log ->
                        Text(log, color = Color.Yellow.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
            }
        }
    }
}
