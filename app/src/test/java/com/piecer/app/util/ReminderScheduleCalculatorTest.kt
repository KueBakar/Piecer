package com.piecer.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderScheduleCalculatorTest {

    @Test
    fun nextDailyTriggerMillis_handlesSameDayAndNextDayRollover() {
        val zone = ZoneId.of("Asia/Jakarta")
        val morning = ZonedDateTime.of(2026, 5, 10, 9, 30, 0, 0, zone)
        val sameDayExpected = ZonedDateTime.of(2026, 5, 10, 20, 0, 0, 0, zone)
        assertEquals(
            sameDayExpected.toInstant().toEpochMilli(),
            ReminderScheduleCalculator.nextDailyTriggerMillis(
                hour = 20,
                minute = 0,
                nowMillis = morning.toInstant().toEpochMilli(),
                zoneId = zone
            )
        )

        // Rantai penjadwalan: tepat pada jam pemicu harus menjadwalkan hari berikutnya
        val atTrigger = ZonedDateTime.of(2026, 5, 10, 20, 0, 0, 0, zone)
        val nextDayExpected = ZonedDateTime.of(2026, 5, 11, 20, 0, 0, 0, zone)
        assertEquals(
            nextDayExpected.toInstant().toEpochMilli(),
            ReminderScheduleCalculator.nextDailyTriggerMillis(
                hour = 20,
                minute = 0,
                nowMillis = atTrigger.toInstant().toEpochMilli(),
                zoneId = zone
            )
        )
    }

    @Test
    fun nextDailyTriggerMillis_handlesMonthEndAndLeapYear() {
        val zone = ZoneId.of("Asia/Jakarta")
        val jan31Late = ZonedDateTime.of(2026, 1, 31, 21, 0, 0, 0, zone)
        val feb1Expected = ZonedDateTime.of(2026, 2, 1, 20, 0, 0, 0, zone)
        assertEquals(
            feb1Expected.toInstant().toEpochMilli(),
            ReminderScheduleCalculator.nextDailyTriggerMillis(
                hour = 20,
                minute = 0,
                nowMillis = jan31Late.toInstant().toEpochMilli(),
                zoneId = zone
            )
        )

        val feb28LeapLate = ZonedDateTime.of(2024, 2, 28, 22, 0, 0, 0, zone)
        val feb29Expected = ZonedDateTime.of(2024, 2, 29, 8, 0, 0, 0, zone)
        assertEquals(
            feb29Expected.toInstant().toEpochMilli(),
            ReminderScheduleCalculator.nextRecurringCheckTriggerMillis(
                nowMillis = feb28LeapLate.toInstant().toEpochMilli(),
                zoneId = zone
            )
        )
    }

    @Test
    fun nextDailyTriggerMillis_respectsTimezone() {
        val wita = ZoneId.of("Asia/Makassar")
        val wib = ZoneId.of("Asia/Jakarta")
        val instant = ZonedDateTime.of(2026, 6, 15, 19, 30, 0, 0, wib).toInstant()

        // 19:30 WIB = 20:30 WITA -> untuk jam 20:00, WIB masih hari ini, WITA sudah besok
        val nextWib = ReminderScheduleCalculator.nextDailyTriggerMillis(20, 0, instant.toEpochMilli(), wib)
        val nextWita = ReminderScheduleCalculator.nextDailyTriggerMillis(20, 0, instant.toEpochMilli(), wita)

        assertEquals(15, java.time.Instant.ofEpochMilli(nextWib).atZone(wib).dayOfMonth)
        assertEquals(16, java.time.Instant.ofEpochMilli(nextWita).atZone(wita).dayOfMonth)
    }

    @Test
    fun nextMidnightWidgetTriggerMillis_schedulesNextDay000005() {
        val zone = ZoneId.of("Asia/Jakarta")
        val dec31 = ZonedDateTime.of(2026, 12, 31, 23, 59, 50, 0, zone)
        val jan1Expected = ZonedDateTime.of(2027, 1, 1, 0, 0, 5, 0, zone)
        assertEquals(
            jan1Expected.toInstant().toEpochMilli(),
            ReminderScheduleCalculator.nextMidnightWidgetTriggerMillis(
                nowMillis = dec31.toInstant().toEpochMilli(),
                zoneId = zone
            )
        )
    }

    @Test
    fun nextDebtReminderTriggerMillis_returnsFutureOnly() {
        val zone = ZoneId.of("Asia/Jakarta")
        val dueMillis = ZonedDateTime.of(2026, 7, 10, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        val nowBeforeH1 = ZonedDateTime.of(2026, 7, 8, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val nowAfterH1 = ZonedDateTime.of(2026, 7, 9, 10, 0, 0, 0, zone).toInstant().toEpochMilli()

        val h1Trigger = ReminderScheduleCalculator.nextDebtReminderTriggerMillis(dueMillis, 1L, nowBeforeH1, zone)
        assertEquals(
            ZonedDateTime.of(2026, 7, 9, 9, 0, 0, 0, zone).toInstant().toEpochMilli(),
            h1Trigger
        )
        assertNull(ReminderScheduleCalculator.nextDebtReminderTriggerMillis(dueMillis, 1L, nowAfterH1, zone))
    }

    @Test
    fun shouldRestoreAlarms_matchesSupportedBroadcastActionsOnly() {
        assertTrue(ReminderScheduleCalculator.shouldRestoreAlarms("android.intent.action.BOOT_COMPLETED"))
        assertTrue(ReminderScheduleCalculator.shouldRestoreAlarms("android.intent.action.MY_PACKAGE_REPLACED"))
        assertTrue(ReminderScheduleCalculator.shouldRestoreAlarms("android.intent.action.TIME_SET"))
        assertTrue(ReminderScheduleCalculator.shouldRestoreAlarms("android.intent.action.TIMEZONE_CHANGED"))
        assertFalse(ReminderScheduleCalculator.shouldRestoreAlarms("android.intent.action.AIRPLANE_MODE"))
        assertFalse(ReminderScheduleCalculator.shouldRestoreAlarms(null))
    }
}
