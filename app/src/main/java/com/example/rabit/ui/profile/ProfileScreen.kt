package com.example.rabit.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rabit.ui.components.AppCard
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.components.SectionHeader
import com.example.rabit.ui.theme.HackieSpacing

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        StarfieldBackground()

        ScreenScaffold(
            title = "Profile",
            subtitle = "About the maker.",
            onBack = onBack,
        ) { _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HackieSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HackieSpacing.md),
            ) {
                Spacer(Modifier.height(HackieSpacing.md))

                // ── Identity card ───────────────────────────────
                AppCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = HackieSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(accent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                        Text(
                            text = "Sagar M",
                            style = MaterialTheme.typography.headlineSmall,
                            color = onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Software craftsman and creator",
                            style = MaterialTheme.typography.bodyMedium,
                            color = accent,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = HackieSpacing.sm),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Text(
                            text = "Building premium digital experiences from Bengaluru. Passionate about AI, P2P networking, and high-performance mobile interfaces.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // ── Social links ────────────────────────────────
                SectionHeader(title = "Connect")
                AppCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = HackieSpacing.sm),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        SocialButton(
                            icon = Icons.Default.Code,
                            label = "GitHub",
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/sgrkannada"),
                                    ),
                                )
                            },
                        )
                        SocialButton(
                            icon = Icons.Default.Group,
                            label = "LinkedIn",
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.linkedin.com/in/sagar-m-595b33298"),
                                        ),
                                    )
                                }
                            },
                        )
                        SocialButton(
                            icon = Icons.Default.PhotoCamera,
                            label = "Instagram",
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.instagram.com/mr.saga_rix_"),
                                    ),
                                )
                            },
                        )
                    }
                }

                // ── About the app ───────────────────────────────
                SectionHeader(title = "About the app")
                AppCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(HackieSpacing.sm),
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = accent,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hackie",
                                style = MaterialTheme.typography.titleSmall,
                                color = onSurface,
                            )
                            Text(
                                text = "Designed and developed for speed, privacy, and seamless cross-device productivity.",
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurfaceVariant,
                            )
                        }
                    }
                }

                // ── Features ────────────────────────────────────
                SectionHeader(title = "What's included")
                AppCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
                        modifier = Modifier.padding(vertical = HackieSpacing.xs),
                    ) {
                        listOf(
                            "Bluetooth HID keyboard with modifiers",
                            "Trackpad and air-mouse controls",
                            "AI assistant with auto-push typing",
                            "Prompt templates, copy, and speak response",
                            "Web Bridge with QR + secure PIN",
                            "File sharing and universal clipboard sync",
                            "URL handoff from Android share sheet",
                            "Automation dashboard and custom macros",
                            "Wake-on-LAN and SSH terminal tools",
                            "Snippets and shortcuts guide",
                            "Biometric lock, stealth mode, auto reconnect",
                            "Shake-to-disconnect and haptic presets",
                            "Theme, voice settings, and feature visibility controls",
                        ).forEach { FeatureRow(text = it) }
                    }
                }

                Spacer(Modifier.height(HackieSpacing.xl))
            }
        }
    }
}

@Composable
private fun StarfieldBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    val bg = MaterialTheme.colorScheme.background
    val bgDeep = MaterialTheme.colorScheme.surface
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    bg,
                    bgDeep,
                    bg,
                ),
            ),
        )
        val random = java.util.Random(42)
        repeat(50) {
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val radius = random.nextFloat() * 2.dp.toPx()
            drawCircle(
                color = Color.White.copy(alpha = alpha * random.nextFloat()),
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(x, y),
            )
        }
    }
}

@Composable
private fun SocialButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(HackieSpacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
