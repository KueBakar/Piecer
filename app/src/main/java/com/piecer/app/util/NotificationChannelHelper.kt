package com.piecer.app.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.piecer.app.R

object NotificationChannelHelper {
    const val CHANNEL_DAILY_REMINDER = "daily_reminder"
    const val CHANNEL_RECURRING_REMINDERS = "recurring_reminders"
    const val CHANNEL_DEBT_REMINDERS = "debt_reminders"
    const val CHANNEL_BUDGET_ALERTS = "budget_alerts"

    fun ensureAllChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val specs = listOf(
            Triple(
                CHANNEL_DAILY_REMINDER,
                R.string.reminder_notif_channel_name,
                R.string.reminder_notif_channel_desc
            ),
            Triple(
                CHANNEL_RECURRING_REMINDERS,
                R.string.recurring_notif_channel_name,
                R.string.recurring_notif_channel_desc
            ),
            Triple(
                CHANNEL_DEBT_REMINDERS,
                R.string.debts_notif_channel_name,
                R.string.debts_notif_channel_desc
            ),
            Triple(
                CHANNEL_BUDGET_ALERTS,
                R.string.budget_notif_channel_name,
                R.string.budget_notif_channel_desc
            )
        )

        for ((id, nameRes, descRes) in specs) {
            if (manager.getNotificationChannel(id) == null) {
                val channel = NotificationChannel(
                    id,
                    context.getString(nameRes),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(descRes)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun isChannelEnabled(context: Context, channelId: String): Boolean {
        ensureAllChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false
        val channel = manager.getNotificationChannel(channelId) ?: return false
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun canPostNotification(context: Context, channelId: String): Boolean {
        if (!hasNotificationPermission(context)) return false
        return isChannelEnabled(context, channelId)
    }
}
