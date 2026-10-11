package com.agriedge.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkGreen = Color(0xFF1B4332)
val MediumGreen = Color(0xFF2D6A4F)
val LightGreen = Color(0xFF40916C)
val MintBackground = Color(0xFFD8F3DC)
val LightSurface = Color(0xFFFFFFFF)
val TextDark = Color(0xFF1F2937)

private val LightColorScheme = lightColorScheme(
    primary = DarkGreen,
    onPrimary = Color.White,
    primaryContainer = MintBackground,
    onPrimaryContainer = DarkGreen,
    secondary = MediumGreen,
    onSecondary = Color.White,
    background = Color(0xFFF8F9FA),
    onBackground = TextDark,
    surface = LightSurface,
    onSurface = TextDark
)

@Composable
fun AgriEdgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
