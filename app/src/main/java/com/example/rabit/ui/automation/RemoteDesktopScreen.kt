package com.example.rabit.ui.automation

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteDesktopScreen(viewModel: AutomationViewModel, onBack: () -> Unit) {
    val screenBase64 by viewModel.remoteScreenBase64.collectAsState()
    val isStreaming by viewModel.isRemoteDesktopActive.collectAsState()
    val isConnected by viewModel.reverseShellConnected.collectAsState()

    val bitmap = remember(screenBase64) {
        if (screenBase64 == null) null
        else try {
            val bytes = Base64.decode(screenBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) { null }
    }

    DisposableEffect(Unit) {
        if (isConnected) viewModel.startRemoteDesktop()
        onDispose { viewModel.stopRemoteDesktop() }
    }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("REMOTE DESKTOP", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text(if (isStreaming) "LIVE STREAM ACTIVE" else "CONNECTING...", color = if (isStreaming) SuccessGreen else AccentGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Platinum) } },
                actions = {
                    IconButton(onClick = { viewModel.startRemoteDesktop() }) { Icon(Icons.Default.Refresh, null, tint = AccentTeal) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Screen Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val xPercent = offset.x / size.width
                            val yPercent = offset.y / size.height
                            viewModel.sendRemoteClick(xPercent, yPercent)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Remote Screen",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentTeal, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("WAITING FOR STREAM...", color = Silver.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // HUD Overlay
                Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            "FPS: ~0.3 | COMPRESSION: 80%",
                            color = SuccessGreen,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Control Bar
            Surface(
                color = Surface0,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlItem(Icons.Default.Keyboard, "Keyboard") {}
                    ControlItem(Icons.Default.Mouse, "Click Mode") {}
                    
                    FloatingActionButton(
                        onClick = { if (isStreaming) viewModel.stopRemoteDesktop() else viewModel.startRemoteDesktop() },
                        containerColor = if (isStreaming) Color.Red else SuccessGreen,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(if (isStreaming) Icons.Default.Stop else Icons.Default.PlayArrow, null)
                    }
                    
                    ControlItem(Icons.Default.Screenshot, "Capture") {}
                    ControlItem(Icons.Default.Settings, "Config") {}
                }
            }
        }
    }
}

@Composable
private fun ControlItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Icon(icon, null, tint = Silver, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = Silver, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
