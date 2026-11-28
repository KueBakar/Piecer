package com.piecer.app.util

import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.widget.WidgetPrivacyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.util.Calendar

class EmptyDataCalculationsTest {

    private val fixedNowMillis = 1_767_225_600_000L // 2026-01-01 00:00:00 UTC

    @Test
    fun `home calculations handle completely empty data safely`() {
        val effective = DateUtils.effectiveTransactions(emptyList(), fixedNowMillis)
        val scheduled = DateUtils.scheduledTransactions(emptyList(), fixedNowMillis)
        val weekly = DateUtils.last7DaysExpenses(effective, fixedNowMillis)
        val period = FinancialCycleHelper.currentPeriod(fixedNowMillis, cycleStartDay = 1)
        val pieces = PieceCalculator.calculatePiecesOverview(
            pieces = emptyList(),
            pieceCategories = emptyList(),
            allocations = emptyList(),
            transactions = effective,
            cyclePeriod = period,
            nowMillis = fixedNowMillis
        )
        val criticalBudget = BudgetCalculator.findMostCritical(emptyList<BudgetProgress>()) { it }

        assertTrue(effective.isEmpty())
        assertTrue(scheduled.isEmpty())
        assertEquals(7, weekly.size)
        assertTrue(weekly.all { it.totalExpense == 0L })
        assertTrue(pieces.summaries.isEmpty())
        assertNull(criticalBudget)
    }

    @Test
    fun `insight engine handles empty and zero-amount data without division by zero`() {
        val emptyInsights = InsightEngine.generateInsights(
            transactions = emptyList(),
            categories = emptyList(),
            budgets = emptyList(),
            currentBalance = 0L,
            cycleStartDay = 1,
            nowMillis = fixedNowMillis,
            zoneId = ZoneId.of("UTC")
        )
        assertTrue(emptyInsights.isEmpty())

        val transferOnly = listOf(
            TransactionEntity(
                id = 1L,
                amount = 100_000L,
                type = TransactionType.TRANSFER,
                categoryId = null,
                walletId = 1L,
                toWalletId = 2L,
                date = fixedNowMillis,
                note = "",
                createdAt = fixedNowMillis
            )
        )
        val transferInsights = InsightEngine.generateInsights(
            transactions = transferOnly,
            categories = emptyList(),
            budgets = emptyList(),
            currentBalance = 0L,
            cycleStartDay = 1,
            nowMillis = fixedNowMillis,
            zoneId = ZoneId.of("UTC")
        )
        assertTrue(transferInsights.isEmpty())
    }

    @Test
    fun `wrapped calculator handles empty and zero-expense data safely`() {
        val emptyMonth = WrappedCalculator.calculate(
            transactions = emptyList(),
            categories = emptyList(),
            mode = WrappedPeriodMode.MONTH,
            year = 2026,
            monthZeroBased = Calendar.JANUARY,
            cycleStartDay = 1,
            nowMillis = fixedNowMillis,
            periodLabel = "Januari 2026"
        )
        assertFalse(emptyMonth.hasData)
        assertEquals(0L, emptyMonth.totalExpense)
        assertEquals(0L, emptyMonth.totalIncome)
        assertEquals(0, emptyMonth.savingsRatioPercent)

        val emptyYear = WrappedCalculator.calculate(
            transactions = emptyList(),
            categories = emptyList(),
            mode = WrappedPeriodMode.YEAR,
            year = 2026,
            monthZeroBased = Calendar.JANUARY,
            cycleStartDay = 1,
            nowMillis = fixedNowMillis,
            periodLabel = "2026"
        )
        assertFalse(emptyYear.hasData)

        val transferOnly = listOf(
            TransactionEntity(
                id = 1L,
                amount = 100_000L,
                type = TransactionType.TRANSFER,
                categoryId = null,
                walletId = 1L,
                toWalletId = 2L,
                date = fixedNowMillis,
                note = "Pindah",
                createdAt = fixedNowMillis
            )
        )
        val transferWrapped = WrappedCalculator.calculate(
            transactions = transferOnly,
            categories = emptyList(),
            mode = WrappedPeriodMode.MONTH,
            year = 2026,
            monthZeroBased = Calendar.JANUARY,
            cycleStartDay = 1,
            nowMillis = fixedNowMillis,
            periodLabel = "Januari 2026"
        )
        assertTrue(transferWrapped.hasData)
        assertEquals(0L, transferWrapped.totalExpense)
        assertEquals(0L, transferWrapped.totalIncome)
        assertEquals(0, transferWrapped.topCategoryPercent)
        assertEquals(0, transferWrapped.savingsRatioPercent)
    }

    @Test
    fun `widget formatting and boleh jajan handle zero and empty state safely`() {
        assertEquals(0L, calculateBolehJajanHariIni(0L, 0L, 0))
        assertEquals(0L, calculateBolehJajanHariIni(0L, 0L, 30))
        assertEquals("Rp0", WidgetPrivacyPolicy.formatAmount(0L, hideAmounts = false))
        assertEquals(WidgetPrivacyPolicy.MASKED_AMOUNT, WidgetPrivacyPolicy.formatAmount(0L, hideAmounts = true))
        assertEquals(0L, calculateWalletBalance(1L, 0L, emptyList(), fixedNowMillis))
    }
}
