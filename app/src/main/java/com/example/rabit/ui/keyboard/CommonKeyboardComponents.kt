package com.example.rabit.ui.keyboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.domain.model.RemoteFile
import com.example.rabit.domain.model.Workstation
import com.example.rabit.ui.theme.*

@Composable
fun PremiumKey(label: String, modifier: Modifier, accent: Color, onPress: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPressed) Surface3 else KeyBackground)
            .border(
                0.5.dp,
                if (isPressed) accent.copy(alpha = 0.4f) else BorderColor,
                RoundedCornerShape(12.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onPress() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) accent else accent.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.W600
        )
    }
}

@Composable
fun MouseButton(modifier: Modifier, text: String, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "mouseBtn")

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Surface2),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.4f)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.W500)
    }
}

@Composable
fun WorkstationCard(
    workstation: Workstation,
    connectionState: HidDeviceManager.ConnectionState,
    onConnect: (Workstation) -> Unit
) {
    val isConnecting = connectionState is HidDeviceManager.ConnectionState.Connecting
    val isConnected = (connectionState as? HidDeviceManager.ConnectionState.Connected)?.deviceName == workstation.name

    Surface(
        modifier = Modifier
            .width(160.dp)
            .clickable(!isConnecting && !isConnected) { onConnect(workstation) },
        color = if (isConnected) AccentBlue.copy(alpha = 0.08f) else Surface2,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isConnected) AccentBlue.copy(alpha = 0.3f) else BorderColor
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isConnected) AccentBlue.copy(alpha = 0.12f) else Surface3,
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (workstation.name.contains("Mac", true)) Icons.Default.LaptopMac else Icons.Default.Computer,
                        contentDescription = null,
                        tint = if (isConnected) AccentBlue else TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isConnected) {
                    Box(modifier = Modifier.size(8.dp).background(SuccessGreen, CircleShape))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                workstation.name,
                color = if (isConnected) AccentBlue else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Text(
                workstation.address.take(12) + "…",
                color = TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.W400
            )
        }
    }
}

@Composable
fun RemoteFileCard(file: RemoteFile, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = if (file.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile
        val tint = if (file.isDirectory) AccentBlue else TextTertiary

        Box(
            modifier = Modifier
                .size(48.dp)
                .background(tint.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            file.name,
            color = TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.W500,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SmallActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = Surface2,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(28.dp).background(accent.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.W600, letterSpacing = 0.5.sp)
        }
    }
}
