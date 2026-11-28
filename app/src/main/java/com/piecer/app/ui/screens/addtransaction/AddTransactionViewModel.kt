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
import com.piecer.app.data.preferences.AppSettings
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
import com.piecer.app.util.RecurringCalculator
import com.piecer.app.util.TransactionValidationError
import com.piecer.app.util.quickentry.CategoryCandidate
import com.piecer.app.util.quickentry.ParsedEntry
import com.piecer.app.util.quickentry.QuickEntryContext
import com.piecer.app.util.quickentry.QuickEntryFlag
import com.piecer.app.util.quickentry.QuickEntryLearningManager
import com.piecer.app.util.quickentry.QuickEntryParseResult
import com.piecer.app.util.quickentry.QuickEntryPipeline
import com.piecer.app.util.quickentry.QuickEntryPreviewPolicy
import com.piecer.app.util.quickentry.VoiceTranscriptSelector
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

data class QuickEntryPreviewItem(
    val id: Long,
    val amount: Long,
    val type: TransactionType,
    val categoryId: Long?,
    val predictedCategoryId: Long?,
    val categoryCandidates: List<CategoryCandidate>,
    val walletId: Long?,
    val toWalletId: Long?,
    val dateMillis: Long,
    val note: String,
    val flags: Set<QuickEntryFlag>
)

data class AddTransactionUiState(
    val editingId: Long? = null,
    val editingCreatedAt: Long? = null,
    val quickEntryText: String = "",
    val quickEntryCandidates: List<CategoryCandidate> = emptyList(),
    val quickEntryFlags: Set<QuickEntryFlag> = emptySet(),
    val quickEntryExtraEntries: List<ParsedEntry> = emptyList(),
    val quickEntryParsedCount: Int = 0,
    val canOpenQuickEntryPreview: Boolean = false,
    val showQuickEntryPreview: Boolean = false,
    val previewEntries: List<QuickEntryPreviewItem> = emptyList(),
    val previewValidationError: Boolean = false,
    val voiceEntryEnabled: Boolean = true,
    val voiceInfoShown: Boolean = false,
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
    val predictedCategoryId: Long? = null,
    val quickEntryCandidates: List<CategoryCandidate> = emptyList(),
    val quickEntryFlags: Set<QuickEntryFlag> = emptySet(),
    val quickEntryExtraEntries: List<ParsedEntry> = emptyList(),
    val lastParseResult: QuickEntryParseResult? = null,
    val showQuickEntryPreview: Boolean = false,
    val previewEntries: List<QuickEntryPreviewItem> = emptyList(),
    val previewValidationError: Boolean = false,
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
    private val budgetNotificationHelper: BudgetNotificationHelper,
    private val quickEntryLearningManager: QuickEntryLearningManager
) : ViewModel() {

    private val draft = MutableStateFlow(FormDraft())

    @Volatile
    private var latestSettings: AppSettings = AppSettings()

    init {
        viewModelScope.launch {
            quickEntryLearningManager.ensureInitialized()
        }
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { latestSettings = it }
        }
    }

    val uiState: StateFlow<AddTransactionUiState> = combine(
        draft,
        categoryRepository.observeAll(),
        walletRepository.observeAll(),
        pieceRepository.observeAllPieces(),
        settingsRepository.settingsFlow
    ) { currentDraft, categories, wallets, pieces, settings ->
        val expenseCategories = categories.filter {
            it.type == CategoryType.EXPENSE && (!it.isArchived || it.id == currentDraft.selectedCategoryId)
        }
        val incomeCategories = categories.filter {
            it.type == CategoryType.INCOME && (!it.isArchived || it.id == currentDraft.selectedCategoryId)
        }

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

        val lastResult = currentDraft.lastParseResult
        val canPreview = lastResult != null && QuickEntryPreviewPolicy.shouldShowPreview(lastResult)

        AddTransactionUiState(
            editingId = currentDraft.editingId,
            editingCreatedAt = currentDraft.editingCreatedAt,
            quickEntryText = currentDraft.quickEntryText,
            quickEntryCandidates = currentDraft.quickEntryCandidates,
            quickEntryFlags = currentDraft.quickEntryFlags,
            quickEntryExtraEntries = currentDraft.quickEntryExtraEntries,
            quickEntryParsedCount = lastResult?.entries?.size ?: 0,
            canOpenQuickEntryPreview = canPreview,
            showQuickEntryPreview = currentDraft.showQuickEntryPreview,
            previewEntries = currentDraft.previewEntries,
            previewValidationError = currentDraft.previewValidationError,
            voiceEntryEnabled = settings.voiceEntryEnabled,
            voiceInfoShown = settings.voiceInfoShown,
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
                    quickEntryActive = false,
                    predictedCategoryId = null,
                    quickEntryCandidates = emptyList(),
                    quickEntryFlags = emptySet(),
                    quickEntryExtraEntries = emptyList(),
                    lastParseResult = null,
                    showQuickEntryPreview = false,
                    previewEntries = emptyList(),
                    previewValidationError = false
                )
            }
            return
        }
        val parseResult = parseQuickEntry(text, state)
        applyQuickEntryParseResult(text, parseResult, openPreviewIfNeeded = false)
    }

    fun onVoiceCandidates(candidates: List<String>, onNoResult: () -> Unit) {
        val state = uiState.value
        val selection = VoiceTranscriptSelector.selectBest(
            candidates = candidates,
            context = buildQuickEntryContext(state)
        )
        if (selection == null) {
            onNoResult()
            return
        }
        applyQuickEntryParseResult(
            text = selection.transcript,
            parseResult = selection.parseResult,
            openPreviewIfNeeded = true
        )
    }

    fun markVoiceInfoShown() {
        viewModelScope.launch {
            settingsRepository.setVoiceInfoShown(true)
        }
    }

    fun openQuickEntryPreviewIfNeeded() {
        val result = draft.value.lastParseResult ?: return
        if (!QuickEntryPreviewPolicy.shouldShowPreview(result)) return
        val state = uiState.value
        val items = buildPreviewItems(result.entries, state.wallets.firstOrNull()?.id)
        if (items.isEmpty()) return
        draft.update {
            it.copy(
                showQuickEntryPreview = true,
                previewEntries = items,
                previewValidationError = false
            )
        }
    }

    fun dismissQuickEntryPreview() {
        draft.update {
            it.copy(
                showQuickEntryPreview = false,
                previewValidationError = false
            )
        }
    }

    fun updatePreviewAmount(itemId: Long, amount: Long) {
        updatePreviewItem(itemId) { item ->
            val updatedFlags = item.flags.toMutableSet()
            if (amount > 0L) updatedFlags.remove(QuickEntryFlag.NO_AMOUNT)
            if (item.type == TransactionType.EXPENSE && amount > 1_000_000_000L) {
                updatedFlags.add(QuickEntryFlag.LARGE_AMOUNT)
            } else {
                updatedFlags.remove(QuickEntryFlag.LARGE_AMOUNT)
            }
            item.copy(amount = amount.coerceAtLeast(0L), flags = updatedFlags)
        }
    }

    fun convertPreviewAmbiguousMToBillion(itemId: Long) {
        updatePreviewItem(itemId) { item ->
            val multiplied = if (item.amount > 0L && item.amount <= Long.MAX_VALUE / 1_000L) {
                item.amount * 1_000L
            } else {
                item.amount
            }
            val updatedFlags = item.flags.toMutableSet().apply {
                remove(QuickEntryFlag.AMBIGUOUS_M)
                if (item.type == TransactionType.EXPENSE && multiplied > 1_000_000_000L) {
                    add(QuickEntryFlag.LARGE_AMOUNT)
                }
            }
            item.copy(amount = multiplied, flags = updatedFlags)
        }
    }

    fun updatePreviewType(itemId: Long, type: TransactionType) {
        val state = uiState.value
        val categoriesForType = when (type) {
            TransactionType.EXPENSE -> state.expenseCategories
            TransactionType.INCOME -> state.incomeCategories
            TransactionType.TRANSFER -> emptyList()
        }
        updatePreviewItem(itemId) { item ->
            val nextCategoryId = if (type == TransactionType.TRANSFER) {
                null
            } else {
                item.categoryId?.takeIf { id -> categoriesForType.any { it.id == id } }
            }
            val nextToWalletId = if (type == TransactionType.TRANSFER) {
                item.toWalletId ?: state.wallets.firstOrNull { it.id != item.walletId }?.id
            } else {
                null
            }
            val updatedFlags = item.flags.toMutableSet().apply {
                if (type == TransactionType.TRANSFER || nextCategoryId != null) {
                    remove(QuickEntryFlag.NO_CATEGORY)
                } else {
                    add(QuickEntryFlag.NO_CATEGORY)
                }
                if (type == TransactionType.EXPENSE && item.amount > 1_000_000_000L) {
                    add(QuickEntryFlag.LARGE_AMOUNT)
                } else {
                    remove(QuickEntryFlag.LARGE_AMOUNT)
                }
            }
            item.copy(
                type = type,
                categoryId = nextCategoryId,
                toWalletId = nextToWalletId,
                flags = updatedFlags
            )
        }
    }

    fun updatePreviewCategory(itemId: Long, categoryId: Long) {
        updatePreviewItem(itemId) { item ->
            item.copy(
                categoryId = categoryId,
                flags = item.flags - QuickEntryFlag.NO_CATEGORY
            )
        }
    }

    fun updatePreviewWallet(itemId: Long, walletId: Long) {
        updatePreviewItem(itemId) { item ->
            item.copy(walletId = walletId)
        }
    }

    fun updatePreviewToWallet(itemId: Long, toWalletId: Long) {
        updatePreviewItem(itemId) { item ->
            item.copy(toWalletId = toWalletId)
        }
    }

    fun updatePreviewDate(itemId: Long, dateMillis: Long) {
        updatePreviewItem(itemId) { item ->
            item.copy(dateMillis = dateMillis)
        }
    }

    fun updatePreviewNote(itemId: Long, note: String) {
        updatePreviewItem(itemId) { item ->
            item.copy(note = note)
        }
    }

    fun removePreviewEntry(itemId: Long) {
        draft.update { current ->
            val remaining = current.previewEntries.filterNot { it.id == itemId }
            current.copy(
                previewEntries = remaining,
                showQuickEntryPreview = remaining.isNotEmpty() && current.showQuickEntryPreview,
                previewValidationError = false
            )
        }
    }

    fun saveAllPreviewEntries(onSaved: () -> Unit) {
        val state = uiState.value
        if (state.isSaving) return
        val items = state.previewEntries
        if (items.isEmpty()) {
            dismissQuickEntryPreview()
            return
        }

        val hasInvalid = items.any { item ->
            validateTransactionInput(
                type = item.type,
                amount = item.amount,
                categoryId = item.categoryId,
                walletId = item.walletId,
                toWalletId = item.toWalletId
            ) != null
        }
        if (hasInvalid) {
            draft.update { it.copy(previewValidationError = true) }
            return
        }

        draft.update { it.copy(isSaving = true, previewValidationError = false) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val allCategories = state.expenseCategories + state.incomeCategories
            var hasExpense = false

            for (item in items) {
                val walletId = item.walletId ?: continue
                if (item.type == TransactionType.EXPENSE) hasExpense = true
                transactionRepository.insert(
                    TransactionEntity(
                        id = 0L,
                        amount = item.amount,
                        type = item.type,
                        categoryId = if (item.type == TransactionType.TRANSFER) null else item.categoryId,
                        walletId = walletId,
                        toWalletId = if (item.type == TransactionType.TRANSFER) item.toWalletId else null,
                        date = item.dateMillis,
                        note = item.note.trim(),
                        createdAt = now
                    )
                )
                quickEntryLearningManager.learnFromSave(
                    note = item.note,
                    chosenCategoryId = if (item.type == TransactionType.TRANSFER) null else item.categoryId,
                    predictedCategoryId = item.predictedCategoryId,
                    categories = allCategories,
                    walletId = walletId
                )
            }

            if (hasExpense) {
                budgetNotificationHelper.evaluateCurrentMonthBudgets(now)
            }

            draft.update {
                it.copy(
                    isSaving = false,
                    showQuickEntryPreview = false,
                    previewEntries = emptyList(),
                    previewValidationError = false
                )
            }
            onSaved()
        }
    }

    private fun updatePreviewItem(
        itemId: Long,
        transform: (QuickEntryPreviewItem) -> QuickEntryPreviewItem
    ) {
        draft.update { current ->
            current.copy(
                previewEntries = current.previewEntries.map { item ->
                    if (item.id == itemId) transform(item) else item
                },
                previewValidationError = false
            )
        }
    }

    private fun buildQuickEntryContext(state: AddTransactionUiState): QuickEntryContext {
        val allCategories = state.expenseCategories + state.incomeCategories
        val settings = latestSettings
        return QuickEntryContext(
            categories = allCategories,
            wallets = state.wallets,
            lastUsedWalletId = settings.lastUsedWalletId,
            userCorrections = settings.quickEntryCorrections,
            historyKeywordMap = quickEntryLearningManager.historyKeywordMap,
            learningEnabled = settings.quickEntryLearningEnabled
        )
    }

    private fun parseQuickEntry(text: String, state: AddTransactionUiState): QuickEntryParseResult {
        return QuickEntryPipeline.parse(
            input = text,
            context = buildQuickEntryContext(state)
        )
    }

    private fun applyQuickEntryParseResult(
        text: String,
        parseResult: QuickEntryParseResult,
        openPreviewIfNeeded: Boolean
    ) {
        val parsed = parseResult.entries.firstOrNull()
        if (parsed == null) {
            draft.update {
                it.copy(
                    quickEntryText = text,
                    lastParseResult = parseResult,
                    showQuickEntryPreview = false,
                    previewEntries = emptyList()
                )
            }
            return
        }
        val state = uiState.value
        val extraEntries = if (parseResult.entries.size > 1) parseResult.entries.drop(1) else emptyList()
        val shouldPreview = openPreviewIfNeeded && QuickEntryPreviewPolicy.shouldShowPreview(parseResult)
        val previewItems = if (shouldPreview) {
            buildPreviewItems(parseResult.entries, state.wallets.firstOrNull()?.id)
        } else {
            emptyList()
        }
        draft.update {
            it.copy(
                quickEntryText = text,
                quickEntryActive = true,
                predictedCategoryId = parsed.categoryId,
                quickEntryCandidates = parsed.categoryCandidates,
                quickEntryFlags = parsed.flags,
                quickEntryExtraEntries = extraEntries,
                lastParseResult = parseResult,
                showQuickEntryPreview = shouldPreview,
                previewEntries = previewItems,
                previewValidationError = false,
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

    private fun buildPreviewItems(
        entries: List<ParsedEntry>,
        fallbackWalletId: Long?
    ): List<QuickEntryPreviewItem> {
        val now = System.currentTimeMillis()
        return entries.mapIndexed { index, entry ->
            QuickEntryPreviewItem(
                id = index.toLong() + 1L,
                amount = entry.amount ?: 0L,
                type = entry.type,
                categoryId = entry.categoryId,
                predictedCategoryId = entry.categoryId,
                categoryCandidates = entry.categoryCandidates.take(3),
                walletId = entry.walletId ?: fallbackWalletId,
                toWalletId = entry.toWalletId,
                dateMillis = entry.dateMillis ?: now,
                note = entry.note,
                flags = entry.flags
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

            // Simpan entri tambahan jika Ketik cepat mendeteksi lebih dari satu transaksi
            if (state.editingId == null && state.quickEntryExtraEntries.isNotEmpty()) {
                for (extra in state.quickEntryExtraEntries) {
                    val extraAmount = extra.amount ?: continue
                    val extraWalletId = extra.walletId ?: walletId
                    if (extraAmount <= 0L) continue
                    transactionRepository.insert(
                        TransactionEntity(
                            id = 0L,
                            amount = extraAmount,
                            type = extra.type,
                            categoryId = if (extra.type == TransactionType.TRANSFER) null else extra.categoryId,
                            walletId = extraWalletId,
                            toWalletId = if (extra.type == TransactionType.TRANSFER) extra.toWalletId else null,
                            date = extra.dateMillis ?: entity.date,
                            note = extra.note.trim(),
                            createdAt = now
                        )
                    )
                }
            }

            // Pelajari koreksi kategori & perbarui indeks riwayat
            val allCategories = state.expenseCategories + state.incomeCategories
            quickEntryLearningManager.learnFromSave(
                note = entity.note,
                chosenCategoryId = entity.categoryId,
                predictedCategoryId = draft.value.predictedCategoryId,
                categories = allCategories,
                walletId = entity.walletId
            )

            draft.update { it.copy(isSaving = false) }
            onSaved()
        }
    }
}
