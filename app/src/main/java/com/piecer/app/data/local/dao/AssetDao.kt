package com.piecer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.piecer.app.data.local.entity.AssetEntity
import com.piecer.app.data.local.entity.AssetHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {

    @Query("SELECT * FROM assets ORDER BY updatedAt DESC, id DESC")
    fun observeAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets ORDER BY id ASC")
    suspend fun getAllAssets(): List<AssetEntity>

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getAssetById(id: Long): AssetEntity?

    @Query("SELECT * FROM asset_history ORDER BY date ASC, id ASC")
    fun observeAllHistory(): Flow<List<AssetHistoryEntity>>

    @Query("SELECT * FROM asset_history WHERE assetId = :assetId ORDER BY date ASC, id ASC")
    fun observeHistoryByAssetId(assetId: Long): Flow<List<AssetHistoryEntity>>

    @Query("SELECT * FROM asset_history ORDER BY id ASC")
    suspend fun getAllHistory(): List<AssetHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAsset(asset: AssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllAssets(assets: List<AssetEntity>)

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHistory(history: AssetHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllHistory(history: List<AssetHistoryEntity>)

    @Query("DELETE FROM asset_history")
    suspend fun deleteAllHistory()

    @Query("DELETE FROM assets")
    suspend fun deleteAllAssets()
}
