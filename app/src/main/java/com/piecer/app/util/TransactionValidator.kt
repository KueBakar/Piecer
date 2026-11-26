package com.piecer.app.util

import com.piecer.app.data.local.entity.TransactionType

enum class TransactionValidationError {
    AMOUNT_REQUIRED,
    CATEGORY_REQUIRED,
    WALLET_REQUIRED,
    TRANSFER_DEST_REQUIRED,
    TRANSFER_SAME_WALLET
}

fun validateTransactionInput(
    type: TransactionType,
    amount: Long,
    categoryId: Long?,
    walletId: Long?,
    toWalletId: Long?
): TransactionValidationError? {
    if (amount <= 0L) {
        return TransactionValidationError.AMOUNT_REQUIRED
    }
    if (walletId == null) {
        return TransactionValidationError.WALLET_REQUIRED
    }
    return when (type) {
        TransactionType.EXPENSE, TransactionType.INCOME -> {
            if (categoryId == null) TransactionValidationError.CATEGORY_REQUIRED else null
        }
        TransactionType.TRANSFER -> {
            when {
                toWalletId == null -> TransactionValidationError.TRANSFER_DEST_REQUIRED
                toWalletId == walletId -> TransactionValidationError.TRANSFER_SAME_WALLET
                else -> null
            }
        }
    }
}
