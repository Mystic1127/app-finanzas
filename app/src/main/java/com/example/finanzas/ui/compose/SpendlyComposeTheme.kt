package com.example.finanzas.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SpendlyLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF7BC47F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFF3E1),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF5FA764),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7F5E8),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFF36C2CF),
    onTertiary = Color.White,
    background = Color(0xFFF8FAF8),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFDFF3E1),
    onSurfaceVariant = Color(0xFF667085),
    outline = Color(0xFFE7F5E8),
    error = Color(0xFFFF6B6B),
    onError = Color.White,
    errorContainer = Color(0xFFFFD9DE),
    onErrorContainer = Color(0xFF40000B)
)

private val SpendlyDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF19B47A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0D1B2A),
    onPrimaryContainer = Color(0xFFF8FAFC),
    secondary = Color(0xFF36D399),
    onSecondary = Color(0xFF07111A),
    secondaryContainer = Color(0xFF17324A),
    onSecondaryContainer = Color(0xFFF8FAFC),
    tertiary = Color(0xFF22D3EE),
    onTertiary = Color(0xFF07111A),
    background = Color(0xFF07111A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0D1B2A),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF17324A),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF17324A),
    outlineVariant = Color(0xFF123348),
    error = Color(0xFFFF7A8A),
    onError = Color(0xFF410009),
    errorContainer = Color(0xFF5A121D),
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
