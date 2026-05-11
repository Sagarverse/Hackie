package com.example.rabit.ui.automation

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeTyperScreen(
    viewModel: MainViewModel,
    automationViewModel: AutomationViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val isConnected by viewModel.isHidConnected.collectAsState()

    var codeInput by remember { mutableStateOf("") }
    var selectedProfile by remember { mutableStateOf("Balanced") }

    val isCodeTyping by automationViewModel.isCodeTyperRunning.collectAsState()
    val isCodeTyperPaused by automationViewModel.isCodeTyperPaused.collectAsState()
    val codeTyperProgress by automationViewModel.codeTyperProgress.collectAsState()
    val codeTyperCharIndex by automationViewModel.codeTyperCharIndex.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Typing profiles: tuned for different editor contexts
    val profiles = remember {
        listOf(
            "Careful" to "60-180ms per key, long pauses at newlines. Best for watched demos.",
            "Balanced" to "40-140ms per key, moderate pauses. Default for IDE coding.",
            "Fluent" to "25-90ms per key, short pauses. Mimics experienced developer.",
            "Interview" to "50-160ms per key, thinking pauses at blocks. Mimics live coding.",
            "Stealth" to "30-120ms per key, micro-variations. Maximum anti-detection."
        )
    }

    // Stats
    val totalChars = codeInput.length
    val totalLines = if (codeInput.isBlank()) 0 else codeInput.lines().size

    Scaffold(
        containerColor = Obsidian,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "CODE TYPER",
                            color = AccentTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Human-Grade HID Code Injection",
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
                    if (isCodeTyping) {
                        if (isCodeTyperPaused) {
                            IconButton(onClick = { automationViewModel.resumeCodeTyper() }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = SuccessGreen)
                            }
                        } else {
                            IconButton(onClick = { automationViewModel.pauseCodeTyper() }) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = WarningYellow)
                            }
                        }
                        IconButton(onClick = { automationViewModel.abortCodeTyper() }) {
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

                // ══ INFO CARD ══
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AccentTeal.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.5.dp, AccentTeal.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                        Text(
                            "Paste your code below. Every character — spaces, tabs, newlines, brackets — will be typed exactly as-is via HID, with human-realistic timing that defeats keystroke analysis.",
                            color = Silver,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // ══ CODE EDITOR ══
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0D0D14),
                    border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.25f))
                ) {
                    Column {
                        // Editor header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AccentTeal.copy(alpha = 0.08f))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(ErrorRed.copy(alpha = 0.7f)))
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(WarningYellow.copy(alpha = 0.7f)))
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SuccessGreen.copy(alpha = 0.7f)))
                                Spacer(Modifier.width(4.dp))
                                Text("code_input.src", color = Silver.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Paste from clipboard
                                IconButton(
                                    onClick = {
                                        val clip = clipboard.getText()?.text.orEmpty()
                                        if (clip.isNotBlank()) {
                                            codeInput = clip
                                        }
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste",
                                        tint = AccentTeal.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                }
                                // Clear
                                IconButton(
                                    onClick = { codeInput = "" },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Clear",
                                        tint = Silver.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        // Text field
                        BasicTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp, max = 400.dp)
                                .padding(14.dp),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Platinum,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(AccentTeal),
                            decorationBox = { inner ->
                                if (codeInput.isEmpty()) {
                                    Text(
                                        "// Paste your code here...\n// Every character will be typed exactly as-is\n// Spaces, tabs, newlines — all preserved\n\nfun main() {\n    println(\"Hello, World!\")\n}",
                                        color = Silver.copy(alpha = 0.2f),
                                        fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp
                                    )
                                }
                                inner()
                            }
                        )

                        // Stats bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AccentTeal.copy(alpha = 0.04f))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$totalLines lines", color = Silver.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("$totalChars chars", color = Silver.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            if (isCodeTyping) {
                                Text(
                                    "${codeTyperCharIndex}/${totalChars} (${(codeTyperProgress * 100).toInt()}%)",
                                    color = AccentTeal,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // ══ PROGRESS BAR ══
                if (isCodeTyping) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { codeTyperProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (isCodeTyperPaused) WarningYellow else AccentTeal,
                            trackColor = Graphite
                        )
                        Text(
                            if (isCodeTyperPaused) "⏸ PAUSED — Tap ▶ to resume"
                            else "⌨ Typing in progress… ${(codeTyperProgress * 100).toInt()}%",
                            color = if (isCodeTyperPaused) WarningYellow else AccentTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // ══ TYPING PROFILE SELECTOR ══
                Text("TYPING PROFILE", color = Silver.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    profiles.forEach { (name, desc) ->
                        val isActive = selectedProfile == name
                        Surface(
                            onClick = { selectedProfile = name },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isActive) AccentTeal.copy(alpha = 0.12f) else Color.Transparent,
                            border = BorderStroke(
                                0.5.dp,
                                if (isActive) AccentTeal.copy(alpha = 0.6f) else BorderColor.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (isActive) AccentTeal else BorderColor.copy(alpha = 0.4f))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, color = if (isActive) AccentTeal else Platinum, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(desc, color = Silver.copy(alpha = 0.6f), fontSize = 10.sp, lineHeight = 14.sp)
                                }
                                if (isActive) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // ══ ANTI-DETECTION FEATURES ══
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Graphite.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ANTI-DETECTION ENGINE", color = AccentTeal.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        val features = listOf(
                            "✓ Gaussian-distributed inter-key timing (not uniform)",
                            "✓ Context-aware pauses: longer after {, (, newlines",
                            "✓ Burst acceleration within common keywords",
                            "✓ Natural deceleration at special characters",
                            "✓ Variable key hold duration (30-70ms)",
                            "✓ Micro-hesitations at indentation changes",
                            "✓ Thinking pauses at blank lines & block boundaries",
                            "✓ Zero typo injection — exact fidelity guaranteed"
                        )
                        features.forEach { feature ->
                            Text(feature, color = Silver.copy(alpha = 0.7f), fontSize = 10.sp, lineHeight = 14.sp)
                        }
                    }
                }

                // ══ TYPE BUTTON ══
                Button(
                    onClick = {
                        if (!isConnected) {
                            scope.launch { snackbarHostState.showSnackbar("Connect to a host before typing") }
                            return@Button
                        }
                        if (codeInput.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Paste some code first") }
                            return@Button
                        }
                        automationViewModel.startCodeTyper(codeInput, selectedProfile)
                        scope.launch { snackbarHostState.showSnackbar("⌨ Code typing started — $selectedProfile profile") }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = codeInput.isNotBlank() && !isCodeTyping,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) AccentTeal else Graphite,
                        disabledContainerColor = Graphite
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isConnected) 8.dp else 0.dp)
                ) {
                    if (isCodeTyping) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Platinum, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            if (isCodeTyperPaused) "PAUSED" else "TYPING...",
                            fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 14.sp
                        )
                    } else {
                        Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            if (isConnected) "START TYPING CODE" else "NO TARGET LINKED",
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
}
