package com.gymapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Tema em branco, preto e cinza para máxima performance
private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF424242), // Cinza escuro
    secondary = androidx.compose.ui.graphics.Color(0xFF757575), // Cinza médio
    tertiary = androidx.compose.ui.graphics.Color(0xFF9E9E9E), // Cinza claro
    background = androidx.compose.ui.graphics.Color.White,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF5F5F5), // Cinza muito claro
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    onTertiary = androidx.compose.ui.graphics.Color.Black,
    onBackground = androidx.compose.ui.graphics.Color.Black,
    onSurface = androidx.compose.ui.graphics.Color.Black,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF424242)
)

@Composable
fun GymAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

