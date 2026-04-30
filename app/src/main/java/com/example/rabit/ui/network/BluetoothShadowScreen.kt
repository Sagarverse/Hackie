package com.example.rabit.ui.network

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothShadowScreen(
    viewModel: BluetoothShadowViewModel,
    onBack: () -> Unit
) {
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isScanning by viewModel.isShadowScanning.collectAsState()
    val isGhosting by viewModel.isGhosting.collectAsState()
    val activeIdentity by viewModel.activeIdentity.collectAsState()
    val shadowLog by viewModel.shadowLog.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "BLUETOOTH SHADOW",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text(
                            if (isScanning) "ACTIVE RECONNAISSANCE" else "DORMANT",
                            color = if (isScanning) AccentBlue else Silver,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                    }
                },
                actions = {
                    IconButton(onClick = { if (isScanning) viewModel.stopShadowScan() else viewModel.startShadowScan() }) {
                        Icon(
                            if (isScanning) Icons.Default.Stop else Icons.Default.Radar,
                            contentDescription = "Scan",
                            tint = if (isScanning) Color.Red else AccentBlue
                        )
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
        ) {
            // ── RADAR DISPLAY ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Radar Rings
                Canvas(modifier = Modifier.size(200.dp)) {
                    drawCircle(color = BorderColor.copy(alpha = 0.2f), style = Stroke(1.dp.toPx()))
                    drawCircle(color = BorderColor.copy(alpha = 0.1f), radius = size.minDimension / 4, style = Stroke(1.dp.toPx()))
                    
                    if (isScanning) {
                        drawCircle(
                            color = AccentBlue.copy(alpha = pulseAlpha),
                            radius = (size.minDimension / 2) * pulseScale,
                            style = Stroke(2.dp.toPx())
                        )
                    }
                }
                
                // Central Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(AccentBlue.copy(alpha = 0.1f), CircleShape)
                        .border(1.dp, AccentBlue.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isGhosting) Icons.Default.Fingerprint else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isGhosting) AccentBlue else Platinum,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Discovered Targets (Floating Dots)
                discoveredDevices.forEachIndexed { index, device ->
                    val angle = (index * 45f) % 360f
                    val radius = 80.dp
                    // Simple orbital positioning logic would go here
                }

                if (isGhosting) {
                    Text(
                        "GHOST MODE: $activeIdentity",
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                        color = AccentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // ── TARGET LIST & LOGS ──
            HorizontalDivider(color = BorderColor.copy(alpha = 0.2f), thickness = 0.5.dp)

            Row(modifier = Modifier.fillMaxSize()) {
                // Left: Targets
                LazyColumn(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .drawBehind {
                            val strokeWidth = 0.5.dp.toPx()
                            drawLine(
                                color = BorderColor.copy(alpha = 0.2f),
                                start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                                strokeWidth = strokeWidth
                            )
                        },
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { SectionTitle("NEARBY TARGETS", Icons.Default.DeviceHub) }
                    
                    if (discoveredDevices.isEmpty()) {
                        item {
                            Text(
                                "No signals detected.\nInitiate scan pulse.",
                                color = Silver.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 20.dp)
                            )
                        }
                    } else {
                        items(discoveredDevices) { device ->
                            ShadowTargetItem(
                                device = device,
                                isGhosting = isGhosting && activeIdentity == device.name,
                                onShadow = { viewModel.toggleGhostMode(device.name) },
                                onLink = { viewModel.initiateShadowLink(device) }
                            )
                        }
                    }
                }

                // Right: Tactical Log
                LazyColumn(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item { SectionTitle("EVENT LOG", Icons.Default.Terminal) }
                    items(shadowLog) { log ->
                        Text(
                            log,
                            color = if (log.contains("SUCCESS") || log.contains("LINK")) AccentBlue else Silver,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(icon, null, tint = Silver, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun ShadowTargetItem(
    device: ShadowDevice,
    isGhosting: Boolean,
    onShadow: () -> Unit,
    onLink: () -> Unit
) {
    Surface(
        onClick = onLink,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isGhosting) AccentBlue.copy(alpha = 0.1f) else Graphite.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            if (isGhosting) AccentBlue.copy(alpha = 0.5f) else BorderColor.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(device.name, color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(device.address, color = Silver, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Text("${device.rssi} dBm", color = if (device.rssi > -60) Color.Green else Silver, fontSize = 10.sp)
            }
            
            if (device.vulnerability != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    device.vulnerability,
                    color = AccentBlue.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onShadow,
                    modifier = Modifier.weight(1f).height(28.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGhosting) Color.Red.copy(alpha = 0.2f) else AccentBlue.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (isGhosting) "UNMASK" else "GHOST",
                        color = if (isGhosting) Color.Red else AccentBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                
                OutlinedButton(
                    onClick = onLink,
                    modifier = Modifier.weight(1f).height(28.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Silver.copy(alpha = 0.3f))
                ) {
                    Text("PROBE", color = Platinum, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun Canvas(modifier: Modifier, onDraw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
    androidx.compose.foundation.Canvas(modifier = modifier, onDraw = onDraw)
}
