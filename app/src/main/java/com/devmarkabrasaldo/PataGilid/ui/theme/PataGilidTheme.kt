package com.devmarkabrasaldo.PataGilid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PataGilidLightColorScheme = lightColorScheme(
    primary = GliderBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = SummitSteel,
    onSecondary = Color.White,
    background = Color(0xFFF2F4F8),
    onBackground = Color(0xFF1E293B),
    surface = Color.White,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color.White,
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFE57373),
    onError = Color.White
)

@Composable
fun PataGilidTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PataGilidLightColorScheme,
        content = content
    )
}
