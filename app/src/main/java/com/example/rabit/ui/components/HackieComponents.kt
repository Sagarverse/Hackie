package com.example.rabit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HackieTopBar(
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    title: String = "HACKIE HELPER"
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                ),
                color = Platinum
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, "Menu", tint = Platinum)
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, "Settings", tint = Platinum)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Platinum
        )
    )
}

@Composable
fun BluetoothDisabledBanner(onEnableClick: () -> Unit) {
    DarkSkeuoCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = WarningYellow.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(WarningYellow.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BluetoothDisabled, null, tint = WarningYellow, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Bluetooth is Off", color = Platinum, fontWeight = FontWeight.Bold)
                Text("Enable it to discove nearby computers", color = Silver, fontSize = 11.sp)
            }
            TextButton(onClick = onEnableClick) {
                Text("ENABLE", color = WarningYellow, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun HackieNavigationDrawer(
    onNavigateToHelper: () -> Unit,
    onNavigateToWebBridge: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToInjector: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToKeyboard: () -> Unit,
    onNavigateToAirPlayReceiver: () -> Unit,
    onNavigateToWakeOnLan: () -> Unit,
    onNavigateToSshTerminal: () -> Unit,
    onNavigateToAutomation: () -> Unit,
    onNavigateToSnippets: () -> Unit,
    onNavigateToPasswordManager: () -> Unit,
    onClose: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Obsidian,
        drawerContentColor = Platinum,
        drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
        modifier = Modifier.width(320.dp).fillMaxHeight()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text("SYSTEM", color = Silver.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            DrawerNavItem("Home", Icons.Default.Home, false, onNavigateToKeyboard)
            DrawerNavItem("Web Bridge", Icons.Default.CloudSync, false, onNavigateToWebBridge)
            DrawerNavItem("Assistant", Icons.Default.AutoAwesome, false, onNavigateToAssistant)
            DrawerNavItem("Automation", Icons.Default.Bolt, false, onNavigateToAutomation)
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("UTILITIES", color = Silver.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            DrawerNavItem("Wake-on-LAN", Icons.Default.PowerSettingsNew, false, onNavigateToWakeOnLan)
            DrawerNavItem("SSH Terminal", Icons.Default.Terminal, false, onNavigateToSshTerminal)
            
            Spacer(modifier = Modifier.weight(1f))
            DrawerNavItem("Settings", Icons.Default.Settings, false, onNavigateToSettings)
            
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("CLOSE", color = Silver)
            }
        }
    }
}

@Composable
private fun DrawerNavItem(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) AccentBlue.copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (isSelected) AccentBlue else Silver, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, color = if (isSelected) Platinum else Silver, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
