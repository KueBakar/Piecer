package com.piecer.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.piecer.app.ui.theme.PiecerTheme

enum class PiecerCardVariant {
    Light,
    Dark
}

@Composable
fun PiecerCard(
    modifier: Modifier = Modifier,
    variant: PiecerCardVariant = PiecerCardVariant.Light,
    interactionSource: MutableInteractionSource? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = PiecerTheme.colors
    val spacing = PiecerTheme.spacing

    val containerColor = when (variant) {
        PiecerCardVariant.Light -> colors.cardLight
        PiecerCardVariant.Dark -> colors.cardDark
    }
    val contentColor = when (variant) {
        PiecerCardVariant.Light -> colors.onCardLight
        PiecerCardVariant.Dark -> colors.onCardDark
    }
    val borderColor = when (variant) {
        PiecerCardVariant.Light -> colors.cardLightBorder
        PiecerCardVariant.Dark -> colors.cardDarkBorder
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            color = containerColor,
            contentColor = contentColor,
            border = BorderStroke(spacing.borderWidth, borderColor),
            interactionSource = interactionSource
        ) {
            Column(
                modifier = Modifier.padding(spacing.cardPadding),
                content = content
            )
        }
    } else {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            color = containerColor,
            contentColor = contentColor,
            border = BorderStroke(spacing.borderWidth, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(spacing.cardPadding),
                content = content
            )
        }
    }
}
