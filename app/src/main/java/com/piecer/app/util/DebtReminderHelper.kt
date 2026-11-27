package com.piecer.app.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.piecer.app.MainActivity
import com.piecer.app.R
import com.piecer.app.data.local.entity.DebtDirection
import com.piecer.app.data.local.entity.DebtEntity
import com.piecer.app.data.repository.DebtRepository
import com.piecer.app.ui.components.formatRupiah
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtReminderHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val debtRepository: DebtRepository
) {
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun scheduleReminderForDebt(debtId: Long, dueDateMillis: Long?) {
        if (dueDateMillis == null) return
        val zone = ZoneId.systemDefault()
        val dueLocal = Instant.ofEpochMilli(dueDateMillis).atZone(zone).toLocalDate()
        val now = System.currentTimeMillis()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Jadwalkan H-1 dan Hari H pada pukul 09:00 waktu lokal (tidak presisi)
        listOf(1L, 0L).forEach { daysBefore ->
            val triggerMillis = dueLocal.minusDays(daysBefore)
                .atTime(9, 0)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
            if (triggerMillis > now) {
                val intent = Intent(context, DailyReminderReceiver::class.java).apply {
                    action = ACTION_DEBT_REMINDER
                }
                val requestCode = (REQUEST_CODE_BASE + debtId * 2 + daysBefore).toInt()
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        }
    }

    suspend fun checkUpcomingReminders(nowMillis: Long = System.currentTimeMillis()) {
        val unpaid = debtRepository.getUnpaidDebtsWithDueDate()
        if (unpaid.isEmpty()) return

        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()

        for (debt in unpaid) {
            val dueMillis = debt.dueDate ?: continue
            val dueLocal = Instant.ofEpochMilli(dueMillis).atZone(zone).toLocalDate()
            val daysUntil = ChronoUnit.DAYS.between(today, dueLocal).toInt()
            if (daysUntil == 0 || daysUntil == 1) {
                val notifKey = "debt_reminded_${debt.id}_${dueMillis}_$daysUntil"
                if (prefs.getBoolean(notifKey, false)) continue
                showNotification(debt, daysUntil, notifKey)
            }
        }
    }

    private fun showNotification(debt: DebtEntity, daysUntil: Int, notifKey: String) {
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
                context.getString(R.string.debts_notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.debts_notif_channel_desc)
            }
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            debt.id.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remainingText = formatRupiah(DebtCalculator.remainingAmount(debt))
        val typeLabel = if (debt.direction == DebtDirection.OWE_ME) {
            context.getString(R.string.debts_type_owe_me)
        } else {
            context.getString(R.string.debts_type_i_owe)
        }
        val whenText = if (daysUntil == 0) {
            context.getString(R.string.debts_due_today)
        } else {
            context.getString(R.string.debts_due_tomorrow)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.debts_notif_title))
            .setContentText("$typeLabel ${debt.personName} • $remainingText ($whenText)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify((6000 + debt.id).toInt(), notification)
            prefs.edit().putBoolean(notifKey, true).apply()
        } catch (_: SecurityException) {
            // Izin notifikasi dicabut saat runtime
        }
    }

    companion object {
        const val ACTION_DEBT_REMINDER = "com.piecer.app.ACTION_DEBT_REMINDER"
        private const val CHANNEL_ID = "debt_reminders"
        private const val PREFS_NAME = "piecer_debt_alerts"
        private const val REQUEST_CODE_BASE = 6200L
    }
}
