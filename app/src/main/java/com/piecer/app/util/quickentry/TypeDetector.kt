package com.piecer.app.util.quickentry

import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletEntity

object TypeDetector {

    private val incomeKeywords: Set<String> = setOf(
        "gaji", "gajian", "bonus", "thr", "dapat", "terima", "nerima",
        "dikasih", "ditransfer", "cashback", "untung", "jual", "honor",
        "angpao", "dividen", "pemasukan", "hadiah", "insentif", "komisi", "refund"
    )

    private val incomePhrases: List<String> = listOf(
        "transfer masuk", "dapat gaji", "dapat bonus", "dapat thr", "dapat angpao"
    )

    private val expenseOverrideIndicators: Set<String> = setOf(
        "beli", "bayar", "belanja", "pesan", "order", "jajan", "makan", "minum"
    )

    private val transferKeywords: Set<String> = setOf(
        "transfer", "pindah", "kirim"
    )

    val allKeywordsSet: Set<String> = buildSet {
        addAll(incomeKeywords)
        addAll(expenseOverrideIndicators)
        addAll(transferKeywords)
        add("diskon")
        add("masuk")
    }

    fun detect(normalizedText: String, wallets: List<WalletEntity>): TransactionType {
        if (normalizedText.isBlank()) return TransactionType.EXPENSE

        // 1. "transfer masuk" selalu INCOME
        if (incomePhrases.any { normalizedText.contains(it) }) {
            return TransactionType.INCOME
        }

        val tokens = normalizedText.split(Regex("""[\s,;+]+""")).filter { it.isNotEmpty() }

        // 2. Transfer antar dompet bila pola transfer/pindah X ke Y dengan dua dompet dikenal
        //    (atau pola eksplisit "transfer ke <dompet>" / "pindah ke <dompet>")
        val hasTransferVerb = tokens.any { it in transferKeywords }
        val hasKe = tokens.contains("ke")
        if (hasTransferVerb && hasKe) {
            val distinctWallets = WalletResolver.countDistinctWalletsMentioned(normalizedText, wallets)
            if (distinctWallets >= 2 || Regex("""\b(?:transfer|pindah|kirim)\s+ke\b""").containsMatchIn(normalizedText)) {
                return TransactionType.TRANSFER
            }
        }

        // 3. "dapat diskon" tetap pengeluaran bila ada beli/bayar
        val hasDapatDiskon = Regex("""\bdapat\s+diskon\b""").containsMatchIn(normalizedText)
        if (hasDapatDiskon && tokens.any { it in expenseOverrideIndicators }) {
            return TransactionType.EXPENSE
        }

        // 4. Kata kunci pemasukan
        if (tokens.any { it in incomeKeywords }) {
            return TransactionType.INCOME
        }

        return TransactionType.EXPENSE
    }
}
