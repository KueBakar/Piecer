package com.piecer.app.util.quickentry

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletEntity
import java.time.ZoneId

enum class QuickEntryFlag {
    AMBIGUOUS_M,
    LARGE_AMOUNT,
    NO_AMOUNT,
    NO_CATEGORY,
    MULTI_AMOUNT_AMBIGUOUS
}

enum class CategoryMatchSource {
    USER_CORRECTION,
    HISTORY,
    BUILTIN_PHRASE,
    BUILTIN_KEYWORD,
    DIRECT_NAME,
    FUZZY_NAME
}

data class CategoryCandidate(
    val categoryId: Long,
    val categoryName: String,
    val score: Float,
    val source: CategoryMatchSource
)

data class ParsedEntry(
    val amount: Long?,
    val type: TransactionType,
    val categoryCandidates: List<CategoryCandidate>,
    val categoryId: Long?,
    val walletId: Long?,
    val toWalletId: Long?,
    val walletName: String?,
    val toWalletName: String?,
    val dateMillis: Long?,
    val note: String,
    val confidence: Float,
    val flags: Set<QuickEntryFlag>
)

data class QuickEntryParseResult(
    val entries: List<ParsedEntry>,
    val normalizedText: String
)

data class QuickEntryContext(
    val categories: List<CategoryEntity> = emptyList(),
    val wallets: List<WalletEntity> = emptyList(),
    val lastUsedWalletId: Long? = null,
    val userCorrections: Map<String, String> = emptyMap(),
    val historyKeywordMap: Map<String, Map<String, Int>> = emptyMap(),
    val learningEnabled: Boolean = true,
    val nowMillis: Long = System.currentTimeMillis(),
    val zoneId: ZoneId = ZoneId.systemDefault()
)
