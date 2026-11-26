package com.piecer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val targetAmount: Long,
    val savedAmount: Long = 0L,
    val deadline: Long? = null,
    val iconKey: String = "savings",
    val colorKey: String = "mint",
    val createdAt: Long = System.currentTimeMillis()
)
