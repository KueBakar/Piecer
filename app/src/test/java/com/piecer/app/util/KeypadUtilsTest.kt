package com.piecer.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class KeypadUtilsTest {

    @Test
    fun appendDigit_buildsNumberFromZero() {
        var amount = 0L
        amount = KeypadUtils.appendDigit(amount, 2)
        amount = KeypadUtils.appendDigit(amount, 5)
        amount = KeypadUtils.appendDigit(amount, 0)
        assertEquals(250L, amount)
    }

    @Test
    fun appendDigit_ignoresInvalidDigits() {
        assertEquals(15L, KeypadUtils.appendDigit(15L, -1))
        assertEquals(15L, KeypadUtils.appendDigit(15L, 10))
    }

    @Test
    fun appendTripleZero_keepsZeroWhenAmountIsZero() {
        assertEquals(0L, KeypadUtils.appendTripleZero(0L))
    }

    @Test
    fun appendTripleZero_multipliesByThousand() {
        assertEquals(25_000L, KeypadUtils.appendTripleZero(25L))
    }

    @Test
    fun appendTripleZero_respectsMaxAmount() {
        val nearMax = KeypadUtils.MAX_AMOUNT
        assertEquals(nearMax, KeypadUtils.appendTripleZero(nearMax))
    }

    @Test
    fun backspace_removesLastDigitAndStopsAtZero() {
        assertEquals(12L, KeypadUtils.backspace(125L))
        assertEquals(0L, KeypadUtils.backspace(5L))
        assertEquals(0L, KeypadUtils.backspace(0L))
    }
}
