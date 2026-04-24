package com.example.rabit.ui.webhub

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@Composable
fun WebHubScreen(
    viewModel: WebHubViewModel,
    onBack: () -> Unit
) {
    val serverState by viewModel.serverState.collectAsState()
    val selectedUri by viewModel.selectedFileUri.collectAsState()
    
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.selectFile(it) }
    }

    Scaffold(
        containerColor = ChatSurface,
        topBar = {
            com.example.rabit.ui.assistant.PremiumChatTopBar(
                modelName = "WEB HUB",
                isThinking = serverState is ServerState.Starting,
                connectionState = com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Disconnected,
                onLeftPanelClick = onBack,
                onRightPanelClick = {},
                onClearChat = {},
                onNewChat = {},
                onExportChat = {},
                onSettingsClick = {}
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Local Preview Server",
                    color = Platinum,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Serve HTML to your local network",
                    color = Silver,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            // File Selection Card
            Surface(
                onClick = { fileLauncher.launch("text/html") },
                color = Graphite.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedUri != null) AccentBlue.copy(alpha = 0.5f) else BorderColor.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (selectedUri != null) Icons.Default.CheckCircle else Icons.Default.FileOpen,
                        contentDescription = null,
                        tint = if (selectedUri != null) AccentBlue else Silver
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            if (selectedUri != null) "File Selected" else "Choose HTML File",
                            color = Platinum,
                            fontWeight = FontWeight.SemiBold
                        )
                        selectedUri?.let {
                            Text(
                                "Ready to host",
                                color = AccentBlue,
                                fontSize = 12.sp
                            )
                        } ?: Text(
                            "Select a file to host on Wi-Fi",
                            color = Silver,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Server Controls
            AnimatedVisibility(visible = selectedUri != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val isRunning = serverState is ServerState.Running
                    
                    Button(
                        onClick = { if (isRunning) viewModel.stopServer() else viewModel.startServer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) Color.Red.copy(alpha = 0.1f) else AccentBlue.copy(alpha = 0.1f),
                            contentColor = if (isRunning) Color.Red else AccentBlue
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text(if (isRunning) "STOP SERVER" else "START SERVER", fontWeight = FontWeight.Bold)
                    }

                    if (serverState is ServerState.Error) {
                        Text((serverState as ServerState.Error).message, color = Color.Red, fontSize = 12.sp)
                    }
                }
            }

            // Running State (QR Code)
            AnimatedVisibility(
                visible = serverState is ServerState.Running,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val runningState = serverState as? ServerState.Running
                runningState?.let { state ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Graphite.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                            .padding(24.dp)
                    ) {
                        Text("SERVER ACTIVE", color = AccentBlue, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.url, color = Platinum, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        val qrBitmap = remember(state.url) { QrCodeGenerator.generate(state.url) }
                        qrBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(12.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Scan with another device to view",
                            color = Silver,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
