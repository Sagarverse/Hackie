package com.example.rabit.ui.assistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.rabit.data.voice.VoiceState
import com.example.rabit.ui.components.PulsingVoiceButton
import com.example.rabit.ui.components.LocalOpenGlobalDrawer
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.ChatSurface
import com.example.rabit.ui.theme.*

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToKeyboard: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val selectedModelName by viewModel.selectedModelName.collectAsState()
    val connectionState by viewModel.deviceConnectionState.collectAsState()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val context = LocalContext.current
    val openGlobalDrawer = LocalOpenGlobalDrawer.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    var showPromptLibrary by remember { mutableStateOf(false) }
    var showHardwareMonitor by remember { mutableStateOf(false) }
    var showMacroGenie by remember { mutableStateOf(false) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Auto-Push logic — fires haptic when pushing
    LaunchedEffect(uiState) {
        if (uiState is AssistantUiState.Success && viewModel.autoPushEnabled.value) {
            val resp = (uiState as AssistantUiState.Success).response
            if (resp.text.isNotBlank()) {
                performHapticDoubleTap(context)
                mainViewModel.sendText(resp.text)
            }
        }
        // Haptic on AI response arrival
        if (uiState is AssistantUiState.Success) {
            performHapticConfirm(context)
        }
    }

    // Haptic tick when AI starts thinking
    LaunchedEffect(uiState) {
        if (uiState is AssistantUiState.Loading) {
            performHapticTick(context)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = ChatSurface,
                drawerContentColor = Platinum,
                modifier = Modifier.width(320.dp)
            ) {
                AssistantDrawerContent(
                    viewModel = viewModel,
                    onPromptLibraryClick = { showPromptLibrary = true; coroutineScope.launch { drawerState.close() } },
                    onHardwareMonitorClick = { showHardwareMonitor = true; coroutineScope.launch { drawerState.close() } },
                    onMacroGenieClick = { showMacroGenie = true; coroutineScope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = ChatSurface,
            topBar = {
                PremiumChatTopBar(
                    modelName = selectedModelName,
                    isThinking = uiState is AssistantUiState.Loading,
                    connectionState = connectionState,
                    onLeftPanelClick = { coroutineScope.launch { drawerState.open() } },
                    onRightPanelClick = { showHardwareMonitor = true },
                    onClearChat = { viewModel.clearConversation() },
                    onNewChat = { viewModel.clearConversation() },
                    onExportChat = { viewModel.exportChatHistory(context) },
                    onSettingsClick = onNavigateToSettings
                )
            }
        ) { padding ->
        val modelLoadState by viewModel.modelLoadState.collectAsState()
        val modelCopyProgress by viewModel.modelCopyProgress.collectAsState()
        val modelLastError by viewModel.modelLastError.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LocalModelStatusBar(
                state = modelLoadState,
                progress = modelCopyProgress,
                error = modelLastError,
                onDismiss = { viewModel.clearModelError() }
            )

            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty()) {
                    PremiumWelcomeScreen(viewModel)
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            ChatBubble(message, viewModel, mainViewModel)
                        }
                    }
                }
            }
            PremiumInputArea(viewModel, mainViewModel)
        }
    }
    }

    if (showPromptLibrary) {
        PromptLibraryModal(
            onDismiss = { showPromptLibrary = false },
            onSelectPrompt = { prompt ->
                viewModel.onInputChanged(prompt)
                showPromptLibrary = false
            }
        )
    }
    if (showHardwareMonitor) {
        HardwareMonitorModal(onDismiss = { showHardwareMonitor = false })
    }

    if (showMacroGenie) {
        MacroGenieModal(
            viewModel = mainViewModel,
            onDismiss = { showMacroGenie = false }
        )
    }
}

@Composable
private fun AssistantLeftPanelContent(
    featureWebBridgeVisible: Boolean,
    featureAutomationVisible: Boolean,
    featureAssistantVisible: Boolean,
    featureSnippetsVisible: Boolean,
    featureWakeOnLanVisible: Boolean,
    featureSshTerminalVisible: Boolean,
    onNavigate: (String) -> Unit
) {
    val scrollState = androidx.compose.foundation.rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = AccentBlue.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "HACKIE",
                    color = AccentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text("Navigation", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        AssistantNavDrawerItem(
            label = "Control Hub",
            subLabel = "Keyboard & Trackpad",
            icon = Icons.AutoMirrored.Filled.Dvr,
            isSelected = false,
            onClick = { onNavigate("keyboard") }
        )

        if (featureWebBridgeVisible) {
            AssistantNavDrawerItem(
                label = "Web Bridge Hub",
                subLabel = "File Sharing & Sync",
                icon = Icons.Default.CloudSync,
                isSelected = false,
                onClick = { onNavigate("web_bridge") }
            )
        }

        if (featureAutomationVisible) {
            AssistantNavDrawerItem(
                label = "Automation Hub",
                subLabel = "Macros & Quick Actions",
                icon = Icons.Default.Bolt,
                isSelected = false,
                onClick = { onNavigate("automation") }
            )

            AssistantNavDrawerItem(
                label = "Payload Injector",
                subLabel = "DuckyScript command injection",
                icon = Icons.Default.ElectricBolt,
                isSelected = false,
                onClick = { onNavigate("injector") }
            )
        }

        AssistantNavDrawerItem(
            label = "AirPlay Receiver",
            subLabel = "Wi-Fi audio target (experimental)",
            icon = Icons.Default.Speaker,
            isSelected = false,
            onClick = { onNavigate("airplay_receiver") }
        )

        if (featureWakeOnLanVisible) {
            AssistantNavDrawerItem(
                label = "Wake-on-LAN",
                subLabel = "Boot Sleeping Mac/PC",
                icon = Icons.Default.PowerSettingsNew,
                isSelected = false,
                onClick = { onNavigate("wake_on_lan") }
            )
        }

        if (featureSshTerminalVisible) {
            AssistantNavDrawerItem(
                label = "SSH Terminal",
                subLabel = "Native secure shell",
                icon = Icons.Default.Terminal,
                isSelected = false,
                onClick = { onNavigate("ssh_terminal") }
            )
        }

        if (featureAssistantVisible) {
            AssistantNavDrawerItem(
                label = "AI Assistant",
                subLabel = "Smart Control & Logic",
                icon = Icons.Default.AutoAwesome,
                isSelected = true,
                onClick = { onNavigate("assistant") }
            )
        }

        if (featureSnippetsVisible) {
            AssistantNavDrawerItem(
                label = "Snippets",
                subLabel = "Saved reusable text blocks",
                icon = Icons.Default.ContentPaste,
                isSelected = false,
                onClick = { onNavigate("snippets") }
            )
        }

        AssistantNavDrawerItem(
            label = "Password Manager",
            subLabel = "Biometric + password push settings",
            icon = Icons.Default.Password,
            isSelected = false,
            onClick = { onNavigate("password_manager") }
        )

        AssistantNavDrawerItem(
            label = "System Settings",
            subLabel = "Configuration & Sensitivity",
            icon = Icons.Default.Settings,
            isSelected = false,
            onClick = { onNavigate("settings") }
        )
    }
}

@Composable
private fun AssistantNavDrawerItem(
    label: String,
    subLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AccentBlue.copy(alpha = 0.18f) else Color.Transparent,
        border = BorderStroke(
            0.5.dp,
            if (isSelected) AccentBlue.copy(alpha = 0.35f) else BorderColor.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) AccentBlue else Silver,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, color = Platinum, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subLabel, color = Silver.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// ── Utility
// ════════════════════════════════════════════════════════════════════

fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroGenieModal(
    viewModel: com.example.rabit.ui.MainViewModel,
    onDismiss: () -> Unit
) {
    var intent by remember { mutableStateOf("") }
    val genieState by viewModel.genieState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ChatSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Silver.copy(alpha = 0.3f))
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "SMART MACRO GENIE",
                        color = AccentBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Generate and execute HID macros from natural language.",
                        color = Silver.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close macro genie", tint = Silver)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Graphite.copy(alpha = 0.45f),
                border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Macro Intent",
                        color = Silver.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = intent,
                        onValueChange = { intent = it },
                        placeholder = { Text("e.g. 'Mute Zoom' or 'New Window'", color = Silver.copy(alpha = 0.5f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        trailingIcon = {
                            val voiceState by viewModel.voiceState.collectAsState()
                            val voiceResult by viewModel.voiceResult.collectAsState()

                            if (voiceResult.isNotBlank() && voiceState == VoiceState.SUCCESS) {
                                intent = voiceResult
                                viewModel.resetVoiceState()
                            }

                            PulsingVoiceButton(
                                state = voiceState,
                                onClick = {
                                    if (voiceState == VoiceState.LISTENING) viewModel.stopVoiceRecognition()
                                    else viewModel.startVoiceRecognition()
                                }
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Graphite.copy(alpha = 0.5f),
                            unfocusedContainerColor = Graphite.copy(alpha = 0.3f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = genieState) {
                is com.example.rabit.ui.MainViewModel.GenieState.Thinking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentBlue, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("AI is brewing your macro...", color = Silver, fontSize = 14.sp)
                    }
                }
                is com.example.rabit.ui.MainViewModel.GenieState.Executing -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(state.currentStep, color = Platinum, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = AccentBlue,
                            trackColor = Graphite
                        )
                        
                        TextButton(
                            onClick = { viewModel.cancelMacro() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("ABORT SEQUENCE", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is com.example.rabit.ui.MainViewModel.GenieState.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Macro Complete: ${state.macroName}", color = AccentBlue, fontSize = 14.sp)
                    }
                }
                is com.example.rabit.ui.MainViewModel.GenieState.Error -> {
                    Column {
                        Text(state.message, color = StopRed, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        com.example.rabit.ui.components.VibrantGradientButton(
                            text = "Try Again",
                            onClick = { viewModel.generateSmartMacro(intent) },
                            gradient = Brush.linearGradient(listOf(Color(0xFF2F3136), Color(0xFF17191D)))
                        )
                    }
                }
                else -> {
                    com.example.rabit.ui.components.VibrantGradientButton(
                        text = "Summon Genie",
                        onClick = { viewModel.generateSmartMacro(intent) },
                        gradient = Brush.linearGradient(listOf(Color(0xFF0A84FF), Color(0xFF0D6EDB)))
                    )
                }
            }
        }
    }
}


