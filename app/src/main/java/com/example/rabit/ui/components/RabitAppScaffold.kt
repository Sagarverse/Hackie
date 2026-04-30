package com.example.rabit.ui.components

import kotlinx.coroutines.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

val LocalOpenGlobalDrawer = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * RabitAppScaffold — Ultra-minimal global container.
 * Provides a grouped modal drawer and a refined top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RabitAppScaffold(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    showTopBar: Boolean = true,
    featureWebBridgeVisible: Boolean = true,
    featureAutomationVisible: Boolean = true,
    featureAssistantVisible: Boolean = true,
    featureSnippetsVisible: Boolean = true,
    featureShortcutsVisible: Boolean = true,
    featureWakeOnLanVisible: Boolean = true,
    featureSshTerminalVisible: Boolean = true,
    activeApp: String? = null,
    onBack: (() -> Unit)? = null,
    topBarActions: @Composable RowScope.() -> Unit = {},
    onPanicLock: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val mainRoutes = listOf("home", "main", "keyboard", "web_bridge", "assistant", "browser", "settings", "ssh_terminal", "airplay_receiver", "global_search", "automation", "password_manager", "helper", "auto_clicker", "process_manager", "system_stats", "remote_explorer", "reverse_shell", "terminal_scanner")
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    val screenTitle = when(currentRoute) {
        "home" -> "Home"
        "main", "keyboard" -> "Control Hub"
        "web_bridge" -> "Web Bridge"
        "assistant" -> "Genie AI"
        "settings" -> "Settings"
        "profile" -> "Profile"
        "customization" -> "Theme"
        "password_manager" -> "Passwords"
        "snippets" -> "Snippets"
        "shortcuts" -> "Automation"
        "ssh_terminal" -> "SSH Terminal"
        "airplay_receiver" -> "AirPlay"
        "global_search" -> "Search"
        "helper" -> "Hackie Helper"
        "auto_clicker" -> "Auto Clicker"
        "process_manager" -> "Processes"
        "system_stats" -> "System Stats"
        "remote_explorer" -> "Remote Explorer"
        "reverse_shell" -> "Reverse Shell"
        "terminal_scanner" -> "Scanner"
        "browser" -> "Browser"
        else -> "Hackie"
    }

    val appBarSubtitle = when {
        currentRoute == "assistant" -> "AI Workspace"
        currentRoute == "browser" -> "Web Explorer"
        currentRoute == "airplay_receiver" -> "Wireless Audio"
        currentRoute == "helper" -> "Desktop Bridge"
        !activeApp.isNullOrBlank() && currentRoute in listOf("main", "keyboard") -> activeApp
        else -> "Hackie Pro"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Surface0,
                drawerContentColor = TextPrimary,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ── Brand Header ──
                    Spacer(modifier = Modifier.height(48.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Hackie",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.W300,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(AccentBlue.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "PRO",
                                color = AccentBlue,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.W700,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    
                    // ── Tactical Search ──
                    var searchQuery by remember { mutableStateOf("") }
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        placeholder = { Text("Search Labs...", color = TextTertiary, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextTertiary, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue.copy(alpha = 0.5f),
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Surface1.copy(alpha = 0.5f),
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Scrollable Navigation ──
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        fun matches(label: String) = searchQuery.isBlank() || label.contains(searchQuery, ignoreCase = true)

                        // ── Primary ──
                        if (matches("Home")) {
                            DrawerNavItem(
                                label = "Home",
                                icon = Icons.Default.Home,
                                isSelected = currentRoute == "home",
                                onClick = { onNavigate("home"); scope.launch { drawerState.close() } }
                            )
                        }
                        if (matches("Control Hub")) {
                            DrawerNavItem(
                                label = "Control Hub",
                                icon = Icons.AutoMirrored.Filled.Dvr,
                                isSelected = currentRoute == "main" || currentRoute == "keyboard",
                                onClick = { onNavigate("main"); scope.launch { drawerState.close() } }
                            )
                        }

                        // ── Connectivity ──
                        if (matches("Connectivity") || matches("Web Bridge") || matches("Web Hub") || matches("Remote Lab") || matches("Network Auditor") || matches("Hackie Helper") || matches("AirPlay")) {
                            DrawerSectionLabel("Connectivity")

                            if (matches("Web Bridge") || matches("Web Hub") || matches("Remote Lab")) {
                                DrawerNavItem(
                                    label = "Web Bridge",
                                    icon = Icons.Default.CloudSync,
                                    isSelected = currentRoute == "web_bridge",
                                    onClick = { onNavigate("web_bridge"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("Hackie Helper")) {
                                DrawerNavItem(
                                    label = "Hackie Helper",
                                    icon = Icons.Default.Devices,
                                    isSelected = currentRoute == "helper",
                                    onClick = { onNavigate("helper"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("AirPlay")) {
                                DrawerNavItem(
                                    label = "AirPlay Receiver",
                                    icon = Icons.Default.Speaker,
                                    isSelected = currentRoute == "airplay_receiver",
                                    onClick = { onNavigate("airplay_receiver"); scope.launch { drawerState.close() } }
                                )
                            }
                        }

                        // ── Tools ──
                        if (matches("Tools") || matches("Automation") || matches("Macro") || matches("HID") || matches("SSH") || matches("Remote Explorer") || matches("ADB") || matches("Process") || matches("Stats") || matches("Auto Clicker") || matches("Injector") || matches("Wake-on-LAN")) {
                            DrawerSectionLabel("Tools")

                            if (matches("Automation")) {
                                DrawerNavItem(
                                    label = "Automation",
                                    icon = Icons.Default.Bolt,
                                    isSelected = currentRoute == "automation",
                                    onClick = { onNavigate("automation"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("Macro Orchestrator")) {
                                DrawerNavItem(
                                    label = "Macro Orchestrator",
                                    icon = Icons.Default.SettingsInputComponent,
                                    isSelected = currentRoute == "macro_orchestrator",
                                    onClick = { onNavigate("macro_orchestrator"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("HID Brute Force")) {
                                DrawerNavItem(
                                    label = "HID Brute Force",
                                    icon = Icons.Default.Security,
                                    isSelected = currentRoute == "hid_brute_force",
                                    onClick = { onNavigate("hid_brute_force"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("SSH Terminal")) {
                                DrawerNavItem(
                                    label = "SSH Terminal",
                                    icon = Icons.Default.Terminal,
                                    isSelected = currentRoute == "ssh_terminal",
                                    onClick = { onNavigate("ssh_terminal"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("Remote Explorer")) {
                                DrawerNavItem(
                                    label = "Remote Explorer",
                                    icon = Icons.Default.FolderZip,
                                    isSelected = currentRoute == "remote_explorer",
                                    onClick = { onNavigate("remote_explorer"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("ADB Manager")) {
                                DrawerNavItem(
                                    label = "ADB Manager",
                                    icon = Icons.Default.PhoneAndroid,
                                    isSelected = currentRoute == "adb_manager",
                                    onClick = { onNavigate("adb_manager"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("Process Manager")) {
                                DrawerNavItem(
                                    label = "Process Manager",
                                    icon = Icons.Default.Memory,
                                    isSelected = currentRoute == "process_manager",
                                    onClick = { onNavigate("process_manager"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("System Stats")) {
                                DrawerNavItem(
                                    label = "System Stats",
                                    icon = Icons.Default.Speed,
                                    isSelected = currentRoute == "system_stats",
                                    onClick = { onNavigate("system_stats"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("Auto Clicker")) {
                                DrawerNavItem(
                                    label = "Auto Clicker",
                                    icon = Icons.Default.AdsClick,
                                    isSelected = currentRoute == "auto_clicker",
                                    onClick = { onNavigate("auto_clicker"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("Payload Injector")) {
                                DrawerNavItem(
                                    label = "Payload Injector",
                                    icon = Icons.Default.ElectricBolt,
                                    isSelected = currentRoute == "injector",
                                    onClick = { onNavigate("injector"); scope.launch { drawerState.close() } }
                                )
                            }
                        }

                        // ── Intelligence ──
                        if (matches("Intelligence") || matches("AI") || matches("Assistant") || matches("Browser") || matches("Snippets")) {
                            DrawerSectionLabel("Intelligence")

                            DrawerNavItem(
                                label = "AI Assistant",
                                icon = Icons.Default.AutoAwesome,
                                isSelected = currentRoute == "assistant",
                                onClick = { onNavigate("assistant"); scope.launch { drawerState.close() } }
                            )
                            DrawerNavItem(
                                label = "Browser",
                                icon = Icons.Default.Explore,
                                isSelected = currentRoute == "browser",
                                onClick = { onNavigate("browser"); scope.launch { drawerState.close() } }
                            )
                            DrawerNavItem(
                                label = "Snippets",
                                icon = Icons.Default.ContentPaste,
                                isSelected = currentRoute == "snippets",
                                onClick = { onNavigate("snippets"); scope.launch { drawerState.close() } }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            thickness = 0.5.dp,
                            color = BorderColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Strategic Labs ──
                        if (matches("Strategic Labs") || matches("Terminal") || matches("Recon") || matches("Auditor") || matches("Attacker") || matches("QA") || matches("Web") || matches("Forge") || matches("Horizon") || matches("Sensor") || matches("OSINT") || matches("Shadow") || matches("Mirror") || matches("Port") || matches("Ping") || matches("Traceroute") || matches("Labs")) {
                            DrawerSectionLabel("Strategic Labs")
                            if (matches("Local Terminal")) {
                                DrawerNavItem(
                                    label = "Local Terminal",
                                    icon = Icons.Default.Code,
                                    isSelected = currentRoute == "local_terminal",
                                    onClick = { onNavigate("local_terminal"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("Ghost Recon")) {
                                DrawerNavItem(
                                    label = "Ghost Recon",
                                    icon = Icons.Default.Radar,
                                    isSelected = currentRoute == "ghost_recon",
                                    onClick = { onNavigate("ghost_recon"); scope.launch { drawerState.close() } }
                                )
                            }
                             if (matches("Network Auditor") || matches("Port") || matches("Ping") || matches("Traceroute")) {
                                 DrawerNavItem(
                                     label = "Network Auditor",
                                     icon = Icons.Default.ScreenSearchDesktop,
                                     isSelected = currentRoute == "network_auditor",
                                     onClick = { onNavigate("network_auditor"); scope.launch { drawerState.close() } }
                                 )
                             }
                             if (matches("Wireless Auditor") || matches("BLE") || matches("Wi-Fi")) {
                                 DrawerNavItem(
                                     label = "Wireless Auditor",
                                     icon = Icons.Default.BluetoothConnected,
                                     isSelected = currentRoute == "wireless_auditor",
                                     onClick = { onNavigate("wireless_auditor"); scope.launch { drawerState.close() } }
                                 )
                             }
                            if (matches("Neural Payload Forge")) {
                                DrawerNavItem(
                                    label = "Neural Payload Forge",
                                    icon = Icons.Default.Bolt,
                                    isSelected = currentRoute == "payload_forge",
                                    onClick = { onNavigate("payload_forge"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("Rogue Horizon")) {
                                DrawerNavItem(
                                    label = "Rogue Horizon",
                                    icon = Icons.Default.WifiTetheringError,
                                    isSelected = currentRoute == "rogue_horizon",
                                    onClick = { onNavigate("rogue_horizon"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("Subdomain Scanner")) {
                                DrawerNavItem(
                                    label = "Subdomain Scanner",
                                    icon = Icons.Default.TravelExplore,
                                    isSelected = currentRoute == "subdomain_scanner",
                                    onClick = { onNavigate("subdomain_scanner"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("Bluetooth Shadow")) {
                                DrawerNavItem(
                                    label = "Bluetooth Shadow",
                                    icon = Icons.Default.Radar,
                                    isSelected = currentRoute == "bluetooth_shadow",
                                    onClick = { onNavigate("bluetooth_shadow"); scope.launch { drawerState.close() } }
                                )
                            }

                            // LABS SUBSECTION
                        }

                        // ── Ethical Hacking ──
                        if (matches("Ethical Hacking") || matches("Auditor") || matches("Security") || matches("NVA") || matches("Hash") || matches("Cracker") || matches("Reverse") || matches("Shell") || matches("Stego")) {
                            DrawerSectionLabel("Ethical Hacking")
                            if (matches("Crypto Encoder")) {
                                DrawerNavItem(
                                    label = "Crypto Encoder",
                                    icon = Icons.Default.Code,
                                    isSelected = currentRoute == "crypto_encoder",
                                    onClick = { onNavigate("crypto_encoder"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("Neural Auditor")) {
                                DrawerNavItem(
                                    label = "Neural Auditor",
                                    icon = Icons.Default.Shield,
                                    isSelected = currentRoute == "security_auditor",
                                    onClick = { onNavigate("security_auditor"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("Neural Packet Inspector") || matches("Sniffer") || matches("Traffic")) {
                                DrawerNavItem(
                                    label = "Neural Packet Inspector",
                                    icon = Icons.Default.Monitor,
                                    isSelected = currentRoute == "traffic_analyzer",
                                    onClick = { onNavigate("traffic_analyzer"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (matches("Reverse Shell")) {
                                DrawerNavItem(
                                    label = "Reverse Shell Gen",
                                    icon = Icons.Default.Terminal,
                                    isSelected = currentRoute == "reverse_shell_gen",
                                    onClick = { onNavigate("reverse_shell_gen"); scope.launch { drawerState.close() } }
                                )
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))

                        // ── Tactical Research Labs ──
                        if (matches("Lab") || matches("Research") || matches("Neural") || matches("Sensor") || matches("Vision") || matches("Macro") || matches("Steganography")) {
                            DrawerSectionLabel("Tactical Research Labs")
                            DrawerNavItem(label = "Neural Lab", icon = Icons.Default.BugReport, isSelected = currentRoute == "neural_lab", onClick = { onNavigate("neural_lab"); scope.launch { drawerState.close() } })
                            DrawerNavItem(label = "Sensor Lab", icon = Icons.Default.Stream, isSelected = currentRoute == "sensor_lab", onClick = { onNavigate("sensor_lab"); scope.launch { drawerState.close() } })
                            DrawerNavItem(label = "Screenshot Lab", icon = Icons.Default.AddAPhoto, isSelected = currentRoute == "screenshot_lab", onClick = { onNavigate("screenshot_lab"); scope.launch { drawerState.close() } })
                            DrawerNavItem(label = "Vision Lab", icon = Icons.Default.Visibility, isSelected = currentRoute == "vision_lab", onClick = { onNavigate("vision_lab"); scope.launch { drawerState.close() } })
                            DrawerNavItem(label = "Macro Lab", icon = Icons.Default.FiberManualRecord, isSelected = currentRoute == "macro_lab", onClick = { onNavigate("macro_lab"); scope.launch { drawerState.close() } })
                            DrawerNavItem(label = "Steganography Lab", icon = Icons.Default.HideImage, isSelected = currentRoute == "stego_lab", onClick = { onNavigate("stego_lab"); scope.launch { drawerState.close() } })
                            DrawerNavItem(label = "Keystroke Monitor", icon = Icons.Default.KeyboardAlt, isSelected = currentRoute == "keystroke_monitor", onClick = { onNavigate("keystroke_monitor"); scope.launch { drawerState.close() } })
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
                        
                        // Stealth Decoy Toggle
                        val decoyViewModel: com.example.rabit.ui.stealth.DecoyViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                        val isStealth by decoyViewModel.isStealthMode.collectAsState()
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(if (isStealth) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = if (isStealth) Color.Red else SuccessGreen, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("DECOY PROTOCOL", color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                            Switch(
                                checked = isStealth,
                                onCheckedChange = { decoyViewModel.toggleStealthIcon(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Red, checkedTrackColor = Color.Red.copy(alpha = 0.3f))
                            )
                        }
                        // Items moved to Strategic Labs

                        Spacer(modifier = Modifier.height(8.dp))

                        // --- Active Exploitation ---
                        Text(
                            text = "ACTIVE EXPLOITATION",
                            color = Silver.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Web Sniper (DAST) ──
                        if (matches("Web Sniper") || matches("DAST") || matches("Auto-Pwn") || matches("Fuzzer") || matches("Enumerator") || matches("Repeater")) {
                            DrawerSectionLabel("Web Sniper (DAST)")
                            DrawerNavItem(
                                label = "Web Sniper Hub",
                                icon = Icons.Default.LocationSearching,
                                isSelected = currentRoute == "web_sniper",
                                onClick = { onNavigate("web_sniper"); scope.launch { drawerState.close() } }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // --- OPSEC & SECURITY ---
                        Text(
                            text = "OPSEC & SECURITY",
                            color = Silver.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                        if (onPanicLock != null) {
                            Surface(
                                onClick = { 
                                    onPanicLock()
                                    scope.launch { drawerState.close() }
                                },
                                color = Color.Red.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text("LOCK SESSION", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        DrawerNavItem(
                            label = "Panic Terminal",
                            icon = Icons.Default.Warning,
                            isSelected = currentRoute == "panic_terminal",
                            onClick = { onNavigate("panic_terminal"); scope.launch { drawerState.close() } }
                        )
                        DrawerNavItem(
                            label = "Kill Switch",
                            icon = Icons.Default.DeleteForever,
                            isSelected = currentRoute == "kill_switch",
                            onClick = { onNavigate("kill_switch"); scope.launch { drawerState.close() } }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Settings group ──
                        DrawerNavItem(
                            label = "Passwords",
                            icon = Icons.Default.Password,
                            isSelected = currentRoute == "password_manager",
                            onClick = { onNavigate("password_manager"); scope.launch { drawerState.close() } }
                        )
                        DrawerNavItem(
                            label = "Settings",
                            icon = Icons.Default.Settings,
                            isSelected = currentRoute == "settings",
                            onClick = { onNavigate("settings"); scope.launch { drawerState.close() } }
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (showTopBar) {
                    Surface(
                        color = Surface0.copy(alpha = 0.98f),
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TopAppBar(
                            title = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = screenTitle,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        letterSpacing = 0.3.sp
                                    )
                                    Text(
                                        text = appBarSubtitle,
                                        color = TextSecondary.copy(alpha = 0.9f),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.alpha(0.95f)
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = openDrawer) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = "Open navigation menu",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            actions = {
                                topBarActions()
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = TextPrimary
                            ),
                            modifier = Modifier.height(74.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            AccentBlue.copy(alpha = 0.35f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppAtmosphereGradient)
            ) {
                CompositionLocalProvider(LocalOpenGlobalDrawer provides openDrawer) {
                    content(padding)
                }
            }
        }
    }
}

// ── Drawer Components ────────────────────────────────────────────────────────

@Composable
private fun DrawerSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        color = TextTertiary,
        fontSize = 9.sp,
        fontWeight = FontWeight.W700,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun DrawerNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (isSelected) TextPrimary else TextSecondary
    val iconTint = if (isSelected) AccentBlue else TextSecondary.copy(alpha = 0.7f)

    Surface(
        onClick = onClick,
        color = if (isSelected) AccentBlue.copy(alpha = 0.08f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 1.dp)
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = label
                selected = isSelected
                stateDescription = if (isSelected) "Selected" else "Not selected"
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent edge indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AccentBlue)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                label,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400
            )
        }
    }
}

// Keep the old DrawerItem signature for any external callers
@Composable
fun DrawerItem(
    label: String,
    subLabel: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DrawerNavItem(
        label = label,
        icon = icon,
        isSelected = isSelected,
        onClick = onClick
    )
}
