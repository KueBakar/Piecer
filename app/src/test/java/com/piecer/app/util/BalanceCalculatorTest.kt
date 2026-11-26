package com.piecer.app.util

import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceCalculatorTest {

    @Test
    fun `calculateWalletBalance returns initialBalance when no transactions`() {
        val balance = calculateWalletBalance(
            walletId = 1L,
            initialBalance = 500_000L,
            transactions = emptyList()
        )
        assertEquals(500_000L, balance)
    }

    @Test
    fun `calculateWalletBalance adds income and subtracts expense`() {
        val transactions = listOf(
            TransactionEntity(
                id = 1L,
                amount = 1_000_000L,
                type = TransactionType.INCOME,
                categoryId = 9L,
                walletId = 1L,
                date = 1_700_000_000_000L,
                note = "Gaji",
                createdAt = 1_700_000_000_000L
            ),
            TransactionEntity(
                id = 2L,
                amount = 250_000L,
                type = TransactionType.EXPENSE,
                categoryId = 1L,
                walletId = 1L,
                date = 1_700_000_100_000L,
                note = "Makan",
                createdAt = 1_700_000_100_000L
            ),
            // Transaksi dompet lain tidak memengaruhi dompet 1
            TransactionEntity(
                id = 3L,
                amount = 500_000L,
                type = TransactionType.EXPENSE,
                categoryId = 1L,
                walletId = 2L,
                date = 1_700_000_200_000L,
                note = "Dompet lain",
                createdAt = 1_700_000_200_000L
            )
        )

        val balance = calculateWalletBalance(
            walletId = 1L,
            initialBalance = 100_000L,
            transactions = transactions
        )
        assertEquals(850_000L, balance)
    }

    @Test
    fun `calculateWalletBalance handles outgoing and incoming transfers`() {
        val transactions = listOf(
            // Transfer keluar dari dompet 1 ke dompet 2 sebesar 300.000
            TransactionEntity(
                id = 1L,
                amount = 300_000L,
                type = TransactionType.TRANSFER,
                categoryId = null,
                walletId = 1L,
                toWalletId = 2L,
                date = 1_700_000_000_000L,
                note = "Tarik tunai",
                createdAt = 1_700_000_000_000L
            ),
            // Transfer masuk ke dompet 1 dari dompet 3 sebesar 150.000
            TransactionEntity(
                id = 2L,
                amount = 150_000L,
                type = TransactionType.TRANSFER,
                categoryId = null,
                walletId = 3L,
                toWalletId = 1L,
                date = 1_700_000_100_000L,
                note = "Top up",
                createdAt = 1_700_000_100_000L
            )
        )

        val wallet1Balance = calculateWalletBalance(
            walletId = 1L,
            initialBalance = 500_000L,
            transactions = transactions
        )
        val wallet2Balance = calculateWalletBalance(
            walletId = 2L,
            initialBalance = 0L,
            transactions = transactions
        )

        assertEquals(350_000L, wallet1Balance)
        assertEquals(300_000L, wallet2Balance)
    }

    @Test
    fun `calculateBolehJajanHariIni divides remaining monthly budget by remaining days`() {
        val daily = calculateBolehJajanHariIni(
            monthlyIncome = 3_000_000L,
            monthlyExpense = 1_500_000L,
            remainingDaysInMonth = 10
        )
        assertEquals(150_000L, daily)
    }

    @Test
    fun `calculateBolehJajanHariIni returns zero when expense exceeds income`() {
        val daily = calculateBolehJajanHariIni(
            monthlyIncome = 1_000_000L,
            monthlyExpense = 1_250_000L,
            remainingDaysInMonth = 5
        )
        assertEquals(0L, daily)
    }

    @Test
    fun `calculateBolehJajanHariIni guards zero or negative remaining days`() {
        val daily = calculateBolehJajanHariIni(
            monthlyIncome = 500_000L,
            monthlyExpense = 200_000L,
            remainingDaysInMonth = 0
        )
        assertEquals(300_000L, daily)
    }
}
