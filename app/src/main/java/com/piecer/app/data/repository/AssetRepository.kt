package com.piecer.app.data.repository

import androidx.room.withTransaction
import com.piecer.app.data.local.PiecerDatabase
import com.piecer.app.data.local.dao.AssetDao
import com.piecer.app.data.local.entity.AssetEntity
import com.piecer.app.data.local.entity.AssetHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface AssetRepository {
    fun observeAllAssets(): Flow<List<AssetEntity>>
    fun observeAllHistory(): Flow<List<AssetHistoryEntity>>
    fun observeHistoryByAssetId(assetId: Long): Flow<List<AssetHistoryEntity>>
    suspend fun createAsset(asset: AssetEntity): Long
    suspend fun updateAssetDetails(asset: AssetEntity)
    suspend fun updateAssetValue(assetId: Long, newValue: Long, nowMillis: Long = System.currentTimeMillis())
    suspend fun deleteAsset(asset: AssetEntity)
}

@Singleton
class AssetRepositoryImpl @Inject constructor(
    private val database: PiecerDatabase,
    private val assetDao: AssetDao
) : AssetRepository {

    override fun observeAllAssets(): Flow<List<AssetEntity>> = assetDao.observeAllAssets()

    override fun observeAllHistory(): Flow<List<AssetHistoryEntity>> = assetDao.observeAllHistory()

    override fun observeHistoryByAssetId(assetId: Long): Flow<List<AssetHistoryEntity>> =
        assetDao.observeHistoryByAssetId(assetId)

    override suspend fun createAsset(asset: AssetEntity): Long {
        return database.withTransaction {
            val id = assetDao.insertAsset(asset)
            assetDao.insertHistory(
                AssetHistoryEntity(
                    assetId = id,
                    value = asset.value,
                    date = asset.updatedAt
                )
            )
            id
        }
    }

    override suspend fun updateAssetDetails(asset: AssetEntity) {
        database.withTransaction {
            val existing = assetDao.getAssetById(asset.id)
            assetDao.updateAsset(asset)
            if (existing == null || existing.value != asset.value) {
                assetDao.insertHistory(
                    AssetHistoryEntity(
                        assetId = asset.id,
                        value = asset.value,
                        date = asset.updatedAt
                    )
                )
            }
        }
    }

    override suspend fun updateAssetValue(assetId: Long, newValue: Long, nowMillis: Long) {
        database.withTransaction {
            val existing = assetDao.getAssetById(assetId) ?: return@withTransaction
            assetDao.updateAsset(existing.copy(value = newValue, updatedAt = nowMillis))
            assetDao.insertHistory(
                AssetHistoryEntity(
                    assetId = assetId,
                    value = newValue,
                    date = nowMillis
                )
            )
        }
    }

    override suspend fun deleteAsset(asset: AssetEntity) = assetDao.deleteAsset(asset)
}
