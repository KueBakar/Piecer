package com.piecer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, id ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY isDefault DESC, id ASC")
    fun observeByType(type: CategoryType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY isDefault DESC, id ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type AND isArchived = 0 ORDER BY isDefault DESC, id ASC")
    fun observeActiveByType(type: CategoryType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun observeById(id: Long): Flow<CategoryEntity?>

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countTransactions(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM budgets WHERE categoryId = :categoryId")
    suspend fun countBudgets(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM piece_categories WHERE categoryId = :categoryId")
    suspend fun countPieceCategories(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM recurring WHERE categoryId = :categoryId")
    suspend fun countRecurring(categoryId: Long): Int

    @Query("UPDATE categories SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
