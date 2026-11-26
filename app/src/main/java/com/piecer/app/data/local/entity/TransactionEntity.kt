package com.piecer.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
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
        Index(value = ["date"]),
        Index(value = ["type", "date"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Long,
    val type: TransactionType,
    val categoryId: Long? = null,
    val walletId: Long,
    val toWalletId: Long? = null,
    val date: Long,
    val note: String = "",
    val createdAt: Long
) {
    init {
        require(amount > 0L) { "Transaction amount must be positive, was $amount" }
        if (type == TransactionType.TRANSFER) {
            require(toWalletId != null && toWalletId != walletId) {
                "Transfer requires a distinct destination wallet"
            }
        }
    }
}
