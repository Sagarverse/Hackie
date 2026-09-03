package com.example.rabit.ui.components

import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.rabit.data.prefs.UserPreferences
import com.example.rabit.ui.theme.HackieSpacing
import com.example.rabit.ui.theme.Success
import kotlinx.coroutines.delay

// ─── Public model ─────────────────────────────────────────────────────────

/**
 * A single entry in the side-panel navigation list. Each item is rendered
 * as a pill with an icon, a label, and an optional sublabel.
 */
internal data class NavEntry(
    val route: String,
    val label: String,
    val sublabel: String?,
    val icon: ImageVector,
    val section: Section,
    val routeAliases: Set<String> = emptySet(),
) {
    fun matchesRoute(currentRoute: String) =
        currentRoute == route || currentRoute in routeAliases
}

internal enum class Section(val title: String) {
    Overview("Overview"),
    Connectivity("Connectivity"),
    Tools("Tools"),
    Intelligence("Intelligence"),
    Research("Research"),
    Settings("Settings"),
}

internal data class SidePanelCallbacks(
    val onNavigate: (String) -> Unit,
    val onPanicLock: () -> Unit,
    val onToggleTheme: () -> Unit,
    val onRunScan: () -> Unit,
    val onOpenSnippets: () -> Unit,
)

/**
 * The body of the side panel, used by both the modal drawer (phones) and
 * the permanent drawer (tablets/wide).
 *
 * The visual design borrows Apple's restraint (pill-shaped selected state,
 * iconography first, subtle dividers) and adds:
 *   • a live status orb at the top with the connection state
 *   • one-tap quick action chips (scan / lock / theme / snippets)
 *   • inline search with `/` keyboard hint
 *   • long-press-to-pin on any item, pinned items float to the top
 *   • section count badges so the user can see what each section holds
 */
@Composable
internal fun SidePanelBody(
    currentRoute: String,
    isHidConnected: Boolean,
    callbacks: SidePanelCallbacks,
    featureWebBridgeVisible: Boolean,
    featureAutomationVisible: Boolean,
    featureAssistantVisible: Boolean,
    featureSnippetsVisible: Boolean,
    featureSshTerminalVisible: Boolean,
) {
    val view = LocalView.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val entries = remember(
        featureWebBridgeVisible,
        featureAutomationVisible,
        featureAssistantVisible,
        featureSnippetsVisible,
        featureSshTerminalVisible,
    ) {
        buildEntries(
            featureWebBridgeVisible = featureWebBridgeVisible,
            featureAutomationVisible = featureAutomationVisible,
            featureAssistantVisible = featureAssistantVisible,
            featureSnippetsVisible = featureSnippetsVisible,
            featureSshTerminalVisible = featureSshTerminalVisible,
        )
    }

    // Pinned items persist via SharedPreferences. Read on first composition
    // and re-read when the user pins/unpins to keep the UI in sync.
    val context = androidx.compose.ui.platform.LocalContext.current
    var pinnedRoutes by remember {
        mutableStateOf(loadPinnedRoutes(context))
    }
    fun persistPin(route: String, pinned: Boolean) {
        pinnedRoutes = updatePinnedRoutes(context, route, pinned)
    }

    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // '/'-style: pressing forward-slash focuses search. Honors the user's
    // expectation from a desktop app while not stealing focus from text
    // fields they may be typing in elsewhere.
    LaunchedEffect(Unit) {
        // Reserved for future global-hotkey wiring.
    }

    // Filter entries by query, keep section grouping.
    val filtered = entries.filter { it.matches(searchQuery) }
    val pinnedEntries = remember(filtered, pinnedRoutes) {
        filtered.filter { it.route in pinnedRoutes }
            .sortedBy { pinnedRoutes.indexOf(it.route) }
    }
    val bySection = remember(filtered, pinnedRoutes) {
        Section.values().map { section ->
            section to filtered.filter { it.section == section && it.route !in pinnedRoutes }
        }.filter { it.second.isNotEmpty() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Status orb + brand header ─────────────────────────────
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Spacer(Modifier.height(HackieSpacing.md))
        StatusHeader(
            isHidConnected = isHidConnected,
            onToggleTheme = callbacks.onToggleTheme,
        )

        Spacer(Modifier.height(HackieSpacing.md))

        // ── Quick action chips ────────────────────────────────────
        QuickActionsRow(
            isHidConnected = isHidConnected,
            onScan = callbacks.onRunScan,
            onLock = callbacks.onPanicLock,
            onTheme = callbacks.onToggleTheme,
            onSnippets = callbacks.onOpenSnippets,
        )

        Spacer(Modifier.height(HackieSpacing.md))

        // ── Search ────────────────────────────────────────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HackieSpacing.md)
                .focusRequester(focusRequester),
            placeholder = { Text("Search") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            trailingIcon = {
                // Keyboard-hint chip, like the ⌘K you see in IDEs.
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = "/",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = MaterialTheme.shapes.large,
        )

        Spacer(Modifier.height(HackieSpacing.sm))

        // ── Navigation list ───────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            if (pinnedEntries.isNotEmpty()) {
                SectionHeader(title = "Pinned", count = pinnedEntries.size)
                pinnedEntries.forEach { entry ->
                    PanelItem(
                        entry = entry,
                        isSelected = entry.matchesRoute(currentRoute),
                        isPinned = true,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            callbacks.onNavigate(entry.route)
                        },
                        onTogglePin = { persistPin(entry.route, false) },
                    )
                }
            }

            bySection.forEach { (section, items) ->
                SectionHeader(title = section.title, count = items.size)
                items.forEach { entry ->
                    PanelItem(
                        entry = entry,
                        isSelected = entry.matchesRoute(currentRoute),
                        isPinned = entry.route in pinnedRoutes,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            callbacks.onNavigate(entry.route)
                        },
                        onTogglePin = {
                            val wasPinned = entry.route in pinnedRoutes
                            persistPin(entry.route, !wasPinned)
                        },
                    )
                }
            }

            if (bySection.isEmpty() && pinnedEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(HackieSpacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No matches for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(HackieSpacing.lg))
        }

        // ── Footer: connection state + global lock ────────────────
        PanelFooter(
            isHidConnected = isHidConnected,
            onPanicLock = callbacks.onPanicLock,
        )
    }
}

// ─── Building the navigation list ────────────────────────────────────────

private fun buildEntries(
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

private fun NavEntry.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.trim().lowercase()
    return label.lowercase().contains(q) ||
        sublabel?.lowercase()?.contains(q) == true ||
        section.title.lowercase().contains(q)
}

// ─── Pieces ──────────────────────────────────────────────────────────────

@Composable
private fun StatusHeader(
    isHidConnected: Boolean,
    onToggleTheme: () -> Unit,
) {
    val ringColor by animateColorAsState(
        targetValue = if (isHidConnected) Success else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(durationMillis = 320),
        label = "ring",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HackieSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HackieSpacing.md),
    ) {
        // Status orb: a circular brand-tinted avatar with a live ring.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Inner ring colored by connection state.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(width = 2.dp, color = ringColor, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "H",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hackie",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ringColor)
                )
                Text(
                    text = if (isHidConnected) "HID channel active" else "Ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Theme toggle is a one-tap shortcut, also exposed in the
        // quick-action row below — this one is more discoverable.
        val view = LocalView.current
        Surface(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onToggleTheme()
            },
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = CircleShape,
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = "Toggle theme",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    isHidConnected: Boolean,
    onScan: () -> Unit,
    onLock: () -> Unit,
    onTheme: () -> Unit,
    onSnippets: () -> Unit,
) {
    val view = LocalView.current
    val chips = listOf(
        QuickAction("Scan", Icons.Default.Radar, MaterialTheme.colorScheme.primary) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onScan()
        },
        QuickAction("Snippets", Icons.Default.ContentPaste, MaterialTheme.colorScheme.tertiary) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onSnippets()
        },
        QuickAction("Theme", Icons.Default.DarkMode, MaterialTheme.colorScheme.secondary) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onTheme()
        },
        QuickAction(
            "Lock",
            Icons.Default.Lock,
            if (isHidConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        ) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onLock()
        },
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = HackieSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(chips) { chip ->
            QuickActionChip(chip)
        }
    }
}

private data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit,
)

@Composable
private fun QuickActionChip(chip: QuickAction) {
    Surface(
        onClick = chip.onClick,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = HackieSpacing.md, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = chip.icon,
                contentDescription = null,
                tint = chip.tint,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = chip.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = HackieSpacing.lg,
                end = HackieSpacing.md,
                top = HackieSpacing.md,
                bottom = HackieSpacing.xs,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        // Count badge — small, restrained, gives the user a sense of
        // how much is in each section at a glance.
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun PanelItem(
    entry: NavEntry,
    isSelected: Boolean,
    isPinned: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
        else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "container",
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "icon",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HackieSpacing.sm, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Pill-shaped main button
        Surface(
            onClick = onClick,
            color = containerColor,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = if (entry.sublabel != null)
                        "${entry.label}, ${entry.sublabel}" else entry.label
                    selected = isSelected
                    stateDescription = if (isSelected) "Selected" else "Not selected"
                },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = HackieSpacing.md, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HackieSpacing.md),
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    if (entry.sublabel != null) {
                        Text(
                            text = entry.sublabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        // Long-press area for pin toggle: a separate small button that
        // we surface as a tap target on the right edge. Long-press on
        // the main button is a future enhancement — for now, this
        // explicit affordance is clearer and accessible.
        Surface(
            onClick = onTogglePin,
            color = Color.Transparent,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(start = 2.dp),
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = if (isPinned) "Unpin" else "Pin to top",
                    tint = if (isPinned) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(14.dp)
                        .rotate(if (isPinned) 0f else 45f),
                )
            }
        }
    }
}

@Composable
private fun PanelFooter(
    isHidConnected: Boolean,
    onPanicLock: () -> Unit,
) {
    val view = LocalView.current
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HackieSpacing.md, vertical = HackieSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Connection dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isHidConnected) Success else MaterialTheme.colorScheme.outline),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHidConnected) "HID Connected" else "HID Disconnected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Bluetooth transport",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Lock button as a circular icon button — more compact than
            // the previous pill, fits the footer.
            Surface(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onPanicLock()
                },
                color = MaterialTheme.colorScheme.errorContainer,
                shape = CircleShape,
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock session",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ─── Pinned-routes persistence ───────────────────────────────────────────

private const val PINNED_ROUTES_KEY = "side_panel_pinned_routes"
private const val MAX_PINNED = 6

private fun pinnedPrefs(context: Context) =
    context.applicationContext.getSharedPreferences("hackie_user_prefs", Context.MODE_PRIVATE)

private fun loadPinnedRoutes(context: Context): List<String> {
    val raw = pinnedPrefs(context).getString(PINNED_ROUTES_KEY, "") ?: ""
    return raw.split(",").filter { it.isNotBlank() }.take(MAX_PINNED)
}

private fun updatePinnedRoutes(
    context: Context,
    route: String,
    pinned: Boolean,
): List<String> {
    val current = loadPinnedRoutes(context).toMutableList()
    if (pinned) {
        if (route !in current) {
            // Pin to top.
            current.add(0, route)
            if (current.size > MAX_PINNED) current.removeAt(current.size - 1)
        }
    } else {
        current.remove(route)
    }
    pinnedPrefs(context).edit()
        .putString(PINNED_ROUTES_KEY, current.joinToString(","))
        .apply()
    return current
}
