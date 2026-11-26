package com.piecer.app.ui.screens.pieces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.PieceEntity
import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.data.repository.PieceRepository
import com.piecer.app.data.repository.TransactionRepository
import com.piecer.app.util.FinancialCycleHelper
import com.piecer.app.util.PieceBalanceSummary
import com.piecer.app.util.PieceCalculator
import com.piecer.app.util.PieceTemplateType
import com.piecer.app.util.PiecesOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PieceFormState(
    val isVisible: Boolean = false,
    val editingPiece: PieceEntity? = null,
    val name: String = "",
    val percentInput: String = "",
    val colorKey: String = "mint",
    val iconKey: String = "savings",
    val errorMessage: String? = null
)

data class PercentEditorState(
    val isVisible: Boolean = false,
    val percentsById: Map<Long, String> = emptyMap(),
    val errorMessage: String? = null
)

data class CategoryMappingState(
    val isVisible: Boolean = false,
    val targetPiece: PieceEntity? = null
)

data class PiecesUiState(
    val piecesEnabled: Boolean = false,
    val overview: PiecesOverview = PiecesOverview(
        summaries = emptyList(),
        unassignedExpense = 0L,
        totalAllocated = 0L,
        totalSpent = 0L,
        totalRemaining = 0L
    ),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val categoryToPieceId: Map<Long, Long> = emptyMap(),
    val totalPercent: Int = 0,
    val isPercentValid: Boolean = false,
    val pieceForm: PieceFormState = PieceFormState(),
    val percentEditor: PercentEditorState = PercentEditorState(),
    val categoryMapping: CategoryMappingState = CategoryMappingState(),
    val pendingDeletePiece: PieceEntity? = null
)

@HiltViewModel
class PiecesViewModel @Inject constructor(
    private val pieceRepository: PieceRepository,
    private val categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val pieceFormState = MutableStateFlow(PieceFormState())
    private val percentEditorState = MutableStateFlow(PercentEditorState())
    private val categoryMappingState = MutableStateFlow(CategoryMappingState())
    private val pendingDeleteState = MutableStateFlow<PieceEntity?>(null)

    private val dataFlow = combine(
        pieceRepository.observeAllPieces(),
        pieceRepository.observeAllPieceCategories(),
        pieceRepository.observeAllAllocations(),
        transactionRepository.observeAll(),
        categoryRepository.observeByType(CategoryType.EXPENSE),
        settingsRepository.settingsFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val pieces = values[0] as List<PieceEntity>
        @Suppress("UNCHECKED_CAST")
        val pieceCategories = values[1] as List<com.piecer.app.data.local.entity.PieceCategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val allocations = values[2] as List<com.piecer.app.data.local.entity.PieceAllocationEntity>
        @Suppress("UNCHECKED_CAST")
        val transactions = values[3] as List<com.piecer.app.data.local.entity.TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val expenseCategories = values[4] as List<CategoryEntity>
        val settings = values[5] as com.piecer.app.data.preferences.AppSettings

        val now = System.currentTimeMillis()
        val period = FinancialCycleHelper.currentPeriod(now, settings.cycleStartDay)
        val overview = PieceCalculator.calculatePiecesOverview(
            pieces = pieces,
            pieceCategories = pieceCategories,
            allocations = allocations,
            transactions = transactions,
            cyclePeriod = period,
            nowMillis = now
        )
        val catToPiece = pieceCategories.associate { it.categoryId to it.pieceId }
        val totalPercent = pieces.sumOf { it.percent }
        PiecesUiState(
            piecesEnabled = settings.piecesEnabled,
            overview = overview,
            expenseCategories = expenseCategories,
            categoryToPieceId = catToPiece,
            totalPercent = totalPercent,
            isPercentValid = PieceCalculator.isTotalPercentValid(pieces.map { it.percent })
        )
    }

    val uiState: StateFlow<PiecesUiState> = combine(
        dataFlow,
        pieceFormState,
        percentEditorState,
        categoryMappingState,
        pendingDeleteState
    ) { base, form, percents, mapping, pendingDelete ->
        base.copy(
            pieceForm = form,
            percentEditor = percents,
            categoryMapping = mapping,
            pendingDeletePiece = pendingDelete
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = PiecesUiState()
    )

    fun setPiecesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPiecesEnabled(enabled)
        }
    }

    fun applyTemplate(template: PieceTemplateType, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val drafts = PieceCalculator.templateDrafts(template)
            val entities = drafts.mapIndexed { index, item ->
                PieceEntity(
                    name = item.name,
                    percent = item.percent,
                    colorKey = item.colorKey,
                    iconKey = item.iconKey,
                    sortOrder = index,
                    createdAt = now
                )
            }
            val categories = categoryRepository.observeByType(CategoryType.EXPENSE).first()
            val mappings = PieceCalculator.defaultCategoryMappings(drafts, categories)
            pieceRepository.replaceSetup(entities, mappings)
            settingsRepository.setPiecesEnabled(true)
            onDone()
        }
    }

    fun openAddPiece() {
        pieceFormState.value = PieceFormState(
            isVisible = true,
            percentInput = "0",
            colorKey = "mint",
            iconKey = "savings"
        )
    }

    fun openEditPiece(summary: PieceBalanceSummary) {
        val piece = summary.piece
        pieceFormState.value = PieceFormState(
            isVisible = true,
            editingPiece = piece,
            name = piece.name,
            percentInput = piece.percent.toString(),
            colorKey = piece.colorKey,
            iconKey = piece.iconKey
        )
    }

    fun closePieceForm() {
        pieceFormState.update { it.copy(isVisible = false, errorMessage = null) }
    }

    fun onPieceNameChange(name: String) {
        pieceFormState.update { it.copy(name = name, errorMessage = null) }
    }

    fun onPiecePercentChange(percent: String) {
        val filtered = percent.filter { it.isDigit() }.take(3)
        pieceFormState.update { it.copy(percentInput = filtered, errorMessage = null) }
    }

    fun onPieceColorSelect(colorKey: String) {
        pieceFormState.update { it.copy(colorKey = colorKey) }
    }

    fun savePiece(
        emptyNameError: String,
        invalidPercentError: String,
        totalMustBe100Error: (Int) -> String
    ) {
        val form = pieceFormState.value
        val trimmedName = form.name.trim()
        if (trimmedName.isEmpty()) {
            pieceFormState.update { it.copy(errorMessage = emptyNameError) }
            return
        }
        val percent = form.percentInput.toIntOrNull()
        if (percent == null || percent !in 0..100) {
            pieceFormState.update { it.copy(errorMessage = invalidPercentError) }
            return
        }

        val currentPieces = uiState.value.overview.summaries.map { it.piece }
        val otherSum = currentPieces
            .filter { it.id != form.editingPiece?.id }
            .sumOf { it.percent }
        val newTotal = otherSum + percent
        if (newTotal != 100) {
            pieceFormState.update { it.copy(errorMessage = totalMustBe100Error(newTotal)) }
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val editing = form.editingPiece
            if (editing != null) {
                pieceRepository.updatePiece(
                    editing.copy(
                        name = trimmedName,
                        percent = percent,
                        colorKey = form.colorKey,
                        iconKey = form.iconKey
                    )
                )
            } else {
                val nextSort = (currentPieces.maxOfOrNull { it.sortOrder } ?: -1) + 1
                pieceRepository.insertPiece(
                    PieceEntity(
                        name = trimmedName,
                        percent = percent,
                        colorKey = form.colorKey,
                        iconKey = form.iconKey,
                        sortOrder = nextSort,
                        createdAt = now
                    )
                )
            }
            pieceFormState.value = PieceFormState(isVisible = false)
        }
    }

    fun openPercentEditor() {
        val map = uiState.value.overview.summaries.associate {
            it.piece.id to it.piece.percent.toString()
        }
        percentEditorState.value = PercentEditorState(
            isVisible = true,
            percentsById = map
        )
    }

    fun closePercentEditor() {
        percentEditorState.update { it.copy(isVisible = false, errorMessage = null) }
    }

    fun onEditorPercentChange(pieceId: Long, value: String) {
        val filtered = value.filter { it.isDigit() }.take(3)
        percentEditorState.update { state ->
            state.copy(
                percentsById = state.percentsById + (pieceId to filtered),
                errorMessage = null
            )
        }
    }

    fun saveAllPercents(totalMustBe100Error: (Int) -> String) {
        val state = percentEditorState.value
        val parsed = state.percentsById.mapValues { it.value.toIntOrNull() ?: -1 }
        if (parsed.values.any { it !in 0..100 }) {
            percentEditorState.update { it.copy(errorMessage = totalMustBe100Error(-1)) }
            return
        }
        val total = parsed.values.sum()
        if (total != 100) {
            percentEditorState.update { it.copy(errorMessage = totalMustBe100Error(total)) }
            return
        }

        viewModelScope.launch {
            val summaries = uiState.value.overview.summaries
            summaries.forEach { summary ->
                val newPercent = parsed[summary.piece.id] ?: summary.piece.percent
                if (newPercent != summary.piece.percent) {
                    pieceRepository.updatePiece(summary.piece.copy(percent = newPercent))
                }
            }
            percentEditorState.value = PercentEditorState(isVisible = false)
        }
    }

    fun openCategoryMapping(piece: PieceEntity) {
        categoryMappingState.value = CategoryMappingState(
            isVisible = true,
            targetPiece = piece
        )
    }

    fun closeCategoryMapping() {
        categoryMappingState.value = CategoryMappingState(isVisible = false)
    }

    fun toggleCategoryForPiece(categoryId: Long, pieceId: Long, assigned: Boolean) {
        viewModelScope.launch {
            pieceRepository.setCategoryPiece(categoryId, if (assigned) pieceId else null)
        }
    }

    fun requestDeletePiece(piece: PieceEntity) {
        pendingDeleteState.value = piece
    }

    fun dismissDeletePiece() {
        pendingDeleteState.value = null
    }

    fun confirmDeletePiece() {
        val target = pendingDeleteState.value ?: return
        viewModelScope.launch {
            pieceRepository.deletePiece(target)
            pendingDeleteState.value = null
        }
    }
}
