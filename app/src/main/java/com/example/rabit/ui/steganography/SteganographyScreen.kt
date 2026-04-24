package com.example.rabit.ui.steganography

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SteganographyScreen(viewModel: SteganographyViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val selectedUri by viewModel.selectedImageUri.collectAsState()
    var secretMessage by remember { mutableStateOf("") }
    var isEncodeMode by remember { mutableStateOf(true) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.selectImage(it) }
    }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("STEGANOGRAPHY LAB", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text("COVERT DATA IN IMAGES", color = Color(0xFF8B5CF6), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Platinum) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Toggle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { isEncodeMode = true }, modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isEncodeMode) Color(0xFF8B5CF6) else Surface1),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Lock, null, tint = if (isEncodeMode) Obsidian else Silver, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ENCODE", fontWeight = FontWeight.Black, color = if (isEncodeMode) Obsidian else Silver, fontSize = 12.sp)
                }
                Button(
                    onClick = { isEncodeMode = false }, modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (!isEncodeMode) Color(0xFF8B5CF6) else Surface1),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.LockOpen, null, tint = if (!isEncodeMode) Obsidian else Silver, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("DECODE", fontWeight = FontWeight.Black, color = if (!isEncodeMode) Obsidian else Silver, fontSize = 12.sp)
                }
            }

            // Image Selector
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface1),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CARRIER IMAGE", color = Color(0xFF8B5CF6), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(12.dp))

                    if (selectedUri != null) {
                        Text("Image selected ✓", color = SuccessGreen, fontWeight = FontWeight.Bold)
                    } else {
                        Text("No image selected", color = Silver)
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Platinum),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.ImageSearch, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("SELECT IMAGE")
                    }
                }
            }

            if (isEncodeMode) {
                // Secret Message Input
                OutlinedTextField(
                    value = secretMessage, onValueChange = { secretMessage = it },
                    label = { Text("Secret Message", color = Silver) },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Platinum, unfocusedTextColor = Platinum, focusedBorderColor = Color(0xFF8B5CF6), unfocusedBorderColor = BorderColor),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                )

                Button(
                    onClick = { viewModel.encodeMessage(secretMessage) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedUri != null && secretMessage.isNotBlank() && state !is StegoState.Processing
                ) {
                    Icon(Icons.Default.Lock, null, tint = Obsidian)
                    Spacer(Modifier.width(8.dp))
                    Text("HIDE MESSAGE IN IMAGE", fontWeight = FontWeight.Black, color = Obsidian)
                }
            } else {
                Button(
                    onClick = { viewModel.decodeMessage() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedUri != null && state !is StegoState.Processing
                ) {
                    Icon(Icons.Default.LockOpen, null, tint = Obsidian)
                    Spacer(Modifier.width(8.dp))
                    Text("EXTRACT HIDDEN MESSAGE", fontWeight = FontWeight.Black, color = Obsidian)
                }
            }

            // Result Display
            when (val s = state) {
                is StegoState.Processing -> {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6), modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is StegoState.Encoded -> {
                    Surface(color = SuccessGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("ENCODING SUCCESSFUL", color = SuccessGreen, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(s.message, color = Platinum, fontSize = 13.sp)
                        }
                    }
                }
                is StegoState.Decoded -> {
                    Surface(color = Color(0xFF8B5CF6).copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("HIDDEN MESSAGE RECOVERED", color = Color(0xFF8B5CF6), fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(s.hiddenMessage, color = Platinum, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                is StegoState.Error -> {
                    Surface(color = Color.Red.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)) {
                        Text(s.reason, color = Color.Red, modifier = Modifier.padding(16.dp), fontSize = 13.sp)
                    }
                }
                else -> {}
            }
        }
    }
}
