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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*

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

    Box(modifier = Modifier.fillMaxSize().background(DeepObsidian)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().alpha(0.05f).background(
                Brush.verticalGradient(listOf(MintTeal, Color.Transparent))
            ))
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "DESKTOP HELPER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            ),
                            color = Platinum
                        )
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
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                DarkSkeuoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ConnectionIndicator(isConnected)
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            text = if (isConnected) helperName.ifBlank { "Hackie System Linked" } else "System Standby",
                            color = Platinum,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                        
                        Text(
                            text = if (isConnected) "Active Connection: $helperIp" else "Awaiting Peer Connection",
                            color = if (isConnected) MintTeal else Silver.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(26.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            InfoChip(label = "IP", value = helperIp.ifBlank { "???" })
                            InfoChip(label = "PORT", value = "8765")
                        }
                    }
                }

                Text(
                    "REMOTE FILE SYSTEM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Silver.copy(alpha = 0.6f)
                    )
                )

                DarkSkeuoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Path: ${currentRemotePath.ifBlank { "/" }}",
                                color = Platinum,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.listParentRemoteFiles() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Silver.copy(alpha = 0.15f))
                            ) {
                                Text("LEVEL UP", color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.listRemoteFiles(currentRemotePath) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Graphite)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("REFRESH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MintTeal, contentColor = DeepObsidian)
                        ) {
                            Icon(Icons.Default.Upload, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("PUSH FILE TO HELPER", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        }

                        HorizontalDivider(color = Silver.copy(alpha = 0.05f))

                        if (helperRemoteFiles.isEmpty()) {
                            Text(
                                "No remote files visible. Connect and refresh to browse.",
                                color = Silver.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                helperRemoteFiles.take(8).forEach { file ->
                                    RemoteFileItem(file) {
                                        if (file.isDirectory) viewModel.listRemoteFiles(file.path)
                                    }
                                }
                                if (helperRemoteFiles.size > 8) {
                                    Text(
                                        "View more in file manager...",
                                        color = AccentBlue,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    "REMOTE SHELL CONSOLE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Silver.copy(alpha = 0.6f)
                    )
                )

                DarkSkeuoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlinedTextField(
                            value = terminalCommand,
                            onValueChange = { terminalCommand = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter shell command...", color = Silver.copy(alpha = 0.4f)) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (terminalCommand.isNotBlank()) {
                                        viewModel.runRemoteShellCommand(terminalCommand)
                                        terminalCommand = ""
                                    }
                                }) {
                                    Icon(Icons.Default.Terminal, null, tint = AccentPurple)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPurple,
                                unfocusedBorderColor = Silver.copy(alpha = 0.1f),
                                focusedContainerColor = Obsidian,
                                unfocusedContainerColor = Obsidian
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            color = Color.Black.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Silver.copy(alpha = 0.05f))
                        ) {
                            Box(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = terminalOutput.ifBlank { "> Ready for command input..." },
                                    color = Color(0xFF32D74B),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                )
                            }
                        }
                    }
                }

                Text(
                    "PAIRING & DISCOVERY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Silver.copy(alpha = 0.6f)
                    )
                )

                DarkSkeuoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lan, null, tint = WarningYellow, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Manual Discovery", color = Platinum, fontWeight = FontWeight.Bold)
                                Text("Search for active helper on Wi-Fi", color = Silver, style = MaterialTheme.typography.labelSmall)
                            }
                            FilledTonalButton(
                                onClick = { viewModel.discoverHelperOnLocalWifi() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("SCAN", fontWeight = FontWeight.Black)
                            }
                        }

                        HorizontalDivider(color = Silver.copy(alpha = 0.05f))

                        SettingToggle(
                            icon = Icons.Default.ContentPaste,
                            title = "Sync Clipboard",
                            subtitle = "Auto-share copy buffers",
                            checked = clipboardSyncEnabled,
                            onCheckedChange = {
                                clipboardSyncEnabled = it
                                viewModel.setClipboardSyncState(it)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DarkSkeuoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        color = CardBackground,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            content()
        }
    }
}

@Composable
private fun ConnectionIndicator(active: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        if (active) {
            val infiniteTransition = rememberInfiniteTransition(label = "glow")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.1f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000),
                    repeatMode = RepeatMode.Reverse
                ), label = "glowAlpha"
            )
            Surface(
                modifier = Modifier.size(80.dp),
                color = SuccessGreen.copy(alpha = glowAlpha),
                shape = CircleShape
            ) {}
        }
        
        Surface(
            modifier = Modifier.size(64.dp),
            color = if (active) MintTeal.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, if (active) MintTeal else Color.White.copy(alpha = 0.1f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (active) Icons.Default.Devices else Icons.Default.CloudOff,
                    null,
                    tint = if (active) MintTeal else Silver,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun RemoteFileItem(file: com.example.rabit.ui.HelperRemoteFile, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                null,
                tint = if (file.isDirectory) AccentBlue else Silver,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                file.name,
                color = Platinum,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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

