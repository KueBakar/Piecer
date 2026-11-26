package com.piecer.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BudgetPeriodType {
    MONTHLY
}

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId", "periodType"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val limitAmount: Long,
    @ColumnInfo(defaultValue = "MONTHLY")
    val periodType: BudgetPeriodType = BudgetPeriodType.MONTHLY,
    val createdAt: Long
) {
    init {
        require(limitAmount > 0L) { "Budget limit must be positive, was $limitAmount" }
    }
}
