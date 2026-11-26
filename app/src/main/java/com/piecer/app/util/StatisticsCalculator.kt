package com.piecer.app.util

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class StatsPeriodType {
    WEEK,
    MONTH,
    YEAR
}

data class PeriodRange(
    val startMillis: Long,
    val endMillis: Long,
    val label: String
)

data class PeriodSummaryComparison(
    val totalIncome: Long,
    val totalExpense: Long,
    val netBalance: Long,
    val previousExpense: Long,
    val expenseChangePercent: Int?
)

data class CategoryExpenseSlice(
    val categoryId: Long,
    val categoryName: String,
    val totalAmount: Long,
    val percentageInt: Int,
    val ratio: Float
)

data class TrendPoint(
    val label: String,
    val totalExpense: Long
)

object StatisticsCalculator {

    private val indoLocale = Locale("in", "ID")
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", indoLocale)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMM yyyy", indoLocale)
    private val shortDayFormatter = DateTimeFormatter.ofPattern("EE", indoLocale)
    private val shortMonthFormatter = DateTimeFormatter.ofPattern("MMM", indoLocale)

    fun computePeriodRange(
        periodType: StatsPeriodType,
        offset: Int,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): PeriodRange {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        return when (periodType) {
            StatsPeriodType.WEEK -> {
                val startOfWeek = today
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .plusWeeks(offset.toLong())
                val endOfWeek = startOfWeek.plusDays(6)
                val startMillis = startOfWeek.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endMillis = endOfWeek.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
                val label = "${startOfWeek.format(dayMonthFormatter)} - ${endOfWeek.format(dayMonthFormatter)}"
                PeriodRange(startMillis, endMillis, label)
            }
            StatsPeriodType.MONTH -> {
                val targetMonth = today.withDayOfMonth(1).plusMonths(offset.toLong())
                val startMillis = targetMonth.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endMillis = targetMonth.plusMonths(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
                val label = targetMonth.format(monthYearFormatter)
                PeriodRange(startMillis, endMillis, label)
            }
            StatsPeriodType.YEAR -> {
                val targetYear = today.withDayOfYear(1).plusYears(offset.toLong())
                val startMillis = targetYear.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endMillis = targetYear.plusYears(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
                val label = targetYear.year.toString()
                PeriodRange(startMillis, endMillis, label)
            }
        }
    }

    fun computeSummaryComparison(
        transactions: List<TransactionEntity>,
        currentRange: PeriodRange,
        previousRange: PeriodRange
    ): PeriodSummaryComparison {
        var currentIncome = 0L
        var currentExpense = 0L
        var previousExpense = 0L

        for (tx in transactions) {
            if (tx.date in currentRange.startMillis..currentRange.endMillis) {
                when (tx.type) {
                    TransactionType.INCOME -> currentIncome += tx.amount
                    TransactionType.EXPENSE -> currentExpense += tx.amount
                    TransactionType.TRANSFER -> Unit
                }
            } else if (tx.date in previousRange.startMillis..previousRange.endMillis) {
                if (tx.type == TransactionType.EXPENSE) {
                    previousExpense += tx.amount
                }
            }
        }

        val expenseChangePercent = if (previousExpense <= 0L) {
            null
        } else {
            (((currentExpense - previousExpense) * 100L) / previousExpense).toInt()
        }

        return PeriodSummaryComparison(
            totalIncome = currentIncome,
            totalExpense = currentExpense,
            netBalance = currentIncome - currentExpense,
            previousExpense = previousExpense,
            expenseChangePercent = expenseChangePercent
        )
    }

    fun computeCategoryBreakdown(
        transactions: List<TransactionEntity>,
        categories: Map<Long, CategoryEntity>,
        startMillis: Long,
        endMillis: Long,
        uncategorizedLabel: String = "Tanpa kategori"
    ): List<CategoryExpenseSlice> {
        val totalsByCategory = HashMap<Long, Long>()
        var grandTotal = 0L

        for (tx in transactions) {
            if (tx.type == TransactionType.EXPENSE && tx.date in startMillis..endMillis) {
                val catId = tx.categoryId ?: -1L
                totalsByCategory[catId] = (totalsByCategory[catId] ?: 0L) + tx.amount
                grandTotal += tx.amount
            }
        }

        if (grandTotal <= 0L) return emptyList()

        return totalsByCategory.entries
            .sortedByDescending { it.value }
            .map { (catId, amount) ->
                val name = if (catId == -1L) {
                    uncategorizedLabel
                } else {
                    categories[catId]?.name ?: uncategorizedLabel
                }
                CategoryExpenseSlice(
                    categoryId = catId,
                    categoryName = name,
                    totalAmount = amount,
                    percentageInt = ((amount * 100L) / grandTotal).toInt(),
                    ratio = amount.toFloat() / grandTotal.toFloat()
                )
            }
    }

    fun computeTrendPoints(
        transactions: List<TransactionEntity>,
        periodType: StatsPeriodType,
        startMillis: Long,
        endMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<TrendPoint> {
        val startDate = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate()

        return when (periodType) {
            StatsPeriodType.WEEK -> {
                val totalsByDate = HashMap<LocalDate, Long>()
                for (tx in transactions) {
                    if (tx.type == TransactionType.EXPENSE && tx.date in startMillis..endMillis) {
                        val d = Instant.ofEpochMilli(tx.date).atZone(zoneId).toLocalDate()
                        totalsByDate[d] = (totalsByDate[d] ?: 0L) + tx.amount
                    }
                }
                (0..6).map { offset ->
                    val d = startDate.plusDays(offset.toLong())
                    TrendPoint(
                        label = d.format(shortDayFormatter).replace(".", "").take(3),
                        totalExpense = totalsByDate[d] ?: 0L
                    )
                }
            }
            StatsPeriodType.MONTH -> {
                val totalsByDay = HashMap<Int, Long>()
                for (tx in transactions) {
                    if (tx.type == TransactionType.EXPENSE && tx.date in startMillis..endMillis) {
                        val d = Instant.ofEpochMilli(tx.date).atZone(zoneId).toLocalDate()
                        totalsByDay[d.dayOfMonth] = (totalsByDay[d.dayOfMonth] ?: 0L) + tx.amount
                    }
                }
                val daysInMonth = startDate.lengthOfMonth()
                (1..daysInMonth).map { day ->
                    TrendPoint(
                        label = day.toString(),
                        totalExpense = totalsByDay[day] ?: 0L
                    )
                }
            }
            StatsPeriodType.YEAR -> {
                val totalsByMonth = HashMap<Int, Long>()
                for (tx in transactions) {
                    if (tx.type == TransactionType.EXPENSE && tx.date in startMillis..endMillis) {
                        val d = Instant.ofEpochMilli(tx.date).atZone(zoneId).toLocalDate()
                        totalsByMonth[d.monthValue] = (totalsByMonth[d.monthValue] ?: 0L) + tx.amount
                    }
                }
                (1..12).map { month ->
                    val monthDate = startDate.withMonth(month)
                    TrendPoint(
                        label = monthDate.format(shortMonthFormatter).replace(".", "").take(3),
                        totalExpense = totalsByMonth[month] ?: 0L
                    )
                }
            }
        }
    }
}
