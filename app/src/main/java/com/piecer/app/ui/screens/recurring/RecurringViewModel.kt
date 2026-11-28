package com.piecer.app.ui.screens.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.RecurringEntity
import com.piecer.app.data.local.entity.RecurringInterval
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.data.repository.RecurringRepository
import com.piecer.app.data.repository.TransactionRepository
import com.piecer.app.data.repository.WalletRepository
import com.piecer.app.util.DateUtils
import com.piecer.app.util.RecurringCalculator
import com.piecer.app.util.RecurringReminderScheduler
import com.piecer.app.util.SubscriptionSuggestion
import com.piecer.app.util.parseRupiahInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class RecurringFormState(
    val isVisible: Boolean = false,
    val editingItem: RecurringEntity? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val categoryId: Long? = null,
    val walletId: Long? = null,
    val note: String = "",
    val interval: RecurringInterval = RecurringInterval.MONTHLY,
    val nextDueDateMillis: Long = DateUtils.startOfDayMillis(System.currentTimeMillis()),
    val remindDaysBefore: Int = 1,
    val errorMessage: String? = null
)

data class RecurringListItem(
    val entity: RecurringEntity,
    val title: String,
    val walletName: String,
    val nextDueText: String
)

data class RecurringUiState(
    val items: List<RecurringListItem> = emptyList(),
    val suggestions: List<SubscriptionSuggestion> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val wallets: List<WalletEntity> = emptyList(),
    val form: RecurringFormState = RecurringFormState(),
    val pendingDelete: RecurringEntity? = null
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    walletRepository: WalletRepository,
    private val recurringReminderScheduler: RecurringReminderScheduler
) : ViewModel() {

    private val formState = MutableStateFlow(RecurringFormState())
    private val pendingDeleteState = MutableStateFlow<RecurringEntity?>(null)

    private val dataFlow = combine(
        recurringRepository.observeAll(),
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        walletRepository.observeAll()
    ) { recurrings, transactions, categories, wallets ->
        val categoryMap = categories.associateBy { it.id }
        val walletMap = wallets.associateBy { it.id }

        val items = recurrings.map { r ->
            val catName = r.categoryId?.let { categoryMap[it]?.name }
            val title = r.note.ifBlank { catName ?: "Transaksi Berulang" }
            val walletName = walletMap[r.walletId]?.name.orEmpty()
            RecurringListItem(
                entity = r,
                title = title,
                walletName = walletName,
                nextDueText = DateUtils.formatDate(r.nextDueDate)
            )
        }

        val suggestions = RecurringCalculator.detectSubscriptionSuggestions(
            transactions = transactions,
            existingRecurring = recurrings
        )

        RecurringUiState(
            items = items,
            suggestions = suggestions,
            expenseCategories = categories.filter { it.type == CategoryType.EXPENSE },
            incomeCategories = categories.filter { it.type == CategoryType.INCOME },
            wallets = wallets
        )
    }

    val uiState: StateFlow<RecurringUiState> = combine(
        dataFlow,
        formState,
        pendingDeleteState
    ) { base, form, pendingDelete ->
        base.copy(
            form = form,
            pendingDelete = pendingDelete
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = RecurringUiState()
    )

    fun openAddForm() {
        val state = uiState.value
        formState.value = RecurringFormState(
            isVisible = true,
            categoryId = state.expenseCategories.firstOrNull()?.id,
            walletId = state.wallets.firstOrNull()?.id,
            nextDueDateMillis = DateUtils.startOfDayMillis(System.currentTimeMillis())
        )
    }

    fun openEditForm(item: RecurringEntity) {
        formState.value = RecurringFormState(
            isVisible = true,
            editingItem = item,
            type = if (item.type == TransactionType.TRANSFER) TransactionType.EXPENSE else item.type,
            amountInput = item.amount.toString(),
            categoryId = item.categoryId,
            walletId = item.walletId,
            note = item.note,
            interval = item.interval,
            nextDueDateMillis = item.nextDueDate,
            remindDaysBefore = item.remindDaysBefore
        )
    }

    fun closeForm() {
        formState.update { it.copy(isVisible = false, errorMessage = null) }
    }

    fun onTypeSelect(type: TransactionType) {
        val state = uiState.value
        val cats = if (type == TransactionType.INCOME) state.incomeCategories else state.expenseCategories
        formState.update {
            it.copy(
                type = type,
                categoryId = cats.firstOrNull()?.id,
                errorMessage = null
            )
        }
    }

    fun onAmountChange(input: String) {
        formState.update { it.copy(amountInput = input, errorMessage = null) }
    }

    fun onCategorySelect(categoryId: Long) {
        formState.update { it.copy(categoryId = categoryId, errorMessage = null) }
    }

    fun onWalletSelect(walletId: Long) {
        formState.update { it.copy(walletId = walletId, errorMessage = null) }
    }

    fun onNoteChange(note: String) {
        formState.update { it.copy(note = note) }
    }

    fun onIntervalSelect(interval: RecurringInterval) {
        formState.update { it.copy(interval = interval) }
    }

    fun onNextDueDateSelect(millis: Long) {
        formState.update { it.copy(nextDueDateMillis = DateUtils.startOfDayMillis(millis)) }
    }

    fun onRemindDaysSelect(days: Int) {
        formState.update { it.copy(remindDaysBefore = days) }
    }

    fun saveForm(
        invalidAmountError: String,
        noWalletError: String
    ) {
        val form = formState.value
        val amount = parseRupiahInput(form.amountInput)
        if (amount == null || amount <= 0L) {
            formState.update { it.copy(errorMessage = invalidAmountError) }
            return
        }
        val walletId = form.walletId ?: uiState.value.wallets.firstOrNull()?.id
        if (walletId == null) {
            formState.update { it.copy(errorMessage = noWalletError) }
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val dueLocalDate = Instant.ofEpochMilli(form.nextDueDateMillis).atZone(zone).toLocalDate()
            val editing = form.editingItem

            val entity = RecurringEntity(
                id = editing?.id ?: 0L,
                type = form.type,
                amount = amount,
                categoryId = form.categoryId,
                walletId = walletId,
                toWalletId = null,
                note = form.note.trim(),
                interval = form.interval,
                dayOfMonth = if (form.interval == RecurringInterval.MONTHLY) dueLocalDate.dayOfMonth else null,
                dayOfWeek = if (form.interval == RecurringInterval.WEEKLY) dueLocalDate.dayOfWeek.value else null,
                nextDueDate = form.nextDueDateMillis,
                isActive = editing?.isActive ?: true,
                remindDaysBefore = form.remindDaysBefore,
                createdAt = editing?.createdAt ?: now
            )

            if (editing != null) {
                recurringRepository.update(entity)
            } else {
                recurringRepository.insert(entity)
            }
            if (entity.isActive) {
                recurringReminderScheduler.scheduleNext()
            }
            formState.value = RecurringFormState(isVisible = false)
        }
    }

    fun toggleActive(item: RecurringEntity, active: Boolean) {
        viewModelScope.launch {
            recurringRepository.update(item.copy(isActive = active))
            if (active) {
                recurringReminderScheduler.scheduleNext()
            }
        }
    }

    fun acceptSuggestion(suggestion: SubscriptionSuggestion) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val nextMonth = today.plusMonths(1)
            val clampedDay = suggestion.dayOfMonth.coerceIn(1, nextMonth.lengthOfMonth())
            val nextDueMillis = nextMonth.withDayOfMonth(clampedDay).atStartOfDay(zone).toInstant().toEpochMilli()

            recurringRepository.insert(
                RecurringEntity(
                    type = TransactionType.EXPENSE,
                    amount = suggestion.amount,
                    categoryId = suggestion.categoryId,
                    walletId = suggestion.walletId,
                    toWalletId = null,
                    note = suggestion.note,
                    interval = RecurringInterval.MONTHLY,
                    dayOfMonth = suggestion.dayOfMonth,
                    dayOfWeek = null,
                    nextDueDate = nextDueMillis,
                    isActive = true,
                    remindDaysBefore = 1,
                    createdAt = now
                )
            )
            recurringReminderScheduler.scheduleNext()
        }
    }

    fun requestDelete(item: RecurringEntity) {
        pendingDeleteState.value = item
    }

    fun dismissDelete() {
        pendingDeleteState.value = null
    }

    fun confirmDelete() {
        val target = pendingDeleteState.value ?: return
        viewModelScope.launch {
            recurringRepository.delete(target)
            pendingDeleteState.value = null
        }
    }
}
