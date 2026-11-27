package com.piecer.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Palet utama monokrom (RULES.md)
val Ink = Color(0xFF0D0D0F)
val Charcoal = Color(0xFF1C1C1E)
val Graphite = Color(0xFF2C2C2E)
val Snow = Color(0xFFF4F4F5)
val White = Color(0xFFFFFFFF)
val Mist = Color(0xFFE4E4E7)
val Stone = Color(0xFF8E8E93)

// Warna khusus brand splash (selaras dengan ic_launcher)
val SplashInk = Ink
val SplashWhite = White

// Aksen khusus makna uang (RULES.md)
val Mint = Color(0xFF34D399)
val Coral = Color(0xFFF87171)
val Amber = Color(0xFFFBBF24)

@Immutable
data class PiecerColors(
    val cardLight: Color,
    val onCardLight: Color,
    val cardLightBorder: Color,
    val cardDark: Color,
    val onCardDark: Color,
    val cardDarkBorder: Color,
    val mutedText: Color,
    val mint: Color,
    val coral: Color,
    val amber: Color,
    val splashPieColors: List<Color>
)

val LightPiecerColors = PiecerColors(
    cardLight = White,
    onCardLight = Ink,
    cardLightBorder = Mist,
    cardDark = Charcoal,
    onCardDark = White,
    cardDarkBorder = Graphite,
    mutedText = Stone,
    mint = Mint,
    coral = Coral,
    amber = Amber,
    splashPieColors = listOf(Ink, Charcoal, Graphite, Stone, Mint)
)

val DarkPiecerColors = PiecerColors(
    cardLight = Charcoal,
    onCardLight = Snow,
    cardLightBorder = Graphite,
    cardDark = Ink,
    onCardDark = Snow,
    cardDarkBorder = Graphite,
    mutedText = Stone,
    mint = Mint,
    coral = Coral,
    amber = Amber,
    splashPieColors = listOf(Snow, Mist, Stone, Graphite, Mint)
)

val LocalPiecerColors = staticCompositionLocalOf { LightPiecerColors }
