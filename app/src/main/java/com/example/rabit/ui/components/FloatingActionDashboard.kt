package com.example.rabit.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun FloatingActionDashboard(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
    viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    
    val jarvisState by viewModel.jarvisState.collectAsState()
    val jarvisMode by viewModel.jarvisMode.collectAsState()
    val isListening = jarvisState == com.example.rabit.data.voice.JarvisCommandEngine.JarvisState.LISTENING_COMMAND
    val isThinking = jarvisState == com.example.rabit.data.voice.JarvisCommandEngine.JarvisState.THINKING
    
    // Pulse animation for mic
    val transition = rememberInfiniteTransition(label = "pulse")
    val micScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.9f, animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F0F13).copy(alpha = 0.95f))
                    .border(
                        1.dp, 
                        Brush.linearGradient(
                            listOf(Color(0xFFBC13FE), Color(0xFF0B84FE))
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
                    .clickable(enabled = false) {} // Consume clicks inside
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SAGAR AI",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFBC13FE),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Engine Toggle
                    val isGemini = jarvisMode == "SMART"
                    val toggleColor by animateColorAsState(
                        targetValue = if (isGemini) Color(0xFFBC13FE) else Color(0xFF3B82F6),
                        label = "toggleColor",
                        animationSpec = tween(300)
                    )
                    
                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .width(220.dp)
                            .height(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E1E24),
                        border = androidx.compose.foundation.BorderStroke(1.dp, toggleColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { viewModel.setJarvisMode("BASIC") }
                                    .background(if (!isGemini) toggleColor.copy(alpha = 0.2f) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "If / Else", 
                                    color = if (!isGemini) Color.White else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { viewModel.setJarvisMode("SMART") }
                                    .background(if (isGemini) toggleColor.copy(alpha = 0.2f) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Gemini LLM", 
                                    color = if (isGemini) Color.White else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // Mic Button
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(micScale)
                            .clip(CircleShape)
                            .background(
                                if (isListening) Color(0xFFBC13FE).copy(alpha = 0.2f) 
                                else Color(0xFFBC13FE).copy(alpha = 0.1f)
                            )
                            .border(1.dp, Color(0xFFBC13FE).copy(alpha = 0.3f), CircleShape)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                if (isListening) {
                                    viewModel.jarvisEngine.resetToListening()
                                } else {
                                    viewModel.jarvisEngine.startListeningForCommand()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic, 
                            contentDescription = "Speak to Sagar AI",
                            tint = if (isListening) Color(0xFFBC13FE) else Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = when {
                            isThinking -> "Thinking..."
                            isListening -> "Listening..."
                            else -> "Tap to speak"
                        },
                        color = if (isListening || isThinking) Color(0xFFBC13FE) else Color.Gray,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Text Input
                    var commandText by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = commandText,
                        onValueChange = { commandText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Or type a command...", color = Color.Gray, fontSize = 14.sp) },
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFBC13FE),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (commandText.isNotBlank()) {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        coroutineScope.launch {
                                            viewModel.jarvisSmartEngine.processCommand(commandText)
                                        }
                                        commandText = ""
                                        onDismiss() // Hide the dashboard when executing text
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF0B84FE))
                            }
                        }
                    )
                }
            }
        }
    }
}
