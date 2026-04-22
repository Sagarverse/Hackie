package com.example.rabit.ui.forensics

import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForensicsLabScreen(
    viewModel: ForensicsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("rabit_prefs", android.content.Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        if (uiState is ForensicsState.Idle) {
            viewModel.loadInstalledApps()
        }
    }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "APP FORENSICS LAB",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text("MANIFEST & APK ANALYSIS", color = SuccessGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState is ForensicsState.AppSelected) {
                            viewModel.backToList()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (val state = uiState) {
                is ForensicsState.Idle, is ForensicsState.LoadingApps -> {
                    CircularProgressIndicator(color = SuccessGreen, modifier = Modifier.align(Alignment.Center))
                }
                is ForensicsState.AppsLoaded -> {
                    AppList(apps = state.apps, onAppClick = { viewModel.selectAppForAnalysis(it) })
                }
                is ForensicsState.AppSelected -> {
                    AppDetailView(
                        state = state,
                        onAnalyzeClick = {
                            val apiKey = prefs.getString("gemini_api_key", "") ?: ""
                            viewModel.analyzeManifestWithAi(state.manifestXml, state.strings, apiKey)
                        }
                    )
                }
                is ForensicsState.Error -> {
                    Text("Error: ${state.message}", color = Color.Red, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun AppList(apps: List<InstalledApp>, onAppClick: (InstalledApp) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredApps = if (searchQuery.isBlank()) {
        apps
    } else {
        apps.filter { it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
    }

    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search packages...", color = Silver) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Silver) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SuccessGreen,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = Graphite,
                unfocusedContainerColor = Graphite
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredApps) { app ->
                AppListItem(app, onAppClick)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun AppListItem(app: InstalledApp, onClick: (InstalledApp) -> Unit) {
    Surface(
        onClick = { onClick(app) },
        color = Graphite,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (app.isSystemApp) Icons.Default.SettingsApplications else Icons.Default.Apps,
                contentDescription = null,
                tint = if (app.isSystemApp) Silver else SuccessGreen,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, color = Platinum, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(app.packageName, color = Silver, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AppDetailView(state: ForensicsState.AppSelected, onAnalyzeClick: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // App Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Android, null, tint = SuccessGreen, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(state.app.appName, color = Platinum, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(state.app.packageName, color = Silver, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = SuccessGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = SuccessGreen
                )
            }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("MANIFEST", fontSize = 11.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("SECRETS", fontSize = 11.sp) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("AI REVIEW", fontSize = 11.sp) })
        }

        Spacer(Modifier.height(16.dp))

        // Content
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            if (selectedTab == 0) {
                // Manifest View
                LazyColumn(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                    item {
                        Text(
                            text = state.manifestXml,
                            color = SuccessGreen.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            } else if (selectedTab == 1) {
                // Secrets View
                if (state.strings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No high-value secrets detected in classes.dex.", color = Silver)
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                        items(state.strings) { secret ->
                            Surface(
                                color = Graphite,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = secret,
                                    color = if (secret.startsWith("http")) AccentBlue else Color(0xFFE11D48),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // AI Review View
                Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    if (state.isAnalyzing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                            CircularProgressIndicator(color = SuccessGreen)
                            Spacer(Modifier.height(16.dp))
                            Text("Gemini Neural Analysis in progress...", color = Silver)
                        }
                    } else if (state.aiInsight != null) {
                        LazyColumn {
                            item {
                                Text(
                                    text = state.aiInsight,
                                    color = Platinum,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Icon(Icons.Default.Security, null, tint = Silver, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("No analysis generated yet.", color = Silver)
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = onAnalyzeClick,
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black)
                            ) {
                                Text("RUN VULNERABILITY SCAN", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
