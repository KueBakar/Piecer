package com.piecer.app.ui.screens.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.local.entity.DebtDirection
import com.piecer.app.data.local.entity.DebtEntity
import com.piecer.app.data.local.entity.DebtPaymentEntity
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.data.repository.DebtRepository
import com.piecer.app.data.repository.WalletRepository
import com.piecer.app.util.DebtCalculator
import com.piecer.app.util.DebtReminderHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DebtFilter {
    ALL,
    OWE_ME,
    I_OWE
}

enum class DebtFormError {
    EMPTY_NAME,
    INVALID_AMOUNT
}

data class DebtFormState(
    val isVisible: Boolean = false,
    val editingId: Long? = null,
    val editingPaidAmount: Long = 0L,
    val editingCreatedAt: Long = System.currentTimeMillis(),
    val personName: String = "",
    val amountInput: String = "",
    val direction: DebtDirection = DebtDirection.OWE_ME,
    val dueDate: Long? = null,
    val note: String = "",
    val walletId: Long? = null,
    val recordAsTransaction: Boolean = false,
    val error: DebtFormError? = null,
    val isSaving: Boolean = false
)

data class DebtPaymentFormState(
    val debt: DebtEntity? = null,
    val amountInput: String = "",
    val walletId: Long? = null,
    val note: String = "",
    val recordAsTransaction: Boolean = false,
    val error: Boolean = false,
    val isSaving: Boolean = false
)

data class DebtListItem(
    val debt: DebtEntity,
    val remaining: Long,
    val progress: Float,
    val isPaidOff: Boolean,
    val isOverdue: Boolean,
    val payments: List<DebtPaymentEntity>
)

data class DebtsUiState(
    val filter: DebtFilter = DebtFilter.ALL,
    val totalOweMe: Long = 0L,
    val totalIOwe: Long = 0L,
    val isBalanceHidden: Boolean = false,
    val items: List<DebtListItem> = emptyList(),
    val wallets: List<WalletEntity> = emptyList(),
    val form: DebtFormState = DebtFormState(),
    val paymentForm: DebtPaymentFormState = DebtPaymentFormState(),
    val pendingDelete: DebtEntity? = null
)

@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    walletRepository: WalletRepository,
    settingsRepository: SettingsRepository,
    private val debtReminderHelper: DebtReminderHelper
) : ViewModel() {

    private val filterState = MutableStateFlow(DebtFilter.ALL)
    private val formState = MutableStateFlow(DebtFormState())
    private val paymentFormState = MutableStateFlow(DebtPaymentFormState())
    private val pendingDeleteState = MutableStateFlow<DebtEntity?>(null)

    private val dataFlow = combine(
        debtRepository.observeAllDebts(),
        debtRepository.observeAllPayments(),
        walletRepository.observeAll(),
        settingsRepository.settingsFlow,
        filterState
    ) { debts, payments, wallets, settings, filter ->
        val paymentsByDebt = payments.groupBy { it.debtId }
        val now = System.currentTimeMillis()
        val filtered = when (filter) {
            DebtFilter.ALL -> debts
            DebtFilter.OWE_ME -> debts.filter { it.direction == DebtDirection.OWE_ME }
            DebtFilter.I_OWE -> debts.filter { it.direction == DebtDirection.I_OWE }
        }
        val items = filtered.map { d ->
            DebtListItem(
                debt = d,
                remaining = DebtCalculator.remainingAmount(d),
                progress = DebtCalculator.progress(d),
                isPaidOff = DebtCalculator.isPaidOff(d),
                isOverdue = DebtCalculator.isOverdue(d, now),
                payments = paymentsByDebt[d.id].orEmpty()
            )
        }
        DebtsUiState(
            filter = filter,
            totalOweMe = DebtCalculator.totalRemainingByDirection(debts, DebtDirection.OWE_ME),
            totalIOwe = DebtCalculator.totalRemainingByDirection(debts, DebtDirection.I_OWE),
            isBalanceHidden = settings.isBalanceHidden,
            items = items,
            wallets = wallets
        )
    }

    val uiState: StateFlow<DebtsUiState> = combine(
        dataFlow,
        formState,
        paymentFormState,
        pendingDeleteState
    ) { base, form, paymentForm, pendingDelete ->
        base.copy(
            form = form,
            paymentForm = paymentForm,
            pendingDelete = pendingDelete
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DebtsUiState()
    )

    fun setFilter(filter: DebtFilter) {
        filterState.value = filter
    }

    fun openAddForm() {
        val defaultWalletId = uiState.value.wallets.firstOrNull()?.id
        formState.value = DebtFormState(
            isVisible = true,
            walletId = defaultWalletId,
            recordAsTransaction = false
        )
    }

    fun openEditForm(debt: DebtEntity) {
        formState.value = DebtFormState(
            isVisible = true,
            editingId = debt.id,
            editingPaidAmount = debt.paidAmount,
            editingCreatedAt = debt.createdAt,
            personName = debt.personName,
            amountInput = debt.totalAmount.toString(),
            direction = debt.direction,
            dueDate = debt.dueDate,
            note = debt.note,
            walletId = debt.walletId ?: uiState.value.wallets.firstOrNull()?.id,
            recordAsTransaction = debt.recordAsTransaction
        )
    }

    fun dismissForm() {
        formState.update { it.copy(isVisible = false, error = null, isSaving = false) }
    }

    fun onPersonNameChange(value: String) {
        formState.update { it.copy(personName = value, error = null) }
    }

    fun onAmountChange(value: String) {
        val digits = value.filter { it.isDigit() }
        formState.update { it.copy(amountInput = digits, error = null) }
    }

    fun onDirectionSelect(direction: DebtDirection) {
        formState.update { it.copy(direction = direction) }
    }

    fun onDueDateSelect(dueDate: Long?) {
        formState.update { it.copy(dueDate = dueDate) }
    }

    fun onNoteChange(value: String) {
        formState.update { it.copy(note = value) }
    }

    fun onWalletSelect(walletId: Long?) {
        formState.update { it.copy(walletId = walletId) }
    }

    fun onRecordAsTransactionChange(enabled: Boolean) {
        formState.update { it.copy(recordAsTransaction = enabled) }
    }

    fun saveDebt(onSuccess: () -> Unit) {
        val current = formState.value
        if (current.isSaving) return
        val name = current.personName.trim()
        if (name.isEmpty()) {
            formState.update { it.copy(error = DebtFormError.EMPTY_NAME) }
            return
        }
        val amount = current.amountInput.toLongOrNull() ?: 0L
        if (amount <= 0L) {
            formState.update { it.copy(error = DebtFormError.INVALID_AMOUNT) }
            return
        }

        formState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (current.editingId != null) {
                val updated = DebtEntity(
                    id = current.editingId,
                    personName = name,
                    totalAmount = amount,
                    paidAmount = current.editingPaidAmount.coerceAtMost(amount),
                    direction = current.direction,
                    dueDate = current.dueDate,
                    note = current.note.trim(),
                    walletId = current.walletId,
                    recordAsTransaction = current.recordAsTransaction,
                    createdAt = current.editingCreatedAt
                )
                debtRepository.updateDebt(updated)
                debtReminderHelper.scheduleReminderForDebt(updated.id, updated.dueDate)
            } else {
                val entity = DebtEntity(
                    personName = name,
                    totalAmount = amount,
                    paidAmount = 0L,
                    direction = current.direction,
                    dueDate = current.dueDate,
                    note = current.note.trim(),
                    walletId = current.walletId,
                    recordAsTransaction = current.recordAsTransaction,
                    createdAt = now
                )
                val id = debtRepository.createDebt(
                    debt = entity,
                    initialRecordAsTransaction = current.recordAsTransaction
                )
                debtReminderHelper.scheduleReminderForDebt(id, entity.dueDate)
            }
            formState.value = DebtFormState()
            onSuccess()
        }
    }

    fun openPaymentForm(debt: DebtEntity) {
        val remaining = DebtCalculator.remainingAmount(debt)
        paymentFormState.value = DebtPaymentFormState(
            debt = debt,
            amountInput = remaining.toString(),
            walletId = debt.walletId ?: uiState.value.wallets.firstOrNull()?.id,
            note = "",
            recordAsTransaction = debt.recordAsTransaction
        )
    }

    fun dismissPaymentForm() {
        paymentFormState.value = DebtPaymentFormState()
    }

    fun onPaymentAmountChange(value: String) {
        val digits = value.filter { it.isDigit() }
        paymentFormState.update { it.copy(amountInput = digits, error = false) }
    }

    fun onPaymentWalletSelect(walletId: Long?) {
        paymentFormState.update { it.copy(walletId = walletId) }
    }

    fun onPaymentNoteChange(value: String) {
        paymentFormState.update { it.copy(note = value) }
    }

    fun onPaymentRecordAsTransactionChange(enabled: Boolean) {
        paymentFormState.update { it.copy(recordAsTransaction = enabled) }
    }

    fun submitPayment(onSuccess: () -> Unit) {
        val current = paymentFormState.value
        val debt = current.debt ?: return
        if (current.isSaving) return
        val amount = current.amountInput.toLongOrNull() ?: 0L
        if (amount <= 0L) {
            paymentFormState.update { it.copy(error = true) }
            return
        }
        paymentFormState.update { it.copy(isSaving = true, error = false) }
        viewModelScope.launch {
            debtRepository.addPayment(
                debtId = debt.id,
                amount = amount,
                date = System.currentTimeMillis(),
                walletId = current.walletId,
                note = current.note.trim(),
                recordAsTransaction = current.recordAsTransaction
            )
            paymentFormState.value = DebtPaymentFormState()
            onSuccess()
        }
    }

    fun markPaidOff(debt: DebtEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            debtRepository.markAsPaid(
                debt = debt,
                date = System.currentTimeMillis(),
                walletId = debt.walletId,
                recordAsTransaction = debt.recordAsTransaction
            )
            onSuccess()
        }
    }

    fun requestDelete(debt: DebtEntity) {
        pendingDeleteState.value = debt
    }

    fun dismissDelete() {
        pendingDeleteState.value = null
    }

    fun confirmDelete(onDeleted: () -> Unit) {
        val target = pendingDeleteState.value ?: return
        pendingDeleteState.value = null
        viewModelScope.launch {
            debtRepository.deleteDebt(target)
            onDeleted()
        }
    }
}
