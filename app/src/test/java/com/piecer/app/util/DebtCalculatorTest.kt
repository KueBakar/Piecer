package com.piecer.app.util

import com.piecer.app.data.local.entity.DebtDirection
import com.piecer.app.data.local.entity.DebtEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DebtCalculatorTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val todayMillis = LocalDate.of(2026, 5, 10).atStartOfDay(zone).toInstant().toEpochMilli()
    private val yesterdayMillis = LocalDate.of(2026, 5, 9).atStartOfDay(zone).toInstant().toEpochMilli()
    private val tomorrowMillis = LocalDate.of(2026, 5, 11).atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun `remainingAmount and totalRemainingByDirection calculate unpaid balances`() {
        val debts = listOf(
            DebtEntity(1L, "Andi", 500_000L, 200_000L, DebtDirection.OWE_ME, createdAt = 1L),
            DebtEntity(2L, "Budi", 300_000L, 300_000L, DebtDirection.OWE_ME, createdAt = 1L),
            DebtEntity(3L, "Citra", 1_000_000L, 250_000L, DebtDirection.I_OWE, createdAt = 1L)
        )
        assertEquals(300_000L, DebtCalculator.remainingAmount(debts[0]))
        assertEquals(0L, DebtCalculator.remainingAmount(debts[1]))
        assertTrue(DebtCalculator.isPaidOff(debts[1]))
        assertEquals(300_000L, DebtCalculator.totalRemainingByDirection(debts, DebtDirection.OWE_ME))
        assertEquals(750_000L, DebtCalculator.totalRemainingByDirection(debts, DebtDirection.I_OWE))
    }

    @Test
    fun `isOverdue detects past due date only when unpaid`() {
        val overdueUnpaid = DebtEntity(1L, "A", 100_000L, 0L, DebtDirection.OWE_ME, dueDate = yesterdayMillis, createdAt = 1L)
        val overduePaid = DebtEntity(2L, "B", 100_000L, 100_000L, DebtDirection.OWE_ME, dueDate = yesterdayMillis, createdAt = 1L)
        val futureUnpaid = DebtEntity(3L, "C", 100_000L, 0L, DebtDirection.OWE_ME, dueDate = tomorrowMillis, createdAt = 1L)

        assertTrue(DebtCalculator.isOverdue(overdueUnpaid, todayMillis, zone))
        assertFalse(DebtCalculator.isOverdue(overduePaid, todayMillis, zone))
        assertFalse(DebtCalculator.isOverdue(futureUnpaid, todayMillis, zone))
    }
}
