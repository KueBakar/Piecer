package com.piecer.app.util

/**
 * Utilitas keypad kustom untuk input nominal rupiah (Long, rupiah penuh).
 */
object KeypadUtils {
    const val MAX_AMOUNT: Long = 999_999_999_999L

    fun appendDigit(
        currentAmount: Long,
        digit: Int,
        maxAmount: Long = MAX_AMOUNT
    ): Long {
        if (digit !in 0..9) return currentAmount
        if (currentAmount < 0L) return digit.toLong().coerceAtMost(maxAmount)
        if (currentAmount == 0L) return digit.toLong().coerceAtMost(maxAmount)
        if (currentAmount > (maxAmount - digit) / 10L) return currentAmount
        return currentAmount * 10L + digit
    }

    fun appendTripleZero(
        currentAmount: Long,
        maxAmount: Long = MAX_AMOUNT
    ): Long {
        if (currentAmount <= 0L) return 0L
        if (currentAmount > maxAmount / 1000L) return currentAmount
        return currentAmount * 1000L
    }

    fun backspace(currentAmount: Long): Long {
        if (currentAmount <= 0L) return 0L
        return currentAmount / 10L
    }
}
