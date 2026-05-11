package com.example.rabit.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.automation.AutomationViewModel
import com.example.rabit.ui.opsec.KillSwitchViewModel
import com.example.rabit.ui.opsec.PanicTerminalContent
import com.example.rabit.ui.components.LocalOpenGlobalDrawer
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsOpsecScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    automationViewModel: AutomationViewModel,
    killSwitchViewModel: KillSwitchViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("SYSTEM SETTINGS", "PASSWORD VAULT", "PANIC TERMINAL")
    val colors = listOf(AccentTeal, AccentBlue, Color(0xFFE11D48))

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            val openDrawer = LocalOpenGlobalDrawer.current
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "SETTINGS & OPSEC",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    color = Platinum
                                )
                            )
                            Text("SECURITY OPERATIONS CENTER", color = colors[selectedTab], fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { openDrawer?.invoke() }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Platinum, modifier = Modifier.size(20.dp))
                        }
                    },
                    actions = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Silver)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
                
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = colors[selectedTab],
                    edgePadding = 16.dp,
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> {
                    SettingsContent(
                        viewModel = viewModel,
                        settingsViewModel = settingsViewModel,
                        automationViewModel = automationViewModel
                    )
                }
                1 -> {
                    PasswordManagerContent(
                        settingsViewModel = settingsViewModel,
                        viewModel = viewModel
                    )
                }
                2 -> {
                    PanicTerminalContent(
                        viewModel = viewModel,
                        killSwitchViewModel = killSwitchViewModel
                    )
                }
            }
        }
    }
}
