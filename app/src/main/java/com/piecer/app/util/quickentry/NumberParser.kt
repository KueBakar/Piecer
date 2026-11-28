package com.piecer.app.util.quickentry

data class NumberMatch(
    val value: Long,
    val hasMoneyUnit: Boolean,
    val isAmbiguousM: Boolean,
    val isExplicitTotal: Boolean,
    val isMultiplication: Boolean,
    val range: IntRange,
    val rawText: String
)

object NumberParser {

    val slangAmounts: Map<String, Long> = mapOf(
        "goceng" to 5_000L,
        "ceban" to 10_000L,
        "noban" to 20_000L,
        "gocap" to 50_000L,
        "gopek" to 500L,
        "cepek" to 100L
    )

    private val unitDigits: Map<String, Long> = mapOf(
        "nol" to 0L,
        "satu" to 1L,
        "dua" to 2L,
        "tiga" to 3L,
        "empat" to 4L,
        "lima" to 5L,
        "enam" to 6L,
        "tujuh" to 7L,
        "delapan" to 8L,
        "sembilan" to 9L,
        "sepuluh" to 10L,
        "sebelas" to 11L
    )

    private val compoundWords: Map<String, Long> = mapOf(
        "seratus" to 100L,
        "seribu" to 1_000L,
        "serebu" to 1_000L,
        "sejuta" to 1_000_000L,
        "semiliar" to 1_000_000_000L,
        "semilyar" to 1_000_000_000L
    )

    private val scaleWords: Map<String, Long> = mapOf(
        "ribu" to 1_000L,
        "rebu" to 1_000L,
        "rb" to 1_000L,
        "k" to 1_000L,
        "juta" to 1_000_000L,
        "jt" to 1_000_000L,
        "miliar" to 1_000_000_000L,
        "milyar" to 1_000_000_000L
    )

    val numberWordsSet: Set<String> = buildSet {
        addAll(slangAmounts.keys)
        addAll(unitDigits.keys)
        addAll(compoundWords.keys)
        addAll(scaleWords.keys)
        add("belas")
        add("puluh")
        add("ratus")
        add("setengah")
    }

    private val totalPrefixRegex = Regex("""\b(?:total|semua|jadi)\s+$""")

    // Perkalian: "3x15k", "3 x 15k", "3 @ 15k", "3@15.000", "3 x 1,5jt"
    private val multiplicationRegex = Regex(
        """(?<![\d.,])\b(\d+)\s*[xX@]\s*(\d+(?:[.,]\d+)?)\s*(rb|ribu|rebu|k|jt|juta|miliar|milyar|m)?\b""",
        RegexOption.IGNORE_CASE
    )

    // Angka dengan satuan: 1,5jt, 1.5jt, 25k, 25rb, 25.000rb, 1m, 2miliar
    private val scaledNumberRegex = Regex(
        """(?<![\d.,])\b(\d{1,3}(?:[.,]\d{3})+|\d+(?:[.,]\d+)?)\s*(rb|ribu|rebu|k|jt|juta|miliar|milyar|m)\b""",
        RegexOption.IGNORE_CASE
    )

    // Angka polos dengan atau tanpa pemisah ribuan: 25000, 25.000, 25,000
    private val plainNumberRegex = Regex(
        """(?<![\d.,])\b(\d{1,3}(?:[.,]\d{3})+|\d+)\b(?![.,]\d)"""
    )

    /**
     * Mencari semua kandidat angka/nominal dalam teks yang sudah dinormalisasi.
     * Hasil diurutkan berdasarkan posisi kemunculan (range.first).
     */
    fun findAllNumbers(normalizedText: String): List<NumberMatch> {
        if (normalizedText.isBlank()) return emptyList()

        val matches = mutableListOf<NumberMatch>()
        val occupied = mutableListOf<IntRange>()

        fun isOccupied(range: IntRange): Boolean =
            occupied.any { it.first <= range.last && range.first <= it.last }

        fun hasTotalPrefix(startIndex: Int): Boolean {
            if (startIndex <= 0) return false
            val prefix = normalizedText.substring(0, startIndex)
            return totalPrefixRegex.containsMatchIn(prefix)
        }

        // 1. Perkalian (3x15k, 3 x 15k, 3 @ 15k)
        for (m in multiplicationRegex.findAll(normalizedText)) {
            if (isOccupied(m.range)) continue
            val count = m.groupValues[1].toLongOrNull() ?: continue
            val unitPriceStr = m.groupValues[2]
            val unit = m.groupValues[3].lowercase()
            val (multiplier, isAmbM) = unitMultiplier(unit)
            val unitPrice = parseNumericWithOptionalMultiplier(unitPriceStr, multiplier) ?: continue
            val total = safeMultiply(count, unitPrice) ?: continue
            if (total > 0L) {
                matches += NumberMatch(
                    value = total,
                    hasMoneyUnit = unit.isNotEmpty() || unitPrice >= 100L,
                    isAmbiguousM = isAmbM,
                    isExplicitTotal = true,
                    isMultiplication = true,
                    range = m.range,
                    rawText = m.value
                )
                occupied += m.range
            }
        }

        // 2. Angka dengan satuan (25k, 1,5jt, 1m, 2miliar)
        for (m in scaledNumberRegex.findAll(normalizedText)) {
            if (isOccupied(m.range)) continue
            val numStr = m.groupValues[1]
            val unit = m.groupValues[2].lowercase()
            val (multiplier, isAmbM) = unitMultiplier(unit)
            val value = parseNumericWithOptionalMultiplier(numStr, multiplier) ?: continue
            if (value > 0L) {
                matches += NumberMatch(
                    value = value,
                    hasMoneyUnit = true,
                    isAmbiguousM = isAmbM,
                    isExplicitTotal = hasTotalPrefix(m.range.first),
                    isMultiplication = false,
                    range = m.range,
                    rawText = m.value
                )
                occupied += m.range
            }
        }

        // 3. Kata bilangan Indonesia (dua puluh ribu, seratus lima puluh ribu, setengah juta, sejuta)
        for (wm in findIndonesianWordNumbers(normalizedText)) {
            if (isOccupied(wm.range)) continue
            matches += wm.copy(isExplicitTotal = hasTotalPrefix(wm.range.first))
            occupied += wm.range
        }

        // 4. Slang berdiri sendiri (goceng, ceban, noban, gocap, gopek, cepek)
        for ((slang, value) in slangAmounts) {
            val regex = Regex("""\b$slang\b""")
            for (m in regex.findAll(normalizedText)) {
                if (isOccupied(m.range)) continue
                matches += NumberMatch(
                    value = value,
                    hasMoneyUnit = true,
                    isAmbiguousM = false,
                    isExplicitTotal = hasTotalPrefix(m.range.first),
                    isMultiplication = false,
                    range = m.range,
                    rawText = m.value
                )
                occupied += m.range
            }
        }

        // 5. Angka polos (25000, 25.000, 25,000, atau angka kecil untuk kuantitas)
        for (m in plainNumberRegex.findAll(normalizedText)) {
            if (isOccupied(m.range)) continue
            val raw = m.groupValues[1]
            val digitsOnly = raw.replace(".", "").replace(",", "")
            val value = digitsOnly.toLongOrNull() ?: continue
            if (value > 0L) {
                matches += NumberMatch(
                    value = value,
                    hasMoneyUnit = false,
                    isAmbiguousM = false,
                    isExplicitTotal = hasTotalPrefix(m.range.first),
                    isMultiplication = false,
                    range = m.range,
                    rawText = m.value
                )
                occupied += m.range
            }
        }

        return matches.sortedBy { it.range.first }
    }

    private fun unitMultiplier(unit: String): Pair<Long, Boolean> = when (unit.lowercase()) {
        "k", "rb", "ribu", "rebu" -> 1_000L to false
        "jt", "juta" -> 1_000_000L to false
        "miliar", "milyar" -> 1_000_000_000L to false
        "m" -> 1_000_000L to true
        else -> 1L to false
    }

    /**
     * Mengubah string angka (misal "1,5", "1.5", "25.000", "25,000") dengan pengali Long
     * menjadi Long penuh tanpa Double/Float.
     */
    fun parseNumericWithOptionalMultiplier(numStr: String, multiplier: Long): Long? {
        if (multiplier == 1L) {
            val cleaned = numStr.replace(".", "").replace(",", "")
            return cleaned.toLongOrNull()
        }

        // Cek apakah ini pemisah ribuan (misal 1.500 atau 1,500.000) vs desimal (1,5 atau 1.25)
        val thousandGroupedRegex = Regex("""^\d{1,3}(?:([.,])\d{3})+$""")
        if (thousandGroupedRegex.matches(numStr)) {
            val whole = numStr.replace(".", "").replace(",", "").toLongOrNull() ?: return null
            return safeMultiply(whole, multiplier)
        }

        val normalized = numStr.replace(',', '.')
        val parts = normalized.split('.')
        return when (parts.size) {
            1 -> {
                val whole = parts[0].toLongOrNull() ?: return null
                safeMultiply(whole, multiplier)
            }
            2 -> {
                val whole = parts[0].toLongOrNull() ?: return null
                val fracStr = parts[1]
                if (fracStr.isEmpty()) return safeMultiply(whole, multiplier)
                val fracDigits = fracStr.take(6)
                val fracValue = fracDigits.toLongOrNull() ?: return null
                var scale = 1L
                repeat(fracDigits.length) { scale *= 10L }
                val base = safeMultiply(whole, multiplier) ?: return null
                val fracPart = (fracValue * multiplier) / scale
                base + fracPart
            }
            else -> null
        }
    }

    private fun safeMultiply(a: Long, b: Long): Long? {
        if (a == 0L || b == 0L) return 0L
        if (a > Long.MAX_VALUE / b) return null
        return a * b
    }

    private data class TokenSpan(val word: String, val range: IntRange)

    private fun findIndonesianWordNumbers(text: String): List<NumberMatch> {
        val tokenRegex = Regex("""[^\s,;+]+""")
        val tokens = tokenRegex.findAll(text).map { TokenSpan(it.value, it.range) }.toList()
        if (tokens.isEmpty()) return emptyList()

        val results = mutableListOf<NumberMatch>()
        var i = 0
        while (i < tokens.size) {
            val startWord = tokens[i].word
            if (!isWordNumberStart(startWord)) {
                i++
                continue
            }

            var j = i
            while (j < tokens.size && isWordNumberPart(tokens[j].word)) {
                j++
            }
            val slice = tokens.subList(i, j)
            val parsed = parseWordNumberTokens(slice.map { it.word })
            if (parsed != null && parsed.first > 0L) {
                val startOffset = slice.first().range.first
                val endOffset = slice.last().range.last
                results += NumberMatch(
                    value = parsed.first,
                    hasMoneyUnit = parsed.second,
                    isAmbiguousM = false,
                    isExplicitTotal = false,
                    isMultiplication = false,
                    range = startOffset..endOffset,
                    rawText = text.substring(startOffset, endOffset + 1)
                )
                i = j
            } else {
                i++
            }
        }
        return results
    }

    private fun isWordNumberStart(word: String): Boolean =
        word in unitDigits || word in compoundWords || word == "setengah"

    private fun isWordNumberPart(word: String): Boolean =
        word in numberWordsSet

    /**
     * Mengembalikan Pair(nilai, hasMoneyUnit).
     * hasMoneyUnit bernilai true jika ada skala ribuan/jutaan/miliar ("ribu", "juta", "miliar", "sejuta", "seribu").
     */
    private fun parseWordNumberTokens(words: List<String>): Pair<Long, Boolean>? {
        if (words.isEmpty()) return null

        var total = 0L
        var current = 0L
        var hasScale = false
        var hasAnyValue = false
        var i = 0

        while (i < words.size) {
            val w = words[i]
            when {
                w == "setengah" -> {
                    val nextScale = if (i + 1 < words.size) scaleWords[words[i + 1]] else null
                    if (nextScale != null) {
                        val base = if (current > 0L) current * nextScale else 0L
                        total += base + (nextScale / 2L)
                        current = 0L
                        hasScale = true
                        hasAnyValue = true
                        i += 2
                        continue
                    } else {
                        return null
                    }
                }
                w in compoundWords -> {
                    val v = compoundWords.getValue(w)
                    if (v >= 1_000L) {
                        total += (current * v) + v
                        current = 0L
                        hasScale = true
                    } else {
                        current += v
                    }
                    hasAnyValue = true
                }
                w in unitDigits -> {
                    val d = unitDigits.getValue(w)
                    val next = words.getOrNull(i + 1)
                    when (next) {
                        "belas" -> {
                            current += 10L + d
                            i += 2
                            hasAnyValue = true
                            continue
                        }
                        "puluh" -> {
                            current += d * 10L
                            i += 2
                            hasAnyValue = true
                            continue
                        }
                        "ratus" -> {
                            current += d * 100L
                            i += 2
                            hasAnyValue = true
                            continue
                        }
                        else -> {
                            current += d
                            hasAnyValue = true
                        }
                    }
                }
                w in scaleWords -> {
                    val scale = scaleWords.getValue(w)
                    val mult = if (current == 0L) 1L else current
                    total += mult * scale
                    current = 0L
                    hasScale = true
                    hasAnyValue = true
                }
                else -> return null
            }
            i++
        }

        if (!hasAnyValue) return null
        val finalTotal = total + current
        return finalTotal to hasScale
    }
}
