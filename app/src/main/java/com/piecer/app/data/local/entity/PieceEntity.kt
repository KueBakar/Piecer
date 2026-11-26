package com.piecer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pieces")
data class PieceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val percent: Int,
    val colorKey: String,
    val iconKey: String,
    val sortOrder: Int,
    val createdAt: Long
) {
    init {
        require(percent in 0..100) { "Piece percent must be in 0..100, was $percent" }
    }
}
