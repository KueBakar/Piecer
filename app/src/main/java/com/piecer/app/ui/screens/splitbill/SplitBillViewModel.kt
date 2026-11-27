package com.piecer.app.ui.screens.splitbill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.local.entity.DebtDirection
import com.piecer.app.data.local.entity.DebtEntity
import com.piecer.app.data.repository.DebtRepository
import com.piecer.app.util.SplitBillCalculator
import com.piecer.app.util.SplitBillResult
import com.piecer.app.util.SplitParticipantInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SplitMode {
    EQUAL,
    CUSTOM
}

data class SplitParticipantRow(
    val id: Int,
    val name: String,
    val amountInput: String = "",
    val includeAsReceivable: Boolean = true
)

data class SplitBillUiState(
    val title: String = "",
    val mode: SplitMode = SplitMode.EQUAL,
    val totalInput: String = "",
    val taxPercentInput: String = "",
    val servicePercentInput: String = "",
    val participants: List<SplitParticipantRow> = listOf(
        SplitParticipantRow(id = 1, name = "Saya", includeAsReceivable = false),
        SplitParticipantRow(id = 2, name = "Teman 1", includeAsReceivable = true)
    ),
    val nextId: Int = 3,
    val isCreatingDebts: Boolean = false
) {
    val result: SplitBillResult
        get() {
            val tax = taxPercentInput.toIntOrNull()?.coerceIn(0, 100) ?: 0
            val svc = servicePercentInput.toIntOrNull()?.coerceIn(0, 100) ?: 0
            return when (mode) {
                SplitMode.EQUAL -> SplitBillCalculator.splitEqual(
                    subtotal = totalInput.toLongOrNull() ?: 0L,
                    participantNames = participants.map { it.name },
                    taxPercent = tax,
                    servicePercent = svc
                )
                SplitMode.CUSTOM -> SplitBillCalculator.splitCustom(
                    participants = participants.map {
                        SplitParticipantInput(
                            name = it.name,
                            customAmount = it.amountInput.toLongOrNull() ?: 0L
                        )
                    },
                    taxPercent = tax,
                    servicePercent = svc
                )
            }
        }
}

@HiltViewModel
class SplitBillViewModel @Inject constructor(
    private val debtRepository: DebtRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplitBillUiState())
    val uiState: StateFlow<SplitBillUiState> = _uiState.asStateFlow()

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun onModeChange(mode: SplitMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun onTotalChange(value: String) {
        val digits = value.filter { it.isDigit() }
        _uiState.update { it.copy(totalInput = digits) }
    }

    fun onTaxPercentChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(3)
        _uiState.update { it.copy(taxPercentInput = digits) }
    }

    fun onServicePercentChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(3)
        _uiState.update { it.copy(servicePercentInput = digits) }
    }

    fun addParticipant() {
        _uiState.update { state ->
            val id = state.nextId
            state.copy(
                participants = state.participants + SplitParticipantRow(
                    id = id,
                    name = "Teman $id",
                    includeAsReceivable = true
                ),
                nextId = id + 1
            )
        }
    }

    fun removeParticipant(id: Int) {
        _uiState.update { state ->
            if (state.participants.size <= 2) state
            else state.copy(participants = state.participants.filterNot { it.id == id })
        }
    }

    fun updateParticipantName(id: Int, name: String) {
        _uiState.update { state ->
            state.copy(
                participants = state.participants.map {
                    if (it.id == id) it.copy(name = name) else it
                }
            )
        }
    }

    fun updateParticipantAmount(id: Int, value: String) {
        val digits = value.filter { it.isDigit() }
        _uiState.update { state ->
            state.copy(
                participants = state.participants.map {
                    if (it.id == id) it.copy(amountInput = digits) else it
                }
            )
        }
    }

    fun toggleParticipantReceivable(id: Int) {
        _uiState.update { state ->
            state.copy(
                participants = state.participants.map {
                    if (it.id == id) it.copy(includeAsReceivable = !it.includeAsReceivable) else it
                }
            )
        }
    }

    fun createReceivables(onCreated: (Int) -> Unit) {
        val current = _uiState.value
        if (current.isCreatingDebts) return
        val result = current.result
        val targets = current.participants.zip(result.shares)
            .filter { (row, share) -> row.includeAsReceivable && share.amount > 0L }
        if (targets.isEmpty()) {
            onCreated(0)
            return
        }

        _uiState.update { it.copy(isCreatingDebts = true) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val noteTitle = current.title.trim().ifEmpty { "Patungan" }
            for ((_, share) in targets) {
                debtRepository.createDebt(
                    debt = DebtEntity(
                        personName = share.name,
                        totalAmount = share.amount,
                        paidAmount = 0L,
                        direction = DebtDirection.OWE_ME,
                        dueDate = null,
                        note = noteTitle,
                        walletId = null,
                        recordAsTransaction = false,
                        createdAt = now
                    ),
                    initialRecordAsTransaction = false
                )
            }
            _uiState.update { it.copy(isCreatingDebts = false) }
            onCreated(targets.size)
        }
    }
}
