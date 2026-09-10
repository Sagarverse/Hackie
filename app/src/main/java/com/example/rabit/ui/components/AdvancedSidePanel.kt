package com.example.rabit.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.HackieSpacing

@Composable
internal fun AdvancedSidePanel(
    currentRoute: String,
    isHidConnected: Boolean,
    callbacks: SidePanelCallbacks,
    featureWebBridgeVisible: Boolean,
    featureAutomationVisible: Boolean,
    featureAssistantVisible: Boolean,
    featureSnippetsVisible: Boolean,
    featureSshTerminalVisible: Boolean,
) {
    val entries = remember(
        featureWebBridgeVisible,
        featureAutomationVisible,
        featureAssistantVisible,
        featureSnippetsVisible,
        featureSshTerminalVisible
    ) {
        buildAdvancedEntries(
            featureWebBridgeVisible,
            featureAutomationVisible,
            featureAssistantVisible,
            featureSnippetsVisible,
            featureSshTerminalVisible
        )
    }

    val sections = entries.groupBy { it.section }
    var selectedSection by remember { mutableStateOf(Section.Overview) }
    
    // Select the section that contains the current route
    LaunchedEffect(currentRoute) {
        val entry = entries.find { it.matchesRoute(currentRoute) }
        if (entry != null) {
            selectedSection = entry.section
        }
    }

    // Advanced Glassmorphism / Dark background
    val panelBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(panelBg)
    ) {
        // 1. Primary Icon Rail
        PrimaryRail(
            sections = sections.keys.toList(),
            selectedSection = selectedSection,
            onSectionSelected = { selectedSection = it },
            isHidConnected = isHidConnected,
            onEngageDecoy = callbacks.onEngageDecoy
        )
        
        // Divider
        VerticalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            thickness = 1.dp
        )
        
        // 2. Secondary Detail Panel
        SecondaryPanel(
            section = selectedSection,
            items = sections[selectedSection] ?: emptyList(),
            allEntries = entries,
            currentRoute = currentRoute,
            onNavigate = callbacks.onNavigate,
            isHidConnected = isHidConnected
        )
    }
}

@Composable
private fun PrimaryRail(
    sections: List<Section>,
    selectedSection: Section,
    onSectionSelected: (Section) -> Unit,
    isHidConnected: Boolean,
    onEngageDecoy: () -> Unit
) {
    val view = LocalView.current
    
    Column(
        modifier = Modifier
            .width(72.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = HackieSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        
        // Status indicator
        val ringColor = if (isHidConnected) Color(0xFF00FF00) else MaterialTheme.colorScheme.outlineVariant
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "H",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Section Icons
        val sectionsList = Section.values().toList()
        val selectedIndex = sectionsList.indexOf(selectedSection)
        val indicatorOffset by androidx.compose.animation.core.animateDpAsState(
            targetValue = (selectedIndex * (48 + 16)).dp,
            animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
            label = "indicatorOffset"
        )

        Box {
            // Smooth sliding indicator
            Box(
                modifier = Modifier
                    .offset(y = indicatorOffset)
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )

            Column {
                sectionsList.forEach { section ->
                    val isSelected = selectedSection == section
                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "iconTint"
                    )

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                onSectionSelected(section)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getSectionIcon(section),
                            contentDescription = section.title,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Power Button at bottom
        IconButton(onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onEngageDecoy()
        }) {
            Icon(
                Icons.Default.PowerSettingsNew,
                contentDescription = "Engage Decoy Mode",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SecondaryPanel(
    section: Section,
    items: List<NavEntry>,
    allEntries: List<NavEntry>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    isHidConnected: Boolean
) {
    val view = LocalView.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = HackieSpacing.md)
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        
        var searchQuery by remember { mutableStateOf("") }
        
        // Header
        Text(
            text = if (searchQuery.isBlank()) section.title.uppercase() else "SEARCH RESULTS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search all tools...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
            shape = RoundedCornerShape(12.dp)
        )
        
        val filteredItems = if (searchQuery.isBlank()) {
            items
        } else {
            allEntries.filter { it.label.contains(searchQuery, ignoreCase = true) || (it.sublabel?.contains(searchQuery, ignoreCase = true) == true) }
        }
        
        // List
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(filteredItems) { entry ->
                val isSelected = entry.matchesRoute(currentRoute)
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                    label = "bg"
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onNavigate(entry.route)
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (entry.sublabel != null) {
                            Text(
                                text = entry.sublabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Footer (System Status)
        AdvancedSystemFooter(isHidConnected)
    }
}

@Composable
private fun AdvancedSystemFooter(isHidConnected: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "SYS STATUS",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isHidConnected) Color.Green else Color.Red))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isHidConnected) "ONLINE" else "OFFLINE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { 0.3f },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

private fun getSectionIcon(section: Section): ImageVector {
    return when(section) {
        Section.Overview -> Icons.Default.Dashboard
        Section.Connectivity -> Icons.Default.CloudSync
        Section.Tools -> Icons.Default.Handyman
        Section.Intelligence -> Icons.Default.AutoAwesome
        Section.Research -> Icons.Default.ScreenSearchDesktop
        Section.Settings -> Icons.Default.Settings
    }
}

private fun buildAdvancedEntries(
    featureWebBridgeVisible: Boolean,
    featureAutomationVisible: Boolean,
    featureAssistantVisible: Boolean,
    featureSnippetsVisible: Boolean,
    featureSshTerminalVisible: Boolean,
): List<NavEntry> {
    val all = mutableListOf<NavEntry>()
    all += NavEntry(
        route = "home",
        label = "Home",
        sublabel = null,
        icon = Icons.Default.Home,
        section = Section.Overview,
    )
    all += NavEntry(
        route = "main",
        label = "Control Hub",
        sublabel = "Keyboard & trackpad",
        icon = Icons.Default.Devices,
        section = Section.Overview,
        routeAliases = setOf("keyboard", "pairing"),
    )
    all += NavEntry(
        route = "profile",
        label = "Developer Profile",
        sublabel = "About the maker",
        icon = Icons.Default.Person,
        section = Section.Overview,
    )
    if (featureWebBridgeVisible) {
        all += NavEntry(
            route = "web_bridge",
            label = "Web Bridge",
            sublabel = "Share files via browser",
            icon = Icons.Default.CloudSync,
            section = Section.Connectivity,
        )
    }
    all += NavEntry(
        route = "helper",
        label = "Helper",
        sublabel = "Scan and push to a desktop",
        icon = Icons.Default.Devices,
        section = Section.Connectivity,
    )
    all += NavEntry(
        route = "airplay_receiver",
        label = "AirPlay",
        sublabel = "Audio receiver",
        icon = Icons.Default.Speaker,
        section = Section.Connectivity,
    )
    all += NavEntry(
        route = "password_manager",
        label = "Passwords",
        sublabel = "Secure vault",
        icon = Icons.Default.VpnKey,
        section = Section.Connectivity,
    )
    if (featureAutomationVisible) {
        all += NavEntry(
            route = "automation",
            label = "Macros",
            sublabel = "Quick actions and orchestrator",
            icon = Icons.Default.Bolt,
            section = Section.Tools,
            routeAliases = setOf("macro_orchestrator"),
        )
        all += NavEntry(
            route = "code_typer",
            label = "Code Typer",
            sublabel = null,
            icon = Icons.Default.Keyboard,
            section = Section.Tools,
        )
        all += NavEntry(
            route = "auto_clicker",
            label = "Auto Clicker",
            sublabel = null,
            icon = Icons.Default.AdsClick,
            section = Section.Tools,
        )
        all += NavEntry(
            route = "bluetooth_injector",
            label = "Bluetooth Automator",
            sublabel = "Inject pairing payload",
            icon = Icons.Default.Bluetooth,
            section = Section.Tools,
        )
        all += NavEntry(
            route = "injector",
            label = "Payload Injector",
            sublabel = null,
            icon = Icons.Default.ElectricBolt,
            section = Section.Tools,
        )
    }
    all += NavEntry(
        route = "adb_manager",
        label = "ADB Manager",
        sublabel = null,
        icon = Icons.Default.PhoneAndroid,
        section = Section.Tools,
    )
    if (featureSshTerminalVisible) {
        all += NavEntry(
            route = "ssh_terminal",
            label = "SSH Terminal",
            sublabel = null,
            icon = Icons.Default.Terminal,
            section = Section.Tools,
            routeAliases = setOf("local_terminal"),
        )
    }
    all += NavEntry(
        route = "remote_explorer",
        label = "Remote Explorer",
        sublabel = null,
        icon = Icons.Default.FolderZip,
        section = Section.Tools,
    )
    all += NavEntry(
        route = "process_manager",
        label = "Processes",
        sublabel = null,
        icon = Icons.Default.Memory,
        section = Section.Tools,
    )
    if (featureAssistantVisible) {
        all += NavEntry(
            route = "assistant",
            label = "AI Assistant",
            sublabel = "Conversational control",
            icon = Icons.Default.AutoAwesome,
            section = Section.Intelligence,
        )
    }
    all += NavEntry(
        route = "browser",
        label = "Browser",
        sublabel = null,
        icon = Icons.Default.Explore,
        section = Section.Intelligence,
    )
    if (featureSnippetsVisible) {
        all += NavEntry(
            route = "snippets",
            label = "Snippets",
            sublabel = "Saved text templates",
            icon = Icons.Default.ContentPaste,
            section = Section.Intelligence,
        )
    }
    all += NavEntry(
        route = "network_auditor",
        label = "Network",
        sublabel = "Hosts, ports, ping, Wi-Fi, BLE",
        icon = Icons.Default.ScreenSearchDesktop,
        section = Section.Research,
    )
    all += NavEntry(
        route = "ghost_recon",
        label = "OSINT",
        sublabel = null,
        icon = Icons.Default.Radar,
        section = Section.Research,
    )
    all += NavEntry(
        route = "crypto_toolkit",
        label = "Crypto",
        sublabel = null,
        icon = Icons.Default.Code,
        section = Section.Research,
    )
    all += NavEntry(
        route = "pentest_toolkit",
        label = "Pentest",
        sublabel = null,
        icon = Icons.Default.Handyman,
        section = Section.Research,
    )
    all += NavEntry(
        route = "web_sniper",
        label = "Web security",
        sublabel = null,
        icon = Icons.Default.LocationSearching,
        section = Section.Research,
    )
    all += NavEntry(
        route = "security_auditor",
        label = "Security audits",
        sublabel = "Device and traffic",
        icon = Icons.Default.Shield,
        section = Section.Research,
    )
    all += NavEntry(
        route = "traffic_analyzer",
        label = "Traffic",
        sublabel = null,
        icon = Icons.Default.Monitor,
        section = Section.Research,
    )
    all += NavEntry(
        route = "payload_forge",
        label = "Payload forge",
        sublabel = null,
        icon = Icons.Default.Bolt,
        section = Section.Research,
    )
    all += NavEntry(
        route = "loot_viewer",
        label = "Loot",
        sublabel = null,
        icon = Icons.Default.Inventory,
        section = Section.Research,
    )
    all += NavEntry(
        route = "reverse_shell",
        label = "Reverse shell",
        sublabel = null,
        icon = Icons.Default.Language,
        section = Section.Research,
    )
    all += NavEntry(
        route = "settings",
        label = "Settings",
        sublabel = null,
        icon = Icons.Default.Settings,
        section = Section.Settings,
    )
    return all
}
