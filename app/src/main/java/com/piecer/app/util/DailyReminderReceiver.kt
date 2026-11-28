package com.piecer.app.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    DailyReminderScheduler.ACTION_DAILY_REMINDER -> {
                        val settings = settingsRepository.getSettings()
                        if (settings.dailyReminderEnabled) {
                            // Jadwalkan hari berikutnya DULU sebelum menampilkan notifikasi
                            dailyReminderScheduler.schedule(
                                settings.dailyReminderHour,
                                settings.dailyReminderMinute
                            )
                            showReminderNotification(
                                context = appContext,
                                body = appContext.getString(R.string.reminder_notif_body),
                                notificationId = NOTIFICATION_ID
                            )
                            settingsRepository.setLastReminderFiredAt(System.currentTimeMillis())
                        }
                    }

                    DailyReminderScheduler.ACTION_TEST_REMINDER -> {
                        showReminderNotification(
                            context = appContext,
                            body = appContext.getString(R.string.reminder_test_notif_body),
                            notificationId = TEST_NOTIFICATION_ID
                        )
                        settingsRepository.setLastReminderFiredAt(System.currentTimeMillis())
                    }
                }
            } catch (_: Throwable) {
                // Jangan biarkan penerima melempar exception di latar belakang
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showReminderNotification(
        context: Context,
        body: String,
        notificationId: Int
    ) {
        NotificationChannelHelper.ensureAllChannels(context)
        if (!NotificationChannelHelper.canPostNotification(context, NotificationChannelHelper.CHANNEL_DAILY_REMINDER)) {
            return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            CONTENT_REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannelHelper.CHANNEL_DAILY_REMINDER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Izin notifikasi dicabut saat runtime
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 4202
        private const val TEST_NOTIFICATION_ID = 4204
        private const val CONTENT_REQUEST_CODE = 14202
    }
}
