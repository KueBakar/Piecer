package com.piecer.app.data.repository

import androidx.room.withTransaction
import com.piecer.app.data.local.PiecerDatabase
import com.piecer.app.data.local.dao.DebtDao
import com.piecer.app.data.local.entity.DebtDirection
import com.piecer.app.data.local.entity.DebtEntity
import com.piecer.app.data.local.entity.DebtPaymentEntity
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface DebtRepository {
    fun observeAllDebts(): Flow<List<DebtEntity>>
    fun observeAllPayments(): Flow<List<DebtPaymentEntity>>
    fun observePaymentsByDebtId(debtId: Long): Flow<List<DebtPaymentEntity>>
    suspend fun getDebtById(id: Long): DebtEntity?
    suspend fun getUnpaidDebtsWithDueDate(): List<DebtEntity>
    suspend fun createDebt(debt: DebtEntity, initialRecordAsTransaction: Boolean): Long
    suspend fun updateDebt(debt: DebtEntity)
    suspend fun deleteDebt(debt: DebtEntity)
    suspend fun addPayment(
        debtId: Long,
        amount: Long,
        date: Long,
        walletId: Long?,
        note: String,
        recordAsTransaction: Boolean
    )
    suspend fun markAsPaid(debt: DebtEntity, date: Long, walletId: Long?, recordAsTransaction: Boolean)
}

@Singleton
class DebtRepositoryImpl @Inject constructor(
    private val database: PiecerDatabase,
    private val debtDao: DebtDao
) : DebtRepository {

    override fun observeAllDebts(): Flow<List<DebtEntity>> = debtDao.observeAllDebts()

    override fun observeAllPayments(): Flow<List<DebtPaymentEntity>> = debtDao.observeAllPayments()

    override fun observePaymentsByDebtId(debtId: Long): Flow<List<DebtPaymentEntity>> =
        debtDao.observePaymentsByDebtId(debtId)

    override suspend fun getDebtById(id: Long): DebtEntity? = debtDao.getDebtById(id)

    override suspend fun getUnpaidDebtsWithDueDate(): List<DebtEntity> =
        debtDao.getUnpaidDebtsWithDueDate()

    override suspend fun createDebt(debt: DebtEntity, initialRecordAsTransaction: Boolean): Long {
        return database.withTransaction {
            val id = debtDao.insertDebt(debt)
            val wId = debt.walletId
            if (initialRecordAsTransaction && wId != null && debt.totalAmount > 0L) {
                val txType = when (debt.direction) {
                    DebtDirection.I_OWE -> TransactionType.INCOME // pinjam uang masuk ke dompet
                    DebtDirection.OWE_ME -> TransactionType.EXPENSE // meminjamkan uang keluar dari dompet
                }
                val prefix = when (debt.direction) {
                    DebtDirection.I_OWE -> "Pinjam dari ${debt.personName}"
                    DebtDirection.OWE_ME -> "Pinjamkan ke ${debt.personName}"
                }
                val fullNote = if (debt.note.isBlank()) prefix else "$prefix (${debt.note})"
                database.transactionDao().insert(
                    TransactionEntity(
                        amount = debt.totalAmount,
                        type = txType,
                        categoryId = null,
                        walletId = wId,
                        toWalletId = null,
                        date = debt.createdAt,
                        note = fullNote,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            id
        }
    }

    override suspend fun updateDebt(debt: DebtEntity) = debtDao.updateDebt(debt)

    override suspend fun deleteDebt(debt: DebtEntity) = debtDao.deleteDebt(debt)

    override suspend fun addPayment(
        debtId: Long,
        amount: Long,
        date: Long,
        walletId: Long?,
        note: String,
        recordAsTransaction: Boolean
    ) {
        if (amount <= 0L) return
        database.withTransaction {
            val debt = debtDao.getDebtById(debtId) ?: return@withTransaction
            val remaining = (debt.totalAmount - debt.paidAmount).coerceAtLeast(0L)
            val applied = amount.coerceAtMost(remaining)
            if (applied <= 0L) return@withTransaction

            debtDao.insertPayment(
                DebtPaymentEntity(
                    debtId = debtId,
                    amount = applied,
                    date = date,
                    walletId = walletId,
                    note = note
                )
            )
            debtDao.updateDebt(debt.copy(paidAmount = debt.paidAmount + applied))

            if (recordAsTransaction && walletId != null) {
                val txType = when (debt.direction) {
                    DebtDirection.OWE_ME -> TransactionType.INCOME // terima cicilan piutang masuk dompet
                    DebtDirection.I_OWE -> TransactionType.EXPENSE // bayar cicilan hutang keluar dompet
                }
                val prefix = when (debt.direction) {
                    DebtDirection.OWE_ME -> "Cicilan piutang ${debt.personName}"
                    DebtDirection.I_OWE -> "Bayar hutang ke ${debt.personName}"
                }
                val fullNote = if (note.isBlank()) prefix else "$prefix ($note)"
                database.transactionDao().insert(
                    TransactionEntity(
                        amount = applied,
                        type = txType,
                        categoryId = null,
                        walletId = walletId,
                        toWalletId = null,
                        date = date,
                        note = fullNote,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    override suspend fun markAsPaid(
        debt: DebtEntity,
        date: Long,
        walletId: Long?,
        recordAsTransaction: Boolean
    ) {
        val remaining = (debt.totalAmount - debt.paidAmount).coerceAtLeast(0L)
        if (remaining > 0L) {
            addPayment(
                debtId = debt.id,
                amount = remaining,
                date = date,
                walletId = walletId,
                note = "Pelunasan",
                recordAsTransaction = recordAsTransaction
            )
        }
    }
}
