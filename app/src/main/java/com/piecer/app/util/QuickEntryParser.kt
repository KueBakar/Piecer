package com.piecer.app.util

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.util.quickentry.QuickEntryContext
import com.piecer.app.util.quickentry.QuickEntryPipeline
import java.time.ZoneId

data class QuickEntryResult(
    val type: TransactionType,
    val amount: Long?,
    val categoryId: Long?,
    val walletId: Long?,
    val toWalletId: Long?,
    val dateMillis: Long?,
    val note: String
)

/**
 * Pembungkus kompatibilitas untuk pemanggil lama [QuickEntryParser.parse].
 * Logika inti kini dijalankan oleh [QuickEntryPipeline].
 */
object QuickEntryParser {

    fun parse(
        input: String,
        categories: List<CategoryEntity>,
        wallets: List<WalletEntity>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): QuickEntryResult {
        val result = QuickEntryPipeline.parse(
            input = input,
            context = QuickEntryContext(
                categories = categories,
                wallets = wallets,
                nowMillis = nowMillis,
                zoneId = zoneId
            )
        )
        val first = result.entries.firstOrNull()
            ?: return QuickEntryResult(
                type = TransactionType.EXPENSE,
                amount = null,
                categoryId = null,
                walletId = null,
                toWalletId = null,
                dateMillis = null,
                note = ""
            )
        return QuickEntryResult(
            type = first.type,
            amount = first.amount,
            categoryId = first.categoryId,
            walletId = first.walletId,
            toWalletId = first.toWalletId,
            dateMillis = first.dateMillis,
            note = first.note
        )
    }
}
