package com.piecer.app.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.piecer.app.MainActivity
import com.piecer.app.R
import com.piecer.app.data.preferences.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DailyReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var dailyReminderScheduler: DailyReminderScheduler

    @Inject
    lateinit var recurringProcessor: RecurringProcessor

    @Inject
    lateinit var debtReminderHelper: DebtReminderHelper

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { recurringProcessor.checkUpcomingReminders() }
                runCatching { debtReminderHelper.checkUpcomingReminders() }
                val settings = settingsRepository.getSettings()
                if (!settings.dailyReminderEnabled) return@launch

                if (action == DailyReminderScheduler.ACTION_DAILY_REMINDER) {
                    showReminderNotification(context)
                }
                dailyReminderScheduler.schedule(
                    settings.dailyReminderHour,
                    settings.dailyReminderMinute
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showReminderNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.reminder_notif_channel_desc)
            }
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.reminder_notif_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Izin notifikasi dicabut saat runtime
        }
    }

    companion object {
        private const val CHANNEL_ID = "daily_reminder"
        private const val NOTIFICATION_ID = 4202
    }
}
