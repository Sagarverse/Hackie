package com.example.rabit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Hackie corner-radius system.
 *
 * Five steps. Most surfaces use 12dp (md). The drawer sheet, dialogs, and
 * bottom sheets step up to 24dp (xl). Pill-shaped controls use 999dp via
 * `RoundedCornerShape(50)` where they appear — not via a token, because
 * Material 3 expects an actual circle for those.
 */
val HackieShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
