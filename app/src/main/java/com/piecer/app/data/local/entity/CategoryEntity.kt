package com.piecer.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["type"])
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val iconKey: String,
    val colorKey: String,
    val isDefault: Boolean,
    val isArchived: Boolean = false
)
