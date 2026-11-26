package com.piecer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.data.local.entity.WalletType
import kotlinx.coroutines.flow.Flow

data class WalletWithBalance(
    val walletId: Long,
    val name: String,
    val type: WalletType,
    val initialBalance: Long,
    val iconKey: String,
    val colorKey: String,
    val createdAt: Long,
    val currentBalance: Long
)

@Dao
interface WalletDao {

    @Query("SELECT * FROM wallets ORDER BY createdAt ASC, id ASC")
    fun observeAll(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets ORDER BY createdAt ASC, id ASC")
    suspend fun getAll(): List<WalletEntity>

    @Query("SELECT * FROM wallets WHERE id = :id")
    fun observeById(id: Long): Flow<WalletEntity?>

    @Query(
        """
        SELECT
            w.id AS walletId,
            w.name AS name,
            w.type AS type,
            w.initialBalance AS initialBalance,
            w.iconKey AS iconKey,
            w.colorKey AS colorKey,
            w.createdAt AS createdAt,
            w.initialBalance + COALESCE((
                SELECT SUM(
                    CASE
                        WHEN t.type = 'INCOME' AND t.walletId = w.id THEN t.amount
                        WHEN t.type = 'EXPENSE' AND t.walletId = w.id THEN -t.amount
                        WHEN t.type = 'TRANSFER' AND t.walletId = w.id THEN -t.amount
                        WHEN t.type = 'TRANSFER' AND t.toWalletId = w.id THEN t.amount
                        ELSE 0
                    END
                )
                FROM transactions t
                WHERE t.walletId = w.id OR t.toWalletId = w.id
            ), 0) AS currentBalance
        FROM wallets w
        ORDER BY w.createdAt ASC, w.id ASC
        """
    )
    fun observeWalletsWithBalance(): Flow<List<WalletWithBalance>>

    @Query(
        """
        SELECT
            w.id AS walletId,
            w.name AS name,
            w.type AS type,
            w.initialBalance AS initialBalance,
            w.iconKey AS iconKey,
            w.colorKey AS colorKey,
            w.createdAt AS createdAt,
            w.initialBalance + COALESCE((
                SELECT SUM(
                    CASE
                        WHEN t.type = 'INCOME' AND t.walletId = w.id THEN t.amount
                        WHEN t.type = 'EXPENSE' AND t.walletId = w.id THEN -t.amount
                        WHEN t.type = 'TRANSFER' AND t.walletId = w.id THEN -t.amount
                        WHEN t.type = 'TRANSFER' AND t.toWalletId = w.id THEN t.amount
                        ELSE 0
                    END
                )
                FROM transactions t
                WHERE t.walletId = w.id OR t.toWalletId = w.id
            ), 0) AS currentBalance
        FROM wallets w
        WHERE w.id = :walletId
        """
    )
    fun observeWalletWithBalance(walletId: Long): Flow<WalletWithBalance?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(wallet: WalletEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wallets: List<WalletEntity>)

    @Update
    suspend fun update(wallet: WalletEntity)

    @Delete
    suspend fun delete(wallet: WalletEntity)

    @Query("DELETE FROM wallets")
    suspend fun deleteAll()
}
