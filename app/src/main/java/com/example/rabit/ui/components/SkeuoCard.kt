package com.example.rabit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.rabit.ui.theme.*

// Light SkeuoCard removed — use DarkSkeuoCard for the app's dark theme

/**
 * Dark variant of SkeuoCard that matches the app's dark premium aesthetic.
 * Use this instead of the light SkeuoCard throughout the app.
 */
@Composable
fun DarkSkeuoCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    background: Brush = Brush.verticalGradient(
        listOf(
            Color(0xFF2A2A2E),
            Color(0xFF1E1E20),
            Color(0xFF141416)
        )
    ),
    borderColor: Color = CardDarkBorder,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(cornerRadius), ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
            .clip(RoundedCornerShape(cornerRadius))
            .background(background)
            .border(0.5.dp, borderColor, RoundedCornerShape(cornerRadius))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
