package com.piecer.app.util

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.data.local.entity.WalletType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CsvExporterTest {

    private val zone = ZoneId.of("Asia/Jakarta")

    @Test
    fun `exportTransactions filters range and formats integer amounts`() {
        val wallets = mapOf(
            1L to WalletEntity(1L, "Tunai", WalletType.CASH, 0L, "wallet", "ink", 0L)
        )
        val categories = mapOf(
            1L to CategoryEntity(1L, "Makan", CategoryType.EXPENSE, "restaurant", "coral", true)
        )
        val inRangeDate = LocalDate.of(2026, 5, 10).atStartOfDay(zone).toInstant().toEpochMilli()
        val outOfRangeDate = LocalDate.of(2026, 4, 10).atStartOfDay(zone).toInstant().toEpochMilli()

        val txs = listOf(
            TransactionEntity(
                id = 1L,
                amount = 125000L,
                type = TransactionType.EXPENSE,
                categoryId = 1L,
                walletId = 1L,
                toWalletId = null,
                date = inRangeDate,
                note = "Makan, siang",
                createdAt = inRangeDate
            ),
            TransactionEntity(
                id = 2L,
                amount = 99999L,
                type = TransactionType.EXPENSE,
                categoryId = 1L,
                walletId = 1L,
                toWalletId = null,
                date = outOfRangeDate,
                note = "Bulan lalu",
                createdAt = outOfRangeDate
            )
        )

        val start = LocalDate.of(2026, 5, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.of(2026, 5, 31).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L

        val csv = CsvExporter.exportTransactions(txs, wallets, categories, start, end, zone)

        assertTrue(csv.contains("2026-05-10,Pengeluaran,Makan,Tunai,,125000,\"Makan, siang\""))
        assertFalse(csv.contains("99999"))
    }
}
