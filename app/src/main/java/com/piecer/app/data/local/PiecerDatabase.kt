package com.piecer.app.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.piecer.app.data.local.dao.CategoryDao
import com.piecer.app.data.local.dao.TransactionDao
import com.piecer.app.data.local.dao.WalletDao
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.data.local.entity.WalletType

@Database(
    entities = [
        WalletEntity::class,
        CategoryEntity::class,
        TransactionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PiecerDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DATABASE_NAME = "piecer.db"

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
                val walletValues = ContentValues().apply {
                    put("name", "Tunai")
                    put("type", WalletType.CASH.name)
                    put("initialBalance", 0L)
                    put("iconKey", "wallet")
                    put("colorKey", "ink")
                    put("createdAt", now)
                }
                db.insert("wallets", SQLiteDatabase.CONFLICT_ABORT, walletValues)

                val expenseCategories = listOf(
                    "Makan" to "restaurant",
                    "Transport" to "directions_car",
                    "Belanja" to "shopping_bag",
                    "Tagihan" to "receipt_long",
                    "Hiburan" to "movie",
                    "Kesehatan" to "medical_services",
                    "Pendidikan" to "school",
                    "Lainnya" to "more_horiz"
                )
                expenseCategories.forEach { (name, iconKey) ->
                    val values = ContentValues().apply {
                        put("name", name)
                        put("type", CategoryType.EXPENSE.name)
                        put("iconKey", iconKey)
                        put("colorKey", "coral")
                        put("isDefault", 1)
                    }
                    db.insert("categories", SQLiteDatabase.CONFLICT_ABORT, values)
                }

                val incomeCategories = listOf(
                    "Gaji" to "payments",
                    "Bonus" to "card_giftcard",
                    "Hadiah" to "redeem",
                    "Lainnya" to "more_horiz"
                )
                incomeCategories.forEach { (name, iconKey) ->
                    val values = ContentValues().apply {
                        put("name", name)
                        put("type", CategoryType.INCOME.name)
                        put("iconKey", iconKey)
                        put("colorKey", "mint")
                        put("isDefault", 1)
                    }
                    db.insert("categories", SQLiteDatabase.CONFLICT_ABORT, values)
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }
}
