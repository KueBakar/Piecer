package com.piecer.app.util

import com.piecer.app.data.local.entity.RecurringEntity
import com.piecer.app.data.local.entity.RecurringInterval
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

data class SubscriptionSuggestion(
    val note: String,
    val amount: Long,
    val categoryId: Long?,
    val walletId: Long,
    val dayOfMonth: Int
)

object RecurringCalculator {

    fun calculateNextDueDate(
        currentDueMillis: Long,
        interval: RecurringInterval,
        dayOfMonth: Int? = null,
        dayOfWeek: Int? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val currentDate = Instant.ofEpochMilli(currentDueMillis).atZone(zoneId).toLocalDate()
        val nextDate: LocalDate = when (interval) {
            RecurringInterval.WEEKLY -> {
                val targetDow = dayOfWeek?.coerceIn(1, 7)?.let { DayOfWeek.of(it) }
                if (targetDow == null || targetDow == currentDate.dayOfWeek) {
                    currentDate.plusWeeks(1)
                } else {
                    currentDate.with(TemporalAdjusters.next(targetDow))
                }
            }
            RecurringInterval.MONTHLY -> {
                val targetMonth = YearMonth.from(currentDate).plusMonths(1)
                val desiredDay = (dayOfMonth ?: currentDate.dayOfMonth).coerceIn(1, 31)
                val clampedDay = desiredDay.coerceAtMost(targetMonth.lengthOfMonth())
                targetMonth.atDay(clampedDay)
            }
        }
        return nextDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    /**
     * Mengumpulkan semua tanggal jatuh tempo yang sudah lewat/hari ini sampai [upToMillis],
     * serta mengembalikan [nextDueDate] berikutnya setelah semua periode diproses.
     */
    fun collectDueOccurrences(
        item: RecurringEntity,
        upToMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        maxOccurrences: Int = 120
    ): Pair<List<Long>, Long> {
        if (!item.isActive) return emptyList<Long>() to item.nextDueDate
        val cutoff = DateUtils.endOfTodayMillis(upToMillis, zoneId)
        val occurrences = ArrayList<Long>()
        var cursor = item.nextDueDate
        var guard = 0
        while (cursor <= cutoff && guard < maxOccurrences) {
            occurrences.add(cursor)
            val next = calculateNextDueDate(
                currentDueMillis = cursor,
                interval = item.interval,
                dayOfMonth = item.dayOfMonth,
                dayOfWeek = item.dayOfWeek,
                zoneId = zoneId
            )
            if (next <= cursor) break
            cursor = next
            guard++
        }
        return occurrences to cursor
    }

    /**
     * Mendeteksi langganan sederhana: 3 transaksi EXPENSE dengan catatan & nominal sama
     * pada tanggal berdekatan (selisih hari <= 3) di 3 bulan beruntun.
     */
    fun detectSubscriptionSuggestions(
        transactions: List<TransactionEntity>,
        existingRecurring: List<RecurringEntity>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<SubscriptionSuggestion> {
        val existingKeys = existingRecurring.map {
            it.note.trim().lowercase() to it.amount
        }.toSet()

        val grouped = transactions
            .filter { it.type == TransactionType.EXPENSE && it.note.isNotBlank() }
            .groupBy { it.note.trim().lowercase() to it.amount }

        val suggestions = ArrayList<SubscriptionSuggestion>()

        for ((key, txs) in grouped) {
            if (key in existingKeys) continue
            if (txs.size < 3) continue

            val sorted = txs.sortedBy { it.date }
            val byMonth = sorted.map { tx ->
                val d = Instant.ofEpochMilli(tx.date).atZone(zoneId).toLocalDate()
                Triple(YearMonth.from(d), d.dayOfMonth, tx)
            }.distinctBy { it.first }

            if (byMonth.size < 3) continue

            var matched = false
            for (i in 0..(byMonth.size - 3)) {
                val m1 = byMonth[i]
                val m2 = byMonth[i + 1]
                val m3 = byMonth[i + 2]
                if (m1.first.plusMonths(1) == m2.first && m2.first.plusMonths(1) == m3.first) {
                    val d1 = m1.second
                    val d2 = m2.second
                    val d3 = m3.second
                    if (abs(d1 - d2) <= 3 && abs(d2 - d3) <= 3 && abs(d1 - d3) <= 3) {
                        val latestTx = m3.third
                        suggestions.add(
                            SubscriptionSuggestion(
                                note = latestTx.note.trim(),
                                amount = latestTx.amount,
                                categoryId = latestTx.categoryId,
                                walletId = latestTx.walletId,
                                dayOfMonth = d3
                            )
                        )
                        matched = true
                        break
                    }
                }
            }
            if (matched) continue
        }

        return suggestions
    }
}
