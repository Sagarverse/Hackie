package com.example.rabit.ui.automation

import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rabit.data.adb.AdbMirrorStreamer
import com.example.rabit.ui.MainViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.rabit.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.example.rabit.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdbMirrorScreen(
    viewModel: AutomationViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adbClient = viewModel.adbClient
    
    if (adbClient == null) {
        Box(modifier = Modifier.fillMaxSize().background(Obsidian), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.WifiOff, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Device Not Connected", color = Color.White)
                Button(onClick = onBack) { Text("Go Back") }
            }
        }
        return
    }

    val streamer = remember { AdbMirrorStreamer(adbClient, scope) }
    
    var deviceWidth by remember { mutableStateOf(1080) }
    var deviceHeight by remember { mutableStateOf(2400) }
    var isMirroring by remember { mutableStateOf(false) }
    val isUsb = remember { com.example.rabit.data.storage.RemoteStorageManager.adbIp.isBlank() }
    
    var startX by remember { mutableStateOf(0f) }
    var startY by remember { mutableStateOf(0f) }
    var startTime by remember { mutableStateOf(0L) }

    // Fetch device resolution on entry
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val sizeStr = adbClient.executeCommand("wm size")
                // Physical size: 1080x2400
                val regex = "Physical size: (\\d+)x(\\d+)".toRegex()
                regex.find(sizeStr)?.let {
                    deviceWidth = it.groupValues[1].toInt()
                    deviceHeight = it.groupValues[2].toInt()
                }
            } catch (e: Exception) {
                // Fallback to defaults
            }
        }
    }

    var showMirrorOverlay by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            TopAppBar(
                title = { Text("ADB Mirror Center", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- Tactical Briefing ---
            var showBriefing by remember { mutableStateOf(true) }
            if (showBriefing) {
                Surface(
                    color = AccentBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("MIRRORING BRIEFING", color = AccentBlue, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { showBriefing = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Silver, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This window lets you see and touch the target phone from your own screen. Perfect for checking files or controlling apps remotely.",
                            color = Platinum, fontSize = 13.sp, lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("HOW TO CONNECT:", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("• 1. Use a high-quality USB cable for the best speed.", color = Silver, fontSize = 12.sp)
                        Text("• 2. Ensure 'USB Debugging' is ON in the target's settings.", color = Silver, fontSize = 12.sp)
                        Text("• 3. Hit 'LAUNCH' below to open the separate window.", color = Silver, fontSize = 12.sp)
                    }
                }
            }

            Icon(Icons.Default.Cast, null, tint = AccentBlue, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(24.dp))
            Text("Ready for Neural Mirroring", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
            Text("Device: ${if (isUsb) "USB Connected" else "Wireless ($deviceWidth x $deviceHeight)"}", color = TextSecondary)
            
            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = { showMirrorOverlay = true },
                modifier = Modifier.fillMaxWidth(0.7f),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("LAUNCH MIRROR SESSION")
            }
        }
    }

    if (showMirrorOverlay) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showMirrorOverlay = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                var surfaceView by remember { mutableStateOf<SurfaceView?>(null) }
                
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            surfaceView = this
                            holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    isMirroring = true
                                    val bitrate = if (isUsb) 8_000_000 else 2_000_000
                                    streamer.startMirroring(holder.surface, deviceWidth / 2, deviceHeight / 2, bitrate)
                                }
                                override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}
                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    isMirroring = false
                                    streamer.stopMirroring()
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize().pointerInteropFilter { event ->
                        val view = surfaceView ?: return@pointerInteropFilter false
                        val x = (event.x / view.width) * deviceWidth
                        val y = (event.y / view.height) * deviceHeight
                        
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                startX = x
                                startY = y
                                startTime = System.currentTimeMillis()
                                true
                            }
                            MotionEvent.ACTION_UP -> {
                                val duration = System.currentTimeMillis() - startTime
                                val dist = Math.sqrt(Math.pow((x - startX).toDouble(), 2.0) + Math.pow((y - startY).toDouble(), 2.0))
                                
                                scope.launch(Dispatchers.IO) {
                                    if (dist < 20 && duration < 300) {
                                        adbClient.executeCommand("input tap ${x.toInt()} ${y.toInt()}")
                                    } else {
                                        adbClient.executeCommand("input swipe ${startX.toInt()} ${startY.toInt()} ${x.toInt()} ${y.toInt()} ${duration.coerceAtMost(1000)}")
                                    }
                                }
                                true
                            }
                            MotionEvent.ACTION_MOVE -> true
                            else -> false
                        }
                    }
                )

                // Tactical Controls Overlay
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { showMirrorOverlay = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                        ) {
                            Icon(Icons.Default.ExitToApp, null)
                            Spacer(Modifier.width(8.dp))
                            Text("EXIT SESSION")
                        }
                    }
                    
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 32.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            IconButton(onClick = { scope.launch { adbClient.executeCommand("input keyevent 4") } }) {
                                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                            }
                            IconButton(onClick = { scope.launch { adbClient.executeCommand("input keyevent 3") } }) {
                                Icon(Icons.Default.Home, "Home", tint = Color.White)
                            }
                            IconButton(onClick = { scope.launch { adbClient.executeCommand("input keyevent 187") } }) {
                                Icon(Icons.Default.Menu, "Recents", tint = Color.White)
                            }
                        }
                    }
                }

                if (!isMirroring) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentBlue)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            streamer.stopMirroring()
        }
    }
}


