package com.piecer.app.util

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ScheduledTransactionTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val today = LocalDate.of(2026, 11, 27)
    private val todayNoon = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val tomorrowMorning = today.plusDays(1).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
    private val afterMidnight = today.plusDays(1).atTime(0, 5).atZone(zone).toInstant().toEpochMilli()

    private val todayTx = TransactionEntity(
        id = 1L,
        amount = 100_000L,
        type = TransactionType.EXPENSE,
        categoryId = 1L,
        walletId = 1L,
        toWalletId = null,
        date = todayNoon,
        note = "Hari ini",
        createdAt = todayNoon
    )
    private val tomorrowTx = TransactionEntity(
        id = 2L,
        amount = 250_000L,
        type = TransactionType.EXPENSE,
        categoryId = 1L,
        walletId = 1L,
        toWalletId = null,
        date = tomorrowMorning,
        note = "Besok",
        createdAt = todayNoon
    )

    @Test
    fun `today transaction is included and tomorrow transaction is excluded before midnight`() {
        assertFalse(DateUtils.isScheduledTransaction(todayNoon, todayNoon, zone))
        assertTrue(DateUtils.isScheduledTransaction(tomorrowMorning, todayNoon, zone))

        val balance = calculateWalletBalance(
            walletId = 1L,
            initialBalance = 1_000_000L,
            transactions = listOf(todayTx, tomorrowTx),
            nowMillis = todayNoon,
            zoneId = zone
        )
        assertEquals(900_000L, balance)

        val range = StatisticsCalculator.computePeriodRange(StatsPeriodType.MONTH, 0, todayNoon, zone, 1)
        val categories = mapOf(1L to CategoryEntity(1L, "Makan", CategoryType.EXPENSE, "restaurant", "coral", true))
        val slices = StatisticsCalculator.computeCategoryBreakdown(
            transactions = listOf(todayTx, tomorrowTx),
            categories = categories,
            startMillis = range.startMillis,
            endMillis = range.endMillis,
            nowMillis = todayNoon,
            zoneId = zone
        )
        assertEquals(1, slices.size)
        assertEquals(100_000L, slices[0].totalAmount)
    }

    @Test
    fun `after midnight tomorrow transaction becomes active and updates balance`() {
        assertFalse(DateUtils.isScheduledTransaction(tomorrowMorning, afterMidnight, zone))

        val balanceAfterMidnight = calculateWalletBalance(
            walletId = 1L,
            initialBalance = 1_000_000L,
            transactions = listOf(todayTx, tomorrowTx),
            nowMillis = afterMidnight,
            zoneId = zone
        )
        assertEquals(650_000L, balanceAfterMidnight)
    }
}
