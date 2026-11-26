package com.piecer.app.data.repository

import com.piecer.app.data.local.dao.CategoryDao
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface CategoryRepository {
    fun observeAll(): Flow<List<CategoryEntity>>
    fun observeByType(type: CategoryType): Flow<List<CategoryEntity>>
    fun observeById(id: Long): Flow<CategoryEntity?>
    suspend fun insert(category: CategoryEntity): Long
    suspend fun update(category: CategoryEntity)
    suspend fun delete(category: CategoryEntity)
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    override fun observeAll(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    override fun observeByType(type: CategoryType): Flow<List<CategoryEntity>> =
        categoryDao.observeByType(type)

    override fun observeById(id: Long): Flow<CategoryEntity?> = categoryDao.observeById(id)

    override suspend fun insert(category: CategoryEntity): Long = categoryDao.insert(category)

    override suspend fun update(category: CategoryEntity) = categoryDao.update(category)

    override suspend fun delete(category: CategoryEntity) = categoryDao.delete(category)
}
