package com.example.rabit.ui.keyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.UsbMouseBridgeManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.PremiumGlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MouseBridgeScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val bridgeState by viewModel.mouseBridgeState.collectAsState()
    val mouseName by viewModel.mouseBridgeMouseName.collectAsState()
    val eventsForwarded by viewModel.mouseBridgeEventsForwarded.collectAsState()
    val isRootAvailable by viewModel.mouseBridgeRootAvailable.collectAsState()
    val errorMessage by viewModel.mouseBridgeError.collectAsState()
    val isConnected by viewModel.isBluetoothConnected.collectAsState()

    val isActive = bridgeState == UsbMouseBridgeManager.BridgeState.ACTIVE
    val isScanning = bridgeState == UsbMouseBridgeManager.BridgeState.SCANNING_DEVICES

    // Pulse animation for active state
    val infiniteTransition = rememberInfiniteTransition(label = "bridge_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mouse Bridge", color = Platinum, fontWeight = FontWeight.Bold)
                        if (isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SuccessGreen.copy(alpha = 0.2f),
                            ) {
                                Text(
                                    "LIVE",
                                    color = SuccessGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Obsidian)
            )
        },
        containerColor = Obsidian
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ═══ Connection Flow Diagram ═══
            PremiumGlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "CONNECTION FLOW",
                        color = Silver,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Wired Mouse
                        FlowNode(
                            icon = "🖱️",
                            label = "Wired\nMouse",
                            isHighlighted = isActive,
                            color = if (isActive) AccentBlue else Silver
                        )
                        FlowArrow(isActive = isActive)
                        // Phone (Bridge)
                        FlowNode(
                            icon = "📱",
                            label = "Phone\n(Bridge)",
                            isHighlighted = true,
                            color = AccentTeal
                        )
                        FlowArrow(isActive = isActive && isConnected)
                        // Bluetooth
                        FlowNode(
                            icon = "🔵",
                            label = "Bluetooth\nHID",
                            isHighlighted = isConnected,
                            color = if (isConnected) AccentBlue else Silver
                        )
                        FlowArrow(isActive = isActive && isConnected)
                        // PC
                        FlowNode(
                            icon = "💻",
                            label = "PC",
                            isHighlighted = isActive && isConnected,
                            color = if (isActive && isConnected) SuccessGreen else Silver
                        )
                    }
                }
            }

            // ═══ Mouse Status Card ═══
            PremiumGlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated mouse icon
                    Box(contentAlignment = Alignment.Center) {
                        if (isActive) {
                            // Pulse ring
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(SuccessGreen.copy(alpha = pulseAlpha * 0.15f))
                                    .border(1.dp, SuccessGreen.copy(alpha = pulseAlpha * 0.4f), CircleShape)
                            )
                        }
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = when (bridgeState) {
                                UsbMouseBridgeManager.BridgeState.ACTIVE -> SuccessGreen.copy(alpha = 0.15f)
                                UsbMouseBridgeManager.BridgeState.SCANNING_DEVICES -> AccentBlue.copy(alpha = 0.15f)
                                UsbMouseBridgeManager.BridgeState.ERROR,
                                UsbMouseBridgeManager.BridgeState.NO_ROOT,
                                UsbMouseBridgeManager.BridgeState.NO_MOUSE_FOUND -> ErrorRed.copy(alpha = 0.15f)
                                else -> SoftGrey.copy(alpha = 0.3f)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when (bridgeState) {
                                    UsbMouseBridgeManager.BridgeState.ACTIVE -> SuccessGreen.copy(alpha = 0.5f)
                                    UsbMouseBridgeManager.BridgeState.SCANNING_DEVICES -> AccentBlue.copy(alpha = 0.5f)
                                    UsbMouseBridgeManager.BridgeState.ERROR,
                                    UsbMouseBridgeManager.BridgeState.NO_ROOT,
                                    UsbMouseBridgeManager.BridgeState.NO_MOUSE_FOUND -> ErrorRed.copy(alpha = 0.5f)
                                    else -> BorderColor.copy(alpha = 0.3f)
                                }
                            )
                        ) {
                            Icon(
                                Icons.Default.Mouse,
                                contentDescription = null,
                                tint = when (bridgeState) {
                                    UsbMouseBridgeManager.BridgeState.ACTIVE -> SuccessGreen
                                    UsbMouseBridgeManager.BridgeState.SCANNING_DEVICES -> AccentBlue
                                    UsbMouseBridgeManager.BridgeState.ERROR,
                                    UsbMouseBridgeManager.BridgeState.NO_ROOT,
                                    UsbMouseBridgeManager.BridgeState.NO_MOUSE_FOUND -> ErrorRed
                                    else -> Silver
                                },
                                modifier = Modifier
                                    .padding(18.dp)
                                    .fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status text
                    Text(
                        text = when (bridgeState) {
                            UsbMouseBridgeManager.BridgeState.IDLE -> "Ready to Bridge"
                            UsbMouseBridgeManager.BridgeState.SCANNING_DEVICES -> "Scanning for USB Mouse..."
                            UsbMouseBridgeManager.BridgeState.ACTIVE -> "Bridge Active"
                            UsbMouseBridgeManager.BridgeState.NO_ROOT -> "Root Access Required"
                            UsbMouseBridgeManager.BridgeState.NO_MOUSE_FOUND -> "No USB Mouse Found"
                            UsbMouseBridgeManager.BridgeState.ERROR -> "Bridge Error"
                        },
                        color = Platinum,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (mouseName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mouseName,
                            color = AccentTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (errorMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = ErrorRed.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ═══ Telemetry ═══
            AnimatedVisibility(visible = isActive) {
                PremiumGlassCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TelemetryItem(
                            label = "Events",
                            value = formatCount(eventsForwarded),
                            color = AccentTeal
                        )
                        TelemetryItem(
                            label = "Status",
                            value = if (isConnected) "Connected" else "No BT Target",
                            color = if (isConnected) SuccessGreen else WarningYellow
                        )
                        TelemetryItem(
                            label = "Root",
                            value = if (isRootAvailable) "✓" else "✗",
                            color = if (isRootAvailable) SuccessGreen else ErrorRed
                        )
                    }
                }
            }

            // ═══ Sensitivity Slider ═══
            PremiumGlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Bridge Sensitivity", color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    var sensitivity by remember { mutableFloatStateOf(1.0f) }
                    Text(
                        "Multiply mouse deltas: ${String.format("%.1f", sensitivity)}x",
                        color = Silver,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 32.dp, top = 4.dp)
                    )
                    Slider(
                        value = sensitivity,
                        onValueChange = {
                            sensitivity = it
                            viewModel.setMouseBridgeSensitivity(it)
                        },
                        valueRange = 0.2f..3.0f,
                        steps = 13,
                        modifier = Modifier.padding(start = 32.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = AccentPurple,
                            activeTrackColor = AccentPurple
                        )
                    )
                }
            }

            // ═══ Start / Stop Button ═══
            Button(
                onClick = {
                    if (isActive || isScanning) {
                        viewModel.stopMouseBridge()
                    } else {
                        viewModel.startMouseBridge()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) ErrorRed else AccentBlue
                ),
                enabled = !isScanning
            ) {
                Icon(
                    if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isScanning -> "Scanning..."
                        isActive -> "Stop Bridge"
                        else -> "Start Bridge"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // ═══ Requirements Info ═══
            if (!isRootAvailable) {
                PremiumGlassCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Root Access Required", color = WarningYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "USB Mouse Bridge needs root to read raw input events from /dev/input/. " +
                                        "This is the same requirement as USB HID Gadget mode.",
                                color = Silver,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (!isConnected) {
                PremiumGlassCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.BluetoothDisabled, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Bluetooth Not Connected", color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Connect to a PC via Bluetooth first. The bridge will forward USB mouse " +
                                        "events to the connected Bluetooth HID target.",
                                color = Silver,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FlowNode(icon: String, label: String, isHighlighted: Boolean, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            color = if (isHighlighted) color else Silver.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

@Composable
private fun FlowArrow(isActive: Boolean) {
    Text(
        "→",
        color = if (isActive) SuccessGreen else Silver.copy(alpha = 0.3f),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun TelemetryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Silver, fontSize = 10.sp)
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> "${String.format("%.1f", count / 1_000_000.0)}M"
        count >= 1_000 -> "${String.format("%.1f", count / 1_000.0)}K"
        else -> count.toString()
    }
}
