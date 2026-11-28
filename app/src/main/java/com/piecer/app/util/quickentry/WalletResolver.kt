package com.piecer.app.util.quickentry

import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.data.local.entity.WalletType

data class WalletResolution(
    val walletId: Long?,
    val toWalletId: Long?,
    val walletName: String?,
    val toWalletName: String?,
    val remainingText: String,
    val explicitlyMentioned: Boolean
)

object WalletResolver {

    private val cashAliases = setOf("tunai", "cash", "kontan")
    private val ewalletAliases = setOf("gopay", "ovo", "dana", "shopeepay", "linkaja")
    private val bankAliases = setOf("bca", "bri", "bni", "mandiri", "jago", "seabank", "blu")

    val allKnownAliases: Set<String> = buildSet {
        addAll(cashAliases)
        addAll(ewalletAliases)
        addAll(bankAliases)
    }

    fun resolve(
        normalizedText: String,
        type: TransactionType,
        wallets: List<WalletEntity>,
        lastUsedWalletId: Long? = null
    ): WalletResolution {
        if (wallets.isEmpty() || normalizedText.isBlank()) {
            val fallback = lastUsedWalletId?.let { id -> wallets.firstOrNull { it.id == id } }
            return WalletResolution(
                walletId = fallback?.id,
                toWalletId = null,
                walletName = fallback?.name,
                toWalletName = null,
                remainingText = normalizedText,
                explicitlyMentioned = false
            )
        }

        var working = normalizedText
        val sortedWallets = wallets.sortedByDescending { it.name.length }

        if (type == TransactionType.TRANSFER) {
            var fromWallet: WalletEntity? = null
            var toWallet: WalletEntity? = null

            // 1. Cari "ke <dompet>" dan "dari <dompet>"
            for (wallet in sortedWallets) {
                val patterns = candidatePatternsForWallet(wallet)
                for (pat in patterns) {
                    val toRegex = Regex("""\bke\s+$pat\b""")
                    val fromRegex = Regex("""\bdari\s+$pat\b""")

                    if (toWallet == null) {
                        val m = toRegex.find(working)
                        if (m != null) {
                            toWallet = wallet
                            working = working.removeRange(m.range)
                            continue
                        }
                    }
                    if (fromWallet == null && wallet.id != toWallet?.id) {
                        val m = fromRegex.find(working)
                        if (m != null) {
                            fromWallet = wallet
                            working = working.removeRange(m.range)
                        }
                    }
                }
            }

            // 2. Cari dompet tanpa preposisi "dari"/"ke"
            for (wallet in sortedWallets) {
                if (wallet.id == fromWallet?.id || wallet.id == toWallet?.id) continue
                val match = findWalletInText(working, wallet)
                if (match != null) {
                    if (fromWallet == null && toWallet != null) {
                        fromWallet = wallet
                        working = working.removeRange(match.range)
                    } else if (toWallet == null) {
                        toWallet = wallet
                        working = working.removeRange(match.range)
                    }
                }
            }

            // Jika hanya toWallet yang ketemu dan fromWallet null, coba pakai lastUsedWalletId
            if (fromWallet == null && lastUsedWalletId != null) {
                val candidate = wallets.firstOrNull { it.id == lastUsedWalletId && it.id != toWallet?.id }
                if (candidate != null) fromWallet = candidate
            }

            val cleaned = working.replace(Regex("""\s+"""), " ").trim()
            return WalletResolution(
                walletId = fromWallet?.id,
                toWalletId = toWallet?.id,
                walletName = fromWallet?.name,
                toWalletName = toWallet?.name,
                remainingText = cleaned,
                explicitlyMentioned = fromWallet != null || toWallet != null
            )
        }

        // Non-transfer: cari satu dompet yang disebut
        for (wallet in sortedWallets) {
            val match = findWalletInText(working, wallet)
            if (match != null) {
                working = working.removeRange(match.range)
                // Hapus preposisi "pakai" / "via" / "di" / "dari" tepat sebelum dompet jika tersisa di akhir
                val cleaned = working.replace(Regex("""\s+"""), " ").trim()
                return WalletResolution(
                    walletId = wallet.id,
                    toWalletId = null,
                    walletName = wallet.name,
                    toWalletName = null,
                    remainingText = cleaned,
                    explicitlyMentioned = true
                )
            }
        }

        // Fuzzy match terhadap nama dompet jika belum ketemu
        val fuzzyMatch = findFuzzyWalletInText(working, sortedWallets)
        if (fuzzyMatch != null) {
            val (wallet, range) = fuzzyMatch
            working = working.removeRange(range)
            val cleaned = working.replace(Regex("""\s+"""), " ").trim()
            return WalletResolution(
                walletId = wallet.id,
                toWalletId = null,
                walletName = wallet.name,
                toWalletName = null,
                remainingText = cleaned,
                explicitlyMentioned = true
            )
        }

        val fallback = lastUsedWalletId?.let { id -> wallets.firstOrNull { it.id == id } }
        return WalletResolution(
            walletId = fallback?.id,
            toWalletId = null,
            walletName = fallback?.name,
            toWalletName = null,
            remainingText = working,
            explicitlyMentioned = false
        )
    }

    fun countDistinctWalletsMentioned(normalizedText: String, wallets: List<WalletEntity>): Int {
        if (wallets.isEmpty() || normalizedText.isBlank()) return 0
        val matchedIds = mutableSetOf<Long>()
        for (wallet in wallets.sortedByDescending { it.name.length }) {
            if (findWalletInText(normalizedText, wallet) != null) {
                matchedIds += wallet.id
            }
        }
        return matchedIds.size
    }

    private fun findWalletInText(text: String, wallet: WalletEntity): MatchResult? {
        for (pat in candidatePatternsForWallet(wallet)) {
            val regex = Regex("""\b$pat\b""")
            val match = regex.find(text)
            if (match != null) return match
        }
        return null
    }

    private fun candidatePatternsForWallet(wallet: WalletEntity): List<String> {
        val nameLower = wallet.name.trim().lowercase()
        if (nameLower.isEmpty()) return emptyList()
        val patterns = mutableListOf(Regex.escape(nameLower))

        if (wallet.type == WalletType.CASH || cashAliases.any { nameLower.contains(it) }) {
            cashAliases.forEach { alias ->
                if (alias != nameLower) patterns += Regex.escape(alias)
            }
        }
        if (wallet.type == WalletType.EWALLET) {
            ewalletAliases.forEach { alias ->
                if (nameLower.contains(alias) || alias.contains(nameLower)) {
                    patterns += Regex.escape(alias)
                }
            }
        }
        if (wallet.type == WalletType.BANK) {
            bankAliases.forEach { alias ->
                if (nameLower.contains(alias) || alias.contains(nameLower)) {
                    patterns += Regex.escape(alias)
                }
            }
        }
        return patterns.distinct()
    }

    private fun findFuzzyWalletInText(
        text: String,
        wallets: List<WalletEntity>
    ): Pair<WalletEntity, IntRange>? {
        val tokenRegex = Regex("""[a-z0-9]+""")
        for (m in tokenRegex.findAll(text)) {
            val token = m.value
            if (token.length < 4 || token.any { it.isDigit() }) continue
            for (wallet in wallets) {
                val wName = wallet.name.trim().lowercase()
                if (wName.length < 4 || wName.contains(' ')) continue
                val maxDist = if (wName.length >= 7) 2 else 1
                if (Normalizer.levenshteinDistance(token, wName, maxDist) <= maxDist) {
                    return wallet to m.range
                }
            }
        }
        return null
    }
}
