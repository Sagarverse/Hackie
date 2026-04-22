package com.example.rabit.ui.automation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rabit.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotLabScreen(
    viewModel: com.example.rabit.ui.webbridge.WebBridgeViewModel,
    onBack: () -> Unit
) {
    val screenshots by viewModel.screenshots.collectAsState()
    var selectedScreenshot by remember { mutableStateOf<File?>(null) }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "SCREENSHOT LAB",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text("DARK EXFILTRATION VAULT", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Platinum)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Refresh logic in VM */ }) {
                        Icon(Icons.Default.Refresh, null, tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (screenshots.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.HideImage, 
                        contentDescription = null, 
                        tint = Silver.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("VAULT IS EMPTY", color = Silver.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Exfiltrated images will appear here", color = Silver.copy(alpha = 0.3f), fontSize = 11.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(screenshots) { file ->
                        ScreenshotCard(file = file, onClick = { selectedScreenshot = file })
                    }
                }
            }

            // --- Floating Capture Action ---
            FloatingActionButton(
                onClick = { 
                    // Trigger HID screenshot and exfil chain
                    viewModel.sendHidCommand("GUI SHIFT 3") // Mac full screenshot
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = AccentBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Camera, contentDescription = "Capture")
            }

            // --- Full Screen Preview Overlay ---
            if (selectedScreenshot != null) {
                FullScreenPreview(
                    file = selectedScreenshot!!,
                    onDismiss = { selectedScreenshot = null }
                )
            }
        }
    }
}

@Composable
private fun ScreenshotCard(file: File, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f/9f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Graphite,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        AsyncImage(
            model = file,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun FullScreenPreview(file: File, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(onClick = onDismiss)
    ) {
        AsyncImage(
            model = file,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentScale = ContentScale.Fit
        )
        
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
        }
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(file.name, color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "${file.length() / 1024} KB • Exfiltrated P2P", 
                color = Silver.copy(alpha = 0.5f), 
                fontSize = 10.sp
            )
        }
    }
}
