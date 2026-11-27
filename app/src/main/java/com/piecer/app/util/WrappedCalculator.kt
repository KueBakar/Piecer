package com.piecer.app.util

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import java.util.Calendar

enum class WrappedPeriodMode {
    MONTH,
    YEAR
}

data class WrappedSummary(
    val periodLabel: String,
    val mode: WrappedPeriodMode,
    val hasData: Boolean,
    val totalExpense: Long,
    val totalIncome: Long,
    val topCategoryName: String?,
    val topCategoryAmount: Long,
    val topCategoryPercent: Int,
    val mostExpensiveDayMillis: Long?,
    val mostExpensiveDayAmount: Long,
    val previousPeriodExpense: Long,
    val expenseChangePercent: Int?,
    val savingsRatioPercent: Int,
    val recordingStreakDays: Int
)

object WrappedCalculator {

    fun calculate(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        mode: WrappedPeriodMode,
        year: Int,
        monthZeroBased: Int,
        cycleStartDay: Int,
        nowMillis: Long,
        periodLabel: String
    ): WrappedSummary {
        // Transaksi terjadwal (masa depan) tidak ikut terhitung.
        val effectiveTxs = transactions.filter { it.dateMillis <= nowMillis }

        val (startMillis, endMillis, prevStartMillis, prevEndMillis) = resolveBounds(
            mode = mode,
            year = year,
            monthZeroBased = monthZeroBased,
            cycleStartDay = cycleStartDay
        )

        val periodTxs = effectiveTxs.filter { it.dateMillis in startMillis..endMillis }
        val prevPeriodTxs = effectiveTxs.filter { it.dateMillis in prevStartMillis..prevEndMillis }

        if (periodTxs.isEmpty()) {
            return WrappedSummary(
                periodLabel = periodLabel,
                mode = mode,
                hasData = false,
                totalExpense = 0L,
                totalIncome = 0L,
                topCategoryName = null,
                topCategoryAmount = 0L,
                topCategoryPercent = 0,
                mostExpensiveDayMillis = null,
                mostExpensiveDayAmount = 0L,
                previousPeriodExpense = 0L,
                expenseChangePercent = null,
                savingsRatioPercent = 0,
                recordingStreakDays = 0
            )
        }

        val expenses = periodTxs.filter { it.type == TransactionType.EXPENSE }
        val incomes = periodTxs.filter { it.type == TransactionType.INCOME }
        val totalExpense = expenses.sumOf { it.amount }
        val totalIncome = incomes.sumOf { it.amount }

        val categoryNames = categories.associate { it.id to it.name }
        val topCategoryEntry = expenses
            .filter { it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .maxByOrNull { it.value }

        val topCategoryName = topCategoryEntry?.key?.let { categoryNames[it] }
        val topCategoryAmount = topCategoryEntry?.value ?: 0L
        val topCategoryPercent = if (totalExpense > 0L && topCategoryAmount > 0L) {
            ((topCategoryAmount * 100L) / totalExpense).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val dayTotals = expenses.groupBy { startOfDayMillis(it.dateMillis) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val mostExpensiveDay = dayTotals.maxByOrNull { it.value }

        val prevExpense = prevPeriodTxs
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val expenseChangePercent = if (prevExpense > 0L) {
            (((totalExpense - prevExpense) * 100L) / prevExpense).toInt()
        } else {
            null
        }

        val savingsRatioPercent = if (totalIncome > 0L) {
            (((totalIncome - totalExpense) * 100L) / totalIncome).toInt().coerceIn(-999, 100)
        } else {
            0
        }

        val streak = calculateLongestStreak(periodTxs)

        return WrappedSummary(
            periodLabel = periodLabel,
            mode = mode,
            hasData = true,
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            topCategoryName = topCategoryName,
            topCategoryAmount = topCategoryAmount,
            topCategoryPercent = topCategoryPercent,
            mostExpensiveDayMillis = mostExpensiveDay?.key,
            mostExpensiveDayAmount = mostExpensiveDay?.value ?: 0L,
            previousPeriodExpense = prevExpense,
            expenseChangePercent = expenseChangePercent,
            savingsRatioPercent = savingsRatioPercent,
            recordingStreakDays = streak
        )
    }

    fun buildShareText(summary: WrappedSummary, hideAmounts: Boolean): String {
        if (!summary.hasData) {
            return "PIECER Wrapped (${summary.periodLabel}) - Belum ada transaksi tercatat."
        }
        val sb = StringBuilder()
        sb.append("PIECER Wrapped • ${summary.periodLabel}\n")
        if (hideAmounts) {
            sb.append("Rasio tabungan: ${summary.savingsRatioPercent}%\n")
            if (summary.topCategoryName != null) {
                sb.append("Kategori teratas: ${summary.topCategoryName} (${summary.topCategoryPercent}%)\n")
            }
        } else {
            sb.append("Total pengeluaran: ${RupiahUtils.format(summary.totalExpense)}\n")
            if (summary.topCategoryName != null) {
                sb.append("Kategori teratas: ${summary.topCategoryName} (${RupiahUtils.format(summary.topCategoryAmount)})\n")
            }
            sb.append("Rasio tabungan: ${summary.savingsRatioPercent}%\n")
        }
        sb.append("Streak mencatat: ${summary.recordingStreakDays} hari")
        return sb.toString()
    }

    private data class PeriodBounds(
        val start: Long,
        val end: Long,
        val prevStart: Long,
        val prevEnd: Long
    )

    private fun resolveBounds(
        mode: WrappedPeriodMode,
        year: Int,
        monthZeroBased: Int,
        cycleStartDay: Int
    ): PeriodBounds {
        return when (mode) {
            WrappedPeriodMode.MONTH -> {
                val anchor = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, monthZeroBased)
                    set(Calendar.DAY_OF_MONTH, cycleStartDay.coerceIn(1, 28))
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val current = FinancialCycleHelper.getCyclePeriod(anchor, cycleStartDay)
                val prevAnchor = Calendar.getInstance().apply {
                    timeInMillis = current.startMillis
                    add(Calendar.MONTH, -1)
                    set(Calendar.HOUR_OF_DAY, 12)
                }.timeInMillis
                val previous = FinancialCycleHelper.getCyclePeriod(prevAnchor, cycleStartDay)
                PeriodBounds(current.startMillis, current.endMillis, previous.startMillis, previous.endMillis)
            }

            WrappedPeriodMode.YEAR -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, Calendar.DECEMBER)
                    set(Calendar.DAY_OF_MONTH, 31)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                val prevStart = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year - 1)
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val prevEnd = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year - 1)
                    set(Calendar.MONTH, Calendar.DECEMBER)
                    set(Calendar.DAY_OF_MONTH, 31)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                PeriodBounds(start, end, prevStart, prevEnd)
            }
        }
    }

    private fun startOfDayMillis(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun calculateLongestStreak(transactions: List<TransactionEntity>): Int {
        if (transactions.isEmpty()) return 0
        val dayKeys = transactions.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        }.distinct().sorted()

        var longest = 1
        var current = 1
        for (i in 1 until dayKeys.size) {
            if (dayKeys[i] == dayKeys[i - 1] + 1) {
                current++
                if (current > longest) longest = current
            } else {
                current = 1
            }
        }
        return longest
    }
}
