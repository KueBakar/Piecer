package com.piecer.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "asset_history",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["assetId"]),
        Index(value = ["date"])
    ]
)
data class AssetHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assetId: Long,
    val value: Long,
    val date: Long
)
