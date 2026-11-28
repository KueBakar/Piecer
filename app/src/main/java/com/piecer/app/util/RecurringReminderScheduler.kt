package com.piecer.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleNext(nowMillis: Long = System.currentTimeMillis()) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, RecurringReminderReceiver::class.java).apply {
            action = ACTION_RECURRING_CHECK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = ReminderScheduleCalculator.nextRecurringCheckTriggerMillis(
            nowMillis = nowMillis
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancel() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, RecurringReminderReceiver::class.java).apply {
            action = ACTION_RECURRING_CHECK
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
        const val ACTION_RECURRING_CHECK = "com.piecer.app.ACTION_RECURRING_CHECK"
        private const val REQUEST_CODE = 4301
    }
}
