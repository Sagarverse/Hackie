package com.example.rabit.ui.network

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.security.FindingCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiAttackerScreen(viewModel: WifiAttackerViewModel, apiKey: String, onBack: () -> Unit) {
    val networks by viewModel.networks.collectAsState()
    val attackState by viewModel.attackState.collectAsState()

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "NEURAL WIFI AUDITOR",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text("802.11 TACTICAL SUITE", color = Color(0xFFEAB308), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (attackState != WifiAttackState.Idle && attackState != WifiAttackState.Scanning) viewModel.stopAttack()
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                    }
                },
                actions = {
                    if (attackState == WifiAttackState.Idle || attackState == WifiAttackState.Scanning) {
                        IconButton(onClick = { viewModel.startScan() }) {
                            Icon(Icons.Default.WifiFind, null, tint = Color(0xFFEAB308))
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = attackState) {
                is WifiAttackState.Idle, is WifiAttackState.Scanning -> {
                    if (state is WifiAttackState.Scanning) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = Color(0xFFEAB308))
                    }
                    WifiNetworkList(networks, viewModel, onAttack = { viewModel.startAttack(it, apiKey) })
                }
                is WifiAttackState.Attacking -> {
                    AttackProgress(state)
                }
                is WifiAttackState.Success -> {
                    AttackSuccess(state, onReset = { viewModel.stopAttack() })
                }
                is WifiAttackState.Failed -> {
                    AttackFailed(state, onReset = { viewModel.stopAttack() })
                }
            }
        }
    }
}

@Composable
fun WifiNetworkList(networks: List<WifiNetwork>, viewModel: WifiAttackerViewModel, onAttack: (WifiNetwork) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(networks) { network ->
            var expanded by remember { mutableStateOf(false) }
            val vulnerabilities = viewModel.getVulnerabilities(network)

            Column {
                Surface(
                    onClick = { expanded = !expanded },
                    color = Color.White.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (vulnerabilities.isNotEmpty()) Color.Red.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                ) {
                    Column {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (network.signalLevel > -60) Icons.Default.Wifi else Icons.Default.Wifi2Bar,
                                null,
                                tint = if (network.isWpsSupported || vulnerabilities.isNotEmpty()) Color(0xFFEAB308) else Platinum
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(network.ssid, color = Platinum, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val entropy = viewModel.calculateEntropy(network)
                                val crackTime = viewModel.getEstimatedCrackTime(entropy)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(network.security, color = if (network.security.contains("VULNERABLE")) Color.Red else Silver.copy(alpha = 0.6f), fontSize = 10.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("•", color = Silver.copy(alpha = 0.3f), fontSize = 10.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("$crackTime Crack", color = if (entropy < 40) Color.Red else SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                if (vulnerabilities.isNotEmpty()) {
                                    Text("${vulnerabilities.size} Vulnerabilities Detected", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                            if (network.isWpsSupported) {
                                Surface(color = Color(0xFFEAB308).copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                    Text("WPS", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color(0xFFEAB308), fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { onAttack(network) }) {
                                Icon(Icons.Default.FlashOn, "Attack", tint = Color.Red)
                            }
                        }
                    }
                }
                
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        vulnerabilities.forEach { finding ->
                            FindingCard(finding)
                        }
                        if (vulnerabilities.isEmpty()) {
                            Text("No major vulnerabilities detected beyond standard crack times.", color = SuccessGreen, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttackProgress(state: WifiAttackState.Attacking) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.size(200.dp),
                color = Color(0xFFEAB308),
                strokeWidth = 8.dp,
                trackColor = Color.White.copy(alpha = 0.05f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TESTING", color = Color(0xFFEAB308), fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text(state.currentPassword, color = Platinum, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text("AUDITING: ${state.network.ssid}", color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("BSSID: ${state.network.bssid}", color = Silver.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = Color(0xFFEAB308)
        )
    }
}

@Composable
fun AttackSuccess(state: WifiAttackState.Success, onReset: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("VULNERABILITY CONFIRMED", color = SuccessGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text("NETWORK COMPROMISED", color = Platinum, fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Surface(
            color = SuccessGreen.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RECOVERED CREDENTIAL", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(state.password, color = SuccessGreen, fontSize = 32.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("FINALIZE AUDIT", fontWeight = FontWeight.Black, color = Obsidian)
        }
    }
}

@Composable
fun AttackFailed(state: WifiAttackState.Failed, onReset: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("AUDIT INCONCLUSIVE", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(state.reason, color = Silver, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("BACK TO SCANNER", fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}
