package com.example.rabit.ui.lockdown

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockdownScreen(viewModel: LockdownViewModel, onBack: () -> Unit) {
    val isActive by viewModel.isLockdownActive.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    val isConnected = connState is HidDeviceManager.ConnectionState.Connected
    
    val bgColor = Color(0xFF05050A)
    val warningRed = Color(0xFFFF3131)
    val accentCyan = Color(0xFF00F2FF)

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("INPUT INTERLOCK", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Background Alert Glow
            if (isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(warningRed.copy(alpha = 0.15f * pulseAlpha), Color.Transparent),
                                radius = 2000f
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Connection Status
                ConnectionBadge(isConnected = isConnected)

                Spacer(modifier = Modifier.weight(1f))

                // Main Interlock Button
                InterlockButton(
                    isActive = isActive,
                    isConnected = isConnected,
                    onClick = { viewModel.toggleLockdown() }
                )

                Text(
                    text = if (isActive) "SATURATION ACTIVE" else "IDLE READY",
                    color = if (isActive) warningRed else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Tactical Actions
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "SYSTEM LOCK MACROS",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TacticalActionButton(
                            label = "MAC LOCK",
                            icon = Icons.Default.Computer,
                            color = Color.White,
                            onClick = { viewModel.triggerMacLock() },
                            modifier = Modifier.weight(1f),
                            enabled = isConnected
                        )
                        TacticalActionButton(
                            label = "WIN LOCK",
                            icon = Icons.Default.GridOn,
                            color = accentCyan,
                            onClick = { viewModel.triggerWindowsLock() },
                            modifier = Modifier.weight(1f),
                            enabled = isConnected
                        )
                    }
                }
                
                Text(
                    "Note: HID interlock spams conflicting mouse/keyboard reports to disrupt functional usage on the target device.",
                    fontSize = 9.sp,
                    color = Color.Gray.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun InterlockButton(isActive: Boolean, isConnected: Boolean, onClick: () -> Unit) {
    val targetColor = if (isActive) Color(0xFFFF3131) else Color(0xFF00F2FF)
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .clip(CircleShape)
            .clickable(enabled = isConnected, onClick = onClick)
    ) {
        // Outer rings
        Box(modifier = Modifier.size(200.dp).border(1.dp, targetColor.copy(alpha = 0.2f), CircleShape))
        Box(modifier = Modifier.size(180.dp).border(2.dp, targetColor.copy(alpha = 0.4f), CircleShape))
        
        // Inner core
        Surface(
            color = targetColor.copy(alpha = if (isActive) 0.2f else 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(140.dp),
            shadowElevation = if (isActive) 20.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isActive) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = targetColor,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        if (!isConnected) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                Text("NO DEVICE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun TacticalActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, if (enabled) color.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.1f)),
        modifier = modifier.height(56.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = if (enabled) color else Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (enabled) Color.White else Color.Gray)
        }
    }
}

@Composable
fun ConnectionBadge(isConnected: Boolean) {
    val color = if (isConnected) Color(0xFF39FF14) else Color(0xFFFF3131)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.1f))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(
            if (isConnected) "HID CHANNEL READY" else "DISCONNECTED",
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}

// End of file
