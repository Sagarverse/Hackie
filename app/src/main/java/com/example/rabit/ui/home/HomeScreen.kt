package com.example.rabit.ui.home

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.domain.model.TargetOs
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.settings.SettingsViewModel
import com.example.rabit.ui.components.AppCard
import com.example.rabit.ui.components.IconTile
import com.example.rabit.ui.components.InfoPill
import com.example.rabit.ui.components.LabelPill
import com.example.rabit.ui.components.MediaMiniPlayer
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.components.StatusDot
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.Graphite
import com.example.rabit.ui.theme.HackieSpacing
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver

import com.example.rabit.ui.theme.hackieColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onOpenHelper: () -> Unit,
    onOpenKeyboard: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenMacros: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenWebBridge: () -> Unit,
) {
    val colors = hackieColors()
    val connectionState by viewModel.connectionState.collectAsState()
    val knownWorkstations by viewModel.knownWorkstations.collectAsState()
    val isHelperConnected by viewModel.isHelperConnected.collectAsState()
    val helperName by viewModel.helperDeviceName.collectAsState()
    val helperMac by viewModel.helperDeviceMac.collectAsState()
    val helperBaseUrl by viewModel.helperBaseUrl.collectAsState()
    val helperIp by viewModel.helperDeviceIp.collectAsState()
    val helperConnectionStatus by viewModel.helperConnectionStatus.collectAsState()
    val nowPlayingTitle by viewModel.nowPlayingTitle.collectAsState()
    val nowPlayingArtist by viewModel.nowPlayingArtist.collectAsState()
    val nowPlayingArtworkBase64 by viewModel.nowPlayingArtworkBase64.collectAsState()
    
    val targetOs by settingsViewModel.targetOs.collectAsState()
    val typingSpeed by viewModel.typingSpeed.collectAsState()

    val artwork = remember(nowPlayingArtworkBase64) {
        try {
            nowPlayingArtworkBase64?.let {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }

    val bluetoothConnectedName =
        (connectionState as? HidDeviceManager.ConnectionState.Connected)?.deviceName
    val bluetoothConnectedMac = if (bluetoothConnectedName != null) {
        knownWorkstations.firstOrNull { it.name == bluetoothConnectedName }?.address
            ?: knownWorkstations.firstOrNull()?.address
    } else {
        null
    }

    val resolvedHelperHost = remember(helperBaseUrl, helperIp) {
        val fromEndpoint = runCatching {
            val host = Uri.parse(helperBaseUrl).host.orEmpty()
            host.ifBlank { null }
        }.getOrNull()
        fromEndpoint ?: helperIp.takeIf { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) }
    }

    val online = bluetoothConnectedName != null || isHelperConnected
    val deviceDisplayName = when {
        bluetoothConnectedName != null -> bluetoothConnectedName
        isHelperConnected -> helperName.ifBlank { "Desktop" }
        else -> null
    }

    ScreenScaffold(
        title = "Hackie",
        subtitle = if (online) "Command Center Active" else "Standing By",
    ) { _ ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HackieSpacing.md),
            contentPadding = PaddingValues(
                top = HackieSpacing.sm,
                bottom = HackieSpacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(HackieSpacing.md),
        ) {
            // ── Premium Hero Card ─────────────────────────────────────
            item {
                PremiumHeroCard(
                    online = online,
                    deviceName = deviceDisplayName,
                    helperStatus = helperConnectionStatus,
                    bluetoothName = bluetoothConnectedName,
                    onPrimaryAction = if (online) onOpenKeyboard else onOpenHelper,
                    primaryLabel = if (online) "Open Keyboard" else "Connect Device",
                )
            }

            // ── OS Switcher & Speed Control ─────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HackieSpacing.md)
                ) {
                    // OS Switcher
                    PremiumGlassCard(modifier = Modifier.weight(1.5f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Computer, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("TARGET OS", color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TargetOs.entries.forEach { os ->
                                    val isSelected = targetOs == os
                                    Surface(
                                        onClick = { settingsViewModel.setTargetOs(os) },
                                        color = if (isSelected) AccentBlue.copy(alpha = 0.2f) else Color.Transparent,
                                        contentColor = if (isSelected) AccentBlue else Silver,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(id = os.iconRes),
                                                contentDescription = os.displayName,
                                                modifier = Modifier.size(18.dp),
                                                tint = if (isSelected) AccentBlue else Silver
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Speed Control
                    PremiumGlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("INJECT", color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            val speeds = listOf("Slow", "Normal", "Fast", "Turbo")
                            val nextSpeed = speeds[(speeds.indexOf(typingSpeed) + 1) % speeds.size]
                            Surface(
                                onClick = { viewModel.setTypingSpeed(nextSpeed) },
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha=0.3f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = typingSpeed,
                                        color = Platinum,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Target Password Input ───────────────────────────────
            item {
                var targetPassword by rememberSaveable { mutableStateOf("") }
                var passwordVisible by rememberSaveable { mutableStateOf(false) }
                
                Surface(
                    color = colors.surface1.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, colors.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = targetPassword,
                        onValueChange = { 
                            targetPassword = it 
                            // Could save to DataStore/ViewModel here, or just keep in memory for macro use
                        },
                        label = { Text("Target Device Password", color = colors.textSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            androidx.compose.material3.IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Password Visibility",
                                    tint = colors.textSecondary
                                )
                            }
                        },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = colors.outline,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Silver
                        )
                    )
                }
            }

            // ── Quick Actions Grid ───────────────────────────────────
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = HackieSpacing.xs, top = HackieSpacing.xs),
                )
            }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                ) {
                    val macPassword by viewModel.macPassword.collectAsState()
                    PremiumActionTile(
                        icon = Icons.Default.Lock,
                        label = "Lock Target",
                        onClick = { viewModel.sendSystemShortcut(MainViewModel.SystemShortcut.LOCK_SCREEN) },
                        modifier = Modifier.weight(1f)
                    )
                    PremiumActionTile(
                        icon = Icons.Default.LockOpen,
                        label = "Unlock Target",
                        onClick = { viewModel.sendMacPassword(macPassword, preEnter = true, postEnter = true) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(HackieSpacing.sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                ) {
                    PremiumActionTile(
                        icon = Icons.Default.Bolt,
                        label = "Macros",
                        onClick = onOpenMacros,
                        modifier = Modifier.weight(1f),
                    )
                    PremiumActionTile(
                        icon = Icons.Default.SmartToy,
                        label = "AI Assistant",
                        onClick = onOpenAssistant,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(HackieSpacing.sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                ) {
                    PremiumActionTile(
                        icon = Icons.Default.AccountTree,
                        label = "Web Bridge",
                        onClick = onOpenWebBridge,
                        modifier = Modifier.weight(1f),
                    )
                    PremiumActionTile(
                        icon = Icons.Default.ContentPaste,
                        label = "Vault",
                        onClick = onOpenVault,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Now playing mini bar ─────────────────────────────────
            if (online) {
                item {
                    PremiumGlassCard {
                        MediaMiniPlayer(
                            title = nowPlayingTitle.ifBlank { "Nothing playing" },
                            artist = nowPlayingArtist.ifBlank { null },
                            isPlaying = nowPlayingTitle.isNotBlank() && nowPlayingTitle != "Nothing playing",
                            onTogglePlay = { viewModel.sendMediaPlayPause() },
                            onNext = { viewModel.sendMediaNextTrack() },
                            onPrevious = { viewModel.sendMediaPreviousTrack() },
                        )
                    }
                }
            }

            // ── Connection Details & Diagnostics ──────────────────────
            item {
                Text(
                    text = "System Diagnostics",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = HackieSpacing.xs, top = HackieSpacing.sm),
                )
            }
            item {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        DiagnosticRow(
                            icon = Icons.Default.Devices,
                            title = "Connection Mode",
                            subtitle = if (online && bluetoothConnectedName != null) "Bluetooth HID" 
                                       else if (online) "Helper Agent" else "Disconnected",
                            onClick = onOpenHelper,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = HackieSpacing.xxs),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        DiagnosticRow(
                            icon = Icons.Default.Sensors,
                            title = "Rescan Network",
                            subtitle = "Re-detect helper on local Wi-Fi",
                            onClick = { viewModel.discoverHelperOnLocalWifi() },
                        )
                    }
                }
            }
        }
    }
}

// ── Components ─────────────────────────────────────────────────────────

@Composable
private fun PremiumHeroCard(
    online: Boolean,
    deviceName: String?,
    helperStatus: String,
    bluetoothName: String?,
    onPrimaryAction: () -> Unit,
    primaryLabel: String,
) {
    val gradientColors = if (online) {
        listOf(Color(0xFF1E3A8A), Color(0xFF0F172A)) // Blue to dark slate
    } else {
        listOf(Color(0xFF334155), Color(0xFF0F172A)) // Slate to darker slate
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onPrimaryAction() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Box(modifier = Modifier.background(Brush.linearGradient(gradientColors))) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(color = if (online) Color(0xFF10B981) else Color(0xFF94A3B8))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = if (online) "SYSTEM ONLINE" else "OFFLINE",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (online) Color(0xFF10B981) else Color(0xFF94A3B8),
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = deviceName ?: "No Target Linked",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.sagar.rabit.R.drawable.rabit_pro_icon),
                        contentDescription = "Device",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                Text(
                    text = when {
                        online && bluetoothName != null -> "Connected via Bluetooth HID"
                        online -> helperStatus.ifBlank { "Helper connected" }
                        else -> "Pair a Mac, PC, or Android to start controlling."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                )
            }
        }
    }
}

@Composable
private fun PremiumGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Graphite.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        content()
    }
}

@Composable
private fun PremiumActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumGlassCard(modifier = modifier.clickable { onClick() }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF3B82F6).copy(alpha = 0.2f), Color.Transparent))),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
            }
            Text(
                text = label,
                color = Platinum,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DiagnosticRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = HackieSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon = icon, size = 36.dp, iconSize = 18.dp)
        Spacer(Modifier.size(HackieSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
