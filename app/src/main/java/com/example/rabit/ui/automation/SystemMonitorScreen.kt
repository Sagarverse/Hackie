package com.example.rabit.ui.automation

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.rabit.ui.helper.HelperViewModel
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemMonitorScreen(
    helperViewModel: HelperViewModel,
    initialSubFeature: String = "processes",
    onBack: () -> Unit
) {
    var currentSubFeature by remember { mutableStateOf(initialSubFeature) }
    val accentColor = AccentBlue
    val bgColor = Graphite

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (currentSubFeature == "processes") "PROCESS MANAGER" else "SYSTEM STATS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = accentColor
                            )
                        )
                        Text(
                            "SYSTEM MONITOR",
                            color = accentColor.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                when (currentSubFeature) {
                    "processes" -> {
                        ProcessManagerContent(helperViewModel)
                    }
                    "stats" -> {
                        SystemStatsContent(helperViewModel)
                    }
                }
            }

            // Right-side Mini Sidebar
            Surface(
                color = Color.White.copy(alpha = 0.03f),
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    MiniSidebarIcon(
                        icon = Icons.Default.Memory,
                        isSelected = currentSubFeature == "processes",
                        accentColor = accentColor,
                        onClick = { currentSubFeature = "processes" }
                    )
                    MiniSidebarIcon(
                        icon = Icons.Default.Speed,
                        isSelected = currentSubFeature == "stats",
                        accentColor = accentColor,
                        onClick = { currentSubFeature = "stats" }
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniSidebarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                null,
                tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .background(accentColor, CircleShape)
                )
            }
        }
    }
}
