package com.piecer.app.util

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvExporter {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun exportTransactions(
        transactions: List<TransactionEntity>,
        wallets: Map<Long, WalletEntity>,
        categories: Map<Long, CategoryEntity>,
        startMillis: Long,
        endMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine("Tanggal,Tipe,Kategori,Dompet,Dompet Tujuan,Nominal,Catatan")

        transactions
            .asSequence()
            .filter { it.date in startMillis..endMillis }
            .sortedByDescending { it.date }
            .forEach { tx ->
                val dateStr = Instant.ofEpochMilli(tx.date)
                    .atZone(zoneId)
                    .toLocalDate()
                    .format(dateFormatter)
                val typeStr = when (tx.type) {
                    TransactionType.INCOME -> "Pemasukan"
                    TransactionType.EXPENSE -> "Pengeluaran"
                    TransactionType.TRANSFER -> "Transfer"
                }
                val categoryName = tx.categoryId?.let { categories[it]?.name }.orEmpty()
                val walletName = wallets[tx.walletId]?.name.orEmpty()
                val toWalletName = tx.toWalletId?.let { wallets[it]?.name }.orEmpty()
                val note = tx.note.orEmpty()

                sb.append(escapeCsv(dateStr)).append(',')
                sb.append(escapeCsv(typeStr)).append(',')
                sb.append(escapeCsv(categoryName)).append(',')
                sb.append(escapeCsv(walletName)).append(',')
                sb.append(escapeCsv(toWalletName)).append(',')
                sb.append(tx.amount.toString()).append(',')
                sb.appendLine(escapeCsv(note))
            }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }
}
