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
        val pieces = database.pieceDao().getAllPieces()
        val pieceCategories = database.pieceDao().getAllPieceCategories()
        val pieceAllocations = database.pieceDao().getAllAllocations()
        val recurring = database.recurringDao().getAll()
        val appSettings = settingsRepository.getSettings()

        return PiecerBackup(
            version = PiecerBackup.CURRENT_VERSION,
            exportedAt = nowMillis,
            wallets = wallets,
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            goals = goals,
            pieces = pieces,
            pieceCategories = pieceCategories,
            pieceAllocations = pieceAllocations,
            recurring = recurring,
            settings = BackupSettings(
                themeMode = appSettings.themeMode,
                cycleStartDay = appSettings.cycleStartDay,
                isBalanceHidden = appSettings.isBalanceHidden,
                dailyReminderEnabled = appSettings.dailyReminderEnabled,
                dailyReminderHour = appSettings.dailyReminderHour,
                dailyReminderMinute = appSettings.dailyReminderMinute,
                blockScreenshots = appSettings.blockScreenshots,
                reduceMotion = appSettings.reduceMotion,
                piecesEnabled = appSettings.piecesEnabled
            )
        )
    }

    suspend fun restoreBackup(backup: PiecerBackup) {
        database.withTransaction {
            database.pieceDao().deleteAllAllocations()
            database.pieceDao().deleteAllPieceCategories()
            database.pieceDao().deleteAllPieces()
            database.recurringDao().deleteAll()
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
            database.pieceDao().insertAllPieces(backup.pieces)
            database.pieceDao().insertAllPieceCategories(backup.pieceCategories)
            database.pieceDao().insertAllocations(backup.pieceAllocations)
            database.recurringDao().insertAll(backup.recurring)
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
            reduceMotion = s.reduceMotion,
            piecesEnabled = s.piecesEnabled
        )
    }
}
