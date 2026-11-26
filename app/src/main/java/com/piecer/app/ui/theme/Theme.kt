package com.piecer.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val LightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = White,
    secondary = Charcoal,
    onSecondary = White,
    background = Snow,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Stone,
    outline = Mist,
    outlineVariant = Mist,
    error = Coral,
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Ink,
    secondary = Graphite,
    onSecondary = Snow,
    background = Ink,
    onBackground = Snow,
    surface = Charcoal,
    onSurface = Snow,
    surfaceVariant = Graphite,
    onSurfaceVariant = Stone,
    outline = Graphite,
    outlineVariant = Graphite,
    error = Coral,
    onError = Ink
)

object PiecerTheme {
    val colors: PiecerColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPiecerColors.current

    val spacing: PiecerSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalPiecerSpacing.current
}

@Composable
fun PiecerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val piecerColors = if (darkTheme) DarkPiecerColors else LightPiecerColors

    CompositionLocalProvider(
        LocalPiecerColors provides piecerColors,
        LocalPiecerSpacing provides PiecerSpacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PiecerTypography,
            shapes = PiecerShapes,
            content = content
        )
    }
}
