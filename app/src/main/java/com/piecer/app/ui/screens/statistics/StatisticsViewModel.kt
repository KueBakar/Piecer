package com.piecer.app.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.data.repository.TransactionRepository
import com.piecer.app.util.CategoryExpenseSlice
import com.piecer.app.util.PeriodSummaryComparison
import com.piecer.app.util.StatisticsCalculator
import com.piecer.app.util.StatsPeriodType
import com.piecer.app.util.TrendPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class StatisticsUiState(
    val periodType: StatsPeriodType = StatsPeriodType.MONTH,
    val periodOffset: Int = 0,
    val periodLabel: String = "",
    val canGoNext: Boolean = false,
    val summary: PeriodSummaryComparison = PeriodSummaryComparison(
        totalIncome = 0L,
        totalExpense = 0L,
        netBalance = 0L,
        previousExpense = 0L,
        expenseChangePercent = null
    ),
    val categorySlices: List<CategoryExpenseSlice> = emptyList(),
    val topCategories: List<CategoryExpenseSlice> = emptyList(),
    val trendPoints: List<TrendPoint> = emptyList(),
    val hasData: Boolean = false
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val periodType = MutableStateFlow(StatsPeriodType.MONTH)
    private val periodOffset = MutableStateFlow(0)

    val uiState: StateFlow<StatisticsUiState> = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        periodType,
        periodOffset,
        settingsRepository.settingsFlow.map { it.cycleStartDay }.distinctUntilChanged()
    ) { transactions, categories, type, offset, cycleStartDay ->
        val now = System.currentTimeMillis()
        val currentRange = StatisticsCalculator.computePeriodRange(
            periodType = type,
            offset = offset,
            nowMillis = now,
            cycleStartDay = cycleStartDay
        )
        val previousRange = StatisticsCalculator.computePeriodRange(
            periodType = type,
            offset = offset - 1,
            nowMillis = now,
            cycleStartDay = cycleStartDay
        )

        val summary = StatisticsCalculator.computeSummaryComparison(
            transactions = transactions,
            currentRange = currentRange,
            previousRange = previousRange
        )

        val categoryMap = categories.associateBy { it.id }
        val slices = StatisticsCalculator.computeCategoryBreakdown(
            transactions = transactions,
            categories = categoryMap,
            startMillis = currentRange.startMillis,
            endMillis = currentRange.endMillis
        )
        val trendPoints = StatisticsCalculator.computeTrendPoints(
            transactions = transactions,
            periodType = type,
            startMillis = currentRange.startMillis,
            endMillis = currentRange.endMillis
        )

        StatisticsUiState(
            periodType = type,
            periodOffset = offset,
            periodLabel = currentRange.label,
            canGoNext = offset < 0,
            summary = summary,
            categorySlices = slices,
            topCategories = slices.take(5),
            trendPoints = trendPoints,
            hasData = summary.totalIncome > 0L || summary.totalExpense > 0L
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = StatisticsUiState()
    )

    fun onPeriodTypeSelect(type: StatsPeriodType) {
        if (periodType.value != type) {
            periodType.value = type
            periodOffset.value = 0
        }
    }

    fun onPreviousPeriod() {
        periodOffset.update { it - 1 }
    }

    fun onNextPeriod() {
        periodOffset.update { if (it < 0) it + 1 else 0 }
    }
}
