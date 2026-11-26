package com.piecer.app.util

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StatisticsCalculatorTest {

    private val zoneId: ZoneId = ZoneId.of("Asia/Jakarta")

    private fun millisOn(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atTime(12, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun `computeSummaryComparison calculates income expense net and percentage change`() {
        val now = millisOn(2026, 11, 15)
        val currentRange = StatisticsCalculator.computePeriodRange(StatsPeriodType.MONTH, 0, now, zoneId)
        val previousRange = StatisticsCalculator.computePeriodRange(StatsPeriodType.MONTH, -1, now, zoneId)

        val transactions = listOf(
            // Current month (Nov 2026)
            TransactionEntity(
                id = 1, amount = 5_000_000L, type = TransactionType.INCOME,
                categoryId = 10, walletId = 1, date = millisOn(2026, 11, 5), createdAt = now
            ),
            TransactionEntity(
                id = 2, amount = 1_500_000L, type = TransactionType.EXPENSE,
                categoryId = 1, walletId = 1, date = millisOn(2026, 11, 10), createdAt = now
            ),
            // Transfer should not count as income or expense
            TransactionEntity(
                id = 3, amount = 500_000L, type = TransactionType.TRANSFER,
                categoryId = null, walletId = 1, toWalletId = 2, date = millisOn(2026, 11, 12), createdAt = now
            ),
            // Previous month (Oct 2026)
            TransactionEntity(
                id = 4, amount = 1_000_000L, type = TransactionType.EXPENSE,
                categoryId = 1, walletId = 1, date = millisOn(2026, 10, 20), createdAt = now
            )
        )

        val summary = StatisticsCalculator.computeSummaryComparison(
            transactions = transactions,
            currentRange = currentRange,
            previousRange = previousRange
        )

        assertEquals(5_000_000L, summary.totalIncome)
        assertEquals(1_500_000L, summary.totalExpense)
        assertEquals(3_500_000L, summary.netBalance)
        assertEquals(1_000_000L, summary.previousExpense)
        assertEquals(50, summary.expenseChangePercent)
    }

    @Test
    fun `computeSummaryComparison returns null percentage when previous expense is zero`() {
        val now = millisOn(2026, 11, 15)
        val currentRange = StatisticsCalculator.computePeriodRange(StatsPeriodType.MONTH, 0, now, zoneId)
        val previousRange = StatisticsCalculator.computePeriodRange(StatsPeriodType.MONTH, -1, now, zoneId)

        val transactions = listOf(
            TransactionEntity(
                id = 1, amount = 200_000L, type = TransactionType.EXPENSE,
                categoryId = 1, walletId = 1, date = millisOn(2026, 11, 10), createdAt = now
            )
        )

        val summary = StatisticsCalculator.computeSummaryComparison(
            transactions = transactions,
            currentRange = currentRange,
            previousRange = previousRange
        )

        assertNull(summary.expenseChangePercent)
    }

    @Test
    fun `computeCategoryBreakdown aggregates expenses sorted descending with percentages`() {
        val now = millisOn(2026, 11, 15)
        val range = StatisticsCalculator.computePeriodRange(StatsPeriodType.MONTH, 0, now, zoneId)

        val categories = mapOf(
            1L to CategoryEntity(1L, "Makan", CategoryType.EXPENSE, "restaurant", "coral", true),
            2L to CategoryEntity(2L, "Transport", CategoryType.EXPENSE, "directions_car", "coral", true)
        )

        val transactions = listOf(
            TransactionEntity(
                id = 1, amount = 750_000L, type = TransactionType.EXPENSE,
                categoryId = 1L, walletId = 1, date = millisOn(2026, 11, 2), createdAt = now
            ),
            TransactionEntity(
                id = 2, amount = 250_000L, type = TransactionType.EXPENSE,
                categoryId = 2L, walletId = 1, date = millisOn(2026, 11, 3), createdAt = now
            )
        )

        val slices = StatisticsCalculator.computeCategoryBreakdown(
            transactions = transactions,
            categories = categories,
            startMillis = range.startMillis,
            endMillis = range.endMillis
        )

        assertEquals(2, slices.size)
        assertEquals("Makan", slices[0].categoryName)
        assertEquals(750_000L, slices[0].totalAmount)
        assertEquals(75, slices[0].percentageInt)
        assertEquals("Transport", slices[1].categoryName)
        assertEquals(25, slices[1].percentageInt)
    }

    @Test
    fun `computeTrendPoints returns 12 months for YEAR and 7 days for WEEK`() {
        val now = millisOn(2026, 11, 15)
        val weekRange = StatisticsCalculator.computePeriodRange(StatsPeriodType.WEEK, 0, now, zoneId)
        val yearRange = StatisticsCalculator.computePeriodRange(StatsPeriodType.YEAR, 0, now, zoneId)

        val weekPoints = StatisticsCalculator.computeTrendPoints(
            transactions = emptyList(),
            periodType = StatsPeriodType.WEEK,
            startMillis = weekRange.startMillis,
            endMillis = weekRange.endMillis,
            zoneId = zoneId
        )
        val yearPoints = StatisticsCalculator.computeTrendPoints(
            transactions = emptyList(),
            periodType = StatsPeriodType.YEAR,
            startMillis = yearRange.startMillis,
            endMillis = yearRange.endMillis,
            zoneId = zoneId
        )

        assertEquals(7, weekPoints.size)
        assertEquals(12, yearPoints.size)
    }
}
