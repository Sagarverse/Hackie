package com.example.rabit.ui.automation

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.rabit.ui.theme.*
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionLabScreen(
    viewModel: AutomationViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val uiState by viewModel.aiGenerationState.collectAsState()
    
    val accentColor = Color(0xFF00F2FF)

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "AI VISION LAB",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text("NEURAL UI ANALYSIS", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black.copy(alpha = 0.5f))
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // --- Camera Preview ---
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("VisionLab", "Camera binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // --- Tactical Overlay ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, accentColor.copy(alpha = 0.2f))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                            radius = 1000f
                        )
                    )
            ) {
                // Corner Accents
                TacticalCorners(accentColor)
                
                // Scanning Line Animation
                if (uiState is AutomationViewModel.AiGenerationState.Generating) {
                    ScanningLine(accentColor)
                }
            }

            // --- Capture Action ---
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState is AutomationViewModel.AiGenerationState.Success) {
                    val payload = (uiState as AutomationViewModel.AiGenerationState.Success).payload
                    VisionResultCard(
                        payload = payload, 
                        onDismiss = { viewModel.resetAiGeneration() },
                        onInject = { viewModel.executeDuckyScript(payload) }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.1f))
                            .border(2.dp, accentColor, CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(if (uiState is AutomationViewModel.AiGenerationState.Generating) Color.Gray else accentColor)
                            .clickable(enabled = uiState !is AutomationViewModel.AiGenerationState.Generating) {
                                val capture = imageCapture ?: return@clickable
                                capture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bitmap = image.toBitmap()
                                            if (bitmap != null) {
                                                viewModel.analyzeUiVision(bitmap)
                                            }
                                            image.close()
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e("VisionLab", "Capture failed", exception)
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState is AutomationViewModel.AiGenerationState.Generating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Adjust, null, tint = Color.Black, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "SCAN INTERFACE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }
            
            // --- Error Display ---
            if (uiState is AutomationViewModel.AiGenerationState.Error) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp).padding(horizontal = 24.dp),
                    color = Color.Red.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        (uiState as AutomationViewModel.AiGenerationState.Error).message,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TacticalCorners(color: Color) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Box(modifier = Modifier.align(Alignment.TopStart).size(30.dp).border(2.dp, color, RoundedCornerShape(topStart = 8.dp)))
        Box(modifier = Modifier.align(Alignment.TopEnd).size(30.dp).border(2.dp, color, RoundedCornerShape(topEnd = 8.dp)))
        Box(modifier = Modifier.align(Alignment.BottomStart).size(30.dp).border(2.dp, color, RoundedCornerShape(bottomStart = 8.dp)))
        Box(modifier = Modifier.align(Alignment.BottomEnd).size(30.dp).border(2.dp, color, RoundedCornerShape(bottomEnd = 8.dp)))
    }
}

@Composable
fun ScanningLine(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "yOffset"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val y = maxHeight * yOffset
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .offset(y = y)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0f), color, color.copy(alpha = 0f))
                    )
                )
        )
    }
}

@Composable
fun VisionResultCard(payload: String, onDismiss: () -> Unit, onInject: () -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Graphite.copy(alpha = 0.9f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF00F2FF), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("NEURAL PAYLOAD READY", color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = Silver, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    payload,
                    color = SuccessGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onInject,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F2FF), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FlashOn, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("STRIKE / INJECT", fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun ImageProxy.toBitmap(): Bitmap? {
    val planeProxy = planes[0] ?: return null
    val buffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    
    val matrix = Matrix()
    matrix.postRotate(imageInfo.rotationDegrees.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
