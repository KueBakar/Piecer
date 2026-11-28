package com.piecer.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun schedule(hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = ReminderScheduleCalculator.nextDailyTriggerMillis(
            hour = hour,
            minute = minute,
            nowMillis = System.currentTimeMillis()
        )

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun scheduleTestReminder(): Long {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return ReminderScheduleCalculator.nextTestReminderTriggerMillis(System.currentTimeMillis())
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_TEST_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TEST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = ReminderScheduleCalculator.nextTestReminderTriggerMillis(
            nowMillis = System.currentTimeMillis()
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
        return triggerAtMillis
    }

    fun cancel() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    companion object {
        const val ACTION_DAILY_REMINDER = "com.piecer.app.ACTION_DAILY_REMINDER"
        const val ACTION_TEST_REMINDER = "com.piecer.app.ACTION_TEST_REMINDER"
        private const val REQUEST_CODE = 4201
        private const val TEST_REQUEST_CODE = 4203
    }
}
