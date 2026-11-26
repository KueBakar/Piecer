package com.piecer.app.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.piecer.app.R
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.repository.BudgetRepository
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.data.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun evaluateCurrentMonthBudgets(nowMillis: Long = System.currentTimeMillis()) {
        val budgets = budgetRepository.getAll()
        if (budgets.isEmpty()) return

        val monthStart = DateUtils.startOfMonthMillis(nowMillis)
        val monthEnd = DateUtils.endOfMonthMillis(nowMillis)
        val categoryTotals = transactionRepository
            .observeTotalByCategory(TransactionType.EXPENSE, monthStart, monthEnd)
            .first()
            .associate { it.categoryId to it.totalAmount }

        val categories = categoryRepository.observeAll().first().associateBy { it.id }

        budgets.forEach { budget ->
            val spent = categoryTotals[budget.categoryId] ?: 0L
            val categoryName = categories[budget.categoryId]?.name ?: return@forEach
            checkAndNotify(
                budgetId = budget.id,
                categoryName = categoryName,
                spent = spent,
                limit = budget.limitAmount,
                nowMillis = nowMillis
            )
        }
    }

    fun checkAndNotify(
        budgetId: Long,
        categoryName: String,
        spent: Long,
        limit: Long,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val progress = BudgetCalculator.calculate(spent, limit)
        val threshold = progress.threshold ?: return

        val localDate = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val monthKey = "${localDate.year}-${localDate.monthValue}"
        val alertKey = "${monthKey}_${budgetId}_${threshold.percent}"

        if (prefs.getBoolean(alertKey, false)) {
            return
        }

        if (!hasNotificationPermission()) {
            return
        }

        ensureChannel()

        val title = if (threshold == BudgetThreshold.REACHED_100) {
            context.getString(R.string.budget_notif_100_title, categoryName)
        } else {
            context.getString(R.string.budget_notif_80_title, categoryName)
        }
        val body = context.getString(
            R.string.budget_notif_body,
            progress.percentageInt,
            formatRupiah(spent),
            formatRupiah(limit)
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationId = (budgetId * 1000L + threshold.percent).toInt()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            prefs.edit().putBoolean(alertKey, true).apply()
        } catch (_: SecurityException) {
            // Izin notifikasi dicabut saat runtime
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.budget_notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.budget_notif_channel_desc)
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "budget_alerts"
        private const val PREFS_NAME = "piecer_budget_alerts"
    }
}
