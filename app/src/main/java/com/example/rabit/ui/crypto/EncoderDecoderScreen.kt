package com.example.rabit.ui.crypto

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncoderDecoderScreen(viewModel: EncoderDecoderViewModel, onBack: () -> Unit) {
    val inputText by viewModel.inputText.collectAsState()
    val base64Output by viewModel.base64Output.collectAsState()
    val hexOutput by viewModel.hexOutput.collectAsState()
    val urlOutput by viewModel.urlOutput.collectAsState()
    val binaryOutput by viewModel.binaryOutput.collectAsState()
    val rot13Output by viewModel.rot13Output.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CRYPTO ENCODER", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = Platinum))
                        Text(if (viewModel.isEncodingMode) "ENCODE MODE" else "DECODE MODE", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Platinum)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setMode(!viewModel.isEncodingMode) }) {
                        Icon(Icons.Default.SwapHoriz, null, tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { viewModel.updateInput(it) },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                label = { Text("Input Payload", color = Silver) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Platinum,
                    unfocusedTextColor = Platinum,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderColor
                ),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            )

            if (inputText.isNotEmpty()) {
                CryptoResultCard("Base64", base64Output, context)
                CryptoResultCard("Hexadecimal", hexOutput, context)
                CryptoResultCard("URL Encoding", urlOutput, context)
                CryptoResultCard("Binary", binaryOutput, context)
                CryptoResultCard("ROT13", rot13Output, context)
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                    Text("Awaiting input payload...", color = Silver.copy(alpha = 0.4f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CryptoResultCard(title: String, content: String, context: Context) {
    Surface(
        color = Surface1,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Copied Payload", content))
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Silver, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                content,
                color = if (content.startsWith("Invalid") || content == "Error") Color.Red else Platinum,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
