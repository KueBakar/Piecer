package com.piecer.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetCalculatorTest {

    @Test
    fun `calculate returns SAFE and no threshold when spent is below 70 percent`() {
        val result = BudgetCalculator.calculate(spent = 500_000L, limit = 1_000_000L)

        assertEquals(50, result.percentageInt)
        assertEquals(500_000L, result.remaining)
        assertEquals(BudgetStatus.SAFE, result.status)
        assertNull(result.threshold)
    }

    @Test
    fun `calculate returns WARNING and no threshold between 70 and 79 percent`() {
        val result = BudgetCalculator.calculate(spent = 750_000L, limit = 1_000_000L)

        assertEquals(75, result.percentageInt)
        assertEquals(250_000L, result.remaining)
        assertEquals(BudgetStatus.WARNING, result.status)
        assertNull(result.threshold)
    }

    @Test
    fun `calculate returns WARNING and REACHED_80 at 80 percent`() {
        val result = BudgetCalculator.calculate(spent = 800_000L, limit = 1_000_000L)

        assertEquals(80, result.percentageInt)
        assertEquals(200_000L, result.remaining)
        assertEquals(BudgetStatus.WARNING, result.status)
        assertEquals(BudgetThreshold.REACHED_80, result.threshold)
    }

    @Test
    fun `calculate returns EXCEEDED and REACHED_100 at 100 percent and over`() {
        val exact = BudgetCalculator.calculate(spent = 1_000_000L, limit = 1_000_000L)
        assertEquals(100, exact.percentageInt)
        assertEquals(0L, exact.remaining)
        assertEquals(BudgetStatus.EXCEEDED, exact.status)
        assertEquals(BudgetThreshold.REACHED_100, exact.threshold)

        val over = BudgetCalculator.calculate(spent = 1_250_000L, limit = 1_000_000L)
        assertEquals(125, over.percentageInt)
        assertEquals(-250_000L, over.remaining)
        assertEquals(BudgetStatus.EXCEEDED, over.status)
        assertEquals(BudgetThreshold.REACHED_100, over.threshold)
    }

    @Test
    fun `findMostCritical selects highest percentage budget`() {
        val low = BudgetCalculator.calculate(spent = 300_000L, limit = 1_000_000L)
        val high = BudgetCalculator.calculate(spent = 900_000L, limit = 1_000_000L)
        val mid = BudgetCalculator.calculate(spent = 600_000L, limit = 1_000_000L)

        val critical = BudgetCalculator.findMostCritical(listOf(low, high, mid)) { it }
        assertEquals(high, critical)
    }
}
