package com.example.rabit.ui.network

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rabit.ui.components.AppCard
import com.example.rabit.ui.components.IconTile
import com.example.rabit.ui.components.LabelPill
import com.example.rabit.ui.components.ScreenScaffold

private data class ReconSubFeature(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkReconScreen(
    networkAuditorViewModel: NetworkAuditorViewModel,
    portScannerViewModel: PortScannerViewModel,
    pingTraceViewModel: PingTraceViewModel,
    bleViewModel: BleAuditorViewModel,
    wifiViewModel: WifiAttackerViewModel,
    bluetoothShadowViewModel: BluetoothShadowViewModel,
    apiKey: String,
    onBack: () -> Unit
) {
    var currentSubFeature by remember { mutableStateOf("auditor") }
    val accent = MaterialTheme.colorScheme.primary

    val features = remember {
        listOf(
            ReconSubFeature("auditor", "Network auditor", Icons.Default.Hub),
            ReconSubFeature("scanner", "Port scanner", Icons.Default.Radar),
            ReconSubFeature("ping", "Ping & trace", Icons.Default.NetworkPing),
            ReconSubFeature("wifi", "Wireless", Icons.Default.Wifi),
            ReconSubFeature("bluetooth", "Bluetooth", Icons.Default.Bluetooth),
        )
    }

    ScreenScaffold(
        title = "Network recon",
        subtitle = "Discover hosts and services",
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Sub-feature chip strip
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    features.forEach { f ->
                        val isSelected = currentSubFeature == f.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { currentSubFeature = f.id },
                            label = { Text(f.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = {
                                Icon(f.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.copy(alpha = 0.15f),
                                selectedLabelColor = accent,
                                selectedLeadingIconColor = accent,
                            ),
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Main content
            Box(modifier = Modifier.weight(1f)) {
                when (currentSubFeature) {
                    "auditor" -> NetworkAuditorContent(networkAuditorViewModel, accent)
                    "scanner" -> PortScannerContent(portScannerViewModel)
                    "ping" -> PingTraceContent(pingTraceViewModel)
                    "wifi" -> WirelessAuditorContent(wifiViewModel, bleViewModel, apiKey)
                    "bluetooth" -> BluetoothShadowContent(bluetoothShadowViewModel)
                }
            }
        }
    }
}
