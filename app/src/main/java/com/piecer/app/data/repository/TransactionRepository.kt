package com.piecer.app.data.repository

import com.piecer.app.data.local.dao.CategoryTotal
import com.piecer.app.data.local.dao.PeriodSummary
import com.piecer.app.data.local.dao.TransactionDao
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface TransactionRepository {
    fun observeAll(): Flow<List<TransactionEntity>>
    fun observeById(id: Long): Flow<TransactionEntity?>
    fun observeByDateRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>
    fun observeTotalByTypeAndDateRange(
        type: TransactionType,
        startMillis: Long,
        endMillis: Long
    ): Flow<Long>
    fun observePeriodSummary(startMillis: Long, endMillis: Long): Flow<PeriodSummary>
    fun observeTotalByCategory(
        type: TransactionType,
        startMillis: Long,
        endMillis: Long
    ): Flow<List<CategoryTotal>>
    suspend fun insert(transaction: TransactionEntity): Long
    suspend fun update(transaction: TransactionEntity)
    suspend fun delete(transaction: TransactionEntity)
    suspend fun deleteById(id: Long)
}

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {
    override fun observeAll(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    override fun observeById(id: Long): Flow<TransactionEntity?> = transactionDao.observeById(id)

    override fun observeByDateRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<TransactionEntity>> = transactionDao.observeByDateRange(startMillis, endMillis)

    override fun observeTotalByTypeAndDateRange(
        type: TransactionType,
        startMillis: Long,
        endMillis: Long
    ): Flow<Long> = transactionDao.observeTotalByTypeAndDateRange(type, startMillis, endMillis)

    override fun observePeriodSummary(startMillis: Long, endMillis: Long): Flow<PeriodSummary> =
        transactionDao.observePeriodSummary(startMillis, endMillis)

    override fun observeTotalByCategory(
        type: TransactionType,
        startMillis: Long,
        endMillis: Long
    ): Flow<List<CategoryTotal>> = transactionDao.observeTotalByCategory(type, startMillis, endMillis)

    override suspend fun insert(transaction: TransactionEntity): Long =
        transactionDao.insert(transaction)

    override suspend fun update(transaction: TransactionEntity) = transactionDao.update(transaction)

    override suspend fun delete(transaction: TransactionEntity) = transactionDao.delete(transaction)

    override suspend fun deleteById(id: Long) = transactionDao.deleteById(id)
}
