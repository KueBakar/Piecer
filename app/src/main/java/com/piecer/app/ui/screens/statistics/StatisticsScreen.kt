package com.piecer.app.ui.screens.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.R
import com.piecer.app.ui.components.AmountText
import com.piecer.app.ui.components.AmountTone
import com.piecer.app.ui.components.EmptyState
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.components.PiecerChip
import com.piecer.app.ui.components.formatRupiah
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.CategoryExpenseSlice
import com.piecer.app.util.StatsPeriodType
import com.piecer.app.util.TrendPoint
import kotlin.math.abs

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onCategoryClick: (categoryId: Long, categoryName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    val palette = listOf(
        colorScheme.onBackground,
        colors.coral,
        colorScheme.secondary,
        colors.amber,
        colors.mutedText,
        colors.mint,
        colorScheme.outline
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = spacing.screenPadding,
            vertical = spacing.md
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.nav_statistics),
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.onBackground
            )
        }

        item {
            val periodOptions = listOf(
                StatsPeriodType.WEEK to R.string.stats_period_week,
                StatsPeriodType.MONTH to R.string.stats_period_month,
                StatsPeriodType.YEAR to R.string.stats_period_year
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                items(periodOptions, key = { it.first.name }) { (type, labelRes) ->
                    PiecerChip(
                        label = stringResource(id = labelRes),
                        selected = uiState.periodType == type,
                        onClick = { viewModel.onPeriodTypeSelect(type) }
                    )
                }
            }
        }

        item {
            PiecerCard(
                modifier = Modifier.fillMaxWidth(),
                variant = PiecerCardVariant.Light
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = viewModel::onPreviousPeriod) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = stringResource(id = R.string.stats_prev_period)
                        )
                    }
                    Text(
                        text = uiState.periodLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onCardLight
                    )
                    IconButton(
                        onClick = viewModel::onNextPeriod,
                        enabled = uiState.canGoNext
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = stringResource(id = R.string.stats_next_period),
                            tint = if (uiState.canGoNext) {
                                colors.onCardLight
                            } else {
                                colors.mutedText.copy(alpha = 0.4f)
                            }
                        )
                    }
                }
            }
        }

        if (!uiState.hasData) {
            item {
                PiecerCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = PiecerCardVariant.Light
                ) {
                    EmptyState(
                        icon = Icons.Rounded.BarChart,
                        title = stringResource(id = R.string.stats_empty_title),
                        description = stringResource(id = R.string.stats_empty_desc)
                    )
                }
            }
        } else {
            item {
                PiecerCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = PiecerCardVariant.Dark
                ) {
                    Text(
                        text = stringResource(id = R.string.stats_net_balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    AmountText(
                        amount = uiState.summary.netBalance,
                        tone = when {
                            uiState.summary.netBalance > 0L -> AmountTone.Income
                            uiState.summary.netBalance < 0L -> AmountTone.Expense
                            else -> AmountTone.Neutral
                        },
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(spacing.lg))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.lg)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.tx_type_income),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.mutedText
                            )
                            Spacer(modifier = Modifier.height(spacing.xxs))
                            AmountText(
                                amount = uiState.summary.totalIncome,
                                tone = AmountTone.Income,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.tx_type_expense),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.mutedText
                            )
                            Spacer(modifier = Modifier.height(spacing.xxs))
                            AmountText(
                                amount = uiState.summary.totalExpense,
                                tone = AmountTone.Expense,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.md))
                    val changePercent = uiState.summary.expenseChangePercent
                    val comparisonText = when {
                        changePercent == null -> stringResource(id = R.string.stats_compare_none)
                        changePercent > 0 -> stringResource(id = R.string.stats_compare_up, changePercent)
                        changePercent < 0 -> stringResource(id = R.string.stats_compare_down, abs(changePercent))
                        else -> stringResource(id = R.string.stats_compare_same)
                    }
                    val comparisonColor = when {
                        changePercent == null || changePercent == 0 -> colors.mutedText
                        changePercent > 0 -> colors.coral
                        else -> colors.mint
                    }
                    Text(
                        text = comparisonText,
                        style = MaterialTheme.typography.labelMedium,
                        color = comparisonColor
                    )
                }
            }

            if (uiState.categorySlices.isNotEmpty()) {
                item {
                    PiecerCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = PiecerCardVariant.Light
                    ) {
                        Text(
                            text = stringResource(id = R.string.stats_donut_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onCardLight
                        )
                        Spacer(modifier = Modifier.height(spacing.lg))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.lg)
                        ) {
                            PiecesDonutChart(
                                slices = uiState.categorySlices,
                                palette = palette,
                                trackColor = colorScheme.surfaceVariant,
                                modifier = Modifier.size(148.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(spacing.sm)
                            ) {
                                uiState.categorySlices.take(5).forEachIndexed { index, slice ->
                                    val dotColor = palette[index % palette.size]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(spacing.sm)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                        Spacer(modifier = Modifier.width(spacing.sm))
                                        Text(
                                            text = slice.categoryName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onCardLight,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(spacing.xs))
                                        Text(
                                            text = stringResource(
                                                id = R.string.budgets_percent_label,
                                                slice.percentageInt
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.mutedText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.trendPoints.any { it.totalExpense > 0L }) {
                item {
                    PiecerCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = PiecerCardVariant.Light
                    ) {
                        Text(
                            text = stringResource(id = R.string.stats_trend_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onCardLight
                        )
                        Spacer(modifier = Modifier.height(spacing.lg))
                        TrendBarChart(
                            points = uiState.trendPoints,
                            periodType = uiState.periodType,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (uiState.topCategories.isNotEmpty()) {
                item {
                    PiecerCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = PiecerCardVariant.Light
                    ) {
                        Text(
                            text = stringResource(id = R.string.stats_top_categories_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onCardLight
                        )
                        Spacer(modifier = Modifier.height(spacing.md))
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                            uiState.topCategories.forEachIndexed { index, slice ->
                                val dotColor = palette[index % palette.size]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable(enabled = slice.categoryId > 0L) {
                                            onCategoryClick(slice.categoryId, slice.categoryName)
                                        }
                                        .padding(vertical = spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(spacing.md)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                    Spacer(modifier = Modifier.width(spacing.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = slice.categoryName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.onCardLight
                                        )
                                        Text(
                                            text = stringResource(
                                                id = R.string.budgets_percent_label,
                                                slice.percentageInt
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.mutedText
                                        )
                                    }
                                    Text(
                                        text = formatRupiah(slice.totalAmount),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.onCardLight
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PiecesDonutChart(
    slices: List<CategoryExpenseSlice>,
    palette: List<Color>,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    val gapDegrees = if (slices.size > 1) 4f else 0f
    val totalGap = gapDegrees * slices.size
    val availableSweep = (360f - totalGap).coerceAtLeast(0f)

    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.22f
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = Size(diameter, diameter)
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)

        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )

        var startAngle = -90f
        slices.forEachIndexed { index, slice ->
            val sweep = (slice.ratio * availableSweep).coerceAtLeast(1f)
            drawArc(
                color = palette[index % palette.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            startAngle += sweep + gapDegrees
        }
    }
}

@Composable
private fun TrendBarChart(
    points: List<TrendPoint>,
    periodType: StatsPeriodType,
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val maxExpense = points.maxOfOrNull { it.totalExpense }?.coerceAtLeast(1L) ?: 1L

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val count = points.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.56f).coerceIn(4.dp.toPx(), 24.dp.toPx())
            val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            val minBarHeight = 4.dp.toPx()

            points.forEachIndexed { index, point ->
                val x = index * slotWidth + (slotWidth - barWidth) / 2f
                val ratio = point.totalExpense.toFloat() / maxExpense.toFloat()
                val barHeight = if (point.totalExpense == 0L) {
                    minBarHeight
                } else {
                    (size.height * ratio).coerceAtLeast(minBarHeight)
                }
                val y = size.height - barHeight
                val barColor = if (point.totalExpense == 0L) {
                    colorScheme.surfaceVariant
                } else {
                    colors.coral
                }

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRadius
                )
            }
        }
        Spacer(modifier = Modifier.height(spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { index, point ->
                val showLabel = when (periodType) {
                    StatsPeriodType.WEEK -> true
                    StatsPeriodType.YEAR -> index % 2 == 0
                    StatsPeriodType.MONTH -> index == 0 || (index + 1) % 5 == 0 || index == points.lastIndex
                }
                Text(
                    text = if (showLabel) point.label else "",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText,
                    maxLines = 1
                )
            }
        }
    }
}
