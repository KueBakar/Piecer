package com.piecer.app.data.repository

import androidx.room.withTransaction
import com.piecer.app.data.local.PiecerDatabase
import com.piecer.app.data.local.dao.PieceDao
import com.piecer.app.data.local.entity.PieceAllocationEntity
import com.piecer.app.data.local.entity.PieceCategoryEntity
import com.piecer.app.data.local.entity.PieceEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface PieceRepository {
    fun observeAllPieces(): Flow<List<PieceEntity>>
    suspend fun getAllPieces(): List<PieceEntity>
    fun observeAllPieceCategories(): Flow<List<PieceCategoryEntity>>
    suspend fun getAllPieceCategories(): List<PieceCategoryEntity>
    fun observeAllAllocations(): Flow<List<PieceAllocationEntity>>
    suspend fun insertPiece(piece: PieceEntity): Long
    suspend fun updatePiece(piece: PieceEntity)
    suspend fun deletePiece(piece: PieceEntity)
    suspend fun replaceSetup(pieces: List<PieceEntity>, categoryMappingsByIndex: Map<Int, List<Long>>)
    suspend fun setCategoryPiece(categoryId: Long, pieceId: Long?)
    suspend fun saveAllocationsForTransaction(transactionId: Long, allocations: List<PieceAllocationEntity>)
    suspend fun deleteAllocationsForTransaction(transactionId: Long)
}

@Singleton
class PieceRepositoryImpl @Inject constructor(
    private val database: PiecerDatabase,
    private val pieceDao: PieceDao
) : PieceRepository {
    override fun observeAllPieces(): Flow<List<PieceEntity>> = pieceDao.observeAllPieces()

    override suspend fun getAllPieces(): List<PieceEntity> = pieceDao.getAllPieces()

    override fun observeAllPieceCategories(): Flow<List<PieceCategoryEntity>> =
        pieceDao.observeAllPieceCategories()

    override suspend fun getAllPieceCategories(): List<PieceCategoryEntity> =
        pieceDao.getAllPieceCategories()

    override fun observeAllAllocations(): Flow<List<PieceAllocationEntity>> =
        pieceDao.observeAllAllocations()

    override suspend fun insertPiece(piece: PieceEntity): Long = pieceDao.insertPiece(piece)

    override suspend fun updatePiece(piece: PieceEntity) = pieceDao.updatePiece(piece)

    override suspend fun deletePiece(piece: PieceEntity) = pieceDao.deletePiece(piece)

    override suspend fun replaceSetup(
        pieces: List<PieceEntity>,
        categoryMappingsByIndex: Map<Int, List<Long>>
    ) {
        database.withTransaction {
            pieceDao.deleteAllPieceCategories()
            pieceDao.deleteAllPieces()
            val insertedIds = pieces.map { pieceDao.insertPiece(it) }
            val mappings = ArrayList<PieceCategoryEntity>()
            categoryMappingsByIndex.forEach { (index, categoryIds) ->
                val pieceId = insertedIds.getOrNull(index) ?: return@forEach
                categoryIds.forEach { catId ->
                    mappings.add(PieceCategoryEntity(pieceId = pieceId, categoryId = catId))
                }
            }
            if (mappings.isNotEmpty()) {
                pieceDao.insertAllPieceCategories(mappings)
            }
        }
    }

    override suspend fun setCategoryPiece(categoryId: Long, pieceId: Long?) {
        database.withTransaction {
            pieceDao.deletePieceCategoryByCategoryId(categoryId)
            if (pieceId != null) {
                pieceDao.insertPieceCategory(PieceCategoryEntity(pieceId = pieceId, categoryId = categoryId))
            }
        }
    }

    override suspend fun saveAllocationsForTransaction(
        transactionId: Long,
        allocations: List<PieceAllocationEntity>
    ) {
        database.withTransaction {
            pieceDao.deleteAllocationsByTransactionId(transactionId)
            if (allocations.isNotEmpty()) {
                pieceDao.insertAllocations(allocations)
            }
        }
    }

    override suspend fun deleteAllocationsForTransaction(transactionId: Long) {
        pieceDao.deleteAllocationsByTransactionId(transactionId)
    }
}
