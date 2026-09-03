package com.example.rabit.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rabit.data.security.NeuralAuditEngine
import com.example.rabit.ui.components.AppCard
import com.example.rabit.ui.components.IconTile
import com.example.rabit.ui.components.LabelPill
import com.example.rabit.ui.components.PrimaryButton
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.components.SectionHeader
import com.example.rabit.ui.theme.HackieSpacing
import com.example.rabit.ui.theme.Success

/**
 * Security auditor: scan a device for weaknesses and review past audits.
 * Tabs: Live audit, History.
 */
@Composable
fun SecurityAuditorScreen(
    viewModel: SecurityAuditorViewModel,
    onBack: () -> Unit
) {
    val findings by viewModel.findings.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showBriefing by remember { mutableStateOf(true) }
    val tabs = listOf("Live audit", "History")
    val accent = MaterialTheme.colorScheme.primary

    ScreenScaffold(
        title = "Security auditor",
        subtitle = "Scan a device for known weaknesses.",
        onBack = onBack,
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HackieSpacing.md),
        ) {
            if (showBriefing) {
                AppCard {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "About this tool",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Looks for open windows and weaknesses in any phone you connect, and tells you if it is safe or needs fixing.",
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
                LiveAuditContent(viewModel)
            } else {
                HistoryContent(viewModel)
            }
        }
    }
}

@Composable
private fun LiveAuditContent(viewModel: SecurityAuditorViewModel) {
    val findings by viewModel.findings.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val progress by viewModel.scanProgress.collectAsState()
    var showReportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = HackieSpacing.md, bottom = HackieSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(HackieSpacing.md),
    ) {
        item {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(HackieSpacing.md)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                    ) {
                        IconTile(
                            icon = Icons.Default.Shield,
                            color = if (findings.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                            else Success,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Target security posture",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (isScanning) "Analysis in progress…"
                                else "${findings.size} finding${if (findings.size == 1) "" else "s"} detected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (findings.isNotEmpty() && !isScanning) {
                            IconButton(onClick = { showReportDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Report",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    if (isScanning) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainer,
                        )
                    } else {
                        PrimaryButton(
                            text = "Run security audit",
                            onClick = { viewModel.runFullAudit() },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.Radar,
                        )
                    }
                }
            }
        }

        if (findings.isNotEmpty()) {
            item { SectionHeader(title = "Findings") }
            items(findings, key = { it.title.hashCode() }) { finding ->
                FindingCard(finding)
            }
        } else if (!isScanning) {
            item {
                AppCard {
                    Text(
                        text = "No findings yet. Run a scan to identify risks.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = HackieSpacing.xs),
                    )
                }
            }
        }
    }

    if (showReportDialog) {
        val reportText = viewModel.getReportText()
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Security report") },
            text = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .height(300.dp)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Box(modifier = Modifier.padding(HackieSpacing.sm)) {
                        Text(
                            text = reportText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, reportText)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share report"))
                        showReportDialog = false
                    },
                ) { Text("Share") }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Close") }
            },
        )
    }
}

@Composable
private fun HistoryContent(viewModel: SecurityAuditorViewModel) {
    val history by viewModel.auditHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = HackieSpacing.md),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HackieSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${history.size} saved",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (history.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearTacticalHistory() }) {
                    Text("Clear all", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (history.isEmpty()) {
            AppCard {
                Text(
                    text = "No past audits. Run a scan to save one here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = HackieSpacing.xs),
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = HackieSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
            ) {
                items(history, key = { it.timestamp.toString() + it.targetName }) { record ->
                    HistoryRecordCard(record)
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(
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
                    text = java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault())
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
private fun FindingCard(finding: NeuralAuditEngine.Finding) {
    val severityColor = when (finding.severity) {
        NeuralAuditEngine.Severity.CRITICAL -> MaterialTheme.colorScheme.error
        NeuralAuditEngine.Severity.HIGH -> Color(0xFFF97316)
        NeuralAuditEngine.Severity.MEDIUM -> Color(0xFFEAB308)
        NeuralAuditEngine.Severity.LOW -> MaterialTheme.colorScheme.primary
        NeuralAuditEngine.Severity.INFO -> Success
    }

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(severityColor)
                        .padding(top = 4.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = finding.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = finding.category.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = severityColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = finding.severity.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = severityColor.copy(alpha = 0.8f),
                )
            }

            Text(
                text = finding.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Remediation box
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(HackieSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handyman,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "How to fix",
                            style = MaterialTheme.typography.labelSmall,
                            color = Success,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = finding.remediation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
