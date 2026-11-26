package com.piecer.app.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.local.entity.GoalEntity
import com.piecer.app.data.repository.GoalRepository
import com.piecer.app.util.GoalCalculator
import com.piecer.app.util.GoalProgress
import com.piecer.app.util.parseRupiahInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalCardItem(
    val goal: GoalEntity,
    val progress: GoalProgress
)

enum class GoalFormError {
    EMPTY_NAME,
    INVALID_TARGET,
    INVALID_SAVED,
    INVALID_TOPUP
}

data class GoalFormState(
    val isVisible: Boolean = false,
    val editingGoalId: Long? = null,
    val editingCreatedAt: Long? = null,
    val name: String = "",
    val targetInput: String = "",
    val savedInput: String = "",
    val deadlineMillis: Long? = null,
    val iconKey: String = "savings",
    val colorKey: String = "mint",
    val error: GoalFormError? = null,
    val isSaving: Boolean = false
)

data class TopUpState(
    val goal: GoalEntity? = null,
    val amountInput: String = "",
    val error: GoalFormError? = null,
    val isSaving: Boolean = false
)

data class GoalsUiState(
    val items: List<GoalCardItem> = emptyList(),
    val form: GoalFormState = GoalFormState(),
    val topUp: TopUpState = TopUpState(),
    val pendingDeleteItem: GoalCardItem? = null
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val formState = MutableStateFlow(GoalFormState())
    private val topUpState = MutableStateFlow(TopUpState())
    private val pendingDelete = MutableStateFlow<GoalCardItem?>(null)

    val uiState: StateFlow<GoalsUiState> = combine(
        goalRepository.observeAll(),
        formState,
        topUpState,
        pendingDelete
    ) { goals, form, topUp, deleteTarget ->
        val now = System.currentTimeMillis()
        val items = goals.map { goal ->
            GoalCardItem(
                goal = goal,
                progress = GoalCalculator.calculate(
                    savedAmount = goal.savedAmount,
                    targetAmount = goal.targetAmount,
                    deadlineMillis = goal.deadline,
                    nowMillis = now
                )
            )
        }
        GoalsUiState(
            items = items,
            form = form,
            topUp = topUp,
            pendingDeleteItem = deleteTarget
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = GoalsUiState()
    )

    fun openAddForm() {
        formState.value = GoalFormState(isVisible = true)
    }

    fun openEditForm(item: GoalCardItem) {
        val g = item.goal
        formState.value = GoalFormState(
            isVisible = true,
            editingGoalId = g.id,
            editingCreatedAt = g.createdAt,
            name = g.name,
            targetInput = g.targetAmount.toString(),
            savedInput = g.savedAmount.toString(),
            deadlineMillis = g.deadline,
            iconKey = g.iconKey,
            colorKey = g.colorKey
        )
    }

    fun closeForm() {
        formState.update { it.copy(isVisible = false, error = null) }
    }

    fun onNameChange(value: String) {
        formState.update { it.copy(name = value, error = null) }
    }

    fun onTargetInputChange(value: String) {
        formState.update { it.copy(targetInput = value, error = null) }
    }

    fun onSavedInputChange(value: String) {
        formState.update { it.copy(savedInput = value, error = null) }
    }

    fun onDeadlineSelect(millis: Long?) {
        formState.update { it.copy(deadlineMillis = millis, error = null) }
    }

    fun onIconSelect(iconKey: String) {
        formState.update { it.copy(iconKey = iconKey) }
    }

    fun onColorSelect(colorKey: String) {
        formState.update { it.copy(colorKey = colorKey) }
    }

    fun saveGoal(onSaved: () -> Unit) {
        val form = formState.value
        if (form.isSaving) return

        val trimmedName = form.name.trim()
        if (trimmedName.isEmpty()) {
            formState.update { it.copy(error = GoalFormError.EMPTY_NAME) }
            return
        }

        val targetAmount = parseRupiahInput(form.targetInput)
        if (targetAmount == null || targetAmount <= 0L) {
            formState.update { it.copy(error = GoalFormError.INVALID_TARGET) }
            return
        }

        val savedAmount = if (form.savedInput.isBlank()) {
            0L
        } else {
            val parsed = parseRupiahInput(form.savedInput)
            if (parsed == null || parsed < 0L) {
                formState.update { it.copy(error = GoalFormError.INVALID_SAVED) }
                return
            }
            parsed
        }

        formState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = GoalEntity(
                id = form.editingGoalId ?: 0L,
                name = trimmedName,
                targetAmount = targetAmount,
                savedAmount = savedAmount,
                deadline = form.deadlineMillis,
                iconKey = form.iconKey,
                colorKey = form.colorKey,
                createdAt = form.editingCreatedAt ?: now
            )
            if (form.editingGoalId != null) {
                goalRepository.update(entity)
            } else {
                goalRepository.insert(entity)
            }
            formState.value = GoalFormState(isVisible = false)
            onSaved()
        }
    }

    fun openTopUp(goal: GoalEntity) {
        topUpState.value = TopUpState(goal = goal)
    }

    fun closeTopUp() {
        topUpState.value = TopUpState()
    }

    fun onTopUpInputChange(value: String) {
        topUpState.update { it.copy(amountInput = value, error = null) }
    }

    fun confirmTopUp(onSaved: () -> Unit) {
        val current = topUpState.value
        val goal = current.goal ?: return
        if (current.isSaving) return

        val amount = parseRupiahInput(current.amountInput)
        if (amount == null || amount <= 0L) {
            topUpState.update { it.copy(error = GoalFormError.INVALID_TOPUP) }
            return
        }

        topUpState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            goalRepository.addSavings(goal.id, amount)
            topUpState.value = TopUpState()
            onSaved()
        }
    }

    fun requestDelete(item: GoalCardItem) {
        pendingDelete.value = item
    }

    fun dismissDelete() {
        pendingDelete.value = null
    }

    fun confirmDelete(onDeleted: () -> Unit) {
        val target = pendingDelete.value ?: return
        viewModelScope.launch {
            goalRepository.delete(target.goal)
            pendingDelete.value = null
            onDeleted()
        }
    }
}
