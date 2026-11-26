package com.piecer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: WalletType,
    val initialBalance: Long,
    val iconKey: String,
    val colorKey: String,
    val createdAt: Long
)
