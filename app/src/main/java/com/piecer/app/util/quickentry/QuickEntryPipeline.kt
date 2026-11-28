package com.piecer.app.util.quickentry

import com.piecer.app.data.local.entity.TransactionType

object QuickEntryPipeline {

    private const val LARGE_EXPENSE_THRESHOLD = 1_000_000_000L

    private val trailingOrStandaloneFunctionWords = Regex(
        """\b(?:pakai|via|total|semua|jadi)\b"""
    )
    private val trailingPrepositionRegex = Regex("""(?:\s+\b(?:di|ke|dari|untuk|buat|sama|dengan|yang)\b)+$""")
    private val leadingPrepositionRegex = Regex("""^(?:\b(?:di|ke|dari|untuk|buat|sama|dengan|yang)\b\s+)+""")
    private val transferWordsRegex = Regex("""\b(?:transfer|pindah|kirim)\b""")

    fun parse(input: String, context: QuickEntryContext = QuickEntryContext()): QuickEntryParseResult {
        if (input.isBlank()) {
            return QuickEntryParseResult(entries = emptyList(), normalizedText = "")
        }

        val extraKnownWords = buildSet {
            context.wallets.forEach { wallet ->
                wallet.name.trim().lowercase().split(Regex("""\s+""")).forEach { w ->
                    if (w.isNotEmpty()) add(w)
                }
            }
            context.categories.forEach { cat ->
                if (!cat.isArchived) {
                    cat.name.trim().lowercase().split(Regex("""\s+""")).forEach { w ->
                        if (w.isNotEmpty()) add(w)
                    }
                }
            }
        }

        val normalized = Normalizer.normalize(input, extraKnownWords)
        if (normalized.isBlank()) {
            return QuickEntryParseResult(entries = emptyList(), normalizedText = "")
        }

        val segmented = Segmenter.segment(
            normalizedText = normalized,
            wallets = context.wallets,
            lastUsedWalletId = context.lastUsedWalletId,
            nowMillis = context.nowMillis,
            zoneId = context.zoneId
        )

        val entries = segmented.segments.map { segmentText ->
            parseSegment(
                segmentText = segmentText,
                context = context,
                sharedDateMillis = segmented.sharedDateMillis,
                sharedWalletId = segmented.sharedWalletId,
                sharedWalletName = segmented.sharedWalletName
            )
        }

        return QuickEntryParseResult(entries = entries, normalizedText = normalized)
    }

    private fun parseSegment(
        segmentText: String,
        context: QuickEntryContext,
        sharedDateMillis: Long?,
        sharedWalletId: Long?,
        sharedWalletName: String?
    ): ParsedEntry {
        val type = TypeDetector.detect(segmentText, context.wallets)

        val dateRes = DateResolver.resolve(segmentText, context.nowMillis, context.zoneId)
        val resolvedDateMillis = dateRes.dateMillis ?: sharedDateMillis

        val amountRes = AmountExtractor.extract(dateRes.remainingText)

        val walletRes = WalletResolver.resolve(
            normalizedText = amountRes.remainingText,
            type = type,
            wallets = context.wallets,
            lastUsedWalletId = if (sharedWalletId != null) null else context.lastUsedWalletId
        )

        val finalWalletId = walletRes.walletId ?: if (type != TransactionType.TRANSFER) sharedWalletId else null
        val finalWalletName = walletRes.walletName ?: if (type != TransactionType.TRANSFER) sharedWalletName else null

        val categoryRes = if (type == TransactionType.TRANSFER) {
            CategoryResolution(categoryId = null, candidates = emptyList())
        } else {
            CategoryResolver.resolve(
                normalizedText = walletRes.remainingText,
                type = type,
                categories = context.categories,
                userCorrections = if (context.learningEnabled) context.userCorrections else emptyMap(),
                historyKeywordMap = if (context.learningEnabled) context.historyKeywordMap else emptyMap()
            )
        }

        val note = buildNote(walletRes.remainingText, type)

        val flags = buildSet {
            if (amountRes.isAmbiguousM) add(QuickEntryFlag.AMBIGUOUS_M)
            if (type == TransactionType.EXPENSE && amountRes.amount != null && amountRes.amount > LARGE_EXPENSE_THRESHOLD) {
                add(QuickEntryFlag.LARGE_AMOUNT)
            }
            if (amountRes.amount == null) add(QuickEntryFlag.NO_AMOUNT)
            if (type != TransactionType.TRANSFER && categoryRes.categoryId == null) {
                add(QuickEntryFlag.NO_CATEGORY)
            }
            if (amountRes.isMultiAmountAmbiguous) add(QuickEntryFlag.MULTI_AMOUNT_AMBIGUOUS)
        }

        var confidence = 0.95f
        if (QuickEntryFlag.NO_AMOUNT in flags) confidence -= 0.40f
        if (QuickEntryFlag.NO_CATEGORY in flags) confidence -= 0.20f
        if (QuickEntryFlag.AMBIGUOUS_M in flags) confidence -= 0.15f
        if (QuickEntryFlag.MULTI_AMOUNT_AMBIGUOUS in flags) confidence -= 0.25f
        confidence = confidence.coerceIn(0f, 1f)

        return ParsedEntry(
            amount = amountRes.amount,
            type = type,
            categoryCandidates = categoryRes.candidates,
            categoryId = categoryRes.categoryId,
            walletId = finalWalletId,
            toWalletId = walletRes.toWalletId,
            walletName = finalWalletName,
            toWalletName = walletRes.toWalletName,
            dateMillis = resolvedDateMillis,
            note = note,
            confidence = confidence,
            flags = flags
        )
    }

    private fun buildNote(remainingText: String, type: TransactionType): String {
        var cleaned = remainingText
        if (type == TransactionType.TRANSFER) {
            cleaned = transferWordsRegex.replace(cleaned, "")
        }
        cleaned = trailingOrStandaloneFunctionWords.replace(cleaned, "")
        cleaned = cleaned
            .replace(Regex("""\s+"""), " ")
            .trim(' ', ',', '.', '-', ';', '+')
        cleaned = trailingPrepositionRegex.replace(cleaned, "").trim()
        cleaned = leadingPrepositionRegex.replace(cleaned, "").trim()
        cleaned = cleaned.trim(' ', ',', '.', '-', ';', '+')

        if (cleaned.isEmpty()) return ""
        return cleaned.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase() else ch.toString()
        }
    }
}
