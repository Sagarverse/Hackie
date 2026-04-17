package com.example.rabit.ui.components

import kotlinx.coroutines.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

/**
 * RabitAppScaffold — Ultra-minimal global container.
 * Provides a grouped modal drawer and a refined top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RabitAppScaffold(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    showTopBar: Boolean = true,
    featureWebBridgeVisible: Boolean = true,
    featureAutomationVisible: Boolean = true,
    featureAssistantVisible: Boolean = true,
    featureSnippetsVisible: Boolean = true,
    featureShortcutsVisible: Boolean = true,
    featureWakeOnLanVisible: Boolean = true,
    featureSshTerminalVisible: Boolean = true,
    activeApp: String? = null,
    onBack: (() -> Unit)? = null,
    topBarActions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val mainRoutes = listOf("home", "main", "keyboard", "web_bridge", "assistant", "settings", "wake_on_lan", "ssh_terminal", "airplay_receiver", "global_search", "automation", "password_manager", "helper", "auto_clicker", "process_manager", "system_stats", "remote_explorer", "reverse_shell", "terminal_scanner")
    val isSubPage = currentRoute !in mainRoutes

    val screenTitle = when(currentRoute) {
        "home" -> "Home"
        "main", "keyboard" -> "Control Hub"
        "web_bridge" -> "Web Bridge"
        "assistant" -> "Genie AI"
        "settings" -> "Settings"
        "profile" -> "Profile"
        "customization" -> "Theme"
        "password_manager" -> "Passwords"
        "snippets" -> "Snippets"
        "shortcuts" -> "Automation"
        "wake_on_lan" -> "Wake on LAN"
        "ssh_terminal" -> "SSH Terminal"
        "airplay_receiver" -> "AirPlay"
        "global_search" -> "Search"
        "helper" -> "Hackie Helper"
        "auto_clicker" -> "Auto Clicker"
        "process_manager" -> "Processes"
        "system_stats" -> "System Stats"
        "remote_explorer" -> "Remote Explorer"
        "reverse_shell" -> "Reverse Shell"
        "terminal_scanner" -> "Scanner"
        else -> "Hackie"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Surface0,
                drawerContentColor = TextPrimary,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ── Brand Header ──
                    Spacer(modifier = Modifier.height(48.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Hackie",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.W300,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(AccentBlue.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "PRO",
                                color = AccentBlue,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.W700,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Scrollable Navigation ──
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // ── Primary ──
                        DrawerNavItem(
                            label = "Home",
                            icon = Icons.Default.Home,
                            isSelected = currentRoute == "home",
                            onClick = { onNavigate("home"); scope.launch { drawerState.close() } }
                        )
                        DrawerNavItem(
                            label = "Control Hub",
                            icon = Icons.AutoMirrored.Filled.Dvr,
                            isSelected = currentRoute == "main" || currentRoute == "keyboard",
                            onClick = { onNavigate("main"); scope.launch { drawerState.close() } }
                        )

                        // ── Connectivity ──
                        DrawerSectionLabel("Connectivity")

                        if (featureWebBridgeVisible) {
                            DrawerNavItem(
                                label = "Web Bridge",
                                icon = Icons.Default.CloudSync,
                                isSelected = currentRoute == "web_bridge",
                                onClick = { onNavigate("web_bridge"); scope.launch { drawerState.close() } }
                            )
                        }
                        DrawerNavItem(
                            label = "Hackie Helper",
                            icon = Icons.Default.Devices,
                            isSelected = currentRoute == "helper",
                            onClick = { onNavigate("helper"); scope.launch { drawerState.close() } }
                        )
                        DrawerNavItem(
                            label = "AirPlay Receiver",
                            icon = Icons.Default.Speaker,
                            isSelected = currentRoute == "airplay_receiver",
                            onClick = { onNavigate("airplay_receiver"); scope.launch { drawerState.close() } }
                        )

                        // ── Tools ──
                        if (featureAutomationVisible) {
                            DrawerSectionLabel("Tools")

                            DrawerNavItem(
                                label = "Automation",
                                icon = Icons.Default.Bolt,
                                isSelected = currentRoute == "automation",
                                onClick = { onNavigate("automation"); scope.launch { drawerState.close() } }
                            )
                            if (featureSshTerminalVisible) {
                                DrawerNavItem(
                                    label = "SSH Terminal",
                                    icon = Icons.Default.Terminal,
                                    isSelected = currentRoute == "ssh_terminal",
                                    onClick = { onNavigate("ssh_terminal"); scope.launch { drawerState.close() } }
                                )
                            }
                            DrawerNavItem(
                                label = "Remote Explorer",
                                icon = Icons.Default.FolderZip,
                                isSelected = currentRoute == "remote_explorer",
                                onClick = { onNavigate("remote_explorer"); scope.launch { drawerState.close() } }
                            )
                            DrawerNavItem(
                                label = "ADB Manager",
                                icon = Icons.Default.PhoneAndroid,
                                isSelected = currentRoute == "adb_manager",
                                onClick = { onNavigate("adb_manager"); scope.launch { drawerState.close() } }
                            )
                            DrawerNavItem(
                                label = "Process Manager",
                                icon = Icons.Default.Memory,
                                isSelected = currentRoute == "process_manager",
                                onClick = { onNavigate("process_manager"); scope.launch { drawerState.close() } }
                            )
                            DrawerNavItem(
                                label = "System Stats",
                                icon = Icons.Default.Speed,
                                isSelected = currentRoute == "system_stats",
                                onClick = { onNavigate("system_stats"); scope.launch { drawerState.close() } }
                            )
                            DrawerNavItem(
                                label = "Auto Clicker",
                                icon = Icons.Default.AdsClick,
                                isSelected = currentRoute == "auto_clicker",
                                onClick = { onNavigate("auto_clicker"); scope.launch { drawerState.close() } }
                            )
                            DrawerNavItem(
                                label = "Payload Injector",
                                icon = Icons.Default.ElectricBolt,
                                isSelected = currentRoute == "injector",
                                onClick = { onNavigate("injector"); scope.launch { drawerState.close() } }
                            )
                            if (featureWakeOnLanVisible) {
                                DrawerNavItem(
                                    label = "Wake-on-LAN",
                                    icon = Icons.Default.PowerSettingsNew,
                                    isSelected = currentRoute == "wake_on_lan",
                                    onClick = { onNavigate("wake_on_lan"); scope.launch { drawerState.close() } }
                                )
                            }
                        }

                        // ── Intelligence ──
                        if (featureAssistantVisible || featureSnippetsVisible) {
                            DrawerSectionLabel("Intelligence")

                            if (featureAssistantVisible) {
                                DrawerNavItem(
                                    label = "AI Assistant",
                                    icon = Icons.Default.AutoAwesome,
                                    isSelected = currentRoute == "assistant",
                                    onClick = { onNavigate("assistant"); scope.launch { drawerState.close() } }
                                )
                            }
                            if (featureSnippetsVisible) {
                                DrawerNavItem(
                                    label = "Snippets",
                                    icon = Icons.Default.ContentPaste,
                                    isSelected = currentRoute == "snippets",
                                    onClick = { onNavigate("snippets"); scope.launch { drawerState.close() } }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            thickness = 0.5.dp,
                            color = BorderColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Settings group ──
                        DrawerNavItem(
                            label = "Passwords",
                            icon = Icons.Default.Password,
                            isSelected = currentRoute == "password_manager",
                            onClick = { onNavigate("password_manager"); scope.launch { drawerState.close() } }
                        )
                        DrawerNavItem(
                            label = "Settings",
                            icon = Icons.Default.Settings,
                            isSelected = currentRoute == "settings",
                            onClick = { onNavigate("settings"); scope.launch { drawerState.close() } }
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (showTopBar) {
                    Surface(
                        color = Surface0.copy(alpha = 0.96f),
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = screenTitle,
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.titleSmall,
                                    letterSpacing = 0.3.sp
                                )
                            },
                            navigationIcon = {
                                if (isSubPage && onBack != null) {
                                    IconButton(onClick = onBack) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Go back",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(
                                            Icons.Default.Menu,
                                            contentDescription = "Open navigation menu",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            actions = {
                                topBarActions()
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = TextPrimary
                            )
                        )
                        // Subtle bottom border
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = BorderColor.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppAtmosphereGradient)
            ) {
                content(padding)
            }
        }
    }
}

// ── Drawer Components ────────────────────────────────────────────────────────

@Composable
private fun DrawerSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        color = TextTertiary,
        fontSize = 9.sp,
        fontWeight = FontWeight.W700,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun DrawerNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (isSelected) TextPrimary else TextSecondary
    val iconTint = if (isSelected) AccentBlue else TextSecondary.copy(alpha = 0.7f)

    Surface(
        onClick = onClick,
        color = if (isSelected) AccentBlue.copy(alpha = 0.08f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 1.dp)
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = label
                selected = isSelected
                stateDescription = if (isSelected) "Selected" else "Not selected"
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent edge indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AccentBlue)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                label,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400
            )
        }
    }
}

// Keep the old DrawerItem signature for any external callers
@Composable
fun DrawerItem(
    label: String,
    subLabel: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DrawerNavItem(
        label = label,
        icon = icon,
        isSelected = isSelected,
        onClick = onClick
    )
}
