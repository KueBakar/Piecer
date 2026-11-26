package com.piecer.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "piece_categories",
    primaryKeys = ["pieceId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = PieceEntity::class,
            parentColumns = ["id"],
            childColumns = ["pieceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["pieceId"]),
        Index(value = ["categoryId"], unique = true)
    ]
)
data class PieceCategoryEntity(
    val pieceId: Long,
    val categoryId: Long
)
