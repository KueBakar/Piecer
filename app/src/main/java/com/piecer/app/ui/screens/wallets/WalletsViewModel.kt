package com.piecer.app.ui.screens.wallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.local.dao.WalletWithBalance
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.data.local.entity.WalletType
import com.piecer.app.data.repository.WalletRepository
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

enum class WalletFormError {
    EMPTY_NAME,
    INVALID_BALANCE
}

data class WalletFormState(
    val isVisible: Boolean = false,
    val editingWalletId: Long? = null,
    val editingCreatedAt: Long? = null,
    val name: String = "",
    val type: WalletType = WalletType.CASH,
    val initialBalanceInput: String = "",
    val iconKey: String = "wallet",
    val colorKey: String = "ink",
    val error: WalletFormError? = null,
    val isSaving: Boolean = false
)

data class WalletsUiState(
    val wallets: List<WalletWithBalance> = emptyList(),
    val totalBalance: Long = 0L,
    val canDeleteWallet: Boolean = false,
    val form: WalletFormState = WalletFormState(),
    val pendingDeleteWallet: WalletWithBalance? = null
)

@HiltViewModel
class WalletsViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val formState = MutableStateFlow(WalletFormState())
    private val pendingDelete = MutableStateFlow<WalletWithBalance?>(null)

    val uiState: StateFlow<WalletsUiState> = combine(
        walletRepository.observeWalletsWithBalance(),
        formState,
        pendingDelete
    ) { wallets, form, deleteTarget ->
        WalletsUiState(
            wallets = wallets,
            totalBalance = wallets.sumOf { it.currentBalance },
            canDeleteWallet = wallets.size > 1,
            form = form,
            pendingDeleteWallet = deleteTarget
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = WalletsUiState()
    )

    fun openAddForm() {
        formState.value = WalletFormState(isVisible = true)
    }

    fun openEditForm(wallet: WalletWithBalance) {
        formState.value = WalletFormState(
            isVisible = true,
            editingWalletId = wallet.walletId,
            editingCreatedAt = wallet.createdAt,
            name = wallet.name,
            type = wallet.type,
            initialBalanceInput = if (wallet.initialBalance == 0L) "" else wallet.initialBalance.toString(),
            iconKey = wallet.iconKey,
            colorKey = wallet.colorKey
        )
    }

    fun closeForm() {
        formState.update { it.copy(isVisible = false, error = null) }
    }

    fun onNameChange(name: String) {
        formState.update { it.copy(name = name, error = null) }
    }

    fun onTypeSelect(type: WalletType) {
        formState.update { it.copy(type = type) }
    }

    fun onInitialBalanceChange(input: String) {
        formState.update { it.copy(initialBalanceInput = input, error = null) }
    }

    fun onIconSelect(iconKey: String) {
        formState.update { it.copy(iconKey = iconKey) }
    }

    fun onColorSelect(colorKey: String) {
        formState.update { it.copy(colorKey = colorKey) }
    }

    fun saveWallet(onSaved: () -> Unit) {
        val form = formState.value
        if (form.isSaving) return

        val trimmedName = form.name.trim()
        if (trimmedName.isEmpty()) {
            formState.update { it.copy(error = WalletFormError.EMPTY_NAME) }
            return
        }

        val initialBalance = if (form.initialBalanceInput.isBlank()) {
            0L
        } else {
            parseRupiahInput(form.initialBalanceInput) ?: run {
                formState.update { it.copy(error = WalletFormError.INVALID_BALANCE) }
                return
            }
        }

        formState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = WalletEntity(
                id = form.editingWalletId ?: 0L,
                name = trimmedName,
                type = form.type,
                initialBalance = initialBalance,
                iconKey = form.iconKey,
                colorKey = form.colorKey,
                createdAt = form.editingCreatedAt ?: now
            )
            if (form.editingWalletId != null) {
                walletRepository.update(entity)
            } else {
                walletRepository.insert(entity)
            }
            formState.value = WalletFormState(isVisible = false)
            onSaved()
        }
    }

    fun requestDelete(wallet: WalletWithBalance) {
        if (!uiState.value.canDeleteWallet) return
        pendingDelete.value = wallet
    }

    fun dismissDelete() {
        pendingDelete.value = null
    }

    fun confirmDelete(onDeleted: () -> Unit) {
        val target = pendingDelete.value ?: return
        if (!uiState.value.canDeleteWallet) {
            pendingDelete.value = null
            return
        }
        viewModelScope.launch {
            walletRepository.delete(
                WalletEntity(
                    id = target.walletId,
                    name = target.name,
                    type = target.type,
                    initialBalance = target.initialBalance,
                    iconKey = target.iconKey,
                    colorKey = target.colorKey,
                    createdAt = target.createdAt
                )
            )
            pendingDelete.value = null
            onDeleted()
        }
    }
}
