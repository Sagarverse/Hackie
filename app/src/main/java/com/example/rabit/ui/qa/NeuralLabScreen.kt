package com.example.rabit.ui.qa

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralLabScreen(
    qaViewModel: NeuralQaViewModel,
    webViewModel: NeuralWebAuditorViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("APP AUDITOR", "WEB AUDITOR")
    val colors = listOf(AccentBlue, Color.Magenta)

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "NEURAL LAB",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    color = Platinum
                                )
                            )
                            Text("QUALITY & SECURITY ASSURANCE", color = colors[selectedTab], fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                    contentColor = colors[selectedTab],
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = colors[selectedTab]
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
                                    fontSize = 11.sp, 
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
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> {
                    val status by qaViewModel.status.collectAsState()
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        if (status is QaAuditStatus.Idle) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Button(
                                    onClick = { qaViewModel.startAudit("") },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors[0]),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("START APP AUDIT", fontWeight = FontWeight.Black)
                                }
                            }
                        } else {
                            AuditDashboard(qaViewModel)
                        }
                    }
                }
                1 -> {
                    val status by webViewModel.status.collectAsState()
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        if (status is WebAuditStatus.Idle) {
                            var urlInput by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                label = { Text("Target URL", color = Silver) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Platinum,
                                    unfocusedTextColor = Platinum,
                                    focusedBorderColor = colors[1],
                                    unfocusedBorderColor = BorderColor
                                )
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { webViewModel.startAudit(urlInput) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = colors[1]),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("START WEB AUDIT", fontWeight = FontWeight.Black)
                            }
                        } else {
                            WebAuditDashboard(webViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditDashboard(viewModel: NeuralQaViewModel) {
    val status by viewModel.status.collectAsState()
    val logs by viewModel.logs.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        if (status is QaAuditStatus.Scanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = AccentBlue)
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs) { log ->
                Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Text("[${log.tag}]", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(log.message, color = Platinum, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WebAuditDashboard(viewModel: NeuralWebAuditorViewModel) {
    val status by viewModel.status.collectAsState()
    val logs by viewModel.logs.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        if (status is WebAuditStatus.Analyzing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = Color.Magenta)
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs) { log ->
                Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Text("[${log.tag}]", color = Color.Magenta, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(log.message, color = Platinum, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
