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
import com.example.rabit.data.storage.RemoteStorageManager
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbManagerScreen(
    onBack: () -> Unit,
    onNavigateToFiles: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
    
    var adbIp by remember { mutableStateOf(prefs.getString("adb_ip", "") ?: "") }
    var adbPort by remember { mutableStateOf((prefs.getInt("adb_port", 5555)).toString()) }
    var isConnecting by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

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
        }
    }
}
