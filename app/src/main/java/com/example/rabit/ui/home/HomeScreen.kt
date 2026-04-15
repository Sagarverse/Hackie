package com.example.rabit.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.CardDark
import com.example.rabit.ui.theme.Graphite
import com.example.rabit.ui.theme.Obsidian
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver
import com.example.rabit.ui.theme.SuccessGreen
import com.example.rabit.ui.theme.WarningYellow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenHelper: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val knownWorkstations by viewModel.knownWorkstations.collectAsState()
    val isHelperConnected by viewModel.isHelperConnected.collectAsState()
    val helperName by viewModel.helperDeviceName.collectAsState()
    val helperMac by viewModel.helperDeviceMac.collectAsState()
    val helperBaseUrl by viewModel.helperBaseUrl.collectAsState()
    val helperIp by viewModel.helperDeviceIp.collectAsState()
    val p2pStatus by viewModel.p2pStatus.collectAsState()
    val helperConnectionStatus by viewModel.helperConnectionStatus.collectAsState()
    val transfers by viewModel.helperTransferEvents.collectAsState()

    val bluetoothConnectedName = (connectionState as? HidDeviceManager.ConnectionState.Connected)?.deviceName
    val bluetoothConnectedMac = if (bluetoothConnectedName != null) {
        knownWorkstations.firstOrNull { it.name == bluetoothConnectedName }?.address
            ?: knownWorkstations.firstOrNull()?.address
    } else {
        null
    }

    val online = bluetoothConnectedName != null || isHelperConnected
    val statusText = when {
        bluetoothConnectedName != null -> "$bluetoothConnectedName is connected via Bluetooth"
        isHelperConnected -> "${helperName.ifBlank { "Desktop Helper" }} is online"
        else -> "No device connected yet"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF1A56D6), Color(0xFF102B63))
                    ),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = Platinum, modifier = Modifier.size(30.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Welcome to Hackie", color = Platinum, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
                Text("A calm control center for phone, helper, and remote actions", color = Silver, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Graphite)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF1C5CFF), Color(0xFF1037A8))))
                    , contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = Platinum)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Device Status", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text(statusText, color = if (online) SuccessGreen else Silver, fontSize = 13.sp)
                    }
                    StatusBadge(text = if (online) "Online" else "Offline", color = if (online) SuccessGreen else WarningYellow)
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricChip(label = "IP", value = if (bluetoothConnectedName != null) "Bluetooth" else helperIp)
                    MetricChip(label = "MAC", value = bluetoothConnectedMac ?: helperMac)
                    MetricChip(label = "Endpoint", value = helperBaseUrl.ifBlank { "Not set" })
                    MetricChip(label = "P2P", value = p2pStatus)
                }

                Text("Connection", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(helperConnectionStatus, color = Silver, fontSize = 13.sp)

                if (bluetoothConnectedName != null) {
                    Text("Transport: Bluetooth HID", color = SuccessGreen, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = AccentBlue)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Quick Actions", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                ActionRow(
                    label = "Open Helper",
                    description = "Switch into the helper dashboard",
                    onClick = onOpenHelper,
                    background = Color(0xFF17325E)
                )
                ActionRow(
                    label = "Rescan Helper",
                    description = "Refresh the local Wi-Fi discovery state",
                    onClick = { viewModel.discoverHelperOnLocalWifi() },
                    background = Color(0xFF143E2B)
                )
                ActionRow(
                    label = "Ping Target",
                    description = "Confirm the current helper endpoint is alive",
                    onClick = { viewModel.pingRemoteDevice() },
                    background = Color(0xFF2D254A)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Graphite)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NetworkPing, contentDescription = null, tint = AccentBlue)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Recent Transfers", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                if (transfers.isEmpty()) {
                    Text("No transfer activity yet", color = Silver, fontSize = 13.sp)
                } else {
                    transfers.takeLast(6).forEach { line ->
                        Text("• $line", color = Silver, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Graphite)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tip", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Use the Helper Control screen when you want direct device actions. Home stays focused on status and fast entry points.",
                    color = Silver,
                    fontSize = 13.sp
                )
                OutlinedButton(onClick = { viewModel.fetchHelperDeviceDetails() }) {
                    Text("Refresh status")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF101723))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label.uppercase(), color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionRow(
    label: String,
    description: String,
    onClick: () -> Unit,
    background: Color
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = background)
    ) {
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(label, color = Platinum, fontWeight = FontWeight.Bold)
            Text(description, color = Silver, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Default.Link, contentDescription = null, tint = Platinum)
    }
}
