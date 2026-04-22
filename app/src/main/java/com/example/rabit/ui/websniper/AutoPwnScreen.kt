package com.example.rabit.ui.websniper

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPwnScreen(
    viewModel: WebSniperViewModel,
    onBack: () -> Unit
) {
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

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "1-CLICK AUTO-PWN",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp,
                                color = Color(0xFFBC13FE)
                            )
                        )
                        Text("AUTONOMOUS PENTEST ENGINE", color = Platinum, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
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
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBC13FE)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Bolt, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("INITIATE AUTO-PWN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp)
                }
            } else {
                Button(
                    onClick = { viewModel.stopAutoPwn() },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Stop, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("ABORT OPERATION", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Phase Trackers
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PhaseIndicator("1. RECON", isActive = autoPwnState == AutoPwnState.RECONNAISSANCE, isDone = autoPwnState.ordinal > AutoPwnState.RECONNAISSANCE.ordinal, pulseAlpha)
                PhaseIndicator("2. WEAPONIZE", isActive = autoPwnState == AutoPwnState.WEAPONIZATION, isDone = autoPwnState.ordinal > AutoPwnState.WEAPONIZATION.ordinal, pulseAlpha)
                PhaseIndicator("3. TRIAGE", isActive = autoPwnState == AutoPwnState.REPORTING, isDone = autoPwnState.ordinal > AutoPwnState.REPORTING.ordinal, pulseAlpha)
            }
            
            Spacer(Modifier.height(24.dp))

            if (autoPwnState == AutoPwnState.COMPLETE && report != null) {
                Text("EXECUTIVE SUMMARY", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            text = report!!,
                            color = Platinum,
                            fontSize = 14.sp
                        )
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
                            Text(
                                text = log,
                                color = color,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
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
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}
