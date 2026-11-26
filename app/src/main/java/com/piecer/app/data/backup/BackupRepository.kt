package com.piecer.app.data.backup

import androidx.room.withTransaction
import com.piecer.app.data.local.PiecerDatabase
import com.piecer.app.data.preferences.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val database: PiecerDatabase,
    private val settingsRepository: SettingsRepository
) {
    suspend fun createBackup(nowMillis: Long = System.currentTimeMillis()): PiecerBackup {
        val wallets = database.walletDao().getAll()
        val categories = database.categoryDao().getAll()
        val transactions = database.transactionDao().getAll()
        val budgets = database.budgetDao().getAll()
        val goals = database.goalDao().getAll()
        val appSettings = settingsRepository.getSettings()

        return PiecerBackup(
            version = PiecerBackup.CURRENT_VERSION,
            exportedAt = nowMillis,
            wallets = wallets,
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            goals = goals,
            settings = BackupSettings(
                themeMode = appSettings.themeMode,
                cycleStartDay = appSettings.cycleStartDay,
                isBalanceHidden = appSettings.isBalanceHidden,
                dailyReminderEnabled = appSettings.dailyReminderEnabled,
                dailyReminderHour = appSettings.dailyReminderHour,
                dailyReminderMinute = appSettings.dailyReminderMinute,
                blockScreenshots = appSettings.blockScreenshots,
                reduceMotion = appSettings.reduceMotion
            )
        )
    }

    suspend fun restoreBackup(backup: PiecerBackup) {
        database.withTransaction {
            database.transactionDao().deleteAll()
            database.budgetDao().deleteAll()
            database.goalDao().deleteAll()
            database.categoryDao().deleteAll()
            database.walletDao().deleteAll()

            database.walletDao().insertAll(backup.wallets)
            database.categoryDao().insertAll(backup.categories)
            database.transactionDao().insertAll(backup.transactions)
            database.budgetDao().insertAll(backup.budgets)
            database.goalDao().insertAll(backup.goals)
        }

        val s = backup.settings
        settingsRepository.applyBackupPreferences(
            themeMode = s.themeMode,
            cycleStartDay = s.cycleStartDay,
            isBalanceHidden = s.isBalanceHidden,
            dailyReminderEnabled = s.dailyReminderEnabled,
            dailyReminderHour = s.dailyReminderHour,
            dailyReminderMinute = s.dailyReminderMinute,
            blockScreenshots = s.blockScreenshots,
            reduceMotion = s.reduceMotion
        )
    }
}
