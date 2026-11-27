package com.piecer.app.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.piecer.app.R
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.theme.PiecerTheme

@Composable
fun MoreScreen(
    onOpenPieces: () -> Unit,
    onOpenWallets: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenSplitBill: () -> Unit,
    onOpenNetWorth: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWrapped: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = spacing.screenPadding,
            vertical = spacing.md
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.nav_more),
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.onBackground
            )
        }

        item {
            MoreMenuCard(
                icon = Icons.Rounded.DonutLarge,
                title = stringResource(id = R.string.more_pieces_title),
                subtitle = stringResource(id = R.string.more_pieces_desc),
                onClick = onOpenPieces
            )
        }

        item {
            MoreMenuCard(
                icon = Icons.Rounded.AccountBalanceWallet,
                title = stringResource(id = R.string.more_wallets_title),
                subtitle = stringResource(id = R.string.more_wallets_desc),
                onClick = onOpenWallets
            )
        }

        item {
            MoreMenuCard(
                icon = Icons.Rounded.Category,
                title = stringResource(id = R.string.more_categories_title),
                subtitle = stringResource(id = R.string.more_categories_desc),
                onClick = onOpenCategories
            )
        }

        item {
            MoreMenuCard(
                icon = Icons.Rounded.Handshake,
                title = stringResource(id = R.string.more_debts_title),
                subtitle = stringResource(id = R.string.more_debts_desc),
                onClick = onOpenDebts
            )
        }

        item {
            MoreMenuCard(
                icon = Icons.Rounded.Groups,
                title = stringResource(id = R.string.more_split_title),
                subtitle = stringResource(id = R.string.more_split_desc),
                onClick = onOpenSplitBill
            )
        }

        item {
            MoreMenuCard(
                icon = Icons.Rounded.AccountBalance,
                title = stringResource(id = R.string.more_networth_title),
                subtitle = stringResource(id = R.string.more_networth_desc),
                onClick = onOpenNetWorth
            )
        }

        item {
            MoreMenuCard(
                icon = Icons.Rounded.PieChart,
                title = stringResource(id = R.string.more_budgets_title),
                subtitle = stringResource(id = R.string.more_budgets_desc),
                onClick = onOpenBudgets
            )
        }

        item {
            MoreMenuCard(
                icon = Icons.Rounded.Savings,
                title = stringResource(id = R.string.more_goals_title),
                subtitle = stringResource(id = R.string.more_goals_desc),
                onClick = onOpenGoals
            )
        }

        item {
            MoreMenuCard(
                icon = Icons.Rounded.Repeat,
                title = stringResource(id = R.string.more_recurring_title),
                subtitle = stringResource(id = R.string.more_recurring_desc),
                onClick = onOpenRecurring
            )
        }

        item {
            MoreMenuCard(
                icon = Icons.Rounded.AutoAwesome,
                title = stringResource(id = R.string.wrapped_title),
                subtitle = stringResource(id = R.string.wrapped_more_desc),
                onClick = onOpenWrapped
            )
        }

        item {
            MoreMenuCard(
                icon = Icons.Rounded.Settings,
                title = stringResource(id = R.string.more_settings_title),
                subtitle = stringResource(id = R.string.more_settings_desc),
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun MoreMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    PiecerCard(
        modifier = Modifier.fillMaxWidth(),
        variant = PiecerCardVariant.Light,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.iconButtonSize)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(spacing.iconMd)
                )
            }
            Spacer(modifier = Modifier.width(spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onCardLight
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.mutedText,
                modifier = Modifier.size(spacing.iconLg)
            )
        }
    }
}
