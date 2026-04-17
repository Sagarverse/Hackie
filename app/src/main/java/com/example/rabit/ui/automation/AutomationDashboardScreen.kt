package com.example.rabit.ui.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.domain.model.CustomMacro
import com.example.rabit.domain.model.EmergencyAction
import com.example.rabit.domain.model.HidKeyCodes
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationDashboardScreen(
    viewModel: AutomationViewModel,
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToWakeOnLan: () -> Unit = {},
    onNavigateToSshTerminal: () -> Unit = {},
    onNavigateToProcessManager: () -> Unit = {},
    onNavigateToSystemStats: () -> Unit = {},
    onNavigateTo: (String) -> Unit = {}
) {
    val customMacros by viewModel.customMacros.collectAsState()
    val emergencyStatus by viewModel.emergencyStatus.collectAsState()
    val connectionState by mainViewModel.connectionState.collectAsState<HidDeviceManager.ConnectionState>()
    val isConnected = connectionState is com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connected
    val isScanning by viewModel.isTerminalScanning.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showTerminalLab by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }

    val systemMacros = remember {
        listOf(
            MacroDefinition("Unlock Mac", Icons.Default.LockOpen, AccentBlue, "UNLOCK_CMD"),
            MacroDefinition("Lock Mac", Icons.Default.Lock, AccentBlue, "LOCK_CMD"),
            MacroDefinition("Spotlight", Icons.Default.Search, AccentBlue, "SPOT_CMD"),
            MacroDefinition("Screen Cap", Icons.Default.Screenshot, AccentTeal, "SHOT_CMD"),
            MacroDefinition("Mute Mic", Icons.Default.MicOff, AccentBlue, "MUTE_CMD"),
            MacroDefinition("Sleep Mac", Icons.Default.NightsStay, Silver, "SLEEP_CMD"),
            MacroDefinition("Say Hello", Icons.Default.RecordVoiceOver, Platinum, "SAY_HELLO_CMD"),
            MacroDefinition("Dark Mode", Icons.Default.DarkMode, AccentBlue, "TOGGLE_DARK_MODE_CMD"),
            MacroDefinition("Sys Info", Icons.Default.Info, AccentBlue, "INFO_CMD"),
            MacroDefinition("Force Quit", Icons.Default.Cancel, AccentBlue, "FORCE_QUIT_CMD")
        )
    }
    val webMacros = remember {
        listOf(
            MacroDefinition("New Tab", Icons.Default.Add, AccentBlue, "TAB_CMD"),
            MacroDefinition("Reload", Icons.Default.Refresh, AccentBlue, "RELOAD_CMD"),
            MacroDefinition("History", Icons.Default.History, AccentBlue, "HIST_CMD"),
            MacroDefinition("Private", Icons.Default.Shield, Silver, "PRIV_CMD"),
            MacroDefinition("Go Back", Icons.AutoMirrored.Filled.ArrowBack, Silver, "BACK_CMD"),
            MacroDefinition("FS Mode", Icons.Default.Fullscreen, AccentTeal, "FS_CMD")
        )
    }
    val productivityMacros = remember {
        listOf(
            MacroDefinition("Mission Ctrl", Icons.Default.GridView, AccentBlue, "MC_CMD"),
            MacroDefinition("Switch App", Icons.Default.Tab, AccentBlue, "SW_CMD"),
            MacroDefinition("Hide Others", Icons.Default.VisibilityOff, AccentBlue, "HIDE_CMD"),
            MacroDefinition("Terminal", Icons.Default.Code, Platinum, "TERM_CMD"),
            MacroDefinition("Open Safari", Icons.Default.Language, AccentBlue, "LAUNCH_SAFARI"),
            MacroDefinition("Open Spotify", Icons.Default.MusicNote, AccentBlue, "LAUNCH_SPOTIFY")
        )
    }
    val creativeMacros = remember {
        listOf(
            MacroDefinition("Play/Pause", Icons.Default.PlayCircle, Platinum, "PLAY_CMD"),
            MacroDefinition("Zoom In", Icons.Default.ZoomIn, Silver, "ZI_CMD"),
            MacroDefinition("Zoom Out", Icons.Default.ZoomOut, Silver, "ZO_CMD"),
            MacroDefinition("Render", Icons.Default.Movie, AccentBlue, "RENDER_CMD"),
            MacroDefinition("Export", Icons.Default.IosShare, AccentTeal, "EXPORT_CMD")
        )
    }

    val filteredSystem = remember(searchQuery) { filterMacros(systemMacros, searchQuery) }
    val filteredWeb = remember(searchQuery) { filterMacros(webMacros, searchQuery) }
    val filteredProductivity = remember(searchQuery) { filterMacros(productivityMacros, searchQuery) }
    val filteredCreative = remember(searchQuery) { filterMacros(creativeMacros, searchQuery) }
    val customMacroList = customMacros.map { MacroDefinition(it.name, Icons.Default.Bolt, AccentBlue, it.command) }
    val filteredCustom = remember(searchQuery, customMacros) { filterMacros(customMacroList, searchQuery) }
    val totalMatches = filteredSystem.size + filteredWeb.size + filteredProductivity.size + filteredCreative.size + filteredCustom.size

    LaunchedEffect(Unit) {
        contentVisible = true
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = tween(320)) + slideInVertically(initialOffsetY = { it / 14 }, animationSpec = tween(320))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        label = { Text("Search macros, commands, categories") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                }
                if (searchQuery.isNotBlank()) {
                    item {
                        Text(
                            text = "$totalMatches result(s) for '$searchQuery'",
                            color = Silver,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }


                if (searchQuery.isBlank()) {
                    item {
                        QuickToolPanel(
                            onSystemStats = { onNavigateTo("system_stats") },
                            onProcessManager = { onNavigateTo("process_manager") },
                            onSshTerminal = { onNavigateTo("ssh_terminal") },
                            onRemoteExplorer = { onNavigateTo("remote_explorer") },
                            onAutoClicker = { onNavigateTo("auto_clicker") }
                        )
                        
                        if (showTerminalLab) {
                            TerminalLabSection(
                                onScan = { onNavigateTo("terminal_scanner") },
                                onUnlockSsh = {
                                    viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(400) && TEXT(Terminal) && KEY(ENTER) && WAIT(1500) && TEXT(ssh local-mac) && KEY(ENTER)")
                                    showTerminalLab = false
                                }
                            )
                        }
                    }

                    item {
                        EmergencyControlPanel(
                            status = emergencyStatus,
                            onAction = { viewModel.runEmergencyAction(it) }
                        )
                    }
                }

                item {
                    IntegratedShortcutPanel(automationViewModel = viewModel, mainViewModel = mainViewModel, query = searchQuery)
                }

                // ─── SYSTEM CORE ───
                if (filteredSystem.isNotEmpty()) item {
                    MacroCategory(
                        title = "SYSTEM CONTROL",
                        icon = Icons.Default.Terminal,
                        macros = filteredSystem,
                        onMacroClick = { handleMacro(it.command, viewModel, mainViewModel) },
                        enabled = isConnected
                    )
                }

                // ─── WEB & BROWSER ───
                if (filteredWeb.isNotEmpty()) item {
                    MacroCategory(
                        title = "WEB & BROWSER",
                        icon = Icons.Default.Language,
                        macros = filteredWeb,
                        onMacroClick = { handleMacro(it.command, viewModel, mainViewModel) },
                        enabled = isConnected
                    )
                }

                // ─── PRODUCTIVITY ───
                if (filteredProductivity.isNotEmpty()) item {
                    MacroCategory(
                        title = "PRODUCTIVITY",
                        icon = Icons.Default.AutoMode,
                        macros = filteredProductivity,
                        onMacroClick = { handleMacro(it.command, viewModel, mainViewModel) },
                        enabled = isConnected
                    )
                }

                // ─── CREATIVE STUDIO ───
                if (filteredCreative.isNotEmpty()) item {
                    MacroCategory(
                        title = "CREATIVE STUDIO",
                        icon = Icons.Default.Palette,
                        macros = filteredCreative,
                        onMacroClick = { handleMacro(it.command, viewModel, mainViewModel) },
                        enabled = isConnected
                    )
                }

                // ─── USER MACROS ───
                if (filteredCustom.isNotEmpty() || searchQuery.isBlank()) item {
                    MacroCategory(
                        title = "USER CUSTOM",
                        icon = Icons.Default.SettingsSuggest,
                        macros = filteredCustom,
                        onMacroClick = { handleMacro(it.command, viewModel, mainViewModel) },
                        onDeleteClick = { macroItem -> 
                            customMacros.find { it: CustomMacro -> it.name == macroItem.name && it.command == macroItem.command }?.let {
                                viewModel.deleteCustomMacro(it)
                            }
                        },
                        onAddClick = { showAddDialog = true },
                        enabled = isConnected
                    )
                }
                if (searchQuery.isNotBlank() && totalMatches == 0) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Graphite.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                "No macros found for '$searchQuery'",
                                color = Silver,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var command by remember { mutableStateOf("") }
        val complexExample = remember {
            """
            KEY(CMD+SPACE)
            WAIT(300)
            TEXT(Terminal)
            KEY(ENTER)
            WAIT(700)
            TEXT(whoami)
            KEY(ENTER)
            """.trimIndent()
        }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Graphite,
            title = { Text("Define New Macro", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Write complex steps with one command per line or use '&&'.", color = Silver, fontSize = 12.sp)
                    Surface(
                        color = SoftGrey.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Supported syntax", color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("KEY(CMD+SPACE)", color = Silver, fontSize = 11.sp)
                            Text("TEXT(hello world)", color = Silver, fontSize = 11.sp)
                            Text("WAIT(500)", color = Silver, fontSize = 11.sp)
                            Text("MEDIA(MUTE)", color = Silver, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Example: Open Terminal and run whoami", color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text("KEY(CMD+SPACE)", color = AccentBlue, fontSize = 11.sp)
                            Text("WAIT(300)", color = AccentBlue, fontSize = 11.sp)
                            Text("TEXT(Terminal)", color = AccentBlue, fontSize = 11.sp)
                            Text("KEY(ENTER)", color = AccentBlue, fontSize = 11.sp)
                            Text("WAIT(700)", color = AccentBlue, fontSize = 11.sp)
                            Text("TEXT(whoami)", color = AccentBlue, fontSize = 11.sp)
                            Text("KEY(ENTER)", color = AccentBlue, fontSize = 11.sp)
                        }
                    }
                    TextButton(
                        onClick = {
                            if (name.isBlank()) name = "Open Terminal + whoami"
                            command = complexExample
                        }
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Insert Example", color = AccentBlue, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Automation Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum
                        )
                    )
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text("Command Sequence / Text") },
                        placeholder = { Text("KEY(CMD+SPACE)\nWAIT(300)\nTEXT(Terminal)\nKEY(ENTER)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && command.isNotBlank()) {
                            viewModel.addCustomMacro(name, command)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) { Text("Deploy", color = Obsidian, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = Silver) }
            }
        )
    }
}

@Composable
private fun EmergencyControlPanel(
    status: String,
    onAction: (EmergencyAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "EMERGENCY CONTROL",
            color = Platinum.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 12.sp
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Graphite.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    EmergencyButton(
                        label = "Lock",
                        icon = Icons.Default.Lock,
                        onClick = { onAction(EmergencyAction.LOCK_MACHINE) },
                        modifier = Modifier.weight(1f)
                    )
                    EmergencyButton(
                        label = "Kill Internet",
                        icon = Icons.Default.WifiOff,
                        onClick = { onAction(EmergencyAction.KILL_INTERNET_ADAPTER) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    EmergencyButton(
                        label = "Stop Audio",
                        icon = Icons.AutoMirrored.Filled.VolumeOff,
                        onClick = { onAction(EmergencyAction.STOP_AUDIO) },
                        modifier = Modifier.weight(1f)
                    )
                    EmergencyButton(
                        label = "Clear Clipboard",
                        icon = Icons.Default.ContentPasteOff,
                        onClick = { onAction(EmergencyAction.CLEAR_CLIPBOARD) },
                        modifier = Modifier.weight(1f)
                    )
                }
                EmergencyButton(
                    label = "Close Sensitive Apps",
                    icon = Icons.Default.NoAccounts,
                    onClick = { onAction(EmergencyAction.CLOSE_SENSITIVE_APPS) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Status: $status",
                    color = Silver,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, start = 2.dp)
                )
                Text(
                    text = "Note: Network/clipboard/app-closing use SSH when available for host-level control.",
                    color = Silver.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun EmergencyButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = SoftGrey.copy(alpha = 0.18f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuickToolPanel(
    onWakeOnLan: () -> Unit = {},
    onSshTerminal: () -> Unit = {},
    onSystemStats: () -> Unit = {},
    onProcessManager: () -> Unit = {},
    onRemoteExplorer: () -> Unit = {},
    onAutoClicker: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ToolCard(
                title = "Remote Explorer",
                desc = "ADB File Provider",
                icon = Icons.Default.FolderOpen,
                color = AccentBlue,
                onClick = onRemoteExplorer,
                modifier = Modifier.weight(1f)
            )

            ToolCard(
                title = "SSH Terminal",
                desc = "Raw remote command",
                icon = Icons.Default.Terminal,
                color = Color.White,
                onClick = onSshTerminal,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ToolCard(
                title = "Process Manager",
                desc = "Force quit apps",
                icon = Icons.Default.Cancel,
                color = Color.Red,
                onClick = onProcessManager,
                modifier = Modifier.weight(1f)
            )

            ToolCard(
                title = "Live Monitor",
                desc = "Real-time health",
                icon = Icons.Default.Speed,
                color = AccentTeal,
                onClick = onSystemStats,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ToolCard(
                title = "Auto Clicker",
                desc = "Automated Clicks",
                icon = Icons.Default.AdsClick,
                color = WarningYellow,
                onClick = onAutoClicker,
                modifier = Modifier.weight(1f)
            )
            
            // Placeholder for future expansion or dual-card row
            Box(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = SoftGrey.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f)),
        modifier = modifier.height(72.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = Platinum, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(desc, color = Silver, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun IntegratedShortcutPanel(automationViewModel: AutomationViewModel, mainViewModel: MainViewModel, query: String) {
    val categories = remember { buildIntegratedShortcutCategories() }
    val normalized = query.trim().lowercase()
    val filteredCategories = if (normalized.isBlank()) {
        categories
    } else {
        categories.mapNotNull { category ->
            val shortcuts = category.shortcuts.filter {
                it.name.lowercase().contains(normalized) || it.keys.lowercase().contains(normalized) || category.name.lowercase().contains(normalized)
            }
            if (shortcuts.isNotEmpty()) category.copy(shortcuts = shortcuts) else null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "SHORTCUT GUIDE",
            color = Platinum.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 12.sp
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Graphite.copy(alpha = 0.45f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (filteredCategories.isEmpty()) {
                    Text("No shortcut guide entries match this search.", color = Silver, fontSize = 12.sp)
                }

                filteredCategories.forEach { category ->
                    Text(
                        text = category.name,
                        color = category.color,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        category.shortcuts.forEach { shortcut ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (shortcut.consumerCode != null) {
                                            mainViewModel.sendConsumerKey(shortcut.consumerCode)
                                        } else {
                                            mainViewModel.sendKeyCombination(shortcut.codes)
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = SoftGrey.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(shortcut.name, color = Platinum, fontSize = 12.sp)
                                    Text(shortcut.keys, color = Silver, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class IntegratedShortcutItem(
    val name: String,
    val keys: String,
    val codes: List<Byte>,
    val consumerCode: Short? = null
)

private data class IntegratedShortcutCategory(
    val name: String,
    val color: Color,
    val shortcuts: List<IntegratedShortcutItem>
)

private fun buildIntegratedShortcutCategories(): List<IntegratedShortcutCategory> = listOf(
    IntegratedShortcutCategory(
        "System",
        AccentBlue,
        listOf(
            IntegratedShortcutItem("Lock Screen", "Ctrl + Cmd + Q", listOf(HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_Q)),
            IntegratedShortcutItem("Spotlight", "Cmd + Space", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_SPACE)),
            IntegratedShortcutItem("Force Quit", "Alt + Cmd + Esc", listOf(HidKeyCodes.MODIFIER_LEFT_ALT, HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_ESC))
        )
    ),
    IntegratedShortcutCategory(
        "Browser",
        AccentBlue,
        listOf(
            IntegratedShortcutItem("New Tab", "Cmd + T", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_T)),
            IntegratedShortcutItem("Reload", "Cmd + R", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_R)),
            IntegratedShortcutItem("Address Bar", "Cmd + L", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_L))
        )
    ),
    IntegratedShortcutCategory(
        "Media",
        AccentBlue,
        listOf(
            IntegratedShortcutItem("Play / Pause", "Media", emptyList(), consumerCode = HidKeyCodes.MEDIA_PLAY_PAUSE),
            IntegratedShortcutItem("Volume Up", "Media", emptyList(), consumerCode = HidKeyCodes.MEDIA_VOL_UP),
            IntegratedShortcutItem("Volume Down", "Media", emptyList(), consumerCode = HidKeyCodes.MEDIA_VOL_DOWN)
        )
    )
)

@Composable
fun MacroCategory(
    title: String,
    icon: ImageVector,
    macros: List<MacroDefinition>,
    onMacroClick: (MacroDefinition) -> Unit,
    onDeleteClick: ((MacroDefinition) -> Unit)? = null,
    onAddClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Platinum.copy(alpha=0.4f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = Platinum.copy(alpha=0.6f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            if (onAddClick != null) {
                IconButton(onClick = onAddClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = AccentBlue, modifier = Modifier.size(18.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Grid of macros
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val chunks = macros.chunked(2)
            chunks.forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEach { macro ->
                        MacroGridItem(
                            macro = macro, 
                            onClick = { onMacroClick(macro) }, 
                            onDelete = if (onDeleteClick != null) { { onDeleteClick(macro) } } else null,
                            enabled = enabled,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun MacroGridItem(
    macro: MacroDefinition,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = SoftGrey.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f)),
        modifier = modifier.height(64.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(36.dp).background(macro.color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(macro.icon, contentDescription = null, tint = macro.color, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = macro.name,
                    color = if (enabled) Platinum else Silver.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = Silver.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}


data class MacroDefinition(val name: String, val icon: ImageVector, val color: Color, val command: String)

@Composable
fun TerminalLabSection(
    onScan: () -> Unit,
    onUnlockSsh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Platinum.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = AccentTeal.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Science, null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("TERMINAL LAB", color = Platinum, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 1.sp)
                    Text("Bridge & Node Management", color = Silver.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onScan,
                    modifier = Modifier.weight(1f).height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Platinum.copy(alpha = 0.05f))
                ) {
                    Icon(Icons.Default.Radar, null, tint = Platinum, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SCAN", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                
                Button(
                    onClick = onUnlockSsh,
                    modifier = Modifier.weight(1f).height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Usb, null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("HID UNLOCK", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun filterMacros(list: List<MacroDefinition>, query: String): List<MacroDefinition> {
    val q = query.trim().lowercase()
    if (q.isBlank()) return list
    return list.filter {
        it.name.lowercase().contains(q) || it.command.lowercase().contains(q)
    }
}

private fun handleMacro(command: String, viewModel: AutomationViewModel, mainViewModel: MainViewModel) {
    if (command.startsWith("DUCKY:")) {
        viewModel.executeDuckyScript(command.substring(6).trim())
        return
    }

    when (command) {
        "UNLOCK_CMD" -> {
            mainViewModel.unlockMac()
        }
        "LOCK_CMD" -> viewModel.executeMacro2Script("KEY(CMD+CTRL+Q)")
        "SPOT_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SPACE)")
        "SHOT_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SHIFT+4)")
        "MUTE_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SHIFT+M)")
        "SLEEP_CMD" -> viewModel.executeMacro2Script("KEY(CMD+ALT+POWER)")
        "SAY_HELLO_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(200) && TEXT(Terminal) && KEY(ENTER) && WAIT(500) && TEXT(say hello) && KEY(ENTER)")
        "TOGGLE_DARK_MODE_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(200) && TEXT(Terminal) && KEY(ENTER) && WAIT(500) && TEXT(osascript -e 'tell app \"System Events\" to tell appearance preferences to set dark mode to not dark mode') && KEY(ENTER)")
        "INFO_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(200) && TEXT(System Information) && KEY(ENTER)")
        "FORCE_QUIT_CMD" -> viewModel.executeMacro2Script("KEY(CMD+ALT+ESC)")

        "TAB_CMD" -> viewModel.executeMacro2Script("KEY(CMD+T)")
        "RELOAD_CMD" -> viewModel.executeMacro2Script("KEY(CMD+R)")
        "HIST_CMD" -> viewModel.executeMacro2Script("KEY(CMD+Y)") 
        "PRIV_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SHIFT+N)") 
        "BACK_CMD" -> viewModel.executeMacro2Script("KEY(CMD+[)")
        "FS_CMD" -> viewModel.executeMacro2Script("KEY(CMD+CTRL+F)")

        "MC_CMD" -> viewModel.executeMacro2Script("KEY(CTRL+UP)")
        "SW_CMD" -> viewModel.executeMacro2Script("KEY(CMD+TAB)")
        "HIDE_CMD" -> viewModel.executeMacro2Script("KEY(CMD+H)")
        "TERM_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(200) && TEXT(Terminal) && KEY(ENTER)")
        "LAUNCH_SAFARI" -> viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(200) && TEXT(Safari) && KEY(ENTER)")
        "LAUNCH_SPOTIFY" -> viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(200) && TEXT(Spotify) && KEY(ENTER)")

        "PLAY_CMD" -> viewModel.executeMacro2Script("MEDIA(PLAY)")
        "ZI_CMD" -> viewModel.executeMacro2Script("KEY(CMD+=)")
        "ZO_CMD" -> viewModel.executeMacro2Script("KEY(CMD+-)")
        "RENDER_CMD" -> viewModel.executeMacro2Script("KEY(CMD+M)")
        "EXPORT_CMD" -> viewModel.executeMacro2Script("KEY(CMD+E)")

        else -> viewModel.executeMacro2Script(command) 
    }
}
