package com.example.rabit.ui.osint

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OsintScreen(
    viewModel: OsintViewModel,
    ghostViewModel: OsintGhostViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("QUICK RECON", "DEEP SEARCH")
    
    // Removed viewModel.searchQuery as it doesn't exist. Using local state.
    val results by viewModel.results.collectAsState()
    val isSearching by viewModel.isScanning.collectAsState()

    val ghostStatus by ghostViewModel.status.collectAsState()
    val ghostLogs by ghostViewModel.searchLogs.collectAsState()

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "GHOST RECON",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    color = Platinum
                                )
                            )
                            Text("OPEN SOURCE INTELLIGENCE", color = if (selectedTab == 0) SuccessGreen else Color.Magenta, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                    contentColor = if (selectedTab == 0) SuccessGreen else Color.Magenta,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = if (selectedTab == 0) SuccessGreen else Color.Magenta
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
        Box(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (selectedTab == 0) {
                Column(modifier = Modifier.fillMaxSize()) {
                    var queryInput by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = queryInput,
                        onValueChange = { queryInput = it },
                        label = { Text("Enter Username / Handle", color = Silver) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = SuccessGreen) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum,
                            focusedBorderColor = SuccessGreen,
                            unfocusedBorderColor = BorderColor
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { viewModel.startUsernameScan(queryInput) })
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isSearching) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = SuccessGreen
                        )
                    }

                    Text(
                        "DISCOVERED TARGETS",
                        color = Silver.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 40.dp)
                    ) {
                        items(results) { result ->
                            OsintResultItem(result)
                        }
                    }
                }
            } else {
                // --- Deep Search Mode (Neural Ghost) ---
                if (ghostStatus is OsintStatus.Idle) {
                    var targetInput by remember { mutableStateOf("") }
                    Column {
                        OutlinedTextField(
                            value = targetInput,
                            onValueChange = { targetInput = it },
                            label = { Text("Target Name / Email / Handle", color = Silver) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Platinum,
                                unfocusedTextColor = Platinum,
                                focusedBorderColor = Color.Magenta,
                                unfocusedBorderColor = BorderColor
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Button(
                            onClick = { ghostViewModel.startDeepSearch(targetInput) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta),
                            shape = RoundedCornerShape(12.dp),
                            enabled = targetInput.isNotBlank()
                        ) {
                            Icon(Icons.Default.AutoAwesome, null)
                            Spacer(Modifier.width(8.dp))
                            Text("INITIATE DEEP SEARCH", fontWeight = FontWeight.Black)
                        }
                    }
                } else {
                    OsintDashboard(ghostViewModel, ghostStatus, ghostLogs)
                }
            }
        }
    }
}

@Composable
fun OsintDashboard(viewModel: OsintGhostViewModel, status: OsintStatus, logs: List<String>) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (status is OsintStatus.Searching) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Color.Magenta
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("DEEP SEARCH LOG", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black, RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))) {
            LazyColumn(modifier = Modifier.padding(12.dp)) {
                items(logs) { log ->
                    Text(log, color = Color.Magenta.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}

@Composable
fun OsintResultItem(result: OsintResult) {
    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Public, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(result.siteName, color = Platinum, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    result.url,
                    color = Silver.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("FOUND", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}
