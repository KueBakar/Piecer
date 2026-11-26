package com.piecer.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.piecer.app.ui.theme.PiecerTheme

enum class PiecerButtonVariant {
    Primary,
    Secondary
}

@Composable
fun PiecerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PiecerButtonVariant = PiecerButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme

    val containerColor = when (variant) {
        PiecerButtonVariant.Primary -> colorScheme.primary
        PiecerButtonVariant.Secondary -> colorScheme.surface
    }
    val contentColor = when (variant) {
        PiecerButtonVariant.Primary -> colorScheme.onPrimary
        PiecerButtonVariant.Secondary -> colorScheme.onSurface
    }
    val border = when (variant) {
        PiecerButtonVariant.Primary -> null
        PiecerButtonVariant.Secondary -> BorderStroke(spacing.borderWidth, colorScheme.outline)
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(spacing.buttonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = colorScheme.surfaceVariant,
            disabledContentColor = colorScheme.onSurfaceVariant
        ),
        border = border,
        contentPadding = PaddingValues(horizontal = spacing.xl),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = spacing.xxs.minus(spacing.xxs),
            pressedElevation = spacing.xxs.minus(spacing.xxs)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(spacing.iconMd)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun PiecerIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PiecerButtonVariant = PiecerButtonVariant.Secondary,
    enabled: Boolean = true
) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme

    val containerColor = when (variant) {
        PiecerButtonVariant.Primary -> colorScheme.primary
        PiecerButtonVariant.Secondary -> colorScheme.surface
    }
    val contentColor = when (variant) {
        PiecerButtonVariant.Primary -> colorScheme.onPrimary
        PiecerButtonVariant.Secondary -> colorScheme.onSurface
    }
    val border = when (variant) {
        PiecerButtonVariant.Primary -> null
        PiecerButtonVariant.Secondary -> BorderStroke(spacing.borderWidth, colorScheme.outline)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.size(spacing.iconButtonSize),
        enabled = enabled,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = border
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(spacing.iconMd)
            )
        }
    }
}
