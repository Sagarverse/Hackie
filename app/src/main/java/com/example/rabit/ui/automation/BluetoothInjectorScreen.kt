package com.example.rabit.ui.automation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.domain.model.TargetOs
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothInjectorScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val isHidConnected = connectionState is HidDeviceManager.ConnectionState.Connected
    val view = LocalView.current
    
    var selectedOs by remember { mutableStateOf(TargetOs.MAC_OS) }
    var isInjecting by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Bluetooth Automator",
        onBack = onNavigateBack,
        actions = {
            Icon(
                imageVector = Icons.Default.Usb,
                contentDescription = null,
                tint = if (isHidConnected) AccentBlue else Color.Gray,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Graphite.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isHidConnected) AccentBlue.copy(alpha = 0.3f) else Color.DarkGray)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isHidConnected) AccentBlue.copy(alpha = 0.1f) else Color.DarkGray.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Usb,
                            contentDescription = null,
                            tint = if (isHidConnected) AccentBlue else Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isHidConnected) "OTG HID Connected" else "USB Disconnected",
                            color = Platinum,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isHidConnected) "Ready to inject payloads" else "Connect phone to target via USB OTG",
                            color = Silver,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Target OS Selector
            Text(
                "TARGET OPERATING SYSTEM",
                color = AccentBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TargetOs.entries.forEach { os ->
                    val isSelected = selectedOs == os
                    Surface(
                        onClick = { selectedOs = os },
                        color = if (isSelected) AccentBlue.copy(alpha = 0.15f) else Graphite.copy(alpha = 0.3f),
                        contentColor = if (isSelected) AccentBlue else Silver,
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) BorderStroke(1.5.dp, AccentBlue.copy(alpha = 0.6f)) else BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = os.iconRes),
                                contentDescription = os.displayName,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) AccentBlue else Silver
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                os.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Inject Button
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val animScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (isHidConnected) 1.05f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(animScale)
                    .clip(CircleShape)
                    .background(if (isHidConnected) AccentBlue.copy(alpha = 0.1f) else Color.DarkGray.copy(alpha = 0.1f))
                    .border(
                        2.dp, 
                        if (isHidConnected) AccentBlue.copy(alpha = 0.5f) else Color.DarkGray,
                        CircleShape
                    )
                    .clickable(enabled = isHidConnected && !isInjecting) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        isInjecting = true
                        
                        // Generate DuckyScript based on OS
                        val payload = when (selectedOs) {
                            TargetOs.MAC_OS -> {
                                """
                                GUI SPACE
                                DELAY 500
                                STRING Bluetooth
                                DELAY 500
                                ENTER
                                DELAY 1000
                                TAB
                                TAB
                                TAB
                                """.trimIndent()
                            }
                            TargetOs.WINDOWS -> {
                                """
                                GUI r
                                DELAY 300
                                STRING ms-settings:bluetooth
                                ENTER
                                DELAY 1500
                                TAB
                                TAB
                                SPACE
                                """.trimIndent()
                            }
                            TargetOs.LINUX -> {
                                """
                                ALT F2
                                DELAY 300
                                STRING gnome-control-center bluetooth
                                ENTER
                                DELAY 1500
                                TAB
                                TAB
                                """.trimIndent()
                            }
                        }
                        
                        viewModel.executeDuckyScript(payload)
                        
                        // Reset UI after injection
                        view.postDelayed({ isInjecting = false }, 3000)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (isInjecting) {
                        CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Injecting...", color = AccentBlue, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.BluetoothConnected,
                            contentDescription = null,
                            tint = if (isHidConnected) AccentBlue else Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "AUTOMATE\nPAIRING",
                            color = if (isHidConnected) Platinum else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Press to inject DuckyScript payload over USB HID to automatically open Bluetooth settings and initiate pairing mode on the target machine.",
                color = Silver,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}
