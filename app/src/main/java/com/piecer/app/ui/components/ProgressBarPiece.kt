package com.piecer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.piecer.app.ui.theme.PiecerTheme

/**
 * Bilah progres dengan warna otomatis berdasarkan persentase (0f..1f+):
 * - < 70% : Mint (aman)
 * - 70%..99% : Amber (peringatan)
 * - >= 100% : Coral (batas tercapai / terlampaui)
 */
@Composable
fun ProgressBarPiece(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val colors = PiecerTheme.colors
    val spacing = PiecerTheme.spacing
    val clampedProgress = progress.coerceIn(0f, 1f)

    val indicatorColor = when {
        progress >= 1.0f -> colors.coral
        progress >= 0.70f -> colors.amber
        else -> colors.mint
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
                .fillMaxWidth(clampedProgress)
                .clip(CircleShape)
                .background(indicatorColor)
        )
    }
}
