package com.piecer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.piecer.app.data.local.entity.RecurringEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {

    @Query("SELECT * FROM recurring ORDER BY isActive DESC, nextDueDate ASC, id ASC")
    fun observeAll(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring ORDER BY id ASC")
    suspend fun getAll(): List<RecurringEntity>

    @Query("SELECT * FROM recurring WHERE isActive = 1 AND nextDueDate <= :upToMillis ORDER BY nextDueDate ASC, id ASC")
    suspend fun getDueActive(upToMillis: Long): List<RecurringEntity>

    @Query("SELECT * FROM recurring WHERE isActive = 1 ORDER BY nextDueDate ASC, id ASC")
    suspend fun getAllActive(): List<RecurringEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: RecurringEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecurringEntity>)

    @Update
    suspend fun update(item: RecurringEntity)

    @Delete
    suspend fun delete(item: RecurringEntity)

    @Query("DELETE FROM recurring WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recurring")
    suspend fun deleteAll()
}
