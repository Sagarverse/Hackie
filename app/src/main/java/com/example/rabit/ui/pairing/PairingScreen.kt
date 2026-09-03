package com.example.rabit.ui.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.components.AppCard
import com.example.rabit.ui.components.AppCardElevated
import com.example.rabit.ui.components.IconTile
import com.example.rabit.ui.components.LabelPill
import com.example.rabit.ui.components.PrimaryButton
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.components.SectionHeader
import com.example.rabit.ui.components.SettingsToggleRow
import com.example.rabit.ui.components.StatusDot
import com.example.rabit.ui.network.BluetoothMirrorViewModel
import com.example.rabit.ui.network.BluetoothShadowViewModel
import com.example.rabit.ui.theme.HackieSpacing
import com.example.rabit.ui.theme.Success
import com.example.rabit.ui.theme.Warning
import kotlinx.coroutines.launch

@Composable
fun PairingScreen(
    viewModel: MainViewModel,
    mirrorViewModel: BluetoothMirrorViewModel,
    shadowViewModel: BluetoothShadowViewModel,
    automationViewModel: com.example.rabit.ui.automation.AutomationViewModel,
    onConnected: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val isScanning by viewModel.isScanning.collectAsState()
    val isBluetoothConnected by viewModel.isBluetoothConnected.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val savedDevices by viewModel.savedDevices.collectAsState()
    val shadowDevices by shadowViewModel.discoveredDevices.collectAsState()
    val isShadowScanning by shadowViewModel.isShadowScanning.collectAsState()
    val isGhosting by shadowViewModel.isGhosting.collectAsState()
    val activeIdentity by shadowViewModel.activeIdentity.collectAsState()
    val isSpamming by shadowViewModel.isSpamming.collectAsState()
    val autoReconnect by viewModel.autoReconnectEnabled.collectAsState()
    val isHidConnected by viewModel.isHidConnected.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var showIdentityLab by remember { mutableStateOf(false) }
    var manualName by remember { mutableStateOf("") }
    var manualMac by remember { mutableStateOf("") }

    val transportMode by viewModel.hidTransportMode.collectAsState()
    val isRootAvail by viewModel.isRootAvailable.collectAsState()
    val usbState by viewModel.usbGadgetState.collectAsState()
    val isUsb = transportMode == com.example.rabit.ui.MainViewModel.HidTransportMode.USB

    // The name of the device the data layer currently reports as
    // connected. Empty string when not connected. Used by the device
    // list to badge the matching row as "Connected".
    val connectedName: String =
        (connectionState as? com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connected)
            ?.deviceName
            .orEmpty()

    // While we're in the "Connecting…" state, pick the address of the
    // device the user is most likely to have tapped — the last one in
    // the combined list. The data layer doesn't expose which row the
    // user clicked, so this is a best-effort highlight.
    val connectingAddress: String? = remember(
        connectionState, savedDevices, discoveredDevices,
    ) {
        if (connectionState !is com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connecting) {
            null
        } else {
            (savedDevices.map { it.address } + discoveredDevices.map { it.address })
                .lastOrNull()
        }
    }

    LaunchedEffect(isBluetoothConnected, isHidConnected) {
        if (isBluetoothConnected || isHidConnected) onConnected()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val bluetoothAdapter = remember(context) {
        try {
            (context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
        } catch (e: Exception) { null }
    }

    fun handleScanToggle() {
        if (isScanning || isShadowScanning) {
            viewModel.stopScan()
            shadowViewModel.stopShadowScan()
            return
        }
        // Make sure Bluetooth is on. If it's not, ask the system to turn
        // it on. The user still has to tap "Allow" in the dialog, but at
        // least they get the dialog instead of a silent no-op.
        if (bluetoothAdapter == null) {
            scope.launch {
                snackbarHostState.showSnackbar("This device has no Bluetooth adapter.")
            }
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            viewModel.requestEnableBluetooth(context)
        }
        viewModel.startScan()
        shadowViewModel.startShadowScan()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenScaffold(
            title = "Devices",
            subtitle = "Pair a Mac, PC, or Android to start.",
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
            // ── Connection status bar (always visible) ───────────────
            item {
                ConnectionStatusBar(
                    connectionState = connectionState,
                    isUsb = isUsb,
                    onCancel = { viewModel.disconnectKeyboard() },
                    onDisconnect = { viewModel.disconnectKeyboard() },
                )
            }

            // ── Quick action row (Scan / Jam) ────────────────────────
            item {
                QuickActionPanel(
                    isJamming = isSpamming,
                    isScanning = isScanning || isShadowScanning,
                    onToggleJam = { shadowViewModel.toggleBleSpam("Apple_Popup_Flood") },
                    onToggleScan = { handleScanToggle() },
                )
            }

            // ── Auto-reconnect toggle ────────────────────────────────
            item {
                AppCard {
                    SettingsToggleRow(
                        title = "Auto-reconnect",
                        subtitle = "Re-link to the last known device",
                        leading = {
                            IconTile(
                                icon = Icons.Default.Sync,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        checked = autoReconnect,
                        onCheckedChange = { viewModel.setAutoReconnectEnabled(it) },
                    )
                }
            }

            // ── Transport mode card (Bluetooth / USB) ────────────────
            item {
                TransportModeCard(
                    isUsb = isUsb,
                    isRootAvailable = isRootAvail,
                    usbStateText = when (usbState) {
                        is com.example.rabit.data.bluetooth.UsbHidGadgetManager.UsbGadgetState.Connected ->
                            "Gadget active — ready to type"
                        is com.example.rabit.data.bluetooth.UsbHidGadgetManager.UsbGadgetState.Configuring ->
                            "Configuring USB gadget…"
                        is com.example.rabit.data.bluetooth.UsbHidGadgetManager.UsbGadgetState.Error ->
                            "Error: ${(usbState as com.example.rabit.data.bluetooth.UsbHidGadgetManager.UsbGadgetState.Error).message}"
                        is com.example.rabit.data.bluetooth.UsbHidGadgetManager.UsbGadgetState.Disconnected ->
                            "Gadget inactive"
                        is com.example.rabit.data.bluetooth.UsbHidGadgetManager.UsbGadgetState.NotAvailable ->
                            "USB gadget not supported"
                    },
                    usbStateTone = when (usbState) {
                        is com.example.rabit.data.bluetooth.UsbHidGadgetManager.UsbGadgetState.Connected -> Success
                        is com.example.rabit.data.bluetooth.UsbHidGadgetManager.UsbGadgetState.Configuring -> Warning
                        is com.example.rabit.data.bluetooth.UsbHidGadgetManager.UsbGadgetState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    onPickBluetooth = {
                        viewModel.setHidTransportMode(
                            com.example.rabit.ui.MainViewModel.HidTransportMode.BLUETOOTH
                        )
                    },
                    onPickUsb = {
                        if (isRootAvail) {
                            viewModel.setHidTransportMode(
                                com.example.rabit.ui.MainViewModel.HidTransportMode.USB
                            )
                        }
                    },
                )
            }

            // ── Active identity card ─────────────────────────────────
            item {
                ActiveIdentityCard(
                    identity = activeIdentity,
                    isGhosting = isGhosting,
                    onReset = { shadowViewModel.stopGhosting() },
                )
            }

            // ── Saved & discovered devices ───────────────────────────
            item {
                SectionHeader(title = "Available devices")
            }
            val noDevicesYet =
                savedDevices.isEmpty() && discoveredDevices.isEmpty()
            if (noDevicesYet) {
                item {
                    if (isScanning || isShadowScanning) {
                        ScanningPlaceholder()
                    } else {
                        AppCard {
                            Text(
                                text = "No devices found yet. Tap Scan to look again.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = HackieSpacing.xs),
                            )
                        }
                    }
                }
            } else {
                items(savedDevices, key = { it.address }) { device ->
                    TargetDeviceCard(
                        name = device.name,
                        address = device.address,
                        isPaired = true,
                        isConnecting = connectingAddress == device.address,
                        isThisDeviceConnected = connectionState is
                            com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connected &&
                            device.name == connectedName,
                        onClick = {
                            shadowViewModel.stopGhosting()
                            shadowViewModel.stopSpamming()
                            viewModel.connectToDevice(device.address)
                        },
                        onDisconnect = { viewModel.disconnectKeyboard() },
                    )
                }
                items(discoveredDevices.toList(), key = { it.address }) { device ->
                    if (savedDevices.none { it.address == device.address }) {
                        TargetDeviceCard(
                            name = device.name ?: "Unknown device",
                            address = device.address,
                            isPaired = false,
                            isConnecting = connectingAddress == device.address,
                            isThisDeviceConnected = connectionState is
                                com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connected &&
                                device.name == connectedName,
                            onClick = {
                                shadowViewModel.stopGhosting()
                                shadowViewModel.stopSpamming()
                                viewModel.connectToDevice(device.address)
                            },
                            onDisconnect = { viewModel.disconnectKeyboard() },
                        )
                    }
                }
            }

            // ── Identity lab (collapsible) ───────────────────────────
            item {
                SectionHeader(
                    title = "Identity",
                    action = {
                        androidx.compose.material3.TextButton(onClick = { showIdentityLab = !showIdentityLab }) {
                            Text(
                                if (showIdentityLab) "Close" else "Customize",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
            if (showIdentityLab) {
                item {
                    IdentityLabPanel(
                        name = manualName,
                        mac = manualMac,
                        onNameChange = { manualName = it },
                        onMacChange = { manualMac = it },
                        onDeploy = { shadowViewModel.toggleGhostMode(manualName) },
                        shadowDevices = shadowDevices,
                        onClone = { device ->
                            manualName = device.name
                            manualMac = device.address
                            shadowViewModel.toggleGhostMode(device.name)
                        },
                        isJamming = isSpamming,
                        onToggleJam = { shadowViewModel.toggleBleSpam("Apple_Popup_Flood") },
                    )
                }
            }

            item { Spacer(Modifier.height(HackieSpacing.lg)) }
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(androidx.compose.ui.Alignment.BottomCenter)
            .padding(16.dp),
    ) { data -> Snackbar(snackbarData = data) }
    }
}

// ── Quick action row (Scan + Jam) ────────────────────────────────────────

@Composable
private fun QuickActionPanel(
    isJamming: Boolean,
    isScanning: Boolean,
    onToggleJam: () -> Unit,
    onToggleScan: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
    ) {
        ActionCard(
            title = "Scan",
            subtitle = if (isScanning) "Scanning…" else "Idle",
            icon = Icons.Default.Radar,
            isActive = isScanning,
            activeColor = MaterialTheme.colorScheme.primary,
            onClick = onToggleScan,
            modifier = Modifier.weight(1f),
        )
        ActionCard(
            title = "Jam",
            subtitle = if (isJamming) "Flooding" else "Dormant",
            icon = Icons.Default.CellTower,
            isActive = isJamming,
            activeColor = MaterialTheme.colorScheme.error,
            onClick = onToggleJam,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCardElevated(
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(HackieSpacing.md),
            verticalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconTile(
                    icon = icon,
                    background = if (isActive) activeColor.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                    color = if (isActive) activeColor
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusDot(color = if (isActive) activeColor else MaterialTheme.colorScheme.outline)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) activeColor
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Transport mode (Bluetooth / USB) ─────────────────────────────────────

@Composable
private fun TransportModeCard(
    isUsb: Boolean,
    isRootAvailable: Boolean,
    usbStateText: String,
    usbStateTone: Color,
    onPickBluetooth: () -> Unit,
    onPickUsb: () -> Unit,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(HackieSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                ) {
                    IconTile(
                        icon = if (isUsb) Icons.Default.Usb else Icons.Default.Bluetooth,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Column {
                        Text(
                            text = "Transport",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (isUsb) "USB HID" else "Bluetooth Classic",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                LabelPill(
                    text = if (isRootAvailable) "Root" else "No root",
                    background = if (isRootAvailable) Success.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    foreground = if (isRootAvailable) Success
                    else MaterialTheme.colorScheme.error,
                )
            }

            // Segmented control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                TransportSegment(
                    label = "Bluetooth",
                    icon = Icons.Default.Bluetooth,
                    selected = !isUsb,
                    onClick = onPickBluetooth,
                    modifier = Modifier.weight(1f),
                )
                TransportSegment(
                    label = "USB",
                    icon = Icons.Default.Usb,
                    selected = isUsb,
                    enabled = isRootAvailable,
                    onClick = onPickUsb,
                    modifier = Modifier.weight(1f),
                )
            }

            if (isUsb) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
                ) {
                    StatusDot(color = usbStateTone)
                    Text(
                        text = usbStateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = usbStateTone,
                    )
                }
                Text(
                    text = "Connect a USB cable to the target — the phone acts as a keyboard and mouse. No pairing required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TransportSegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val container = if (selected) MaterialTheme.colorScheme.primary
    else Color.Transparent
    val content = if (selected) MaterialTheme.colorScheme.onPrimary
    else if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(vertical = HackieSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(HackieSpacing.xs))
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = content)
        }
    }
}

// ── Active identity ──────────────────────────────────────────────────────

@Composable
private fun ActiveIdentityCard(
    identity: String,
    isGhosting: Boolean,
    onReset: () -> Unit,
) {
    AppCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
        ) {
            IconTile(
                icon = if (isGhosting) Icons.Default.Fingerprint else Icons.Default.Hardware,
                background = if (isGhosting) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                color = if (isGhosting) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Active identity",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (isGhosting) identity else "Original hardware",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isGhosting) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
            if (isGhosting) {
                IconButton(onClick = onReset) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Discovered / paired target ──────────────────────────────────────────

@Composable
private fun TargetDeviceCard(
    name: String,
    address: String,
    isPaired: Boolean,
    isConnecting: Boolean,
    isThisDeviceConnected: Boolean,
    onClick: () -> Unit,
    onDisconnect: () -> Unit,
) {
    // Three explicit visual states so the user always knows what tapping
    // a device will do, and what the app is doing right now.
    val (leadingColor, leadingBackground) = when {
        isThisDeviceConnected -> Success to Success.copy(alpha = 0.15f)
        isPaired -> MaterialTheme.colorScheme.primary to
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        isConnecting -> MaterialTheme.colorScheme.primary to
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant to
            MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val nameColor = when {
        isThisDeviceConnected -> Success
        isConnecting -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val subtitle = when {
        isThisDeviceConnected -> "Connected"
        isConnecting -> "Connecting…"
        isPaired -> "Tap to connect"
        else -> "Tap to pair"
    }

    AppCard(
        onClick = if (isConnecting || isThisDeviceConnected) null else onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
        ) {
            IconTile(
                icon = deviceIcon(name),
                background = leadingBackground,
                color = leadingColor,
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = nameColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isThisDeviceConnected) {
                        LabelPill(
                            text = "Connected",
                            background = Success.copy(alpha = 0.15f),
                            foreground = Success,
                        )
                    } else if (isPaired) {
                        LabelPill(
                            text = "Saved",
                            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            foreground = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isThisDeviceConnected || isConnecting) nameColor
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isThisDeviceConnected || isConnecting) FontWeight.Medium
                    else FontWeight.Normal,
                )
                Text(
                    text = address,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when {
                isConnecting -> {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                isThisDeviceConnected -> {
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            imageVector = Icons.Default.BluetoothDisabled,
                            contentDescription = "Disconnect $name",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                else -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Connection status bar (always visible) ──────────────────────────────

@Composable
private fun ConnectionStatusBar(
    connectionState: com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState,
    isUsb: Boolean,
    onCancel: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val label: String
    val sub: String?
    val accent: androidx.compose.ui.graphics.Color
    val isError: Boolean
    when (connectionState) {
        is com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connected -> {
            label = "Connected"
            sub = connectionState.deviceName
            accent = Success
            isError = false
        }
        is com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connecting -> {
            label = "Connecting…"
            sub = "Pairing with the target device"
            accent = MaterialTheme.colorScheme.primary
            isError = false
        }
        is com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Disconnected -> {
            label = "Not connected"
            sub = if (isUsb) "Connect a USB cable to the target"
            else "Pick a device below to pair"
            accent = MaterialTheme.colorScheme.onSurfaceVariant
            isError = false
        }
    }

    AppCardElevated {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
        ) {
            if (connectionState is com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connecting) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = accent,
                )
            } else {
                StatusDot(color = accent, size = 10.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                )
                if (sub != null) {
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when (connectionState) {
                is com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connecting -> {
                    androidx.compose.material3.TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                }
                is com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connected -> {
                    androidx.compose.material3.TextButton(onClick = onDisconnect) {
                        Text("Disconnect")
                    }
                }
                else -> Unit
            }
        }
    }
}

// Small holder so the destructuring above stays readable. We use a
// regular class (not a data class) so we can name the component
// operators without colliding with the ones data classes generate
// automatically.
private class StatusBarCopy<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
) {
    operator fun component1(): A = first
    operator fun component2(): B = second
    operator fun component3(): C = third
    operator fun component4(): D = fourth
    operator fun component5(): E = fifth
}// ── In-progress scan placeholder ────────────────────────────────────────

@Composable
private fun ScanningPlaceholder() {
    AppCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Column {
                Text(
                    text = "Scanning nearby devices…",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Make sure the target's Bluetooth is on and discoverable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun deviceIcon(name: String): ImageVector {
    val lower = name.lowercase()
    return when {
        lower.contains("mac") || lower.contains("laptop") || lower.contains("pc") -> Icons.Default.Laptop
        lower.contains("phone") || lower.contains("pixel") || lower.contains("galaxy") -> Icons.Default.Phone
        else -> Icons.Default.Bluetooth
    }
}

// ── Identity lab panel (collapsible) ─────────────────────────────────────

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun IdentityLabPanel(
    name: String,
    mac: String,
    onNameChange: (String) -> Unit,
    onMacChange: (String) -> Unit,
    onDeploy: () -> Unit,
    shadowDevices: List<com.example.rabit.ui.network.ShadowDevice>,
    onClone: (com.example.rabit.ui.network.ShadowDevice) -> Unit,
    isJamming: Boolean,
    onToggleJam: () -> Unit,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(HackieSpacing.md)) {
            Text(
                text = "Cloned identity lets the phone advertise a different name and MAC. Use only on networks you own.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Signal disruption toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Signal disruption",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Disconnect other nearby nodes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isJamming,
                    onCheckedChange = { onToggleJam() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.error,
                    ),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Nearby identities
            Column(verticalArrangement = Arrangement.spacedBy(HackieSpacing.xs)) {
                Text(
                    text = "Nearby identities",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (shadowDevices.isEmpty()) {
                    Text(
                        text = "Searching for nearby signatures…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
                    ) {
                        shadowDevices.forEach { device ->
                            AppCard(
                                onClick = { onClone(device) },
                                contentPadding = PaddingValues(
                                    horizontal = HackieSpacing.sm,
                                    vertical = HackieSpacing.xs,
                                ),
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Devices,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = device.name.take(10),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = device.address.take(8),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Manual override
            Column(verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm)) {
                Text(
                    text = "Manual override",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Emulated name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                androidx.compose.material3.OutlinedTextField(
                    value = mac,
                    onValueChange = onMacChange,
                    label = { Text("Emulated MAC") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PrimaryButton(
                    text = "Deploy identity",
                    onClick = onDeploy,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.CheckCircle,
                )
            }
        }
    }
}
