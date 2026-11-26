package com.piecer.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.data.repository.BudgetRepository
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.data.repository.TransactionRepository
import com.piecer.app.data.repository.WalletRepository
import com.piecer.app.ui.components.TransactionDisplayItem
import com.piecer.app.util.BudgetCalculator
import com.piecer.app.util.BudgetProgress
import com.piecer.app.util.DailyExpensePoint
import com.piecer.app.util.DateUtils
import com.piecer.app.util.FinancialCycleHelper
import com.piecer.app.util.calculateBolehJajanHariIni
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
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
    val hasAnyTransaction: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    walletRepository: WalletRepository,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val cycleStartDayFlow = settingsRepository.settingsFlow
        .map { it.cycleStartDay }
        .distinctUntilChanged()

    private val monthlySummaryFlow = cycleStartDayFlow.flatMapLatest { startDay ->
        val period = FinancialCycleHelper.currentPeriod(cycleStartDay = startDay)
        transactionRepository.observePeriodSummary(period.startMillis, period.endMillis)
    }

    private val baseDataFlow = combine(
        walletRepository.observeWalletsWithBalance(),
        monthlySummaryFlow,
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        budgetRepository.observeAll(),
        cycleStartDayFlow
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

        val currentNow = System.currentTimeMillis()
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
        val currentMonthEnd = cyclePeriod.endMillis
        val spentByCategory = allTransactions
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

        val recentItems = allTransactions.take(5).map { tx ->
            toDisplayItem(tx, categoryNames, walletNames)
        }

        val weeklyExpenses = DateUtils.last7DaysExpenses(allTransactions, currentNow)

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
            hasAnyTransaction = allTransactions.isNotEmpty()
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseDataFlow,
        settingsRepository.settingsFlow.map { it.isBalanceHidden }.distinctUntilChanged()
    ) { state, hidden ->
        state.copy(isBalanceHidden = hidden)
    }.stateIn(
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
        walletNames: Map<Long, String>
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
                    subtitle = subtitle
                )
            }
            TransactionType.INCOME, TransactionType.EXPENSE -> {
                val categoryName = tx.categoryId?.let { categoryNames[it] } ?: "Tanpa kategori"
                val subtitle = listOf(walletName, dateText).filter { it.isNotBlank() }.joinToString(" • ")
                TransactionDisplayItem(
                    transaction = tx,
                    title = categoryName,
                    subtitle = subtitle
                )
            }
        }
    }
}
