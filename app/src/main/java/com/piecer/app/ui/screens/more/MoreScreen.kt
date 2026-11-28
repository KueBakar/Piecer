package com.piecer.app.ui.screens.more

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.piecer.app.R
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.theme.LocalReduceMotion
import com.piecer.app.ui.theme.PiecerTheme

private data class MoreTileItem(
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val onClick: () -> Unit
)

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
    onOpenAbout: () -> Unit = {},
    onOpenWrapped: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current

    var hasAnimated by rememberSaveable { mutableStateOf(false) }
    val shouldAnimateEntrance = remember { !hasAnimated && !reduceMotion }
    LaunchedEffect(Unit) {
        hasAnimated = true
    }

    val financeItems = remember(
        onOpenWallets,
        onOpenPieces,
        onOpenBudgets,
        onOpenGoals,
        onOpenDebts,
        onOpenNetWorth
    ) {
        listOf(
            MoreTileItem(
                icon = Icons.Rounded.AccountBalanceWallet,
                titleRes = R.string.more_wallets_title,
                descRes = R.string.more_wallets_desc,
                onClick = onOpenWallets
            ),
            MoreTileItem(
                icon = Icons.Rounded.DonutLarge,
                titleRes = R.string.more_pieces_title,
                descRes = R.string.more_pieces_desc,
                onClick = onOpenPieces
            ),
            MoreTileItem(
                icon = Icons.Rounded.PieChart,
                titleRes = R.string.more_budgets_title,
                descRes = R.string.more_budgets_desc,
                onClick = onOpenBudgets
            ),
            MoreTileItem(
                icon = Icons.Rounded.Savings,
                titleRes = R.string.more_goals_title,
                descRes = R.string.more_goals_desc,
                onClick = onOpenGoals
            ),
            MoreTileItem(
                icon = Icons.Rounded.Handshake,
                titleRes = R.string.more_debts_title,
                descRes = R.string.more_debts_desc,
                onClick = onOpenDebts
            ),
            MoreTileItem(
                icon = Icons.Rounded.AccountBalance,
                titleRes = R.string.more_networth_title,
                descRes = R.string.more_networth_desc,
                onClick = onOpenNetWorth
            )
        )
    }

    val toolsItems = remember(
        onOpenRecurring,
        onOpenSplitBill,
        onOpenWrapped,
        onOpenCategories
    ) {
        listOf(
            MoreTileItem(
                icon = Icons.Rounded.Repeat,
                titleRes = R.string.more_recurring_title,
                descRes = R.string.more_recurring_desc,
                onClick = onOpenRecurring
            ),
            MoreTileItem(
                icon = Icons.Rounded.Groups,
                titleRes = R.string.more_split_title,
                descRes = R.string.more_split_desc,
                onClick = onOpenSplitBill
            ),
            MoreTileItem(
                icon = Icons.Rounded.AutoAwesome,
                titleRes = R.string.wrapped_title,
                descRes = R.string.wrapped_more_desc,
                onClick = onOpenWrapped
            ),
            MoreTileItem(
                icon = Icons.Rounded.Category,
                titleRes = R.string.more_categories_title,
                descRes = R.string.more_categories_desc,
                onClick = onOpenCategories
            )
        )
    }

    val appItems = remember(onOpenSettings, onOpenAbout) {
        listOf(
            MoreTileItem(
                icon = Icons.Rounded.Settings,
                titleRes = R.string.more_settings_title,
                descRes = R.string.more_settings_desc,
                onClick = onOpenSettings
            ),
            MoreTileItem(
                icon = Icons.Rounded.Info,
                titleRes = R.string.more_about_title,
                descRes = R.string.more_about_desc,
                onClick = onOpenAbout
            )
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = spacing.screenPadding,
            vertical = spacing.md
        ),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        item(span = { GridItemSpan(2) }) {
            Text(
                text = stringResource(id = R.string.nav_more),
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.onBackground
            )
        }

        moreSection(
            titleRes = R.string.more_section_finance,
            items = financeItems,
            indexOffset = 0,
            shouldAnimateEntrance = shouldAnimateEntrance,
            reduceMotion = reduceMotion
        )

        moreSection(
            titleRes = R.string.more_section_tools,
            items = toolsItems,
            indexOffset = financeItems.size,
            shouldAnimateEntrance = shouldAnimateEntrance,
            reduceMotion = reduceMotion
        )

        moreSection(
            titleRes = R.string.more_section_app,
            items = appItems,
            indexOffset = financeItems.size + toolsItems.size,
            shouldAnimateEntrance = shouldAnimateEntrance,
            reduceMotion = reduceMotion
        )
    }
}

private fun LazyGridScope.moreSection(
    @StringRes titleRes: Int,
    items: List<MoreTileItem>,
    indexOffset: Int,
    shouldAnimateEntrance: Boolean,
    reduceMotion: Boolean
) {
    item(span = { GridItemSpan(2) }) {
        val spacing = PiecerTheme.spacing
        Text(
            text = stringResource(id = titleRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.xs)
        )
    }

    itemsIndexed(
        items = items,
        key = { _, item -> item.titleRes }
    ) { localIndex, item ->
        MoreGridTile(
            item = item,
            staggerIndex = indexOffset + localIndex,
            shouldAnimateEntrance = shouldAnimateEntrance,
            reduceMotion = reduceMotion
        )
    }
}

@Composable
private fun MoreGridTile(
    item: MoreTileItem,
    staggerIndex: Int,
    shouldAnimateEntrance: Boolean,
    reduceMotion: Boolean
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    val entranceProgress = remember { Animatable(if (shouldAnimateEntrance) 0f else 1f) }
    LaunchedEffect(shouldAnimateEntrance, reduceMotion) {
        if (shouldAnimateEntrance && !reduceMotion) {
            entranceProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 220,
                    delayMillis = staggerIndex * 40,
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            entranceProgress.snapTo(1f)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) 0.96f else 1f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "moreTilePress"
    )

    PiecerCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                val e = entranceProgress.value
                val entranceScale = 0.92f + 0.08f * e
                alpha = e
                scaleX = entranceScale * pressScale
                scaleY = entranceScale * pressScale
            },
        variant = PiecerCardVariant.Light,
        interactionSource = interactionSource,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            item.onClick()
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.iconButtonSize)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(spacing.iconMd)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xxs)
            ) {
                Text(
                    text = stringResource(id = item.titleRes),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.onCardLight,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(id = item.descRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
