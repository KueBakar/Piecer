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
    fun observeActive(): Flow<List<CategoryEntity>>
    fun observeActiveByType(type: CategoryType): Flow<List<CategoryEntity>>
    fun observeById(id: Long): Flow<CategoryEntity?>
    suspend fun getAll(): List<CategoryEntity>
    suspend fun isCategoryInUse(categoryId: Long): Boolean
    suspend fun setArchived(id: Long, isArchived: Boolean)
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

    override fun observeActive(): Flow<List<CategoryEntity>> = categoryDao.observeActive()

    override fun observeActiveByType(type: CategoryType): Flow<List<CategoryEntity>> =
        categoryDao.observeActiveByType(type)

    override fun observeById(id: Long): Flow<CategoryEntity?> = categoryDao.observeById(id)

    override suspend fun getAll(): List<CategoryEntity> = categoryDao.getAll()

    override suspend fun isCategoryInUse(categoryId: Long): Boolean {
        return categoryDao.countTransactions(categoryId) > 0 ||
            categoryDao.countBudgets(categoryId) > 0 ||
            categoryDao.countPieceCategories(categoryId) > 0 ||
            categoryDao.countRecurring(categoryId) > 0
    }

    override suspend fun setArchived(id: Long, isArchived: Boolean) =
        categoryDao.setArchived(id, isArchived)

    override suspend fun insert(category: CategoryEntity): Long = categoryDao.insert(category)

    override suspend fun update(category: CategoryEntity) = categoryDao.update(category)

    override suspend fun delete(category: CategoryEntity) = categoryDao.delete(category)
}
