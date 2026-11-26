package com.piecer.app.util

import com.piecer.app.data.local.entity.BudgetEntity
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.ui.components.formatRupiah
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

enum class InsightType {
    CATEGORY_CHANGE,
    TOP_SPEND_DAY,
    END_CYCLE_PREDICTION,
    BUDGET_ALERT,
    STREAK
}

data class HomeInsight(
    val type: InsightType,
    val title: String,
    val message: String
)

object InsightEngine {

    fun generateInsights(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        budgets: List<BudgetEntity>,
        currentBalance: Long,
        cycleStartDay: Int,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        maxInsights: Int = 3
    ): List<HomeInsight> {
        val effectiveTxs = DateUtils.effectiveTransactions(transactions, nowMillis, zoneId)
        if (effectiveTxs.isEmpty()) return emptyList()

        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val currentPeriod = FinancialCycleHelper.currentPeriod(nowMillis, cycleStartDay, zoneId)
        val previousPeriod = FinancialCycleHelper.periodForOffset(nowMillis, cycleStartDay, offset = -1, zoneId)
        val categoryMap = categories.associateBy { it.id }

        val txDates = effectiveTxs.map {
            Instant.ofEpochMilli(it.date).atZone(zoneId).toLocalDate()
        }
        val earliestDate = txDates.minOrNull() ?: today
        val dataSpanDays = ChronoUnit.DAYS.between(earliestDate, today).toInt() + 1
        val hasEnoughData = dataSpanDays >= 7

        val insights = ArrayList<HomeInsight>()

        // (d) Anggaran hampir habis (>= 80%) - aman ditampilkan kapan saja
        budgetAlertInsight(effectiveTxs, budgets, categoryMap, currentPeriod)?.let {
            insights.add(it)
        }

        // (e) Streak hari berturut-turut mencatat - aman ditampilkan kapan saja (>= 2 hari)
        streakInsight(txDates.toSet(), today)?.let {
            insights.add(it)
        }

        if (hasEnoughData) {
            // (a) Kategori yang naik/turun paling besar dibanding siklus sebelumnya
            categoryChangeInsight(effectiveTxs, categoryMap, currentPeriod, previousPeriod)?.let {
                insights.add(it)
            }

            // (b) Hari dalam seminggu paling boros
            topSpendDayInsight(effectiveTxs, currentPeriod, zoneId)?.let {
                insights.add(it)
            }

            // (c) Prediksi saldo akhir siklus
            endCyclePredictionInsight(
                effectiveTxs = effectiveTxs,
                currentBalance = currentBalance,
                currentPeriod = currentPeriod,
                today = today
            )?.let {
                insights.add(it)
            }
        }

        return insights.take(maxInsights)
    }

    internal fun categoryChangeInsight(
        effectiveTxs: List<TransactionEntity>,
        categoryMap: Map<Long, CategoryEntity>,
        currentPeriod: CyclePeriod,
        previousPeriod: CyclePeriod
    ): HomeInsight? {
        val currentByCat = HashMap<Long, Long>()
        val prevByCat = HashMap<Long, Long>()

        for (tx in effectiveTxs) {
            if (tx.type != TransactionType.EXPENSE || tx.categoryId == null) continue
            when (tx.date) {
                in currentPeriod.startMillis..currentPeriod.endMillis -> {
                    currentByCat[tx.categoryId] = (currentByCat[tx.categoryId] ?: 0L) + tx.amount
                }
                in previousPeriod.startMillis..previousPeriod.endMillis -> {
                    prevByCat[tx.categoryId] = (prevByCat[tx.categoryId] ?: 0L) + tx.amount
                }
            }
        }

        if (prevByCat.isEmpty() || currentByCat.isEmpty()) return null

        var bestCatId: Long? = null
        var bestDiffAbs = 0L
        var bestChangePercent = 0

        for ((catId, prevAmount) in prevByCat) {
            if (prevAmount <= 0L) continue
            val currAmount = currentByCat[catId] ?: 0L
            val diff = currAmount - prevAmount
            val pct = ((diff * 100L) / prevAmount).toInt()
            if (abs(pct) >= 10 && abs(diff) > bestDiffAbs) {
                bestDiffAbs = abs(diff)
                bestCatId = catId
                bestChangePercent = pct
            }
        }

        val catName = bestCatId?.let { categoryMap[it]?.name } ?: return null
        val message = if (bestChangePercent > 0) {
            "Pengeluaran $catName naik ${bestChangePercent}% dibanding siklus sebelumnya."
        } else {
            "Pengeluaran $catName turun ${abs(bestChangePercent)}% dibanding siklus sebelumnya."
        }
        return HomeInsight(
            type = InsightType.CATEGORY_CHANGE,
            title = "Perubahan Kategori",
            message = message
        )
    }

    internal fun topSpendDayInsight(
        effectiveTxs: List<TransactionEntity>,
        currentPeriod: CyclePeriod,
        zoneId: ZoneId
    ): HomeInsight? {
        val totalsByDow = HashMap<DayOfWeek, Long>()
        for (tx in effectiveTxs) {
            if (tx.type == TransactionType.EXPENSE && tx.date in currentPeriod.startMillis..currentPeriod.endMillis) {
                val dow = Instant.ofEpochMilli(tx.date).atZone(zoneId).dayOfWeek
                totalsByDow[dow] = (totalsByDow[dow] ?: 0L) + tx.amount
            }
        }
        val top = totalsByDow.maxByOrNull { it.value } ?: return null
        if (top.value <= 0L) return null

        val dayName = when (top.key) {
            DayOfWeek.MONDAY -> "Senin"
            DayOfWeek.TUESDAY -> "Selasa"
            DayOfWeek.WEDNESDAY -> "Rabu"
            DayOfWeek.THURSDAY -> "Kamis"
            DayOfWeek.FRIDAY -> "Jumat"
            DayOfWeek.SATURDAY -> "Sabtu"
            DayOfWeek.SUNDAY -> "Minggu"
        }
        return HomeInsight(
            type = InsightType.TOP_SPEND_DAY,
            title = "Pola Mingguan",
            message = "Hari dengan pengeluaran paling tinggi di siklus ini: $dayName (${formatRupiah(top.value)})."
        )
    }

    internal fun endCyclePredictionInsight(
        effectiveTxs: List<TransactionEntity>,
        currentBalance: Long,
        currentPeriod: CyclePeriod,
        today: LocalDate
    ): HomeInsight? {
        val cycleExpense = effectiveTxs
            .filter { it.type == TransactionType.EXPENSE && it.date in currentPeriod.startMillis..currentPeriod.endMillis }
            .sumOf { it.amount }

        val elapsedDays = (ChronoUnit.DAYS.between(currentPeriod.startDate, today).toInt() + 1)
            .coerceAtLeast(1)
        if (elapsedDays < 3 || cycleExpense <= 0L) return null

        val avgDailyExpense = cycleExpense / elapsedDays.toLong()
        val predictedEndBalance = currentBalance - (avgDailyExpense * currentPeriod.remainingDays.toLong())

        return HomeInsight(
            type = InsightType.END_CYCLE_PREDICTION,
            title = "Perkiraan Akhir Siklus",
            message = "Rata-rata keluar ${formatRupiah(avgDailyExpense)}/hari. Perkiraan saldo akhir siklus sekitar ${formatRupiah(predictedEndBalance)}."
        )
    }

    internal fun budgetAlertInsight(
        effectiveTxs: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        categoryMap: Map<Long, CategoryEntity>,
        currentPeriod: CyclePeriod
    ): HomeInsight? {
        if (budgets.isEmpty()) return null
        val spentByCat = effectiveTxs
            .filter {
                it.type == TransactionType.EXPENSE &&
                    it.categoryId != null &&
                    it.date in currentPeriod.startMillis..currentPeriod.endMillis
            }
            .groupBy { it.categoryId!! }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val critical = budgets
            .mapNotNull { b ->
                val catName = categoryMap[b.categoryId]?.name ?: return@mapNotNull null
                val progress = BudgetCalculator.calculate(spentByCat[b.categoryId] ?: 0L, b.limitAmount)
                catName to progress
            }
            .filter { it.second.percentageInt >= 80 }
            .maxByOrNull { it.second.percentageInt }
            ?: return null

        val (catName, progress) = critical
        val message = if (progress.percentageInt >= 100) {
            "Anggaran $catName sudah mencapai ${progress.percentageInt}% di siklus ini."
        } else {
            "Anggaran $catName sudah terpakai ${progress.percentageInt}% di siklus ini."
        }
        return HomeInsight(
            type = InsightType.BUDGET_ALERT,
            title = "Info Anggaran",
            message = message
        )
    }

    internal fun streakInsight(
        txDates: Set<LocalDate>,
        today: LocalDate
    ): HomeInsight? {
        if (txDates.isEmpty()) return null
        var cursor = if (today in txDates) today else today.minusDays(1)
        if (cursor !in txDates) return null

        var streak = 0
        while (cursor in txDates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        if (streak < 2) return null

        return HomeInsight(
            type = InsightType.STREAK,
            title = "Catatan Rutin",
            message = "Kamu sudah mencatat transaksi $streak hari berturut-turut."
        )
    }
}
