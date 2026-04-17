package com.example.rabit.ui.automation

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.hardware.usb.UsbDevice
import com.example.rabit.data.storage.adb_tls.AdbTlsPairingManager
import com.example.rabit.data.storage.RemoteStorageManager
import com.example.rabit.ui.automation.AutomationViewModel
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver
import androidx.compose.material.icons.filled.Search
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbManagerScreen(
    viewModel: AutomationViewModel,
    onBack: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToMirror: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
    
    var adbIp by remember { mutableStateOf(prefs.getString("adb_ip", "") ?: "") }
    var adbPort by remember { mutableStateOf((prefs.getInt("adb_port", 5555)).toString()) }
    var isConnecting by remember { mutableStateOf(false) }
    
    var showPairDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var pairingHost by remember { mutableStateOf("") }
    var pairingPort by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var isPairing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    
    var usbDevices by remember { mutableStateOf<List<UsbDevice>>(emptyList()) }
    var isScanningUsb by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showQrScanner = true
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ADB Storage Manager", color = Platinum, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1E1E1E).copy(alpha = 0.95f),
                    titleContentColor = Platinum
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Platinum)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect Android Device", color = Platinum, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "1. Turn on Developer Options on the target device.\n" +
                        "2. Enable 'Wireless Debugging' (Android 11+).\n" +
                        "3. Enter the IP and Port provided on that screen.",
                        color = Silver,
                        fontSize = 14.sp
                    )
                }
            }

            Text("Wireless Connection", color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = adbIp,
                onValueChange = { adbIp = it },
                label = { Text("Device IP Address (e.g. 192.168.1.10)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Platinum,
                    unfocusedTextColor = Platinum,
                    focusedBorderColor = Color(0xFF0A84FF),
                    unfocusedBorderColor = Silver,
                    focusedLabelColor = Color(0xFF0A84FF),
                    unfocusedLabelColor = Silver
                )
            )

            OutlinedTextField(
                value = adbPort,
                onValueChange = { adbPort = it.filter { char -> char.isDigit() } },
                label = { Text("Port (Usually 5555 or 37465)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Platinum,
                    unfocusedTextColor = Platinum,
                    focusedBorderColor = Color(0xFF0A84FF),
                    unfocusedBorderColor = Silver,
                    focusedLabelColor = Color(0xFF0A84FF),
                    unfocusedLabelColor = Silver
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showPairDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Platinum)
                ) {
                    Text("Pair with Code")
                }
                
                OutlinedButton(
                    onClick = {
                        val permission = Manifest.permission.CAMERA
                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                            showQrScanner = true
                        } else {
                            cameraPermissionLauncher.launch(permission)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Platinum)
                ) {
                    Text("Scan QR Code")
                }
            }

            Button(
                onClick = {
                    isConnecting = true
                    prefs.edit()
                        .putString("adb_ip", adbIp)
                        .putInt("adb_port", adbPort.toIntOrNull() ?: 5555)
                        .apply()

                    scope.launch(Dispatchers.IO) {
                        try {
                            RemoteStorageManager.mount(context)
                            val isConnected = RemoteStorageManager.isConnected
                            withContext(Dispatchers.Main) {
                                isConnecting = false
                                if (isConnected) {
                                    Toast.makeText(context, "Mounted ADB Storage Successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to connect", Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isConnecting = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = adbIp.isNotBlank() && adbPort.isNotBlank() && !isConnecting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF))
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = Platinum)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect & Mount", color = Platinum, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onNavigateToFiles,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Platinum)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open ADB Storage in Files App")
            }

            Button(
                onClick = onNavigateToMirror,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5856D6))
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Platinum)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Wireless Screen Mirror", color = Platinum)
            }

            // USB OTG Section
            HorizontalDivider(color = Silver.copy(alpha = 0.2f), thickness = 1.dp)
            Text("USB OTG Connection", color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            
            Button(
                onClick = {
                    isScanningUsb = true
                    usbDevices = viewModel.usbAdbManager.findAdbDevices()
                    isScanningUsb = false
                    if (usbDevices.isEmpty()) {
                        Toast.makeText(context, "No ADB devices found. Check cable & USB Debugging.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
            ) {
                if (isScanningUsb) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Platinum)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan for USB Devices", color = Platinum)
                }
            }

            usbDevices.forEach { device ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        isConnecting = true
                        RemoteStorageManager.connectUsb(context, device) { success ->
                            isConnecting = false
                            if (success) {
                                Toast.makeText(context, "Connected to ${device.productName} via USB!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "USB Connection Failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.productName ?: "Unknown Device", color = Platinum, fontWeight = FontWeight.Bold)
                            Text("ID: ${device.deviceId} | Vendor: ${device.vendorId}", color = Silver, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF34C759))
                    }
                }
            }
        }
    }

    if (showPairDialog) {
        AlertDialog(
            onDismissRequest = { if (!isPairing) showPairDialog = false },
            title = { Text("Wireless Pairing") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Step: Wireless Debugging > Pair device with pairing code", fontSize = 12.sp, color = Silver)
                    OutlinedTextField(
                        value = pairingHost,
                        onValueChange = { pairingHost = it },
                        label = { Text("IP Address") }
                    )
                    OutlinedTextField(
                        value = pairingPort,
                        onValueChange = { pairingPort = it },
                        label = { Text("Pairing Port") }
                    )
                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = { pairingCode = it },
                        label = { Text("Pairing Code") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isPairing = true
                        scope.launch {
                            val result = AdbTlsPairingManager.pairDevice(
                                context,
                                pairingHost,
                                pairingPort.toIntOrNull() ?: 0,
                                pairingCode
                            )
                            isPairing = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "Paired Successfully!", Toast.LENGTH_SHORT).show()
                                showPairDialog = false
                                // Auto fill the connection fields with the pair host
                                adbIp = pairingHost
                            } else {
                                Toast.makeText(context, "Pairing Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isPairing && pairingHost.isNotEmpty() && pairingPort.isNotEmpty() && pairingCode.isNotEmpty()
                ) {
                    if (isPairing) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    else Text("Pair")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPairDialog = false }, enabled = !isPairing) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showQrScanner) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AdbPairingScanner(
                onQrScanned = { qr ->
                    val data = AdbTlsPairingManager.parseQrCode(qr)
                    if (data != null) {
                        // For standard Google QR codes, we still need the dynamic IP/Port discovered via mDNS
                        // since the QR code only contains the service name and password.
                        // However, some custom QR codes might provide ip:port.
                        // For now, let's notify the user or try to auto-fill if possible.
                        Toast.makeText(context, "QR Detected. Service: ${data.first}. Enter IP/Port manually to pair.", Toast.LENGTH_LONG).show()
                        pairingCode = data.third
                        showQrScanner = false
                        showPairDialog = true
                    }
                }
            )
            IconButton(
                onClick = { showQrScanner = false },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Close", tint = Color.White)
            }
            Text(
                "Scan the Wireless Debugging QR Code",
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
            )
        }
    }
}
