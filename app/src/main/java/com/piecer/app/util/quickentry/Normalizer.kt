package com.piecer.app.util.quickentry

import java.util.Locale

object Normalizer {

    private val idLocale = Locale("id", "ID")

    private val abbreviations: Map<String, String> = mapOf(
        "dpt" to "dapat",
        "dpet" to "dapat",
        "dapet" to "dapat",
        "dapetin" to "dapat",
        "didapet" to "dapat",
        "didapat" to "dapat",
        "byr" to "bayar",
        "trf" to "transfer",
        "tf" to "transfer",
        "trnsfr" to "transfer",
        "mkn" to "makan",
        "mnm" to "minum",
        "kmrn" to "kemarin",
        "kmren" to "kemarin",
        "kmarin" to "kemarin",
        "tgl" to "tanggal",
        "hr" to "hari",
        "bln" to "bulan",
        "thn" to "tahun",
        "mgg" to "minggu",
        "mggu" to "minggu",
        "yg" to "yang",
        "utk" to "untuk",
        "bwt" to "buat",
        "bt" to "buat",
        "sm" to "sama",
        "sma" to "sama",
        "dgn" to "dengan",
        "dg" to "dengan",
        "gk" to "tidak",
        "ga" to "tidak",
        "gak" to "tidak",
        "nggak" to "tidak",
        "ngga" to "tidak",
        "tdk" to "tidak",
        "dr" to "dari",
        "dri" to "dari",
        "dlm" to "dalam",
        "pd" to "pada",
        "sdh" to "sudah",
        "udh" to "sudah",
        "udah" to "sudah",
        "blm" to "belum",
        "bgt" to "banget",
        "bbrp" to "beberapa",
        "org" to "orang",
        "brg" to "barang",
        "blj" to "belanja",
        "blnja" to "belanja",
        "pk" to "pakai",
        "pke" to "pakai",
        "pake" to "pakai",
        "skrg" to "sekarang",
        "skr" to "sekarang",
        "bsk" to "besok",
        "jg" to "juga",
        "aja" to "saja",
        "aj" to "saja",
        "kalo" to "kalau",
        "kl" to "kalau",
        "klo" to "kalau",
        "krn" to "karena",
        "karna" to "karena",
        "tp" to "tapi",
        "tpi" to "tapi",
        "trs" to "terus",
        "dtg" to "datang",
        "krm" to "kirim",
        "trm" to "terima",
        "bpk" to "bapak",
        "sy" to "saya",
        "gw" to "aku",
        "gue" to "aku",
        "ane" to "aku",
        "spt" to "seperti",
        "ttg" to "tentang",
        "sptnya" to "sepertinya",
        "msh" to "masih",
        "lg" to "lagi",
        "lgi" to "lagi",
        "sklh" to "sekolah",
        "klh" to "kuliah",
        "kntr" to "kantor",
        "rmh" to "rumah",
        "mtr" to "motor",
        "mbl" to "mobil",
        "bsn" to "bensin",
        "lstrk" to "listrik",
        "pls" to "pulsa"
    )

    private val spokenForms: Map<String, String> = mapOf(
        "nyewa" to "sewa",
        "ngopi" to "kopi",
        "nyicil" to "cicil",
        "dibayarin" to "bayar",
        "ngebayar" to "bayar",
        "ngebyr" to "bayar",
        "mbayar" to "bayar",
        "ngekos" to "kos",
        "ngeteh" to "teh",
        "ngegrab" to "grab",
        "ngojek" to "ojek",
        "ngojol" to "ojek",
        "nyumbang" to "sumbang",
        "ngasih" to "kasih",
        "dikasihin" to "dikasih",
        "nerima" to "terima",
        "diterima" to "terima",
        "ditransferin" to "ditransfer",
        "ngegame" to "game",
        "belanjain" to "belanja",
        "nyuci" to "cuci",
        "nambal" to "tambal",
        "nyervis" to "servis",
        "nyervice" to "servis",
        "ngisi" to "isi",
        "makanin" to "makan",
        "jajanin" to "jajan",
        "minumin" to "minum",
        "ngirim" to "kirim",
        "nyimpan" to "simpan",
        "nabung" to "tabung",
        "minjem" to "pinjam",
        "nyetak" to "cetak",
        "ngeprint" to "print",
        "ngeles" to "les",
        "nontonin" to "nonton",
        "nyalon" to "salon",
        "ngobat" to "obat",
        "periksa" to "dokter"
    )

    private val rpPrefixRegex = Regex("""(?<![a-z])rp\.?\s*(?=\d)""")
    private val rpStandaloneRegex = Regex("""\b(?:rp|rupiah)\b\.?""")
    private val strayPunctRegex = Regex("""[!?"'()\[\]{}:~]""")
    private val multiSpaceRegex = Regex("""[ \t]+""")
    private val commaBetweenWordsRegex = Regex("""(?<!\d),(?!\d)|(?<=\d),(?!\d{1,3}(?:\D|$))""")
    private val dotAfterWordRegex = Regex("""(?<!\d)\.(?!\d)""")

    // Lazy dictionary for typo correction (built once from known vocabulary)
    private val knownWordsByLength: Map<Int, List<String>> by lazy {
        val allWords = buildSet {
            addAll(abbreviations.values)
            addAll(spokenForms.values)
            addAll(NumberParser.numberWordsSet)
            addAll(DateResolver.dateKeywordsSet)
            addAll(TypeDetector.allKeywordsSet)
            addAll(AmountExtractor.quantityUnitsSet)
            addAll(CategoryResolver.allDictionarySingleWords)
            addAll(WalletResolver.allKnownAliases)
        }.filter { it.length >= 4 && it.all { ch -> ch in 'a'..'z' } }
        allWords.groupBy { it.length }
    }

    private val exactKnownWords: Set<String> by lazy {
        buildSet {
            addAll(abbreviations.values)
            addAll(spokenForms.values)
            addAll(NumberParser.numberWordsSet)
            addAll(DateResolver.dateKeywordsSet)
            addAll(TypeDetector.allKeywordsSet)
            addAll(AmountExtractor.quantityUnitsSet)
            addAll(CategoryResolver.allDictionarySingleWords)
            addAll(WalletResolver.allKnownAliases)
            add("dari")
            add("ke")
            add("pakai")
            add("dengan")
            add("untuk")
            add("buat")
            add("sama")
            add("yang")
            add("dan")
            add("terus")
            add("lalu")
            add("total")
            add("semua")
            add("jadi")
            add("beli")
            add("bayar")
            add("jual")
            add("ortu")
        }
    }

    fun normalize(input: String, extraKnownWords: Set<String> = emptySet()): String {
        if (input.isBlank()) return ""

        var text = input
            .lowercase(idLocale)
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        // Hapus "rp" / "rupiah"
        text = rpPrefixRegex.replace(text, "")
        text = rpStandaloneRegex.replace(text, " ")

        // Rapikan tanda baca pengganggu tapi pertahankan koma/titik angka, +, ;, \n, @
        text = strayPunctRegex.replace(text, " ")
        text = dotAfterWordRegex.replace(text, " ")

        // Pisahkan delimiter koma/titik-koma/plus agar tokenisasi akurat
        text = text.replace(";", " ; ")
            .replace("+", " + ")
            .replace("\n", " \n ")

        // Pisahkan koma yang bukan pemisah angka (misal "bakso 20k, es teh 5k" -> "bakso 20k , es teh 5k")
        text = commaBetweenWordsRegex.replace(text, " , ")

        val lines = text.split('\n')
        val normalizedLines = lines.map { line ->
            val rawTokens = line.trim().split(multiSpaceRegex).filter { it.isNotEmpty() }
            val processed = rawTokens.map { token ->
                normalizeToken(token, extraKnownWords)
            }
            processed.joinToString(" ")
        }

        return normalizedLines
            .joinToString("\n")
            .replace(Regex(""" *\n *"""), "\n")
            .trim()
    }

    private fun normalizeToken(token: String, extraKnownWords: Set<String>): String {
        if (token == "," || token == ";" || token == "+" || token == "@") return token
        // Jangan sentuh token yang mengandung angka (misal 25k, 1,5jt, 3x15k, 25.000)
        if (token.any { it.isDigit() }) return token

        val clean = token.trim(',', '.', ';', '+', '-')
        if (clean.isEmpty()) return token

        abbreviations[clean]?.let { return it }
        spokenForms[clean]?.let { return it }

        if (clean in exactKnownWords || clean in extraKnownWords) return clean

        // Koreksi typo ringan hanya untuk kata alfabetis >= 5 huruf
        if (clean.length >= 5 && clean.all { it in 'a'..'z' }) {
            val maxDist = if (clean.length >= 8) 2 else 1
            val corrected = findClosestDictionaryWord(clean, maxDist)
            if (corrected != null) return corrected
        }

        return clean
    }

    private fun findClosestDictionaryWord(word: String, maxDist: Int): String? {
        var bestWord: String? = null
        var bestDist = maxDist + 1

        for (len in (word.length - maxDist)..(word.length + maxDist)) {
            val bucket = knownWordsByLength[len] ?: continue
            for (candidate in bucket) {
                val dist = levenshteinDistance(word, candidate, maxDist)
                if (dist < bestDist) {
                    bestDist = dist
                    bestWord = candidate
                    if (dist == 1 && maxDist == 1) return bestWord
                }
            }
        }
        return bestWord
    }

    fun levenshteinDistance(a: String, b: String, maxDist: Int = Int.MAX_VALUE): Int {
        val lenA = a.length
        val lenB = b.length
        if (kotlin.math.abs(lenA - lenB) > maxDist) return maxDist + 1
        if (a == b) return 0
        if (lenA == 0) return lenB
        if (lenB == 0) return lenA

        var prev = IntArray(lenB + 1) { it }
        var curr = IntArray(lenB + 1)

        for (i in 1..lenA) {
            curr[0] = i
            var minInRow = curr[0]
            val ca = a[i - 1]
            for (j in 1..lenB) {
                val cost = if (ca == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    prev[j] + 1,
                    curr[j - 1] + 1,
                    prev[j - 1] + cost
                )
                if (curr[j] < minInRow) minInRow = curr[j]
            }
            if (minInRow > maxDist) return maxDist + 1
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[lenB]
    }
}
