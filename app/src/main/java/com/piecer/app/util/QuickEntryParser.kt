package com.piecer.app.util

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletEntity
import java.time.Instant
import java.time.LocalDate
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

object QuickEntryParser {

    private val incomeKeywords = listOf(
        "gaji", "gajian", "terima", "dapat", "pemasukan", "masuk", "bonus", "thr", "dividen", "hadiah"
    )

    private val transferPhrases = listOf(
        "transfer ke", "tf ke", "kirim ke", "pindah ke"
    )

    private val categoryKeywords: Map<String, List<String>> = mapOf(
        "Makan" to listOf(
            "makan", "sarapan", "siang", "malam", "kopi", "ngopi", "bakso", "mie", "nasi",
            "ayam", "sate", "minum", "jajan", "snack", "cafe", "kafe", "warteg", "resto",
            "boba", "teh", "roti", "soto", "gado"
        ),
        "Transport" to listOf(
            "bensin", "grab", "gojek", "ojek", "ojol", "parkir", "tol", "bus", "kereta",
            "mrt", "krl", "taxi", "taksi", "maxim", "angkot", "transport", "pertamax", "pertalite"
        ),
        "Tagihan" to listOf(
            "listrik", "wifi", "pulsa", "paket", "internet", "air", "pdam", "pln", "bpjs",
            "cicilan", "sewa", "kos", "kontrakan", "tagihan", "token", "indihome", "netflix",
            "spotify", "iuran"
        ),
        "Belanja" to listOf(
            "belanja", "baju", "sepatu", "indomaret", "alfamart", "supermarket", "tokopedia",
            "shopee", "lazada", "mall", "skincare", "sabun", "minimarket"
        ),
        "Hiburan" to listOf(
            "hiburan", "bioskop", "nonton", "film", "game", "konser", "rekreasi", "liburan",
            "karaoke", "main"
        ),
        "Kesehatan" to listOf(
            "kesehatan", "obat", "dokter", "apotek", "klinik", "rumah sakit", "vitamin", "rs"
        ),
        "Pendidikan" to listOf(
            "pendidikan", "buku", "kursus", "les", "kuliah", "sekolah", "spp", "seminar"
        ),
        "Gaji" to listOf("gaji", "gajian", "salary", "upah"),
        "Bonus" to listOf("bonus", "thr", "insentif", "komisi"),
        "Hadiah" to listOf("hadiah", "kado", "angpao", "giveaway")
    )

    private val slangAmounts: Map<String, Long> = mapOf(
        "goceng" to 5_000L,
        "ceban" to 10_000L,
        "gopek" to 500L,
        "cepek" to 100L
    )

    private val dateDayRegex = Regex("""\b(?:tanggal|tgl)\s+(\d{1,2})\b""", RegexOption.IGNORE_CASE)
    private val multiplierAmountRegex = Regex(
        """(?<![\d.,])\b(\d+(?:[.,]\d+)?)\s*(rb|ribu|k|jt|juta)\b""",
        RegexOption.IGNORE_CASE
    )
    private val plainAmountRegex = Regex("""(?<!\d)(\d{1,3}(?:\.\d{3})+|\d{2,})(?!\d)""")

    fun parse(
        input: String,
        categories: List<CategoryEntity>,
        wallets: List<WalletEntity>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): QuickEntryResult {
        val raw = input.trim()
        if (raw.isEmpty()) {
            return QuickEntryResult(
                type = TransactionType.EXPENSE,
                amount = null,
                categoryId = null,
                walletId = null,
                toWalletId = null,
                dateMillis = null,
                note = ""
            )
        }

        val lower = raw.lowercase()
        val type = detectType(lower)

        // 1. Tanggal
        val (dateMillis, textWithoutDate) = extractDate(raw, nowMillis, zoneId)

        // 2. Nominal
        val (amount, textWithoutAmount) = extractAmount(textWithoutDate)

        // 3. Dompet
        val (walletId, toWalletId, textWithoutWallet) = extractWallets(
            raw = textWithoutAmount,
            type = type,
            wallets = wallets
        )

        // 4. Kategori (hanya untuk INCOME / EXPENSE; jangan menebak jika tidak cocok)
        val categoryId = if (type == TransactionType.TRANSFER) {
            null
        } else {
            detectCategory(lower, type, categories)
        }

        val cleanedNote = cleanNote(textWithoutWallet, type)

        return QuickEntryResult(
            type = type,
            amount = amount,
            categoryId = categoryId,
            walletId = walletId,
            toWalletId = toWalletId,
            dateMillis = dateMillis,
            note = cleanedNote
        )
    }

    private fun detectType(lower: String): TransactionType {
        if (transferPhrases.any { lower.contains(it) }) {
            return TransactionType.TRANSFER
        }
        val tokens = lower.split(Regex("""\s+""")).map { it.trim(',', '.', '!', '?') }
        if (tokens.any { it in incomeKeywords }) {
            return TransactionType.INCOME
        }
        return TransactionType.EXPENSE
    }

    private fun extractDate(
        raw: String,
        nowMillis: Long,
        zoneId: ZoneId
    ): Pair<Long?, String> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        var working = raw
        var resolvedDate: LocalDate? = null

        val dateDayMatch = dateDayRegex.find(working)
        if (dateDayMatch != null) {
            val day = dateDayMatch.groupValues[1].toIntOrNull()
            if (day != null && day in 1..31) {
                val clampedDay = day.coerceAtMost(today.lengthOfMonth())
                resolvedDate = today.withDayOfMonth(clampedDay)
                working = working.removeRange(dateDayMatch.range)
            }
        } else {
            val lower = working.lowercase()
            when {
                lower.contains("kemarin") -> {
                    resolvedDate = today.minusDays(1)
                    working = working.replace(Regex("""\bkemarin\b""", RegexOption.IGNORE_CASE), "")
                }
                lower.contains("besok") -> {
                    resolvedDate = today.plusDays(1)
                    working = working.replace(Regex("""\bbesok\b""", RegexOption.IGNORE_CASE), "")
                }
                Regex("""\b(tadi\s+pagi|tadi\s+siang|tadi\s+sore|tadi\s+malam|hari\s+ini)\b""", RegexOption.IGNORE_CASE)
                    .containsMatchIn(lower) -> {
                    resolvedDate = today
                    working = working.replace(
                        Regex("""\b(tadi\s+pagi|tadi\s+siang|tadi\s+sore|tadi\s+malam|hari\s+ini)\b""", RegexOption.IGNORE_CASE),
                        ""
                    )
                }
            }
        }

        val dateMillis = resolvedDate?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli()
        return dateMillis to working
    }

    private fun extractAmount(input: String): Pair<Long?, String> {
        var working = input

        // Cek slang: goceng, ceban, gopek, cepek
        for ((slang, value) in slangAmounts) {
            val slangRegex = Regex("""\b$slang\b""", RegexOption.IGNORE_CASE)
            val match = slangRegex.find(working)
            if (match != null) {
                working = working.removeRange(match.range)
                return value to working
            }
        }

        // Cek angka dengan pengali (25rb, 25k, 25ribu, 1,5jt, 2jt)
        val multMatch = multiplierAmountRegex.find(working)
        if (multMatch != null) {
            val numStr = multMatch.groupValues[1]
            val unit = multMatch.groupValues[2].lowercase()
            val multiplier = when (unit) {
                "rb", "ribu", "k" -> 1_000L
                "jt", "juta" -> 1_000_000L
                else -> 1L
            }
            val amount = parseScaledDecimalToLong(numStr, multiplier)
            if (amount != null && amount > 0L) {
                working = working.removeRange(multMatch.range)
                return amount to working
            }
        }

        // Cek angka polos (25000, 25.000)
        val plainMatch = plainAmountRegex.find(working)
        if (plainMatch != null) {
            val digitsOnly = plainMatch.groupValues[1].replace(".", "")
            val amount = digitsOnly.toLongOrNull()
            if (amount != null && amount > 0L) {
                working = working.removeRange(plainMatch.range)
                return amount to working
            }
        }

        return null to working
    }

    /**
     * Mengubah string angka dengan desimal (mis. "1,5" atau "1.5") dan pengali (1_000_000L)
     * menjadi Long penuh tanpa Double/Float.
     */
    private fun parseScaledDecimalToLong(numStr: String, multiplier: Long): Long? {
        val normalized = numStr.replace(',', '.')
        val parts = normalized.split('.')
        return when (parts.size) {
            1 -> {
                val whole = parts[0].toLongOrNull() ?: return null
                whole * multiplier
            }
            2 -> {
                val whole = parts[0].toLongOrNull() ?: return null
                val fracStr = parts[1]
                if (fracStr.isEmpty()) return whole * multiplier
                val fracDigits = fracStr.take(6)
                val fracValue = fracDigits.toLongOrNull() ?: return null
                var scale = 1L
                repeat(fracDigits.length) { scale *= 10L }
                (whole * multiplier) + ((fracValue * multiplier) / scale)
            }
            else -> null
        }
    }

    private fun extractWallets(
        raw: String,
        type: TransactionType,
        wallets: List<WalletEntity>
    ): Triple<Long?, Long?, String> {
        if (wallets.isEmpty()) return Triple(null, null, raw)

        var working = raw
        val sortedWallets = wallets.sortedByDescending { it.name.length }

        if (type == TransactionType.TRANSFER) {
            var fromWalletId: Long? = null
            var toWalletId: Long? = null

            for (wallet in sortedWallets) {
                val escaped = Regex.escape(wallet.name)
                val toRegex = Regex("""\b(?:ke)\s+$escaped\b""", RegexOption.IGNORE_CASE)
                val fromRegex = Regex("""\b(?:dari)\s+$escaped\b""", RegexOption.IGNORE_CASE)

                val toMatch = toRegex.find(working)
                if (toMatch != null && toWalletId == null) {
                    toWalletId = wallet.id
                    working = working.removeRange(toMatch.range)
                    continue
                }
                val fromMatch = fromRegex.find(working)
                if (fromMatch != null && fromWalletId == null) {
                    fromWalletId = wallet.id
                    working = working.removeRange(fromMatch.range)
                }
            }

            // Jika masih ada dompet yang disebut tanpa preposisi
            for (wallet in sortedWallets) {
                val escaped = Regex.escape(wallet.name)
                val regex = Regex("""\b$escaped\b""", RegexOption.IGNORE_CASE)
                val match = regex.find(working)
                if (match != null) {
                    if (toWalletId == null) {
                        toWalletId = wallet.id
                        working = working.removeRange(match.range)
                    } else if (fromWalletId == null && wallet.id != toWalletId) {
                        fromWalletId = wallet.id
                        working = working.removeRange(match.range)
                    }
                }
            }

            return Triple(fromWalletId, toWalletId, working)
        }

        for (wallet in sortedWallets) {
            val escaped = Regex.escape(wallet.name)
            val regex = Regex("""\b$escaped\b""", RegexOption.IGNORE_CASE)
            val match = regex.find(working)
            if (match != null) {
                working = working.removeRange(match.range)
                return Triple(wallet.id, null, working)
            }
        }

        return Triple(null, null, working)
    }

    private fun detectCategory(
        lower: String,
        type: TransactionType,
        categories: List<CategoryEntity>
    ): Long? {
        val targetCatType = when (type) {
            TransactionType.INCOME -> CategoryType.INCOME
            TransactionType.EXPENSE -> CategoryType.EXPENSE
            TransactionType.TRANSFER -> return null
        }
        val candidates = categories.filter { it.type == targetCatType && !it.isArchived }
        if (candidates.isEmpty()) return null

        // 1. Cocokkan langsung dengan nama kategori yang ada (termasuk kategori buatan pengguna)
        val directMatch = candidates
            .sortedByDescending { it.name.trim().length }
            .firstOrNull { cat ->
                val trimmed = cat.name.trim().lowercase()
                trimmed.isNotEmpty() &&
                    Regex("""\b${Regex.escape(trimmed)}\b""").containsMatchIn(lower)
            }
        if (directMatch != null) return directMatch.id

        // 2. Cocokkan kata kunci ke nama kategori
        for ((catName, keywords) in categoryKeywords) {
            val category = candidates.firstOrNull { it.name.equals(catName, ignoreCase = true) }
                ?: continue
            for (kw in keywords) {
                if (Regex("""\b${Regex.escape(kw)}\b""").containsMatchIn(lower)) {
                    return category.id
                }
            }
        }

        return null
    }

    private fun cleanNote(text: String, type: TransactionType): String {
        var cleaned = text
        if (type == TransactionType.TRANSFER) {
            for (phrase in transferPhrases) {
                cleaned = cleaned.replace(Regex("""\b${Regex.escape(phrase)}\b""", RegexOption.IGNORE_CASE), "")
            }
        }
        return cleaned
            .replace(Regex("""\s+"""), " ")
            .trim(' ', ',', '.', '-')
    }
}
