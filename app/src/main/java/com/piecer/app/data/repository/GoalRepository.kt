package com.piecer.app.data.repository

import com.piecer.app.data.local.dao.GoalDao
import com.piecer.app.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GoalRepository {
    fun observeAll(): Flow<List<GoalEntity>>
    suspend fun getAll(): List<GoalEntity>
    suspend fun getById(id: Long): GoalEntity?
    suspend fun insert(goal: GoalEntity): Long
    suspend fun update(goal: GoalEntity)
    suspend fun delete(goal: GoalEntity)
    suspend fun addSavings(goalId: Long, amount: Long)
}

class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalRepository {
    override fun observeAll(): Flow<List<GoalEntity>> = goalDao.observeAll()

    override suspend fun getAll(): List<GoalEntity> = goalDao.getAll()

    override suspend fun getById(id: Long): GoalEntity? = goalDao.getById(id)

    override suspend fun insert(goal: GoalEntity): Long = goalDao.insert(goal)

    override suspend fun update(goal: GoalEntity) = goalDao.update(goal)

    override suspend fun delete(goal: GoalEntity) = goalDao.delete(goal)

    override suspend fun addSavings(goalId: Long, amount: Long) {
        if (amount > 0L) {
            goalDao.addSavings(goalId, amount)
        }
    }
}
