package com.piecer.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PiecerColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = White,
    background = Snow,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Stone,
    outline = Mist
)

@Composable
fun PiecerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PiecerColorScheme,
        content = content
    )
}
