package com.piecer.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.piecer.app.R
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.ui.theme.PiecerTheme

data class TransactionDisplayItem(
    val transaction: TransactionEntity,
    val title: String,
    val subtitle: String,
    val isScheduled: Boolean = false
)

@Composable
fun TransactionRowItem(
    item: TransactionDisplayItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAmountHidden: Boolean = false,
    onDeleteClick: (() -> Unit)? = null
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors

    val tone = when (item.transaction.type) {
        TransactionType.INCOME -> AmountTone.Income
        TransactionType.EXPENSE -> AmountTone.Expense
        TransactionType.TRANSFER -> AmountTone.Neutral
    }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.cardLight,
        contentColor = colors.onCardLight,
        border = BorderStroke(spacing.borderWidth, colors.cardLightBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onCardLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.isScheduled) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = colors.amber.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = stringResource(id = R.string.tx_scheduled_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.amber,
                                modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.xxs)
                            )
                        }
                    }
                }
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(spacing.sm))

            AmountText(
                amount = item.transaction.amount,
                isHidden = isAmountHidden,
                tone = tone,
                style = MaterialTheme.typography.titleMedium
            )

            if (onDeleteClick != null) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(spacing.iconButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(id = R.string.history_delete),
                        tint = colors.mutedText,
                        modifier = Modifier.size(spacing.iconMd)
                    )
                }
            }
        }
    }
}
