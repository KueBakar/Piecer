package com.piecer.app.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object ReminderScheduleCalculator {

    const val TEST_REMINDER_DELAY_MILLIS = 60_000L

    private val RESTORE_ACTIONS = setOf(
        "android.intent.action.BOOT_COMPLETED",
        "android.intent.action.MY_PACKAGE_REPLACED",
        "android.intent.action.TIME_SET",
        "android.intent.action.TIMEZONE_CHANGED"
    )

    fun nextDailyTriggerMillis(
        hour: Int,
        minute: Int,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zoneId)
        var nextTrigger = now
            .withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!nextTrigger.isAfter(now)) {
            nextTrigger = nextTrigger.plusDays(1)
        }
        return nextTrigger.toInstant().toEpochMilli()
    }

    fun nextRecurringCheckTriggerMillis(
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        checkHour: Int = 8,
        checkMinute: Int = 0
    ): Long = nextDailyTriggerMillis(
        hour = checkHour,
        minute = checkMinute,
        nowMillis = nowMillis,
        zoneId = zoneId
    )

    fun nextDebtReminderTriggerMillis(
        dueDateMillis: Long,
        daysBefore: Long,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val dueLocal = Instant.ofEpochMilli(dueDateMillis).atZone(zoneId).toLocalDate()
        val triggerMillis = dueLocal
            .minusDays(daysBefore)
            .atTime(9, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        return triggerMillis.takeIf { it > nowMillis }
    }

    fun nextMidnightWidgetTriggerMillis(
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zoneId)
        return now.toLocalDate()
            .plusDays(1)
            .atTime(0, 0, 5)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun nextTestReminderTriggerMillis(
        nowMillis: Long,
        delayMillis: Long = TEST_REMINDER_DELAY_MILLIS
    ): Long = nowMillis + delayMillis

    fun shouldRestoreAlarms(action: String?): Boolean {
        return action != null && action in RESTORE_ACTIONS
    }
}
