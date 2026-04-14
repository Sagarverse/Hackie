package com.example.rabit.ui.helper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.Obsidian
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.AccentBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelperScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var syncClipboard by remember { mutableStateOf(true) }
    val isConnected by viewModel.isHelperConnected.collectAsState()
    val helperName by viewModel.helperDeviceName.collectAsState()
    val helperBaseUrl by viewModel.helperBaseUrl.collectAsState()
    val helperIp by viewModel.helperDeviceIp.collectAsState()
    val helperMac by viewModel.helperDeviceMac.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    var helperUrlInput by remember(helperBaseUrl) { mutableStateOf(helperBaseUrl) }
    val scrollState = androidx.compose.foundation.rememberScrollState()
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.sendFileToHelper(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchHelperDeviceDetails()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = com.example.rabit.ui.theme.Graphite),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val icon = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Error
                val tint = if (isConnected) com.example.rabit.ui.theme.SuccessGreen else com.example.rabit.ui.theme.ErrorRed
                Icon(icon, contentDescription = "Connection Status", tint = tint, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isConnected) "Connected to Desktop" else "Waiting for Target...",
                    color = Platinum,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (helperName.isBlank()) "Unknown Helper" else helperName,
                    color = com.example.rabit.ui.theme.Silver,
                    fontSize = 13.sp
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = com.example.rabit.ui.theme.Graphite),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lan, contentDescription = null, tint = AccentBlue)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Direct P2P Connection", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Text(
                    "Local Wi-Fi is auto-preferred. For internet direct mode, enter public helper URL (IP/DDNS + port 8765).",
                    color = com.example.rabit.ui.theme.Silver,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = helperUrlInput,
                    onValueChange = { helperUrlInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("http://192.168.1.10:8765") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Obsidian,
                        unfocusedContainerColor = Obsidian,
                        focusedTextColor = Platinum,
                        unfocusedTextColor = Platinum,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = com.example.rabit.ui.theme.SoftGrey
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { viewModel.discoverHelperOnLocalWifi() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Obsidian)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan LAN", color = Obsidian, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            viewModel.setHelperBaseUrl(helperUrlInput)
                            viewModel.fetchHelperDeviceDetails()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.rabit.ui.theme.SuccessGreen)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Obsidian)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect", color = Obsidian, fontWeight = FontWeight.Bold)
                    }
                }
                if (helperBaseUrl.isNotBlank()) {
                    Text("Active endpoint: $helperBaseUrl", color = com.example.rabit.ui.theme.Silver, fontSize = 12.sp)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = com.example.rabit.ui.theme.Graphite),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Device Information", tint = com.example.rabit.ui.theme.Silver)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Active Target Info", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("IP Address", color = com.example.rabit.ui.theme.Silver, fontSize = 14.sp)
                        Text(helperIp, color = Platinum, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MAC Address", color = com.example.rabit.ui.theme.Silver, fontSize = 14.sp)
                        Text(helperMac, color = Platinum, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = com.example.rabit.ui.theme.Graphite),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Devices, contentDescription = "Device Control", tint = AccentBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Target Control", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.listRemoteFiles() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = "List Files", tint = Obsidian)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Explore Remote Files", color = Obsidian, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.pingRemoteDevice() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.rabit.ui.theme.SuccessGreen)
                    ) {
                        Icon(Icons.Default.NetworkPing, contentDescription = "Ping Device", tint = Obsidian)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ping Target", color = Obsidian, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.rabit.ui.theme.AccentPurple)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Send File", tint = Platinum)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send File to Helper", color = Platinum, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = com.example.rabit.ui.theme.Graphite),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = "Security", tint = com.example.rabit.ui.theme.ErrorRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("System Security", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.lockRemoteScreen() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.rabit.ui.theme.ErrorRed)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock Screen", tint = Platinum)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lock Remote Screen", color = Platinum, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = com.example.rabit.ui.theme.Graphite),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Browser Handoff", tint = com.example.rabit.ui.theme.WarningYellow)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Browser Handoff", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    var handoffUrl by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = handoffUrl,
                        onValueChange = { handoffUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://example.com", color = com.example.rabit.ui.theme.Silver) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Obsidian,
                            unfocusedContainerColor = Obsidian,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = com.example.rabit.ui.theme.SoftGrey
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { 
                            if (handoffUrl.isNotBlank()) {
                                viewModel.openUrlOnRemote(handoffUrl)
                                handoffUrl = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.rabit.ui.theme.WarningYellow)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send URL", tint = Obsidian)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Push URL to Desktop", color = Obsidian, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = com.example.rabit.ui.theme.Graphite),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Clipboard Sync", tint = AccentBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Clipboard Synchronization", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Real-time Sync", color = Platinum, fontWeight = FontWeight.Medium)
                            Text("Send and receive clipboard", color = com.example.rabit.ui.theme.Silver, fontSize = 12.sp)
                        }
                        Switch(
                            checked = syncClipboard,
                            onCheckedChange = { 
                                syncClipboard = it
                                viewModel.setClipboardSyncState(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Obsidian,
                                checkedTrackColor = com.example.rabit.ui.theme.SuccessGreen,
                                uncheckedThumbColor = Platinum,
                                uncheckedTrackColor = com.example.rabit.ui.theme.SoftGrey
                            )
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = com.example.rabit.ui.theme.Graphite),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = "Remote Shell", tint = com.example.rabit.ui.theme.AccentPurple)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Remote Shell Execution", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    var shellCmd by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = shellCmd,
                        onValueChange = { shellCmd = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("echo 'Hello World'", color = com.example.rabit.ui.theme.Silver) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Obsidian,
                            unfocusedContainerColor = Obsidian,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum,
                            focusedBorderColor = com.example.rabit.ui.theme.AccentPurple,
                            unfocusedBorderColor = com.example.rabit.ui.theme.SoftGrey
                        ),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                if (shellCmd.isNotBlank()) {
                                    viewModel.runRemoteShellCommand(shellCmd)
                                    shellCmd = ""
                                }
                            }) {
                                Icon(Icons.Default.Send, contentDescription = "Execute", tint = com.example.rabit.ui.theme.AccentPurple)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 250.dp)
                            .background(Obsidian, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (terminalOutput.isBlank()) "> Waiting for command output..." else "> $terminalOutput",
                            color = com.example.rabit.ui.theme.SuccessGreen,
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
