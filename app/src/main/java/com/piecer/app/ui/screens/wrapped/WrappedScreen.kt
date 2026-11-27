package com.piecer.app.ui.screens.wrapped

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.R
import com.piecer.app.ui.theme.Mint
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.DateUtils
import com.piecer.app.util.WrappedCardShareContent
import com.piecer.app.util.WrappedPeriodMode
import com.piecer.app.util.WrappedShareHelper
import com.piecer.app.util.WrappedSummary
import com.piecer.app.util.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrappedScreen(
    viewModel: WrappedViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val spacing = PiecerTheme.spacing
    val summary = uiState.summary

    val cards = if (summary == null || !summary.hasData) {
        listOf(
            WrappedCardShareContent(
                eyebrow = stringResource(id = R.string.wrapped_title),
                title = stringResource(id = R.string.wrapped_empty_title),
                bigValue = "—",
                subtitle = stringResource(id = R.string.wrapped_empty_desc)
            )
        )
    } else {
        buildWrappedCards(summary = summary, hideAmountsForDisplay = false)
    }

    val shareCards = if (summary == null || !summary.hasData) {
        cards
    } else {
        buildWrappedCards(summary = summary, hideAmountsForDisplay = uiState.hideAmountsOnShare)
    }

    val pagerState = rememberPagerState(pageCount = { cards.size })
    val shareChooserTitle = stringResource(id = R.string.wrapped_share_chooser)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.wrapped_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = spacing.screenPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.mode == WrappedPeriodMode.MONTH,
                    onClick = { viewModel.setMode(WrappedPeriodMode.MONTH) },
                    label = { Text(text = stringResource(id = R.string.wrapped_mode_month)) }
                )
                FilterChip(
                    selected = uiState.mode == WrappedPeriodMode.YEAR,
                    onClick = { viewModel.setMode(WrappedPeriodMode.YEAR) },
                    label = { Text(text = stringResource(id = R.string.wrapped_mode_year)) }
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::previousPeriod) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = stringResource(id = R.string.wrapped_prev_period)
                    )
                }
                Text(
                    text = summary?.periodLabel ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = viewModel::nextPeriod) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(id = R.string.wrapped_next_period)
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                pageSpacing = 12.dp
            ) { page ->
                WrappedCardView(
                    card = cards[page],
                    periodLabel = summary?.periodLabel ?: ""
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(cards.size) { idx ->
                    val selected = idx == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) Mint else colorScheme.outline)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.wrapped_hide_amounts_share),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = uiState.hideAmountsOnShare,
                    onCheckedChange = { viewModel.toggleHideAmountsOnShare() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val current = pagerState.currentPage.coerceIn(0, shareCards.lastIndex)
                    WrappedShareHelper.shareCard(
                        context = context,
                        card = shareCards[current],
                        periodLabel = summary?.periodLabel ?: "",
                        shareChooserTitle = shareChooserTitle
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.md),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.wrapped_share_button))
            }
        }
    }
}

@Composable
private fun WrappedCardView(
    card: WrappedCardShareContent,
    periodLabel: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(colorScheme.surface)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = card.eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Mint,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            if (card.isClosingCard) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val primaryColor = colorScheme.onSurface
                    Canvas(modifier = Modifier.size(96.dp)) {
                        drawArc(
                            color = primaryColor,
                            startAngle = -60f,
                            sweepAngle = 300f,
                            useCenter = true
                        )
                        drawArc(
                            color = Mint,
                            startAngle = 250f,
                            sweepAngle = 40f,
                            useCenter = true
                        )
                    }
                }
            }

            Column {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = card.bigValue,
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Mint
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = card.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun buildWrappedCards(
    summary: WrappedSummary,
    hideAmountsForDisplay: Boolean
): List<WrappedCardShareContent> {
    val totalExpenseValue = if (hideAmountsForDisplay) {
        "${100 - summary.savingsRatioPercent.coerceIn(0, 100)}%"
    } else {
        formatRupiah(summary.totalExpense)
    }

    val topCatValue = summary.topCategoryName ?: stringResource(id = R.string.tx_uncategorized)
    val topCatSub = if (hideAmountsForDisplay) {
        stringResource(id = R.string.wrapped_top_cat_percent_sub, summary.topCategoryPercent)
    } else {
        stringResource(
            id = R.string.wrapped_top_cat_amount_sub,
            formatRupiah(summary.topCategoryAmount),
            summary.topCategoryPercent
        )
    }

    val mostExpensiveDayText = summary.mostExpensiveDayMillis?.let {
        DateUtils.formatDate(it)
    } ?: "—"
    val mostExpensiveValue = if (hideAmountsForDisplay) {
        val sharePct = if (summary.totalExpense > 0L) {
            ((summary.mostExpensiveDayAmount * 100L) / summary.totalExpense).toInt()
        } else 0
        "$sharePct%"
    } else {
        formatRupiah(summary.mostExpensiveDayAmount)
    }

    val deltaText = when (val delta = summary.expenseChangePercent) {
        null -> stringResource(id = R.string.wrapped_delta_none)
        in Int.MIN_VALUE..-1 -> "${delta}%"
        0 -> "0%"
        else -> "+$delta%"
    }

    return listOf(
        WrappedCardShareContent(
            eyebrow = stringResource(id = R.string.wrapped_card_expense_eyebrow),
            title = stringResource(id = R.string.wrapped_card_expense_title),
            bigValue = totalExpenseValue,
            subtitle = stringResource(id = R.string.wrapped_card_expense_sub)
        ),
        WrappedCardShareContent(
            eyebrow = stringResource(id = R.string.wrapped_card_top_cat_eyebrow),
            title = stringResource(id = R.string.wrapped_card_top_cat_title),
            bigValue = topCatValue,
            subtitle = topCatSub
        ),
        WrappedCardShareContent(
            eyebrow = stringResource(id = R.string.wrapped_card_peak_day_eyebrow),
            title = mostExpensiveDayText,
            bigValue = mostExpensiveValue,
            subtitle = stringResource(id = R.string.wrapped_card_peak_day_sub)
        ),
        WrappedCardShareContent(
            eyebrow = stringResource(id = R.string.wrapped_card_compare_eyebrow),
            title = stringResource(id = R.string.wrapped_card_compare_title),
            bigValue = deltaText,
            subtitle = stringResource(id = R.string.wrapped_card_compare_sub)
        ),
        WrappedCardShareContent(
            eyebrow = stringResource(id = R.string.wrapped_card_savings_eyebrow),
            title = stringResource(id = R.string.wrapped_card_savings_title),
            bigValue = "${summary.savingsRatioPercent}%",
            subtitle = stringResource(id = R.string.wrapped_card_savings_sub)
        ),
        WrappedCardShareContent(
            eyebrow = stringResource(id = R.string.wrapped_card_streak_eyebrow),
            title = stringResource(id = R.string.wrapped_card_streak_title),
            bigValue = stringResource(id = R.string.wrapped_streak_days, summary.recordingStreakDays),
            subtitle = stringResource(id = R.string.wrapped_card_streak_sub)
        ),
        WrappedCardShareContent(
            eyebrow = stringResource(id = R.string.wrapped_title),
            title = stringResource(id = R.string.wrapped_closing_title),
            bigValue = stringResource(id = R.string.app_name),
            subtitle = stringResource(id = R.string.splash_tagline),
            isClosingCard = true
        )
    )
}
