package com.example.rabit.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.rabit.data.voice.VoiceState
import com.example.rabit.ui.assistant.AssistantMotion
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.Silver

@Composable
fun PulsingVoiceButton(
    state: VoiceState,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == VoiceState.LISTENING) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AssistantMotion.PULSE_DOT, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (state == VoiceState.LISTENING) 0.8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AssistantMotion.PULSE_DOT, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        if (state == VoiceState.LISTENING) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    .background(AccentBlue.copy(alpha = pulseAlpha), CircleShape)
            )
        }
        
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (state == VoiceState.LISTENING) AccentBlue.copy(alpha = 0.2f) 
                    else Color.Transparent, 
                    CircleShape
                )
        ) {
            Icon(
                if (state == VoiceState.LISTENING) Icons.Default.Mic 
                else Icons.Default.MicNone,
                contentDescription = "Voice Input",
                tint = if (state == VoiceState.LISTENING) AccentBlue else Silver,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
