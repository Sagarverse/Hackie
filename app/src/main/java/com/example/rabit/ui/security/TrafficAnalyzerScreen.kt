package com.example.rabit.ui.security

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.security.NeuralTrafficAnalyzer
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.AppCard
import com.example.rabit.ui.components.IconTile
import com.example.rabit.ui.components.LabelPill
import com.example.rabit.ui.components.PrimaryButton
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficAnalyzerScreen(
    viewModel: TrafficAnalyzerViewModel,
    onBack: () -> Unit
) {
    val packets by viewModel.packets.collectAsState()
    val isHacked by viewModel.isHacked.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    val accent = MaterialTheme.colorScheme.primary

    ScreenScaffold(
        title = "Traffic analyzer",
        subtitle = "Inspect local packets",
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // Briefing card
            var showBriefing by remember { mutableStateOf(true) }
            if (showBriefing) {
                AppCard {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "About this tool",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Listens to the local network so you can spot spy apps and viruses in real time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { showBriefing = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            var selectedTab by remember { mutableIntStateOf(0) }
            val tabs = listOf("Live intercept", "Traffic history")

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = accent,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = accent,
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selectedTab == index) accent
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }

            if (selectedTab == 0) {
                LiveTrafficContent(viewModel)
            } else {
                TrafficHistoryContent(viewModel)
            }
        }
    }
}

@Composable
fun LiveTrafficContent(viewModel: TrafficAnalyzerViewModel) {
    val packets by viewModel.packets.collectAsState()
    val isHacked by viewModel.isHacked.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        // --- Status HUD ---
        item {
            val statusColor = when (isHacked) {
                true -> MaterialTheme.colorScheme.error
                false -> Success
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val statusText = when (isHacked) {
                true -> "Compromise detected"
                false -> "Secure, no anomalies"
                else -> "Awaiting telemetry"
            }
            val statusSub = when (isHacked) {
                true -> "Anomalous command-and-control traffic patterns identified."
                false -> "Network state within ethical parameters."
                else -> "Press the button to begin capturing packets."
            }

            AppCard {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconTile(
                        icon = if (isHacked == true) Icons.Default.Warning else Icons.Default.Shield,
                        color = statusColor,
                        modifier = Modifier.size(56.dp),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleMedium,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = statusSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PrimaryButton(
                        text = if (isAnalyzing) "Terminate sniffer" else "Initialize inspection",
                        onClick = {
                            if (isAnalyzing) viewModel.stopSniffing() else viewModel.startSniffing()
                        },
                        icon = if (isAnalyzing) Icons.Default.Stop else Icons.Default.PlayArrow,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // --- Packet stream header ---
        item {
            SectionHeader(
                title = "Live packet stream",
                action = if (isAnalyzing) {
                    { LabelPill(text = "Sniffing", background = Success.copy(alpha = 0.15f), foreground = Success) }
                } else null,
            )
        }

        if (packets.isEmpty()) {
            item {
                AppCard {
                    Text(
                        text = "No traffic captured yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(packets) { packet ->
                PacketItem(packet)
            }
        }
    }
}

@Composable
fun TrafficHistoryContent(viewModel: TrafficAnalyzerViewModel) {
    val history by viewModel.trafficHistory.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${history.size} capture${if (history.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (history.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearTrafficHistory() }) {
                    Text("Wipe history", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (history.isEmpty()) {
            AppCard {
                Text(
                    text = "No saved captures. Run a scan to log traffic.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(history) { record ->
                    TrafficRecordCard(record)
                }
            }
        }
    }
}

@Composable
fun TrafficRecordCard(record: com.example.rabit.data.security.TrafficStorageManager.TrafficRecord) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconTile(
                    icon = Icons.Default.History,
                    color = if (record.wasCompromised) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = java.text.SimpleDateFormat("MMM dd, yyyy · HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(record.timestamp)),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = record.targetName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LabelPill(
                    text = if (record.wasCompromised) "Compromised" else "Clean",
                    background = if (record.wasCompromised) MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    else Success.copy(alpha = 0.15f),
                    foreground = if (record.wasCompromised) MaterialTheme.colorScheme.error else Success,
                )
            }
            Text(
                text = "${record.packets.size} packets captured",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PacketItem(packet: NeuralTrafficAnalyzer.Packet) {
    val threatColor = if (packet.isSuspicious) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurfaceVariant

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LabelPill(
                    text = packet.protocol,
                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    foreground = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = packet.source,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = packet.destination,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = if (packet.isSuspicious) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
            if (packet.isSuspicious) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "Threat: ${packet.threatReason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = threatColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
