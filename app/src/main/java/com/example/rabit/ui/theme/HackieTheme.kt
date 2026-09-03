package com.example.rabit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * The Hackie brand color tokens, expressed in a way that *follows the
 * active Material 3 color scheme* so screens that haven't migrated
 * yet can still render correctly in light and dark themes.
 *
 * Today, the vast majority of the app imports the legacy `val`
 * aliases at the bottom of [Color.kt] (Platinum, Silver, Surface1,
 * Obsidian, AccentTeal, …). Those values are pinned to the *dark*
 * scheme constants, so light mode looks dark.
 *
 * New code should call [hackieColors] instead of importing the
 * legacy `val`s. Old code keeps compiling because the legacy
 * aliases still exist; they'll be migrated file by file in a later
 * pass.
 */
data class HackieColors(
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val surface1: Color,
    val surface2: Color,
    val canvas: Color,
    val outline: Color,
    val accentTeal: Color,
    val success: Color,
    val error: Color,
    val warning: Color,
)

private val DarkHackieColors = HackieColors(
    textPrimary    = DarkOnBackground,
    textSecondary  = DarkOnSurfaceVariant,
    textTertiary   = TextTertiary,
    surface1       = DarkSurface,
    surface2       = DarkSurfaceContainer,
    canvas         = DarkBackground,
    outline        = DarkOutline,
    accentTeal     = AccentTeal,
    success        = Success,
    error          = LightError,
    warning        = Warning,
)

private val LightHackieColors = HackieColors(
    textPrimary    = LightOnBackground,
    textSecondary  = LightOnSurfaceVariant,
    textTertiary   = LightOnSurfaceVariant.copy(alpha = 0.7f),
    surface1       = LightSurface,
    surface2       = LightSurfaceContainer,
    canvas         = LightBackground,
    outline        = LightOutline,
    accentTeal     = BrandBlue,
    success        = Success,
    error          = LightError,
    warning        = Warning,
)

/**
 * Resolves the active [HackieColors] for the current theme. Honors
 * the user's explicit choice from
 * [com.example.rabit.data.prefs.UserPreferences.ThemeMode] when
 * provided; otherwise falls back to the system setting.
 *
 * Called as a function (e.g. `hackieColors()`) because it needs to
 * read the live MaterialTheme composition local. A property-style
 * access would not work without a CompositionLocal receiver.
 */
@Composable
@ReadOnlyComposable
fun hackieColors(): HackieColors {
    // `MaterialTheme.colorScheme.background` reflects the active
    // scheme (light or dark). We sample its luminance to pick the
    // matching Hackie palette. Because HackieTheme rebuilds the
    // composition when the mode changes, this re-runs and the
    // correct palette is returned for the new scheme.
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    return if (isDark) DarkHackieColors else LightHackieColors
}
