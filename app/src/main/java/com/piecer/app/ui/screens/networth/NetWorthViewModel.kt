package com.piecer.app.ui.screens.networth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.local.entity.AssetEntity
import com.piecer.app.data.local.entity.AssetType
import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.data.repository.AssetRepository
import com.piecer.app.data.repository.DebtRepository
import com.piecer.app.data.repository.TransactionRepository
import com.piecer.app.data.repository.WalletRepository
import com.piecer.app.util.AssetHistoryPoint
import com.piecer.app.util.NetWorthCalculator
import com.piecer.app.util.NetWorthSummary
import com.piecer.app.util.calculateWalletBalance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AssetFormError {
    EMPTY_NAME,
    INVALID_VALUE
}

data class AssetFormState(
    val isVisible: Boolean = false,
    val editingId: Long? = null,
    val name: String = "",
    val type: AssetType = AssetType.EMAS,
    val valueInput: String = "",
    val note: String = "",
    val error: AssetFormError? = null,
    val isSaving: Boolean = false
)

data class UpdateAssetValueState(
    val asset: AssetEntity? = null,
    val valueInput: String = "",
    val error: Boolean = false,
    val isSaving: Boolean = false
)

data class NetWorthUiState(
    val summary: NetWorthSummary = NetWorthSummary(0L, 0L, 0L, 0L),
    val assets: List<AssetEntity> = emptyList(),
    val timeline: List<AssetHistoryPoint> = emptyList(),
    val isBalanceHidden: Boolean = false,
    val form: AssetFormState = AssetFormState(),
    val updateValueDialog: UpdateAssetValueState = UpdateAssetValueState(),
    val pendingDelete: AssetEntity? = null
)

@HiltViewModel
class NetWorthViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
    walletRepository: WalletRepository,
    transactionRepository: TransactionRepository,
    debtRepository: DebtRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val formState = MutableStateFlow(AssetFormState())
    private val updateValueState = MutableStateFlow(UpdateAssetValueState())
    private val pendingDeleteState = MutableStateFlow<AssetEntity?>(null)

    private val dataFlow = combine(
        assetRepository.observeAllAssets(),
        assetRepository.observeAllHistory(),
        walletRepository.observeAll(),
        transactionRepository.observeAll(),
        debtRepository.observeAllDebts(),
        settingsRepository.settingsFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val assets = values[0] as List<AssetEntity>
        @Suppress("UNCHECKED_CAST")
        val history = values[1] as List<com.piecer.app.data.local.entity.AssetHistoryEntity>
        @Suppress("UNCHECKED_CAST")
        val wallets = values[2] as List<com.piecer.app.data.local.entity.WalletEntity>
        @Suppress("UNCHECKED_CAST")
        val transactions = values[3] as List<com.piecer.app.data.local.entity.TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val debts = values[4] as List<com.piecer.app.data.local.entity.DebtEntity>
        val settings = values[5] as com.piecer.app.data.preferences.AppSettings

        val now = System.currentTimeMillis()
        val totalWalletBalance = wallets.sumOf { w ->
            calculateWalletBalance(
                walletId = w.id,
                initialBalance = w.initialBalance,
                transactions = transactions,
                nowMillis = now
            )
        }
        val summary = NetWorthCalculator.calculate(
            assets = assets,
            totalWalletBalance = totalWalletBalance,
            debts = debts
        )
        val timeline = NetWorthCalculator.buildAssetTimeline(assets, history)

        NetWorthUiState(
            summary = summary,
            assets = assets,
            timeline = timeline,
            isBalanceHidden = settings.isBalanceHidden
        )
    }

    val uiState: StateFlow<NetWorthUiState> = combine(
        dataFlow,
        formState,
        updateValueState,
        pendingDeleteState
    ) { base, form, updateDialog, pendingDelete ->
        base.copy(
            form = form,
            updateValueDialog = updateDialog,
            pendingDelete = pendingDelete
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NetWorthUiState()
    )

    fun openAddForm() {
        formState.value = AssetFormState(isVisible = true)
    }

    fun openEditForm(asset: AssetEntity) {
        formState.value = AssetFormState(
            isVisible = true,
            editingId = asset.id,
            name = asset.name,
            type = asset.type,
            valueInput = asset.value.toString(),
            note = asset.note
        )
    }

    fun dismissForm() {
        formState.update { it.copy(isVisible = false, error = null, isSaving = false) }
    }

    fun onNameChange(value: String) {
        formState.update { it.copy(name = value, error = null) }
    }

    fun onTypeSelect(type: AssetType) {
        formState.update { it.copy(type = type) }
    }

    fun onValueChange(value: String) {
        val digits = value.filter { it.isDigit() }
        formState.update { it.copy(valueInput = digits, error = null) }
    }

    fun onNoteChange(value: String) {
        formState.update { it.copy(note = value) }
    }

    fun saveAsset(onSuccess: () -> Unit) {
        val current = formState.value
        if (current.isSaving) return
        val name = current.name.trim()
        if (name.isEmpty()) {
            formState.update { it.copy(error = AssetFormError.EMPTY_NAME) }
            return
        }
        val value = current.valueInput.toLongOrNull()
        if (value == null || value < 0L) {
            formState.update { it.copy(error = AssetFormError.INVALID_VALUE) }
            return
        }

        formState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (current.editingId != null) {
                assetRepository.updateAssetDetails(
                    AssetEntity(
                        id = current.editingId,
                        name = name,
                        type = current.type,
                        value = value,
                        note = current.note.trim(),
                        updatedAt = now
                    )
                )
            } else {
                assetRepository.createAsset(
                    AssetEntity(
                        name = name,
                        type = current.type,
                        value = value,
                        note = current.note.trim(),
                        updatedAt = now
                    )
                )
            }
            formState.value = AssetFormState()
            onSuccess()
        }
    }

    fun openUpdateValueDialog(asset: AssetEntity) {
        updateValueState.value = UpdateAssetValueState(
            asset = asset,
            valueInput = asset.value.toString()
        )
    }

    fun dismissUpdateValueDialog() {
        updateValueState.value = UpdateAssetValueState()
    }

    fun onUpdateValueInputChange(value: String) {
        val digits = value.filter { it.isDigit() }
        updateValueState.update { it.copy(valueInput = digits, error = false) }
    }

    fun confirmUpdateValue(onSuccess: () -> Unit) {
        val current = updateValueState.value
        val asset = current.asset ?: return
        if (current.isSaving) return
        val newValue = current.valueInput.toLongOrNull()
        if (newValue == null || newValue < 0L) {
            updateValueState.update { it.copy(error = true) }
            return
        }
        updateValueState.update { it.copy(isSaving = true, error = false) }
        viewModelScope.launch {
            assetRepository.updateAssetValue(asset.id, newValue)
            updateValueState.value = UpdateAssetValueState()
            onSuccess()
        }
    }

    fun requestDelete(asset: AssetEntity) {
        pendingDeleteState.value = asset
    }

    fun dismissDelete() {
        pendingDeleteState.value = null
    }

    fun confirmDelete(onDeleted: () -> Unit) {
        val target = pendingDeleteState.value ?: return
        pendingDeleteState.value = null
        viewModelScope.launch {
            assetRepository.deleteAsset(target)
            onDeleted()
        }
    }
}
