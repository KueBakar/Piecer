package com.piecer.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "piece_allocations",
    foreignKeys = [
        ForeignKey(
            entity = PieceEntity::class,
            parentColumns = ["id"],
            childColumns = ["pieceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["pieceId"]),
        Index(value = ["transactionId"])
    ]
)
data class PieceAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pieceId: Long,
    val transactionId: Long,
    val amount: Long,
    val createdAt: Long
)
