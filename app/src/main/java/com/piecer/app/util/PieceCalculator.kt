package com.piecer.app.util

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.PieceAllocationEntity
import com.piecer.app.data.local.entity.PieceCategoryEntity
import com.piecer.app.data.local.entity.PieceEntity
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import java.time.ZoneId

data class PieceAllocationResult(
    val pieceId: Long,
    val pieceName: String,
    val percent: Int,
    val amount: Long
)

data class PieceBalanceSummary(
    val piece: PieceEntity,
    val allocated: Long,
    val spent: Long,
    val remaining: Long,
    val progressRatio: Float,
    val categoryIds: List<Long>
)

data class PiecesOverview(
    val summaries: List<PieceBalanceSummary>,
    val unassignedExpense: Long,
    val totalAllocated: Long,
    val totalSpent: Long,
    val totalRemaining: Long
)

enum class PieceTemplateType {
    BALANCED_50_30_20,
    SAVING_70_20_10,
    CUSTOM
}

data class PieceDraftItem(
    val name: String,
    val percent: Int,
    val colorKey: String,
    val iconKey: String
)

object PieceCalculator {

    private val needsCategoryNames = setOf(
        "makan", "transport", "tagihan", "kesehatan", "pendidikan"
    )
    private val wantsCategoryNames = setOf(
        "belanja", "hiburan", "lainnya"
    )

    fun isTotalPercentValid(percents: List<Int>): Boolean {
        if (percents.isEmpty()) return false
        if (percents.any { it < 0 || it > 100 }) return false
        return percents.sum() == 100
    }

    /**
     * Membagi nominal pemasukan ke daftar piece sesuai persen masing-masing.
     * Sisa pembulatan (remainder) masuk ke piece pertama agar total alokasi persis sama dengan nominal.
     */
    fun allocateIncomeToPieces(
        amount: Long,
        pieces: List<PieceEntity>
    ): List<PieceAllocationResult> {
        if (amount <= 0L || pieces.isEmpty()) return emptyList()
        val sorted = pieces.sortedWith(compareBy<PieceEntity> { it.sortOrder }.thenBy { it.id })
        val rawAmounts = sorted.map { piece ->
            (amount * piece.percent.toLong()) / 100L
        }.toMutableList()
        val allocatedSum = rawAmounts.sum()
        val remainder = amount - allocatedSum
        if (remainder != 0L && rawAmounts.isNotEmpty()) {
            rawAmounts[0] = rawAmounts[0] + remainder
        }
        return sorted.mapIndexed { index, piece ->
            PieceAllocationResult(
                pieceId = piece.id,
                pieceName = piece.name,
                percent = piece.percent,
                amount = rawAmounts[index]
            )
        }
    }

    fun calculatePiecesOverview(
        pieces: List<PieceEntity>,
        pieceCategories: List<PieceCategoryEntity>,
        allocations: List<PieceAllocationEntity>,
        transactions: List<TransactionEntity>,
        cyclePeriod: CyclePeriod,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): PiecesOverview {
        val sortedPieces = pieces.sortedWith(compareBy<PieceEntity> { it.sortOrder }.thenBy { it.id })
        val cutoff = DateUtils.endOfTodayMillis(nowMillis, zoneId)
        val effectiveEnd = minOf(cyclePeriod.endMillis, cutoff)

        val txById = transactions.associateBy { it.id }
        val categoryToPieceId = pieceCategories.associate { it.categoryId to it.pieceId }
        val categoriesByPieceId = pieceCategories.groupBy({ it.pieceId }, { it.categoryId })

        val allocatedByPiece = HashMap<Long, Long>()
        for (alloc in allocations) {
            val sourceDate = txById[alloc.transactionId]?.date ?: alloc.createdAt
            if (sourceDate in cyclePeriod.startMillis..effectiveEnd) {
                allocatedByPiece[alloc.pieceId] = (allocatedByPiece[alloc.pieceId] ?: 0L) + alloc.amount
            }
        }

        val spentByPiece = HashMap<Long, Long>()
        var unassignedExpense = 0L
        for (tx in transactions) {
            if (tx.type == TransactionType.EXPENSE && tx.date in cyclePeriod.startMillis..effectiveEnd) {
                val pieceId = tx.categoryId?.let { categoryToPieceId[it] }
                if (pieceId != null) {
                    spentByPiece[pieceId] = (spentByPiece[pieceId] ?: 0L) + tx.amount
                } else {
                    unassignedExpense += tx.amount
                }
            }
        }

        val summaries = sortedPieces.map { piece ->
            val allocated = allocatedByPiece[piece.id] ?: 0L
            val spent = spentByPiece[piece.id] ?: 0L
            val remaining = allocated - spent
            val progressRatio = when {
                allocated > 0L -> spent.toFloat() / allocated.toFloat()
                spent > 0L -> 1f
                else -> 0f
            }
            PieceBalanceSummary(
                piece = piece,
                allocated = allocated,
                spent = spent,
                remaining = remaining,
                progressRatio = progressRatio,
                categoryIds = categoriesByPieceId[piece.id].orEmpty()
            )
        }

        return PiecesOverview(
            summaries = summaries,
            unassignedExpense = unassignedExpense,
            totalAllocated = summaries.sumOf { it.allocated },
            totalSpent = summaries.sumOf { it.spent },
            totalRemaining = summaries.sumOf { it.remaining }
        )
    }

    fun templateDrafts(template: PieceTemplateType): List<PieceDraftItem> = when (template) {
        PieceTemplateType.BALANCED_50_30_20 -> listOf(
            PieceDraftItem("Kebutuhan", 50, "ink", "home"),
            PieceDraftItem("Keinginan", 30, "amber", "shopping_bag"),
            PieceDraftItem("Tabungan", 20, "mint", "savings")
        )
        PieceTemplateType.SAVING_70_20_10 -> listOf(
            PieceDraftItem("Kebutuhan", 70, "ink", "home"),
            PieceDraftItem("Tabungan", 20, "mint", "savings"),
            PieceDraftItem("Keinginan", 10, "amber", "shopping_bag")
        )
        PieceTemplateType.CUSTOM -> listOf(
            PieceDraftItem("Kebutuhan", 50, "ink", "home"),
            PieceDraftItem("Lainnya", 50, "mint", "savings")
        )
    }

    /**
     * Memetakan kategori pengeluaran bawaan ke indeks piece pada template (0 = Kebutuhan, dst).
     */
    fun defaultCategoryMappings(
        drafts: List<PieceDraftItem>,
        categories: List<CategoryEntity>
    ): Map<Int, List<Long>> {
        val needsIndex = drafts.indexOfFirst { it.name.equals("Kebutuhan", ignoreCase = true) }
        val wantsIndex = drafts.indexOfFirst { it.name.equals("Keinginan", ignoreCase = true) }
        val result = HashMap<Int, MutableList<Long>>()

        categories
            .filter { it.type == CategoryType.EXPENSE }
            .forEach { cat ->
                val lower = cat.name.trim().lowercase()
                val targetIndex = when {
                    lower in needsCategoryNames && needsIndex >= 0 -> needsIndex
                    lower in wantsCategoryNames && wantsIndex >= 0 -> wantsIndex
                    else -> null
                }
                if (targetIndex != null) {
                    result.getOrPut(targetIndex) { mutableListOf() }.add(cat.id)
                }
            }
        return result
    }
}
