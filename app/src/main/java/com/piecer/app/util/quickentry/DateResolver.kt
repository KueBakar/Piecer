package com.piecer.app.util.quickentry

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class DateResolution(
    val dateMillis: Long?,
    val remainingText: String,
    val matchedSpan: IntRange? = null
)

object DateResolver {

    val dateKeywordsSet: Set<String> = setOf(
        "hari", "ini", "tadi", "pagi", "siang", "sore", "malam",
        "kemarin", "lusa", "besok", "tanggal", "minggu", "bulan", "lalu",
        "senin", "selasa", "rabu", "kamis", "jumat", "sabtu"
    )

    private val dayOfWeekMap: Map<String, DayOfWeek> = mapOf(
        "senin" to DayOfWeek.MONDAY,
        "selasa" to DayOfWeek.TUESDAY,
        "rabu" to DayOfWeek.WEDNESDAY,
        "kamis" to DayOfWeek.THURSDAY,
        "jumat" to DayOfWeek.FRIDAY,
        "sabtu" to DayOfWeek.SATURDAY,
        "minggu" to DayOfWeek.SUNDAY
    )

    private val todayPhraseRegex = Regex(
        """\b(?:tadi\s+(?:pagi|siang|sore|malam)|hari\s+ini)\b"""
    )
    private val yesterdayBeforeRegex = Regex("""\bkemarin\s+lusa\b""")
    private val yesterdayRegex = Regex("""\bkemarin\b""")
    private val dayAfterTomorrowRegex = Regex("""\blusa\b""")
    private val tomorrowRegex = Regex("""\bbesok\b""")
    private val lastWeekRegex = Regex("""\bminggu\s+lalu\b""")
    private val lastMonthRegex = Regex("""\bbulan\s+lalu\b""")
    private val dateDayRegex = Regex("""\btanggal\s+(\d{1,2})\b""")
    private val dayNameRegex = Regex(
        """\b(?:hari\s+)?(senin|selasa|rabu|kamis|jumat|sabtu|minggu)(?:\s+(?:kemarin|lalu))?\b"""
    )

    /**
     * Mengekstrak tanggal dari teks yang sudah dinormalisasi.
     *
     * Aturan "tanggal X":
     * - Menggunakan bulan berjalan dengan tanggal X (di-clamp ke jumlah hari di bulan tersebut).
     * - Bila X lebih besar dari tanggal hari ini (`day > today.dayOfMonth`), diasumsikan
     *   merujuk ke bulan lalu (di-clamp ke jumlah hari di bulan lalu).
     *
     * Tanggal masa depan (misal "lusa", "besok") menghasilkan `dateMillis` di masa depan
     * sehingga otomatis diperlakukan sebagai transaksi terjadwal oleh Piecer.
     */
    fun resolve(
        normalizedText: String,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DateResolution {
        if (normalizedText.isBlank()) {
            return DateResolution(dateMillis = null, remainingText = normalizedText)
        }

        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        var working = normalizedText
        var resolvedDate: LocalDate? = null
        var matchedRange: IntRange? = null

        // 1. "tanggal X"
        val dateDayMatch = dateDayRegex.find(working)
        if (dateDayMatch != null) {
            val day = dateDayMatch.groupValues[1].toIntOrNull()
            if (day != null && day in 1..31) {
                resolvedDate = if (day > today.dayOfMonth) {
                    val lastMonth = today.minusMonths(1)
                    lastMonth.withDayOfMonth(day.coerceAtMost(lastMonth.lengthOfMonth()))
                } else {
                    today.withDayOfMonth(day.coerceAtMost(today.lengthOfMonth()))
                }
                matchedRange = dateDayMatch.range
                working = working.removeRange(dateDayMatch.range)
            }
        }

        // 2. "kemarin lusa" (harus sebelum "kemarin" dan "lusa")
        if (resolvedDate == null) {
            val m = yesterdayBeforeRegex.find(working)
            if (m != null) {
                resolvedDate = today.minusDays(2)
                matchedRange = m.range
                working = working.removeRange(m.range)
            }
        }

        // 3. "minggu lalu" & "bulan lalu" (harus sebelum nama hari "minggu")
        if (resolvedDate == null) {
            val mWeek = lastWeekRegex.find(working)
            if (mWeek != null) {
                resolvedDate = today.minusWeeks(1)
                matchedRange = mWeek.range
                working = working.removeRange(mWeek.range)
            } else {
                val mMonth = lastMonthRegex.find(working)
                if (mMonth != null) {
                    resolvedDate = today.minusMonths(1)
                    matchedRange = mMonth.range
                    working = working.removeRange(mMonth.range)
                }
            }
        }

        // 4. Nama hari ("senin kemarin", "hari rabu", "jumat") sebelum "kemarin" berdiri sendiri
        if (resolvedDate == null) {
            for (m in dayNameRegex.findAll(working)) {
                val dayWord = m.groupValues[1]
                // Hindari salah tafsir "2 minggu" sebagai hari Minggu
                if (dayWord == "minggu") {
                    val before = working.substring(0, m.range.first).trimEnd()
                    val lastWord = before.substringAfterLast(' ', before)
                    val isQuantityBefore = lastWord.all { it.isDigit() } && lastWord.isNotEmpty() ||
                        lastWord in NumberParser.numberWordsSet
                    if (isQuantityBefore && !m.value.contains("hari") && !m.value.contains("kemarin") && !m.value.contains("lalu")) {
                        continue
                    }
                }
                val dow = dayOfWeekMap[dayWord] ?: continue
                resolvedDate = today.with(TemporalAdjusters.previous(dow))
                matchedRange = m.range
                working = working.removeRange(m.range)
                break
            }
        }

        // 5. "kemarin", "lusa", "besok", "hari ini", "tadi pagi/siang/sore/malam"
        if (resolvedDate == null) {
            val mYesterday = yesterdayRegex.find(working)
            val mLusa = dayAfterTomorrowRegex.find(working)
            val mTomorrow = tomorrowRegex.find(working)
            val mToday = todayPhraseRegex.find(working)

            when {
                mYesterday != null -> {
                    resolvedDate = today.minusDays(1)
                    matchedRange = mYesterday.range
                    working = working.removeRange(mYesterday.range)
                }
                mLusa != null -> {
                    resolvedDate = today.plusDays(2)
                    matchedRange = mLusa.range
                    working = working.removeRange(mLusa.range)
                }
                mTomorrow != null -> {
                    resolvedDate = today.plusDays(1)
                    matchedRange = mTomorrow.range
                    working = working.removeRange(mTomorrow.range)
                }
                mToday != null -> {
                    resolvedDate = today
                    matchedRange = mToday.range
                    working = working.removeRange(mToday.range)
                }
            }
        }

        val dateMillis = resolvedDate?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli()
        val cleaned = working.replace(Regex("""\s+"""), " ").trim()
        return DateResolution(dateMillis = dateMillis, remainingText = cleaned, matchedSpan = matchedRange)
    }
}
