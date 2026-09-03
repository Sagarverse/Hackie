package com.example.rabit.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.automation.AutomationViewModel
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.opsec.KillSwitchViewModel
import com.example.rabit.ui.opsec.PanicTerminalContent

@Composable
fun SettingsOpsecScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    automationViewModel: AutomationViewModel,
    killSwitchViewModel: KillSwitchViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Passwords", "Panic")
    val accent = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error

    ScreenScaffold(
        title = "Settings",
        subtitle = "App preferences and security.",
        onBack = onBack,
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = accent,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = if (selectedTab == 2) error else accent,
                            )
                        }
                    },
                ) {
                    tabs.forEachIndexed { index, title ->
                        val color = if (index == 2) error else accent
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selectedTab == index) color
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }

                when (selectedTab) {
                    0 -> SettingsContent(
                        viewModel = viewModel,
                        settingsViewModel = settingsViewModel,
                        automationViewModel = automationViewModel,
                    )
                    1 -> PasswordManagerContent(
                        settingsViewModel = settingsViewModel,
                        viewModel = viewModel,
                    )
                    2 -> PanicTerminalContent(
                        viewModel = viewModel,
                        killSwitchViewModel = killSwitchViewModel,
                    )
                }
            }
        }
    }
}
