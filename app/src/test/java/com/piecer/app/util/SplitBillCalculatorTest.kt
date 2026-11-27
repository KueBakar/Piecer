package com.piecer.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SplitBillCalculatorTest {

    @Test
    fun `splitEqual distributes rounding remainder fairly so sum equals grandTotal`() {
        val result = SplitBillCalculator.splitEqual(
            subtotal = 100_000L,
            participantNames = listOf("Ani", "Budi", "Cici")
        )
        assertEquals(100_000L, result.grandTotal)
        assertEquals(3, result.shares.size)
        assertEquals(33_334L, result.shares[0].amount)
        assertEquals(33_333L, result.shares[1].amount)
        assertEquals(33_333L, result.shares[2].amount)
        assertEquals(result.grandTotal, result.shares.sumOf { it.amount })
    }

    @Test
    fun `splitEqual applies tax and service percent in integer rupiah`() {
        val result = SplitBillCalculator.splitEqual(
            subtotal = 200_000L,
            participantNames = listOf("Saya", "Teman"),
            taxPercent = 10,
            servicePercent = 5
        )
        assertEquals(20_000L, result.taxAmount)
        assertEquals(10_000L, result.serviceAmount)
        assertEquals(230_000L, result.grandTotal)
        assertEquals(115_000L, result.shares[0].amount)
        assertEquals(115_000L, result.shares[1].amount)
    }

    @Test
    fun `splitCustom distributes tax and rounding remainder so sum equals grandTotal`() {
        val result = SplitBillCalculator.splitCustom(
            participants = listOf(
                SplitParticipantInput("A", 30_000L),
                SplitParticipantInput("B", 40_000L),
                SplitParticipantInput("C", 30_000L)
            ),
            taxPercent = 11,
            servicePercent = 0
        )
        assertEquals(100_000L, result.subtotal)
        assertEquals(11_000L, result.taxAmount)
        assertEquals(111_000L, result.grandTotal)
        assertEquals(result.grandTotal, result.shares.sumOf { it.amount })
    }
}
