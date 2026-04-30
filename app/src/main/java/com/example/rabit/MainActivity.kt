package com.example.rabit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Obsidian
import com.example.rabit.ui.theme.RabitTheme
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.data.bluetooth.HidService
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.assistant.AssistantScreen
import com.example.rabit.ui.assistant.AssistantViewModel
import com.example.rabit.ui.components.BridgeBiometricAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.example.rabit.ui.keyboard.KeyboardScreen
import com.example.rabit.ui.home.HomeScreen
import com.example.rabit.ui.onboarding.OnboardingScreen
import com.example.rabit.ui.pairing.PairingScreen
import com.example.rabit.ui.settings.PasswordManagerScreen
import com.example.rabit.ui.settings.SettingsScreen
import com.example.rabit.ui.snippets.SnippetsScreen
import com.example.rabit.ui.profile.ProfileScreen
import com.example.rabit.ui.automation.AutomationDashboardScreen
import com.example.rabit.ui.search.GlobalSearchScreen
import com.example.rabit.ui.theme.RabitTheme

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val assistantViewModel: AssistantViewModel by viewModels()
    private val settingsViewModel: com.example.rabit.ui.settings.SettingsViewModel by viewModels()
    private val webBridgeViewModel: com.example.rabit.ui.webbridge.WebBridgeViewModel by viewModels()
    private val helperViewModel: com.example.rabit.ui.helper.HelperViewModel by viewModels()
    private val automationViewModel: com.example.rabit.ui.automation.AutomationViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val repository = com.example.rabit.data.repository.KeyboardRepositoryImpl(applicationContext)
                return com.example.rabit.ui.automation.AutomationViewModel(application, repository) as T
            }
        }
    }
    private val neuralQaViewModel: com.example.rabit.ui.qa.NeuralQaViewModel by viewModels()
    private val neuralWebAuditorViewModel: com.example.rabit.ui.qa.NeuralWebAuditorViewModel by viewModels()
    private val payloadForgeViewModel: com.example.rabit.ui.payload.PayloadForgeViewModel by viewModels()
    private val rogueHorizonViewModel: com.example.rabit.ui.network.RogueHorizonViewModel by viewModels()
    private val sensorLabViewModel: com.example.rabit.ui.sensors.SensorLabViewModel by viewModels()
    private val osintGhostViewModel: com.example.rabit.ui.osint.OsintGhostViewModel by viewModels()
    private val bluetoothShadowViewModel: com.example.rabit.ui.network.BluetoothShadowViewModel by viewModels()
    private val bluetoothMirrorViewModel: com.example.rabit.ui.network.BluetoothMirrorViewModel by viewModels()
    private val securityAuditorViewModel: com.example.rabit.ui.security.SecurityAuditorViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val adbClient = com.example.rabit.data.storage.RemoteStorageManager.adbClient
                val storageManager = com.example.rabit.data.security.TacticalStorageManager(applicationContext)
                return com.example.rabit.ui.security.SecurityAuditorViewModel(adbClient, storageManager) as T
            }
        }
    }
    private val trafficAnalyzerViewModel: com.example.rabit.ui.security.TrafficAnalyzerViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val adbClient = com.example.rabit.data.storage.RemoteStorageManager.adbClient
                val storageManager = com.example.rabit.data.security.TrafficStorageManager(applicationContext)
                return com.example.rabit.ui.security.TrafficAnalyzerViewModel(adbClient, storageManager) as T
            }
        }
    }
    private val decoyViewModel: com.example.rabit.ui.stealth.DecoyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            RabitTheme {
                val isDecoyMode by viewModel.isDecoyMode.collectAsState()
                
                if (isDecoyMode) {
                    com.example.rabit.ui.opsec.DecoyScreen(
                        onDeactivate = { viewModel.setDecoyMode(false) }
                    )
                } else {
                    BluetoothPermissions {
                        LaunchedEffect(Unit) {
                            val serviceIntent = Intent(this@MainActivity, HidService::class.java)
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startForegroundService(serviceIntent)
                                } else {
                                    startService(serviceIntent)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Failed to start HidService", e)
                            }
                        }

                        LaunchedEffect(Unit) {
                            viewModel.biometricRequests.collectLatest { deferred ->
                                BridgeBiometricAuth.authenticate(
                                    activity = this@MainActivity,
                                    title = "Authorize Hackie Bridge",
                                    subtitle = "A new device is trying to connect with your PIN",
                                    onSuccess = { deferred.complete(true) },
                                    onError = { deferred.complete(false) }
                                )
                            }
                        }
                        
                        val isStealthActive by decoyViewModel.isStealthMode.collectAsState()
                        val isUnlocked by decoyViewModel.isSessionUnlocked.collectAsState()

                        val isVaultUnlocked by decoyViewModel.isVaultUnlocked.collectAsState()

                        if (isStealthActive && !isUnlocked && !isVaultUnlocked) {
                            com.example.rabit.ui.stealth.DecoyCalculatorScreen(
                                viewModel = decoyViewModel,
                                onUnlock = { decoyViewModel.unlockHackie() }
                            )
                        } else if (isStealthActive && isVaultUnlocked) {
                            // VAULT VIEW - Show a unified list of forensic exfiltrations
                            com.example.rabit.ui.security.VaultScreen(
                                auditorViewModel = securityAuditorViewModel,
                                trafficViewModel = trafficAnalyzerViewModel,
                                onLock = { decoyViewModel.lockAll() }
                            )
                        } else {
                            val biometricEnabled by viewModel.biometricLockEnabled.collectAsState()
                            com.example.rabit.ui.components.BiometricGuard(isEnabled = biometricEnabled) {
                                AppNavigation(
                                    viewModel,
                                    assistantViewModel,
                                    settingsViewModel,
                                    webBridgeViewModel,
                                    automationViewModel,
                                    helperViewModel,
                                    neuralQaViewModel = neuralQaViewModel,
                                    neuralWebAuditorViewModel = neuralWebAuditorViewModel,
                                    payloadForgeViewModel = payloadForgeViewModel,
                                    rogueHorizonViewModel = rogueHorizonViewModel,
                                    sensorLabViewModel = sensorLabViewModel,
                                    osintGhostViewModel = osintGhostViewModel,
                                    bluetoothShadowViewModel = bluetoothShadowViewModel,
                                    bluetoothMirrorViewModel = bluetoothMirrorViewModel,
                                    securityAuditorViewModel = securityAuditorViewModel,
                                    trafficAnalyzerViewModel = trafficAnalyzerViewModel,
                                    decoyViewModel = decoyViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.parcelableExtraCompat<android.net.Uri>(Intent.EXTRA_STREAM)
                uri?.let { webBridgeViewModel.addSharedFile(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.parcelableArrayListExtraCompat<android.net.Uri>(Intent.EXTRA_STREAM)
                uris?.forEach { webBridgeViewModel.addSharedFile(it) }
            }
        }
    }

    private inline fun <reified T : Parcelable> Intent.parcelableExtraCompat(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key) as? T
        }
    }

    private inline fun <reified T : Parcelable> Intent.parcelableArrayListExtraCompat(key: String): ArrayList<T>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayListExtra(key)
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    assistantViewModel: AssistantViewModel,
    settingsViewModel: com.example.rabit.ui.settings.SettingsViewModel,
    webBridgeViewModel: com.example.rabit.ui.webbridge.WebBridgeViewModel,
    automationViewModel: com.example.rabit.ui.automation.AutomationViewModel,
    helperViewModel: com.example.rabit.ui.helper.HelperViewModel,
    browserViewModel: com.example.rabit.ui.browser.BrowserViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    webHubViewModel: com.example.rabit.ui.webhub.WebHubViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    remoteDeckViewModel: com.example.rabit.ui.remotedeck.RemoteDeckViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    lockdownViewModel: com.example.rabit.ui.lockdown.LockdownViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    networkAuditorViewModel: com.example.rabit.ui.network.NetworkAuditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    bruteForceViewModel: com.example.rabit.ui.automation.HidBruteForceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    webSniperViewModel: com.example.rabit.ui.websniper.WebSniperViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    localTerminalViewModel: com.example.rabit.ui.automation.LocalTerminalViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    osintViewModel: com.example.rabit.ui.osint.OsintViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    bleAuditorViewModel: com.example.rabit.ui.network.BleAuditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    wifiAttackerViewModel: com.example.rabit.ui.network.WifiAttackerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    neuralQaViewModel: com.example.rabit.ui.qa.NeuralQaViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    neuralWebAuditorViewModel: com.example.rabit.ui.qa.NeuralWebAuditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    payloadForgeViewModel: com.example.rabit.ui.payload.PayloadForgeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    rogueHorizonViewModel: com.example.rabit.ui.network.RogueHorizonViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    sensorLabViewModel: com.example.rabit.ui.sensors.SensorLabViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    osintGhostViewModel: com.example.rabit.ui.osint.OsintGhostViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    bluetoothShadowViewModel: com.example.rabit.ui.network.BluetoothShadowViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    bluetoothMirrorViewModel: com.example.rabit.ui.network.BluetoothMirrorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    securityAuditorViewModel: com.example.rabit.ui.security.SecurityAuditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    trafficAnalyzerViewModel: com.example.rabit.ui.security.TrafficAnalyzerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    hashCrackerViewModel: com.example.rabit.ui.security.HashCrackerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    encoderDecoderViewModel: com.example.rabit.ui.crypto.EncoderDecoderViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    subdomainScannerViewModel: com.example.rabit.ui.osint.SubdomainScannerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    exifForensicsViewModel: com.example.rabit.ui.forensics.ExifForensicsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    reverseShellViewModel: com.example.rabit.ui.exploit.ReverseShellViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    portScannerViewModel: com.example.rabit.ui.network.PortScannerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    steganographyViewModel: com.example.rabit.ui.steganography.SteganographyViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    killSwitchViewModel: com.example.rabit.ui.opsec.KillSwitchViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    pingTraceViewModel: com.example.rabit.ui.network.PingTraceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    pentestToolkitViewModel: com.example.rabit.ui.pentest.PentestToolkitViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    decoyViewModel: com.example.rabit.ui.stealth.DecoyViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val navController = rememberNavController()
    val startDest = if (viewModel.onboardingCompleted) "home" else "onboarding"
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDest
    val featureWebBridgeVisible by viewModel.featureWebBridgeVisible.collectAsState()
    val featureAutomationVisible by viewModel.featureAutomationVisible.collectAsState()
    val featureAssistantVisible by viewModel.featureAssistantVisible.collectAsState()
    val featureSnippetsVisible by viewModel.featureSnippetsVisible.collectAsState()
    val featureShortcutsVisible by viewModel.featureShortcutsVisible.collectAsState()
    val featureSshTerminalVisible by viewModel.featureSshTerminalVisible.collectAsState()
    val webBridgeEnabled by webBridgeViewModel.isWebBridgeRunning.collectAsState()
    val bluetoothState by viewModel.connectionState.collectAsState()
    val isBluetoothConnected = bluetoothState is HidDeviceManager.ConnectionState.Connected

    // Routes that should NOT show the professional drawer (Onboarding & Initial Pairing)
    val noDrawerRoutes = listOf("onboarding", "onboarding_splash")
    val showDrawer = currentRoute.split("?").first() !in noDrawerRoutes

    fun routeAllowed(route: String): Boolean {
        return when (route) {
            "web_bridge" -> true
            "automation" -> true
            "shortcuts" -> true
            "assistant" -> true
            "snippets" -> true
            "ssh_terminal" -> true
            "auto_clicker" -> true
            "system_monitor" -> true
            "remote_explorer" -> true
            "reverse_shell" -> true
            "terminal_scanner" -> true
            "process_manager" -> true
            "system_stats" -> true
            "wake_on_lan" -> true
            "remote_deck" -> true
            "lockdown" -> true
            "network_auditor" -> true
            "tactical_terminal" -> true
            "screenshot_lab" -> true
            "keystroke_monitor" -> true
            "vision_lab" -> true
            "macro_lab" -> true
            "forensics_lab" -> true
            "web_sniper" -> true
            "panic_terminal" -> true
            "bluetooth_shadow" -> true
            "forensic_vault" -> true
            "local_terminal" -> true
            "ghost_recon" -> true
            "wireless_auditor" -> true
            "neural_lab" -> true
            "payload_forge" -> true
            "rogue_horizon" -> true
            "sensor_lab" -> true
            "hid_brute_force" -> true
            "hash_cracker" -> true
            "crypto_encoder" -> true
            "subdomain_scanner" -> true
            "exif_forensics" -> true
            "reverse_shell_gen" -> true
            "port_scanner" -> true
            "stego_lab" -> true
            "ping_trace" -> true
            "pentest_toolkit" -> true
            "web_hub" -> true
            "global_search" -> true
            else -> true
        }
    }

    LaunchedEffect(
        currentRoute,
        featureWebBridgeVisible,
        featureAutomationVisible,
        featureAssistantVisible,
        featureSnippetsVisible,
        featureSshTerminalVisible
    ) {
        val current = currentRoute.split("?").first()
        if (!routeAllowed(current)) {
            navController.navigate("keyboard") {
                popUpTo(current) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val navHost = @Composable { padding: androidx.compose.foundation.layout.PaddingValues ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = startDest) {
                composable("onboarding") {
                    OnboardingScreen(
                        onComplete = {
                            viewModel.markOnboardingCompleted()
                            navController.navigate("home") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    )
                }
                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenHelper = {
                            navController.navigate("helper") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable("pairing") {
                    com.example.rabit.ui.pairing.PairingScreen(
                        viewModel = viewModel,
                        mirrorViewModel = bluetoothMirrorViewModel,
                        shadowViewModel = bluetoothShadowViewModel,
                        automationViewModel = automationViewModel,
                        onConnected = {
                            navController.navigate("keyboard") {
                                popUpTo("pairing") { inclusive = true }
                            }
                        },
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }
                composable("media_deck") {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Media Deck Coming Soon", color = Platinum)
                    }
                }
                composable("keyboard") {
                    KeyboardScreen(
                        viewModel = viewModel,
                        helperViewModel = helperViewModel,
                        webBridgeViewModel = webBridgeViewModel,
                        onDisconnect = { 
                            viewModel.disconnectKeyboard()
                            navController.navigate("pairing") { popUpTo(0) } 
                        },
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToAssistant = { if (featureAssistantVisible) navController.navigate("assistant") },
                        onNavigateToSnippets = { if (featureSnippetsVisible) navController.navigate("snippets") },
                        onNavigateToAutomation = { if (featureAutomationVisible) navController.navigate("automation") },
                        onNavigateToWebBridge = { if (featureWebBridgeVisible) navController.navigate("web_bridge") }
                    )
                }
                composable("web_bridge") {
                    com.example.rabit.ui.webbridge.WebBridgeScreen(
                        viewModel = webBridgeViewModel,
                        webHubViewModel = webHubViewModel,
                        remoteDeckViewModel = remoteDeckViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("automation") {
                    AutomationDashboardScreen(
                        viewModel = automationViewModel,
                        mainViewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToWakeOnLan = { },
                        onNavigateToSshTerminal = { if (featureSshTerminalVisible) navController.navigate("ssh_terminal") },
                        onNavigateTo = { route -> navController.navigate(route) }
                    )
                }
                composable("airplay_receiver") {
                    com.example.rabit.ui.airplay.AirPlayReceiverScreen(
                        viewModel = viewModel,
                        webBridgeViewModel = webBridgeViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("ssh_terminal") {
                    com.example.rabit.ui.automation.SshTerminalScreen(
                        viewModel = helperViewModel,
                        automationViewModel = automationViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("assistant") {
                    com.example.rabit.ui.assistant.AssistantBrowserPager(
                        assistantViewModel = assistantViewModel,
                        browserViewModel = browserViewModel,
                        mainViewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToSettings = {
                            navController.navigate("settings") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToKeyboard = { navController.navigate("keyboard") },
                        onNavigate = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable("web_hub") {
                    com.example.rabit.ui.webhub.WebHubScreen(
                        viewModel = webHubViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("remote_deck") {
                    com.example.rabit.ui.remotedeck.RemoteDeckScreen(
                        viewModel = remoteDeckViewModel
                    )
                }
                composable("lockdown") {
                    com.example.rabit.ui.lockdown.LockdownScreen(
                        viewModel = lockdownViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("network_auditor") {
                    com.example.rabit.ui.network.NetworkAuditorScreen(
                        viewModel = networkAuditorViewModel,
                        portScannerViewModel = portScannerViewModel,
                        pingTraceViewModel = pingTraceViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("hid_brute_force") {
                    com.example.rabit.ui.automation.HidBruteForceScreen(
                        viewModel = bruteForceViewModel,
                        hashCrackerViewModel = hashCrackerViewModel,
                        apiKey = settingsViewModel.geminiApiKey,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("adb_mirror") {
                    com.example.rabit.ui.automation.AdbMirrorScreen(
                        viewModel = automationViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("injector") {
                    com.example.rabit.ui.automation.InjectorScreen(
                        viewModel = viewModel,
                        automationViewModel = automationViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("auto_clicker") {
                    com.example.rabit.ui.automation.AutoClickerScreen(
                        mainViewModel = viewModel,
                        viewModel = automationViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("system_monitor") {
                    com.example.rabit.ui.automation.SystemMonitorScreen(
                        helperViewModel = helperViewModel,
                        initialSubFeature = "processes",
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("remote_explorer") {
                    com.example.rabit.ui.automation.RemoteExplorerScreen(
                        viewModel = helperViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("reverse_shell") {
                    com.example.rabit.ui.automation.ReverseShellScreen(
                        viewModel = automationViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("terminal_scanner") {
                    com.example.rabit.ui.automation.TerminalScannerScreen(
                        viewModel = automationViewModel,
                        onBack = { navController.popBackStack() },
                        onConnect = { _, _, _ -> navController.popBackStack() }
                    )
                }
                // --- Wave 1: Tactical C2 ---
                composable("screenshot_lab") {
                    com.example.rabit.ui.automation.ScreenshotLabScreen(
                        viewModel = webBridgeViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("panic_terminal") {
                    com.example.rabit.ui.opsec.PanicTerminalScreen(
                        mainViewModel = viewModel,
                        killSwitchViewModel = killSwitchViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("local_terminal") {
                    com.example.rabit.ui.automation.LocalTerminalScreen(
                        viewModel = localTerminalViewModel,
                        apiKey = settingsViewModel.geminiApiKey,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("ghost_recon") {
                    com.example.rabit.ui.osint.OsintScreen(
                        viewModel = osintViewModel,
                        ghostViewModel = osintGhostViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("wireless_auditor") {
                    com.example.rabit.ui.network.WirelessAuditorScreen(
                        bleViewModel = bleAuditorViewModel,
                        wifiViewModel = wifiAttackerViewModel,
                        apiKey = settingsViewModel.geminiApiKey,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("neural_lab") {
                    com.example.rabit.ui.qa.NeuralLabScreen(
                        qaViewModel = neuralQaViewModel,
                        webViewModel = neuralWebAuditorViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("crypto_encoder") {
                    com.example.rabit.ui.crypto.EncoderDecoderScreen(
                        viewModel = encoderDecoderViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("subdomain_scanner") {
                    com.example.rabit.ui.osint.SubdomainScannerScreen(
                        viewModel = subdomainScannerViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("reverse_shell_gen") {
                    com.example.rabit.ui.exploit.ReverseShellScreen(
                        viewModel = reverseShellViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("stego_lab") {
                    com.example.rabit.ui.steganography.SteganographyScreen(
                        viewModel = steganographyViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("pentest_toolkit") {
                    com.example.rabit.ui.pentest.PentestToolkitScreen(
                        viewModel = pentestToolkitViewModel,
                        helperViewModel = helperViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("payload_forge") {
                    val scope = rememberCoroutineScope()
                    com.example.rabit.ui.payload.PayloadForgeScreen(
                        viewModel = payloadForgeViewModel,
                        onBack = { navController.popBackStack() },
                        onExecuteHid = { code -> viewModel.executeDuckyScript(code) },
                        onDeployAdb = { code -> 
                             scope.launch {
                                  com.example.rabit.data.storage.RemoteStorageManager.adbClient?.executeCommand(code)
                             }
                        }
                    )
                }
                composable("rogue_horizon") {
                    com.example.rabit.ui.network.RogueHorizonScreen(
                        viewModel = rogueHorizonViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("sensor_lab") {
                    com.example.rabit.ui.sensors.SensorLabScreen(
                        viewModel = sensorLabViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("security_auditor") {
                    com.example.rabit.ui.security.SecurityAuditorScreen(
                        viewModel = securityAuditorViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("traffic_analyzer") {
                    com.example.rabit.ui.security.TrafficAnalyzerScreen(
                        viewModel = trafficAnalyzerViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                // --- Wave 6: Web Sniper ---
                composable("web_sniper") {
                    com.example.rabit.ui.websniper.WebSniperScreen(
                        viewModel = webSniperViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("keystroke_monitor") {
                    com.example.rabit.ui.automation.KeystrokeMonitorScreen(
                        viewModel = webBridgeViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("vision_lab") {
                    com.example.rabit.ui.automation.VisionLabScreen(
                        viewModel = automationViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("macro_lab") {
                    com.example.rabit.ui.automation.MacroLabScreen(
                        viewModel = automationViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("forensics_lab") {
                    val forensicsViewModel: com.example.rabit.ui.forensics.ForensicsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    val exifForensicsViewModel: com.example.rabit.ui.forensics.ExifForensicsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    com.example.rabit.ui.forensics.ForensicsLabScreen(
                        viewModel = forensicsViewModel,
                        exifViewModel = exifForensicsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        settingsViewModel = settingsViewModel,
                        automationViewModel = automationViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToProfile = { navController.navigate("profile") },
                        onNavigateToPasswordManager = { navController.navigate("password_manager") }
                    )
                }
                composable("password_manager") {
                    PasswordManagerScreen(
                        settingsViewModel = settingsViewModel,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("helper") {
                    com.example.rabit.ui.helper.HelperScreen(
                        viewModel = helperViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("adb_manager") {
                    com.example.rabit.ui.automation.AdbManagerScreen(
                        viewModel = automationViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToFiles = { navController.navigate("remote_explorer") },
                        onNavigateToMirror = { navController.navigate("adb_mirror") }
                    )
                }
                composable("profile") {
                    ProfileScreen(onBack = { navController.popBackStack() })
                }
                composable("snippets") {
                    SnippetsScreen(viewModel, onBack = { navController.popBackStack() })
                }
                // "shortcuts" route removed — use "automation" instead
                composable("global_search") {
                    val available = buildSet {
                        add("home")
                        add("keyboard")
                        add("injector")
                        add("airplay_receiver")
                        add("settings")
                        add("customization")
                        add("password_manager")
                        add("profile")
                        if (featureWebBridgeVisible) add("web_bridge")
                        if (featureAutomationVisible) add("automation")
                        if (featureAssistantVisible) add("assistant")
                        if (featureSnippetsVisible) add("snippets")
                        if (featureSnippetsVisible) add("snippets")
                        if (featureSshTerminalVisible) add("ssh_terminal")
                        add("screenshot_lab")
                        add("keystroke_monitor")
                        add("security_auditor")
                        add("traffic_analyzer")
                    }
                    val availableActions = buildSet {
                        add("action_unlock_mac")
                        add("action_lock_screen")
                        add("action_media_play_pause")
                        add("action_media_vol_up")
                        add("action_media_vol_down")
                        add("action_now_playing")
                        add("action_disconnect_keyboard")
                        if (featureWebBridgeVisible) add("action_web_bridge_toggle")
                    }
                    GlobalSearchScreen(
                        currentRoute = currentRoute.split("?").first(),
                        availableRoutes = available,
                        availableActionIds = availableActions,
                        onBack = { navController.popBackStack() },
                        onNavigate = { route ->
                            if (!routeAllowed(route)) return@GlobalSearchScreen
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        },
                        onExecuteAction = { actionId ->
                            when (actionId) {
                                "action_unlock_mac" -> viewModel.unlockMac()
                                "action_lock_screen" -> viewModel.sendSystemShortcut(MainViewModel.SystemShortcut.LOCK_SCREEN)
                                "action_media_play_pause" -> viewModel.sendMediaPlayPause()
                                "action_media_vol_up" -> viewModel.sendMediaVolumeUp()
                                "action_media_vol_down" -> viewModel.sendMediaVolumeDown()
                                "action_now_playing" -> {
                                    // Metadata is now reactive in WebBridgeViewModel
                                }
                                "action_disconnect_keyboard" -> viewModel.disconnectKeyboard()
                                "action_web_bridge_toggle" -> {
                                    if (featureWebBridgeVisible) {
                                        if (webBridgeEnabled) webBridgeViewModel.stopWebBridge() else webBridgeViewModel.startWebBridge()
                                    }
                                }
                            }
                        }
                    )
                }
                composable("browser") {
                    com.example.rabit.ui.browser.BrowserScreen(
                        viewModel = browserViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("macro_orchestrator") {
                    com.example.rabit.ui.automation.MacroLabScreen(
                        viewModel = automationViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("forensic_vault") {
                    val forensicsViewModel: com.example.rabit.ui.forensics.ForensicsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    val exifForensicsViewModel: com.example.rabit.ui.forensics.ExifForensicsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    com.example.rabit.ui.forensics.ForensicsLabScreen(
                        viewModel = forensicsViewModel,
                        exifViewModel = exifForensicsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("bluetooth_shadow") {
                    com.example.rabit.ui.network.BluetoothShadowScreen(
                        viewModel = bluetoothShadowViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("process_manager") {
                    com.example.rabit.ui.automation.SystemMonitorScreen(
                        helperViewModel = helperViewModel,
                        initialSubFeature = "processes",
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("system_stats") {
                    com.example.rabit.ui.automation.SystemMonitorScreen(
                        helperViewModel = helperViewModel,
                        initialSubFeature = "stats",
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("wake_on_lan") {
                    // Map to automation dashboard as WOL is integrated there
                    navController.navigate("automation") { launchSingleTop = true }
                }
                composable("tactical_terminal") {
                    com.example.rabit.ui.opsec.PanicTerminalScreen(
                        mainViewModel = viewModel,
                        killSwitchViewModel = killSwitchViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    val activeApp by viewModel.activeApp.collectAsState()

    if (showDrawer) {
        com.example.rabit.ui.components.RabitAppScaffold(
            currentRoute = if (currentRoute == "keyboard") "main" else currentRoute,
            onNavigate = { route ->
                val target = when (route) {
                    "main" -> if (isBluetoothConnected) "keyboard" else "pairing"
                    else -> route
                }
                if (!routeAllowed(target)) return@RabitAppScaffold
                navController.navigate(target) {
                    popUpTo("keyboard") { saveState = true }
                    launchSingleTop = true
                    restoreState = target != "assistant"
                }
            },
            showTopBar = currentRoute.split("?").first() != "assistant",
            featureWebBridgeVisible = featureWebBridgeVisible,
            featureAutomationVisible = featureAutomationVisible,
            featureAssistantVisible = featureAssistantVisible,
            featureSnippetsVisible = featureSnippetsVisible,
            featureShortcutsVisible = featureShortcutsVisible,
            featureSshTerminalVisible = featureSshTerminalVisible,
            activeApp = activeApp,
            onBack = { navController.popBackStack() },
            topBarActions = {
                val route = currentRoute.split("?").first()
                IconButton(onClick = {
                    if (route != "global_search") {
                        navController.navigate("global_search") { launchSingleTop = true }
                    }
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Open global search")
                }
                if (route == "assistant") return@RabitAppScaffold
            }
        ) { padding ->
            navHost(padding)
        }
    } else {
        navHost(androidx.compose.foundation.layout.PaddingValues(0.dp))
    }
}

@Composable
fun BluetoothPermissions(content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Only require critical Bluetooth permissions to show app content
    val criticalPermissions = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    // Optional permissions (requested but not blocking)
    val optionalPermissions = mutableListOf<String>().apply {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.VIBRATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val allPermissions = (criticalPermissions + optionalPermissions).toTypedArray()

    var criticalGranted by remember {
        mutableStateOf(criticalPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        criticalGranted = criticalPermissions.all { result[it] == true }
    }

    LaunchedEffect(Unit) {
        val anyMissing = allPermissions.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (anyMissing) {
            permissionLauncher.launch(allPermissions)
        }
    }

    if (criticalGranted) {
        content()
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = "Bluetooth permission icon",
                    tint = Color(0xFF0A84FF),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "Bluetooth Permission Required",
                    color = Color(0xFFF2F2F7),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Hackie needs Bluetooth access to connect to your Mac.",
                    color = Color(0xFF8E8E93),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { permissionLauncher.launch(allPermissions) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A84FF)
                    )
                ) {
                    Text("Grant Permission", color = Color.White)
                }
            }
        }
    }
}
