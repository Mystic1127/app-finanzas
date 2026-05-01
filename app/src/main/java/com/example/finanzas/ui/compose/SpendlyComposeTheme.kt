package com.example.finanzas.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SpendlyLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF236B4E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F0E3),
    onPrimaryContainer = Color(0xFF063820),
    secondary = Color(0xFF4F695A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7F1EA),
    onSecondaryContainer = Color(0xFF13251B),
    tertiary = Color(0xFF3F6B6B),
    onTertiary = Color.White,
    background = Color(0xFFF7FBF8),
    onBackground = Color(0xFF17211B),
    surface = Color.White,
    onSurface = Color(0xFF17211B),
    surfaceVariant = Color(0xFFE6F0EA),
    onSurfaceVariant = Color(0xFF516159),
    outline = Color(0xFFB8C8BF),
    error = Color(0xFFBA1A35),
    onError = Color.White,
    errorContainer = Color(0xFFFFD9DE),
    onErrorContainer = Color(0xFF40000B)
)

private val SpendlyDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF86DDB2),
    onPrimary = Color(0xFF062315),
    primaryContainer = Color(0xFF123626),
    onPrimaryContainer = Color(0xFFB8F2D4),
    secondary = Color(0xFFA6CDB6),
    onSecondary = Color(0xFF122219),
    secondaryContainer = Color(0xFF1B272B),
    onSecondaryContainer = Color(0xFFD5E0E3),
    tertiary = Color(0xFF9ED0CD),
    onTertiary = Color(0xFF062021),
    background = Color(0xFF0B141A),
    onBackground = Color(0xFFE9EEF0),
    surface = Color(0xFF111B1F),
    onSurface = Color(0xFFE9EEF0),
    surfaceVariant = Color(0xFF1B272B),
    onSurfaceVariant = Color(0xFFAAB4B8),
    outline = Color(0xFF51636A),
    error = Color(0xFFFFB3BE),
    onError = Color(0xFF4A0711),
    errorContainer = Color(0xFF7B2632),
    onErrorContainer = Color(0xFFFFDDE2)
)

@Composable
fun SpendlyComposeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) SpendlyDarkColors else SpendlyLightColors,
        content = content
    )
}
