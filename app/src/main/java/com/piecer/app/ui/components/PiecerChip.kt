package com.piecer.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.piecer.app.ui.theme.PiecerTheme

@Composable
fun PiecerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme

    val containerColor = if (selected) colorScheme.primary else colorScheme.surface
    val contentColor = if (selected) colorScheme.onPrimary else colorScheme.onSurface
    val border = if (selected) null else BorderStroke(spacing.borderWidth, colorScheme.outline)

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = border
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(
                horizontal = spacing.lg,
                vertical = spacing.sm
            )
        )
    }
}
