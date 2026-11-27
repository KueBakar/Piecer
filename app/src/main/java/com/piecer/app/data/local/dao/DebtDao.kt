package com.piecer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.piecer.app.data.local.entity.DebtEntity
import com.piecer.app.data.local.entity.DebtPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Query("SELECT * FROM debts ORDER BY CASE WHEN paidAmount >= totalAmount THEN 1 ELSE 0 END ASC, CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END ASC, dueDate ASC, createdAt DESC")
    fun observeAllDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts ORDER BY id ASC")
    suspend fun getAllDebts(): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: Long): DebtEntity?

    @Query("SELECT * FROM debts WHERE paidAmount < totalAmount AND dueDate IS NOT NULL")
    suspend fun getUnpaidDebtsWithDueDate(): List<DebtEntity>

    @Query("SELECT * FROM debt_payments ORDER BY date DESC, id DESC")
    fun observeAllPayments(): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY date DESC, id DESC")
    fun observePaymentsByDebtId(debtId: Long): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments ORDER BY id ASC")
    suspend fun getAllPayments(): List<DebtPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllDebts(debts: List<DebtEntity>)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayment(payment: DebtPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllPayments(payments: List<DebtPaymentEntity>)

    @Query("DELETE FROM debt_payments")
    suspend fun deleteAllPayments()

    @Query("DELETE FROM debts")
    suspend fun deleteAllDebts()
}
