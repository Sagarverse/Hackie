package com.example.rabit.ui.crypto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.steganography.SteganographyViewModel
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.ScreenScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoToolkitScreen(
    encoderDecoderViewModel: EncoderDecoderViewModel,
    steganographyViewModel: SteganographyViewModel,
    onBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("ENCODER/DECODER", "STEGANOGRAPHY")

    ScreenScaffold(
        title = "Crypto toolkit",
        subtitle = "Ciphers, hashes, encoding",
        onBack = onBack
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = AccentBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = AccentBlue,
                        height = 3.dp
                    )
                },
                divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Black else FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (selectedTabIndex == index) AccentBlue else Silver
                            )
                        }
                    )
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> {
                        EncoderDecoderContent(encoderDecoderViewModel)
                    }
                    1 -> {
                        com.example.rabit.ui.steganography.SteganographyContent(steganographyViewModel)
                    }
                }
            }
        }
    }
}
