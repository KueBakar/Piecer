package com.piecer.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GoalCalculatorTest {

    private val zone = ZoneId.of("Asia/Jakarta")

    @Test
    fun `calculate computes remaining, ratio, and achieved state`() {
        val result = GoalCalculator.calculate(
            savedAmount = 3_000_000L,
            targetAmount = 10_000_000L,
            deadlineMillis = null
        )
        assertEquals(7_000_000L, result.remainingAmount)
        assertEquals(30, result.percentageInt)
        assertFalse(result.isAchieved)
        assertNull(result.estimatedMonthlySaving)
    }

    @Test
    fun `calculate marks achieved when saved reaches or exceeds target`() {
        val result = GoalCalculator.calculate(
            savedAmount = 12_000_000L,
            targetAmount = 10_000_000L,
            deadlineMillis = null
        )
        assertEquals(0L, result.remainingAmount)
        assertTrue(result.isAchieved)
        assertNull(result.estimatedMonthlySaving)
    }

    @Test
    fun `estimateMonthlySaving rounds up using integer division`() {
        val now = LocalDate.of(2026, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val deadline = LocalDate.of(2026, 4, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        // 90 days -> 3 months, 1_000_000 / 3 -> 333_334
        val monthly = GoalCalculator.estimateMonthlySaving(
            remainingAmount = 1_000_000L,
            deadlineMillis = deadline,
            nowMillis = now,
            zoneId = zone
        )
        assertEquals(333_334L, monthly)
    }
}
