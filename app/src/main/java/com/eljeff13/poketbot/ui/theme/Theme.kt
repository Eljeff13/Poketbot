package com.eljeff13.poketbot.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// The workshop palette: dark metal with neon coolant.
val Ink = Color(0xFF080C11)
val Panel = Color(0xFF121A23)
val PanelHigh = Color(0xFF1B2733)
val Coolant = Color(0xFF37E2C4)
val Amber = Color(0xFFFFB020)
val Danger = Color(0xFFFF5A5F)
val TextPrimary = Color(0xFFE8F1F6)
val TextMuted = Color(0xFF8FA3B3)

private val PoketbotColors = darkColorScheme(
    primary = Coolant,
    onPrimary = Ink,
    secondary = Amber,
    onSecondary = Ink,
    error = Danger,
    onError = Ink,
    background = Ink,
    onBackground = TextPrimary,
    surface = Panel,
    onSurface = TextPrimary,
    surfaceVariant = PanelHigh,
    onSurfaceVariant = TextMuted,
    outline = Color(0xFF2C3B4A),
)

private val PoketbotTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        letterSpacing = 2.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.5.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 1.sp,
    ),
)

@Composable
fun PoketbotTheme(content: @Composable () -> Unit) {
    // The game commits to a single dark look regardless of the system setting.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = PoketbotColors,
        typography = PoketbotTypography,
        content = content,
    )
}
