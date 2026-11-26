package com.piecer.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RupiahUtilsTest {

    @Test
    fun `formatRupiah formats zero and positive amounts`() {
        assertEquals("Rp0", formatRupiah(0L))
        assertEquals("Rp500", formatRupiah(500L))
        assertEquals("Rp25.000", formatRupiah(25_000L))
        assertEquals("Rp1.250.000", formatRupiah(1_250_000L))
        assertEquals("Rp100.000.000", formatRupiah(100_000_000L))
    }

    @Test
    fun `formatRupiah formats negative amounts`() {
        assertEquals("-Rp25.000", formatRupiah(-25_000L))
        assertEquals("-Rp1.250.000", formatRupiah(-1_250_000L))
    }

    @Test
    fun `parseRupiahInput parses plain and dot-grouped numbers`() {
        assertEquals(25_000L, parseRupiahInput("25000"))
        assertEquals(25_000L, parseRupiahInput("25.000"))
        assertEquals(1_250_000L, parseRupiahInput("1.250.000"))
        assertEquals(1_250_000L, parseRupiahInput("Rp1.250.000"))
        assertEquals(1_250_000L, parseRupiahInput("  rp 1.250.000  "))
    }

    @Test
    fun `parseRupiahInput parses rb, k, and jt suffixes`() {
        assertEquals(25_000L, parseRupiahInput("25rb"))
        assertEquals(25_000L, parseRupiahInput("25k"))
        assertEquals(25_000L, parseRupiahInput("25K"))
        assertEquals(25_000L, parseRupiahInput("25 rb"))
        assertEquals(1_500_000L, parseRupiahInput("1,5jt"))
        assertEquals(1_500_000L, parseRupiahInput("1.5jt"))
        assertEquals(2_250_000L, parseRupiahInput("2,25jt"))
        assertEquals(12_500L, parseRupiahInput("12,5rb"))
    }

    @Test
    fun `parseRupiahInput returns null for invalid inputs`() {
        assertNull(parseRupiahInput(""))
        assertNull(parseRupiahInput("   "))
        assertNull(parseRupiahInput("abc"))
        assertNull(parseRupiahInput("rb"))
        assertNull(parseRupiahInput("-25000"))
        assertNull(parseRupiahInput("1,2,3jt"))
    }
}
