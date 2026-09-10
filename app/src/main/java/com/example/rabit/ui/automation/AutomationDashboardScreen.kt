package com.example.rabit.ui.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.rabit.ui.theme.hackieColors
import com.example.rabit.ui.components.ScreenScaffold

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

    val scope = rememberCoroutineScope()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    val colors = hackieColors()

    val systemMacros = remember(colors.accentTeal, colors.textSecondary) {
        listOf(
            MacroDefinition("Unlock Device", Icons.Default.LockOpen, colors.accentTeal, "UNLOCK_CMD"),
            MacroDefinition("Lock Device", Icons.Default.Lock, colors.accentTeal, "LOCK_CMD"),
            MacroDefinition("Search/Spotlight", Icons.Default.Search, colors.accentTeal, "SPOT_CMD"),
            MacroDefinition("Screen Cap", Icons.Default.Screenshot, colors.accentTeal, "SHOT_CMD"),
            MacroDefinition("Screen Cap Region", Icons.Default.Crop, colors.textSecondary, "SHOT_REGION_CMD"),
            MacroDefinition("Mute Mic", Icons.Default.MicOff, colors.accentTeal, "MUTE_CMD"),
            MacroDefinition("Sleep Device", Icons.Default.NightsStay, colors.textSecondary, "SLEEP_CMD"),
            MacroDefinition("Sys Info", Icons.Default.Info, colors.accentTeal, "INFO_CMD"),
            MacroDefinition("Volume Up", Icons.Default.VolumeUp, colors.accentTeal, "VOL_UP_CMD"),
            MacroDefinition("Volume Down", Icons.Default.VolumeDown, colors.textSecondary, "VOL_DOWN_CMD"),
            MacroDefinition("Volume Mute", Icons.Default.VolumeOff, colors.textSecondary, "VOL_MUTE_CMD"),
            MacroDefinition("Brightness Up", Icons.Default.BrightnessHigh, colors.accentTeal, "BRIGHT_UP_CMD"),
            MacroDefinition("Brightness Down", Icons.Default.BrightnessLow, colors.textSecondary, "BRIGHT_DOWN_CMD"),
            MacroDefinition("Play/Pause", Icons.Default.PlayArrow, colors.accentTeal, "MEDIA_PLAY_CMD"),
            MacroDefinition("Next Track", Icons.Default.SkipNext, colors.textSecondary, "MEDIA_NEXT_CMD"),
            MacroDefinition("Prev Track", Icons.Default.SkipPrevious, colors.textSecondary, "MEDIA_PREV_CMD"),
            MacroDefinition("Force Quit Menu", Icons.Default.Close, colors.accentTeal, "FORCE_QUIT_CMD"),
            MacroDefinition("Activity Monitor", Icons.Default.Monitor, colors.accentTeal, "ACT_MONITOR_CMD")
        )
    }
    val webMacros = remember(colors.accentTeal, colors.textSecondary) {
        listOf(
            MacroDefinition("New Tab", Icons.Default.Add, colors.accentTeal, "TAB_CMD"),
            MacroDefinition("Close Tab", Icons.Default.Close, colors.textSecondary, "CLOSE_TAB_CMD"),
            MacroDefinition("Reopen Tab", Icons.Default.Restore, colors.accentTeal, "REOPEN_TAB_CMD"),
            MacroDefinition("Reload", Icons.Default.Refresh, colors.accentTeal, "RELOAD_CMD"),
            MacroDefinition("Hard Reload", Icons.Default.Sync, colors.accentTeal, "HARD_RELOAD_CMD"),
            MacroDefinition("History", Icons.Default.History, colors.textSecondary, "HIST_CMD"),
            MacroDefinition("Private/Incognito", Icons.Default.Shield, colors.accentTeal, "PRIV_CMD"),
            MacroDefinition("Go Back", Icons.AutoMirrored.Filled.ArrowBack, colors.textSecondary, "BACK_CMD"),
            MacroDefinition("Go Forward", Icons.AutoMirrored.Filled.ArrowForward, colors.textSecondary, "FORWARD_CMD"),
            MacroDefinition("FS Mode", Icons.Default.Fullscreen, colors.accentTeal, "FS_CMD"),
            MacroDefinition("Dev Tools", Icons.Default.DeveloperMode, colors.accentTeal, "DEV_TOOLS_CMD"),
            MacroDefinition("Zoom In", Icons.Default.ZoomIn, colors.accentTeal, "WEB_ZOOM_IN_CMD"),
            MacroDefinition("Zoom Out", Icons.Default.ZoomOut, colors.textSecondary, "WEB_ZOOM_OUT_CMD")
        )
    }
    val productivityMacros = remember(colors.accentTeal, colors.textPrimary) {
        listOf(
            MacroDefinition("Mission Ctrl/Task View", Icons.Default.GridView, colors.accentTeal, "MC_CMD"),
            MacroDefinition("Switch App", Icons.Default.Tab, colors.textSecondary, "SW_CMD"),
            MacroDefinition("Hide App", Icons.Default.VisibilityOff, colors.textSecondary, "HIDE_APP_CMD"),
            MacroDefinition("Hide Others", Icons.Default.VisibilityOff, colors.accentTeal, "HIDE_CMD"),
            MacroDefinition("Terminal", Icons.Default.Code, colors.textPrimary, "TERM_CMD"),
            MacroDefinition("Open Safari/Edge", Icons.Default.Language, colors.accentTeal, "LAUNCH_SAFARI"),
            MacroDefinition("Open Spotify", Icons.Default.MusicNote, colors.textSecondary, "LAUNCH_SPOTIFY"),
            MacroDefinition("Open VS Code", Icons.Default.Code, colors.accentTeal, "LAUNCH_VSCODE"),
            MacroDefinition("Open Notes", Icons.Default.Notes, colors.textSecondary, "LAUNCH_NOTES"),
            MacroDefinition("Copy", Icons.Default.ContentCopy, colors.accentTeal, "COPY_CMD"),
            MacroDefinition("Paste", Icons.Default.ContentPaste, colors.accentTeal, "PASTE_CMD"),
            MacroDefinition("Cut", Icons.Default.ContentCut, colors.textSecondary, "CUT_CMD"),
            MacroDefinition("Select All", Icons.Default.SelectAll, colors.textSecondary, "SELECT_ALL_CMD"),
            MacroDefinition("Undo", Icons.AutoMirrored.Filled.Undo, colors.textSecondary, "UNDO_CMD"),
            MacroDefinition("Redo", Icons.AutoMirrored.Filled.Redo, colors.textSecondary, "REDO_CMD"),
            MacroDefinition("Find", Icons.Default.FindInPage, colors.accentTeal, "FIND_CMD")
        )
    }
    val creativeMacros = remember(colors.accentTeal, colors.textSecondary) {
        listOf(
            MacroDefinition("Zoom In", Icons.Default.ZoomIn, colors.textSecondary, "ZI_CMD"),
            MacroDefinition("Zoom Out", Icons.Default.ZoomOut, colors.textSecondary, "ZO_CMD"),
            MacroDefinition("Render", Icons.Default.Movie, colors.accentTeal, "RENDER_CMD"),
            MacroDefinition("Export", Icons.Default.IosShare, colors.accentTeal, "EXPORT_CMD"),
            MacroDefinition("New Project", Icons.Default.CreateNewFolder, colors.accentTeal, "NEW_PROJ_CMD"),
            MacroDefinition("Save", Icons.Default.Save, colors.accentTeal, "SAVE_CMD"),
            MacroDefinition("Save As", Icons.Default.SaveAs, colors.textSecondary, "SAVE_AS_CMD"),
            MacroDefinition("Color Picker", Icons.Default.Colorize, colors.textSecondary, "COLOR_PICKER_CMD"),
            MacroDefinition("Brush Tool", Icons.Default.Brush, colors.textSecondary, "BRUSH_TOOL_CMD"),
            MacroDefinition("Selection Tool", Icons.Default.CropFree, colors.textSecondary, "SELECT_TOOL_CMD")
        )
    }

    val filteredSystem = remember(searchQuery) { filterMacros(systemMacros, searchQuery) }
    val filteredWeb = remember(searchQuery) { filterMacros(webMacros, searchQuery) }
    val filteredProductivity = remember(searchQuery) { filterMacros(productivityMacros, searchQuery) }
    val filteredCreative = remember(searchQuery) { filterMacros(creativeMacros, searchQuery) }
    val customMacroList = customMacros.map { MacroDefinition(it.name, Icons.Default.Bolt, colors.accentTeal, it.command) }
    val filteredCustom = remember(searchQuery, customMacros) { filterMacros(customMacroList, searchQuery) }
    val totalMatches = filteredSystem.size + filteredWeb.size + filteredProductivity.size + filteredCreative.size + filteredCustom.size

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    val pagerState = rememberPagerState(pageCount = { 2 })

    ScreenScaffold(
        title = "Automation hub",
        subtitle = "System orchestration",
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Pager Indicator
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(2) { iteration ->
                    val color = if (pagerState.currentPage == iteration) colors.accentTeal else colors.outline
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .background(color, CircleShape)
                            .size(8.dp)
                    )
                }
            }
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
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
                            focusedBorderColor = colors.accentTeal,
                            unfocusedBorderColor = colors.outline,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                }
                if (searchQuery.isNotBlank()) {
                    item {
                        Text(
                            text = "$totalMatches result(s) for '$searchQuery'",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }


                if (searchQuery.isBlank()) {
                // ─── QUICK COMMAND BAR ───
                item {
                    var quickCmd by rememberSaveable { mutableStateOf("") }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface1.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.outline.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("QUICK HID COMMAND", color = colors.textPrimary.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = quickCmd,
                                onValueChange = { quickCmd = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Type command to send to host...", fontSize = 13.sp) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (quickCmd.isNotBlank()) {
                                                viewModel.executeMacro2Script("TEXT($quickCmd) && KEY(ENTER)")
                                                quickCmd = ""
                                            }
                                        },
                                        enabled = isConnected
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = if (isConnected) colors.accentTeal else colors.textSecondary)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.accentTeal,
                                    unfocusedBorderColor = colors.outline,
                                    focusedTextColor = colors.textPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Text("Hits ENTER automatically. For raw text use shortcuts below.", color = colors.textSecondary.copy(alpha = 0.5f), fontSize = 10.sp)
                        }
                    }
                }

                item {
                    EmergencyControlPanel(
                        status = emergencyStatus,
                        onAction = { viewModel.runEmergencyAction(it) }
                    )
                }

                item {
                    IntegratedShortcutPanel(automationViewModel = viewModel, mainViewModel = mainViewModel, query = searchQuery)
                }
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
                            color = colors.surface1.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.outline.copy(alpha = 0.4f))
                        ) {
                            Text(
                                "No macros found for '$searchQuery'",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }
        }
    
                    }
                    1 -> {
                        val macroViewModel: MacroBuilderViewModel = viewModel()
                        MacroBuilderContent(macroViewModel, mainViewModel)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by rememberSaveable { mutableStateOf("") }
        var command by rememberSaveable { mutableStateOf("") }
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
            containerColor = colors.surface1,
            title = { Text("Define New Macro", color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Write complex steps with one command per line or use '&&'.", color = colors.textSecondary, fontSize = 12.sp)
                    Surface(
                        color = colors.surface2.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.outline.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Supported syntax", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("KEY(CMD+SPACE)", color = colors.textSecondary, fontSize = 11.sp)
                            Text("TEXT(hello world)", color = colors.textSecondary, fontSize = 11.sp)
                            Text("WAIT(500)", color = colors.textSecondary, fontSize = 11.sp)
                            Text("MEDIA(MUTE)", color = colors.textSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Example: Open Terminal and run whoami", color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text("KEY(CMD+SPACE)", color = colors.accentTeal, fontSize = 11.sp)
                            Text("WAIT(300)", color = colors.accentTeal, fontSize = 11.sp)
                            Text("TEXT(Terminal)", color = colors.accentTeal, fontSize = 11.sp)
                            Text("KEY(ENTER)", color = colors.accentTeal, fontSize = 11.sp)
                            Text("WAIT(700)", color = colors.accentTeal, fontSize = 11.sp)
                            Text("TEXT(whoami)", color = colors.accentTeal, fontSize = 11.sp)
                            Text("KEY(ENTER)", color = colors.accentTeal, fontSize = 11.sp)
                        }
                    }
                    TextButton(
                        onClick = {
                            if (name.isBlank()) name = "Open Terminal + whoami"
                            command = complexExample
                        }
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = colors.accentTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Insert Example", color = colors.accentTeal, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Automation Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accentTeal,
                            unfocusedBorderColor = colors.outline,
                            focusedTextColor = colors.textPrimary
                        )
                    )
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text("Command Sequence / Text") },
                        placeholder = { Text("KEY(CMD+SPACE)\nWAIT(300)\nTEXT(Terminal)\nKEY(ENTER)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accentTeal,
                            unfocusedBorderColor = colors.outline,
                            focusedTextColor = colors.textPrimary
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
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentTeal)
                ) { Text("Deploy", color = colors.canvas, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = colors.textSecondary) }
            }
        )
    }
}

@Composable
private fun EmergencyControlPanel(
    status: String,
    onAction: (EmergencyAction) -> Unit
) {
    val colors = hackieColors()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "EMERGENCY CONTROL",
            color = colors.textPrimary.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 12.sp
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = colors.surface1.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.outline.copy(alpha = 0.35f))
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
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, start = 2.dp)
                )
                Text(
                    text = "Note: Network/clipboard/app-closing use SSH when available for host-level control.",
                    color = colors.textSecondary.copy(alpha = 0.7f),
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
    val colors = hackieColors()
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = colors.surface2.copy(alpha = 0.18f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.accentTeal, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
    val colors = hackieColors()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "SHORTCUT GUIDE",
            color = colors.textPrimary.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 12.sp
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = colors.surface1.copy(alpha = 0.45f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.outline.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (filteredCategories.isEmpty()) {
                    Text("No shortcut guide entries match this search.", color = colors.textSecondary, fontSize = 12.sp)
                }

                filteredCategories.forEach { category ->
                    Text(
                        text = category.name,
                        color = colors.accentTeal,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        category.shortcuts.forEach { shortcut ->
                            Surface(
                                onClick = {
                                    if (shortcut.consumerCode != null) {
                                        mainViewModel.sendConsumerKey(shortcut.consumerCode)
                                    } else {
                                        mainViewModel.sendKeyCombination(shortcut.codes)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surface2.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.outline.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(shortcut.name, color = colors.textPrimary, fontSize = 12.sp)
                                    Text(shortcut.keys, color = colors.textSecondary, fontSize = 11.sp)
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
    val shortcuts: List<IntegratedShortcutItem>
)

private fun buildIntegratedShortcutCategories(): List<IntegratedShortcutCategory> = listOf(
    IntegratedShortcutCategory(
        "System",
        listOf(
            IntegratedShortcutItem("Lock Screen", "Ctrl + Cmd + Q", listOf(HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_Q)),
            IntegratedShortcutItem("Spotlight", "Cmd + Space", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_SPACE)),
            IntegratedShortcutItem("Force Quit", "Alt + Cmd + Esc", listOf(HidKeyCodes.MODIFIER_LEFT_ALT, HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_ESC))
        )
    ),
    IntegratedShortcutCategory(
        "Browser",
        listOf(
            IntegratedShortcutItem("New Tab", "Cmd + T", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_T)),
            IntegratedShortcutItem("Reload", "Cmd + R", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_R)),
            IntegratedShortcutItem("Address Bar", "Cmd + L", listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_L))
        )
    ),
    IntegratedShortcutCategory(
        "Media",
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
    val colors = hackieColors()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = colors.textPrimary.copy(alpha=0.4f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = colors.textPrimary.copy(alpha=0.6f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            if (onAddClick != null) {
                IconButton(onClick = onAddClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = colors.accentTeal, modifier = Modifier.size(18.dp))
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
    val colors = hackieColors()
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = colors.surface2.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.outline.copy(alpha = 0.2f)),
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
                    color = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = colors.textSecondary.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}


data class MacroDefinition(val name: String, val icon: ImageVector, val color: Color, val command: String)

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
        "SHOT_REGION_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SHIFT+4) && WAIT(200)")
        "MUTE_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SHIFT+M)")
        "SLEEP_CMD" -> viewModel.executeMacro2Script("KEY(CMD+ALT+POWER)")
        "SAY_HELLO_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(200) && TEXT(Terminal) && KEY(ENTER) && WAIT(500) && TEXT(say hello) && KEY(ENTER)")
        "TOGGLE_DARK_MODE_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(200) && TEXT(Terminal) && KEY(ENTER) && WAIT(500) && TEXT(osascript -e 'tell app \"System Events\" to tell appearance preferences to set dark mode to not dark mode') && KEY(ENTER)")
        "INFO_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(200) && TEXT(System Information) && KEY(ENTER)")
        "FORCE_QUIT_CMD" -> viewModel.executeMacro2Script("KEY(CMD+ALT+ESC)")
        "VOL_UP_CMD" -> viewModel.executeMacro2Script("MEDIA(VOL_UP)")
        "VOL_DOWN_CMD" -> viewModel.executeMacro2Script("MEDIA(VOL_DOWN)")
        "VOL_MUTE_CMD" -> viewModel.executeMacro2Script("MEDIA(VOL_MUTE)")
        "BRIGHT_UP_CMD" -> viewModel.executeMacro2Script("KEY(F15)")
        "BRIGHT_DOWN_CMD" -> viewModel.executeMacro2Script("KEY(F14)")
        "MEDIA_PLAY_CMD" -> viewModel.executeMacro2Script("MEDIA(PLAY)")
        "MEDIA_NEXT_CMD" -> viewModel.executeMacro2Script("MEDIA(NEXT)")
        "MEDIA_PREV_CMD" -> viewModel.executeMacro2Script("MEDIA(PREV)")
        "ACT_MONITOR_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(200) && TEXT(Activity Monitor) && KEY(ENTER)")

        "TAB_CMD" -> viewModel.executeMacro2Script("KEY(CMD+T)")
        "CLOSE_TAB_CMD" -> viewModel.executeMacro2Script("KEY(CMD+W)")
        "REOPEN_TAB_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SHIFT+T)")
        "RELOAD_CMD" -> viewModel.executeMacro2Script("KEY(CMD+R)")
        "HARD_RELOAD_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SHIFT+R)")
        "HIST_CMD" -> viewModel.executeMacro2Script("KEY(CMD+Y)") 
        "PRIV_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SHIFT+N)") 
        "BACK_CMD" -> viewModel.executeMacro2Script("KEY(CMD+[)")
        "FORWARD_CMD" -> viewModel.executeMacro2Script("KEY(CMD+])")
        "FS_CMD" -> viewModel.executeMacro2Script("KEY(CMD+CTRL+F)")
        "DEV_TOOLS_CMD" -> viewModel.executeMacro2Script("KEY(CMD+ALT+I)")
        "WEB_ZOOM_IN_CMD" -> viewModel.executeMacro2Script("KEY(CMD+=)")
        "WEB_ZOOM_OUT_CMD" -> viewModel.executeMacro2Script("KEY(CMD+-)")

        "MC_CMD" -> viewModel.executeMacro2Script("KEY(CTRL+UP)")
        "SW_CMD" -> viewModel.executeMacro2Script("KEY(CMD+TAB)")
        "HIDE_APP_CMD" -> viewModel.executeMacro2Script("KEY(CMD+H)")
        "HIDE_CMD" -> viewModel.executeMacro2Script("KEY(CMD+ALT+H)")
        "TERM_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(200) && TEXT(Terminal) && KEY(ENTER)")
        "LAUNCH_SAFARI" -> viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(200) && TEXT(Safari) && KEY(ENTER)")
        "LAUNCH_SPOTIFY" -> viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(200) && TEXT(Spotify) && KEY(ENTER)")
        "LAUNCH_VSCODE" -> viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(200) && TEXT(Visual Studio Code) && KEY(ENTER)")
        "LAUNCH_NOTES" -> viewModel.executeMacro2Script("KEY(CMD+SPACE) && WAIT(200) && TEXT(Notes) && KEY(ENTER)")
        
        "COPY_CMD" -> viewModel.executeMacro2Script("KEY(CMD+C)")
        "PASTE_CMD" -> viewModel.executeMacro2Script("KEY(CMD+V)")
        "CUT_CMD" -> viewModel.executeMacro2Script("KEY(CMD+X)")
        "SELECT_ALL_CMD" -> viewModel.executeMacro2Script("KEY(CMD+A)")
        "UNDO_CMD" -> viewModel.executeMacro2Script("KEY(CMD+Z)")
        "REDO_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SHIFT+Z)")
        "FIND_CMD" -> viewModel.executeMacro2Script("KEY(CMD+F)")

        "PLAY_CMD" -> viewModel.executeMacro2Script("MEDIA(PLAY)")
        "ZI_CMD" -> viewModel.executeMacro2Script("KEY(CMD+=)")
        "ZO_CMD" -> viewModel.executeMacro2Script("KEY(CMD+-)")
        "RENDER_CMD" -> viewModel.executeMacro2Script("KEY(CMD+M)")
        "EXPORT_CMD" -> viewModel.executeMacro2Script("KEY(CMD+E)")
        "NEW_PROJ_CMD" -> viewModel.executeMacro2Script("KEY(CMD+N)")
        "SAVE_CMD" -> viewModel.executeMacro2Script("KEY(CMD+S)")
        "SAVE_AS_CMD" -> viewModel.executeMacro2Script("KEY(CMD+SHIFT+S)")
        "COLOR_PICKER_CMD" -> viewModel.executeMacro2Script("KEY(CMD+CTRL+C)")
        "BRUSH_TOOL_CMD" -> viewModel.executeMacro2Script("KEY(B)")
        "SELECT_TOOL_CMD" -> viewModel.executeMacro2Script("KEY(V)")

        else -> viewModel.executeMacro2Script(command) 
    }
}
