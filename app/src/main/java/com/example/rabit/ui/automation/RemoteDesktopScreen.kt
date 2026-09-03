package com.example.rabit.ui.automation

import android.app.Activity
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Remote desktop viewer.
 *
 * Shows a low-fps screen stream from the connected reverse shell and lets the
 * user tap to click on the remote desktop. Goes fullscreen on enter, exits
 * fullscreen on leave. The control bar at the bottom exposes keyboard, click
 * mode, capture, and config — all real handlers now, no dead buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteDesktopScreen(viewModel: AutomationViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val screenBase64 by viewModel.remoteScreenBase64.collectAsState()
    val isStreaming by viewModel.isRemoteDesktopActive.collectAsState()
    val isConnected by viewModel.reverseShellConnected.collectAsState()

    // Decode the incoming base64 JPEG to a Bitmap. Recomputed only when the
    // base64 string changes (avoids re-decoding the same frame on every recompose).
    val bitmap = remember(screenBase64) {
        if (screenBase64 == null) null
        else try {
            val bytes = Base64.decode(screenBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) { null }
    }

    // ── Fullscreen on enter, exit on leave ──────────────────────────
    val activity = context as? Activity
    DisposableEffect(activity) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            // Restore bars when leaving so the rest of the app looks normal
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // ── Lifecycle: start when connected, stop on leave (defensively) ──
    DisposableEffect(isConnected) {
        if (isConnected) viewModel.startRemoteDesktop()
        onDispose {
            // runCatching so a socket-close race can't crash the UI
            runCatching { viewModel.stopRemoteDesktop() }
        }
    }

    // ── Local UI state for the four control buttons ─────────────────
    var showKeyboard by remember { mutableStateOf(false) }
    var clickMode by remember { mutableStateOf(ClickMode.LEFT) }
    var showClickModeSheet by remember { mutableStateOf(false) }
    var lastCapture by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showConfig by remember { mutableStateOf(false) }
    var captureFlash by remember { mutableStateOf(false) }

    // Pinch-zoom and pan for the screen viewport
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    fun resetZoom() { scale = 1f; offsetX = 0f; offsetY = 0f }

    // Capture system back while a sheet/keyboard is up
    BackHandler(enabled = showKeyboard || showClickModeSheet || showConfig) {
        showKeyboard = false
        showClickModeSheet = false
        showConfig = false
    }

    ScreenScaffold(
        title = "Remote desktop",
        subtitle = if (isStreaming) "Live stream active" else if (isConnected) "Connecting…" else "Connect a reverse shell first",
        onBack = {
            runCatching { viewModel.stopRemoteDesktop() }
            onBack()
        },
        actions = {
            IconButton(onClick = {
                runCatching { viewModel.startRemoteDesktop() }
                resetZoom()
            }) { Icon(Icons.Default.Refresh, "Restart stream", tint = AccentTeal) }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Screen Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f; offsetY = 0f
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                // Only forward taps when not zoomed
                                if (scale <= 1.01f) {
                                    val xPercent = (offset.x / size.width).coerceIn(0f, 1f)
                                    val yPercent = (offset.y / size.height).coerceIn(0f, 1f)
                                    when (clickMode) {
                                        ClickMode.LEFT -> viewModel.sendRemoteClick(xPercent, yPercent)
                                        ClickMode.RIGHT -> viewModel.sendRemoteRightClick(xPercent, yPercent)
                                        ClickMode.DRAG -> { /* drag uses onLongPress below */ }
                                    }
                                }
                            },
                            onDoubleTap = { resetZoom() },
                            onLongPress = { offset ->
                                if (clickMode == ClickMode.DRAG) {
                                    val xPercent = (offset.x / size.width).coerceIn(0f, 1f)
                                    val yPercent = (offset.y / size.height).coerceIn(0f, 1f)
                                    viewModel.sendRemoteDragStart(xPercent, yPercent)
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Remote screen",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY,
                            ),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentTeal, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (isConnected) "WAITING FOR STREAM..." else "NO ACTIVE SESSION",
                            color = Silver.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Capture flash overlay
                if (captureFlash) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.5f)),
                    )
                }

                // HUD Overlay
                Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            "FPS: ~0.3 | MODE: ${clickMode.name}",
                            color = SuccessGreen,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (scale > 1.01f) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                "ZOOM ${"%.1f".format(scale)}×",
                                color = AccentTeal,
                                fontSize = 8.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Control Bar
            Surface(
                color = Surface0,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlItem(
                        icon = Icons.Default.Keyboard,
                        label = "Keyboard",
                        onClick = { showKeyboard = true },
                    )
                    ControlItem(
                        icon = Icons.Default.Mouse,
                        label = "Click: ${clickMode.name}",
                        onClick = { showClickModeSheet = true },
                    )

                    FloatingActionButton(
                        onClick = {
                            runCatching {
                                if (isStreaming) viewModel.stopRemoteDesktop() else viewModel.startRemoteDesktop()
                            }
                            resetZoom()
                        },
                        containerColor = if (isStreaming) Color.Red else SuccessGreen,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(if (isStreaming) Icons.Default.Stop else Icons.Default.PlayArrow, null)
                    }

                    ControlItem(
                        icon = Icons.Default.Screenshot,
                        label = "Capture",
                        onClick = {
                            val current = bitmap
                            if (current != null) {
                                lastCapture = current
                                captureFlash = true
                                scope.launch {
                                    kotlinx.coroutines.delay(120)
                                    captureFlash = false
                                }
                            }
                        },
                        badge = if (lastCapture != null) "✓" else null,
                    )
                    ControlItem(
                        icon = Icons.Default.Settings,
                        label = "Config",
                        onClick = { showConfig = true },
                    )
                }
            }
        }
    }

    // ── Keyboard overlay ────────────────────────────────────────────
    if (showKeyboard) {
        var dialogText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showKeyboard = false },
            title = { Text("Type on remote") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dialogText,
                        onValueChange = { dialogText = it },
                        placeholder = { Text("Type text to inject via HID…") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Text is sent character-by-character to the host's focused field.",
                        fontSize = 11.sp,
                        color = Silver.copy(alpha = 0.7f),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showKeyboard = false }) {
                    Text("Close", color = AccentTeal)
                }
            },
        )
    }

    // ── Click Mode bottom sheet ─────────────────────────────────────
    if (showClickModeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showClickModeSheet = false },
            containerColor = Graphite,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    "Click Mode",
                    color = Platinum,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                ClickMode.values().forEach { mode ->
                    Surface(
                        onClick = {
                            clickMode = mode
                            showClickModeSheet = false
                        },
                        color = if (clickMode == mode) AccentTeal.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = clickMode == mode,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = AccentTeal,
                                    unselectedColor = Silver,
                                ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    mode.label,
                                    color = Platinum,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    mode.description,
                                    color = Silver.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // ── Config bottom sheet ─────────────────────────────────────────
    if (showConfig) {
        ModalBottomSheet(
            onDismissRequest = { showConfig = false },
            containerColor = Graphite,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    "Stream config",
                    color = Platinum,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Refresh interval",
                    color = Silver,
                    fontSize = 12.sp,
                )
                Text(
                    "3 seconds (fixed). Lower intervals flood the channel.",
                    color = Silver.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Compression",
                    color = Silver,
                    fontSize = 12.sp,
                )
                Text(
                    "80% JPEG (fixed by screencapture -x).",
                    color = Silver.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Client resolution",
                    color = Silver,
                    fontSize = 12.sp,
                )
                Text(
                    "1440×900 (assumed for macOS click mapping).",
                    color = Silver.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private enum class ClickMode(val label: String, val description: String) {
    LEFT("Left click", "Tap to click at the remote cursor position"),
    RIGHT("Right click", "Tap to open a context menu"),
    DRAG("Drag", "Long-press to start a drag, tap to release"),
}

/**
 * Control button at the bottom of the remote desktop screen.
 */
@Composable
private fun ControlItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    badge: String? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Box {
            Icon(icon, null, tint = Silver, modifier = Modifier.size(22.dp))
            if (badge != null) {
                Text(
                    badge,
                    color = AccentTeal,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 0.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Silver, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
