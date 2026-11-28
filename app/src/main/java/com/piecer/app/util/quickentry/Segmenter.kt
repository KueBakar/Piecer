package com.piecer.app.util.quickentry

import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletEntity
import java.time.ZoneId

data class SegmentedInput(
    val segments: List<String>,
    val sharedDateMillis: Long?,
    val sharedWalletId: Long?,
    val sharedWalletName: String?
)

object Segmenter {

    private val explicitDelimiterRegex = Regex("""\s*(?:,|;|\n|\+|\b(?:dan|terus|lalu|sama)\b)\s*""")

    fun segment(
        normalizedText: String,
        wallets: List<WalletEntity>,
        lastUsedWalletId: Long? = null,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): SegmentedInput {
        if (normalizedText.isBlank()) {
            return SegmentedInput(
                segments = emptyList(),
                sharedDateMillis = null,
                sharedWalletId = null,
                sharedWalletName = null
            )
        }

        // Jika pola transfer, jangan dipecah
        if (TypeDetector.detect(normalizedText, wallets) == TransactionType.TRANSFER) {
            return SegmentedInput(
                segments = listOf(normalizedText),
                sharedDateMillis = null,
                sharedWalletId = null,
                sharedWalletName = null
            )
        }

        val moneyNumbers = NumberParser.findAllNumbers(normalizedText).filter { it.hasMoneyUnit }
        // Jika ada penanda total eksplisit (total/semua/jadi), jangan pecah menjadi banyak transaksi
        if (moneyNumbers.size <= 1 || moneyNumbers.any { it.isExplicitTotal && !it.isMultiplication }) {
            return SegmentedInput(
                segments = listOf(normalizedText),
                sharedDateMillis = null,
                sharedWalletId = null,
                sharedWalletName = null
            )
        }

        // Cek apakah ada delimiter eksplisit di antara nominal
        val hasExplicitDelimiters = explicitDelimiterRegex.containsMatchIn(normalizedText)
        val rawSegments: List<String> = if (hasExplicitDelimiters) {
            splitByExplicitDelimiters(normalizedText)
        } else {
            splitByItemNominalPattern(normalizedText, moneyNumbers)
        }

        if (rawSegments.size <= 1) {
            return SegmentedInput(
                segments = listOf(normalizedText),
                sharedDateMillis = null,
                sharedWalletId = null,
                sharedWalletName = null
            )
        }

        // Ekstrak tanggal & dompet bersama jika disebut sekali di akhir atau di salah satu segmen
        var sharedDateMillis: Long? = null
        var sharedWalletId: Long? = null
        var sharedWalletName: String? = null

        val cleanedSegments = mutableListOf<String>()
        for (seg in rawSegments) {
            val trimmed = seg.trim()
            if (trimmed.isEmpty()) continue

            val segMoneyCount = NumberParser.findAllNumbers(trimmed).count { it.hasMoneyUnit || it.value >= 100L }
            if (segMoneyCount == 0) {
                // Segmen ini kemungkinan hanya berisi modifier global seperti "pakai gopay" atau "kemarin"
                val dateRes = DateResolver.resolve(trimmed, nowMillis, zoneId)
                if (dateRes.dateMillis != null && sharedDateMillis == null) {
                    sharedDateMillis = dateRes.dateMillis
                }
                val walletRes = WalletResolver.resolve(
                    normalizedText = dateRes.remainingText,
                    type = TransactionType.EXPENSE,
                    wallets = wallets,
                    lastUsedWalletId = null
                )
                if (walletRes.explicitlyMentioned && sharedWalletId == null) {
                    sharedWalletId = walletRes.walletId
                    sharedWalletName = walletRes.walletName
                }
                // Jika masih ada kata item selain modifier, pertahankan sebagai segmen
                val leftover = walletRes.remainingText
                    .replace(Regex("""\b(?:pakai|via|di|ke|dari)\b"""), "")
                    .trim()
                if (leftover.isNotEmpty() && dateRes.dateMillis == null && !walletRes.explicitlyMentioned) {
                    cleanedSegments += trimmed
                }
            } else {
                cleanedSegments += trimmed
            }
        }

        // Jika sharedDateMillis atau sharedWalletId belum ketemu dari trailing segment,
        // cek apakah hanya disebut sekali di keseluruhan segmen
        if (sharedDateMillis == null) {
            val dateRes = DateResolver.resolve(normalizedText, nowMillis, zoneId)
            if (dateRes.dateMillis != null) {
                sharedDateMillis = dateRes.dateMillis
            }
        }
        if (sharedWalletId == null) {
            val distinctWallets = WalletResolver.countDistinctWalletsMentioned(normalizedText, wallets)
            if (distinctWallets == 1) {
                val walletRes = WalletResolver.resolve(
                    normalizedText = normalizedText,
                    type = TransactionType.EXPENSE,
                    wallets = wallets,
                    lastUsedWalletId = null
                )
                if (walletRes.explicitlyMentioned) {
                    sharedWalletId = walletRes.walletId
                    sharedWalletName = walletRes.walletName
                }
            } else if (distinctWallets == 0 && lastUsedWalletId != null) {
                val fallback = wallets.firstOrNull { it.id == lastUsedWalletId }
                sharedWalletId = fallback?.id
                sharedWalletName = fallback?.name
            }
        }

        val finalSegments = if (cleanedSegments.isEmpty()) listOf(normalizedText) else cleanedSegments
        return SegmentedInput(
            segments = finalSegments,
            sharedDateMillis = sharedDateMillis,
            sharedWalletId = sharedWalletId,
            sharedWalletName = sharedWalletName
        )
    }

    private fun splitByExplicitDelimiters(text: String): List<String> {
        val parts = text.split(explicitDelimiterRegex).map { it.trim() }.filter { it.isNotEmpty() }
        // Jika salah satu part masih memiliki >1 nominal pola "item nominal item nominal", pecah lagi
        return parts.flatMap { part ->
            val moneyNumbers = NumberParser.findAllNumbers(part).filter { it.hasMoneyUnit }
            if (moneyNumbers.size > 1) {
                splitByItemNominalPattern(part, moneyNumbers)
            } else {
                listOf(part)
            }
        }
    }

    private fun splitByItemNominalPattern(text: String, moneyNumbers: List<NumberMatch>): List<String> {
        // Pastikan ada teks item di antara nominal-nominal tersebut (bukan "bakso 20k 25k")
        for (idx in 1 until moneyNumbers.size) {
            val between = text.substring(moneyNumbers[idx - 1].range.last + 1, moneyNumbers[idx].range.first).trim()
            if (between.isEmpty()) {
                return listOf(text)
            }
        }

        val segments = mutableListOf<String>()
        var start = 0
        for (idx in moneyNumbers.indices) {
            val end = moneyNumbers[idx].range.last + 1
            val chunk = text.substring(start, end).trim()
            if (chunk.isNotEmpty()) {
                segments += chunk
            }
            start = end
        }
        if (start < text.length) {
            val trailing = text.substring(start).trim()
            if (trailing.isNotEmpty()) {
                segments += trailing
            }
        }
        return segments
    }
}
