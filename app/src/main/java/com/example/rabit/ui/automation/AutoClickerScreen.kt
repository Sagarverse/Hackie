package com.example.rabit.ui.automation

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoClickerScreen(
    viewModel: AutomationViewModel,
    onBack: () -> Unit
) {
    val isRunning by viewModel.isAutoClicking.collectAsState()
    val interval by viewModel.autoClickInterval.collectAsState()
    val loops by viewModel.autoClickLoops.collectAsState()
    val unit by viewModel.autoClickUnit.collectAsState()
    val currentCount by viewModel.currentClickCount.collectAsState()

    var intervalText by remember(interval) { mutableStateOf(interval.toString()) }
    var loopsText by remember(loops) { mutableStateOf(loops.toString()) }

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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Status Card
            StatusDisplay(isRunning, currentCount, loops)

            // Configuration Section
            PremiumGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("CONFIGURATION", color = Silver.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = intervalText,
                            onValueChange = { 
                                intervalText = it
                                it.toLongOrNull()?.let { v -> viewModel.setAutoClickInterval(v) }
                            },
                            label = { Text("Interval") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Platinum,
                                unfocusedTextColor = Platinum,
                                focusedBorderColor = AccentTeal,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                        
                        // Unit Selector
                        Row(
                            modifier = Modifier
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Surface3)
                                .border(0.5.dp, BorderColor, RoundedCornerShape(8.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AutomationViewModel.ClickTimeUnit.values().forEach { u ->
                                val isSelected = u == unit
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(50.dp)
                                        .background(if (isSelected) AccentTeal else Color.Transparent)
                                        .clickable { viewModel.setAutoClickUnit(u) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        u.name,
                                        color = if (isSelected) Obsidian else Silver,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = loopsText,
                        onValueChange = { 
                            loopsText = it
                            it.toIntOrNull()?.let { v -> viewModel.setAutoClickLoops(v) }
                        },
                        label = { Text("Loop Count (0 for Infinite)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    // More Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Session Control", color = Silver.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = { viewModel.stopAutoClicker(); loopsText = "0"; viewModel.setAutoClickLoops(0) },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Loops", fontSize = 11.sp)
                        }
                    }
                }
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
