package com.example.rabit.ui.helper

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.AccentPurple
import com.example.rabit.ui.theme.Graphite
import com.example.rabit.ui.theme.Obsidian
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver
import com.example.rabit.ui.theme.SoftGrey
import com.example.rabit.ui.theme.SuccessGreen
import com.example.rabit.ui.theme.WarningYellow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HelperScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val isConnected by viewModel.isHelperConnected.collectAsState()
    val helperName by viewModel.helperDeviceName.collectAsState()
    val helperBaseUrl by viewModel.helperBaseUrl.collectAsState()
    val helperConnectionStatus by viewModel.helperConnectionStatus.collectAsState()
    val helperAutoConnectStatus by viewModel.helperAutoConnectStatus.collectAsState()
    val helperLastAutoDiscoverAt by viewModel.helperLastAutoDiscoverAt.collectAsState()
    val helperIp by viewModel.helperDeviceIp.collectAsState()
    val helperMac by viewModel.helperDeviceMac.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val currentRemotePath by viewModel.currentRemotePath.collectAsState()
    val helperRemoteFiles by viewModel.helperRemoteFiles.collectAsState()
    var helperUrlInput by remember(helperBaseUrl) { mutableStateOf(helperBaseUrl) }
    var terminalCommand by remember { mutableStateOf("") }
    var clipboardSyncEnabled by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.sendFileToHelper(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.ensurePhoneHelperReceiverRunning()
        viewModel.fetchHelperDeviceDetails()
        viewModel.setClipboardSyncState(clipboardSyncEnabled)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Obsidian
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF0B0F14), Color(0xFF0F1520), Color(0xFF0B0F14))
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Graphite)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1C5CFF), Color(0xFF1037A8))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isConnected) Icons.Filled.Devices else Icons.Filled.Lan,
                                contentDescription = null,
                                tint = Platinum
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isConnected) "Connected to ${helperName.ifBlank { "Desktop Helper" }}" else "Waiting for helper connection",
                                color = Platinum,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = helperConnectionStatus,
                                color = Silver,
                                fontSize = 12.sp
                            )
                        }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(label = "Target", value = helperName.ifBlank { "Unknown" })
                        InfoChip(label = "IP", value = helperIp)
                        InfoChip(label = "MAC", value = helperMac)
                        InfoChip(label = "Last check", value = helperLastAutoDiscoverAt)
                    }

                    Text(
                        text = helperAutoConnectStatus,
                        color = Silver,
                        fontSize = 12.sp
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Graphite)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle(icon = Icons.Filled.Folder, title = "Remote Files")
                    Text("Path: $currentRemotePath", color = Silver, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { viewModel.listParentRemoteFiles() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Up")
                        }
                        FilledTonalButton(
                            onClick = { viewModel.listRemoteFiles(currentRemotePath) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Refresh")
                        }
                    }
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Send File to Helper", color = Platinum, fontWeight = FontWeight.Bold)
                    }
                    if (helperRemoteFiles.isEmpty()) {
                        Text("No files loaded yet. Tap Refresh to load remote files.", color = Silver, fontSize = 12.sp)
                    } else {
                        helperRemoteFiles.take(20).forEach { file ->
                            OutlinedButton(
                                onClick = {
                                    if (file.isDirectory) {
                                        viewModel.listRemoteFiles(file.path)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = if (file.isDirectory) "📁 ${file.name}" else "📄 ${file.name}",
                                        color = Platinum,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (file.isDirectory) "Open" else "File",
                                        color = Silver,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Graphite)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(icon = Icons.Filled.Terminal, title = "Terminal")
                    Text(
                        text = "Run a shell command on the connected helper device.",
                        color = Silver,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = terminalCommand,
                        onValueChange = { terminalCommand = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("logcat -d", color = Silver) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Obsidian,
                            unfocusedContainerColor = Obsidian,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum,
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = SoftGrey
                        )
                    )
                    Button(
                        onClick = {
                            if (terminalCommand.isNotBlank()) {
                                viewModel.runRemoteShellCommand(terminalCommand)
                                terminalCommand = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Icon(Icons.Filled.Terminal, contentDescription = null, tint = Platinum)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Command", color = Platinum, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp, max = 220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Obsidian)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = if (terminalOutput.isBlank()) "Waiting for terminal output..." else terminalOutput,
                            color = SuccessGreen,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Graphite)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionTitle(icon = Icons.Filled.Lan, title = "Connection")
                    Text(
                        text = "Local Wi-Fi is preferred. Use a public IP or DDNS URL only when you need internet-direct mode.",
                        color = Silver,
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
                            unfocusedBorderColor = SoftGrey
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilledTonalButton(
                            onClick = { viewModel.discoverHelperOnLocalWifi() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF17263F),
                                contentColor = Platinum
                            )
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan LAN", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                viewModel.setHelperBaseUrl(helperUrlInput)
                                viewModel.fetchHelperDeviceDetails()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null, tint = Obsidian)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect", color = Obsidian, fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            helperUrlInput = ""
                            viewModel.resetHelperConnectionAndRescan()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningYellow)
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset + Rescan", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { viewModel.fetchHelperDeviceDetails() }) {
                        Text("Refresh health", color = AccentBlue)
                    }
                    if (helperBaseUrl.isNotBlank()) {
                        Text(
                            text = "Active endpoint: $helperBaseUrl",
                            color = Silver,
                            fontSize = 12.sp
                        )
                    }
                }
            }


            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Graphite)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(icon = Icons.Filled.ContentPaste, title = "Clipboard")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clipboard sync", color = Platinum, fontWeight = FontWeight.SemiBold)
                            Text("Send and receive clipboard between phone and helper", color = Silver, fontSize = 12.sp)
                        }
                        Switch(
                            checked = clipboardSyncEnabled,
                            onCheckedChange = {
                                clipboardSyncEnabled = it
                                viewModel.setClipboardSyncState(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Obsidian,
                                checkedTrackColor = SuccessGreen,
                                uncheckedThumbColor = Platinum,
                                uncheckedTrackColor = SoftGrey
                            )
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = AccentBlue)
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF101723))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label.uppercase(), color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

