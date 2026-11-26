package com.piecer.app.util

import com.piecer.app.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionValidatorTest {

    @Test
    fun expense_withZeroAmount_returnsAmountRequired() {
        val error = validateTransactionInput(
            type = TransactionType.EXPENSE,
            amount = 0L,
            categoryId = 1L,
            walletId = 1L,
            toWalletId = null
        )
        assertEquals(TransactionValidationError.AMOUNT_REQUIRED, error)
    }

    @Test
    fun expense_withoutCategory_returnsCategoryRequired() {
        val error = validateTransactionInput(
            type = TransactionType.EXPENSE,
            amount = 25_000L,
            categoryId = null,
            walletId = 1L,
            toWalletId = null
        )
        assertEquals(TransactionValidationError.CATEGORY_REQUIRED, error)
    }

    @Test
    fun income_withValidFields_returnsNull() {
        val error = validateTransactionInput(
            type = TransactionType.INCOME,
            amount = 500_000L,
            categoryId = 2L,
            walletId = 1L,
            toWalletId = null
        )
        assertNull(error)
    }

    @Test
    fun transfer_withoutCategory_isValidWhenWalletsDistinct() {
        val error = validateTransactionInput(
            type = TransactionType.TRANSFER,
            amount = 100_000L,
            categoryId = null,
            walletId = 1L,
            toWalletId = 2L
        )
        assertNull(error)
    }

    @Test
    fun transfer_withSameWallet_returnsSameWalletError() {
        val error = validateTransactionInput(
            type = TransactionType.TRANSFER,
            amount = 100_000L,
            categoryId = null,
            walletId = 1L,
            toWalletId = 1L
        )
        assertEquals(TransactionValidationError.TRANSFER_SAME_WALLET, error)
    }

    @Test
    fun transfer_withoutDestinationWallet_returnsDestRequired() {
        val error = validateTransactionInput(
            type = TransactionType.TRANSFER,
            amount = 100_000L,
            categoryId = null,
            walletId = 1L,
            toWalletId = null
        )
        assertEquals(TransactionValidationError.TRANSFER_DEST_REQUIRED, error)
    }
}
