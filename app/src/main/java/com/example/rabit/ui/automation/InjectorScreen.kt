package com.example.rabit.ui.automation

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class InjectorSavedPayload(
    val name: String,
    val script: String,
    val updatedAtMs: Long
)

data class InjectorScriptAnalysis(
    val commandLines: Int,
    val warningLines: Int,
    val estimatedDurationMs: Long,
    val warnings: List<String>
)

data class InjectorRunEntry(
    val ts: Long,
    val title: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InjectorScreen(
    viewModel: MainViewModel,
    automationViewModel: AutomationViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val prefs = remember { context.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE) }
    val connectionState by viewModel.connectionState.collectAsState()
    val isConnected = connectionState is HidDeviceManager.ConnectionState.Connected

    val defaultPayload = """
REM Hackie — System Audit (macOS)
DELAY 500
GUI SPACE
DELAY 400
STRING Terminal
ENTER
DELAY 1500
KEY (CTRL+CMD+F)
DELAY 300
STRING clear
ENTER
DELAY 200
STRING echo "[ HACKIE ] System access established — $(whoami)@$(hostname)"
ENTER
STRING say "You are under the control of Hackie"
ENTER
DELAY 800
STRING cmatrix -b -C green
ENTER
""".trimIndent()
    var payload by remember { mutableStateOf(defaultPayload) }
    var isInjecting by remember { mutableStateOf(false) }
    var showToolsSheet by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var savedPayloads by remember { mutableStateOf(loadInjectorSavedPayloads(prefs)) }
    var runHistory by remember { mutableStateOf(loadInjectorRunHistory(prefs)) }
    var analysis by remember(payload) { mutableStateOf(analyzeInjectorScript(payload)) }
    val typingSpeed by viewModel.typingSpeed.collectAsState()

    // AI Agent State
    var aiPrompt by remember { mutableStateOf("") }
    val aiState by automationViewModel.aiGenerationState.collectAsState()
    
    val isInjectorRunning by automationViewModel.isInjectorRunning.collectAsState()
    val isInjectorPaused by automationViewModel.isInjectorPaused.collectAsState()
    
    // Auto-load AI response when success
    LaunchedEffect(aiState) {
        if (aiState is AutomationViewModel.AiGenerationState.Success) {
            val generated = (aiState as AutomationViewModel.AiGenerationState.Success).payload
            payload = generated
            analysis = analyzeInjectorScript(generated)
            automationViewModel.resetAiGeneration()
        }
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val templateLibrary = remember {
        listOf(
            // ── Quick Actions ──
            "🔐 Lock Screen" to "KEY (CTRL+CMD+Q)\n",
            "🔊 Mute / Unmute" to "F10\n",
            "📸 Screenshot" to "KEY (CMD+SHIFT+3)\n",
            "📋 Screenshot → Clipboard" to "KEY (CMD+CTRL+SHIFT+3)\n",
            "🔍 Spotlight Search" to "DELAY 300\nGUI SPACE\n",
            "🌐 Open Browser" to "DELAY 300\nGUI SPACE\nDELAY 400\nSTRING safari\nENTER\n",
            // ── Terminal ──
            "💻 Open Terminal" to "DELAY 300\nGUI SPACE\nDELAY 400\nSTRING Terminal\nENTER\nDELAY 1500\n",
            "💻 Terminal + whoami" to "DELAY 300\nGUI SPACE\nDELAY 400\nSTRING Terminal\nENTER\nDELAY 1500\nSTRING whoami\nENTER\n",
            "💻 Terminal + ifconfig" to "DELAY 300\nGUI SPACE\nDELAY 400\nSTRING Terminal\nENTER\nDELAY 1500\nSTRING ifconfig | grep 'inet '\nENTER\n",
            "💻 Terminal + netstat" to "DELAY 300\nGUI SPACE\nDELAY 400\nSTRING Terminal\nENTER\nDELAY 1500\nSTRING netstat -an | grep LISTEN\nENTER\n",
            // ── macOS Shortcuts ──
            "⌘ Select All + Copy" to "KEY (CMD+A)\nDELAY 100\nKEY (CMD+C)\n",
            "⌘ Undo" to "KEY (CMD+Z)\n",
            "⌘ Force Quit Menu" to "KEY (CMD+ALT+ESC)\n",
            "⌘ Close Window" to "KEY (CMD+W)\n",
            "⌘ Quit App" to "KEY (CMD+Q)\n",
            "⌘ Switch App" to "KEY (CMD+TAB)\n",
            "⌘ Mission Control" to "F3\n",
            "⌘ Show Desktop" to "KEY (CMD+F3)\n",
            // ── Hackie Ops ──
            "🎭 Hackie Takeover" to "DELAY 500\nGUI SPACE\nDELAY 400\nSTRING Terminal\nENTER\nDELAY 1500\nKEY (CTRL+CMD+F)\nDELAY 300\nSTRING clear\nENTER\nDELAY 200\nSTRING echo \"[ HACKIE ] System access established — \$(whoami)@\$(hostname)\"\nENTER\nSTRING say \"You are under the control of Hackie\"\nENTER\nDELAY 800\nSTRING cmatrix -b -C green\nENTER\n",
            "🕵️ System Recon" to "DELAY 300\nGUI SPACE\nDELAY 400\nSTRING Terminal\nENTER\nDELAY 1500\nSTRING echo \"=== RECON ===\" && whoami && hostname && sw_vers -productVersion && ipconfig getifaddr en0\nENTER\n",
            "🔐 Dump Keychain List" to "DELAY 300\nGUI SPACE\nDELAY 400\nSTRING Terminal\nENTER\nDELAY 1500\nSTRING security list-keychains\nENTER\n",
            "🌐 DNS Leak Test" to "DELAY 300\nGUI SPACE\nDELAY 400\nSTRING Terminal\nENTER\nDELAY 1500\nSTRING nslookup whoami.akamai.net\nENTER\n"
        )
    }

    Scaffold(
        containerColor = Obsidian,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "PAYLOAD INJECTOR",
                            color = AccentPurple,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "DuckyScript HID Engine",
                            color = Silver.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Platinum)
                    }
                },
                actions = {
                    if (isInjectorRunning) {
                        if (isInjectorPaused) {
                            IconButton(onClick = { automationViewModel.resumeInjector() }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = SuccessGreen)
                            }
                        } else {
                            IconButton(onClick = { automationViewModel.pauseInjector() }) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = WarningYellow)
                            }
                        }
                        IconButton(onClick = { automationViewModel.abortInjector() }) {
                            Icon(Icons.Default.Stop, contentDescription = "Abort", tint = ErrorRed)
                        }
                    }
                    // Connection pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isConnected) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, if (isConnected) SuccessGreen.copy(alpha = 0.5f) else ErrorRed.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isConnected) SuccessGreen else ErrorRed)
                            )
                            Text(
                                if (isConnected) "LINKED" else "NO LINK",
                                color = if (isConnected) SuccessGreen else ErrorRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Graphite.copy(alpha = 0.7f),
                    scrolledContainerColor = Graphite.copy(alpha = 0.7f)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ══ AI AGENT ══
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (aiState is AutomationViewModel.AiGenerationState.Generating)
                        AccentBlue.copy(alpha = 0.07f) else Graphite.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (aiState is AutomationViewModel.AiGenerationState.Generating)
                            AccentBlue.copy(alpha = 0.6f) else BorderColor.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                            Text("AI Payload Generator", color = Platinum, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            if (aiState is AutomationViewModel.AiGenerationState.Generating) {
                                Spacer(Modifier.weight(1f))
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = AccentBlue, strokeWidth = 1.5.dp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = aiPrompt,
                                onValueChange = { aiPrompt = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Describe what to do on the target...", color = Silver.copy(alpha = 0.4f), fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedBorderColor = BorderColor.copy(alpha = 0.4f),
                                    focusedBorderColor = AccentBlue,
                                    unfocusedTextColor = Platinum,
                                    focusedTextColor = Platinum
                                ),
                                shape = RoundedCornerShape(10.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                maxLines = 2,
                                singleLine = true
                            )
                            Button(
                                onClick = { automationViewModel.generateAiDuckyPayload(aiPrompt) },
                                enabled = aiPrompt.isNotBlank() && aiState !is AutomationViewModel.AiGenerationState.Generating,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 14.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (aiState is AutomationViewModel.AiGenerationState.Error) {
                            Text(
                                "⚠ ${(aiState as AutomationViewModel.AiGenerationState.Error).message}",
                                color = ErrorRed, fontSize = 11.sp
                            )
                        }
                    }
                }

                // ══ SCRIPT EDITOR ══
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0D0D14),
                    border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.25f))
                ) {
                    Column {
                        // Editor header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AccentPurple.copy(alpha = 0.08f))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(ErrorRed.copy(alpha = 0.7f)))
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(WarningYellow.copy(alpha = 0.7f)))
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SuccessGreen.copy(alpha = 0.7f)))
                                Spacer(Modifier.width(4.dp))
                                Text("payload.ducky", color = Silver.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Validate button
                                IconButton(
                                    onClick = {
                                        analysis = analyzeInjectorScript(payload)
                                        scope.launch {
                                            if (analysis.warningLines == 0) snackbarHostState.showSnackbar("✓ Script valid")
                                            else snackbarHostState.showSnackbar("⚠ ${analysis.warningLines} warning(s)")
                                        }
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = "Validate",
                                        tint = if (analysis.warningLines == 0) SuccessGreen.copy(alpha = 0.8f) else WarningYellow,
                                        modifier = Modifier.size(16.dp))
                                }
                                // Reset button
                                IconButton(
                                    onClick = { payload = defaultPayload; analysis = analyzeInjectorScript(defaultPayload) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset",
                                        tint = Silver.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                }
                                // Tools button
                                IconButton(
                                    onClick = { showToolsSheet = true },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = "Tools",
                                        tint = AccentGold.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        // Text field
                        BasicTextField(
                            value = payload,
                            onValueChange = { payload = it; analysis = analyzeInjectorScript(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 240.dp, max = 400.dp)
                                .padding(14.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Platinum,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentPurple),
                            decorationBox = { inner ->
                                if (payload.isEmpty()) {
                                    Text("REM Write your DuckyScript payload here...\nDELAY 500\nSTRING hello world\nENTER",
                                        color = Silver.copy(alpha = 0.25f),
                                        fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp)
                                }
                                inner()
                            }
                        )
                    }
                }

                // ══ SPEED + CONTROLS ROW ══
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SPEED", color = Silver.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf("Too Slow" to "MIN", "Slow" to "SLOW", "Normal" to "NORM", "Fast" to "FAST", "Super Fast" to "MAX").forEach { (speed, label) ->
                            val isActive = typingSpeed == speed
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isActive) AccentPurple.copy(alpha = 0.25f) else Color.Transparent,
                                border = BorderStroke(0.5.dp, if (isActive) AccentPurple.copy(alpha = 0.7f) else BorderColor.copy(alpha = 0.3f)),
                                onClick = { viewModel.setTypingSpeed(speed) }
                            ) {
                                Text(
                                    label,
                                    color = if (isActive) AccentPurple else Silver.copy(alpha = 0.5f),
                                    fontSize = 9.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // ══ SAVED PAYLOADS INLINE ══
                if (savedPayloads.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            "SAVED PAYLOADS",
                            color = Silver.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            savedPayloads.take(4).forEach { item ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = AccentPurple.copy(alpha = 0.07f),
                                    border = BorderStroke(0.5.dp, AccentPurple.copy(alpha = 0.2f)),
                                    onClick = {
                                        payload = item.script
                                        analysis = analyzeInjectorScript(item.script)
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                        Text(item.name, color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text("${item.script.lines().size}L", color = AccentPurple.copy(alpha = 0.7f), fontSize = 9.sp)
                                    }
                                }
                            }
                            // Fill empty spots
                            repeat(maxOf(0, 4 - savedPayloads.take(4).size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // ══ INJECT BUTTON ══
                Button(
                    onClick = {
                        if (!isConnected) {
                            val updated = listOf(
                                InjectorRunEntry(System.currentTimeMillis(), "Inject aborted", "No host connected")
                            ) + runHistory
                            runHistory = updated.take(12)
                            saveInjectorRunHistory(prefs, runHistory)
                            scope.launch { snackbarHostState.showSnackbar("Connect to a host before injecting") }
                            return@Button
                        }
                        if (payload.isBlank()) return@Button
                        automationViewModel.executeDuckyScript(payload)
                        val updated = listOf(
                            InjectorRunEntry(
                                ts = System.currentTimeMillis(),
                                title = payload.lineSequence().firstOrNull()?.take(36)?.ifBlank { "Payload" } ?: "Payload",
                                status = "Dispatched"
                            )
                        ) + runHistory
                        runHistory = updated.take(12)
                        saveInjectorRunHistory(prefs, runHistory)
                        scope.launch {
                            snackbarHostState.showSnackbar("⚡ Payload dispatched")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = payload.isNotBlank() && !isInjectorRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) AccentPurple else Graphite,
                        disabledContainerColor = Graphite
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isConnected) 8.dp else 0.dp)
                ) {
                    if (isInjectorRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Platinum, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (isInjectorPaused) "PAUSED" else "INJECTING...", fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 14.sp)
                    } else {
                        Icon(Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            if (isConnected) "INJECT PAYLOAD" else "NO TARGET LINKED",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 13.sp,
                            color = if (isConnected) Platinum else Silver.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    if (showToolsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showToolsSheet = false },
            containerColor = Obsidian,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Silver.copy(alpha = 0.45f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Injector Tools", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                // ── Template Library ──
                val categories = listOf(
                    "Quick Actions" to templateLibrary.filter { it.first.startsWith("🔐") || it.first.startsWith("🔊") || it.first.startsWith("📸") || it.first.startsWith("📋") || it.first.startsWith("🔍") || it.first.startsWith("🌐") && !it.first.contains("DNS") },
                    "Terminal" to templateLibrary.filter { it.first.startsWith("💻") },
                    "macOS Shortcuts" to templateLibrary.filter { it.first.startsWith("⌘") },
                    "Hackie Ops" to templateLibrary.filter { it.first.startsWith("🎭") || it.first.startsWith("🕵") || it.first.startsWith("🔐 Dump") || it.first.startsWith("🌐 DNS") }
                )

                categories.forEach { (catName, items) ->
                    if (items.isNotEmpty()) {
                        Text(catName, color = AccentPurple.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        items.forEach { (name, script) ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Graphite.copy(alpha = 0.4f),
                                border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f)),
                                onClick = {
                                    payload = script
                                    analysis = analyzeInjectorScript(script)
                                    showToolsSheet = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, color = Platinum, fontSize = 13.sp)
                                    Icon(Icons.Default.NorthWest, contentDescription = null, tint = AccentPurple.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))

                // ── Command Palette ──
                Text("Command Palette", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                val commandPalette = listOf(
                    "DELAY 500", "DEFAULTDELAY 100",
                    "STRING text here", "ENTER",
                    "GUI SPACE", "KEY (CMD+A)",
                    "KEY (CMD+C)", "KEY (CMD+V)",
                    "KEY (CTRL+CMD+F)", "KEY (CTRL+CMD+Q)",
                    "F5", "F11"
                )
                commandPalette.chunked(3).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { cmd ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = AccentBlue.copy(alpha = 0.1f),
                                border = BorderStroke(0.5.dp, AccentBlue.copy(alpha = 0.2f)),
                                onClick = {
                                    payload = if (payload.endsWith("\n") || payload.isBlank()) "$payload$cmd\n" else "$payload\n$cmd\n"
                                    analysis = analyzeInjectorScript(payload)
                                }
                            ) {
                                Text(cmd, color = Platinum, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp), maxLines = 1)
                            }
                        }
                        repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))

                // ── Key Reference ──
                Text("Key Reference", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = AccentBlue.copy(alpha = 0.05f),
                    border = BorderStroke(0.5.dp, AccentBlue.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "F1 … F12" to "Function keys (standalone line)",
                            "KEY (F11)" to "Function key via KEY command",
                            "KEY (CMD+A)" to "Select All",
                            "KEY (CMD+C)" to "Copy",
                            "KEY (CMD+V)" to "Paste",
                            "KEY (CTRL+CMD+Q)" to "Lock Screen",
                            "KEY (CTRL+CMD+F)" to "Toggle Full Screen",
                            "KEY (CMD+SHIFT+3)" to "Screenshot",
                            "KEY (CMD+ALT+ESC)" to "Force Quit Menu",
                            "GUI A" to "Classic DuckyScript style"
                        ).forEach { (cmd, desc) ->
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(cmd, color = AccentBlue, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                                Text(desc, color = Silver.copy(alpha = 0.75f), fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Modifiers: GUI/CMD  CTRL  ALT/OPTION  SHIFT\nKeys: A-Z  0-9  F1-F12  SPACE  ENTER  ESC  TAB  BACKSPACE  LEFT  RIGHT  UP  DOWN",
                            color = Silver.copy(alpha = 0.5f), fontSize = 9.sp, lineHeight = 13.sp
                        )
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))

                // ── Save Payload ──
                Text("Save Payload", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Name...", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = {
                            val normalized = saveName.trim()
                            if (normalized.isBlank() || payload.isBlank()) return@Button
                            val updated = savedPayloads
                                .filterNot { it.name.equals(normalized, ignoreCase = true) }
                                .toMutableList()
                                .apply { add(0, InjectorSavedPayload(normalized, payload, System.currentTimeMillis())) }
                            savedPayloads = updated
                            saveInjectorSavedPayloads(prefs, updated)
                            saveName = ""
                            scope.launch { snackbarHostState.showSnackbar("Saved: $normalized") }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Obsidian, modifier = Modifier.size(18.dp))
                    }
                }

                // ── Saved Payloads ──
                if (savedPayloads.isNotEmpty()) {
                    Text("Saved Payloads (${savedPayloads.size})", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    savedPayloads.forEach { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Graphite.copy(alpha = 0.4f),
                            border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${item.script.lines().size} lines", color = Silver.copy(alpha = 0.7f), fontSize = 10.sp)
                                }
                                TextButton(onClick = {
                                    payload = item.script
                                    analysis = analyzeInjectorScript(item.script)
                                    showToolsSheet = false
                                }) { Text("Load", color = AccentPurple, fontSize = 12.sp) }
                                IconButton(onClick = {
                                    val updated = savedPayloads.filterNot { it.name == item.name }
                                    savedPayloads = updated
                                    saveInjectorSavedPayloads(prefs, updated)
                                }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))

                // ── Clipboard & Backup ──
                Text("Clipboard & Backup", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(payload)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Script", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val clip = clipboard.getText()?.text.orEmpty()
                            if (clip.isNotBlank()) { payload = clip; analysis = analyzeInjectorScript(clip) }
                        },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste Script", fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val exportJson = JSONArray().apply {
                                savedPayloads.forEach { item ->
                                    put(JSONObject().apply { put("name", item.name); put("script", item.script); put("updatedAtMs", item.updatedAtMs) })
                                }
                            }
                            clipboard.setText(AnnotatedString(exportJson.toString()))
                            scope.launch { snackbarHostState.showSnackbar("Payloads exported to clipboard") }
                        },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export All", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val raw = clipboard.getText()?.text.orEmpty()
                            val imported = parseImportedInjectorPayloads(raw)
                            if (imported.isNotEmpty()) {
                                val merged = (imported + savedPayloads).distinctBy { it.name.lowercase() }.sortedByDescending { it.updatedAtMs }
                                savedPayloads = merged
                                saveInjectorSavedPayloads(prefs, merged)
                                scope.launch { snackbarHostState.showSnackbar("Imported ${imported.size} payload(s)") }
                            }
                        },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontSize = 12.sp)
                    }
                }

                // ── Validation Warnings ──
                if (analysis.warningLines > 0) {
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))
                    Text("Script Warnings", color = WarningYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    analysis.warnings.take(6).forEach {
                        Text("⚠ $it", color = Silver.copy(alpha = 0.85f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}



private fun analyzeInjectorScript(script: String): InjectorScriptAnalysis {
    val knownCommands = setOf(
        "DEFAULTDELAY", "DELAY", "STRING", "ENTER", "TAB", "SPACE",
        "UP", "UPARROW", "DOWN", "DOWNARROW", "GUI", "WINDOWS", "COMMAND",
        "CTRL", "CONTROL", "ALT", "SHIFT", "MAC_STEALTH"
    )

    var defaultDelay = 10L
    var estimatedMs = 0L
    var commandLines = 0
    val warnings = mutableListOf<String>()

    script.lines().forEachIndexed { index, raw ->
        val line = raw.trim()
        if (line.isBlank() || line.uppercase().startsWith("REM ")) return@forEachIndexed

        commandLines += 1
        val parts = line.split(" ", limit = 2)
        val cmd = parts[0].uppercase()
        val arg = if (parts.size > 1) parts[1] else ""

        if (!knownCommands.contains(cmd)) {
            warnings += "Line ${index + 1}: unknown command '$cmd' (typed as raw text)"
        }

        when (cmd) {
            "DEFAULTDELAY" -> defaultDelay = arg.toLongOrNull() ?: defaultDelay
            "DELAY" -> estimatedMs += (arg.toLongOrNull() ?: defaultDelay).coerceAtLeast(0L)
            "MAC_STEALTH" -> estimatedMs += 1200L
            else -> estimatedMs += defaultDelay
        }
    }

    return InjectorScriptAnalysis(
        commandLines = commandLines,
        warningLines = warnings.size,
        estimatedDurationMs = estimatedMs,
        warnings = warnings
    )
}

private fun loadInjectorSavedPayloads(prefs: android.content.SharedPreferences): List<InjectorSavedPayload> {
    val json = prefs.getString("injector_saved_payloads", null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { idx ->
            val obj = arr.getJSONObject(idx)
            InjectorSavedPayload(
                name = obj.optString("name", "Payload ${idx + 1}"),
                script = obj.optString("script", ""),
                updatedAtMs = obj.optLong("updatedAtMs", 0L)
            )
        }.sortedByDescending { it.updatedAtMs }
    } catch (e: Exception) {
        Log.w("InjectorScreen", "Failed to parse saved payloads JSON", e)
        emptyList()
    }
}

private fun saveInjectorSavedPayloads(
    prefs: android.content.SharedPreferences,
    payloads: List<InjectorSavedPayload>
) {
    val arr = JSONArray()
    payloads.forEach { payload ->
        arr.put(
            JSONObject().apply {
                put("name", payload.name)
                put("script", payload.script)
                put("updatedAtMs", payload.updatedAtMs)
            }
        )
    }
    prefs.edit().putString("injector_saved_payloads", arr.toString()).apply()
}

private fun loadInjectorRunHistory(prefs: android.content.SharedPreferences): List<InjectorRunEntry> {
    val json = prefs.getString("injector_run_history", null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { idx ->
            val obj = arr.getJSONObject(idx)
            InjectorRunEntry(
                ts = obj.optLong("ts", 0L),
                title = obj.optString("title", "Payload"),
                status = obj.optString("status", "Unknown")
            )
        }.sortedByDescending { it.ts }
    } catch (e: Exception) {
        Log.w("InjectorScreen", "Failed to parse run history JSON", e)
        emptyList()
    }
}

private fun saveInjectorRunHistory(
    prefs: android.content.SharedPreferences,
    entries: List<InjectorRunEntry>
) {
    val arr = JSONArray()
    entries.forEach { entry ->
        arr.put(
            JSONObject().apply {
                put("ts", entry.ts)
                put("title", entry.title)
                put("status", entry.status)
            }
        )
    }
    prefs.edit().putString("injector_run_history", arr.toString()).apply()
}

private fun parseImportedInjectorPayloads(raw: String): List<InjectorSavedPayload> {
    if (raw.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { idx ->
            val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
            val name = obj.optString("name").trim()
            val script = obj.optString("script").trim()
            if (name.isBlank() || script.isBlank()) return@mapNotNull null
            InjectorSavedPayload(
                name = name,
                script = script,
                updatedAtMs = obj.optLong("updatedAtMs", System.currentTimeMillis())
            )
        }
    } catch (e: Exception) {
        Log.w("InjectorScreen", "Failed to parse imported payload JSON", e)
        emptyList()
    }
}

private fun payloadFromHistoryTitle(
    title: String,
    saved: List<InjectorSavedPayload>,
    currentPayload: String
): String {
    return saved.firstOrNull { it.name.equals(title, ignoreCase = true) }?.script ?: currentPayload
}
