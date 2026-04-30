package com.example.rabit.ui.security

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashCrackerScreen(viewModel: HashCrackerViewModel, apiKey: String, onBack: () -> Unit) {
    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "HASH CRACKER",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Platinum
                            )
                        )
                        Text("LOCAL DICTIONARY & NEURAL LOOKUP", color = Color(0xFFEAB308), fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
        Box(modifier = Modifier.padding(padding)) {
            HashCrackerContent(viewModel, apiKey)
        }
    }
}

@Composable
fun HashCrackerContent(viewModel: HashCrackerViewModel, apiKey: String) {
    val state by viewModel.crackerState.collectAsState()
    var hashInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        when (val s = state) {
            is HashCrackerState.Idle -> {
                Text(
                    "Paste a cryptographic hash below. The system will auto-detect the algorithm (MD5, SHA-1, SHA-256) and attempt to recover the plaintext password using an onboard dictionary, falling back to a Neural Lookup if configured.",
                    color = Silver,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = hashInput,
                    onValueChange = { hashInput = it.trim() },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    label = { Text("Target Hash", color = Silver) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Platinum,
                        unfocusedTextColor = Platinum,
                        focusedBorderColor = Color(0xFFEAB308),
                        unfocusedBorderColor = BorderColor
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                )

                val hashType = viewModel.determineHashType(hashInput)
                if (hashInput.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (hashType != "Unknown") Icons.Default.CheckCircle else Icons.Default.Warning,
                            null,
                            tint = if (hashType != "Unknown") SuccessGreen else Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Algorithm: $hashType",
                            color = if (hashType != "Unknown") SuccessGreen else Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.startCracking(hashInput, apiKey) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308), disabledContainerColor = Color.Gray),
                    shape = RoundedCornerShape(12.dp),
                    enabled = hashInput.isNotBlank() && hashType != "Unknown"
                ) {
                    Icon(Icons.Default.VpnKey, null, tint = Obsidian)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("INITIATE CRACK", fontWeight = FontWeight.Black, color = Obsidian)
                }
            }
            is HashCrackerState.Cracking -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { s.progress },
                            modifier = Modifier.size(200.dp),
                            color = Color(0xFFEAB308),
                            strokeWidth = 8.dp,
                            trackColor = Color.White.copy(alpha = 0.05f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ALGORITHM", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(s.hashType, color = Color(0xFFEAB308), fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Text("TESTING PAYLOAD", color = Silver.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(s.currentGuess, color = Platinum, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                    Spacer(modifier = Modifier.height(24.dp))

                    LinearProgressIndicator(
                        progress = { s.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = Color(0xFFEAB308)
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    TextButton(onClick = { viewModel.stopCracking() }) {
                        Text("ABORT", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
            is HashCrackerState.Success -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.LockOpen, null, tint = SuccessGreen, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("HASH CRACKED", color = SuccessGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("Method: ${s.method}", color = Silver, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(40.dp))

                    Surface(
                        color = SuccessGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RECOVERED PLAINTEXT", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(s.plaintext, color = SuccessGreen, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = { viewModel.reset(); hashInput = "" },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("NEW HASH", fontWeight = FontWeight.Black, color = Obsidian)
                    }
                }
            }
            is HashCrackerState.Failed -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("CRACK FAILED", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(s.reason, color = Silver, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = { viewModel.reset() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("TRY ANOTHER", fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }
    }
}
