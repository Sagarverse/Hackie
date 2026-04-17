package com.example.rabit.ui.pairing

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.*
import kotlinx.coroutines.delay

@SuppressLint("MissingPermission")
@Composable
fun PairingScreen(
    viewModel: MainViewModel, 
    onConnected: () -> Unit,
    onNavigateToSettings: () -> Unit,
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
            delay(1000)
        }
    }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            is HidDeviceManager.ConnectionState.Connected -> {
                viewModel.stopScanning()
                connectingDeviceAddress = null
                connectionError = null
                onConnected()
            }
            is HidDeviceManager.ConnectionState.Disconnected -> {
                if (connectingDeviceAddress != null) {
                    val deviceName = try {
                        bluetoothAdapter?.bondedDevices?.find { it.address == connectingDeviceAddress }?.name ?: ""
                    } catch (e: Exception) { "" }
                    val deviceType = guessDeviceType(deviceName)
                    connectionError = when (deviceType) {
                        DeviceType.WINDOWS -> "Connection failed. Check Windows Bluetooth settings."
                        DeviceType.MAC -> "Connection failed. Ensure Mac is discoverable."
                        else -> "Connection failed. Ensure Bluetooth is enabled on target."
                    }
                    connectingDeviceAddress = null
                }
            }
            else -> {}
        }
    }

    val currentStep = remember(isBluetoothEnabled, isScanning, connectionState) {
        when {
            !isBluetoothEnabled -> 0
            connectionState is HidDeviceManager.ConnectionState.Connected -> 3
            isScanning -> 1
            else -> 2
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Obsidian)) {
        // Hero Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PremiumDarkGradient)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Text(
                    "PAIRING INTERFACE",
                    color = Platinum,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    "HID Control Connection",
                    color = AccentBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(24.dp))
                ConnectionStepIndicator(currentStep = currentStep)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AccentBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, AccentBlue.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TipsAndUpdates, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Enable Bluetooth, choose a workstation, and confirm pairing on your computer.",
                            color = Silver,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            if (connectionError != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ErrorRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, ErrorRed.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(connectionError!!, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { connectionError = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = ErrorRed)
                            }
                        }
                    }
                }
            }

            if (!isBluetoothEnabled) {
                item {
                    StatusCard(
                        icon = Icons.Default.BluetoothDisabled,
                        title = "Bluetooth Disabled",
                        desc = "Enable Bluetooth to connect as a keyboard/mouse.",
                        buttonText = "Enable Bluetooth",
                        onClick = { viewModel.requestEnableBluetooth(context) }
                    )
                }
            } else {
                if (savedDevices.isNotEmpty() && connectionState !is HidDeviceManager.ConnectionState.Connected) {
                    val lastDevice = savedDevices.first()
                    item {
                        SectionHeader("QUICK CONNECT")
                        QuickConnectCard(
                            deviceName = lastDevice.name,
                            lastConnectedTime = lastDevice.lastConnected,
                            isConnecting = connectingDeviceAddress != null,
                            onClick = {
                                bluetoothAdapter?.bondedDevices?.find { it.name == lastDevice.name }?.let {
                                    connectingDeviceAddress = it.address
                                    viewModel.connectWithRetry(it)
                                }
                            }
                        )
                    }
                }

                item { SectionHeader("KNOWN WORKSTATIONS", "Previously paired devices.") }

                val bondedDevices = try { bluetoothAdapter?.bondedDevices?.toList() ?: emptyList() } catch (e: Exception) { emptyList() }
                if (bondedDevices.isEmpty()) {
                    item { EmptyStateCard("No paired devices found") }
                } else {
                    items(bondedDevices, key = { it.address }) { device ->
                        AnimatedDeviceCard(
                            name = device.name ?: "Unknown",
                            deviceType = guessDeviceType(device.name),
                            subtitle = if (connectingDeviceAddress == device.address) "Connecting..." else "Tap to connect",
                            isConnecting = connectingDeviceAddress == device.address,
                            isBonded = true,
                            onClick = {
                                connectingDeviceAddress = device.address
                                viewModel.connect(device)
                            }
                        )
                    }
                }

                item {
                    SectionHeader("NEARBY DISCOVERIES", "Available machines in range.", isScanning) {
                        viewModel.startScanning()
                    }
                }

                val bondedAddresses = bondedDevices.map { it.address }.toSet()
                val newDevices = scannedDevices.filter { it.address !in bondedAddresses }

                if (newDevices.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            color = Graphite.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isScanning) RadarAnimationSmall() else Text("No nearby devices", color = Silver)
                            }
                        }
                    }
                } else {
                    items(newDevices, key = { it.address }) { device ->
                        AnimatedDeviceCard(
                            name = device.name,
                            deviceType = guessDeviceType(device.name),
                            subtitle = if (connectingDeviceAddress == device.address) "Connecting..." else "New Discovery",
                            isConnecting = connectingDeviceAddress == device.address,
                            isBonded = false,
                            onClick = {
                                connectingDeviceAddress = device.address
                                viewModel.connect(device)
                            }
                        )
                    }
                }

                item {
                    PremiumGlassCard(modifier = Modifier.padding(top = 16.dp)) {
                        Column {
                            Text("NOT SEEING YOUR DEVICE?", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Ensure target machine is discoverable.", color = Silver.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null, isScanning: Boolean = false, onRefresh: (() -> Unit)? = null) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Silver, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            if (onRefresh != null) {
                if (isScanning) CircularProgressIndicator(modifier = Modifier.size(12.dp), color = AccentBlue, strokeWidth = 2.dp)
                else {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(24.dp)) { 
                        Icon(Icons.Default.Refresh, null, tint = AccentBlue, modifier = Modifier.size(16.dp)) 
                    }
                }
            }
        }
        if (subtitle != null) Text(subtitle, color = Silver.copy(alpha = 0.6f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun EmptyStateCard(text: String) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Graphite.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
        Text(text, color = Silver, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun StatusCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, buttonText: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = AccentBlue.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = AccentBlue, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, color = Platinum, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = Silver, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) { Text(buttonText) }
        }
    }
}

@Composable
fun RadarAnimationSmall() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0.01f, 
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing), 
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0.01f, 
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, 1000, easing = LinearEasing), 
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension / 2
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(AccentBlue.copy(alpha = 0.15f), Color.Transparent),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            fun drawRing(progress: Float) {
                val r = maxRadius * progress
                if (progress > 0.01f) {
                    val alpha = (1f - progress) * 0.5f
                    drawCircle(
                        color = AccentBlue.copy(alpha = alpha),
                        radius = r,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
            drawRing(ring1)
            drawRing(ring2)
        }
        Icon(Icons.Default.Bluetooth, null, tint = AccentBlue, modifier = Modifier.size(24.dp))
    }
}
