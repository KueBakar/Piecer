package com.piecer.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debt_payments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["debtId"]),
        Index(value = ["walletId"])
    ]
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val debtId: Long,
    val amount: Long,
    val date: Long,
    val walletId: Long? = null,
    val note: String = ""
)
