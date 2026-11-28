package com.piecer.app.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.piecer.app.data.local.dao.AssetDao
import com.piecer.app.data.local.dao.BudgetDao
import com.piecer.app.data.local.dao.CategoryDao
import com.piecer.app.data.local.dao.DebtDao
import com.piecer.app.data.local.dao.GoalDao
import com.piecer.app.data.local.dao.PieceDao
import com.piecer.app.data.local.dao.RecurringDao
import com.piecer.app.data.local.dao.TransactionDao
import com.piecer.app.data.local.dao.WalletDao
import com.piecer.app.data.local.entity.AssetEntity
import com.piecer.app.data.local.entity.AssetHistoryEntity
import com.piecer.app.data.local.entity.BudgetEntity
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.DebtEntity
import com.piecer.app.data.local.entity.DebtPaymentEntity
import com.piecer.app.data.local.entity.GoalEntity
import com.piecer.app.data.local.entity.PieceAllocationEntity
import com.piecer.app.data.local.entity.PieceCategoryEntity
import com.piecer.app.data.local.entity.PieceEntity
import com.piecer.app.data.local.entity.RecurringEntity
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.WalletEntity

@Database(
    entities = [
        WalletEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        PieceEntity::class,
        PieceCategoryEntity::class,
        PieceAllocationEntity::class,
        RecurringEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
        AssetEntity::class,
        AssetHistoryEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PiecerDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun pieceDao(): PieceDao
    abstract fun recurringDao(): RecurringDao
    abstract fun debtDao(): DebtDao
    abstract fun assetDao(): AssetDao

    companion object {
        const val DATABASE_NAME = "piecer.db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `limitAmount` INTEGER NOT NULL,
                        `periodType` TEXT NOT NULL DEFAULT 'MONTHLY',
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_categoryId_periodType` ON `budgets` (`categoryId`, `periodType`)"
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `targetAmount` INTEGER NOT NULL,
                        `savedAmount` INTEGER NOT NULL,
                        `deadline` INTEGER,
                        `iconKey` TEXT NOT NULL,
                        `colorKey` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pieces` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `percent` INTEGER NOT NULL,
                        `colorKey` TEXT NOT NULL,
                        `iconKey` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `piece_categories` (
                        `pieceId` INTEGER NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        PRIMARY KEY(`pieceId`, `categoryId`),
                        FOREIGN KEY(`pieceId`) REFERENCES `pieces`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_piece_categories_pieceId` ON `piece_categories` (`pieceId`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_piece_categories_categoryId` ON `piece_categories` (`categoryId`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `piece_allocations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `pieceId` INTEGER NOT NULL,
                        `transactionId` INTEGER NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`pieceId`) REFERENCES `pieces`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_piece_allocations_pieceId` ON `piece_allocations` (`pieceId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_piece_allocations_transactionId` ON `piece_allocations` (`transactionId`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `categoryId` INTEGER,
                        `walletId` INTEGER NOT NULL,
                        `toWalletId` INTEGER,
                        `note` TEXT NOT NULL,
                        `interval` TEXT NOT NULL,
                        `dayOfMonth` INTEGER,
                        `dayOfWeek` INTEGER,
                        `nextDueDate` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `remindDaysBefore` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`walletId`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`toWalletId`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recurring_categoryId` ON `recurring` (`categoryId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recurring_walletId` ON `recurring` (`walletId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recurring_toWalletId` ON `recurring` (`toWalletId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recurring_nextDueDate` ON `recurring` (`nextDueDate`)"
                )
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `categories` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `debts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `personName` TEXT NOT NULL,
                        `totalAmount` INTEGER NOT NULL,
                        `paidAmount` INTEGER NOT NULL,
                        `direction` TEXT NOT NULL,
                        `dueDate` INTEGER,
                        `note` TEXT NOT NULL,
                        `walletId` INTEGER,
                        `recordAsTransaction` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`walletId`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_direction` ON `debts` (`direction`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_dueDate` ON `debts` (`dueDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_walletId` ON `debts` (`walletId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `debt_payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `debtId` INTEGER NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `walletId` INTEGER,
                        `note` TEXT NOT NULL,
                        FOREIGN KEY(`debtId`) REFERENCES `debts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`walletId`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_debtId` ON `debt_payments` (`debtId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_walletId` ON `debt_payments` (`walletId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `assets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `value` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `asset_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `assetId` INTEGER NOT NULL,
                        `value` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        FOREIGN KEY(`assetId`) REFERENCES `assets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_asset_history_assetId` ON `asset_history` (`assetId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_asset_history_date` ON `asset_history` (`date`)")
            }
        }

        fun createSeedCallback(): Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedInitialData(db)
            }
        }

        private fun seedInitialData(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            db.beginTransaction()
            try {
                SeedData.rowsByTable(now).forEach { (tableName, rows) ->
                    rows.forEach { row ->
                        db.insert(tableName, SQLiteDatabase.CONFLICT_ABORT, row.toContentValues())
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        private fun Map<String, Any?>.toContentValues(): ContentValues = ContentValues().apply {
            forEach { (key, value) ->
                when (value) {
                    null -> putNull(key)
                    is String -> put(key, value)
                    is Int -> put(key, value)
                    is Long -> put(key, value)
                    is Boolean -> put(key, if (value) 1 else 0)
                    is Double -> put(key, value)
                    is Float -> put(key, value)
                    else -> throw IllegalArgumentException("Unsupported seed value type for $key: ${value::class.java}")
                }
            }
        }
    }
}
