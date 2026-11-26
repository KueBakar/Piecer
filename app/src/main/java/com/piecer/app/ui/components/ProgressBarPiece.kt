package com.piecer.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.piecer.app.ui.theme.LocalReduceMotion
import com.piecer.app.ui.theme.PiecerTheme

/**
 * Bilah progres dengan warna otomatis berdasarkan persentase (0f..1f+):
 * - Untuk anggaran (isGoal = false): < 70% Mint, 70%..99% Amber, >= 100% Coral
 * - Untuk target tabungan (isGoal = true): selalu Mint
 */
@Composable
fun ProgressBarPiece(
    progress: Float,
    modifier: Modifier = Modifier,
    isGoal: Boolean = false
) {
    val colors = PiecerTheme.colors
    val spacing = PiecerTheme.spacing
    val reduceMotion = LocalReduceMotion.current
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 250),
        label = "progress_bar_animation"
    )
    val displayProgress = if (reduceMotion) clampedProgress else animatedProgress

    val indicatorColor = if (isGoal) {
        colors.mint
    } else {
        when {
            progress >= 1.0f -> colors.coral
            progress >= 0.70f -> colors.amber
            else -> colors.mint
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.progressHeight)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(displayProgress)
                .clip(CircleShape)
                .background(indicatorColor)
        )
    }
}
