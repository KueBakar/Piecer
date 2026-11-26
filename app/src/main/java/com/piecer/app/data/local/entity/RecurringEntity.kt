package com.piecer.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["toWalletId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["walletId"]),
        Index(value = ["toWalletId"]),
        Index(value = ["nextDueDate"])
    ]
)
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    val categoryId: Long? = null,
    val walletId: Long,
    val toWalletId: Long? = null,
    val note: String = "",
    val interval: RecurringInterval,
    val dayOfMonth: Int? = null,
    val dayOfWeek: Int? = null,
    val nextDueDate: Long,
    val isActive: Boolean = true,
    val remindDaysBefore: Int = 0,
    val createdAt: Long
) {
    init {
        require(amount > 0L) { "Recurring amount must be positive, was $amount" }
        if (type == TransactionType.TRANSFER) {
            require(toWalletId != null && toWalletId != walletId) {
                "Transfer requires a distinct destination wallet"
            }
        }
    }
}
