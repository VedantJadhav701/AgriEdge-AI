package com.agriedge.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palette: forest greens for trust and farming, warm cream for a calm field-day feel,
// amber for actions and warnings, clay for "needs attention".
val Forest = Color(0xFF1B4332)
val Leaf = Color(0xFF2D6A4F)
val Sprout = Color(0xFF52B788)
val Mint = Color(0xFFD8F3DC)
val Cream = Color(0xFFF7F5EF)
val Amber = Color(0xFFE9A03B)
val Clay = Color(0xFFB85C38)
val Ink = Color(0xFF1C2620)
val Muted = Color(0xFF65736A)
val Danger = Color(0xFFB3261E)

// Names used by older screens
val DarkGreen = Forest
val MediumGreen = Leaf

private val AgriScheme = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = Forest,
    secondary = Leaf,
    onSecondary = Color.White,
    tertiary = Amber,
    onTertiary = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEEF2EC),
    onSurfaceVariant = Muted,
    outline = Color(0xFFCAD4CC),
    error = Danger
)

private val AgriTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp)
)

@Composable
fun AgriEdgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AgriScheme,
        typography = AgriTypography,
        content = content
    )
}
