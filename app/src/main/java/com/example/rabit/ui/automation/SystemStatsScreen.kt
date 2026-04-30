package com.example.rabit.ui.automation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.helper.HelperViewModel
import com.example.rabit.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SystemStatsContent(
    viewModel: HelperViewModel
) {
    val stats by viewModel.systemStats.collectAsState()
    val connected by viewModel.sshConnected.collectAsState()

    LaunchedEffect(connected) {
        while (connected) {
            viewModel.fetchSystemStats()
            delay(5000) // Refresh every 5s
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "System Monitor",
            color = Platinum,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Text(
            if (connected) "Live tracking active" else "Connect SSH to monitor",
            color = if (connected) AccentBlue else Silver,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (connected) {
            StatGauge(
                label = "CPU Load",
                value = stats.cpuLoad,
                max = 4.0f, // Assuming 4 core load reference
                suffix = " load"
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            StatGauge(
                label = "Memory Pressure",
                value = stats.memUsage,
                max = 100f,
                suffix = "%"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF162031)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = AccentBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("System Uptime", color = Silver, fontSize = 12.sp)
                        Text(stats.uptime, color = Platinum, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Silver, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No metrics available", color = Silver)
                }
            }
        }
    }
}


@Composable
fun StatGauge(label: String, value: Float, max: Float, suffix: String) {
    val progress = (value / max).coerceIn(0f, 1f)
    val color = when {
        progress > 0.8f -> Color.Red
        progress > 0.5f -> Color(0xFFFFA500)
        else -> AccentBlue
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Platinum, fontSize = 14.sp)
            Text("${String.format("%.1f", value)}$suffix", color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = color,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
    }
}
