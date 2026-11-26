package com.piecer.app.data.repository

import com.piecer.app.data.local.dao.BudgetDao
import com.piecer.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface BudgetRepository {
    fun observeAll(): Flow<List<BudgetEntity>>
    suspend fun getAll(): List<BudgetEntity>
    fun observeById(id: Long): Flow<BudgetEntity?>
    suspend fun insert(budget: BudgetEntity): Long
    suspend fun update(budget: BudgetEntity)
    suspend fun delete(budget: BudgetEntity)
    suspend fun deleteById(id: Long)
}

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {
    override fun observeAll(): Flow<List<BudgetEntity>> = budgetDao.observeAll()

    override suspend fun getAll(): List<BudgetEntity> = budgetDao.getAll()

    override fun observeById(id: Long): Flow<BudgetEntity?> = budgetDao.observeById(id)

    override suspend fun insert(budget: BudgetEntity): Long = budgetDao.insert(budget)

    override suspend fun update(budget: BudgetEntity) = budgetDao.update(budget)

    override suspend fun delete(budget: BudgetEntity) = budgetDao.delete(budget)

    override suspend fun deleteById(id: Long) = budgetDao.deleteById(id)
}
