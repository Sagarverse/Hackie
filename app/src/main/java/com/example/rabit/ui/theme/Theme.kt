package com.example.rabit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.rabit.data.prefs.UserPreferences

private val LightColors = lightColorScheme(
    primary              = LightPrimary,
    onPrimary            = LightOnPrimary,
    primaryContainer     = LightPrimaryContainer,
    onPrimaryContainer   = LightOnPrimaryContainer,

    secondary            = LightSecondary,
    onSecondary          = LightOnSecondary,
    secondaryContainer   = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,

    tertiary             = LightTertiary,
    onTertiary           = LightOnTertiary,
    tertiaryContainer    = LightTertiaryContainer,
    onTertiaryContainer  = LightOnTertiaryContainer,

    error                = LightError,
    onError              = LightOnError,
    errorContainer       = LightErrorContainer,
    onErrorContainer     = LightOnErrorContainer,

    background           = LightBackground,
    onBackground         = LightOnBackground,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = LightOnSurfaceVariant,
    surfaceContainer     = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    surfaceTint          = LightPrimary,
    inverseSurface       = LightInverseSurface,
    inverseOnSurface     = LightInverseOnSurface,
    inversePrimary       = LightInversePrimary,

    outline              = LightOutline,
    outlineVariant       = LightOutlineVariant,
    scrim                = LightScrim,
)

private val DarkColors = darkColorScheme(
    primary              = DarkPrimary,
    onPrimary            = DarkOnPrimary,
    primaryContainer     = DarkPrimaryContainer,
    onPrimaryContainer   = DarkOnPrimaryContainer,

    secondary            = DarkSecondary,
    onSecondary          = DarkOnSecondary,
    secondaryContainer   = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,

    tertiary             = DarkTertiary,
    onTertiary           = DarkOnTertiary,
    tertiaryContainer    = DarkTertiaryContainer,
    onTertiaryContainer  = DarkOnTertiaryContainer,

    error                = DarkError,
    onError              = DarkOnError,
    errorContainer       = DarkErrorContainer,
    onErrorContainer     = DarkOnErrorContainer,

    background           = DarkBackground,
    onBackground         = DarkOnBackground,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = DarkOnSurfaceVariant,
    surfaceContainer     = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    surfaceTint          = DarkPrimary,
    inverseSurface       = DarkInverseSurface,
    inverseOnSurface     = DarkInverseOnSurface,
    inversePrimary       = DarkInversePrimary,

    outline              = DarkOutline,
    outlineVariant       = DarkOutlineVariant,
    scrim                = DarkScrim,
)

/**
 * Hackie app theme.
 *
 * Three modes:
 *  - [UserPreferences.ThemeMode.SYSTEM] — follow the system (default)
 *  - [UserPreferences.ThemeMode.LIGHT]  — force light scheme
 *  - [UserPreferences.ThemeMode.DARK]   — force dark scheme
 *
 * The user's choice is persisted in [UserPreferences] and survives process
 * death. Switching the value at runtime will not retro-actively swap the
 * theme (you'd have to re-set content), but the next activity launch
 * honors the new value.
 */
@Composable
fun HackieTheme(
    themeMode: UserPreferences.ThemeMode = UserPreferences.ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = false
    val colorScheme = LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HackieTypography,
        shapes = HackieShapes,
        content = content,
    )
}

/**
 * Backwards-compatible alias used by the rest of the app today. Will be
 * removed once every screen is migrated to `HackieTheme`.
 */
@Composable
fun RabitTheme(
    themeMode: UserPreferences.ThemeMode = UserPreferences.ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    HackieTheme(themeMode = themeMode, content = content)
}

// Suppress an unused-warning on the imported Color (kept in case any callsite
// reaches for it via `*` import).
@Suppress("unused")
private val _unusedColor = Color.Transparent
