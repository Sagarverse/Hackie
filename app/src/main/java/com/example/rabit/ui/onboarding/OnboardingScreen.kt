package com.example.rabit.ui.onboarding

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rabit.ui.components.PrimaryButton
import com.example.rabit.ui.components.ScreenScaffold
import com.example.rabit.ui.theme.HackieSpacing
import com.example.rabit.ui.theme.Success
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue

private data class OnboardingPage(
    val icon: ImageVector,
    val accent: Color,
    val title: String,
    val subtitle: String,
    val description: String,
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val pages = remember {
        listOf(
            OnboardingPage(
                icon = Icons.Default.Bluetooth,
                accent = accent,
                title = "Wireless control",
                subtitle = "Turn your phone into a keyboard and mouse",
                description = "Control your Mac or Android device wirelessly over Bluetooth HID. No apps needed on the host device.",
            ),
            OnboardingPage(
                icon = Icons.Default.Keyboard,
                accent = accent,
                title = "Smart keyboard",
                subtitle = "Type, dictate, and push text",
                description = "Use the custom keyboard, voice-to-text, or batch-send entire files. Pause and resume anytime.",
            ),
            OnboardingPage(
                icon = Icons.Default.Dashboard,
                accent = accent,
                title = "Macros and shortcuts",
                subtitle = "One-tap automation for Mac",
                description = "Lock screen, take screenshots, launch Spotlight, and create custom shell macros — all with a single tap.",
            ),
            OnboardingPage(
                icon = Icons.Default.AutoAwesome,
                accent = accent,
                title = "AI assistant",
                subtitle = "Gemini-powered smart responses",
                description = "Generate text with AI and push it directly to your Mac. Works online with Gemini or offline with local models.",
            ),
            OnboardingPage(
                icon = Icons.Default.Shield,
                accent = Success,
                title = "Secure and private",
                subtitle = "End-to-end encrypted",
                description = "All data is AES-GCM 256-bit encrypted. Your keystrokes never leave the local network.",
            ),
            OnboardingPage(
                icon = Icons.Default.VpnKey,
                accent = accent,
                title = "Macro Genie & Web Bridge",
                subtitle = "AI macros + zero-install browser control",
                description = "Tell the Genie what to do on your Mac, then control any host from a browser with a 4-digit PIN.",
            ),
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    ScreenScaffold(
        title = "Welcome",
        subtitle = "${pagerState.currentPage + 1} of ${pages.size}",
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = HackieSpacing.md),
        ) {
            // Top-right skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = HackieSpacing.xs),
                horizontalArrangement = Arrangement.End,
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(onClick = onComplete) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = HackieSpacing.xs),
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(HackieSpacing.xs),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = HackieSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                pages.indices.forEach { index ->
                    val isActive = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isActive) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) accent
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                    )
                }
                Spacer(Modifier.weight(1f))
            }

            val isLastPage = pagerState.currentPage == pages.size - 1
            PrimaryButton(
                text = if (isLastPage) "Get started" else "Continue",
                onClick = {
                    if (isLastPage) onComplete()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = HackieSpacing.md),
                icon = if (isLastPage) null else Icons.AutoMirrored.Filled.ArrowForward,
            )
            Text(
                text = if (isLastPage) "You're all set for pairing and control."
                else "Swipe to preview features or continue.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = HackieSpacing.lg),
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val infiniteTransition = rememberInfiniteTransition(label = "onboardGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )
    val accent = page.accent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = HackieSpacing.md),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Glow ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = glowAlpha), Color.Transparent),
                        center = center,
                        radius = size.minDimension / 2,
                    ),
                )
            }
            // Inner circle
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        Spacer(Modifier.height(HackieSpacing.xl))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(HackieSpacing.xs))

        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.titleSmall,
            color = accent,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(HackieSpacing.md))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
