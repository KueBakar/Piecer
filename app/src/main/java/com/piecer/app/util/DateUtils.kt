package com.piecer.app.util

import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface DayHeaderLabel {
    data object Today : DayHeaderLabel
    data object Yesterday : DayHeaderLabel
    data class Formatted(val text: String) : DayHeaderLabel
}

data class DailyExpensePoint(
    val dayStartMillis: Long,
    val dayShortLabel: String,
    val totalExpense: Long,
    val isToday: Boolean
)

object DateUtils {
    private val indoLocale = Locale("in", "ID")
    private val fullDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", indoLocale)
    private val shortDayFormatter = DateTimeFormatter.ofPattern("EE", indoLocale)

    fun startOfDayMillis(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val localDate = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun endOfTodayMillis(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        return today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
    }

    fun isScheduledTransaction(
        txDateMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean = txDateMillis > endOfTodayMillis(nowMillis, zoneId)

    fun effectiveTransactions(
        transactions: List<TransactionEntity>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<TransactionEntity> {
        val cutoff = endOfTodayMillis(nowMillis, zoneId)
        return transactions.filter { it.date <= cutoff }
    }

    fun scheduledTransactions(
        transactions: List<TransactionEntity>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<TransactionEntity> {
        val cutoff = endOfTodayMillis(nowMillis, zoneId)
        return transactions.filter { it.date > cutoff }
    }

    fun startOfMonthMillis(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val localDate = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        return localDate.withDayOfMonth(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun endOfMonthMillis(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val localDate = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        val nextMonthStart = localDate.withDayOfMonth(1).plusMonths(1)
            .atStartOfDay(zoneId).toInstant().toEpochMilli()
        return nextMonthStart - 1L
    }

    fun remainingDaysInMonth(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int {
        val localDate = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        return (localDate.lengthOfMonth() - localDate.dayOfMonth + 1).coerceAtLeast(1)
    }

    fun formatDate(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val localDate = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        return localDate.format(fullDateFormatter)
    }

    fun dayHeaderLabel(
        dayStartMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DayHeaderLabel {
        val targetDate = Instant.ofEpochMilli(dayStartMillis).atZone(zoneId).toLocalDate()
        val todayDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        return when (targetDate) {
            todayDate -> DayHeaderLabel.Today
            todayDate.minusDays(1) -> DayHeaderLabel.Yesterday
            else -> DayHeaderLabel.Formatted(targetDate.format(fullDateFormatter))
        }
    }

    fun last7DaysExpenses(
        transactions: List<TransactionEntity>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<DailyExpensePoint> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val totalsByDate = HashMap<LocalDate, Long>(8)

        for (tx in transactions) {
            if (tx.type == TransactionType.EXPENSE) {
                val txDate = Instant.ofEpochMilli(tx.date).atZone(zoneId).toLocalDate()
                if (!txDate.isBefore(today.minusDays(6)) && !txDate.isAfter(today)) {
                    totalsByDate[txDate] = (totalsByDate[txDate] ?: 0L) + tx.amount
                }
            }
        }

        return (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val dayStart = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val label = date.format(shortDayFormatter).replace(".", "").take(3)
            DailyExpensePoint(
                dayStartMillis = dayStart,
                dayShortLabel = label,
                totalExpense = totalsByDate[date] ?: 0L,
                isToday = offset == 0
            )
        }
    }
}
