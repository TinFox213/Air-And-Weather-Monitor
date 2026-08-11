package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SleekColorScheme = lightColorScheme(
    primary = SleekPrimary,
    onPrimary = SleekOnPrimary,
    primaryContainer = SleekHighlight,
    onPrimaryContainer = SleekPrimary,
    secondaryContainer = SleekSurface,
    background = SleekBackground,
    onBackground = SleekOnBackground,
    surface = SleekBackground,
    onSurface = SleekOnBackground,
    surfaceVariant = SleekSurface,
    onSurfaceVariant = SleekTextSecondary,
    outline = SleekBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // We strictly use the sleek color scheme to match the design aesthetic
    val colorScheme = SleekColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
