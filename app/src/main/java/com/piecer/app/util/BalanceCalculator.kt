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

/**
 * Menghitung batas pengeluaran harian ("Boleh Jajan Hari Ini") dalam rupiah penuh (Long).
 *
 * Rumus Boleh Jajan Hari Ini:
 * sisaBulanIni = pemasukanBulanIni - pengeluaranBulanIni
 * bolehJajanHariIni = max(0, sisaBulanIni / max(1, sisaHariDiBulanIni))
 *
 * Keterangan: sisaHariDiBulanIni dihitung termasuk hari ini (minimal 1).
 */
fun calculateBolehJajanHariIni(
    monthlyIncome: Long,
    monthlyExpense: Long,
    remainingDaysInMonth: Int
): Long {
    val remainingBudget = monthlyIncome - monthlyExpense
    if (remainingBudget <= 0L) return 0L
    val safeDays = remainingDaysInMonth.coerceAtLeast(1).toLong()
    return remainingBudget / safeDays
}
