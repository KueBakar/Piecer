package com.piecer.app.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.withTransaction
import com.piecer.app.MainActivity
import com.piecer.app.R
import com.piecer.app.data.local.PiecerDatabase
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PiecerDatabase,
    private val budgetNotificationHelper: BudgetNotificationHelper
) {
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun processDueAndNotify(nowMillis: Long = System.currentTimeMillis()): Int {
        val generatedCount = processDueRecurring(nowMillis)
        checkUpcomingReminders(nowMillis)
        return generatedCount
    }

    suspend fun processDueRecurring(nowMillis: Long = System.currentTimeMillis()): Int {
        val cutoff = DateUtils.endOfTodayMillis(nowMillis)
        var generatedCount = 0
        var hasExpense = false

        database.withTransaction {
            val recurringDao = database.recurringDao()
            val txDao = database.transactionDao()
            val dueItems = recurringDao.getDueActive(cutoff)

            for (item in dueItems) {
                val (dueDates, nextDue) = RecurringCalculator.collectDueOccurrences(item, nowMillis)
                if (dueDates.isNotEmpty()) {
                    for (dueDate in dueDates) {
                        txDao.insert(
                            TransactionEntity(
                                amount = item.amount,
                                type = item.type,
                                categoryId = if (item.type == TransactionType.TRANSFER) null else item.categoryId,
                                walletId = item.walletId,
                                toWalletId = if (item.type == TransactionType.TRANSFER) item.toWalletId else null,
                                date = dueDate,
                                note = item.note,
                                createdAt = nowMillis
                            )
                        )
                        generatedCount++
                        if (item.type == TransactionType.EXPENSE) {
                            hasExpense = true
                        }
                    }
                    recurringDao.update(item.copy(nextDueDate = nextDue))
                }
            }
        }

        if (hasExpense) {
            budgetNotificationHelper.evaluateCurrentMonthBudgets(nowMillis)
        }
        return generatedCount
    }

    suspend fun checkUpcomingReminders(nowMillis: Long = System.currentTimeMillis()) {
        val activeItems = database.recurringDao().getAllActive()
        if (activeItems.isEmpty()) return

        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()

        for (item in activeItems) {
            if (item.remindDaysBefore <= 0) continue
            val dueDate = Instant.ofEpochMilli(item.nextDueDate).atZone(zone).toLocalDate()
            val daysUntil = ChronoUnit.DAYS.between(today, dueDate).toInt()
            if (daysUntil in 0..item.remindDaysBefore) {
                val notifKey = "reminded_${item.id}_${item.nextDueDate}"
                if (prefs.getBoolean(notifKey, false)) continue
                showReminderNotification(item.id, item.note, item.amount, daysUntil, notifKey)
            }
        }
    }

    private fun showReminderNotification(
        recurringId: Long,
        note: String,
        amount: Long,
        daysUntil: Int,
        notifKey: String
    ) {
        NotificationChannelHelper.ensureAllChannels(context)
        if (!NotificationChannelHelper.canPostNotification(context, CHANNEL_ID)) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            (CONTENT_REQUEST_CODE_BASE + recurringId).toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val label = note.ifBlank { context.getString(R.string.recurring_default_label) }
        val whenText = if (daysUntil == 0) {
            context.getString(R.string.recurring_due_today)
        } else {
            context.getString(R.string.recurring_due_in_days, daysUntil)
        }
        val body = "$label • ${formatRupiah(amount)} ($whenText)"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.recurring_notif_title))
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify((5000 + recurringId).toInt(), notification)
            prefs.edit().putBoolean(notifKey, true).apply()
        } catch (_: SecurityException) {
            // Izin notifikasi dicabut saat runtime
        }
    }

    companion object {
        private const val CHANNEL_ID = NotificationChannelHelper.CHANNEL_RECURRING_REMINDERS
        private const val PREFS_NAME = "piecer_recurring_alerts"
        private const val CONTENT_REQUEST_CODE_BASE = 15000L
    }
}
