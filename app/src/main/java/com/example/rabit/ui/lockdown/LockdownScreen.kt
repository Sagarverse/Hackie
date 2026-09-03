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
import com.example.rabit.ui.components.ScreenScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockdownScreen(viewModel: LockdownViewModel, onBack: () -> Unit) {
    val isActive by viewModel.isLockdownActive.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    val isConnected = connState is HidDeviceManager.ConnectionState.Connected

    val warningRed = MaterialTheme.colorScheme.error
    val accentCyan = MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    ScreenScaffold(
        title = "Lockdown",
        subtitle = "Tighten device security",
        onBack = onBack
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
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
                    text = if (isActive) "Saturation active" else "Idle",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) warningRed
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.weight(1f))

                // Tactical Actions
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Lock actions",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TacticalActionButton(
                            label = "Mac lock",
                            icon = Icons.Default.Computer,
                            color = MaterialTheme.colorScheme.onSurface,
                            onClick = { viewModel.triggerMacLock() },
                            modifier = Modifier.weight(1f),
                            enabled = isConnected
                        )
                        TacticalActionButton(
                            label = "Windows lock",
                            icon = Icons.Default.GridOn,
                            color = accentCyan,
                            onClick = { viewModel.triggerWindowsLock() },
                            modifier = Modifier.weight(1f),
                            enabled = isConnected
                        )
                    }
                }

                Text(
                    "Sends conflicting mouse and keyboard reports to disrupt the target device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun InterlockButton(isActive: Boolean, isConnected: Boolean, onClick: () -> Unit) {
    val targetColor = if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
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
