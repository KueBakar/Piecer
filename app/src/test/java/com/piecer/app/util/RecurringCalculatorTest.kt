package com.piecer.app.util

import com.piecer.app.data.local.entity.RecurringEntity
import com.piecer.app.data.local.entity.RecurringInterval
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RecurringCalculatorTest {

    private val zone = ZoneId.of("Asia/Jakarta")

    private fun millis(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun toDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    @Test
    fun `monthly nextDueDate handles end of month 31 to feb 28 and back to mar 31`() {
        val jan31 = millis(2026, 1, 31)
        val febNext = RecurringCalculator.calculateNextDueDate(
            currentDueMillis = jan31,
            interval = RecurringInterval.MONTHLY,
            dayOfMonth = 31,
            zoneId = zone
        )
        assertEquals(LocalDate.of(2026, 2, 28), toDate(febNext))

        val marNext = RecurringCalculator.calculateNextDueDate(
            currentDueMillis = febNext,
            interval = RecurringInterval.MONTHLY,
            dayOfMonth = 31,
            zoneId = zone
        )
        assertEquals(LocalDate.of(2026, 3, 31), toDate(marNext))
    }

    @Test
    fun `monthly nextDueDate handles leap year feb 29`() {
        val jan30 = millis(2024, 1, 30)
        val febNext = RecurringCalculator.calculateNextDueDate(
            currentDueMillis = jan30,
            interval = RecurringInterval.MONTHLY,
            dayOfMonth = 30,
            zoneId = zone
        )
        assertEquals(LocalDate.of(2024, 2, 29), toDate(febNext))
    }

    @Test
    fun `weekly nextDueDate advances by 7 days`() {
        val start = millis(2026, 11, 1)
        val next = RecurringCalculator.calculateNextDueDate(
            currentDueMillis = start,
            interval = RecurringInterval.WEEKLY,
            zoneId = zone
        )
        assertEquals(LocalDate.of(2026, 11, 8), toDate(next))
    }

    @Test
    fun `collectDueOccurrences catches up missed periods and prevents duplicates`() {
        val item = RecurringEntity(
            id = 1L,
            type = TransactionType.EXPENSE,
            amount = 100_000L,
            categoryId = 3L,
            walletId = 1L,
            toWalletId = null,
            note = "WiFi",
            interval = RecurringInterval.MONTHLY,
            dayOfMonth = 15,
            dayOfWeek = null,
            nextDueDate = millis(2026, 9, 15),
            isActive = true,
            remindDaysBefore = 1,
            createdAt = millis(2026, 8, 15)
        )

        val now = millis(2026, 11, 20)
        val (dueDates, updatedNextDue) = RecurringCalculator.collectDueOccurrences(item, now, zone)
        assertEquals(3, dueDates.size)
        assertEquals(LocalDate.of(2026, 9, 15), toDate(dueDates[0]))
        assertEquals(LocalDate.of(2026, 10, 15), toDate(dueDates[1]))
        assertEquals(LocalDate.of(2026, 11, 15), toDate(dueDates[2]))
        assertEquals(LocalDate.of(2026, 12, 15), toDate(updatedNextDue))

        // Re-running with updated nextDueDate produces 0 duplicates
        val (secondRunDates, secondNextDue) = RecurringCalculator.collectDueOccurrences(
            item.copy(nextDueDate = updatedNextDue),
            now,
            zone
        )
        assertTrue(secondRunDates.isEmpty())
        assertEquals(updatedNextDue, secondNextDue)
    }

    @Test
    fun `detectSubscriptionSuggestions finds 3 consecutive months with same note and amount`() {
        val txs = listOf(
            TransactionEntity(1L, 54_000L, TransactionType.EXPENSE, 3L, 1L, null, millis(2026, 9, 10), "Netflix", 1L),
            TransactionEntity(2L, 54_000L, TransactionType.EXPENSE, 3L, 1L, null, millis(2026, 10, 11), "Netflix", 1L),
            TransactionEntity(3L, 54_000L, TransactionType.EXPENSE, 3L, 1L, null, millis(2026, 11, 10), "Netflix", 1L)
        )
        val suggestions = RecurringCalculator.detectSubscriptionSuggestions(txs, emptyList(), zone)
        assertEquals(1, suggestions.size)
        assertEquals("Netflix", suggestions[0].note)
        assertEquals(54_000L, suggestions[0].amount)
    }
}
