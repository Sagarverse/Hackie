package com.example.rabit.ui.webbridge

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import com.sagar.rabit.R
import com.example.rabit.data.network.RabitNetworkServer
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.components.QrCodeGenerator
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.GlassCard
import java.io.File
import android.net.Uri
import android.content.Context
import android.database.Cursor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebBridgeScreen(
    viewModel: com.example.rabit.ui.webbridge.WebBridgeViewModel,
    onBack: () -> Unit
) {
    val isRunning by viewModel.isWebBridgeRunning.collectAsState(initial = RabitNetworkServer.isRunning)
    val currentPin by viewModel.webBridgePin.collectAsState(initial = RabitNetworkServer.currentPin)
    val localIp by viewModel.localIp.collectAsState(initial = "0.0.0.0")
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val sharedFiles by viewModel.sharedFiles.collectAsState()
    val p2pEnabled by viewModel.p2pEnabled.collectAsState("false".toBoolean())
    val p2pStatus by viewModel.p2pStatus.collectAsState("Disconnected")
    val p2pPeerId by viewModel.p2pPeerId.collectAsState(initial = "")
    val localUrl = if (localIp.isNotEmpty() && localIp != "0.0.0.0")
        "http://$localIp:${RabitNetworkServer.PORT}" else "Identifying network..."
    val gatewayBaseUrl = "https://hackie-sagar.web.app"
    val p2pUrl = if (!p2pPeerId.isNullOrEmpty()) "$gatewayBaseUrl/bridge?id=$p2pPeerId" else gatewayBaseUrl

    var qrMode by remember { mutableStateOf("LAN") }
    val receivedFiles by viewModel.receivedFiles.collectAsState()
    val activeSessions by viewModel.activeSessions.collectAsState()

    LaunchedEffect(isRunning) {
        if (isRunning) {
            viewModel.refreshWebBridgeData()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { viewModel.addSharedFile(it) }
    }

    // Connect Server Providers - Now handled in WebBridgeViewModel init
    // SideEffect block removed as logic migrated to ViewModel

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Obsidian)) {
            // High-End Mesh Gradient Background
            PremiumMeshBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Status Card with Pulse
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        SophisticatedStatusIndicator(isRunning)
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            if (isRunning) "BRIDGE ONLINE" else "SYSTEM IDLE",
                            color = Platinum,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                        
                        Text(
                            if (isRunning) "Encrypted tunneling active" else "Signals ready for handshake",
                            color = Silver.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Main Toggle Button
                            Button(
                                onClick = { if (isRunning) viewModel.stopWebBridge() else viewModel.startWebBridge() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRunning) Color(0xFFE11D48).copy(alpha = 0.9f) else AccentBlue
                                ),
                                modifier = Modifier.weight(1f).height(60.dp),
                                shape = RoundedCornerShape(20.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isRunning) Icons.Default.PowerSettingsNew else Icons.Default.Launch,
                                        null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        if (isRunning) "STOP HUB" else "INIT HUB",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            
                            if (isRunning) {
                                Surface(
                                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                                    color = Platinum.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, Platinum.copy(alpha = 0.1f)),
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Add, null, tint = Platinum)
                                    }
                                }
                            }
                        }
                    }
                }

                if (isRunning) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Security & P2P Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // PIN CARD
                        GlassCard(modifier = Modifier.weight(1.2f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "SECURE PIN", 
                                    color = Silver.copy(alpha = 0.5f), 
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    currentPin,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 4.sp,
                                        color = Platinum
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Tap to regen",
                                    color = AccentBlue.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.clickable { viewModel.regenerateWebBridgePin() }
                                )
                            }
                        }

                        // P2P TOGGLE CARD
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "P2P RELAY", 
                                    color = Silver.copy(alpha = 0.5f), 
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Switch(
                                    checked = p2pEnabled,
                                    onCheckedChange = { if (it) viewModel.startP2PHosting() else viewModel.stopP2PHosting() },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = AccentBlue.copy(alpha = 0.5f),
                                        checkedThumbColor = Platinum
                                    ),
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Discovery Section
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "CONNECTION GATEWAY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = Platinum.copy(alpha = 0.4f)
                            )
                        )
                        
                        DiscoveryToggle(qrMode) { qrMode = it }
                    }

                    // QR CARD
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(240.dp),
                                color = Color.White,
                                shape = RoundedCornerShape(24.dp),
                                shadowElevation = 12.dp
                            ) {
                                val activeUrl = if (qrMode == "LAN") localUrl else p2pUrl
                                Box(modifier = Modifier.padding(20.dp)) {
                                    val qrBitmap = remember(activeUrl) {
                                        QrCodeGenerator.generateQrCode(activeUrl, 512)?.asImageBitmap()
                                    }
                                    if (qrBitmap != null) {
                                        Image(
                                            bitmap = qrBitmap, 
                                            contentDescription = "QR", 
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        CircularProgressIndicator(color = Obsidian, modifier = Modifier.align(Alignment.Center))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            SophisticatedLinkCard(
                                title = if (qrMode == "LAN") "Direct Network" else "Global Secure Relay",
                                url = if (qrMode == "LAN") localUrl else p2pUrl,
                                p2pStatus = if (qrMode == "P2P") p2pStatus else null,
                                onCopy = { 
                                    val url = if (qrMode == "LAN") localUrl else p2pUrl
                                    clipboardManager.setText(AnnotatedString(url))
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    // ─── CONNECTED DEVICES ───
                    if (activeSessions.isNotEmpty()) {
                        SectionHeader("ACTIVE SESSIONS", Icons.Default.Devices)
                        activeSessions.forEach { session ->
                            DeviceSessionItem(
                                session = session,
                                onRevoke = { viewModel.revokeActiveSession(session.token) }
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // ─── ASSET HUB ───
                    SectionHeader("ASSET MANAGEMENT", Icons.Default.FolderOpen)
                    
                    var assetTab by remember { mutableStateOf(0) } // 0: Shared (Outgoing), 1: Received (Incoming)
                    
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            listOf("SHARED" to sharedFiles.size, "RECEIVED" to receivedFiles.size).forEachIndexed { index, (label, count) ->
                                val selected = assetTab == index
                                Surface(
                                    onClick = { assetTab = index },
                                    color = if (selected) AccentBlue.copy(alpha = 0.2f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            label,
                                            color = if (selected) AccentBlue else Silver,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (count > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = if (selected) AccentBlue else Silver.copy(alpha = 0.2f),
                                                shape = CircleShape
                                            ) {
                                                Text(
                                                    count.toString(),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    color = if (selected) Platinum else Silver,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (assetTab == 0) {
                        if (sharedFiles.isEmpty()) {
                            EmptyStateCard("No files shared", "Add files to make them available on the web hub")
                        } else {
                            sharedFiles.forEach { uri ->
                                SharedAssetItem(
                                    uri = uri,
                                    context = context,
                                    onDelete = { viewModel.removeSharedFile(uri) }
                                )
                            }
                        }
                    } else {
                        if (receivedFiles.isEmpty()) {
                            EmptyStateCard("No files received", "Files uploaded from other devices will appear here")
                        } else {
                            receivedFiles.forEach { file ->
                                ReceivedAssetItem(
                                    file = file,
                                    onDelete = { viewModel.deleteReceivedFile(file) }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Platinum.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Platinum.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
private fun EmptyStateCard(title: String, sub: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.CloudQueue, null, tint = Silver.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = Silver, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(sub, color = Silver.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DeviceSessionItem(session: RabitNetworkServer.TrustedSession, onRevoke: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = AccentBlue.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (session.userAgent.contains("Mobi", true)) Icons.Default.Smartphone else Icons.Default.Laptop,
                        null,
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    parseUserAgent(session.userAgent), 
                    color = Platinum, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp
                )
                Text(
                    "ID: ${session.deviceId.takeLast(8)} • Active", 
                    color = SuccessGreen, 
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onRevoke) {
                Icon(Icons.Default.LinkOff, null, tint = Color(0xFFFF453A), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SharedAssetItem(uri: Uri, context: Context, onDelete: () -> Unit) {
    var name by remember { mutableStateOf("Loading...") }
    var size by remember { mutableStateOf("") }
    
    LaunchedEffect(uri) {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            val sIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nIdx != -1) name = cursor.getString(nIdx)
                if (sIdx != -1) size = formatFileSize(cursor.getLong(sIdx))
            }
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CloudUpload, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Platinum, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(size, color = Silver.copy(alpha = 0.6f), fontSize = 11.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Silver, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ReceivedAssetItem(file: File, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FileDownload, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name.removePrefix("Hackie_"), color = Platinum, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatFileSize(file.length()), color = Silver.copy(alpha = 0.6f), fontSize = 11.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Silver, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun parseUserAgent(ua: String): String {
    return when {
        ua.contains("Chrome") -> "Chrome on " + (if(ua.contains("Mac")) "Mac" else "Windows")
        ua.contains("Safari") && !ua.contains("Chrome") -> "Safari on Mac"
        ua.contains("Firefox") -> "Firefox Browser"
        ua.contains("HackieHelper") -> "Helper Desktop App"
        else -> "Web Client"
    }
}

private fun formatFileSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    return if (mb > 1) String.format("%.1f MB", mb) else String.format("%.1f KB", kb)
}

@Composable
private fun PremiumMeshBackground() {
    val infiniteTransition = rememberInfiniteTransition()
    val dx1 by infiniteTransition.animateFloat(
        initialValue = -200f, targetValue = 400f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse)
    )
    val dy1 by infiniteTransition.animateFloat(
        initialValue = -200f, targetValue = 400f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().alpha(0.15f).background(
            Brush.radialGradient(
                colors = listOf(AccentBlue, Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(dx1, dy1),
                radius = 1200f
            )
        ))
        Box(modifier = Modifier.fillMaxSize().alpha(0.1f).background(
            Brush.radialGradient(
                colors = listOf(SuccessGreen.copy(alpha = 0.8f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(1000f - dx1, 1200f - dy1),
                radius = 1000f
            )
        ))
    }
}



@Composable
private fun SophisticatedStatusIndicator(active: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart)
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart)
    )

    Box(contentAlignment = Alignment.Center) {
        if (active) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                    .alpha(pulseAlpha)
                    .background(SuccessGreen, CircleShape)
            )
        }
        
        Surface(
            modifier = Modifier.size(48.dp),
            color = if (active) SuccessGreen.copy(alpha = 0.2f) else Platinum.copy(alpha = 0.05f),
            shape = CircleShape,
            border = BorderStroke(2.dp, if (active) SuccessGreen else Silver.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (active) Icons.Default.WifiTethering else Icons.Default.WifiOff,
                    null,
                    tint = if (active) SuccessGreen else Silver,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DiscoveryToggle(selected: String, onSelect: (String) -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(2.dp)) {
            listOf("LAN", "P2P").forEach { mode ->
                val isSelected = selected == mode
                Surface(
                    onClick = { onSelect(mode) },
                    color = if (isSelected) AccentBlue else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        mode,
                        color = if (isSelected) Platinum else Silver,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SophisticatedLinkCard(
    title: String,
    url: String,
    p2pStatus: String?,
    onCopy: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.2f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, null, tint = Silver, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                url,
                color = Silver.copy(alpha = 0.9f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
            
            if (p2pStatus != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(
                        if (p2pStatus.contains("Connected")) SuccessGreen else Color.Yellow, 
                        CircleShape
                    ))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(p2pStatus, color = Silver.copy(alpha = 0.6f), fontSize = 10.sp)
                }
            }
        }
    }
}

