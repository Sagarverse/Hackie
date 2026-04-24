package com.example.rabit.ui.settings

import android.Manifest
import android.widget.Toast
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.example.rabit.data.secure.EncryptionManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.*
import com.example.rabit.ui.automation.AutomationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel, 
    settingsViewModel: SettingsViewModel,
    automationViewModel: AutomationViewModel,
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToPasswordManager: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val biometricAuthenticator = remember(activity) { activity?.let { com.example.rabit.data.secure.BiometricAuthenticator(it) } }
    val clipboardManager = LocalClipboardManager.current
    val geminiSettingsViewModel: GeminiSettingsViewModel = viewModel(
        factory = androidx.lifecycle.viewmodel.viewModelFactory {
            addInitializer(GeminiSettingsViewModel::class) {
                GeminiSettingsViewModel(context.applicationContext as android.app.Application)
            }
        }
    )
    
    val autoReconnect by viewModel.autoReconnectEnabled.collectAsState()
    val typingSpeed by viewModel.typingSpeed.collectAsState()
    val notificationSync by viewModel.notificationSyncEnabled.collectAsState()
    val autoPush by viewModel.autoPushEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val trackpadSensitivity by viewModel.trackpadSensitivity.collectAsState()
    val proximityAutoUnlockEnabled by viewModel.proximityAutoUnlockEnabled.collectAsState()
    val proximityNearRssi by viewModel.proximityNearRssi.collectAsState()
    val proximityFarRssi by viewModel.proximityFarRssi.collectAsState()
    val proximityCooldownSec by viewModel.proximityCooldownSec.collectAsState()
    val proximityRequirePhoneUnlock by viewModel.proximityRequirePhoneUnlock.collectAsState()
    val proximityTargetAddress by viewModel.proximityTargetAddress.collectAsState()
    val proximityLiveRssi by viewModel.proximityLiveRssi.collectAsState()
    val proximityLiveDistanceMeters by viewModel.proximityLiveDistanceMeters.collectAsState()
    val proximityUnlockArmed by viewModel.proximityUnlockArmed.collectAsState()
    val proximityMacLockStateGuess by viewModel.proximityMacLockStateGuess.collectAsState()


    val biometricEnabled by viewModel.biometricLockEnabled.collectAsState()
    val biometricMacAutofillEnabled by viewModel.biometricMacAutofillEnabled.collectAsState()
    val macAutofillPreEnter by viewModel.macAutofillPreEnter.collectAsState()
    val macAutofillPostEnter by viewModel.macAutofillPostEnter.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val shakeToDisconnect by viewModel.shakeToDisconnectEnabled.collectAsState()
    val stealthMode by viewModel.stealthModeEnabled.collectAsState()
    val featureWebBridgeVisible by viewModel.featureWebBridgeVisible.collectAsState()
    val featureAutomationVisible by viewModel.featureAutomationVisible.collectAsState()
    val featureAssistantVisible by viewModel.featureAssistantVisible.collectAsState()
    val featureSnippetsVisible by viewModel.featureSnippetsVisible.collectAsState()
    val featureShortcutsVisible by viewModel.featureShortcutsVisible.collectAsState()
    val featureWakeOnLanVisible by viewModel.featureWakeOnLanVisible.collectAsState()
    val featureSshTerminalVisible by viewModel.featureSshTerminalVisible.collectAsState()
    
    val prefs = remember { context.getSharedPreferences("rabit_prefs", android.content.Context.MODE_PRIVATE) }


    var dndOnConnect by remember { mutableStateOf(prefs.getBoolean("auto_dnd_on_connect", false)) }
    var wakeLockOnConnect by remember { mutableStateOf(prefs.getBoolean("auto_wake_lock_on_connect", false)) }

    val encryptionManager = remember { EncryptionManager(context) }
    var e2eeEnabled by remember { mutableStateOf(encryptionManager.isEnabled) }

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showProximityCalibrationDialog by remember { mutableStateOf(false) }
    var showProximityTargetDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    val liveDistanceLabel = if (proximityLiveDistanceMeters > 0f) {
        String.format("%.1f m", proximityLiveDistanceMeters)
    } else {
        "Searching..."
    }
    val lockStateLabel = when (proximityMacLockStateGuess) {
        "LIKELY_LOCKED" -> "Likely locked"
        "LIKELY_UNLOCKED" -> "Likely unlocked"
        else -> "Unknown"
    }

    val hasBluetoothConnectPermission = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val bondedDevices = remember(hasBluetoothConnectPermission) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (!hasBluetoothConnectPermission) {
            emptyList()
        } else {
            try { manager.adapter?.bondedDevices?.toList().orEmpty() } catch (e: Exception) { emptyList() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

            GeminiApiSettingsSection(viewModel = geminiSettingsViewModel)
            
            // ─── Security & Access ───
            PremiumSectionHeader("SECURITY & ACCESS")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Biometric Lock",
                    subtitle = "Require fingerprint or face ID to open app",
                    icon = Icons.Default.Fingerprint,
                    iconColor = SuccessGreen,
                    checked = biometricEnabled,
                    onCheckedChange = { viewModel.setBiometricLockEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Stealth History",
                    subtitle = "Auto-clear session data on app exit",
                    icon = Icons.Default.VisibilityOff,
                    iconColor = AccentOrange,
                    checked = stealthMode,
                    onCheckedChange = { viewModel.setStealthModeEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Biometric Password Autofill",
                    subtitle = "Require fingerprint/face before typing Mac password",
                    icon = Icons.Default.Password,
                    iconColor = AccentTeal,
                    checked = biometricMacAutofillEnabled,
                    onCheckedChange = { viewModel.setBiometricMacAutofillEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Press Enter Before Typing",
                    subtitle = "Wake login field before sending password",
                    icon = Icons.Default.Keyboard,
                    iconColor = AccentBlue,
                    checked = macAutofillPreEnter,
                    onCheckedChange = { viewModel.setMacAutofillPreEnter(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Press Enter After Typing",
                    subtitle = "Submit credentials after autofill",
                    icon = Icons.Default.Key,
                    iconColor = SuccessGreen,
                    checked = macAutofillPostEnter,
                    onCheckedChange = { viewModel.setMacAutofillPostEnter(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (connectionState is com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connected) "Mac connection detected" else "Connect to Mac to use autofill",
                        color = Silver,
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = {
                            val dispatchAutofill = {
                                val error = viewModel.sendStoredMacPasswordToHost()
                                val message = error ?: "Password sent securely via HID."
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }

                            if (biometricMacAutofillEnabled) {
                                if (biometricAuthenticator?.isBiometricAvailable() == true) {
                                    biometricAuthenticator.authenticate(
                                        title = "Hackie Mac Autofill",
                                        subtitle = "Authenticate to type your Mac password",
                                        onSuccess = dispatchAutofill,
                                        onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                    )
                                } else {
                                    Toast.makeText(context, "Biometric authentication is not available on this device.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                dispatchAutofill()
                            }
                        },
                        enabled = connectionState is com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connected,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Authenticate & Autofill Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // ─── Connection ───
            PremiumSectionHeader("CONNECTION")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Auto Reconnect",
                    subtitle = "Automatically connect to last device",
                    icon = Icons.Default.Sync,
                    iconColor = AccentBlue,
                    checked = autoReconnect,
                    onCheckedChange = { viewModel.setAutoReconnectEnabled(it) }
                )
            }

            // ─── Input & Controls ───
            PremiumSectionHeader("INPUT & CONTROLS")
            PremiumGlassCard {
                SettingsClickItem(
                    title = "Typing Speed",
                    subtitle = "Current: $typingSpeed",
                    icon = Icons.Default.Speed,
                    iconColor = AccentPurple,
                    onClick = { showSpeedDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Shake to Disconnect",
                    subtitle = "Physically shake phone to end Mac connection",
                    icon = Icons.Default.Vibration,
                    iconColor = AccentPink,
                    checked = shakeToDisconnect,
                    onCheckedChange = { viewModel.setShakeToDisconnectEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Haptic Feedback",
                    subtitle = "Vibrate on key press & button taps",
                    icon = Icons.Default.Vibration,
                    iconColor = AccentPink,
                    checked = vibrationEnabled,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                
                // Haptic Tactile Engine
                val currentHaptic by viewModel.hapticPreset.collectAsState()
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Tactile Engine", color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Soft", "Mechanical", "Sharp").forEach { preset ->
                            val isSelected = currentHaptic == preset
                            Surface(
                                onClick = { viewModel.setHapticPreset(preset) },
                                color = if (isSelected) AccentTeal.copy(alpha = 0.15f) else Graphite.copy(alpha = 0.3f),
                                contentColor = if (isSelected) AccentTeal else Silver,
                                shape = RoundedCornerShape(8.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, AccentTeal.copy(alpha = 0.5f)) else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                // Trackpad Sensitivity
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconBadge(icon = Icons.Default.TouchApp, backgroundColor = AccentTeal)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trackpad Sensitivity", color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text("Adjust cursor speed: ${String.format("%.1f", trackpadSensitivity)}x", color = Silver, fontSize = 12.sp)
                        }
                    }
                    Slider(
                        value = trackpadSensitivity,
                        onValueChange = { viewModel.setTrackpadSensitivity(it) },
                        valueRange = 0.5f..3.0f,
                        steps = 4,
                        modifier = Modifier.padding(start = 46.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = AccentTeal,
                            activeTrackColor = AccentTeal
                        )
                    )
                }
            }

            // ─── Clipboard & Sync ───
            PremiumSectionHeader("CLIPBOARD & SYNC")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Clipboard Auto-Push",
                    subtitle = "Automatically push copied text to Mac",
                    icon = Icons.Default.ContentPasteGo,
                    iconColor = SuccessGreen,
                    checked = autoPush,
                    onCheckedChange = { viewModel.setAutoPushEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Notification Sync",
                    subtitle = "Type phone notifications to Mac",
                    icon = Icons.Default.Notifications,
                    iconColor = AccentPink,
                    checked = notificationSync,
                    onCheckedChange = { viewModel.setNotificationSyncEnabled(it) }
                )
            }



            // ─── Macros ───
            PremiumSectionHeader("MACROS")
            PremiumGlassCard {
                SettingsClickItem(
                    title = "Export Macros",
                    subtitle = "Copy all custom macros as JSON",
                    icon = Icons.Default.Upload,
                    iconColor = AccentBlue,
                    onClick = {
                        val json = automationViewModel.exportMacrosJson()
                        clipboardManager.setText(AnnotatedString(json))
                        Toast.makeText(context, "Macros copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsClickItem(
                    title = "Import Macros",
                    subtitle = "Paste JSON to import macros",
                    icon = Icons.Default.Download,
                    iconColor = SuccessGreen,
                    onClick = { showImportDialog = true }
                )
            }

            // ─── Automation ───
            PremiumSectionHeader("SMART AUTOMATION")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Proximity Auto-Unlock",
                    subtitle = "Auto-connect and unlock Mac when you get near",
                    icon = Icons.AutoMirrored.Filled.BluetoothSearching,
                    iconColor = AccentBlue,
                    checked = proximityAutoUnlockEnabled,
                    onCheckedChange = { viewModel.setProximityAutoUnlockEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsClickItem(
                    title = "Proximity Calibration",
                    subtitle = "Unlock at ${String.format("%.1f", viewModel.proximityNearDistanceMeters())}m or closer • Cooldown ${proximityCooldownSec}s",
                    icon = Icons.Default.Tune,
                    iconColor = AccentBlue,
                    onClick = { showProximityCalibrationDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsClickItem(
                    title = "Live Proximity Meter",
                    subtitle = "Current $liveDistanceLabel • RSSI ${proximityLiveRssi} dBm • ${if (proximityUnlockArmed) "Armed" else "Waiting for away-reset"}",
                    icon = Icons.Default.MyLocation,
                    iconColor = AccentBlue,
                    onClick = { showProximityCalibrationDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsClickItem(
                    title = "Mac Lock State (Estimated)",
                    subtitle = "$lockStateLabel • inferred from proximity transitions; no direct macOS lock API",
                    icon = Icons.Default.Info,
                    iconColor = Silver,
                    onClick = { }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsClickItem(
                    title = "Proximity Target Device",
                    subtitle = if (proximityTargetAddress.isBlank()) "Auto-detect from saved hosts" else proximityTargetAddress,
                    icon = Icons.Default.Devices,
                    iconColor = AccentTeal,
                    onClick = { showProximityTargetDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Require Phone Unlock",
                    subtitle = "Do not auto-type Mac password while phone is locked",
                    icon = Icons.Default.Security,
                    iconColor = SuccessGreen,
                    checked = proximityRequirePhoneUnlock,
                    onCheckedChange = { viewModel.setProximityRequirePhoneUnlock(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Do Not Disturb on Connect",
                    subtitle = "Silence phone when Mac is connected",
                    icon = Icons.Default.DoNotDisturb,
                    iconColor = ErrorRed,
                    checked = dndOnConnect,
                    onCheckedChange = {
                        dndOnConnect = it
                        prefs.edit().putBoolean("auto_dnd_on_connect", it).apply()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Keep Screen Awake",
                    subtitle = "Prevent screen timeout while connected",
                    icon = Icons.Default.BrightnessHigh,
                    iconColor = WarningYellow,
                    checked = wakeLockOnConnect,
                    onCheckedChange = {
                        wakeLockOnConnect = it
                        prefs.edit().putBoolean("auto_wake_lock_on_connect", it).apply()
                    }
                )

            }

            // ─── Voice & Speech ───
            PremiumSectionHeader("VOICE & SPEECH ENGINE")
            PremiumGlassCard {
                val ttsPitch by viewModel.ttsPitch.collectAsState()
                val ttsSpeechRate by viewModel.ttsSpeechRate.collectAsState()
                
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = AccentPink, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Vocal Pitch", color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = ttsPitch,
                        onValueChange = { viewModel.setTtsPitch(it) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = AccentPink, activeTrackColor = AccentPink.copy(alpha = 0.5f))
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Speech Rate", color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = ttsSpeechRate,
                        onValueChange = { viewModel.setTtsSpeechRate(it) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue.copy(alpha = 0.5f))
                    )
                }
            }

            // ─── Security & Encryption ───
            PremiumSectionHeader("ENCRYPTION")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "End-to-End Encryption",
                    subtitle = "AES-GCM 256-bit • Requires Mac pairing",
                    icon = Icons.Default.Shield,
                    iconColor = SuccessGreen,
                    checked = e2eeEnabled,
                    onCheckedChange = {
                        e2eeEnabled = it
                        encryptionManager.setEnabled(it)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsClickItem(
                    title = "Pair with Mac (Show QR)",
                    subtitle = if (encryptionManager.isPaired()) "✅ Paired — key exchanged" else "Scan on Mac to exchange keys",
                    icon = Icons.Default.QrCode,
                    iconColor = AccentBlue,
                    onClick = { showQrDialog = true }
                )
            }

            // ─── Feature Visibility ───
            PremiumSectionHeader("FEATURE VISIBILITY")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Show Web Bridge",
                    subtitle = "Browser portal and file sync pages",
                    icon = Icons.Default.CloudSync,
                    iconColor = AccentBlue,
                    checked = featureWebBridgeVisible,
                    onCheckedChange = { viewModel.setFeatureWebBridgeVisible(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Show Automation + Shortcuts",
                    subtitle = "Macro dashboard, shortcut guide, and quick tools",
                    icon = Icons.Default.Bolt,
                    iconColor = AccentGold,
                    checked = featureAutomationVisible,
                    onCheckedChange = { viewModel.setFeatureAutomationVisible(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Show AI Assistant",
                    subtitle = "Chat assistant screen and tools",
                    icon = Icons.Default.AutoAwesome,
                    iconColor = AccentTeal,
                    checked = featureAssistantVisible,
                    onCheckedChange = { viewModel.setFeatureAssistantVisible(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Show Snippets",
                    subtitle = "Saved text snippets screen",
                    icon = Icons.AutoMirrored.Filled.Notes,
                    iconColor = AccentPurple,
                    checked = featureSnippetsVisible,
                    onCheckedChange = { viewModel.setFeatureSnippetsVisible(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Show Wake-on-LAN",
                    subtitle = "Network power-on tool",
                    icon = Icons.Default.PowerSettingsNew,
                    iconColor = SuccessGreen,
                    checked = featureWakeOnLanVisible,
                    onCheckedChange = { viewModel.setFeatureWakeOnLanVisible(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Show SSH Terminal",
                    subtitle = "Native secure shell screen",
                    icon = Icons.Default.Terminal,
                    iconColor = AccentOrange,
                    checked = featureSshTerminalVisible,
                    onCheckedChange = { viewModel.setFeatureSshTerminalVisible(it) }
                )
            }

            // ─── About ───
            PremiumSectionHeader("ABOUT")
            PremiumGlassCard {
                SettingsClickItem(
                    title = "About Developer",
                    subtitle = "Sagar M • Bengaluru, India",
                    icon = Icons.Default.Person,
                    iconColor = Platinum,
                    onClick = onNavigateToProfile
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsClickItem(
                    title = "Version Info",
                    subtitle = "v1.5.0-pro (Stable Build)",
                    icon = Icons.Default.Info,
                    iconColor = Silver,
                    onClick = { /* Could show a changelog */ }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

    // ─── Dialogs ───

    if (showSpeedDialog) {
        val speeds = listOf("Too Slow", "Slow", "Normal", "Fast", "Super Fast")
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = Graphite,
            title = { Text("Typing Speed", color = Platinum) },
            text = {
                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.setTypingSpeed(speed); showSpeedDialog = false }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (speed == typingSpeed), onClick = { viewModel.setTypingSpeed(speed); showSpeedDialog = false }, colors = RadioButtonDefaults.colors(selectedColor = AccentBlue))
                            Text(speed, color = Platinum, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSpeedDialog = false }) { Text("Cancel", color = Silver) } }
        )
    }

    if (showProximityCalibrationDialog) {
        var unlockDistance by remember { mutableStateOf(viewModel.proximityNearDistanceMeters()) }
        var cooldown by remember { mutableStateOf(proximityCooldownSec.toFloat()) }
        AlertDialog(
            onDismissRequest = { showProximityCalibrationDialog = false },
            containerColor = Graphite,
            title = { Text("Proximity Calibration", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Current distance: $liveDistanceLabel", color = Silver, fontSize = 12.sp)
                    Text("Current RSSI: ${proximityLiveRssi} dBm", color = Silver, fontSize = 12.sp)
                    Text("Unlock when phone is within ${String.format("%.1f", unlockDistance)} m", color = Silver, fontSize = 12.sp)
                    Slider(
                        value = unlockDistance,
                        onValueChange = { unlockDistance = it },
                        valueRange = 0.5f..8f,
                        colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue)
                    )
                    Text("Cooldown: ${cooldown.toInt()} sec", color = Silver, fontSize = 12.sp)
                    Slider(
                        value = cooldown,
                        onValueChange = { cooldown = it },
                        valueRange = 3f..60f,
                        colors = SliderDefaults.colors(thumbColor = AccentTeal, activeTrackColor = AccentTeal)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setProximityUnlockDistanceMeters(unlockDistance)
                        viewModel.setProximityCooldownSec(cooldown.toInt())
                        showProximityCalibrationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showProximityCalibrationDialog = false }) { Text("Cancel", color = Silver) }
            }
        )
    }

    if (showProximityTargetDialog) {
        AlertDialog(
            onDismissRequest = { showProximityTargetDialog = false },
            containerColor = Graphite,
            title = { Text("Select Proximity Target", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setProximityTargetAddress("") }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = proximityTargetAddress.isBlank(),
                            onClick = { viewModel.setProximityTargetAddress("") },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentBlue)
                        )
                        Text("Auto-select from saved devices", color = Platinum)
                    }

                    bondedDevices.forEach { device ->
                        val address = device.address ?: return@forEach
                        val name = if (hasBluetoothConnectPermission) {
                            runCatching { device.name }.getOrDefault(null) ?: "Unknown Device"
                        } else {
                            "Unknown Device"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setProximityTargetAddress(address) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = proximityTargetAddress == address,
                                onClick = { viewModel.setProximityTargetAddress(address) },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentBlue)
                            )
                            Column {
                                Text(name, color = Platinum)
                                Text(address, color = Silver, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProximityTargetDialog = false }) { Text("Done", color = AccentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showProximityTargetDialog = false }) { Text("Cancel", color = Silver) }
            }
        )
    }



    if (showQrDialog) {
        val myPublicKey = remember { encryptionManager.getPublicKeyBase64() }
        var peerKey by remember { mutableStateOf("") }
        var pairingError by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            containerColor = Graphite,
            title = { Text("🔐 E2EE Pairing", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Step 1 — Share your public key:", color = Silver, fontSize = 13.sp)
                    Surface(color = Obsidian, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(myPublicKey, modifier = Modifier.padding(8.dp), fontSize = 9.sp, color = AccentBlue, fontWeight = FontWeight.Bold)
                    }
                    Text("Step 2 — Paste the Mac's public key:", color = Silver, fontSize = 13.sp)
                    OutlinedTextField(
                        value = peerKey,
                        onValueChange = { peerKey = it; pairingError = "" },
                        label = { Text("Mac's public key (Base64)") },
                        minLines = 3, maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = BorderColor, focusedTextColor = Platinum)
                    )
                    if (pairingError.isNotEmpty()) { Text("❌ $pairingError", color = ErrorRed, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    try { encryptionManager.acceptPeerPublicKey(peerKey.trim()); showQrDialog = false }
                    catch (e: Exception) { pairingError = "Invalid key format." }
                }, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) { Text("Pair") }
            },
            dismissButton = { TextButton(onClick = { showQrDialog = false }) { Text("Cancel", color = Silver) } }
        )
    }

    if (showImportDialog) {
        var importJson by remember { mutableStateOf("") }
        var importError by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = Graphite,
            title = { Text("Import Macros", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste the exported macro JSON below:", color = Silver, fontSize = 13.sp)
                    OutlinedTextField(
                        value = importJson,
                        onValueChange = { importJson = it; importError = "" },
                        label = { Text("JSON content") },
                        minLines = 4, maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SuccessGreen, unfocusedBorderColor = BorderColor, focusedTextColor = Platinum)
                    )
                    if (importError.isNotEmpty()) { Text("❌ $importError", color = ErrorRed, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (automationViewModel.importMacrosJson(importJson)) {
                        Toast.makeText(context, "Macros imported!", Toast.LENGTH_SHORT).show()
                        showImportDialog = false
                    } else {
                        importError = "Invalid JSON format."
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)) { Text("Import", color = Obsidian) }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("Cancel", color = Silver) } }
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Settings Item Components
// ────────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = AccentBlue,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "$title. $subtitle"
                stateDescription = if (checked) "On" else "Off"
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            SettingsIconBadge(icon = icon, backgroundColor = iconColor)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = Silver, fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = SuccessGreen, checkedThumbColor = Color.White, uncheckedThumbColor = Color.White, uncheckedTrackColor = SoftGrey)
        )
    }
}

@Composable
fun SettingsClickItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = AccentBlue,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$title. $subtitle"
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBadge(icon = icon, backgroundColor = iconColor)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Silver, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Silver.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ContactItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = text }
    ) {
        Icon(icon, contentDescription = null, tint = Silver, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, color = Platinum, fontSize = 14.sp)
    }
}

@Composable
fun SocialIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = SoftGrey.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f))
        ) {
            Icon(icon, contentDescription = label, tint = Platinum, modifier = Modifier.padding(10.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Silver, fontSize = 10.sp)
    }
}
