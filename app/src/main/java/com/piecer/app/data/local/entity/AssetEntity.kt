package com.piecer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AssetType,
    val value: Long,
    val note: String = "",
    val updatedAt: Long
)
