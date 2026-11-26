package com.piecer.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class PiecerSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val cardPadding: Dp = 20.dp,
    val screenPadding: Dp = 16.dp,
    val buttonHeight: Dp = 48.dp,
    val iconButtonSize: Dp = 44.dp,
    val fabSize: Dp = 52.dp,
    val iconSm: Dp = 16.dp,
    val iconMd: Dp = 20.dp,
    val iconLg: Dp = 24.dp,
    val emptyIconContainer: Dp = 64.dp,
    val progressHeight: Dp = 8.dp,
    val borderWidth: Dp = 1.dp,
    val bottomBarHeight: Dp = 72.dp
)

val LocalPiecerSpacing = staticCompositionLocalOf { PiecerSpacing() }
