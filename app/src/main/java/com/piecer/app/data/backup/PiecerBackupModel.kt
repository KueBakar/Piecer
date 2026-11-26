package com.piecer.app.data.backup

import com.piecer.app.data.local.entity.BudgetEntity
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.GoalEntity
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.data.preferences.ThemeMode

data class BackupSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val cycleStartDay: Int = 1,
    val isBalanceHidden: Boolean = false,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 20,
    val dailyReminderMinute: Int = 0,
    val blockScreenshots: Boolean = false,
    val reduceMotion: Boolean = false
)

data class PiecerBackup(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long,
    val wallets: List<WalletEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val goals: List<GoalEntity>,
    val settings: BackupSettings
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
