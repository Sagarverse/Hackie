package com.example.rabit.ui.pairing

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.*

@SuppressLint("MissingPermission")
@Composable
fun PairingScreen(
    viewModel: MainViewModel, 
    onConnected: () -> Unit,
    onNavigateToKeyboard: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToWebBridge: () -> Unit,
    onNavigateToInjector: () -> Unit,
    onNavigateToAirPlayReceiver: () -> Unit,
    onNavigateToWakeOnLan: () -> Unit,
    onNavigateToSshTerminal: () -> Unit,
    onNavigateToGlobalSearch: () -> Unit,
    onNavigateToSnippets: () -> Unit,
    onNavigateToAutomation: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCustomization: () -> Unit,
    onNavigateToPasswordManager: () -> Unit,
    onNavigateToHelper: () -> Unit = {}
) {
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val savedDevices by viewModel.savedDevices.collectAsState()
    val context = LocalContext.current
    val bluetoothAdapter = remember { context.getSystemService(BluetoothManager::class.java)?.adapter }
    var isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }
    var connectingDeviceAddress by remember { mutableStateOf<String?>(null) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (bluetoothAdapter?.isEnabled == true) {
            viewModel.startScanning()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val wasEnabled = isBluetoothEnabled
            isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
            if (!wasEnabled && isBluetoothEnabled) {
                viewModel.startScanning()
            }
            kotlinx.coroutines.delay(2000)
        }
    }

    LaunchedEffect(connectionState) {
        if (connectionState is HidDeviceManager.ConnectionState.Connected) {
            viewModel.stopScanning()
            connectingDeviceAddress = null
            connectionError = null
            onConnected()
        }
    }

    val newDevices = scannedDevices.toList()

    val currentStep = remember(isBluetoothEnabled, isScanning, connectionState) {
        when {
            !isBluetoothEnabled -> 0
            connectionState is HidDeviceManager.ConnectionState.Connected -> 3
            isScanning -> 1
            else -> 2
        }
    }


    
    // Connect to a saved device by address
    fun connectToSavedDevice(address: String) {
        val bluetoothDevice = try {
            bluetoothAdapter?.getRemoteDevice(address)
        } catch (e: Exception) { null }

        if (bluetoothDevice != null) {
            viewModel.stopScanning()
            connectingDeviceAddress = bluetoothDevice.address
            connectionError = null
            viewModel.connect(bluetoothDevice)
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Obsidian)) {
        // Ambient Aesthetic Glows
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().alpha(0.15f).background(
                Brush.radialGradient(
                    colors = listOf(AccentBlue, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(0f, 0f),
                    radius = 1400f
                )
            ))
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
                    // Hero Stepper
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                        DarkSkeuoCard(modifier = Modifier.fillMaxWidth()) {
                            ConnectionStepIndicator(currentStep = currentStep)
                        }
                    }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Saved Devices / Quick Reconnect
                    if (savedDevices.isNotEmpty()) {
                        item {
                            Text(
                                "Known Devices",
                                color = Platinum,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(savedDevices) { saved ->
                            val isThisDeviceConnecting = connectingDeviceAddress == saved.address
                            AnimatedDeviceCard(
                                name = saved.name,
                                deviceType = guessDeviceType(saved.name),
                                subtitle = if (isThisDeviceConnecting) "Reconnecting..." else "Saved computer",
                                isConnecting = isThisDeviceConnecting,
                                isBonded = true,
                                onClick = { connectToSavedDevice(saved.address) }
                            )
                        }
                        item {
                            HorizontalDivider(
                                color = BorderColor.copy(alpha = 0.2f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // Scan Headline
                    item {
                        Text(
                            "Discover Near Me",
                            color = Platinum,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                        // Bluetooth Banner
                        if (!isBluetoothEnabled) {
                            item {
                                BluetoothDisabledBanner { viewModel.requestEnableBluetooth(context) }
                            }
                        }

                        // Connection Logic State
                        if (isBluetoothEnabled && newDevices.isEmpty()) {
                            item {
                                DarkSkeuoCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    borderColor = BorderColor.copy(alpha = 0.2f)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(160.dp)) {
                                        if (isScanning) {
                                            RadarAnimationSmall()
                                        } else {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null, tint = Silver.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("No new devices found", color = Silver.copy(alpha = 0.6f), fontSize = 13.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "Move closer and keep your computer discoverable",
                                                    color = Silver.copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (isBluetoothEnabled) {
                            items(newDevices, key = { it.address }) { device ->
                                val deviceName = try { device.name ?: "Unknown Device" } catch(e: Exception) { "Unknown" }
                                val deviceType = guessDeviceType(deviceName)
                                val isThisDeviceConnecting = connectingDeviceAddress == device.address

                                AnimatedDeviceCard(
                                    name = deviceName,
                                    deviceType = deviceType,
                                    subtitle = if (isThisDeviceConnecting) "Pairing & Connecting..." else "New device",
                                    isConnecting = isThisDeviceConnecting,
                                    isBonded = false,
                                    onClick = {
                                        // Stop scanning before attempting to connect for better success rate
                                        viewModel.stopScanning() 
                                        connectingDeviceAddress = device.address
                                        connectionError = null
                                        viewModel.connect(device)
                                    }
                                )
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            // Make Mac discoverable hint
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.requestDiscoverable() },
                                color = SoftGrey.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp).background(AccentGold.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Don't see your Mac?", color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Tap here to make Hackie discoverable", color = Silver, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            // Skip to Offline AI Assistant
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { onNavigateToAssistant() },
                                color = AiViolet.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, AiViolet.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp).background(AiViolet.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ChatBubble, contentDescription = null, tint = AiViolet, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Skip to Chat Assistant", color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Use offline AI mode without connecting", color = Silver, fontSize = 12.sp)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AiViolet, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Radar Animation
// ────────────────────────────────────────────────────────────────────────────────

@Composable
fun RadarAnimationSmall() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0.01f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring1"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0.01f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, 1000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring2"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension / 2
            
            // Ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentBlue.copy(alpha = 0.15f), Color.Transparent),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            listOf(ring1, ring2).forEach { progress ->
                val r = maxRadius * progress
                if (r > 0.5f) {
                    val alpha = (1f - progress) * 0.5f
                    drawCircle(
                        color = AccentBlue.copy(alpha = alpha),
                        radius = r,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
        
        Icon(
            Icons.Default.Bluetooth,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(24.dp)
        )
    }
}
