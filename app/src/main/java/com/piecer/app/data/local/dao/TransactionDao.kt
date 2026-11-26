package com.piecer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

data class PeriodSummary(
    val totalIncome: Long,
    val totalExpense: Long
)

data class CategoryTotal(
    val categoryId: Long,
    val categoryName: String,
    val iconKey: String,
    val colorKey: String,
    val totalAmount: Long
)

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC, id DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query(
        """
        SELECT * FROM transactions
        WHERE date BETWEEN :startMillis AND :endMillis
        ORDER BY date DESC, createdAt DESC, id DESC
        """
    )
    fun observeByDateRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = :type AND date BETWEEN :startMillis AND :endMillis
        """
    )
    fun observeTotalByTypeAndDateRange(
        type: TransactionType,
        startMillis: Long,
        endMillis: Long
    ): Flow<Long>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS totalIncome,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS totalExpense
        FROM transactions
        WHERE date BETWEEN :startMillis AND :endMillis
        """
    )
    fun observePeriodSummary(startMillis: Long, endMillis: Long): Flow<PeriodSummary>

    @Query(
        """
        SELECT
            c.id AS categoryId,
            c.name AS categoryName,
            c.iconKey AS iconKey,
            c.colorKey AS colorKey,
            COALESCE(SUM(t.amount), 0) AS totalAmount
        FROM categories c
        INNER JOIN transactions t ON t.categoryId = c.id
        WHERE t.type = :type AND t.date BETWEEN :startMillis AND :endMillis
        GROUP BY c.id
        ORDER BY totalAmount DESC
        """
    )
    fun observeTotalByCategory(
        type: TransactionType,
        startMillis: Long,
        endMillis: Long
    ): Flow<List<CategoryTotal>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE walletId = :walletId OR toWalletId = :walletId")
    suspend fun deleteByWalletId(walletId: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
