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
import com.example.rabit.ui.theme.Platinum

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HidBruteForceScreen(viewModel: HidBruteForceViewModel, onBack: () -> Unit) {
    val isAttacking by viewModel.isAttacking.collectAsState()
    val currentAttempt by viewModel.currentAttempt.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val autoEnter by viewModel.autoEnter.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var pinLength by remember { mutableStateOf("4") }
    var delayMs by remember { mutableStateOf("250") }
    var wordlistUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> wordlistUri = uri }

    val accentColor = Color(0xFFFF3131) // Aggressive Red
    val bgColor = Color(0xFF05050A)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("HID BRUTE FORCER", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Live Attack Status ---
            AttackFeedCard(
                isAttacking = isAttacking,
                currentAttempt = currentAttempt,
                progress = progress,
                stats = stats,
                accentColor = accentColor
            )

            // --- Configuration Tabs ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = accentColor,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
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

            // --- Settings Area ---
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

            // --- Control Buttons ---
            Button(
                onClick = {
                    if (isAttacking) {
                        viewModel.stopAttack()
                    } else {
                        val d = delayMs.toLongOrNull() ?: 250L
                        val suffix = if (autoEnter) "ENTER" else ""
                        if (selectedTab == 0) {
                            val len = pinLength.toIntOrNull() ?: 4
                            viewModel.startNumericAttack(len, d, suffix)
                        } else {
                            wordlistUri?.let { viewModel.startWordlistAttack(it, d, suffix) }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAttacking) Color.White.copy(alpha = 0.1f) else accentColor,
                    contentColor = if (isAttacking) Color.White else Color.Black
                )
            ) {
                Icon(if (isAttacking) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isAttacking) "ABORT ATTACK" else "INITIALIZE SEQUENCE", fontWeight = FontWeight.Black)
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
                progress = progress,
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
