package com.piecer.app.ui.screens.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.PieceAllocationEntity
import com.piecer.app.data.local.entity.PieceEntity
import com.piecer.app.data.local.entity.RecurringEntity
import com.piecer.app.data.local.entity.RecurringInterval
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.data.repository.PieceRepository
import com.piecer.app.data.repository.RecurringRepository
import com.piecer.app.data.repository.TransactionRepository
import com.piecer.app.data.repository.WalletRepository
import com.piecer.app.util.BudgetNotificationHelper
import com.piecer.app.util.KeypadUtils
import com.piecer.app.util.PieceAllocationResult
import com.piecer.app.util.PieceCalculator
import com.piecer.app.util.QuickEntryParser
import com.piecer.app.util.RecurringCalculator
import com.piecer.app.util.TransactionValidationError
import com.piecer.app.util.validateTransactionInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class AddTransactionUiState(
    val editingId: Long? = null,
    val editingCreatedAt: Long? = null,
    val quickEntryText: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: Long = 0L,
    val selectedCategoryId: Long? = null,
    val selectedWalletId: Long? = null,
    val selectedToWalletId: Long? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val error: TransactionValidationError? = null,
    val isSaving: Boolean = false,
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val wallets: List<WalletEntity> = emptyList(),
    val pieces: List<PieceEntity> = emptyList(),
    val splitToPieces: Boolean = false,
    val pieceSplitPreview: List<PieceAllocationResult> = emptyList(),
    val makeRecurring: Boolean = false,
    val recurringInterval: RecurringInterval = RecurringInterval.MONTHLY
) {
    val activeCategories: List<CategoryEntity>
        get() = when (type) {
            TransactionType.EXPENSE -> expenseCategories
            TransactionType.INCOME -> incomeCategories
            TransactionType.TRANSFER -> emptyList()
        }
}

private data class FormDraft(
    val editingId: Long? = null,
    val editingCreatedAt: Long? = null,
    val quickEntryText: String = "",
    val quickEntryActive: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: Long = 0L,
    val selectedCategoryId: Long? = null,
    val selectedWalletId: Long? = null,
    val selectedToWalletId: Long? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val error: TransactionValidationError? = null,
    val isSaving: Boolean = false,
    val splitToPiecesOverride: Boolean? = null,
    val makeRecurring: Boolean = false,
    val recurringInterval: RecurringInterval = RecurringInterval.MONTHLY
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    walletRepository: WalletRepository,
    private val pieceRepository: PieceRepository,
    private val recurringRepository: RecurringRepository,
    private val settingsRepository: SettingsRepository,
    private val budgetNotificationHelper: BudgetNotificationHelper
) : ViewModel() {

    private val draft = MutableStateFlow(FormDraft())

    val uiState: StateFlow<AddTransactionUiState> = combine(
        draft,
        categoryRepository.observeAll(),
        walletRepository.observeAll(),
        pieceRepository.observeAllPieces(),
        settingsRepository.settingsFlow
    ) { currentDraft, categories, wallets, pieces, settings ->
        val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }
        val incomeCategories = categories.filter { it.type == CategoryType.INCOME }

        val resolvedCategoryId = when (currentDraft.type) {
            TransactionType.EXPENSE -> currentDraft.selectedCategoryId
                ?.takeIf { id -> expenseCategories.any { it.id == id } }
                ?: if (currentDraft.quickEntryActive) null else expenseCategories.firstOrNull()?.id
            TransactionType.INCOME -> currentDraft.selectedCategoryId
                ?.takeIf { id -> incomeCategories.any { it.id == id } }
                ?: if (currentDraft.quickEntryActive) null else incomeCategories.firstOrNull()?.id
            TransactionType.TRANSFER -> null
        }

        val resolvedWalletId = currentDraft.selectedWalletId
            ?.takeIf { id -> wallets.any { it.id == id } }
            ?: if (currentDraft.quickEntryActive) null else wallets.firstOrNull()?.id

        val resolvedToWalletId = if (currentDraft.type == TransactionType.TRANSFER) {
            currentDraft.selectedToWalletId
                ?.takeIf { id -> wallets.any { it.id == id } }
                ?: if (currentDraft.quickEntryActive) null else wallets.firstOrNull { it.id != resolvedWalletId }?.id
        } else {
            null
        }

        val splitToPieces = currentDraft.splitToPiecesOverride ?: settings.piecesEnabled
        val splitPreview = if (currentDraft.type == TransactionType.INCOME && splitToPieces && pieces.isNotEmpty() && currentDraft.amount > 0L) {
            PieceCalculator.allocateIncomeToPieces(currentDraft.amount, pieces)
        } else {
            emptyList()
        }

        AddTransactionUiState(
            editingId = currentDraft.editingId,
            editingCreatedAt = currentDraft.editingCreatedAt,
            quickEntryText = currentDraft.quickEntryText,
            type = currentDraft.type,
            amount = currentDraft.amount,
            selectedCategoryId = resolvedCategoryId,
            selectedWalletId = resolvedWalletId,
            selectedToWalletId = resolvedToWalletId,
            dateMillis = currentDraft.dateMillis,
            note = currentDraft.note,
            error = currentDraft.error,
            isSaving = currentDraft.isSaving,
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            wallets = wallets,
            pieces = pieces,
            splitToPieces = splitToPieces,
            pieceSplitPreview = splitPreview,
            makeRecurring = currentDraft.makeRecurring,
            recurringInterval = currentDraft.recurringInterval
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AddTransactionUiState()
    )

    fun startNew(initialType: TransactionType = TransactionType.EXPENSE) {
        draft.value = FormDraft(
            type = initialType,
            dateMillis = System.currentTimeMillis()
        )
    }

    fun startEdit(transactionId: Long) {
        viewModelScope.launch {
            val tx = transactionRepository.observeById(transactionId).firstOrNull() ?: return@launch
            val hasAllocations = pieceRepository.observeAllAllocations().first()
                .any { it.transactionId == tx.id }
            draft.value = FormDraft(
                editingId = tx.id,
                editingCreatedAt = tx.createdAt,
                type = tx.type,
                amount = tx.amount,
                selectedCategoryId = tx.categoryId,
                selectedWalletId = tx.walletId,
                selectedToWalletId = tx.toWalletId,
                dateMillis = tx.date,
                note = tx.note,
                error = null,
                isSaving = false,
                splitToPiecesOverride = if (tx.type == TransactionType.INCOME) hasAllocations else null
            )
        }
    }

    fun onQuickEntryChange(text: String) {
        val state = uiState.value
        if (text.isBlank()) {
            draft.update {
                it.copy(
                    quickEntryText = text,
                    quickEntryActive = false
                )
            }
            return
        }
        val allCategories = state.expenseCategories + state.incomeCategories
        val parsed = QuickEntryParser.parse(
            input = text,
            categories = allCategories,
            wallets = state.wallets
        )
        draft.update {
            it.copy(
                quickEntryText = text,
                quickEntryActive = true,
                type = parsed.type,
                amount = parsed.amount ?: 0L,
                selectedCategoryId = parsed.categoryId,
                selectedWalletId = parsed.walletId,
                selectedToWalletId = parsed.toWalletId,
                dateMillis = parsed.dateMillis ?: it.dateMillis,
                note = parsed.note,
                error = null
            )
        }
    }

    fun onSplitToPiecesChange(enabled: Boolean) {
        draft.update { it.copy(splitToPiecesOverride = enabled) }
    }

    fun onMakeRecurringChange(enabled: Boolean) {
        draft.update { it.copy(makeRecurring = enabled) }
    }

    fun onRecurringIntervalSelect(interval: RecurringInterval) {
        draft.update { it.copy(recurringInterval = interval) }
    }

    fun onTypeChange(type: TransactionType) {
        draft.update {
            it.copy(
                type = type,
                selectedCategoryId = null,
                error = null
            )
        }
    }

    fun onDigitClick(digit: Int) {
        draft.update {
            it.copy(
                amount = KeypadUtils.appendDigit(it.amount, digit),
                error = null
            )
        }
    }

    fun onTripleZeroClick() {
        draft.update {
            it.copy(
                amount = KeypadUtils.appendTripleZero(it.amount),
                error = null
            )
        }
    }

    fun onBackspaceClick() {
        draft.update {
            it.copy(
                amount = KeypadUtils.backspace(it.amount),
                error = null
            )
        }
    }

    fun onCategorySelect(categoryId: Long) {
        draft.update { it.copy(selectedCategoryId = categoryId, error = null) }
    }

    fun onWalletSelect(walletId: Long) {
        draft.update { it.copy(selectedWalletId = walletId, error = null) }
    }

    fun onToWalletSelect(walletId: Long) {
        draft.update { it.copy(selectedToWalletId = walletId, error = null) }
    }

    fun onDateSelect(dateMillis: Long) {
        draft.update { it.copy(dateMillis = dateMillis, error = null) }
    }

    fun onNoteChange(note: String) {
        draft.update { it.copy(note = note) }
    }

    fun save(onSaved: () -> Unit) {
        val state = uiState.value
        if (state.isSaving) return

        val validationError = validateTransactionInput(
            type = state.type,
            amount = state.amount,
            categoryId = state.selectedCategoryId,
            walletId = state.selectedWalletId,
            toWalletId = state.selectedToWalletId
        )
        if (validationError != null) {
            draft.update { it.copy(error = validationError) }
            return
        }

        val walletId = state.selectedWalletId ?: return
        draft.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = TransactionEntity(
                id = state.editingId ?: 0L,
                amount = state.amount,
                type = state.type,
                categoryId = if (state.type == TransactionType.TRANSFER) null else state.selectedCategoryId,
                walletId = walletId,
                toWalletId = if (state.type == TransactionType.TRANSFER) state.selectedToWalletId else null,
                date = state.dateMillis,
                note = state.note.trim(),
                createdAt = state.editingCreatedAt ?: now
            )
            val savedId = if (state.editingId != null) {
                transactionRepository.update(entity)
                state.editingId
            } else {
                transactionRepository.insert(entity)
            }

            if (entity.type == TransactionType.INCOME && state.splitToPieces && state.pieces.isNotEmpty()) {
                val allocations = PieceCalculator.allocateIncomeToPieces(entity.amount, state.pieces)
                    .map { alloc ->
                        PieceAllocationEntity(
                            pieceId = alloc.pieceId,
                            transactionId = savedId,
                            amount = alloc.amount,
                            createdAt = now
                        )
                    }
                pieceRepository.saveAllocationsForTransaction(savedId, allocations)
            } else if (state.editingId != null) {
                pieceRepository.deleteAllocationsForTransaction(savedId)
            }

            if (state.editingId == null && state.makeRecurring) {
                val zone = ZoneId.systemDefault()
                val txDate = Instant.ofEpochMilli(entity.date).atZone(zone).toLocalDate()
                val nextDue = RecurringCalculator.calculateNextDueDate(
                    currentDueMillis = entity.date,
                    interval = state.recurringInterval,
                    dayOfMonth = if (state.recurringInterval == RecurringInterval.MONTHLY) txDate.dayOfMonth else null,
                    dayOfWeek = if (state.recurringInterval == RecurringInterval.WEEKLY) txDate.dayOfWeek.value else null,
                    zoneId = zone
                )
                recurringRepository.insert(
                    RecurringEntity(
                        type = entity.type,
                        amount = entity.amount,
                        categoryId = entity.categoryId,
                        walletId = entity.walletId,
                        toWalletId = entity.toWalletId,
                        note = entity.note,
                        interval = state.recurringInterval,
                        dayOfMonth = if (state.recurringInterval == RecurringInterval.MONTHLY) txDate.dayOfMonth else null,
                        dayOfWeek = if (state.recurringInterval == RecurringInterval.WEEKLY) txDate.dayOfWeek.value else null,
                        nextDueDate = nextDue,
                        isActive = true,
                        remindDaysBefore = 1,
                        createdAt = now
                    )
                )
            }

            if (entity.type == TransactionType.EXPENSE) {
                budgetNotificationHelper.evaluateCurrentMonthBudgets(now)
            }
            draft.update { it.copy(isSaving = false) }
            onSaved()
        }
    }
}
