package com.gymapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Paleta compartilhada ──────────────────────────────────────────────────────
private val White        = Color(0xFFFFFFFF)
private val Black        = Color(0xFF000000)
private val DarkGray     = Color(0xFF424242)
private val MediumGray   = Color(0xFF757575)
private val LightGray    = Color(0xFF9E9E9E)
private val SurfaceGray  = Color(0xFFF5F5F5)

// Cores dark
private val DarkBg           = Color(0xFF121212)
private val DarkSurface      = Color(0xFF1E1E1E)
private val DarkSurfaceVar   = Color(0xFF2C2C2C)
private val DarkOnSurface    = Color(0xFFE0E0E0)
private val DarkOnSurfaceVar = Color(0xFFBDBDBD)
private val DarkOutline      = Color(0xFF555555)

// ── Light scheme ─────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary                 = DarkGray,
    onPrimary               = White,
    primaryContainer        = SurfaceGray,
    onPrimaryContainer      = Black,
    secondary               = MediumGray,
    onSecondary             = Black,
    secondaryContainer      = SurfaceGray,
    onSecondaryContainer    = Black,
    tertiary                = LightGray,
    onTertiary              = Black,
    tertiaryContainer       = SurfaceGray,
    onTertiaryContainer     = Black,
    background              = White,
    onBackground            = Black,
    surface                 = White,
    onSurface               = Black,
    surfaceVariant          = SurfaceGray,
    onSurfaceVariant        = DarkGray,
    surfaceContainerLowest  = White,
    surfaceContainerLow     = White,
    surfaceContainer        = White,
    surfaceContainerHigh    = White,
    surfaceContainerHighest = White,
    outline                 = MediumGray,
    outlineVariant          = LightGray,
    error                   = Color(0xFFB00020),
    onError                 = White,
    errorContainer          = Color(0xFFFFDAD6),
    onErrorContainer        = Color(0xFF410002),
    inverseSurface          = DarkGray,
    inverseOnSurface        = White,
    inversePrimary          = LightGray,
    scrim                   = Black,
)

// ── Dark scheme ───────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary                 = DarkOnSurface,
    onPrimary               = DarkBg,
    primaryContainer        = DarkSurfaceVar,
    onPrimaryContainer      = DarkOnSurface,
    secondary               = DarkOnSurfaceVar,
    onSecondary             = DarkBg,
    secondaryContainer      = DarkSurfaceVar,
    onSecondaryContainer    = DarkOnSurface,
    tertiary                = DarkOutline,
    onTertiary              = DarkOnSurface,
    tertiaryContainer       = DarkSurfaceVar,
    onTertiaryContainer     = DarkOnSurface,
    background              = DarkBg,
    onBackground            = DarkOnSurface,
    surface                 = DarkSurface,
    onSurface               = DarkOnSurface,
    surfaceVariant          = DarkSurfaceVar,
    onSurfaceVariant        = DarkOnSurfaceVar,
    surfaceContainerLowest  = DarkBg,
    surfaceContainerLow     = DarkSurface,
    surfaceContainer        = DarkSurface,
    surfaceContainerHigh    = DarkSurfaceVar,
    surfaceContainerHighest = DarkSurfaceVar,
    outline                 = DarkOutline,
    outlineVariant          = Color(0xFF3A3A3A),
    error                   = Color(0xFFCF6679),
    onError                 = DarkBg,
    errorContainer          = Color(0xFF93000A),
    onErrorContainer        = Color(0xFFFFDAD6),
    inverseSurface          = DarkOnSurface,
    inverseOnSurface        = DarkBg,
    inversePrimary          = DarkGray,
    scrim                   = Black,
)

@Composable
fun GymAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = Typography,
        content     = content
    )
}