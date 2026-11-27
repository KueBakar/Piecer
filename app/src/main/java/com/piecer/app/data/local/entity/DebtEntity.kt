package com.piecer.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debts",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["direction"]),
        Index(value = ["dueDate"]),
        Index(value = ["walletId"])
    ]
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personName: String,
    val totalAmount: Long,
    val paidAmount: Long = 0L,
    val direction: DebtDirection,
    val dueDate: Long? = null,
    val note: String = "",
    val walletId: Long? = null,
    val recordAsTransaction: Boolean = false,
    val createdAt: Long
)
