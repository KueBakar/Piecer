package com.piecer.app.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.R
import com.piecer.app.ui.components.AmountText
import com.piecer.app.ui.components.AmountTone
import com.piecer.app.ui.components.EmptyState
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.components.PiecerIconButton
import com.piecer.app.ui.components.ProgressBarPiece
import com.piecer.app.ui.components.TransactionRowItem
import com.piecer.app.ui.components.formatRupiah
import com.piecer.app.ui.screens.pieces.PiecesSegmentedDonut
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.BudgetStatus
import com.piecer.app.util.DailyExpensePoint

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSeeAllTransactions: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenPieces: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = spacing.screenPadding,
            vertical = spacing.md
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = greetingText(uiState.greetingPeriod),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorScheme.onBackground
                )
                PiecerIconButton(
                    icon = if (uiState.isBalanceHidden) {
                        Icons.Rounded.VisibilityOff
                    } else {
                        Icons.Rounded.Visibility
                    },
                    contentDescription = stringResource(
                        id = if (uiState.isBalanceHidden) {
                            R.string.action_show_amount
                        } else {
                            R.string.action_hide_amount
                        }
                    ),
                    onClick = viewModel::toggleBalanceVisibility
                )
            }
        }

        item {
            PiecerCard(
                modifier = Modifier.fillMaxWidth(),
                variant = PiecerCardVariant.Dark
            ) {
                Text(
                    text = stringResource(id = R.string.home_total_balance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mutedText
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                AmountText(
                    amount = uiState.totalBalance,
                    isHidden = uiState.isBalanceHidden,
                    style = MaterialTheme.typography.displayLarge,
                    animate = true
                )
                Spacer(modifier = Modifier.height(spacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.lg)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.home_income_month),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.mutedText
                        )
                        Spacer(modifier = Modifier.height(spacing.xxs))
                        AmountText(
                            amount = uiState.monthlyIncome,
                            isHidden = uiState.isBalanceHidden,
                            tone = AmountTone.Income,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.home_expense_month),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.mutedText
                        )
                        Spacer(modifier = Modifier.height(spacing.xxs))
                        AmountText(
                            amount = uiState.monthlyExpense,
                            isHidden = uiState.isBalanceHidden,
                            tone = AmountTone.Expense,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        item {
            PiecerCard(
                modifier = Modifier.fillMaxWidth(),
                variant = PiecerCardVariant.Light
            ) {
                Text(
                    text = stringResource(id = R.string.home_daily_allowance_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onCardLight
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                AmountText(
                    amount = uiState.dailyAllowance,
                    isHidden = uiState.isBalanceHidden,
                    tone = if (uiState.dailyAllowance > 0L) AmountTone.Income else AmountTone.Neutral,
                    style = MaterialTheme.typography.headlineLarge,
                    animate = true
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = stringResource(
                        id = R.string.home_daily_allowance_sub,
                        uiState.remainingDaysInMonth
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mutedText
                )
            }
        }

        if (uiState.piecesEnabled && uiState.pieceSummaries.isNotEmpty()) {
            item {
                PiecerCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = PiecerCardVariant.Light,
                    onClick = onOpenPieces
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        PiecesSegmentedDonut(
                            summaries = uiState.pieceSummaries,
                            size = 64.dp,
                            strokeWidth = 10.dp
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(spacing.xs)
                        ) {
                            Text(
                                text = stringResource(id = R.string.home_pieces_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onCardLight
                            )
                            uiState.lowestPieces.forEach { summary ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = summary.piece.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.mutedText
                                    )
                                    AmountText(
                                        amount = summary.remaining,
                                        isHidden = uiState.isBalanceHidden,
                                        tone = if (summary.remaining >= 0L) AmountTone.Income else AmountTone.Expense,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.insights.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(
                        text = stringResource(id = R.string.home_insights_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onBackground
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        items(uiState.insights) { insight ->
                            PiecerCard(
                                modifier = Modifier.width(260.dp),
                                variant = PiecerCardVariant.Light
                            ) {
                                Text(
                                    text = insight.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.mutedText
                                )
                                Spacer(modifier = Modifier.height(spacing.xs))
                                Text(
                                    text = insight.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onCardLight
                                )
                            }
                        }
                    }
                }
            }
        }

        uiState.criticalBudget?.let { critical ->
            item {
                val badgeColor = when (critical.progress.status) {
                    BudgetStatus.SAFE -> colors.mint
                    BudgetStatus.WARNING -> colors.amber
                    BudgetStatus.EXCEEDED -> colors.coral
                }
                PiecerCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = PiecerCardVariant.Light,
                    onClick = onOpenBudgets
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.home_budget_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onCardLight
                        )
                        Text(
                            text = stringResource(
                                id = R.string.budgets_percent_label,
                                critical.progress.percentageInt
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = badgeColor
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = stringResource(
                            id = R.string.home_budget_sub,
                            critical.categoryName,
                            formatRupiah(critical.progress.spent),
                            formatRupiah(critical.progress.limit)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(spacing.sm))
                    ProgressBarPiece(progress = critical.progress.progressRatio)
                }
            }
        }

        item {
            PiecerCard(
                modifier = Modifier.fillMaxWidth(),
                variant = PiecerCardVariant.Light
            ) {
                Text(
                    text = stringResource(id = R.string.home_weekly_chart_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onCardLight
                )
                Spacer(modifier = Modifier.height(spacing.lg))
                if (uiState.weeklyExpenses.all { it.totalExpense == 0L }) {
                    Text(
                        text = stringResource(id = R.string.home_weekly_chart_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedText,
                        modifier = Modifier.padding(vertical = spacing.md)
                    )
                } else {
                    WeeklyExpenseBarChart(
                        points = uiState.weeklyExpenses,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (uiState.upcomingTransactions.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(id = R.string.home_upcoming_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onBackground
                )
            }
            items(
                items = uiState.upcomingTransactions,
                key = { "upcoming_${it.transaction.id}" }
            ) { item ->
                TransactionRowItem(
                    item = item,
                    onClick = { onEditTransaction(item.transaction.id) },
                    isAmountHidden = uiState.isBalanceHidden
                )
            }
        }

        if (!uiState.hasAnyTransaction) {
            item {
                PiecerCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = PiecerCardVariant.Light
                ) {
                    EmptyState(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        title = stringResource(id = R.string.home_empty_title),
                        description = stringResource(id = R.string.home_empty_desc)
                    )
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.home_recent_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = colorScheme.onBackground
                    )
                    TextButton(onClick = onSeeAllTransactions) {
                        Text(
                            text = stringResource(id = R.string.home_see_all),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            items(
                items = uiState.recentTransactions,
                key = { it.transaction.id }
            ) { item ->
                TransactionRowItem(
                    item = item,
                    onClick = { onEditTransaction(item.transaction.id) },
                    isAmountHidden = uiState.isBalanceHidden
                )
            }
        }
    }
}

@Composable
private fun WeeklyExpenseBarChart(
    points: List<DailyExpensePoint>,
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val maxExpense = points.maxOfOrNull { it.totalExpense }?.coerceAtLeast(1L) ?: 1L

    val barColorDefault = colorScheme.surfaceVariant
    val barColorToday = colors.coral

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val count = points.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.45f).coerceAtMost(28.dp.toPx())
            val minBarHeight = 6.dp.toPx()
            val cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())

            points.forEachIndexed { index, point ->
                val ratio = if (point.totalExpense <= 0L) {
                    0f
                } else {
                    point.totalExpense.toFloat() / maxExpense.toFloat()
                }
                val barHeight = (size.height * ratio).coerceAtLeast(
                    if (point.totalExpense > 0L) minBarHeight else 4.dp.toPx()
                )
                val left = index * slotWidth + (slotWidth - barWidth) / 2f
                val top = size.height - barHeight

                drawRoundRect(
                    color = if (point.isToday) barColorToday else barColorDefault,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRadius
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.sm))

        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { point ->
                Text(
                    text = point.dayShortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (point.isToday) colors.onCardLight else colors.mutedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun greetingText(period: GreetingPeriod): String {
    return when (period) {
        GreetingPeriod.MORNING -> stringResource(id = R.string.home_greeting_morning)
        GreetingPeriod.AFTERNOON -> stringResource(id = R.string.home_greeting_afternoon)
        GreetingPeriod.EVENING -> stringResource(id = R.string.home_greeting_evening)
        GreetingPeriod.NIGHT -> stringResource(id = R.string.home_greeting_night)
    }
}
