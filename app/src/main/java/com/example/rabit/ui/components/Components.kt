package com.example.rabit.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@Composable
fun PremiumGlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Surface1,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun VibrantGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush = PremiumBlueGradient
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(gradient),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun PremiumSectionHeader(title: String) {
    Row(
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(AccentBlue.copy(alpha = 0.6f), CircleShape)
        )
        Text(
            text = title.uppercase(),
            color = TextTertiary,
            fontSize = 9.sp,
            fontWeight = FontWeight.W700,
            letterSpacing = 1.2.sp
        )
    }
}

/**
 * Floating control bar for pause/resume/stop during text push operations.
 */
@Composable
fun PushControlBar(
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pushPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isPaused) Surface3 else AccentBlue.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isPaused) BorderStrong else AccentBlue.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (isPaused) PausedAmber else SuccessGreen.copy(alpha = pulseAlpha),
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    if (isPaused) "PAUSED" else "TYPING…",
                    color = if (isPaused) TextSecondary else TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W700,
                    letterSpacing = 1.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledIconButton(
                    onClick = { if (isPaused) onResume() else onPause() },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isPaused) SuccessGreen else PausedAmber
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                FilledIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Surface4
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Icon with colored background — settings icon indicator.
 */
@Composable
fun SettingsIconBadge(
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .background(backgroundColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = backgroundColor,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Device type badge with icon and label.
 */
@Composable
fun DeviceTypeBadge(
    deviceType: DeviceType,
    modifier: Modifier = Modifier
) {
    val (icon, label, color) = when (deviceType) {
        DeviceType.MAC -> Triple(Icons.Default.Laptop, "Mac", MacDeviceColor)
        DeviceType.ANDROID -> Triple(Icons.Default.PhoneAndroid, "Android", AndroidDeviceColor)
        DeviceType.WINDOWS -> Triple(Icons.Default.DesktopWindows, "Windows", WindowsDeviceColor)
        DeviceType.UNKNOWN -> Triple(Icons.Default.Devices, "Device", UnknownDeviceColor)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(14.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.W500)
    }
}

enum class DeviceType {
    MAC, ANDROID, WINDOWS, UNKNOWN
}

fun guessDeviceType(name: String): DeviceType {
    val lower = name.lowercase()
    return when {
        lower.contains("macbook") || lower.contains("imac") || lower.contains("mac pro") ||
        lower.contains("mac mini") || lower.contains("mac studio") || lower.contains("apple") -> DeviceType.MAC
        lower.contains("android") || lower.contains("galaxy") || lower.contains("pixel") ||
        lower.contains("samsung") || lower.contains("oneplus") || lower.contains("xiaomi") ||
        lower.contains("redmi") || lower.contains("oppo") || lower.contains("vivo") ||
        lower.contains("realme") || lower.contains("poco") || lower.contains("motorola") ||
        lower.contains("huawei") || lower.contains("nokia") || lower.contains("lg") ||
        lower.contains("sony") || lower.contains("asus") || lower.contains("zte") -> DeviceType.ANDROID
        lower.contains("windows") || lower.contains("surface") || lower.contains("dell") ||
        lower.contains("hp ") || lower.contains("hp-") || lower.contains("lenovo") || lower.contains("thinkpad") ||
        lower.contains("asus desktop") || lower.contains("acer") || lower.contains("msi") ||
        lower.contains("desktop") || lower.contains("laptop") -> DeviceType.WINDOWS
        else -> DeviceType.UNKNOWN
    }
}

/**
 * Connection quality indicator pill.
 */
@Composable
fun ConnectionQualityIndicator(
    isConnected: Boolean,
    deviceName: String = ""
) {
    if (!isConnected) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(SuccessGreen.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .border(0.5.dp, SuccessGreen.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(SuccessGreen, CircleShape)
        )
        Text(
            if (deviceName.isNotBlank()) deviceName else "Connected",
            color = SuccessGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.W600,
            maxLines = 1
        )
    }
}

/**
 * Step indicator for the pairing flow.
 */
@Composable
fun ConnectionStepIndicator(
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    val steps = listOf("Enable BT", "Scan", "Select", "Connected")
    val stepIcons = listOf(
        Icons.Default.Bluetooth,
        Icons.Default.Search,
        Icons.Default.TouchApp,
        Icons.Default.CheckCircle
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(14.dp))
            .border(0.5.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val isCompleted = index < currentStep
            val isCurrent = index == currentStep
            val color = when {
                isCompleted -> SuccessGreen
                isCurrent -> AccentBlue
                else -> TextTertiary.copy(alpha = 0.3f)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color.copy(alpha = if (isCurrent) 0.12f else if (isCompleted) 0.08f else 0.04f),
                            CircleShape
                        )
                        .then(
                            if (isCurrent) Modifier.border(1.dp, color.copy(alpha = 0.4f), CircleShape) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        stepIcons[index],
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    label,
                    color = color,
                    fontSize = 9.sp,
                    fontWeight = if (isCurrent) FontWeight.W700 else FontWeight.W500,
                    maxLines = 1
                )
            }

            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .height(1.dp)
                        .width(16.dp)
                        .background(
                            if (isCompleted) SuccessGreen.copy(alpha = 0.3f) else BorderColor.copy(alpha = 0.2f),
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

/**
 * Premium device card for pairing screen.
 */
@Composable
fun AnimatedDeviceCard(
    name: String,
    deviceType: DeviceType,
    subtitle: String,
    isConnecting: Boolean = false,
    isBonded: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, iconColor) = when (deviceType) {
        DeviceType.MAC -> Icons.Default.Laptop to MacDeviceColor
        DeviceType.ANDROID -> Icons.Default.PhoneAndroid to AndroidDeviceColor
        DeviceType.WINDOWS -> Icons.Default.DesktopWindows to WindowsDeviceColor
        DeviceType.UNKNOWN -> Icons.Default.Devices to UnknownDeviceColor
    }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isConnecting) iconColor.copy(alpha = 0.04f) else Surface1,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isConnecting) iconColor.copy(alpha = 0.3f) else BorderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W600,
                            maxLines = 1
                        )
                        if (isBonded) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AccentGold.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    "PAIRED",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    color = AccentGold,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.W700
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        subtitle,
                        color = if (isConnecting) iconColor else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isConnecting) FontWeight.W500 else FontWeight.W400
                    )
                }
            }
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = iconColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextTertiary.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Quick-connect card for the last connected device.
 */
@Composable
fun QuickConnectCard(
    deviceName: String,
    lastConnectedTime: Long,
    isConnecting: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceType = guessDeviceType(deviceName)
    val (icon, iconColor) = when (deviceType) {
        DeviceType.MAC -> Icons.Default.Laptop to MacDeviceColor
        DeviceType.ANDROID -> Icons.Default.PhoneAndroid to AndroidDeviceColor
        DeviceType.WINDOWS -> Icons.Default.DesktopWindows to WindowsDeviceColor
        DeviceType.UNKNOWN -> Icons.Default.Devices to UnknownDeviceColor
    }

    val timeAgo = remember(lastConnectedTime) {
        val diff = System.currentTimeMillis() - lastConnectedTime
        when {
            diff < 60_000 -> "just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> "${diff / 86_400_000}d ago"
        }
    }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = iconColor.copy(alpha = 0.04f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, iconColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    deviceName,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Last connected $timeAgo",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = iconColor,
                    strokeWidth = 2.dp
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconColor.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Connect",
                            color = iconColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W600
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BluetoothDisabledBanner(onEnableClick: () -> Unit) {
    DarkSkeuoCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = BorderColor
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(WarningYellow.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BluetoothDisabled, null, tint = WarningYellow, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Bluetooth is Off", color = TextPrimary, fontWeight = FontWeight.W600, fontSize = 14.sp)
                Text("Enable to discover nearby computers", color = TextSecondary, fontSize = 11.sp)
            }
            TextButton(onClick = onEnableClick) {
                Text("ENABLE", color = WarningYellow, fontWeight = FontWeight.W700, fontSize = 12.sp)
            }
        }
    }
}
