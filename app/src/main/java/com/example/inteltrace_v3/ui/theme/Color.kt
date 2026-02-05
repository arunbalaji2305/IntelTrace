package com.example.inteltrace_v3.ui.theme

import androidx.compose.ui.graphics.Color

// Apple-inspired Color Palette

// Primitives
val SystemBlue = Color(0xFF007AFF)
val SystemGreen = Color(0xFF34C759)
val SystemRed = Color(0xFFFF3B30)
val SystemOrange = Color(0xFFFF9500)
val SystemYellow = Color(0xFFFFCC00)
val SystemTeal = Color(0xFF5AC8FA)
val SystemIndigo = Color(0xFF5856D6)
val SystemPurple = Color(0xFFAF52DE)
val SystemPink = Color(0xFFFF2D55)

// Greys
val SystemGray = Color(0xFF8E8E93)
val SystemGray2 = Color(0xFFAEAEB2)
val SystemGray3 = Color(0xFFC7C7CC)
val SystemGray4 = Color(0xFFD1D1D6)
val SystemGray5 = Color(0xFFE5E5EA)
val SystemGray6 = Color(0xFFF2F2F7)

// Text Colors
val LabelPrimary = Color(0xFF000000)
val LabelSecondary = Color(0xFF3C3C43) // High contrast secondary
val LabelTertiary = Color(0xFF3C3C43).copy(alpha = 0.6f)
val LabelQuaternary = Color(0xFF3C3C43).copy(alpha = 0.3f)

// Background Colors
val SystemBackground = Color(0xFFF2F2F7) // Default background for grouped view
val SystemGroupedBackground = Color(0xFFF2F2F7)
val SecondarySystemBackground = Color(0xFFF2F2F7)
val CardBackground = Color(0xFFFFFFFF)

// Dark Mode variants
val LabelPrimaryDark = Color(0xFFFFFFFF)
val SystemBackgroundDark = Color(0xFF000000)
val CardBackgroundDark = Color(0xFF1C1C1E)

// New Theme Colors mapping
val PrimaryColor = SystemBlue
val SecondaryColor = SystemGray2
val BackgroundColor = SystemBackground
val SurfaceColor = CardBackground
val OnPrimaryColor = Color.White
val OnBackgroundColor = LabelPrimary
val OnSurfaceColor = LabelPrimary

// Legacy colors to prevent build errors if referenced elsewhere before full migration
val Purple80 = SystemBlue.copy(alpha=0.5f)
val PurpleGrey80 = SystemGray2
val Pink80 = SystemRed.copy(alpha=0.5f)
val Purple40 = SystemBlue
val PurpleGrey40 = SystemGray
val Pink40 = SystemRed