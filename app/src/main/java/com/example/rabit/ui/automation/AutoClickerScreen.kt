package com.example.rabit.ui.automation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoClickerScreen(
    viewModel: AutomationViewModel,
    onBack: () -> Unit
) {
    val isRunning by viewModel.isAutoClicking.collectAsState()
    val interval by viewModel.autoClickInterval.collectAsState()
    val loops by viewModel.autoClickLoops.collectAsState()
    val currentCount by viewModel.currentClickCount.collectAsState()

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            TopAppBar(
                title = { Text("AUTO-CLICKER", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Status Card
            StatusDisplay(isRunning, currentCount, loops)

            // Configuration Section
            ConfigCard(
                label = "Interval",
                value = "${interval}ms",
                icon = Icons.Default.Timer,
                color = AccentTeal
            ) {
                Slider(
                    value = interval.toFloat(),
                    onValueChange = { viewModel.setAutoClickInterval(it.toLong()) },
                    valueRange = 100f..5000f,
                    steps = 49,
                    colors = SliderDefaults.colors(thumbColor = AccentTeal, activeTrackColor = AccentTeal)
                )
            }

            ConfigCard(
                label = "Loop Count",
                value = if (loops == 0) "Infinite" else "$loops Clicks",
                icon = Icons.Default.Loop,
                color = AccentBlue
            ) {
                Slider(
                    value = loops.toFloat(),
                    onValueChange = { viewModel.setAutoClickLoops(it.toInt()) },
                    valueRange = 0f..1000f,
                    steps = 20,
                    colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Button
            StartStopButton(isRunning) {
                if (isRunning) viewModel.stopAutoClicker() else viewModel.startAutoClicker()
            }
        }
    }
}

@Composable
private fun StatusDisplay(isRunning: Boolean, count: Int, total: Int) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isRunning) 1.2f else 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )

    Surface(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Pulsing ring
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) SuccessGreen.copy(alpha = 0.1f) else Silver.copy(alpha = 0.05f))
                        .then(if (isRunning) Modifier.padding(8.dp) else Modifier)
                )
                
                Text(
                    "$count",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isRunning) SuccessGreen else Platinum
                )
            }
            
            Text(
                if (total == 0) "CLICKING FOREVER" else "TARGET: $total",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Silver.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun ConfigCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Platinum, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        content()
    }
}

@Composable
private fun StartStopButton(isRunning: Boolean, onClick: () -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    Button(
        onClick = { 
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onClick() 
        },
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRunning) Color.Red.copy(alpha = 0.2f) else SuccessGreen.copy(alpha = 0.2f)
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, if (isRunning) Color.Red else SuccessGreen)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                null, 
                tint = if (isRunning) Color.Red else SuccessGreen
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                if (isRunning) "STOP AUTO-CLICKER" else "START MACRO",
                color = if (isRunning) Color.Red else SuccessGreen,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}
