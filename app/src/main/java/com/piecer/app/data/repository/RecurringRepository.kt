package com.piecer.app.data.repository

import com.piecer.app.data.local.dao.RecurringDao
import com.piecer.app.data.local.entity.RecurringEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface RecurringRepository {
    fun observeAll(): Flow<List<RecurringEntity>>
    suspend fun getAll(): List<RecurringEntity>
    suspend fun getDueActive(upToMillis: Long): List<RecurringEntity>
    suspend fun getAllActive(): List<RecurringEntity>
    suspend fun insert(item: RecurringEntity): Long
    suspend fun update(item: RecurringEntity)
    suspend fun delete(item: RecurringEntity)
    suspend fun deleteById(id: Long)
}

@Singleton
class RecurringRepositoryImpl @Inject constructor(
    private val recurringDao: RecurringDao
) : RecurringRepository {
    override fun observeAll(): Flow<List<RecurringEntity>> = recurringDao.observeAll()

    override suspend fun getAll(): List<RecurringEntity> = recurringDao.getAll()

    override suspend fun getDueActive(upToMillis: Long): List<RecurringEntity> =
        recurringDao.getDueActive(upToMillis)

    override suspend fun getAllActive(): List<RecurringEntity> = recurringDao.getAllActive()

    override suspend fun insert(item: RecurringEntity): Long = recurringDao.insert(item)

    override suspend fun update(item: RecurringEntity) = recurringDao.update(item)

    override suspend fun delete(item: RecurringEntity) = recurringDao.delete(item)

    override suspend fun deleteById(id: Long) = recurringDao.deleteById(id)
}
