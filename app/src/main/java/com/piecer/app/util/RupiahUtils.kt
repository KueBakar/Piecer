package com.piecer.app.util

/**
 * Memformat nominal rupiah (Long, rupiah penuh) menjadi string seperti "Rp1.250.000".
 * Untuk nilai negatif menghasilkan "-Rp1.250.000".
 */
fun formatRupiah(amount: Long): String {
    val isNegative = amount < 0L
    val digits = if (amount == Long.MIN_VALUE) {
        "9223372036854775808"
    } else {
        kotlin.math.abs(amount).toString()
    }

    val grouped = buildString(digits.length + digits.length / 3) {
        val len = digits.length
        for (i in 0 until len) {
            if (i > 0 && (len - i) % 3 == 0) {
                append('.')
            }
            append(digits[i])
        }
    }

    return if (isNegative) "-Rp$grouped" else "Rp$grouped"
}

/**
 * Mengurai input teks rupiah menjadi Long (rupiah penuh) tanpa menggunakan Double/Float.
 * Mendukung format seperti: "25000", "25.000", "Rp1.250.000", "25rb", "25k", "1,5jt".
 * Mengembalikan null jika input kosong atau tidak valid.
 */
fun parseRupiahInput(input: String): Long? {
    var text = input.trim().lowercase()
    if (text.isEmpty()) return null

    if (text.startsWith("rp")) {
        text = text.removePrefix("rp").trim()
    }
    text = text.replace(" ", "")
    if (text.isEmpty()) return null

    val multiplier: Long
    val numberPart: String
    when {
        text.endsWith("ribu") -> {
            multiplier = 1_000L
            numberPart = text.removeSuffix("ribu")
        }
        text.endsWith("rb") -> {
            multiplier = 1_000L
            numberPart = text.removeSuffix("rb")
        }
        text.endsWith("k") -> {
            multiplier = 1_000L
            numberPart = text.removeSuffix("k")
        }
        text.endsWith("juta") -> {
            multiplier = 1_000_000L
            numberPart = text.removeSuffix("juta")
        }
        text.endsWith("jt") -> {
            multiplier = 1_000_000L
            numberPart = text.removeSuffix("jt")
        }
        else -> {
            multiplier = 1L
            numberPart = text
        }
    }

    if (numberPart.isEmpty()) return null

    return parseNumberPart(numberPart, multiplier)
}

private fun parseNumberPart(raw: String, multiplier: Long): Long? {
    if (raw.any { !it.isDigit() && it != '.' && it != ',' }) return null

    // Pisahkan bagian bulat dan desimal bila ada koma (atau titik desimal saat memakai akhiran)
    val decimalIndex = findDecimalSeparatorIndex(raw, hasMultiplier = multiplier > 1L)

    val intRaw: String
    val fracRaw: String
    if (decimalIndex >= 0) {
        intRaw = raw.substring(0, decimalIndex)
        fracRaw = raw.substring(decimalIndex + 1)
        if (fracRaw.isEmpty() || !fracRaw.all { it.isDigit() }) return null
    } else {
        intRaw = raw
        fracRaw = ""
    }

    val cleanInt = intRaw.replace(".", "")
    if (cleanInt.isEmpty() || !cleanInt.all { it.isDigit() }) return null

    val whole = cleanInt.toLongOrNull() ?: return null
    val wholeScaled = multiplyExactOrNull(whole, multiplier) ?: return null

    if (fracRaw.isEmpty()) return wholeScaled

    // Hitung pecahan secara integer murni: frac * multiplier / 10^fracRaw.length
    var divisor = 1L
    var fracValue = 0L
    for (ch in fracRaw) {
        if (divisor >= multiplier) break
        divisor *= 10L
        fracValue = fracValue * 10L + (ch - '0')
    }
    val fracScaled = (fracValue * multiplier) / divisor
    return addExactOrNull(wholeScaled, fracScaled)
}

private fun findDecimalSeparatorIndex(raw: String, hasMultiplier: Boolean): Int {
    val commaCount = raw.count { it == ',' }
    if (commaCount > 1) return -1
    if (commaCount == 1) {
        return raw.indexOf(',')
    }

    if (hasMultiplier) {
        val dotCount = raw.count { it == '.' }
        if (dotCount == 1) {
            val dotIdx = raw.indexOf('.')
            val afterDot = raw.substring(dotIdx + 1)
            // Jika setelah titik bukan 3 digit, anggap sebagai pemisah desimal (mis. "1.5jt")
            if (afterDot.length != 3) {
                return dotIdx
            }
        }
    }
    return -1
}

private fun multiplyExactOrNull(a: Long, b: Long): Long? {
    if (a == 0L || b == 0L) return 0L
    val result = a * b
    return if (result / b == a) result else null
}

private fun addExactOrNull(a: Long, b: Long): Long? {
    val result = a + b
    return if (((a xor result) and (b xor result)) < 0L) null else result
}
