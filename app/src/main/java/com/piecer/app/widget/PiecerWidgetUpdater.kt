package com.piecer.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.piecer.app.MainActivity
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.ui.navigation.PiecerRoutes
import com.piecer.app.util.BalanceCalculator
import com.piecer.app.util.BudgetCalculator
import com.piecer.app.util.DateUtils
import com.piecer.app.util.FinancialCycleHelper
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.util.Calendar

data class WidgetTransactionItem(
    val title: String,
    val amountText: String,
    val isIncome: Boolean,
    val isExpense: Boolean
)

data class PiecerWidgetData(
    val dailyAllowanceText: String,
    val totalBalanceText: String,
    val recentTransactions: List<WidgetTransactionItem>,
    val hideAmounts: Boolean
)

object PiecerWidgetUpdater {
    const val EXTRA_START_ROUTE = "extra_start_route"
    const val EXTRA_QUICK_TYPE = "extra_quick_type"
    const val EXTRA_FOCUS_QUICK_ENTRY = "extra_focus_quick_entry"
    private const val MIDNIGHT_REQUEST_CODE = 4401

    fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        listOf(
            PiecerCompactWidgetProvider::class.java,
            PiecerLargeWidgetProvider::class.java
        ).forEach { providerClass ->
            val ids = manager.getAppWidgetIds(ComponentName(appContext, providerClass))
            if (ids.isNotEmpty()) {
                val intent = Intent(appContext, providerClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                appContext.sendBroadcast(intent)
            }
        }
    }

    fun scheduleMidnightUpdate(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 5)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val intent = Intent(appContext, PiecerWidgetMidnightReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            MIDNIGHT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, nextMidnight, pendingIntent)
    }

    fun createAddTransactionPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_START_ROUTE, PiecerRoutes.ADD_TRANSACTION)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun createOpenAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    suspend fun loadWidgetData(context: Context): PiecerWidgetData {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val settings = entryPoint.settingsRepository().settingsFlow.first()
        val wallets = entryPoint.walletRepository().observeAll().first()
        val transactions = entryPoint.transactionRepository().observeAll().first()
        val budgets = entryPoint.budgetRepository().observeAll().first()
        val categories = entryPoint.categoryRepository().observeAll().first()

        val now = System.currentTimeMillis()
        val effectiveTxs = transactions.filter { it.dateMillis <= now }

        val walletBalances = BalanceCalculator.calculateAllWalletBalances(wallets, effectiveTxs)
        val totalBalance = walletBalances.sumOf { it.currentBalance }

        val cyclePeriod = FinancialCycleHelper.getCyclePeriod(now, settings.cycleStartDay)
        val remainingDays = DateUtils.remainingDaysInPeriod(now, cyclePeriod.endMillis)
        val cycleTxs = effectiveTxs.filter {
            it.dateMillis in cyclePeriod.startMillis..cyclePeriod.endMillis
        }
        val monthlySummary = BalanceCalculator.calculatePeriodSummary(cycleTxs)
        val totalBudgetLimit = budgets.sumOf { it.limitAmount }
        val dailyAllowance = BudgetCalculator.calculateDailyAllowance(
            totalBudget = totalBudgetLimit,
            totalExpense = monthlySummary.totalExpense,
            totalBalance = totalBalance,
            remainingDays = remainingDays
        )

        val hideAmounts = WidgetPrivacyPolicy.shouldHideAmounts(
            isBalanceHidden = settings.isBalanceHidden,
            isPinEnabled = settings.isPinEnabled,
            biometricEnabled = settings.biometricEnabled
        )

        val categoryNames = categories.associate { it.id to it.name }
        val recent = effectiveTxs
            .sortedWith(compareByDescending<TransactionEntity> { it.dateMillis }.thenByDescending { it.createdAt })
            .take(3)
            .map { tx ->
                val title = tx.note.takeIf { it.isNotBlank() }
                    ?: categoryNames[tx.categoryId]
                    ?: if (tx.type == TransactionType.TRANSFER) "Transfer" else "Transaksi"
                val prefix = when (tx.type) {
                    TransactionType.INCOME -> "+"
                    TransactionType.EXPENSE -> "-"
                    TransactionType.TRANSFER -> ""
                }
                val amountStr = if (hideAmounts) {
                    WidgetPrivacyPolicy.MASKED_AMOUNT
                } else {
                    "$prefix${WidgetPrivacyPolicy.formatAmount(tx.amount, false)}"
                }
                WidgetTransactionItem(
                    title = title,
                    amountText = amountStr,
                    isIncome = tx.type == TransactionType.INCOME,
                    isExpense = tx.type == TransactionType.EXPENSE
                )
            }

        return PiecerWidgetData(
            dailyAllowanceText = WidgetPrivacyPolicy.formatAmount(dailyAllowance, hideAmounts),
            totalBalanceText = WidgetPrivacyPolicy.formatAmount(totalBalance, hideAmounts),
            recentTransactions = recent,
            hideAmounts = hideAmounts
        )
    }
}
