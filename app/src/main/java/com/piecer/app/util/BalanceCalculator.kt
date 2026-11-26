package com.piecer.app.util

import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType

/**
 * Menghitung saldo dompet: initialBalance + pemasukan - pengeluaran +/- transfer.
 */
fun calculateWalletBalance(
    walletId: Long,
    initialBalance: Long,
    transactions: List<TransactionEntity>
): Long {
    var balance = initialBalance
    for (tx in transactions) {
        when (tx.type) {
            TransactionType.INCOME -> {
                if (tx.walletId == walletId) {
                    balance += tx.amount
                }
            }
            TransactionType.EXPENSE -> {
                if (tx.walletId == walletId) {
                    balance -= tx.amount
                }
            }
            TransactionType.TRANSFER -> {
                if (tx.walletId == walletId) {
                    balance -= tx.amount
                }
                if (tx.toWalletId == walletId) {
                    balance += tx.amount
                }
            }
        }
    }
    return balance
}
