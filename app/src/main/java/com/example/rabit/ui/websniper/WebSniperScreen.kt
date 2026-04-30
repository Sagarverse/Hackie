package com.example.rabit.ui.websniper

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSniperScreen(
    viewModel: WebSniperViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("AUTO-PWN", "FUZZER", "ENUMERATOR", "REPEATER")

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "WEB SNIPER",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 3.sp,
                                    color = Color(0xFFBC13FE)
                                )
                            )
                            Text("DAST EXPLOITATION SUITE", color = Platinum, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFBC13FE),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFFBC13FE)
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    title, 
                                    fontSize = 9.sp, 
                                    fontWeight = if (selectedTab == index) FontWeight.Black else FontWeight.Normal,
                                    color = if (selectedTab == index) Platinum else Silver.copy(alpha = 0.5f)
                                ) 
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> AutoPwnContent(viewModel)
                1 -> WebFuzzerContent(viewModel)
                2 -> DirectoryScannerContent(viewModel)
                3 -> RequestRepeaterContent(viewModel)
            }
        }
    }
}

@Composable
fun AutoPwnContent(viewModel: WebSniperViewModel) {
    var targetDomain by remember { mutableStateOf("192.168.1.100") }
    val autoPwnState by viewModel.autoPwnState.collectAsState()
    val logs by viewModel.autoPwnLogs.collectAsState()
    val report by viewModel.autoPwnReport.collectAsState()
    
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("rabit_prefs", android.content.Context.MODE_PRIVATE)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (autoPwnState == AutoPwnState.IDLE || autoPwnState == AutoPwnState.ERROR || autoPwnState == AutoPwnState.COMPLETE) {
            OutlinedTextField(
                value = targetDomain,
                onValueChange = { targetDomain = it },
                label = { Text("Target IP or Domain") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Platinum,
                    unfocusedTextColor = Platinum,
                    focusedBorderColor = Color(0xFFBC13FE),
                    unfocusedBorderColor = BorderColor
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = {
                    val apiKey = prefs.getString("gemini_api_key", "") ?: ""
                    viewModel.startAutoPwn(targetDomain, apiKey)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBC13FE)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Bolt, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("INITIATE AUTO-PWN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
            }
        } else {
            Button(
                onClick = { viewModel.stopAutoPwn() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Stop, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("ABORT OPERATION", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
        
        Spacer(Modifier.height(20.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PhaseIndicator("1. RECON", isActive = autoPwnState == AutoPwnState.RECONNAISSANCE, isDone = autoPwnState.ordinal > AutoPwnState.RECONNAISSANCE.ordinal, pulseAlpha)
            PhaseIndicator("2. WEAPONIZE", isActive = autoPwnState == AutoPwnState.WEAPONIZATION, isDone = autoPwnState.ordinal > AutoPwnState.WEAPONIZATION.ordinal, pulseAlpha)
            PhaseIndicator("3. TRIAGE", isActive = autoPwnState == AutoPwnState.REPORTING, isDone = autoPwnState.ordinal > AutoPwnState.REPORTING.ordinal, pulseAlpha)
        }
        
        Spacer(Modifier.height(20.dp))

        if (autoPwnState == AutoPwnState.COMPLETE && report != null) {
            Text("EXECUTIVE SUMMARY", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text(text = report!!, color = Platinum, fontSize = 13.sp)
                }
            }
        } else {
            Text("LIVE TELEMETRY", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                LazyColumn(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                    items(logs) { log ->
                        val color = when {
                            log.startsWith("[*]") -> Color(0xFFBC13FE)
                            log.startsWith("[+]") -> SuccessGreen
                            log.startsWith("[-]") -> Color.Red
                            log.startsWith("[!]") -> Color(0xFFE11D48)
                            else -> Silver
                        }
                        Text(text = log, color = color, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WebFuzzerContent(viewModel: WebSniperViewModel) {
    var targetUrl by remember { mutableStateOf("http://192.168.1.100/vulnerable.php?id=") }
    var extractionMode by remember { mutableStateOf(false) }
    val isFuzzing by viewModel.isFuzzing.collectAsState()
    val logs by viewModel.fuzzerLogs.collectAsState()
    val results by viewModel.fuzzerResults.collectAsState()
    
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("rabit_prefs", android.content.Context.MODE_PRIVATE)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = targetUrl,
            onValueChange = { targetUrl = it },
            label = { Text("Target URL Parameter") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Platinum,
                unfocusedTextColor = Platinum,
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = BorderColor
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(12.dp))

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
                Icon(Icons.Default.Storage, contentDescription = null, tint = if (extractionMode) Color(0xFFFF3131) else Silver, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Database Extraction", color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Focus on dumping table data (SQLi)", color = Silver.copy(alpha = 0.6f), fontSize = 10.sp)
                }
            }
            Switch(
                checked = extractionMode,
                onCheckedChange = { extractionMode = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFFF3131)
                )
            )
        }

        Spacer(Modifier.height(12.dp))
        
        Button(
            onClick = {
                if (isFuzzing) viewModel.stopFuzzing()
                else {
                    val apiKey = prefs.getString("gemini_api_key", "") ?: ""
                    if (extractionMode) viewModel.startSqlInjection(targetUrl, apiKey)
                    else viewModel.startFuzzing(targetUrl, apiKey)
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFuzzing) Color(0xFFE11D48) else if (extractionMode) Color(0xFFFF3131) else AccentBlue
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(if (isFuzzing) Icons.Default.Stop else Icons.Default.AutoAwesome, null, tint = Color.Black)
            Spacer(Modifier.width(8.dp))
            Text(if (isFuzzing) "ABORT ATTACK" else if (extractionMode) "INITIALIZE SQLi DUMP" else "GENERATE & INJECT PAYLOADS", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text("EXECUTION LOGS", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            LazyColumn(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                items(logs) { log ->
                    val color = when {
                        log.startsWith("[+]") -> SuccessGreen
                        log.startsWith("[-]") -> Color.Red
                        log.startsWith("[!]") -> Color(0xFFE11D48)
                        else -> Silver
                    }
                    Text(text = log, color = color, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        Text("VULNERABILITIES FOUND: ${results.count { it.reflectionFound || it.dbErrorFound }}", color = Color(0xFFE11D48), fontWeight = FontWeight.Black, fontSize = 11.sp)
    }
}

@Composable
fun DirectoryScannerContent(viewModel: WebSniperViewModel) {
    var targetDomain by remember { mutableStateOf("192.168.1.100") }
    val isScanning by viewModel.isScanningDirs.collectAsState()
    val progress by viewModel.dirProgress.collectAsState()
    val results by viewModel.dirResults.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = targetDomain,
            onValueChange = { targetDomain = it },
            label = { Text("Target Domain/IP") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Platinum,
                unfocusedTextColor = Platinum,
                focusedBorderColor = SuccessGreen,
                unfocusedBorderColor = BorderColor
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(12.dp))
        
        Button(
            onClick = {
                if (isScanning) viewModel.stopDirScanner()
                else viewModel.startDirScanner(targetDomain, 10)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isScanning) Color(0xFFE11D48) else SuccessGreen
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow, null, tint = Color.Black)
            Spacer(Modifier.width(8.dp))
            Text(if (isScanning) "ABORT SCAN" else "LAUNCH ENUMERATION", color = Color.Black, fontWeight = FontWeight.Black)
        }
        
        if (isScanning || progress > 0) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(4.dp), color = SuccessGreen, trackColor = Graphite)
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text("DISCOVERED PATHS", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            LazyColumn(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                items(results) { result ->
                    val color = when (result.statusCode) {
                        200 -> SuccessGreen
                        403 -> Color(0xFFE11D48)
                        301, 302 -> AccentBlue
                        else -> Silver
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "/${result.path}", color = Platinum, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color)) {
                            Text(text = result.statusCode.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestRepeaterContent(viewModel: WebSniperViewModel) {
    var method by remember { mutableStateOf("GET") }
    var url by remember { mutableStateOf("http://192.168.1.100/api/v1/user") }
    var headersText by remember { mutableStateOf("Authorization: Bearer <token>\nUser-Agent: Mozilla/5.0") }
    var bodyText by remember { mutableStateOf("") }
    
    val isRepeating by viewModel.isRepeating.collectAsState()
    val responseHeaders by viewModel.repeaterHeaders.collectAsState()
    val responseBody by viewModel.repeaterResponse.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFFBC13FE),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = Color(0xFFBC13FE))
            }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("REQUEST", fontSize = 10.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("RESPONSE", fontSize = 10.sp) })
        }

        Spacer(Modifier.height(12.dp))

        if (selectedTab == 0) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("Method") }, modifier = Modifier.weight(0.3f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFFBC13FE)))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, modifier = Modifier.weight(0.7f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Platinum))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = headersText, onValueChange = { headersText = it }, label = { Text("Headers") }, modifier = Modifier.fillMaxWidth().height(120.dp), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = bodyText, onValueChange = { bodyText = it }, label = { Text("Body") }, modifier = Modifier.fillMaxWidth().height(150.dp), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp))
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.sendRepeaterRequest(method, url, headersText, bodyText); selectedTab = 1 },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBC13FE))
                ) {
                    if (isRepeating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else {
                        Icon(Icons.AutoMirrored.Filled.Send, null)
                        Spacer(Modifier.width(8.dp))
                        Text("EXECUTE REQUEST", fontWeight = FontWeight.Black)
                    }
                }
            }
        } else {
            Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor), modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                    if (responseHeaders.isNotEmpty()) {
                        Text(text = responseHeaders, color = Color(0xFFBC13FE), fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = BorderColor)
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(text = responseBody, color = Platinum, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun PhaseIndicator(label: String, isActive: Boolean, isDone: Boolean, pulseAlpha: Float) {
    val color = when {
        isDone -> SuccessGreen
        isActive -> Color(0xFFBC13FE)
        else -> Graphite
    }
    
    val modifier = if (isActive) Modifier.alpha(pulseAlpha) else Modifier
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        if (isDone) {
            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color, RoundedCornerShape(12.dp))
            )
        }
        Spacer(Modifier.height(4.dp))
        @SuppressLint("MissingPermission")
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}
