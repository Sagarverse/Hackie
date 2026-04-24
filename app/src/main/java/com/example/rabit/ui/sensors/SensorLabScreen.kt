package com.example.rabit.ui.sensors

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorLabScreen(
    viewModel: SensorLabViewModel,
    onBack: () -> Unit
) {
    val accel by viewModel.accelReading.collectAsState()
    val mag by viewModel.magReading.collectAsState()
    val interpretation by viewModel.aiInterpretation.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    DisposableEffect(Unit) {
        viewModel.startSurveillance()
        onDispose { viewModel.stopSurveillance() }
    }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SIDE-CHANNEL SENSOR LAB", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("ACOUSTIC & EMF SIGNAL INTELLIGENCE", color = SuccessGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Oscilloscopes
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SensorGraph("ACCELEROMETER (VIBE)", accel, SuccessGreen, Modifier.weight(1f))
                SensorGraph("MAGNETOMETER (EMF)", mag, Color.Cyan, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // AI Interpretation Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.weight(1.5f).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("NEURAL INTERPRETATION", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Text(
                        text = interpretation,
                        color = Platinum,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Pulse Animation
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(8.dp).background(if (isAnalyzing) SuccessGreen else Color.Gray, RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(8.dp))
                Text(if (isAnalyzing) "LIVE SURVEILLANCE ACTIVE" else "SENSORS STANDBY", color = if (isAnalyzing) SuccessGreen else Silver, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun SensorGraph(label: String, data: SensorReading, color: Color, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, color = Silver, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxSize().background(Color.Black, RoundedCornerShape(8.dp)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))) {
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                val midY = size.height / 2
                drawLine(color.copy(alpha = 0.1f), Offset(0f, midY), Offset(size.width, midY))
                
                // Simple visualization of the magnitude
                val magnitude = kotlin.math.sqrt(data.x * data.x + data.y * data.y + data.z * data.z)
                val normalizedMag = (magnitude % 20f) / 20f
                
                drawCircle(color, radius = 4.dp.toPx() * normalizedMag, center = Offset(size.width / 2, midY))
            }
        }
    }
}
