package com.example.rabit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// ── Core Palette ──────────────────────────────────────────────────────────────
// A deliberately restrained monochromatic base with one signature accent.
val Obsidian       = Color(0xFF090D12)
val DeepObsidian   = Color(0xFF060910)
val Graphite       = Color(0xFF111720)
val SoftGrey       = Color(0xFF1A2230)
val Silver         = Color(0xFF8A95A8)
val Platinum       = Color(0xFFF0F4FC)

// ── Surface Elevation System ─────────────────────────────────────────────────
// Progressive surface tints for layered card depth (dark-mode only).
val Surface0 = Color(0xFF0B0F15)   // canvas
val Surface1 = Color(0xFF111720)   // primary card
val Surface2 = Color(0xFF161D28)   // elevated card / drawer
val Surface3 = Color(0xFF1C2433)   // modal / dialog
val Surface4 = Color(0xFF222B3C)   // hover / active state

// ── Semantic Text ────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFF0F4FC)
val TextSecondary = Color(0xFF8A95A8)
val TextTertiary  = Color(0xFF586270)
val TextDisabled  = Color(0xFF3A4452)

// ── Accent ───────────────────────────────────────────────────────────────────
// Single signature accent with tonal variants.
val AccentBlue     = Color(0xFF3B82F6)
val AccentBlueDim  = Color(0xFF2563EB)
val AccentBlueMute = Color(0xFF1E40AF)

// Legacy aliases – mapped to the single accent so nothing breaks.
val AccentPurple = AccentBlue
val AccentTeal   = Color(0xFF38BDF8)
val AccentPink   = AccentBlue
val AccentGold   = Color(0xFF94A3B8)
val AccentOrange = Color(0xFF94A3B8)

// ── Functional Colors ────────────────────────────────────────────────────────
val SuccessGreen   = Color(0xFF22C55E)
val ErrorRed: Color get() = Color(0xFFEF4444)
val WarningYellow  = Color(0xFFEAB308)
val MintTeal       = Color(0xFF2DD4BF)

// ── Borders & Glass ──────────────────────────────────────────────────────────
val BorderColor     = Color(0xFF1E2738)
val BorderStrong    = Color(0xFF2A3548)
val BorderSubtle    = Color(0xFF161D28)
val GlassOverlay    = Color(0x550B0F15)

// ── Component-specific tokens ────────────────────────────────────────────────
val CardBackground  = Surface1
val CardDark        = Surface1
val CardDarkBorder  = BorderColor
val KeyBackground   = Color(0xFF151C27)
val KeyText         = Color(0xFFF0F4FC)

// ── Device Colors ────────────────────────────────────────────────────────────
val MacDeviceColor      = Color(0xFFFFFFFF)
val AndroidDeviceColor  = Color(0xFFD1D1D6)
val WindowsDeviceColor  = Color(0xFFAEAEC0)
val UnknownDeviceColor  = Color(0xFF8E8E93)

// ── Status ───────────────────────────────────────────────────────────────────
val PausedAmber = Color(0xFFD1D1D6)
val StopRed     = Color(0xFFEF4444)

// ── AI Chat ──────────────────────────────────────────────────────────────────
val AiViolet    = AccentBlue
val AiIndigo    = AccentBlueDim
val ChatSurface = Surface0
val InputBarGlass = Surface1
val AiOrbGlow   = Color(0x553B82F6)

// ── Gradients ────────────────────────────────────────────────────────────────
val PremiumBlueGradient = Brush.verticalGradient(
    listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
)
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

// ── AI Chat Gradients ────────────────────────────────────────────────────────
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
