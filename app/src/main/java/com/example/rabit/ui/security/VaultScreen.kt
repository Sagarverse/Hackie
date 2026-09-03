package com.example.rabit.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.rabit.ui.components.AppCard
import com.example.rabit.ui.components.EmptyState
import com.example.rabit.ui.components.IconTile
import com.example.rabit.ui.components.LabelPill
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.theme.HackieSpacing
import com.example.rabit.ui.theme.Success

/**
 * Hidden Vault. Reached via the decoy calculator's secret code.
 * Shows the captured audits and traffic logs side-by-side.
 */
@Composable
fun VaultScreen(
    auditorViewModel: SecurityAuditorViewModel,
    trafficViewModel: TrafficAnalyzerViewModel,
    onLock: () -> Unit
) {
    val auditHistory by auditorViewModel.auditHistory.collectAsState()
    val trafficHistory by trafficViewModel.trafficHistory.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Audits", "Traffic")
    val accent = MaterialTheme.colorScheme.primary

    ScreenScaffold(
        title = "Vault",
        subtitle = "Encrypted, on-device only.",
        onBack = onLock,
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HackieSpacing.md),
        ) {
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
                },
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
                if (auditHistory.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.History,
                        title = "No audits yet",
                        body = "Audits you run will appear here.",
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            top = HackieSpacing.md,
                            bottom = HackieSpacing.xl,
                        ),
                        verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                    ) {
                        items(auditHistory, key = { it.timestamp.toString() + it.targetName }) { record ->
                            VaultAuditCard(record)
                        }
                    }
                }
            } else {
                if (trafficHistory.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.History,
                        title = "No traffic captured",
                        body = "Captured traffic will appear here.",
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            top = HackieSpacing.md,
                            bottom = HackieSpacing.xl,
                        ),
                        verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                    ) {
                        items(trafficHistory, key = { it.timestamp.toString() + it.targetName }) { record ->
                            VaultTrafficCard(record)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyVaultState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HackieSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun VaultAuditCard(
    record: com.example.rabit.data.security.TacticalStorageManager.AuditRecord
) {
    AppCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
        ) {
            IconTile(
                icon = Icons.Default.History,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = java.text.SimpleDateFormat("MMM dd • HH:mm", java.util.Locale.getDefault())
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
                text = "${record.findings.size} findings",
                background = if (record.findings.isEmpty()) Success.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                foreground = if (record.findings.isEmpty()) Success
                else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
fun VaultTrafficCard(
    record: com.example.rabit.data.security.TrafficStorageManager.TrafficRecord
) {
    AppCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
        ) {
            IconTile(
                icon = Icons.Default.History,
                color = if (record.wasCompromised) MaterialTheme.colorScheme.error
                else Success,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = java.text.SimpleDateFormat("MMM dd • HH:mm", java.util.Locale.getDefault())
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
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${record.packets.size} pkts",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (record.wasCompromised) {
                    LabelPill(
                        text = "Compromised",
                        background = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        foreground = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
