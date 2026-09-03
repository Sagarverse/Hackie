package com.example.rabit.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

/**
 * Material 3 motion tokens. Three easings, four durations.
 *
 *   standard     — most transitions (page changes, sheets, fades)
 *   emphasized   — primary actions and hero state changes
 *   decelerated  — elements entering the screen (top app bar, FAB)
 *   accelerated  — elements leaving the screen
 */
object HackieMotion {
    val Standard     = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val Emphasized   = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val Decelerated  = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val Accelerated  = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)

    const val Short       = 150
    const val Medium      = 250
    const val Long        = 400
    const val ExtraLong   = 600
}
