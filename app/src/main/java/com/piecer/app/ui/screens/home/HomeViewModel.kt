package com.piecer.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.repository.BudgetRepository
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.data.repository.TransactionRepository
import com.piecer.app.data.repository.WalletRepository
import com.piecer.app.ui.components.TransactionDisplayItem
import com.piecer.app.util.BudgetCalculator
import com.piecer.app.util.BudgetProgress
import com.piecer.app.util.DailyExpensePoint
import com.piecer.app.util.DateUtils
import com.piecer.app.util.calculateBolehJajanHariIni
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

@HiltViewModel
class HomeViewModel @Inject constructor(
    walletRepository: WalletRepository,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    budgetRepository: BudgetRepository
) : ViewModel() {

    private val isBalanceHidden = MutableStateFlow(false)

    private val nowMillis = System.currentTimeMillis()
    private val monthStart = DateUtils.startOfMonthMillis(nowMillis)
    private val monthEnd = DateUtils.endOfMonthMillis(nowMillis)

    private val baseDataFlow = combine(
        walletRepository.observeWalletsWithBalance(),
        transactionRepository.observePeriodSummary(monthStart, monthEnd),
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        budgetRepository.observeAll()
    ) { walletsWithBalance, monthlySummary, allTransactions, categories, budgets ->
        val currentNow = System.currentTimeMillis()
        val totalBalance = walletsWithBalance.sumOf { it.currentBalance }
        val remainingDays = DateUtils.remainingDaysInMonth(currentNow)

        // Rumus Boleh Jajan Hari Ini:
        // (pemasukan bulan ini - pengeluaran bulan ini) / sisa hari di bulan ini, minimal 0.
        val dailyAllowance = calculateBolehJajanHariIni(
            monthlyIncome = monthlySummary.totalIncome,
            monthlyExpense = monthlySummary.totalExpense,
            remainingDaysInMonth = remainingDays
        )

        val categoryNames = categories.associate { it.id to it.name }
        val walletNames = walletsWithBalance.associate { it.walletId to it.name }

        val currentMonthStart = DateUtils.startOfMonthMillis(currentNow)
        val currentMonthEnd = DateUtils.endOfMonthMillis(currentNow)
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
        isBalanceHidden
    ) { state, hidden ->
        state.copy(isBalanceHidden = hidden)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HomeUiState()
    )

    fun toggleBalanceVisibility() {
        isBalanceHidden.update { !it }
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
