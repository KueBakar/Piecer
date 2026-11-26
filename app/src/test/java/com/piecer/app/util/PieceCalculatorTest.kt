package com.piecer.app.util

import com.piecer.app.data.local.entity.PieceAllocationEntity
import com.piecer.app.data.local.entity.PieceCategoryEntity
import com.piecer.app.data.local.entity.PieceEntity
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PieceCalculatorTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val pieces = listOf(
        PieceEntity(1L, "Kebutuhan", 50, "ink", "home", 0, 1L),
        PieceEntity(2L, "Keinginan", 30, "amber", "shopping_bag", 1, 1L),
        PieceEntity(3L, "Tabungan", 20, "mint", "savings", 2, 1L)
    )

    @Test
    fun `allocateIncomeToPieces splits exact multiples accurately`() {
        val result = PieceCalculator.allocateIncomeToPieces(10_000_000L, pieces)
        assertEquals(3, result.size)
        assertEquals(5_000_000L, result[0].amount)
        assertEquals(3_000_000L, result[1].amount)
        assertEquals(2_000_000L, result[2].amount)
        assertEquals(10_000_000L, result.sumOf { it.amount })
    }

    @Test
    fun `allocateIncomeToPieces assigns rounding remainder to first piece`() {
        val customPieces = listOf(
            PieceEntity(1L, "A", 33, "ink", "home", 0, 1L),
            PieceEntity(2L, "B", 33, "amber", "shopping_bag", 1, 1L),
            PieceEntity(3L, "C", 34, "mint", "savings", 2, 1L)
        )
        // 1001 * 33 / 100 = 330; 1001 * 33 / 100 = 330; 1001 * 34 / 100 = 340 -> sum = 1000, remainder = 1
        val result = PieceCalculator.allocateIncomeToPieces(1_001L, customPieces)
        assertEquals(331L, result[0].amount)
        assertEquals(330L, result[1].amount)
        assertEquals(340L, result[2].amount)
        assertEquals(1_001L, result.sumOf { it.amount })
    }

    @Test
    fun `isTotalPercentValid requires sum equal to 100 and valid bounds`() {
        assertTrue(PieceCalculator.isTotalPercentValid(listOf(50, 30, 20)))
        assertTrue(PieceCalculator.isTotalPercentValid(listOf(70, 20, 10)))
        assertFalse(PieceCalculator.isTotalPercentValid(listOf(50, 30, 10)))
        assertFalse(PieceCalculator.isTotalPercentValid(listOf(60, 50)))
        assertFalse(PieceCalculator.isTotalPercentValid(listOf(110, -10)))
        assertFalse(PieceCalculator.isTotalPercentValid(emptyList()))
    }

    @Test
    fun `calculatePiecesOverview computes piece balance and unassigned expense`() {
        val today = LocalDate.of(2026, 11, 15)
        val nowMillis = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val period = FinancialCycleHelper.currentPeriod(nowMillis, cycleStartDay = 1, zoneId = zone)

        val mappings = listOf(
            PieceCategoryEntity(pieceId = 1L, categoryId = 10L),
            PieceCategoryEntity(pieceId = 2L, categoryId = 20L)
        )
        val txs = listOf(
            TransactionEntity(100L, 1_000_000L, TransactionType.INCOME, 1L, 1L, null, nowMillis, "Gaji", nowMillis),
            TransactionEntity(101L, 200_000L, TransactionType.EXPENSE, 10L, 1L, null, nowMillis, "Makan", nowMillis),
            TransactionEntity(102L, 50_000L, TransactionType.EXPENSE, 99L, 1L, null, nowMillis, "Lain", nowMillis)
        )
        val allocations = listOf(
            PieceAllocationEntity(1L, 1L, 100L, 500_000L, nowMillis),
            PieceAllocationEntity(2L, 2L, 100L, 300_000L, nowMillis),
            PieceAllocationEntity(3L, 3L, 100L, 200_000L, nowMillis)
        )

        val overview = PieceCalculator.calculatePiecesOverview(
            pieces = pieces,
            pieceCategories = mappings,
            allocations = allocations,
            transactions = txs,
            cyclePeriod = period,
            nowMillis = nowMillis,
            zoneId = zone
        )

        val kebutuhan = overview.summaries.first { it.piece.id == 1L }
        assertEquals(500_000L, kebutuhan.allocated)
        assertEquals(200_000L, kebutuhan.spent)
        assertEquals(300_000L, kebutuhan.remaining)

        val keinginan = overview.summaries.first { it.piece.id == 2L }
        assertEquals(300_000L, keinginan.allocated)
        assertEquals(0L, keinginan.spent)
        assertEquals(300_000L, keinginan.remaining)

        assertEquals(50_000L, overview.unassignedExpense)
    }
}
