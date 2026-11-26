package com.piecer.app.util

import com.piecer.app.data.local.entity.BudgetEntity
import com.piecer.app.data.local.entity.BudgetPeriodType
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class InsightEngineTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val categories = listOf(
        CategoryEntity(1L, "Makan", CategoryType.EXPENSE, "restaurant", "coral", true),
        CategoryEntity(2L, "Transport", CategoryType.EXPENSE, "directions_car", "coral", true)
    )

    private fun millis(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `empty transactions returns empty insights`() {
        val result = InsightEngine.generateInsights(
            transactions = emptyList(),
            categories = categories,
            budgets = emptyList(),
            currentBalance = 1_000_000L,
            cycleStartDay = 1,
            nowMillis = millis(2026, 11, 20),
            zoneId = zone
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `less than 7 days of data hides complex insights and only keeps safe ones`() {
        val now = millis(2026, 11, 20)
        val txs = listOf(
            TransactionEntity(1L, 100_000L, TransactionType.EXPENSE, 1L, 1L, null, millis(2026, 11, 18), "Makan", now),
            TransactionEntity(2L, 100_000L, TransactionType.EXPENSE, 1L, 1L, null, millis(2026, 11, 19), "Makan", now),
            TransactionEntity(3L, 100_000L, TransactionType.EXPENSE, 1L, 1L, null, millis(2026, 11, 20), "Makan", now)
        )
        val result = InsightEngine.generateInsights(
            transactions = txs,
            categories = categories,
            budgets = emptyList(),
            currentBalance = 1_000_000L,
            cycleStartDay = 1,
            nowMillis = now,
            zoneId = zone
        )
        assertTrue(result.all { it.type == InsightType.STREAK || it.type == InsightType.BUDGET_ALERT })
        assertEquals(1, result.size)
        assertEquals(InsightType.STREAK, result[0].type)
    }

    @Test
    fun `generates max 3 insights including budget alert, category change, top day, prediction`() {
        val now = millis(2026, 11, 20)
        val txs = listOf(
            // Siklus lalu (Oktober)
            TransactionEntity(1L, 200_000L, TransactionType.EXPENSE, 1L, 1L, null, millis(2026, 10, 10), "Makan", now),
            // Siklus ini (November) span >= 7 hari
            TransactionEntity(2L, 300_000L, TransactionType.EXPENSE, 1L, 1L, null, millis(2026, 11, 5), "Makan", now),
            TransactionEntity(3L, 500_000L, TransactionType.EXPENSE, 1L, 1L, null, millis(2026, 11, 14), "Makan", now), // Sabtu
            TransactionEntity(4L, 50_000L, TransactionType.EXPENSE, 2L, 1L, null, millis(2026, 11, 19), "Transport", now),
            TransactionEntity(5L, 50_000L, TransactionType.EXPENSE, 2L, 1L, null, millis(2026, 11, 20), "Transport", now)
        )
        val budgets = listOf(
            BudgetEntity(1L, 1L, 900_000L, BudgetPeriodType.MONTHLY, now) // 800k / 900k = 88%
        )

        val result = InsightEngine.generateInsights(
            transactions = txs,
            categories = categories,
            budgets = budgets,
            currentBalance = 2_000_000L,
            cycleStartDay = 1,
            nowMillis = now,
            zoneId = zone
        )

        assertEquals(3, result.size)
        assertTrue(result.any { it.type == InsightType.BUDGET_ALERT })
        assertTrue(result.any { it.type == InsightType.CATEGORY_CHANGE })
    }
}
