package com.piecer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.piecer.app.data.local.entity.PieceAllocationEntity
import com.piecer.app.data.local.entity.PieceCategoryEntity
import com.piecer.app.data.local.entity.PieceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PieceDao {

    @Query("SELECT * FROM pieces ORDER BY sortOrder ASC, id ASC")
    fun observeAllPieces(): Flow<List<PieceEntity>>

    @Query("SELECT * FROM pieces ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllPieces(): List<PieceEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPiece(piece: PieceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPieces(pieces: List<PieceEntity>)

    @Update
    suspend fun updatePiece(piece: PieceEntity)

    @Delete
    suspend fun deletePiece(piece: PieceEntity)

    @Query("DELETE FROM pieces")
    suspend fun deleteAllPieces()

    @Query("SELECT * FROM piece_categories")
    fun observeAllPieceCategories(): Flow<List<PieceCategoryEntity>>

    @Query("SELECT * FROM piece_categories")
    suspend fun getAllPieceCategories(): List<PieceCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPieceCategory(mapping: PieceCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPieceCategories(mappings: List<PieceCategoryEntity>)

    @Query("DELETE FROM piece_categories WHERE categoryId = :categoryId")
    suspend fun deletePieceCategoryByCategoryId(categoryId: Long)

    @Query("DELETE FROM piece_categories WHERE pieceId = :pieceId")
    suspend fun deletePieceCategoriesByPieceId(pieceId: Long)

    @Query("DELETE FROM piece_categories")
    suspend fun deleteAllPieceCategories()

    @Query("SELECT * FROM piece_allocations ORDER BY createdAt DESC, id DESC")
    fun observeAllAllocations(): Flow<List<PieceAllocationEntity>>

    @Query("SELECT * FROM piece_allocations ORDER BY createdAt DESC, id DESC")
    suspend fun getAllAllocations(): List<PieceAllocationEntity>

    @Query("SELECT * FROM piece_allocations WHERE transactionId = :transactionId")
    suspend fun getAllocationsByTransactionId(transactionId: Long): List<PieceAllocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocations(allocations: List<PieceAllocationEntity>)

    @Query("DELETE FROM piece_allocations WHERE transactionId = :transactionId")
    suspend fun deleteAllocationsByTransactionId(transactionId: Long)

    @Query("DELETE FROM piece_allocations")
    suspend fun deleteAllAllocations()
}
