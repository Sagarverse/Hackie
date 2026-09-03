package com.example.rabit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// ════════════════════════════════════════════════════════════════════════════
// Hackie color system — Material 3 expressive + Apple HIG restraint.
// ════════════════════════════════════════════════════════════════════════════
//
// The old file hand-rolled a dark color scheme with one accent. We now expose
// real Material 3 schemes (light + dark) and a brand color. The legacy tokens
// at the bottom of this file are kept as deprecated aliases pointing at the
// new values so the rest of the codebase continues to compile while we
// migrate the UI.
// ════════════════════════════════════════════════════════════════════════════

// ── Brand seed (the one accent) ────────────────────────────────────────────
val BrandBlue       = Color(0xFF1E6BFF)   // primary brand — used in CTAs, links
val BrandBlueDark   = Color(0xFF0B4DDB)   // dark-mode primary
val BrandOnLightBg  = Color(0xFFFFFFFF)
val BrandOnDarkBg   = Color(0xFFFFFFFF)

// ── Light scheme primitives (raw, fed into lightColorScheme() in Theme.kt) ─
val LightPrimary              = Color(0xFF1E6BFF)
val LightOnPrimary            = Color(0xFFFFFFFF)
val LightPrimaryContainer     = Color(0xFFE0EBFF)
val LightOnPrimaryContainer   = Color(0xFF002A77)

val LightSecondary            = Color(0xFF5B6470)
val LightOnSecondary          = Color(0xFFFFFFFF)
val LightSecondaryContainer   = Color(0xFFE2E7EE)
val LightOnSecondaryContainer = Color(0xFF1B2027)

val LightTertiary             = Color(0xFF1F8A6E)
val LightOnTertiary           = Color(0xFFFFFFFF)
val LightTertiaryContainer    = Color(0xFFB7F1DD)
val LightOnTertiaryContainer  = Color(0xFF002115)

val LightError                = Color(0xFFD64545)
val LightOnError              = Color(0xFFFFFFFF)
val LightErrorContainer       = Color(0xFFFFE4E4)
val LightOnErrorContainer     = Color(0xFF410002)

val LightBackground           = Color(0xFFFAFBFD)   // canvas
val LightOnBackground         = Color(0xFF111418)   // primary text
val LightSurface              = Color(0xFFFFFFFF)   // primary card
val LightOnSurface            = Color(0xFF111418)
val LightSurfaceVariant       = Color(0xFFEFF1F5)   // secondary card / input
val LightOnSurfaceVariant     = Color(0xFF454C55)   // secondary text
val LightSurfaceContainer     = Color(0xFFF2F4F8)   // elevated card
val LightSurfaceContainerHigh = Color(0xFFECEFF4)   // list item hover
val LightSurfaceContainerHighest = Color(0xFFE5E9EF)
val LightOutline              = Color(0xFFC9CFD8)
val LightOutlineVariant       = Color(0xFFE2E6EC)
val LightScrim                = Color(0x66000000)
val LightInverseSurface       = Color(0xFF1A1D22)
val LightInverseOnSurface     = Color(0xFFF1F2F4)
val LightInversePrimary       = Color(0xFFA9C7FF)

// ── Dark scheme primitives ────────────────────────────────────────────────
val DarkPrimary              = Color(0xFFA9C7FF)
val DarkOnPrimary            = Color(0xFF002A77)
val DarkPrimaryContainer     = Color(0xFF1B4FBF)
val DarkOnPrimaryContainer   = Color(0xFFD9E3FF)

val DarkSecondary            = Color(0xFFC1C8D2)
val DarkOnSecondary          = Color(0xFF2B3138)
val DarkSecondaryContainer   = Color(0xFF424850)
val DarkOnSecondaryContainer = Color(0xFFDFE4EB)

val DarkTertiary             = Color(0xFF7CD9B8)
val DarkOnTertiary           = Color(0xFF003828)
val DarkTertiaryContainer    = Color(0xFF005C42)
val DarkOnTertiaryContainer  = Color(0xFFB7F1DD)

val DarkError                = Color(0xFFFFB4AB)
val DarkOnError              = Color(0xFF690005)
val DarkErrorContainer       = Color(0xFF93000A)
val DarkOnErrorContainer     = Color(0xFFFFDAD6)

val DarkBackground           = Color(0xFF0E1116)   // canvas
val DarkOnBackground         = Color(0xFFE6E8EC)   // primary text
val DarkSurface              = Color(0xFF15181D)   // primary card
val DarkOnSurface            = Color(0xFFE6E8EC)
val DarkSurfaceVariant       = Color(0xFF1F232A)   // secondary card / input
val DarkOnSurfaceVariant     = Color(0xFFA6ABB5)   // secondary text
val DarkSurfaceContainer     = Color(0xFF181B21)   // elevated card
val DarkSurfaceContainerHigh = Color(0xFF1D2128)
val DarkSurfaceContainerHighest = Color(0xFF252A32)
val DarkOutline              = Color(0xFF3A4049)
val DarkOutlineVariant       = Color(0xFF272C34)
val DarkScrim                = Color(0x99000000)
val DarkInverseSurface       = Color(0xFFE6E8EC)
val DarkInverseOnSurface     = Color(0xFF1A1D22)
val DarkInversePrimary       = Color(0xFF1E6BFF)

// ── Functional (semantic) ────────────────────────────────────────────────
val Success                  = Color(0xFF1F8A4C)
val SuccessContainer         = Color(0xFFCDEDD7)
val OnSuccess                = Color(0xFFFFFFFF)
val OnSuccessContainer       = Color(0xFF002111)
val SuccessDark              = Color(0xFF6FDD9B)
val SuccessContainerDark     = Color(0xFF1E5C36)
val OnSuccessDark            = Color(0xFF00391C)
val OnSuccessContainerDark   = Color(0xFFB7F1D1)

val Warning                  = Color(0xFFB25E09)
val WarningContainer         = Color(0xFFFFDDB4)
val OnWarning                = Color(0xFFFFFFFF)
val OnWarningContainer       = Color(0xFF2C1700)
val WarningDark              = Color(0xFFFFB95B)
val WarningContainerDark     = Color(0xFF5A3B00)
val OnWarningDark            = Color(0xFF4A2A00)
val OnWarningContainerDark   = Color(0xFFFFDDB4)

// ── Gradients (kept for the few places that still want a brand wash) ─────
val BrandGradient = Brush.verticalGradient(
    listOf(Color(0xFF1E6BFF), Color(0xFF0B4DDB))
)
val BrandGradientSubtle = Brush.verticalGradient(
    listOf(Color(0x141E6BFF), Color(0x000B4DDB))
)

// ════════════════════════════════════════════════════════════════════════════
// Deprecated aliases — kept so the rest of the codebase continues to compile
// while we migrate. New code MUST use the tokens above, or the theme-aware
// accessor [com.example.rabit.ui.theme.hackieColors] for any token that
// needs to follow the active scheme (Platinum, Silver, Surface1, Obsidian,
// AccentTeal, etc.).
//
// The aliases below are pinned to the *dark* scheme, so screens that
// import them will render dark in both light and dark themes. That's
// the same as today's behavior — see `audit-pass-3.md` for the
// migration plan. The accessor in `HackieTheme.kt` is the supported
// path forward; new screens should call it via
// `hackieColors().textPrimary` / `hackieColors().surface1` / etc.
// ════════════════════════════════════════════════════════════════════════════

// Surfaces
val Obsidian       = LightBackground
val DeepObsidian   = Color(0xFFFAFBFD)
val Graphite       = LightSurface
val SoftGrey       = LightSurfaceVariant
val Surface0       = LightBackground
val Surface1       = LightSurface
val Surface2       = LightSurfaceContainer
val Surface3       = LightSurfaceContainerHigh
val Surface4       = LightSurfaceContainerHighest

// Text
val TextPrimary    = LightOnSurface
val TextSecondary  = LightOnSurfaceVariant
val Silver         = LightOnSurfaceVariant
val TextTertiary   = Color(0xFF7A818B)
val TextDisabled   = LightOutline
val Platinum       = LightOnBackground

// Accents (the old single-accent world)
val AccentBlue     = BrandBlue
val AccentBlueDim  = BrandBlueDark
val AccentBlueMute = Color(0xFF1E40AF)
val AccentPurple   = Color(0xFF8B5CF6)
val AccentTeal     = Color(0xFF38BDF8)
val AccentPink     = Color(0xFFEC4899)
val AccentGold     = Color(0xFFF59E0B)
val AccentOrange   = Color(0xFFF97316)

// Functional
val SuccessGreen   = Success
val ErrorRed       = LightError
val WarningYellow  = Warning
val MintTeal       = DarkTertiary

// Borders
val BorderColor     = LightOutline
val BorderStrong    = LightOutline
val BorderSubtle    = LightOutlineVariant
val GlassOverlay    = Color(0x550B0F15)

// Component tokens
val CardBackground  = LightSurface
val CardDark        = LightSurface
val CardDarkBorder  = LightOutline
val KeyBackground   = LightSurfaceVariant
val KeyText         = LightOnSurface

// Device colors
val MacDeviceColor      = Color(0xFFFFFFFF)
val AndroidDeviceColor  = Color(0xFFD1D1D6)
val WindowsDeviceColor  = Color(0xFFAEAEC0)
val UnknownDeviceColor  = Color(0xFF8E8E93)

// Status
val PausedAmber = Color(0xFFFFB95B)
val StopRed     = LightError

// AI Chat
val AiViolet     = BrandBlue
val AiIndigo     = BrandBlueDark
val ChatSurface  = LightBackground
val InputBarGlass = LightSurfaceContainer
val AiOrbGlow    = Color(0x553B82F6)

// Old gradient presets (kept for any callsite that still imports them)
val PremiumBlueGradient = BrandGradient
val DarkGlassGradient = Brush.verticalGradient(
    listOf(Color(0xCC0B0F15), Color(0xAA111720))
)
val AppAtmosphereGradient = Brush.verticalGradient(
    listOf(Surface0, Color(0xFF0D1219), Surface0)
)
val GlassCardGradient = Brush.verticalGradient(
    listOf(Color(0xCC161D28), Color(0xB3111720))
)
val PremiumDarkGradient = Brush.verticalGradient(
    listOf(Color(0xFF1A2230), Color(0xFF111720), Surface0)
)
val PremiumGoldGradient = Brush.horizontalGradient(
    listOf(Color(0xFF64748B), Color(0xFF94A3B8), Color(0xFF64748B))
)
val GlowBlue  = Color(0x303B82F6)
val GlowGreen = Color(0x2022C55E)
val GlowGold  = Color(0x3094A3B8)

// AI chat gradients
val UserBubbleGradient = Brush.linearGradient(
    listOf(Color(0xFF1E3A5F), Color(0xFF172D4D))
)
val AiBubbleGradient = Brush.verticalGradient(
    listOf(Surface1, Surface0)
)
val AiOrbGradient = Brush.radialGradient(
    listOf(Color(0x663B82F6), Color.Transparent)
)
val SuggestionChipGradient = Brush.horizontalGradient(
    listOf(Surface2, Surface1)
)
