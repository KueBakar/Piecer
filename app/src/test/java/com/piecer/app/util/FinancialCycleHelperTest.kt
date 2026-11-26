package com.piecer.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FinancialCycleHelperTest {

    private val zone = ZoneId.of("Asia/Jakarta")

    @Test
    fun `cycleStartDay 1 behaves like calendar month`() {
        val now = LocalDate.of(2026, 5, 15).atStartOfDay(zone).toInstant().toEpochMilli()
        val period = FinancialCycleHelper.currentPeriod(now, cycleStartDay = 1, zoneId = zone)

        assertEquals(LocalDate.of(2026, 5, 1), period.startDate)
        assertEquals(LocalDate.of(2026, 5, 31), period.endDate)
        assertEquals(17, period.remainingDays)
    }

    @Test
    fun `cycleStartDay 25 before 25th starts in previous month`() {
        val now = LocalDate.of(2026, 3, 10).atStartOfDay(zone).toInstant().toEpochMilli()
        val period = FinancialCycleHelper.currentPeriod(now, cycleStartDay = 25, zoneId = zone)

        assertEquals(LocalDate.of(2026, 2, 25), period.startDate)
        assertEquals(LocalDate.of(2026, 3, 24), period.endDate)
        assertEquals(15, period.remainingDays)
    }

    @Test
    fun `cycleStartDay 25 on 25th starts today`() {
        val now = LocalDate.of(2026, 3, 25).atStartOfDay(zone).toInstant().toEpochMilli()
        val period = FinancialCycleHelper.currentPeriod(now, cycleStartDay = 25, zoneId = zone)

        assertEquals(LocalDate.of(2026, 3, 25), period.startDate)
        assertEquals(LocalDate.of(2026, 4, 24), period.endDate)
        assertEquals(31, period.remainingDays)
    }

    @Test
    fun `periodForOffset -1 returns previous cycle`() {
        val now = LocalDate.of(2026, 3, 10).atStartOfDay(zone).toInstant().toEpochMilli()
        val prev = FinancialCycleHelper.periodForOffset(now, cycleStartDay = 25, offset = -1, zoneId = zone)

        assertEquals(LocalDate.of(2026, 1, 25), prev.startDate)
        assertEquals(LocalDate.of(2026, 2, 24), prev.endDate)
    }

    @Test
    fun `cycleStartDay is clamped to 1 to 28`() {
        val now = LocalDate.of(2026, 2, 28).atStartOfDay(zone).toInstant().toEpochMilli()
        val period = FinancialCycleHelper.currentPeriod(now, cycleStartDay = 31, zoneId = zone)

        assertEquals(LocalDate.of(2026, 2, 28), period.startDate)
        assertEquals(LocalDate.of(2026, 3, 27), period.endDate)
    }
}
