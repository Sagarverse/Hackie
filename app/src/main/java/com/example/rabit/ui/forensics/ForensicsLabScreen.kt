package com.example.rabit.ui.forensics

import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
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
    exifViewModel: ExifForensicsViewModel,
    onBack: () -> Unit
) {
    var currentSubFeature by remember { mutableStateOf("app") } // "app", "exif"
    val accentColor = SuccessGreen
    val exifAccentColor = AccentBlue
    val bgColor = Obsidian

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("rabit_prefs", android.content.Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        if (uiState is ForensicsState.Idle) {
            viewModel.loadInstalledApps()
        }
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (currentSubFeature == "app") "APP FORENSICS LAB" else "EXIF FORENSICS LAB",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentSubFeature == "app" && uiState is ForensicsState.AppSelected) {
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
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                when (currentSubFeature) {
                    "app" -> {
                        ForensicsLabContent(viewModel, uiState, prefs)
                    }
                    "exif" -> {
                        ExifForensicsContent(exifViewModel)
                    }
                }
            }

            // Right-side Mini Sidebar
            Surface(
                color = Color.White.copy(alpha = 0.03f),
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    MiniSidebarIcon(
                        icon = Icons.Default.Apps,
                        isSelected = currentSubFeature == "app",
                        accentColor = accentColor,
                        onClick = { currentSubFeature = "app" }
                    )
                    MiniSidebarIcon(
                        icon = Icons.Default.ImageSearch,
                        isSelected = currentSubFeature == "exif",
                        accentColor = exifAccentColor,
                        onClick = { currentSubFeature = "exif" }
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniSidebarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                null,
                tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .background(accentColor, CircleShape)
                )
            }
        }
    }
}

@Composable
fun ForensicsLabContent(
    viewModel: ForensicsViewModel,
    uiState: ForensicsState,
    prefs: android.content.SharedPreferences
) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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


