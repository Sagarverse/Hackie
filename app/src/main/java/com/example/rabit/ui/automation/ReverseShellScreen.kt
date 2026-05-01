package com.example.rabit.ui.automation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.exploit.ReverseShellViewModel
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReverseShellScreen(
    viewModel: AutomationViewModel,
    genViewModel: ReverseShellViewModel,
    onBack: () -> Unit,
    onNavigateToDesktop: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("TACTICAL HUB", "PAYLOAD FACTORY")

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "REVERSE SHELL SUITE",
                                color = AccentTeal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Text(
                                "C2 Tunnel & Payload Generator",
                                color = Silver.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToDesktop) {
                            Icon(Icons.Default.Monitor, "Desktop View", tint = AccentTeal)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Graphite.copy(alpha = 0.5f),
                        scrolledContainerColor = Graphite.copy(alpha = 0.5f)
                    )
                )
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Graphite.copy(alpha = 0.5f),
                    contentColor = Platinum,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = AccentTeal
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    color = if (selectedTab == index) AccentTeal else Silver.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Crossfade(targetState = selectedTab, label = "tab_crossfade") { tab ->
                when (tab) {
                    0 -> HubTab(viewModel = viewModel)
                    1 -> FactoryTab(genViewModel = genViewModel)
                }
            }
        }
    }
}

@Composable
private fun HubTab(viewModel: AutomationViewModel) {
    val lines by viewModel.reverseShellLines.collectAsState()
    val status by viewModel.reverseShellStatus.collectAsState()
    val isConnected by viewModel.reverseShellConnected.collectAsState()
    val isListening by viewModel.isReverseShellListening.collectAsState()

    var commandInput by remember { mutableStateOf("") }
    var portInput by remember { mutableStateOf("4444") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        // Connection Panel
        Surface(
            color = if (isConnected) SuccessGreen.copy(alpha = 0.1f) else Graphite.copy(alpha = 0.4f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isConnected) SuccessGreen.copy(alpha = 0.5f) else BorderColor.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            when {
                                isConnected -> SuccessGreen
                                isListening -> WarningYellow
                                else -> ErrorRed
                            },
                            RoundedCornerShape(5.dp)
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        status,
                        color = Platinum,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        when {
                            isConnected -> "C2 Session Active & Stable"
                            isListening -> "Listener active, awaiting payload..."
                            else -> "Listener offline. Set port and start."
                        },
                        color = Silver.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                if (!isConnected) {
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it },
                        label = { Text("LPORT", color = Silver.copy(alpha = 0.5f), fontSize = 9.sp) },
                        modifier = Modifier.width(80.dp),
                        singleLine = true,
                        enabled = !isListening,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum,
                            focusedBorderColor = AccentTeal,
                            unfocusedBorderColor = BorderColor.copy(alpha = 0.3f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = {
                            if (isListening) viewModel.stopReverseShellListener()
                            else viewModel.startReverseShellListener(portInput.toIntOrNull() ?: 4444)
                        },
                        modifier = Modifier.background(
                            if (isListening) ErrorRed.copy(alpha = 0.1f) else AccentTeal.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                    ) {
                        Icon(
                            if (isListening) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isListening) ErrorRed else AccentTeal
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.stopReverseShellListener() },
                        modifier = Modifier.background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, tint = ErrorRed)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isConnected && !isListening) {
            val context = LocalContext.current
            val tunnelActive by viewModel.isTunnelActive.collectAsState()
            val globalAddr by viewModel.globalC2Address.collectAsState()
            val localIp = remember { com.example.rabit.data.network.LanIpResolver.preferredLanIpv4String(context) ?: "127.0.0.1" }
            val currentIp = if (tunnelActive && globalAddr.isNotBlank()) globalAddr else localIp

            Card(
                colors = CardDefaults.cardColors(containerColor = Graphite.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (tunnelActive) Icons.Default.Language else Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = if (tunnelActive) SuccessGreen else AccentTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (tunnelActive) "GLOBAL INFILTRATION TUNNEL" else "LOCAL LAN EXPLOITATION",
                            color = if (tunnelActive) SuccessGreen else AccentTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        "Your current active listener IP address is:",
                        color = Silver.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                    Surface(
                        color = Obsidian,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            currentIp,
                            color = Platinum,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        } else {
            if (isConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TacticButton(
                        text = "PERSISTENCE",
                        icon = Icons.Default.CloudSync,
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.installPersistence() }
                    )
                    TacticButton(
                        text = "PHISH CREDS",
                        icon = Icons.Default.LockOpen,
                        color = WarningYellow,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.injectCredentialPrompt() }
                    )
                    TacticButton(
                        text = "KEYLOG",
                        icon = Icons.Default.Keyboard,
                        color = AccentBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.deployKeylogger() }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                color = Color(0xFF0D0D14),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.4f))
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    items(lines) { line ->
                        Text(
                            line,
                            color = if (line.startsWith(">")) AccentTeal else SuccessGreen.copy(alpha = 0.85f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    placeholder = { Text("C:\\> command...", color = Silver.copy(alpha = 0.3f), fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.weight(1f),
                    enabled = isConnected,
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Platinum, fontSize = 13.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (commandInput.isNotBlank()) {
                            viewModel.sendReverseShellCommand(commandInput)
                            commandInput = ""
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Platinum,
                        unfocusedTextColor = Platinum,
                        focusedBorderColor = AccentTeal,
                        unfocusedBorderColor = BorderColor.copy(alpha = 0.5f),
                        focusedContainerColor = Graphite.copy(alpha = 0.3f),
                        unfocusedContainerColor = Graphite.copy(alpha = 0.3f)
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = {
                        if (commandInput.isNotBlank()) {
                            viewModel.sendReverseShellCommand(commandInput)
                            commandInput = ""
                        }
                    },
                    enabled = isConnected,
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (isConnected) AccentTeal.copy(alpha = 0.2f) else Graphite.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = if (isConnected) AccentTeal else Silver.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun FactoryTab(genViewModel: ReverseShellViewModel) {
    val ip by genViewModel.ip.collectAsState()
    val port by genViewModel.port.collectAsState()
    val shells by genViewModel.generatedShells.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                color = Graphite.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Construction, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TARGET CONFIGURATION", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = ip, onValueChange = { genViewModel.updateIp(it) },
                            label = { Text("LHOST (Your IP)", color = Silver, fontSize = 10.sp) },
                            modifier = Modifier.weight(2f), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Platinum,
                                unfocusedTextColor = Platinum,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor.copy(alpha = 0.5f)
                            ),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            trailingIcon = {
                                IconButton(onClick = { genViewModel.autoFetchIp() }) {
                                    Icon(Icons.Default.MyLocation, "Auto-fetch IP", tint = AccentBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                        OutlinedTextField(
                            value = port, onValueChange = { genViewModel.updatePort(it) },
                            label = { Text("LPORT", color = Silver, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Platinum,
                                unfocusedTextColor = Platinum,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor.copy(alpha = 0.5f)
                            ),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { genViewModel.generate() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SettingsApplications, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("GENERATE PAYLOADS", fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }

        items(shells) { shell ->
            val langColor = when (shell.language.lowercase()) {
                "bash" -> SuccessGreen
                "python" -> Color(0xFF3776AB)
                "powershell" -> Color(0xFF5391FE)
                "perl" -> Color(0xFF39457E)
                "php" -> Color(0xFF777BB4)
                "ruby" -> Color(0xFFCC342D)
                "netcat" -> AccentTeal
                else -> Color.White
            }
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, langColor.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(8.dp).background(langColor, RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(shell.name, color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Shell", shell.template))
                            },
                            modifier = Modifier.size(32.dp).background(langColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = langColor, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        shell.template,
                        color = Platinum.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(12.dp).fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun TacticButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}
