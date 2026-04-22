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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rabit.data.adb.AdbMirrorStreamer
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
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

    Scaffold(
        containerColor = Obsidian,
        bottomBar = {
            Surface(color = Color.Black.copy(alpha = 0.8f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
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
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
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
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInteropFilter { event ->
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

            if (!isMirroring) {
                CircularProgressIndicator(color = AccentBlue)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            streamer.stopMirroring()
        }
    }
}

private annotation class ExperimentalComposeUiApi
