package com.example.rabit.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 4dp grid used across the app. Stop hardcoding random padding values.
 *
 * Typical pairings:
 *   screen edge gutter: lg (20)
 *   card padding:       md (16)
 *   list item padding:  sm (12) vertical / md (16) horizontal
 *   between sections:   lg (20) or xl (24)
 *   icon-to-label gap:  xxs (4) to xs (8)
 */
object HackieSpacing {
    val xxs = 4.dp
    val xs  = 8.dp
    val sm  = 12.dp
    val md  = 16.dp
    val lg  = 20.dp
    val xl  = 24.dp
    val xxl = 32.dp
}
