package com.piecer.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.local.entity.PieceAllocationEntity
import com.piecer.app.data.local.entity.PieceCategoryEntity
import com.piecer.app.data.local.entity.PieceEntity
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.data.repository.BudgetRepository
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.data.repository.PieceRepository
import com.piecer.app.data.repository.TransactionRepository
import com.piecer.app.data.repository.WalletRepository
import com.piecer.app.ui.components.TransactionDisplayItem
import com.piecer.app.util.BudgetCalculator
import com.piecer.app.util.BudgetProgress
import com.piecer.app.util.DailyExpensePoint
import com.piecer.app.util.DateUtils
import com.piecer.app.util.FinancialCycleHelper
import com.piecer.app.util.HomeInsight
import com.piecer.app.util.InsightEngine
import com.piecer.app.util.PieceBalanceSummary
import com.piecer.app.util.PieceCalculator
import com.piecer.app.util.calculateBolehJajanHariIni
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

enum class GreetingPeriod {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}

data class HomeCriticalBudget(
    val categoryName: String,
    val progress: BudgetProgress
)

data class HomeUiState(
    val greetingPeriod: GreetingPeriod = GreetingPeriod.MORNING,
    val isBalanceHidden: Boolean = false,
    val totalBalance: Long = 0L,
    val monthlyIncome: Long = 0L,
    val monthlyExpense: Long = 0L,
    val dailyAllowance: Long = 0L,
    val remainingDaysInMonth: Int = 1,
    val criticalBudget: HomeCriticalBudget? = null,
    val weeklyExpenses: List<DailyExpensePoint> = emptyList(),
    val recentTransactions: List<TransactionDisplayItem> = emptyList(),
    val upcomingTransactions: List<TransactionDisplayItem> = emptyList(),
    val hasAnyTransaction: Boolean = false,
    val piecesEnabled: Boolean = false,
    val pieceSummaries: List<PieceBalanceSummary> = emptyList(),
    val lowestPieces: List<PieceBalanceSummary> = emptyList(),
    val insights: List<HomeInsight> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    walletRepository: WalletRepository,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    budgetRepository: BudgetRepository,
    pieceRepository: PieceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val cycleStartDayFlow = settingsRepository.settingsFlow
        .map { it.cycleStartDay }
        .distinctUntilChanged()

    private val monthlySummaryFlow = cycleStartDayFlow.flatMapLatest { startDay ->
        val now = System.currentTimeMillis()
        val period = FinancialCycleHelper.currentPeriod(nowMillis = now, cycleStartDay = startDay)
        val effectiveEnd = minOf(period.endMillis, DateUtils.endOfTodayMillis(now))
        transactionRepository.observePeriodSummary(period.startMillis, effectiveEnd)
    }

    private val piecesDataFlow = combine(
        pieceRepository.observeAllPieces(),
        pieceRepository.observeAllPieceCategories(),
        pieceRepository.observeAllAllocations()
    ) { pieces, pieceCats, allocations ->
        Triple(pieces, pieceCats, allocations)
    }

    private val baseDataFlow = combine(
        walletRepository.observeWalletsWithBalance(),
        monthlySummaryFlow,
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        budgetRepository.observeAll(),
        cycleStartDayFlow,
        piecesDataFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val walletsWithBalance = values[0] as List<com.piecer.app.data.local.dao.WalletWithBalance>
        val monthlySummary = values[1] as com.piecer.app.data.local.dao.PeriodSummary
        @Suppress("UNCHECKED_CAST")
        val allTransactions = values[2] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val categories = values[3] as List<com.piecer.app.data.local.entity.CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val budgets = values[4] as List<com.piecer.app.data.local.entity.BudgetEntity>
        val cycleStartDay = values[5] as Int
        @Suppress("UNCHECKED_CAST")
        val piecesTuple = values[6] as Triple<List<PieceEntity>, List<PieceCategoryEntity>, List<PieceAllocationEntity>>
        val (pieces, pieceCats, allocations) = piecesTuple

        val currentNow = System.currentTimeMillis()
        val effectiveTxs = DateUtils.effectiveTransactions(allTransactions, currentNow)
        val upcomingTxs = DateUtils.scheduledTransactions(allTransactions, currentNow)
            .sortedBy { it.date }

        val totalBalance = walletsWithBalance.sumOf { it.currentBalance }
        val cyclePeriod = FinancialCycleHelper.currentPeriod(
            nowMillis = currentNow,
            cycleStartDay = cycleStartDay
        )
        val remainingDays = cyclePeriod.remainingDays

        // Rumus Boleh Jajan Hari Ini:
        // (pemasukan siklus ini - pengeluaran siklus ini) / sisa hari di siklus ini, minimal 0.
        val dailyAllowance = calculateBolehJajanHariIni(
            monthlyIncome = monthlySummary.totalIncome,
            monthlyExpense = monthlySummary.totalExpense,
            remainingDaysInMonth = remainingDays
        )

        val categoryNames = categories.associate { it.id to it.name }
        val walletNames = walletsWithBalance.associate { it.walletId to it.name }

        val currentMonthStart = cyclePeriod.startMillis
        val currentMonthEnd = minOf(cyclePeriod.endMillis, DateUtils.endOfTodayMillis(currentNow))
        val spentByCategory = effectiveTxs
            .asSequence()
            .filter {
                it.type == TransactionType.EXPENSE &&
                    it.categoryId != null &&
                    it.date in currentMonthStart..currentMonthEnd
            }
            .groupBy { it.categoryId!! }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        val budgetSummaries = budgets.mapNotNull { budget ->
            val catName = categoryNames[budget.categoryId] ?: return@mapNotNull null
            val spent = spentByCategory[budget.categoryId] ?: 0L
            HomeCriticalBudget(
                categoryName = catName,
                progress = BudgetCalculator.calculate(spent = spent, limit = budget.limitAmount)
            )
        }
        val criticalBudget = BudgetCalculator.findMostCritical(budgetSummaries) { it.progress }

        val recentItems = effectiveTxs.take(5).map { tx ->
            toDisplayItem(tx, categoryNames, walletNames, isScheduled = false)
        }
        val upcomingItems = upcomingTxs.take(3).map { tx ->
            toDisplayItem(tx, categoryNames, walletNames, isScheduled = true)
        }

        val weeklyExpenses = DateUtils.last7DaysExpenses(effectiveTxs, currentNow)

        val piecesOverview = PieceCalculator.calculatePiecesOverview(
            pieces = pieces,
            pieceCategories = pieceCats,
            allocations = allocations,
            transactions = effectiveTxs,
            cyclePeriod = cyclePeriod,
            nowMillis = currentNow
        )
        val lowestPieces = piecesOverview.summaries
            .sortedBy { it.remaining }
            .take(2)

        val insights = InsightEngine.generateInsights(
            transactions = effectiveTxs,
            categories = categories,
            budgets = budgets,
            currentBalance = totalBalance,
            cycleStartDay = cycleStartDay,
            nowMillis = currentNow
        )

        HomeUiState(
            greetingPeriod = resolveGreetingPeriod(currentNow),
            isBalanceHidden = false,
            totalBalance = totalBalance,
            monthlyIncome = monthlySummary.totalIncome,
            monthlyExpense = monthlySummary.totalExpense,
            dailyAllowance = dailyAllowance,
            remainingDaysInMonth = remainingDays,
            criticalBudget = criticalBudget,
            weeklyExpenses = weeklyExpenses,
            recentTransactions = recentItems,
            upcomingTransactions = upcomingItems,
            hasAnyTransaction = effectiveTxs.isNotEmpty(),
            piecesEnabled = false,
            pieceSummaries = piecesOverview.summaries,
            lowestPieces = lowestPieces,
            insights = insights
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseDataFlow,
        settingsRepository.settingsFlow.map { it.isBalanceHidden to it.piecesEnabled }.distinctUntilChanged()
    ) { state, (hidden, piecesEnabled) ->
        state.copy(isBalanceHidden = hidden, piecesEnabled = piecesEnabled)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HomeUiState()
        )

    fun toggleBalanceVisibility() {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().isBalanceHidden
            settingsRepository.setBalanceHidden(!current)
        }
    }

    private fun resolveGreetingPeriod(epochMillis: Long): GreetingPeriod {
        val hour = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .hour
        return when (hour) {
            in 4..10 -> GreetingPeriod.MORNING
            in 11..14 -> GreetingPeriod.AFTERNOON
            in 15..18 -> GreetingPeriod.EVENING
            else -> GreetingPeriod.NIGHT
        }
    }

    private fun toDisplayItem(
        tx: TransactionEntity,
        categoryNames: Map<Long, String>,
        walletNames: Map<Long, String>,
        isScheduled: Boolean = false
    ): TransactionDisplayItem {
        val walletName = walletNames[tx.walletId].orEmpty()
        val dateText = DateUtils.formatDate(tx.date)
        return when (tx.type) {
            TransactionType.TRANSFER -> {
                val toWalletName = tx.toWalletId?.let { walletNames[it] }.orEmpty()
                val routeText = if (walletName.isNotEmpty() && toWalletName.isNotEmpty()) {
                    "$walletName → $toWalletName"
                } else {
                    walletName
                }
                val subtitle = listOf(routeText, dateText).filter { it.isNotBlank() }.joinToString(" • ")
                TransactionDisplayItem(
                    transaction = tx,
                    title = "Transfer",
                    subtitle = subtitle,
                    isScheduled = isScheduled
                )
            }
            TransactionType.INCOME, TransactionType.EXPENSE -> {
                val categoryName = tx.categoryId?.let { categoryNames[it] } ?: "Tanpa kategori"
                val subtitle = listOf(walletName, dateText).filter { it.isNotBlank() }.joinToString(" • ")
                TransactionDisplayItem(
                    transaction = tx,
                    title = categoryName,
                    subtitle = subtitle,
                    isScheduled = isScheduled
                )
            }
        }
    }
}
