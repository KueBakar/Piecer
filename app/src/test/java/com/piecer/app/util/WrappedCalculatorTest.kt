package com.piecer.app.util

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class WrappedCalculatorTest {

    @Test
    fun calculate_excludesScheduledTransactions_andComputesMetrics() {
        val now = calendarMillis(2026, Calendar.MAY, 20, 12)
        val categories = listOf(
            CategoryEntity(id = 1L, name = "Makan", type = CategoryType.EXPENSE, iconKey = "food", colorKey = "mint", isDefault = true),
            CategoryEntity(id = 2L, name = "Transport", type = CategoryType.EXPENSE, iconKey = "car", colorKey = "mint", isDefault = true)
        )
        val txs = listOf(
            TransactionEntity(id = 1L, type = TransactionType.INCOME, amount = 1_000_000L, categoryId = null, walletId = 1L, date = calendarMillis(2026, Calendar.MAY, 2, 10), createdAt = 0L),
            TransactionEntity(id = 2L, type = TransactionType.EXPENSE, amount = 200_000L, categoryId = 1L, walletId = 1L, date = calendarMillis(2026, Calendar.MAY, 3, 10), createdAt = 0L),
            TransactionEntity(id = 3L, type = TransactionType.EXPENSE, amount = 100_000L, categoryId = 2L, walletId = 1L, date = calendarMillis(2026, Calendar.MAY, 4, 10), createdAt = 0L),
            // Scheduled future transaction should be excluded
            TransactionEntity(id = 4L, type = TransactionType.EXPENSE, amount = 9_000_000L, categoryId = 1L, walletId = 1L, date = calendarMillis(2026, Calendar.MAY, 25, 10), createdAt = 0L)
        )

        val summary = WrappedCalculator.calculate(
            transactions = txs,
            categories = categories,
            mode = WrappedPeriodMode.MONTH,
            year = 2026,
            monthZeroBased = Calendar.MAY,
            cycleStartDay = 1,
            nowMillis = now,
            periodLabel = "Mei 2026"
        )

        assertTrue(summary.hasData)
        assertEquals(300_000L, summary.totalExpense)
        assertEquals(1_000_000L, summary.totalIncome)
        assertEquals("Makan", summary.topCategoryName)
        assertEquals(200_000L, summary.topCategoryAmount)
        assertEquals(66, summary.topCategoryPercent)
        assertEquals(70, summary.savingsRatioPercent)
        assertEquals(3, summary.recordingStreakDays)
    }

    @Test
    fun calculate_emptyPeriod_returnsHasDataFalse() {
        val summary = WrappedCalculator.calculate(
            transactions = emptyList(),
            categories = emptyList(),
            mode = WrappedPeriodMode.MONTH,
            year = 2026,
            monthZeroBased = Calendar.MAY,
            cycleStartDay = 1,
            nowMillis = calendarMillis(2026, Calendar.MAY, 20, 12),
            periodLabel = "Mei 2026"
        )
        assertFalse(summary.hasData)
        assertEquals(0L, summary.totalExpense)
    }

    @Test
    fun buildShareText_respectsHideAmounts() {
        val summary = WrappedSummary(
            periodLabel = "Mei 2026",
            mode = WrappedPeriodMode.MONTH,
            hasData = true,
            totalExpense = 500_000L,
            totalIncome = 1_000_000L,
            topCategoryName = "Makan",
            topCategoryAmount = 300_000L,
            topCategoryPercent = 60,
            mostExpensiveDayMillis = null,
            mostExpensiveDayAmount = 0L,
            previousPeriodExpense = 400_000L,
            expenseChangePercent = 25,
            savingsRatioPercent = 50,
            recordingStreakDays = 5
        )

        val publicText = WrappedCalculator.buildShareText(summary, hideAmounts = false)
        assertTrue(publicText.contains("Rp500.000"))
        assertTrue(publicText.contains("Streak mencatat: 5 hari"))

        val hiddenText = WrappedCalculator.buildShareText(summary, hideAmounts = true)
        assertFalse(hiddenText.contains("Rp"))
        assertTrue(hiddenText.contains("50%"))
        assertTrue(hiddenText.contains("60%"))
    }

    private fun calendarMillis(year: Int, month: Int, day: Int, hour: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
