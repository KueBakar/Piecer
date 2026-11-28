package com.piecer.app.data.local

import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.WalletType

/**
 * Pure seed row builders used by [PiecerDatabase] onCreate and verified by schema unit tests.
 */
object SeedData {

    fun walletRows(nowMillis: Long): List<Map<String, Any?>> = listOf(
        mapOf(
            "name" to "Tunai",
            "type" to WalletType.CASH.name,
            "initialBalance" to 0L,
            "iconKey" to "wallet",
            "colorKey" to "ink",
            "createdAt" to nowMillis
        )
    )

    fun categoryRows(): List<Map<String, Any?>> {
        val expenseCategories = listOf(
            "Makan" to "restaurant",
            "Transport" to "directions_car",
            "Belanja" to "shopping_bag",
            "Tagihan" to "receipt_long",
            "Hiburan" to "movie",
            "Kesehatan" to "medical_services",
            "Pendidikan" to "school",
            "Lainnya" to "more_horiz"
        )
        val incomeCategories = listOf(
            "Gaji" to "payments",
            "Bonus" to "card_giftcard",
            "Hadiah" to "redeem",
            "Lainnya" to "more_horiz"
        )

        return expenseCategories.map { (name, iconKey) ->
            categoryRow(
                name = name,
                type = CategoryType.EXPENSE.name,
                iconKey = iconKey,
                colorKey = "coral"
            )
        } + incomeCategories.map { (name, iconKey) ->
            categoryRow(
                name = name,
                type = CategoryType.INCOME.name,
                iconKey = iconKey,
                colorKey = "mint"
            )
        }
    }

    fun rowsByTable(nowMillis: Long = System.currentTimeMillis()): Map<String, List<Map<String, Any?>>> =
        mapOf(
            "wallets" to walletRows(nowMillis),
            "categories" to categoryRows()
        )

    private fun categoryRow(
        name: String,
        type: String,
        iconKey: String,
        colorKey: String
    ): Map<String, Any?> = mapOf(
        "name" to name,
        "type" to type,
        "iconKey" to iconKey,
        "colorKey" to colorKey,
        "isDefault" to 1,
        "isArchived" to 0
    )
}
