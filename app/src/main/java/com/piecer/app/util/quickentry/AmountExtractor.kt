package com.piecer.app.util.quickentry

data class AmountExtractionResult(
    val amount: Long?,
    val remainingText: String,
    val isAmbiguousM: Boolean,
    val isMultiAmountAmbiguous: Boolean
)

object AmountExtractor {

    val quantityUnitsSet: Set<String> = setOf(
        "hari", "minggu", "bulan", "tahun", "jam", "menit",
        "porsi", "pcs", "buah", "biji", "orang", "kali",
        "bungkus", "botol", "gelas", "cup", "liter", "kg",
        "gram", "lembar", "unit"
    )

    private val nextWordRegex = Regex("""^\s*([a-z]+)""")

    fun extract(normalizedText: String): AmountExtractionResult {
        if (normalizedText.isBlank()) {
            return AmountExtractionResult(
                amount = null,
                remainingText = normalizedText,
                isAmbiguousM = false,
                isMultiAmountAmbiguous = false
            )
        }

        val allNumbers = NumberParser.findAllNumbers(normalizedText)
        if (allNumbers.isEmpty()) {
            return AmountExtractionResult(
                amount = null,
                remainingText = normalizedText,
                isAmbiguousM = false,
                isMultiAmountAmbiguous = false
            )
        }

        val nominalCandidates = mutableListOf<NumberMatch>()
        for (num in allNumbers) {
            val after = normalizedText.substring((num.range.last + 1).coerceAtMost(normalizedText.length))
            val nextWord = nextWordRegex.find(after)?.groupValues?.get(1)
            val isFollowedByQuantityUnit = nextWord != null && nextWord in quantityUnitsSet

            if (!num.hasMoneyUnit && isFollowedByQuantityUnit) {
                // Kuantitas (mis. "10 hari", "2 porsi") -> tetap di catatan, bukan nominal
                continue
            }
            if (num.hasMoneyUnit || num.value >= 100L) {
                nominalCandidates += num
            }
        }

        if (nominalCandidates.isEmpty()) {
            return AmountExtractionResult(
                amount = null,
                remainingText = normalizedText,
                isAmbiguousM = false,
                isMultiAmountAmbiguous = false
            )
        }

        val explicitTotal = nominalCandidates.lastOrNull { it.isExplicitTotal }
        val chosen = explicitTotal ?: nominalCandidates.first()
        val isMultiAmbiguous = nominalCandidates.size > 1 && explicitTotal == null

        var working = normalizedText.removeRange(chosen.range)
        // Jika ada kata fungsi "total"/"semua"/"jadi" tepat sebelum angka yang dipilih, tetap bisa dibersihkan di note
        working = working.replace(Regex("""\s+"""), " ").trim()

        return AmountExtractionResult(
            amount = chosen.value,
            remainingText = working,
            isAmbiguousM = chosen.isAmbiguousM,
            isMultiAmountAmbiguous = isMultiAmbiguous
        )
    }
}
