package com.example.rabit.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.Graphite
import com.example.rabit.ui.theme.Obsidian
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver
import com.example.rabit.ui.theme.SuccessGreen

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToHelper: () -> Unit,
    onNavigateToKeyboard: () -> Unit,
    onNavigateToWebBridge: () -> Unit,
    onNavigateToPairing: () -> Unit
) {
    val isHelperConnected by viewModel.isHelperConnected.collectAsState()
    val helperName by viewModel.helperDeviceName.collectAsState()
    val helperMac by viewModel.helperDeviceMac.collectAsState()
    val helperBaseUrl by viewModel.helperBaseUrl.collectAsState()
    val helperIp by viewModel.helperDeviceIp.collectAsState()
    val p2pStatus by viewModel.p2pStatus.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val transfers = remember(terminalOutput) {
        terminalOutput.lines().filter { it.contains("Upload complete") || it.contains("savedPath") || it.contains("Error") }.takeLast(6)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Home, contentDescription = null, tint = AccentBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Hackie Home", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Graphite)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Connected Device", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    if (isHelperConnected) "${helperName.ifBlank { "Desktop Helper" }} is online" else "No helper connected yet",
                    color = if (isHelperConnected) SuccessGreen else Silver,
                    fontSize = 14.sp
                )
                Text("IP: $helperIp", color = Silver, fontSize = 12.sp)
                Text("MAC: $helperMac", color = Silver, fontSize = 12.sp)
                if (helperBaseUrl.isNotBlank()) {
                    Text("Endpoint: $helperBaseUrl", color = Silver, fontSize = 12.sp)
                }
                Text("P2P: $p2pStatus", color = Silver, fontSize = 12.sp)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Graphite)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick Actions", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                Button(
                    onClick = {
                        viewModel.discoverHelperOnLocalWifi()
                        onNavigateToHelper()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Obsidian)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Find Helper on Local Wi-Fi", color = Obsidian, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigateToHelper,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Devices, contentDescription = null, tint = Obsidian)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Helper Control", color = Obsidian, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigateToWebBridge,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = Obsidian)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open File Sharing Hub", color = Obsidian, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigateToKeyboard,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = null, tint = Obsidian)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Control Deck", color = Obsidian, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigateToPairing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Graphite)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = Platinum)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Pairing", color = Platinum, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Graphite)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Recent Transfers", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (transfers.isEmpty()) {
                    Text("No transfer activity yet", color = Silver, fontSize = 13.sp)
                } else {
                    transfers.forEach { line ->
                        Text("• $line", color = Silver, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "File sharing supports LAN direct mode first, and internet direct mode by setting helper public URL in Helper Control.",
            color = Silver,
            fontSize = 12.sp
        )
    }
}
