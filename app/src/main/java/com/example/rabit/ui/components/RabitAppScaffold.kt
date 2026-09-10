package com.example.rabit.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.example.rabit.data.prefs.UserPreferences
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import kotlin.math.roundToInt

val LocalOpenGlobalDrawer = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * The set of routes that the navigation graph actually resolves. Used by
 * the drawer to show a Snackbar instead of silently navigating to a
 * dead screen. Keep this in sync with MainActivity's NavHost.
 */
private val ValidRoutes: Set<String> = setOf(
    "home", "pairing", "main", "keyboard",
    "web_bridge", "automation", "airplay_receiver", "ssh_terminal",
    "assistant", "lockdown", "network_auditor", "hid_brute_force",
    "adb_mirror", "injector", "code_typer", "auto_clicker",
    "remote_explorer", "reverse_shell", "c2_tunnel", "remote_desktop",
    "terminal_scanner", "screenshot_lab", "local_terminal", "ghost_recon",
    "neural_lab", "phish_portal", "loot_viewer", "crypto_toolkit",
    "pentest_toolkit", "payload_forge", "security_auditor",
    "traffic_analyzer", "web_sniper", "keystroke_monitor", "vision_lab",
    "macro_lab", "forensics_lab", "settings", "helper", "adb_manager",
    "profile", "snippets", "global_search", "browser",
    "macro_orchestrator", "process_manager", "system_stats",
    "onboarding", "decoy", "wake_on_lan", "password_manager",
)

/**
 * Hackie global scaffold.
 *
 * - `ModalNavigationDrawer` on phones, `PermanentNavigationDrawer` on
 *   tablets / wide screens (>= 600dp). The single source of truth for
 *   the side panel UI (see [SidePanelBody]).
 * - `topBar = false` because every screen uses its own `ScreenScaffold`.
 *   The drawer here is purely navigation chrome.
 * - Dead-route guard: a Snackbar at the bottom of the drawer if the
 *   user taps a route that doesn't exist in the NavHost.
 * - The new side panel body lives in [SidePanelBody] and is shared by
 *   both the modal and permanent drawer variants.
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
    isHidConnected: Boolean = false,
    onBack: (() -> Unit)? = null,
    topBarActions: @Composable RowScope.() -> Unit = {},
    onPanicLock: (() -> Unit)? = null,
    onEngageDecoy: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    var showFad by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isWide = configuration.screenWidthDp >= 600
    val view = LocalView.current

    // Snackbar message for unknown routes. We capture the message in a
    // state so the snackbar can be triggered from the navigation lambda.
    val unknownRouteMessage = "This screen is not available yet."

    fun dispatchNavigate(route: String) {
        if (ValidRoutes.contains(route)) {
            onNavigate(route)
        } else {
            scope.launch { snackbarHostState.showSnackbar(unknownRouteMessage) }
        }
    }

    fun closeDrawer() {
        scope.launch { drawerState.close() }
    }

    fun navigateAndClose(route: String) {
        dispatchNavigate(route)
        closeDrawer()
    }

    val drawerContent: @Composable () -> Unit = {
        AdvancedSidePanel(
            currentRoute = currentRoute,
            isHidConnected = isHidConnected,
            callbacks = SidePanelCallbacks(
                onNavigate = ::navigateAndClose,
                onPanicLock = {
                    closeDrawer()
                    onPanicLock?.invoke()
                },
                onEngageDecoy = {
                    closeDrawer()
                    onEngageDecoy()
                },
                onRunScan = {
                    dispatchNavigate("home")
                    closeDrawer()
                },
                onOpenSnippets = {
                    dispatchNavigate("snippets")
                    closeDrawer()
                },
            ),
            featureWebBridgeVisible = featureWebBridgeVisible,
            featureAutomationVisible = featureAutomationVisible,
            featureAssistantVisible = featureAssistantVisible,
            featureSnippetsVisible = featureSnippetsVisible,
            featureSshTerminalVisible = featureSshTerminalVisible,
        )
    }
    if (isWide) {
        PermanentNavigationDrawer(
            modifier = Modifier.fillMaxHeight(),
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = Modifier.width(304.dp).fillMaxHeight(),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        drawerContent()
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                        ) { data -> Snackbar(snackbarData = data) }
                    }
                }
            },
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    CompositionLocalProvider(LocalOpenGlobalDrawer provides openDrawer) {
                        content(PaddingValues(0.dp))
                    }
                    
                    FloatingActionDashboard(
                        isVisible = showFad,
                        onDismiss = { showFad = false },
                        onAction = { action ->
                            showFad = false
                        }
                    )
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        DraggableFadButton(
                            onClick = { 
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                showFad = true 
                            }
                        )
                    }
                }
            }
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight(),
                    drawerShape = RoundedCornerShape(
                        topEnd = 24.dp, bottomEnd = 24.dp,
                    ),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        drawerContent()
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                        ) { data -> Snackbar(snackbarData = data) }
                    }
                }
            },
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    CompositionLocalProvider(LocalOpenGlobalDrawer provides openDrawer) {
                        content(PaddingValues(0.dp))
                    }
                    
                    FloatingActionDashboard(
                        isVisible = showFad,
                        onDismiss = { showFad = false },
                        onAction = { action ->
                            showFad = false
                        }
                    )
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        DraggableFadButton(
                            onClick = { 
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                showFad = true 
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableFadButton(onClick: () -> Unit) {
    var offsetX by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }
    var offsetY by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }
    
    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        androidx.compose.material3.FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Dashboard,
                contentDescription = "Open FAD"
            )
        }
    }
}
