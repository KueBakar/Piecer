package com.piecer.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class CyclePeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startMillis: Long,
    val endMillis: Long,
    val remainingDays: Int
)

object FinancialCycleHelper {
    private val indoLocale = Locale("in", "ID")
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", indoLocale)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMM yyyy", indoLocale)

    fun currentPeriod(
        nowMillis: Long = System.currentTimeMillis(),
        cycleStartDay: Int = 1,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): CyclePeriod = periodForOffset(
        nowMillis = nowMillis,
        cycleStartDay = cycleStartDay,
        offset = 0,
        zoneId = zoneId
    )

    fun periodForOffset(
        nowMillis: Long = System.currentTimeMillis(),
        cycleStartDay: Int = 1,
        offset: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): CyclePeriod {
        val safeStartDay = cycleStartDay.coerceIn(1, 28)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()

        val currentStart = if (today.dayOfMonth >= safeStartDay) {
            today.withDayOfMonth(safeStartDay)
        } else {
            today.minusMonths(1).withDayOfMonth(safeStartDay)
        }

        val periodStart = currentStart.plusMonths(offset.toLong())
        val nextPeriodStart = periodStart.plusMonths(1)
        val periodEnd = nextPeriodStart.minusDays(1)

        val startMillis = periodStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = nextPeriodStart.atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
        val remainingDays = ChronoUnit.DAYS.between(today, nextPeriodStart).toInt().coerceAtLeast(1)

        return CyclePeriod(
            startDate = periodStart,
            endDate = periodEnd,
            startMillis = startMillis,
            endMillis = endMillis,
            remainingDays = remainingDays
        )
    }

    fun formatCycleLabel(period: CyclePeriod, cycleStartDay: Int): String {
        return if (cycleStartDay.coerceIn(1, 28) == 1) {
            period.startDate.format(monthYearFormatter)
        } else {
            "${period.startDate.format(dayMonthFormatter)} - ${period.endDate.format(dayMonthFormatter)}"
        }
    }
}
