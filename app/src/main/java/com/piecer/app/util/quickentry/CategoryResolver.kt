package com.piecer.app.util.quickentry

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType

data class CategoryResolution(
    val categoryId: Long?,
    val candidates: List<CategoryCandidate>
)

object CategoryResolver {

    const val CONFIDENCE_THRESHOLD = 0.55f

    private val stopWords: Set<String> = setOf(
        "beli", "bayar", "pakai", "dari", "ke", "di", "untuk", "buat", "sama",
        "dengan", "yang", "dan", "atau", "total", "semua", "jadi", "aku", "saya",
        "hari", "ini", "tadi", "kemarin", "lusa", "besok", "minggu", "bulan", "tahun"
    )

    // Frasa gabungan berbobot lebih tinggi daripada kata tunggal
    private val phraseDictionary: Map<String, Pair<String, CategoryType>> = mapOf(
        // Makan
        "nasi padang" to ("Makan" to CategoryType.EXPENSE),
        "mie ayam" to ("Makan" to CategoryType.EXPENSE),
        "nasi goreng" to ("Makan" to CategoryType.EXPENSE),
        "ayam geprek" to ("Makan" to CategoryType.EXPENSE),
        "ayam bakar" to ("Makan" to CategoryType.EXPENSE),
        "ayam goreng" to ("Makan" to CategoryType.EXPENSE),
        "pecel lele" to ("Makan" to CategoryType.EXPENSE),
        "nasi uduk" to ("Makan" to CategoryType.EXPENSE),
        "nasi kuning" to ("Makan" to CategoryType.EXPENSE),
        "nasi campur" to ("Makan" to CategoryType.EXPENSE),
        "nasi rames" to ("Makan" to CategoryType.EXPENSE),
        "nasi pecel" to ("Makan" to CategoryType.EXPENSE),
        "es teh" to ("Makan" to CategoryType.EXPENSE),
        "es jeruk" to ("Makan" to CategoryType.EXPENSE),
        "es kopi" to ("Makan" to CategoryType.EXPENSE),
        "kopi susu" to ("Makan" to CategoryType.EXPENSE),
        "teh tarik" to ("Makan" to CategoryType.EXPENSE),
        "tahu tek" to ("Makan" to CategoryType.EXPENSE),
        "tahu gejrot" to ("Makan" to CategoryType.EXPENSE),
        "tahu bulat" to ("Makan" to CategoryType.EXPENSE),
        "gado gado" to ("Makan" to CategoryType.EXPENSE),
        "soto ayam" to ("Makan" to CategoryType.EXPENSE),
        "soto betawi" to ("Makan" to CategoryType.EXPENSE),
        "soto lamongan" to ("Makan" to CategoryType.EXPENSE),
        "sate ayam" to ("Makan" to CategoryType.EXPENSE),
        "sate padang" to ("Makan" to CategoryType.EXPENSE),
        "sate kambing" to ("Makan" to CategoryType.EXPENSE),
        "bakso urat" to ("Makan" to CategoryType.EXPENSE),
        "bakso aci" to ("Makan" to CategoryType.EXPENSE),
        "bubur ayam" to ("Makan" to CategoryType.EXPENSE),
        "bubur kacang" to ("Makan" to CategoryType.EXPENSE),
        "martabak manis" to ("Makan" to CategoryType.EXPENSE),
        "martabak telur" to ("Makan" to CategoryType.EXPENSE),
        "pisang goreng" to ("Makan" to CategoryType.EXPENSE),
        "pisang coklat" to ("Makan" to CategoryType.EXPENSE),
        "roti bakar" to ("Makan" to CategoryType.EXPENSE),
        "roti canai" to ("Makan" to CategoryType.EXPENSE),
        "mie goreng" to ("Makan" to CategoryType.EXPENSE),
        "mie rebus" to ("Makan" to CategoryType.EXPENSE),
        "mie gacoan" to ("Makan" to CategoryType.EXPENSE),
        "dimsum ayam" to ("Makan" to CategoryType.EXPENSE),
        "air mineral" to ("Makan" to CategoryType.EXPENSE),
        "air galon" to ("Makan" to CategoryType.EXPENSE),
        "makan siang" to ("Makan" to CategoryType.EXPENSE),
        "makan malam" to ("Makan" to CategoryType.EXPENSE),
        "makan pagi" to ("Makan" to CategoryType.EXPENSE),
        "pesan makan" to ("Makan" to CategoryType.EXPENSE),

        // Transport
        "sewa mobil" to ("Transport" to CategoryType.EXPENSE),
        "rental mobil" to ("Transport" to CategoryType.EXPENSE),
        "sewa motor" to ("Transport" to CategoryType.EXPENSE),
        "tambal ban" to ("Transport" to CategoryType.EXPENSE),
        "cuci motor" to ("Transport" to CategoryType.EXPENSE),
        "cuci mobil" to ("Transport" to CategoryType.EXPENSE),
        "servis motor" to ("Transport" to CategoryType.EXPENSE),
        "servis mobil" to ("Transport" to CategoryType.EXPENSE),
        "ganti oli" to ("Transport" to CategoryType.EXPENSE),
        "isi bensin" to ("Transport" to CategoryType.EXPENSE),
        "isi pertalite" to ("Transport" to CategoryType.EXPENSE),
        "isi pertamax" to ("Transport" to CategoryType.EXPENSE),
        "tiket kereta" to ("Transport" to CategoryType.EXPENSE),
        "tiket pesawat" to ("Transport" to CategoryType.EXPENSE),
        "tiket bus" to ("Transport" to CategoryType.EXPENSE),
        "kartu tol" to ("Transport" to CategoryType.EXPENSE),
        "bayar tol" to ("Transport" to CategoryType.EXPENSE),
        "bayar parkir" to ("Transport" to CategoryType.EXPENSE),
        "naik gojek" to ("Transport" to CategoryType.EXPENSE),
        "naik grab" to ("Transport" to CategoryType.EXPENSE),
        "naik ojek" to ("Transport" to CategoryType.EXPENSE),
        "naik kereta" to ("Transport" to CategoryType.EXPENSE),
        "naik bus" to ("Transport" to CategoryType.EXPENSE),

        // Tagihan
        "sewa kos" to ("Tagihan" to CategoryType.EXPENSE),
        "sewa kost" to ("Tagihan" to CategoryType.EXPENSE),
        "bayar kos" to ("Tagihan" to CategoryType.EXPENSE),
        "sewa rumah" to ("Tagihan" to CategoryType.EXPENSE),
        "sewa apartemen" to ("Tagihan" to CategoryType.EXPENSE),
        "paket data" to ("Tagihan" to CategoryType.EXPENSE),
        "token listrik" to ("Tagihan" to CategoryType.EXPENSE),
        "tagihan listrik" to ("Tagihan" to CategoryType.EXPENSE),
        "bayar listrik" to ("Tagihan" to CategoryType.EXPENSE),
        "bayar wifi" to ("Tagihan" to CategoryType.EXPENSE),
        "tagihan wifi" to ("Tagihan" to CategoryType.EXPENSE),
        "tagihan air" to ("Tagihan" to CategoryType.EXPENSE),
        "bayar air" to ("Tagihan" to CategoryType.EXPENSE),
        "bayar pdam" to ("Tagihan" to CategoryType.EXPENSE),
        "isi pulsa" to ("Tagihan" to CategoryType.EXPENSE),
        "beli pulsa" to ("Tagihan" to CategoryType.EXPENSE),
        "bayar bpjs" to ("Tagihan" to CategoryType.EXPENSE),
        "bayar cicilan" to ("Tagihan" to CategoryType.EXPENSE),
        "cicilan motor" to ("Tagihan" to CategoryType.EXPENSE),
        "cicil motor" to ("Tagihan" to CategoryType.EXPENSE),
        "cicilan mobil" to ("Tagihan" to CategoryType.EXPENSE),
        "cicilan rumah" to ("Tagihan" to CategoryType.EXPENSE),
        "iuran bulanan" to ("Tagihan" to CategoryType.EXPENSE),

        // Belanja
        "beli baju" to ("Belanja" to CategoryType.EXPENSE),
        "beli sepatu" to ("Belanja" to CategoryType.EXPENSE),
        "beli celana" to ("Belanja" to CategoryType.EXPENSE),
        "beli kado" to ("Belanja" to CategoryType.EXPENSE),
        "belanja bulanan" to ("Belanja" to CategoryType.EXPENSE),
        "belanja mingguan" to ("Belanja" to CategoryType.EXPENSE),
        "perlengkapan mandi" to ("Belanja" to CategoryType.EXPENSE),
        "alat rumah" to ("Belanja" to CategoryType.EXPENSE),

        // Hiburan
        "nonton bioskop" to ("Hiburan" to CategoryType.EXPENSE),
        "tiket bioskop" to ("Hiburan" to CategoryType.EXPENSE),
        "tiket konser" to ("Hiburan" to CategoryType.EXPENSE),
        "topup game" to ("Hiburan" to CategoryType.EXPENSE),
        "top up" to ("Hiburan" to CategoryType.EXPENSE),
        "main game" to ("Hiburan" to CategoryType.EXPENSE),

        // Kesehatan
        "beli obat" to ("Kesehatan" to CategoryType.EXPENSE),
        "cek darah" to ("Kesehatan" to CategoryType.EXPENSE),
        "cek kesehatan" to ("Kesehatan" to CategoryType.EXPENSE),
        "dokter gigi" to ("Kesehatan" to CategoryType.EXPENSE),
        "rumah sakit" to ("Kesehatan" to CategoryType.EXPENSE),
        "beli vitamin" to ("Kesehatan" to CategoryType.EXPENSE),

        // Pendidikan
        "bayar spp" to ("Pendidikan" to CategoryType.EXPENSE),
        "bayar ukt" to ("Pendidikan" to CategoryType.EXPENSE),
        "uang kuliah" to ("Pendidikan" to CategoryType.EXPENSE),
        "uang sekolah" to ("Pendidikan" to CategoryType.EXPENSE),
        "beli buku" to ("Pendidikan" to CategoryType.EXPENSE),
        "alat tulis" to ("Pendidikan" to CategoryType.EXPENSE),

        // Pemasukan
        "dikasih ortu" to ("Hadiah" to CategoryType.INCOME),
        "dikasih orang tua" to ("Hadiah" to CategoryType.INCOME),
        "transfer masuk" to ("Lainnya" to CategoryType.INCOME),
        "gaji bulanan" to ("Gaji" to CategoryType.INCOME),
        "dapat bonus" to ("Bonus" to CategoryType.INCOME),
        "dapat thr" to ("Bonus" to CategoryType.INCOME),
        "dapat angpao" to ("Hadiah" to CategoryType.INCOME),
        "dapat hadiah" to ("Hadiah" to CategoryType.INCOME),
        "dapat cashback" to ("Lainnya" to CategoryType.INCOME)
    )

    // Kamus kata tunggal besar (>= 400 kata/frasa total)
    private val expenseKeywords: Map<String, List<String>> = mapOf(
        "Makan" to listOf(
            "makan", "minum", "sarapan", "jajan", "snack", "cemilan", "kuliner", "catering",
            "bakso", "geprek", "cimol", "seblak", "batagor", "siomay", "martabak", "gorengan",
            "boba", "kopi", "soto", "penyetan", "warteg", "kafe", "cafe", "resto", "restoran",
            "warung", "kantin", "gofood", "grabfood", "shopeefood",
            "nasi", "mie", "bihun", "kwetiau", "lontong", "ketupat", "bubur", "roti", "kue",
            "donat", "biskuit", "keripik", "kerupuk", "cireng", "cilok", "cilor", "maklor",
            "pempek", "tekwan", "model", "selat", "rawon", "rendang", "gudeg", "tongseng",
            "gulai", "opor", "semur", "sate", "ayam", "bebek", "ikan", "lele", "nila",
            "gurame", "udang", "cumi", "kepiting", "daging", "iga", "buntut", "steak",
            "burger", "pizza", "pasta", "spaghetti", "ramen", "udon", "sushi", "takoyaki",
            "dimsum", "gyoza", "kebab", "shawarma", "taco", "salad", "sandwich", "croissant",
            "teh", "matcha", "latte", "americano", "cappuccino", "espresso", "mocha", "thai",
            "jus", "smoothie", "milkshake", "yogurt", "susu", "coklat", "sirup", "soda",
            "galon", "aqua", "mineral", "air", "eskrim", "gelato", "pudding", "agar", "kolak",
            "pisang", "singkong", "ubi", "tahu", "tempe", "telur", "sayur", "buah",
            "pecel", "gado", "ketoprak", "lotek", "karedok", "rujak", "asinan", "bakmi",
            "capcay", "fuyunghai", "nasgor", "miegor", "laksa", "konro", "coto", "pallubasa",
            "wagyu", "barbeque", "grill", "shabu", "yakiniku", "teriyaki", "katsu", "bento"
        ),
        "Transport" to listOf(
            "transport", "transportasi", "parkir", "bensin", "bbm", "pertalite", "pertamax",
            "solar", "dex", "dexlite", "spbu", "pom", "shell", "vivo", "bp",
            "ojek", "ojol", "gojek", "grab", "maxim", "indrive", "goride", "gocar",
            "grabcar", "grabbike", "taksi", "taxi", "bluebird", "angkot", "mikrolet",
            "bus", "bis", "transjakarta", "busway", "damri", "shuttle", "travel",
            "kereta", "krl", "commuter", "kai", "mrt", "lrt", "whoosh", "argo",
            "tol", "etoll", "flazz", "emoney", "brizzi", "tapcash",
            "pesawat", "garuda", "lion", "citilink", "airasia", "batik", "bandara", "airport",
            "kapal", "ferry", "pelabuhan", "pelni",
            "servis", "service", "bengkel", "oli", "ban", "tambal", "velg", "aki",
            "busi", "rantai", "kampas", "rem", "cuci", "doorsmeer", "salon", "kir",
            "stnk", "sim", "tilang", "valet"
        ),
        "Tagihan" to listOf(
            "tagihan", "bill", "listrik", "pln", "token", "kwh",
            "wifi", "internet", "indihome", "biznet", "firstmedia", "myrepublic", "cbn", "oxygen",
            "pulsa", "kuota", "paket", "telkomsel", "indosat", "xl", "axis", "tri", "smartfren", "byu",
            "pdam", "pam", "gas", "pgn", "lpg", "elpiji", "bright",
            "kos", "kost", "kontrakan", "apartemen", "sewa", "cicil", "cicilan", "angsuran", "kredit",
            "kpr", "leasing", "kredivo", "akulaku", "spaylater", "paylater", "pinjol",
            "bpjs", "asuransi", "prudential", "allianz", "axa", "manulife",
            "pajak", "pbb", "pph", "ppn", "samsat", "iuran", "ipl", "sampah", "keamanan", "rt", "rw",
            "subscription", "icloud", "gdrive", "canva", "chatgpt"
        ),
        "Belanja" to listOf(
            "belanja", "shopping", "baju", "kaos", "kemeja", "celana", "jeans", "rok", "dress",
            "jaket", "hoodie", "sweater", "jas", "blazer", "gamis", "hijab", "kerudung",
            "sepatu", "sandal", "sneakers", "kaoskaki", "tas", "ransel", "dompet", "topi",
            "sabuk", "ikat", "kacamata", "jam", "aksesoris", "perhiasan", "emas",
            "shopee", "tokopedia", "lazada", "blibli", "bukalapak", "zalora", "sociolla",
            "indomaret", "alfamart", "alfamidi", "lawson", "familymart", "circlek",
            "superindo", "transmart", "hypermart", "lottemart", "ranch", "farmers",
            "minimarket", "supermarket", "hypermarket", "mall", "pasar", "toko", "warung",
            "skincare", "kosmetik", "makeup", "sunscreen", "serum", "toner", "moisturizer",
            "lipstik", "bedak", "parfum", "deodoran", "sabun", "sampo", "shampoo", "conditioner",
            "odol", "pasta", "sikat", "tisu", "kapas", "pembalut", "popok", "diapers",
            "deterjen", "pewangi", "pelembut", "pel", "sapu", "ember", "handuk", "sprei",
            "bantal", "selimut", "gorden", "lampu", "baterai", "charger", "kabel", "casing",
            "earphone", "headset", "mouse", "keyboard", "flashdisk", "elektronik", "gadget",
            "kado", "souvenir", "parsel", "hampers", "perlengkapan", "perabot", "furniture"
        ),
        "Hiburan" to listOf(
            "hiburan", "nonton", "bioskop", "cinema", "xxi", "cgv", "cinepolis", "film", "movie",
            "netflix", "spotify", "youtube", "disney", "vidio", "viu", "iqiyi", "wetv", "joox",
            "game", "gaming", "topup", "voucher", "steam", "playstation", "ps", "nintendo", "xbox",
            "ml", "mobilelegends", "pubg", "ff", "freefire", "genshin", "valorant", "roblox",
            "konser", "festival", "pameran", "museum", "kebun", "zoo", "akuarium",
            "wisata", "liburan", "piknik", "jalan", "pantai", "gunung", "taman", "kolam",
            "renang", "hotel", "villa", "resort", "penginapan", "staycation", "traveloka", "tiket",
            "karaoke", "ktv", "biliar", "billiard", "bowling", "arcade", "timezone", "main",
            "rekreasi", "hobi", "komik", "manga", "mainan", "lego", "koleksi", "gym", "fitness"
        ),
        "Kesehatan" to listOf(
            "kesehatan", "sehat", "obat", "apotek", "kimia", "k24", "century", "guardian", "watsons",
            "dokter", "klinik", "puskesmas", "rs", "igd", "ugd", "poli", "rawat", "inap",
            "lab", "laboratorium", "rontgen", "usg", "mri", "swab", "antigen", "pcr",
            "vitamin", "suplemen", "multivitamin", "madu", "jamu", "herbal",
            "masker", "handsanitizer", "alkohol", "perban", "plester", "hansaplast", "betadine",
            "termometer", "tensi", "oksigen", "inhaler", "salep", "tetes", "sirup", "tablet", "kapsul",
            "gigi", "behel", "tambal", "cabut", "scaling", "mata", "softlens", "terapi",
            "fisioterapi", "pijat", "urut", "refleksi", "spa", "vaksin", "imunisasi", "medical"
        ),
        "Pendidikan" to listOf(
            "pendidikan", "sekolah", "kuliah", "kampus", "universitas", "institut", "akademi",
            "spp", "ukt", "pangkal", "gedung", "praktikum", "wisuda", "skripsi", "tesis",
            "buku", "novel", "ensiklopedia", "kamus", "modul", "diktat", "ebook",
            "atk", "pulpen", "pensil", "penghapus", "penggaris", "spidol", "stabilo",
            "kertas", "hvs", "map", "binder", "catatan", "fotokopi", "print", "jilid", "laminating",
            "kursus", "les", "bimbel", "privat", "pelatihan", "training", "bootcamp",
            "seminar", "workshop", "webinar", "konferensi", "sertifikasi", "ujian", "tryout",
            "toefl", "ielts", "utbk", "cpns", "beasiswa", "edukasi", "udemy", "coursera"
        )
    )

    private val incomeKeywords: Map<String, List<String>> = mapOf(
        "Gaji" to listOf(
            "gaji", "gajian", "salary", "upah", "payroll", "penghasilan", "pendapatan", "honorarium"
        ),
        "Bonus" to listOf(
            "bonus", "thr", "insentif", "komisi", "lembur", "overtime", "tunjangan", "uang makan",
            "uang transport", "pesangon", "deviden", "dividen"
        ),
        "Hadiah" to listOf(
            "hadiah", "angpao", "dikasih", "ortu", "orang tua", "giveaway", "menang", "undian",
            "doorprize", "sumbangan", "hibah", "warisan", "kado", "tip", "tips"
        ),
        "Lainnya" to listOf(
            "jual", "penjualan", "dagang", "jualan", "laba", "untung", "profit", "omzet",
            "cashback", "refund", "kembalian", "reimburse", "klaim", "freelance", "proyek",
            "project", "honor", "royalti", "bunga", "bagi hasil", "sewa masuk"
        )
    )

    val allDictionarySingleWords: Set<String> by lazy {
        buildSet {
            expenseKeywords.values.flatten().forEach { if (!it.contains(' ')) add(it) }
            incomeKeywords.values.flatten().forEach { if (!it.contains(' ')) add(it) }
            phraseDictionary.keys.forEach { phrase ->
                phrase.split(' ').forEach { add(it) }
            }
        }
    }

    fun resolve(
        normalizedText: String,
        type: TransactionType,
        categories: List<CategoryEntity>,
        userCorrections: Map<String, String> = emptyMap(),
        historyKeywordMap: Map<String, Map<String, Int>> = emptyMap()
    ): CategoryResolution {
        val targetCatType = when (type) {
            TransactionType.INCOME -> CategoryType.INCOME
            TransactionType.EXPENSE -> CategoryType.EXPENSE
            TransactionType.TRANSFER -> return CategoryResolution(null, emptyList())
        }

        val activeCategories = categories.filter { it.type == targetCatType && !it.isArchived }
        if (activeCategories.isEmpty()) {
            return CategoryResolution(null, emptyList())
        }

        val tokens = normalizedText
            .split(Regex("""[\s,;+]+"""))
            .map { it.trim(',', '.', '-') }
            .filter { it.isNotEmpty() && !it.all { ch -> ch.isDigit() } && it !in stopWords }

        // Map: CategoryEntity.id -> best (score, source)
        val scoreMap = mutableMapOf<Long, Pair<Float, CategoryMatchSource>>()

        fun recordScore(category: CategoryEntity, score: Float, source: CategoryMatchSource) {
            val current = scoreMap[category.id]
            if (current == null || score > current.first) {
                scoreMap[category.id] = score to source
            }
        }

        // Lapisan (a): Koreksi pengguna di DataStore (bobot tertinggi = 1.0f)
        if (userCorrections.isNotEmpty()) {
            val matchedCorrectionCat = findCorrectionMatch(normalizedText, tokens, userCorrections)
            if (matchedCorrectionCat != null) {
                val target = mapCategoryName(matchedCorrectionCat, activeCategories, allowFallbackLainnya = false)
                if (target != null) {
                    recordScore(target, 1.0f, CategoryMatchSource.USER_CORRECTION)
                }
            }
        }

        // Lapisan (b): Peta kata ke kategori dari riwayat transaksi (bobot 0.78f - 0.90f)
        if (historyKeywordMap.isNotEmpty()) {
            for (token in tokens) {
                if (token.length < 3) continue
                val catCounts = historyKeywordMap[token] ?: continue
                val total = catCounts.values.sum()
                if (total <= 0) continue
                for ((catName, count) in catCounts) {
                    val target = mapCategoryName(catName, activeCategories, allowFallbackLainnya = false) ?: continue
                    val dominance = count.toFloat() / total.toFloat()
                    val score = (0.78f + 0.12f * dominance).coerceAtMost(0.90f)
                    recordScore(target, score, CategoryMatchSource.HISTORY)
                }
            }
        }

        // Cocokkan langsung dengan nama kategori pengguna (bobot 0.92f)
        for (cat in activeCategories.sortedByDescending { it.name.trim().length }) {
            val catNameLower = cat.name.trim().lowercase()
            if (catNameLower.isNotEmpty() &&
                Regex("""\b${Regex.escape(catNameLower)}\b""").containsMatchIn(normalizedText)
            ) {
                recordScore(cat, 0.92f, CategoryMatchSource.DIRECT_NAME)
            }
        }

        // Lapisan (c): Kamus bawaan besar — frasa gabungan berbobot lebih tinggi (0.86f) dari kata tunggal (0.72f)
        for ((phrase, catPair) in phraseDictionary) {
            val (catName, catType) = catPair
            if (catType != targetCatType) continue
            if (Regex("""\b${Regex.escape(phrase)}\b""").containsMatchIn(normalizedText)) {
                val target = mapCategoryName(catName, activeCategories, allowFallbackLainnya = true)
                if (target != null) {
                    recordScore(target, 0.86f, CategoryMatchSource.BUILTIN_PHRASE)
                }
            }
        }

        val dict = if (targetCatType == CategoryType.EXPENSE) expenseKeywords else incomeKeywords
        for ((catName, keywords) in dict) {
            for (kw in keywords) {
                val matched = if (kw.contains(' ')) {
                    Regex("""\b${Regex.escape(kw)}\b""").containsMatchIn(normalizedText)
                } else {
                    tokens.any { it == kw }
                }
                if (matched) {
                    // Untuk "Lainnya" di INCOME (proyek/freelance), hanya cocokkan bila kategori target ada atau ada "Lainnya"
                    val target = mapCategoryName(
                        catName = catName,
                        activeCategories = activeCategories,
                        allowFallbackLainnya = true
                    )
                    if (target != null) {
                        val score = if (kw.contains(' ')) 0.84f else 0.72f
                        recordScore(target, score, CategoryMatchSource.BUILTIN_KEYWORD)
                    }
                }
            }
        }

        // Lapisan (d): Fuzzy ke nama kategori buatan pengguna (bobot 0.50f - 0.65f)
        for (cat in activeCategories) {
            val catNameLower = cat.name.trim().lowercase()
            if (catNameLower.length < 4 || catNameLower.contains(' ')) continue
            for (token in tokens) {
                if (token.length < 4) continue
                val maxDist = if (catNameLower.length >= 7) 2 else 1
                val dist = Normalizer.levenshteinDistance(token, catNameLower, maxDist)
                if (dist in 1..maxDist) {
                    val score = if (dist == 1) 0.65f else 0.50f
                    recordScore(cat, score, CategoryMatchSource.FUZZY_NAME)
                }
            }
        }

        val candidates = scoreMap.entries
            .mapNotNull { (catId, pair) ->
                val cat = activeCategories.firstOrNull { it.id == catId } ?: return@mapNotNull null
                CategoryCandidate(
                    categoryId = cat.id,
                    categoryName = cat.name,
                    score = pair.first,
                    source = pair.second
                )
            }
            .sortedByDescending { it.score }
            .take(3)

        val top = candidates.firstOrNull()
        val confidentCategoryId = if (top != null && top.score >= CONFIDENCE_THRESHOLD) {
            top.categoryId
        } else {
            null
        }

        return CategoryResolution(
            categoryId = confidentCategoryId,
            candidates = candidates
        )
    }

    private fun findCorrectionMatch(
        normalizedText: String,
        tokens: List<String>,
        userCorrections: Map<String, String>
    ): String? {
        // Prioritaskan frasa terpanjang terlebih dahulu
        val sortedKeys = userCorrections.keys.sortedByDescending { it.length }
        for (key in sortedKeys) {
            val k = key.trim().lowercase()
            if (k.isEmpty()) continue
            if (k.contains(' ')) {
                if (Regex("""\b${Regex.escape(k)}\b""").containsMatchIn(normalizedText)) {
                    return userCorrections[key]
                }
            } else if (tokens.any { it == k }) {
                return userCorrections[key]
            }
        }
        return null
    }

    private fun mapCategoryName(
        catName: String,
        activeCategories: List<CategoryEntity>,
        allowFallbackLainnya: Boolean
    ): CategoryEntity? {
        // 1. Cocok persis
        activeCategories.firstOrNull { it.name.equals(catName, ignoreCase = true) }?.let { return it }

        // 2. Padanan terdekat (mis. "Makan" -> "Makanan")
        val targetLower = catName.lowercase()
        activeCategories.firstOrNull {
            val nameLower = it.name.trim().lowercase()
            nameLower.startsWith(targetLower) || targetLower.startsWith(nameLower)
        }?.let { return it }

        // 3. Fallback ke "Lainnya" bila tersedia
        if (allowFallbackLainnya) {
            activeCategories.firstOrNull { it.name.equals("Lainnya", ignoreCase = true) }?.let { return it }
        }
        return null
    }

    /**
     * Membangun peta kata -> frekuensi kategori dari maksimal 1000 transaksi terbaru yang memiliki catatan.
     */
    fun buildHistoryKeywordMap(
        transactions: List<TransactionEntity>,
        categoriesById: Map<Long, CategoryEntity>
    ): Map<String, Map<String, Int>> {
        val result = HashMap<String, MutableMap<String, Int>>()
        var processed = 0
        for (tx in transactions) {
            if (processed >= 1000) break
            if (tx.type == TransactionType.TRANSFER) continue
            val note = tx.note.trim()
            if (note.isEmpty()) continue
            val catId = tx.categoryId ?: continue
            val cat = categoriesById[catId] ?: continue
            if (cat.isArchived) continue

            processed++
            val normalizedNote = Normalizer.normalize(note)
            val tokens = normalizedNote
                .split(Regex("""[\s,;+]+"""))
                .map { it.trim(',', '.', '-') }
                .filter { it.length >= 3 && !it.all { ch -> ch.isDigit() } && it !in stopWords }
                .distinct()

            for (token in tokens) {
                val counts = result.getOrPut(token) { HashMap() }
                counts[cat.name] = (counts[cat.name] ?: 0) + 1
            }
        }
        return result
    }

    fun extractPrimaryNoteKeyword(note: String): String {
        val normalized = Normalizer.normalize(note)
        val tokens = normalized
            .split(Regex("""[\s,;+]+"""))
            .map { it.trim(',', '.', '-') }
            .filter { it.length >= 3 && !it.all { ch -> ch.isDigit() } && it !in stopWords }
        return if (tokens.size >= 2) {
            tokens.take(2).joinToString(" ")
        } else {
            tokens.firstOrNull().orEmpty()
        }
    }
}
