package com.example.rabit.ui.automation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HidBruteForceScreen(
    viewModel: HidBruteForceViewModel,
    apiKey: String,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: HID, 1: HASH
    val accentColor = Color(0xFFFF3131) // Aggressive Red
    val hashAccentColor = Color(0xFFEAB308) // Gold
    val bgColor = Color(0xFF05050A)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "BRUTE FORCE LAB",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                        Text(
                            if (selectedTab == 0) "HID INJECTION" else "HASH CRACKER",
                            color = if (selectedTab == 0) accentColor else hashAccentColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // --- Feature Tabs ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = if (selectedTab == 0) accentColor else hashAccentColor,
                divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.05f)) },
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = if (selectedTab == 0) accentColor else hashAccentColor
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Security, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("HID BRUTE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("HASH CRACK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- Main Content Area ---
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> HidBruteForceContent(viewModel, accentColor)
                    1 -> HashCrackerContent(viewModel, apiKey)
                }
            }
        }
    }
}

@Composable
fun HidBruteForceContent(viewModel: HidBruteForceViewModel, accentColor: Color) {
    val isAttacking by viewModel.isAttacking.collectAsState()
    val currentAttempt by viewModel.currentAttempt.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val autoEnter by viewModel.autoEnter.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var pinLength by remember { mutableStateOf("4") }
    var delayMs by remember { mutableStateOf("250") }
    val wordlistPreview by viewModel.wordlistPreview.collectAsState()
    var wordlistUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> 
        wordlistUri = uri 
        uri?.let { viewModel.loadWordlist(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        AttackFeedCard(
            isAttacking = isAttacking,
            currentAttempt = currentAttempt,
            progress = progress,
            stats = stats,
            accentColor = accentColor
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = accentColor,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = accentColor
                )
            }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("NUMERIC PIN", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("WORDLIST", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (selectedTab == 0) {
                ConfigInput("PIN Length", pinLength, onValueChange = { pinLength = it })
            } else {
                WordlistSelector(uri = wordlistUri, onPick = { filePicker.launch("text/plain") })
            }
            
            ConfigInput("Inter-Attempt Delay (ms)", delayMs, onValueChange = { delayMs = it })

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KeyboardReturn, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Auto-Enter", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Suffix [ENTER] to each sequence", color = Color.Gray, fontSize = 10.sp)
                    }
                }
                Switch(
                    checked = autoEnter,
                    onCheckedChange = { viewModel.toggleAutoEnter() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (isAttacking) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        if (isPaused) viewModel.resumeAttack()
                        else viewModel.pauseAttack()
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPaused) "RESUME" else "PAUSE", fontWeight = FontWeight.Black)
                }

                Button(
                    onClick = { viewModel.stopAttack() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("ABORT", fontWeight = FontWeight.Black)
                }
            }
        } else {
            Button(
                onClick = {
                    val d = delayMs.toLongOrNull() ?: 250L
                    val suffix = if (autoEnter) "ENTER" else ""
                    if (selectedTab == 0) {
                        val len = pinLength.toIntOrNull() ?: 4
                        viewModel.startNumericAttack(len, d, suffix)
                    } else {
                        wordlistUri?.let { viewModel.startWordlistAttack(it, d, suffix) }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("INITIALIZE SEQUENCE", fontWeight = FontWeight.Black)
            }
        }

        if (selectedTab == 1 && wordlistPreview.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "DATA VALIDATOR: PREVIEW (First 100 entries)", 
                    color = accentColor, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                wordlistPreview.forEach { line ->
                    Text(
                        line, 
                        color = Color.White.copy(alpha = 0.6f), 
                        fontSize = 12.sp, 
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HashCrackerContent(viewModel: HidBruteForceViewModel, apiKey: String) {
    val state by viewModel.crackerState.collectAsState()
    var hashInput by remember { mutableStateOf("") }
    val accentColor = Color(0xFFEAB308) // Gold

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        when (val s = state) {
            is HashCrackerState.Idle -> {
                Text(
                    "Paste a cryptographic hash below. The system will auto-detect the algorithm (MD5, SHA-1, SHA-256) and attempt to recover the plaintext password using an onboard dictionary, falling back to a Neural Lookup if configured.",
                    color = Silver,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = hashInput,
                    onValueChange = { hashInput = it.trim() },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    label = { Text("Target Hash", color = Silver) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Platinum,
                        unfocusedTextColor = Platinum,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = BorderColor
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                )

                val hashType = viewModel.determineHashType(hashInput)
                if (hashInput.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (hashType != "Unknown") Icons.Default.CheckCircle else Icons.Default.Warning,
                            null,
                            tint = if (hashType != "Unknown") SuccessGreen else Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Algorithm: $hashType",
                            color = if (hashType != "Unknown") SuccessGreen else Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.startCracking(hashInput, apiKey) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, disabledContainerColor = Color.Gray),
                    shape = RoundedCornerShape(12.dp),
                    enabled = hashInput.isNotBlank() && hashType != "Unknown"
                ) {
                    Icon(Icons.Default.VpnKey, null, tint = Obsidian)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("INITIATE CRACK", fontWeight = FontWeight.Black, color = Obsidian)
                }
            }
            is HashCrackerState.Cracking -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { s.progress },
                            modifier = Modifier.size(200.dp),
                            color = accentColor,
                            strokeWidth = 8.dp,
                            trackColor = Color.White.copy(alpha = 0.05f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ALGORITHM", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(s.hashType, color = accentColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Text("TESTING PAYLOAD", color = Silver.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(s.currentGuess, color = Platinum, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                    Spacer(modifier = Modifier.height(24.dp))

                    LinearProgressIndicator(
                        progress = { s.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = accentColor
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    TextButton(onClick = { viewModel.stopCracking() }) {
                        Text("ABORT", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
            is HashCrackerState.Success -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.LockOpen, null, tint = SuccessGreen, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("HASH CRACKED", color = SuccessGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("Method: ${s.method}", color = Silver, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(40.dp))

                    Surface(
                        color = SuccessGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SuccessGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RECOVERED PLAINTEXT", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(s.plaintext, color = SuccessGreen, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = { viewModel.resetCracker(); hashInput = "" },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("NEW HASH", fontWeight = FontWeight.Black, color = Obsidian)
                    }
                }
            }
            is HashCrackerState.Failed -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("CRACK FAILED", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(s.reason, color = Silver, fontSize = 13.sp, textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = { viewModel.resetCracker() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("TRY ANOTHER", fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AttackFeedCard(
    isAttacking: Boolean,
    currentAttempt: String,
    progress: Float,
    stats: HidBruteForceViewModel.AttackStats,
    accentColor: Color
) {
    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isAttacking) accentColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CURRENT PAYLOAD", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(
                text = if (currentAttempt.isEmpty()) "READY" else currentAttempt,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (isAttacking) accentColor else Color.White
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = accentColor,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("PROGRESS", "${(progress * 100).toInt()}%")
                StatItem("COMPLETED", "${stats.completedAttempts}/${stats.totalAttempts}")
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigInput(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFFFF3131),
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
        )
    )
}

@Composable
fun WordlistSelector(uri: android.net.Uri?, onPick: () -> Unit) {
    Surface(
        onClick = onPick,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.Gray)
            Spacer(Modifier.width(12.dp))
            Text(
                text = uri?.lastPathSegment ?: "Pick custom wordlist (.txt)",
                color = if (uri == null) Color.Gray else Color.White,
                fontSize = 13.sp
            )
        }
    }
}
