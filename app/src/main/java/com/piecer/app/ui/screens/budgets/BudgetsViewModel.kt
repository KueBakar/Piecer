package com.piecer.app.ui.screens.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.local.entity.BudgetEntity
import com.piecer.app.data.local.entity.BudgetPeriodType
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.data.repository.BudgetRepository
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.data.repository.TransactionRepository
import com.piecer.app.util.BudgetCalculator
import com.piecer.app.util.BudgetNotificationHelper
import com.piecer.app.util.BudgetProgress
import com.piecer.app.util.DateUtils
import com.piecer.app.util.FinancialCycleHelper
import com.piecer.app.util.parseRupiahInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetCardItem(
    val budget: BudgetEntity,
    val category: CategoryEntity,
    val progress: BudgetProgress
)

enum class BudgetFormError {
    NO_CATEGORY,
    INVALID_LIMIT
}

data class BudgetFormState(
    val isVisible: Boolean = false,
    val editingBudgetId: Long? = null,
    val editingCreatedAt: Long? = null,
    val selectedCategoryId: Long? = null,
    val limitInput: String = "",
    val error: BudgetFormError? = null,
    val isSaving: Boolean = false
)

data class BudgetsUiState(
    val items: List<BudgetCardItem> = emptyList(),
    val availableCategoriesForAdd: List<CategoryEntity> = emptyList(),
    val allExpenseCategories: List<CategoryEntity> = emptyList(),
    val form: BudgetFormState = BudgetFormState(),
    val pendingDeleteItem: BudgetCardItem? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
    private val budgetNotificationHelper: BudgetNotificationHelper,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val formState = MutableStateFlow(BudgetFormState())
    private val pendingDelete = MutableStateFlow<BudgetCardItem?>(null)

    private val categoryTotalsFlow = settingsRepository.settingsFlow
        .map { it.cycleStartDay }
        .distinctUntilChanged()
        .flatMapLatest { startDay ->
            val now = System.currentTimeMillis()
            val period = FinancialCycleHelper.currentPeriod(nowMillis = now, cycleStartDay = startDay)
            val effectiveEnd = minOf(period.endMillis, DateUtils.endOfTodayMillis(now))
            transactionRepository.observeTotalByCategory(
                TransactionType.EXPENSE,
                period.startMillis,
                effectiveEnd
            )
        }

    val uiState: StateFlow<BudgetsUiState> = combine(
        budgetRepository.observeAll(),
        categoryRepository.observeByType(CategoryType.EXPENSE),
        categoryTotalsFlow,
        formState,
        pendingDelete
    ) { budgets, expenseCategories, categoryTotals, form, deleteTarget ->
        val categoryMap = expenseCategories.associateBy { it.id }
        val spentByCategory = categoryTotals.associate { it.categoryId to it.totalAmount }

        val items = budgets.mapNotNull { budget ->
            val category = categoryMap[budget.categoryId] ?: return@mapNotNull null
            val spent = spentByCategory[budget.categoryId] ?: 0L
            BudgetCardItem(
                budget = budget,
                category = category,
                progress = BudgetCalculator.calculate(spent = spent, limit = budget.limitAmount)
            )
        }.sortedByDescending { it.progress.percentageInt }

        val budgetedCategoryIds = budgets.map { it.categoryId }.toSet()
        val availableForAdd = expenseCategories.filter { it.id !in budgetedCategoryIds }

        BudgetsUiState(
            items = items,
            availableCategoriesForAdd = availableForAdd,
            allExpenseCategories = expenseCategories,
            form = form,
            pendingDeleteItem = deleteTarget
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = BudgetsUiState()
    )

    fun openAddForm() {
        val firstAvailable = uiState.value.availableCategoriesForAdd.firstOrNull()?.id
        formState.value = BudgetFormState(
            isVisible = true,
            selectedCategoryId = firstAvailable
        )
    }

    fun openEditForm(item: BudgetCardItem) {
        formState.value = BudgetFormState(
            isVisible = true,
            editingBudgetId = item.budget.id,
            editingCreatedAt = item.budget.createdAt,
            selectedCategoryId = item.budget.categoryId,
            limitInput = item.budget.limitAmount.toString()
        )
    }

    fun closeForm() {
        formState.update { it.copy(isVisible = false, error = null) }
    }

    fun onCategorySelect(categoryId: Long) {
        formState.update { it.copy(selectedCategoryId = categoryId, error = null) }
    }

    fun onLimitInputChange(input: String) {
        formState.update { it.copy(limitInput = input, error = null) }
    }

    fun saveBudget(onSaved: () -> Unit) {
        val form = formState.value
        if (form.isSaving) return

        val categoryId = form.selectedCategoryId
        if (categoryId == null) {
            formState.update { it.copy(error = BudgetFormError.NO_CATEGORY) }
            return
        }

        val limitAmount = parseRupiahInput(form.limitInput)
        if (limitAmount == null || limitAmount <= 0L) {
            formState.update { it.copy(error = BudgetFormError.INVALID_LIMIT) }
            return
        }

        formState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = BudgetEntity(
                id = form.editingBudgetId ?: 0L,
                categoryId = categoryId,
                limitAmount = limitAmount,
                periodType = BudgetPeriodType.MONTHLY,
                createdAt = form.editingCreatedAt ?: now
            )
            if (form.editingBudgetId != null) {
                budgetRepository.update(entity)
            } else {
                budgetRepository.insert(entity)
            }
            budgetNotificationHelper.evaluateCurrentMonthBudgets(now)
            formState.value = BudgetFormState(isVisible = false)
            onSaved()
        }
    }

    fun requestDelete(item: BudgetCardItem) {
        pendingDelete.value = item
    }

    fun dismissDelete() {
        pendingDelete.value = null
    }

    fun confirmDelete(onDeleted: () -> Unit) {
        val target = pendingDelete.value ?: return
        viewModelScope.launch {
            budgetRepository.delete(target.budget)
            pendingDelete.value = null
            onDeleted()
        }
    }
}
