package com.devmarkabrasaldo.PataGilid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PataGilidDarkColorScheme = darkColorScheme(
    primary = GliderBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1D4ED8),
    onPrimaryContainer = Color.White,
    secondary = SummitSteel,
    onSecondary = Color(0xFF0F172A),
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SummitSteel,
    error = Color(0xFFE57373),
    onError = Color.White
)

@Composable
fun PataGilidTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PataGilidDarkColorScheme,
        content = content
    )
}
