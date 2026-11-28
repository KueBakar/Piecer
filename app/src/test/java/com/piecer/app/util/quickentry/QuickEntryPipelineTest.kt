package com.piecer.app.util.quickentry

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.data.local.entity.WalletType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class QuickEntryPipelineTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    // Jumat, 27 November 2026
    private val today = LocalDate.of(2026, 11, 27)
    private val nowMillis = today.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()

    private val catMakan = CategoryEntity(1L, "Makan", CategoryType.EXPENSE, "restaurant", "coral", true)
    private val catTransport = CategoryEntity(2L, "Transport", CategoryType.EXPENSE, "directions_car", "coral", true)
    private val catTagihan = CategoryEntity(3L, "Tagihan", CategoryType.EXPENSE, "receipt_long", "coral", true)
    private val catBelanja = CategoryEntity(4L, "Belanja", CategoryType.EXPENSE, "shopping_bag", "coral", true)
    private val catHiburan = CategoryEntity(5L, "Hiburan", CategoryType.EXPENSE, "movie", "coral", true)
    private val catKesehatan = CategoryEntity(6L, "Kesehatan", CategoryType.EXPENSE, "medical_services", "coral", true)
    private val catPendidikan = CategoryEntity(7L, "Pendidikan", CategoryType.EXPENSE, "school", "coral", true)
    private val catLainnyaExp = CategoryEntity(8L, "Lainnya", CategoryType.EXPENSE, "more_horiz", "coral", true)

    private val catGaji = CategoryEntity(10L, "Gaji", CategoryType.INCOME, "payments", "mint", true)
    private val catBonus = CategoryEntity(11L, "Bonus", CategoryType.INCOME, "card_giftcard", "mint", true)
    private val catHadiah = CategoryEntity(12L, "Hadiah", CategoryType.INCOME, "redeem", "mint", true)
    private val catLainnyaInc = CategoryEntity(13L, "Lainnya", CategoryType.INCOME, "more_horiz", "mint", true)

    private val categories = listOf(
        catMakan, catTransport, catTagihan, catBelanja, catHiburan, catKesehatan, catPendidikan, catLainnyaExp,
        catGaji, catBonus, catHadiah, catLainnyaInc
    )

    private val walletTunai = WalletEntity(1L, "Tunai", WalletType.CASH, 0L, "wallet", "ink", 1L)
    private val walletGopay = WalletEntity(2L, "GoPay", WalletType.EWALLET, 0L, "ewallet", "mint", 1L)
    private val walletBca = WalletEntity(3L, "BCA", WalletType.BANK, 0L, "bank", "ink", 1L)
    private val walletOvo = WalletEntity(4L, "OVO", WalletType.EWALLET, 0L, "ewallet", "mint", 1L)
    private val walletMandiri = WalletEntity(5L, "Mandiri", WalletType.BANK, 0L, "bank", "ink", 1L)

    private val wallets = listOf(walletTunai, walletGopay, walletBca, walletOvo, walletMandiri)

    private fun baseContext(lastUsedWalletId: Long? = null) = QuickEntryContext(
        categories = categories,
        wallets = wallets,
        lastUsedWalletId = lastUsedWalletId,
        nowMillis = nowMillis,
        zoneId = zone
    )

    private data class TableCase(
        val input: String,
        val expectedType: TransactionType,
        val expectedAmount: Long?,
        val expectedCategoryId: Long?,
        val expectedWalletId: Long? = null,
        val expectedToWalletId: Long? = null,
        val expectedDate: LocalDate? = null,
        val expectedNoteContains: String? = null,
        val expectedFlag: QuickEntryFlag? = null,
        val expectedEntriesCount: Int = 1
    )

    @Test
    fun tableDriven_150PlusSentences() {
        val cases = listOf(
            // === KASUS WAJIB DARI SPESIFIKASI ===
            TableCase("beli bakso 20k", TransactionType.EXPENSE, 20_000L, 1L, expectedNoteContains = "Beli bakso"),
            TableCase("aku dpt 1m", TransactionType.INCOME, 1_000_000L, null, expectedFlag = QuickEntryFlag.AMBIGUOUS_M),
            TableCase("nyewa mobil 10 hari tunai 1jt", TransactionType.EXPENSE, 1_000_000L, 2L, walletTunai.id, expectedNoteContains = "10 hari"),
            TableCase("geprek 10k", TransactionType.EXPENSE, 10_000L, 1L),
            TableCase("parkir 2k", TransactionType.EXPENSE, 2_000L, 2L),
            TableCase("jajan cimol 5k", TransactionType.EXPENSE, 5_000L, 1L),
            TableCase("gaji 5jt", TransactionType.INCOME, 5_000_000L, 10L),
            TableCase("dikasih ortu 500rb", TransactionType.INCOME, 500_000L, 12L),
            TableCase("bayar wifi 350rb kemarin", TransactionType.EXPENSE, 350_000L, 3L, expectedDate = today.minusDays(1)),
            TableCase("dua puluh ribu bakso", TransactionType.EXPENSE, 20_000L, 1L),
            TableCase("seratus lima puluh ribu servis motor", TransactionType.EXPENSE, 150_000L, 2L),
            TableCase("geprek 10k es teh 3k parkir 2k pakai gopay", TransactionType.EXPENSE, 10_000L, 1L, walletGopay.id, expectedEntriesCount = 3),
            TableCase("3x15k nasi goreng", TransactionType.EXPENSE, 45_000L, 1L),
            TableCase("makan 2 porsi 40rb", TransactionType.EXPENSE, 40_000L, 1L, expectedNoteContains = "2 porsi"),
            TableCase("transfer dari bca ke gopay 100rb", TransactionType.TRANSFER, 100_000L, null, walletBca.id, walletGopay.id),
            TableCase("kopi", TransactionType.EXPENSE, null, 1L, expectedFlag = QuickEntryFlag.NO_AMOUNT),
            TableCase("tf 1,5jt", TransactionType.EXPENSE, 1_500_000L, null),
            TableCase("isi bensin 50000", TransactionType.EXPENSE, 50_000L, 2L),

            // === MAKAN & MINUMAN & JAJANAN ===
            TableCase("seblak 15k", TransactionType.EXPENSE, 15_000L, 1L),
            TableCase("batagor 12rb tunai", TransactionType.EXPENSE, 12_000L, 1L, walletTunai.id),
            TableCase("siomay 10ribu", TransactionType.EXPENSE, 10_000L, 1L),
            TableCase("martabak manis 35k gopay", TransactionType.EXPENSE, 35_000L, 1L, walletGopay.id),
            TableCase("gorengan 5rb", TransactionType.EXPENSE, 5_000L, 1L),
            TableCase("boba 25k ovo", TransactionType.EXPENSE, 25_000L, 1L, walletOvo.id),
            TableCase("ngopi 22rb", TransactionType.EXPENSE, 22_000L, 1L),
            TableCase("nasi padang 28k", TransactionType.EXPENSE, 28_000L, 1L),
            TableCase("soto ayam 18k", TransactionType.EXPENSE, 18_000L, 1L),
            TableCase("mie ayam 15rb", TransactionType.EXPENSE, 15_000L, 1L),
            TableCase("penyetan 20k", TransactionType.EXPENSE, 20_000L, 1L),
            TableCase("warteg 17rb", TransactionType.EXPENSE, 17_000L, 1L),
            TableCase("sarapan bubur ayam 12k", TransactionType.EXPENSE, 12_000L, 1L),
            TableCase("mkn malam sate ayam 30rb", TransactionType.EXPENSE, 30_000L, 1L),
            TableCase("kopi susu 24k", TransactionType.EXPENSE, 24_000L, 1L),
            TableCase("es teh 5k", TransactionType.EXPENSE, 5_000L, 1L),
            TableCase("pecel lele 22rb", TransactionType.EXPENSE, 22_000L, 1L),
            TableCase("nasi uduk 14k", TransactionType.EXPENSE, 14_000L, 1L),
            TableCase("dimsum 25k", TransactionType.EXPENSE, 25_000L, 1L),
            TableCase("sushi 120rb bca", TransactionType.EXPENSE, 120_000L, 1L, walletBca.id),
            TableCase("ramen 65k", TransactionType.EXPENSE, 65_000L, 1L),
            TableCase("pizza 150rb", TransactionType.EXPENSE, 150_000L, 1L),
            TableCase("burger 45k", TransactionType.EXPENSE, 45_000L, 1L),
            TableCase("kebab 20rb", TransactionType.EXPENSE, 20_000L, 1L),
            TableCase("roti bakar 18k", TransactionType.EXPENSE, 18_000L, 1L),
            TableCase("air galon 20k", TransactionType.EXPENSE, 20_000L, 1L),
            TableCase("gofood 55k gopay", TransactionType.EXPENSE, 55_000L, 1L, walletGopay.id),
            TableCase("grabfood 48rb", TransactionType.EXPENSE, 48_000L, 1L),

            // === TRANSPORT ===
            TableCase("pertalite 30rb tunai", TransactionType.EXPENSE, 30_000L, 2L, walletTunai.id),
            TableCase("pertamax 50k", TransactionType.EXPENSE, 50_000L, 2L),
            TableCase("ojek 15ribu", TransactionType.EXPENSE, 15_000L, 2L),
            TableCase("gojek 22k gopay", TransactionType.EXPENSE, 22_000L, 2L, walletGopay.id),
            TableCase("grab 35rb ovo", TransactionType.EXPENSE, 35_000L, 2L, walletOvo.id),
            TableCase("bayar tol 25k", TransactionType.EXPENSE, 25_000L, 2L),
            TableCase("krl 6rb", TransactionType.EXPENSE, 6_000L, 2L),
            TableCase("tiket kereta 180k bca", TransactionType.EXPENSE, 180_000L, 2L, walletBca.id),
            TableCase("bus transjakarta 3500", TransactionType.EXPENSE, 3_500L, 2L),
            TableCase("angkot 5000 tunai", TransactionType.EXPENSE, 5_000L, 2L, walletTunai.id),
            TableCase("tambal ban 15k", TransactionType.EXPENSE, 15_000L, 2L),
            TableCase("cuci motor 20k", TransactionType.EXPENSE, 20_000L, 2L),
            TableCase("ganti oli 65rb", TransactionType.EXPENSE, 65_000L, 2L),
            TableCase("taksi 75k mandiri", TransactionType.EXPENSE, 75_000L, 2L, walletMandiri.id),
            TableCase("parkir mall 10k", TransactionType.EXPENSE, 10_000L, 2L),
            TableCase("maxim 14rb", TransactionType.EXPENSE, 14_000L, 2L),

            // === TAGIHAN ===
            TableCase("byr listrik 250.000 bca", TransactionType.EXPENSE, 250_000L, 3L, walletBca.id),
            TableCase("token pln 100k", TransactionType.EXPENSE, 100_000L, 3L),
            TableCase("indihome 380rb mandiri", TransactionType.EXPENSE, 380_000L, 3L, walletMandiri.id),
            TableCase("isi pulsa 50k", TransactionType.EXPENSE, 50_000L, 3L),
            TableCase("paket data 75rb", TransactionType.EXPENSE, 75_000L, 3L),
            TableCase("bayar pdam 95k", TransactionType.EXPENSE, 95_000L, 3L),
            TableCase("sewa kos 1,2jt bca", TransactionType.EXPENSE, 1_200_000L, 3L, walletBca.id),
            TableCase("kontrakan 1.5jt", TransactionType.EXPENSE, 1_500_000L, 3L),
            TableCase("nyicil motor 900rb", TransactionType.EXPENSE, 900_000L, 3L),
            TableCase("bpjs 150rb", TransactionType.EXPENSE, 150_000L, 3L),
            TableCase("asuransi 500k", TransactionType.EXPENSE, 500_000L, 3L),
            TableCase("iuran sampah 30rb", TransactionType.EXPENSE, 30_000L, 3L),

            // === BELANJA ===
            TableCase("beli baju 150k shopee", TransactionType.EXPENSE, 150_000L, 4L),
            TableCase("sepatu 350rb tokopedia", TransactionType.EXPENSE, 350_000L, 4L),
            TableCase("indomaret 45k", TransactionType.EXPENSE, 45_000L, 4L),
            TableCase("alfamart 62rb gopay", TransactionType.EXPENSE, 62_000L, 4L, walletGopay.id),
            TableCase("skincare 220k", TransactionType.EXPENSE, 220_000L, 4L),
            TableCase("beli kado 100rb", TransactionType.EXPENSE, 100_000L, 4L),
            TableCase("sabun dan sampo 40k", TransactionType.EXPENSE, 40_000L, 4L),
            TableCase("beli celana 180rb", TransactionType.EXPENSE, 180_000L, 4L),
            TableCase("supermarket 250k bca", TransactionType.EXPENSE, 250_000L, 4L, walletBca.id),
            TableCase("beli charger 85k", TransactionType.EXPENSE, 85_000L, 4L),

            // === HIBURAN ===
            TableCase("nonton bioskop 50k", TransactionType.EXPENSE, 50_000L, 5L),
            TableCase("netflix 54rb", TransactionType.EXPENSE, 54_000L, 5L),
            TableCase("spotify 55k", TransactionType.EXPENSE, 55_000L, 5L),
            TableCase("topup game 100k", TransactionType.EXPENSE, 100_000L, 5L),
            TableCase("tiket wisata 75rb", TransactionType.EXPENSE, 75_000L, 5L),
            TableCase("karaoke 120k", TransactionType.EXPENSE, 120_000L, 5L),
            TableCase("konser 500rb bca", TransactionType.EXPENSE, 500_000L, 5L, walletBca.id),
            TableCase("staycation hotel 650k", TransactionType.EXPENSE, 650_000L, 5L),

            // === KESEHATAN ===
            TableCase("beli obat 45k", TransactionType.EXPENSE, 45_000L, 6L),
            TableCase("apotek 80rb tunai", TransactionType.EXPENSE, 80_000L, 6L, walletTunai.id),
            TableCase("dokter 150k", TransactionType.EXPENSE, 150_000L, 6L),
            TableCase("klinik 120rb", TransactionType.EXPENSE, 120_000L, 6L),
            TableCase("vitamin 95k", TransactionType.EXPENSE, 95_000L, 6L),
            TableCase("dokter gigi 300rb bca", TransactionType.EXPENSE, 300_000L, 6L, walletBca.id),

            // === PENDIDIKAN ===
            TableCase("bayar spp 500k bca", TransactionType.EXPENSE, 500_000L, 7L, walletBca.id),
            TableCase("ukt 3,5jt mandiri", TransactionType.EXPENSE, 3_500_000L, 7L, walletMandiri.id),
            TableCase("beli buku 120rb", TransactionType.EXPENSE, 120_000L, 7L),
            TableCase("kursus bahasa 400k", TransactionType.EXPENSE, 400_000L, 7L),
            TableCase("les privat 250rb", TransactionType.EXPENSE, 250_000L, 7L),
            TableCase("fotokopi 15k", TransactionType.EXPENSE, 15_000L, 7L),

            // === PEMASUKAN ===
            TableCase("gajian 8jt mandiri", TransactionType.INCOME, 8_000_000L, 10L, walletMandiri.id),
            TableCase("terima bonus 2jt bca", TransactionType.INCOME, 2_000_000L, 11L, walletBca.id),
            TableCase("dapat thr 3jt", TransactionType.INCOME, 3_000_000L, 11L),
            TableCase("angpao 200k tunai", TransactionType.INCOME, 200_000L, 12L, walletTunai.id),
            TableCase("nerima honor 750rb", TransactionType.INCOME, 750_000L, 13L),
            TableCase("jual barang 300k", TransactionType.INCOME, 300_000L, 13L),
            TableCase("dapat cashback 25rb gopay", TransactionType.INCOME, 25_000L, 13L, walletGopay.id),
            TableCase("untung jualan 450k", TransactionType.INCOME, 450_000L, 13L),
            TableCase("ditransfer freelance 1,5jt bca", TransactionType.INCOME, 1_500_000L, 13L, walletBca.id),
            TableCase("transfer masuk 500rb ovo", TransactionType.INCOME, 500_000L, 13L, walletOvo.id),
            TableCase("beli baju dapat diskon 50rb", TransactionType.EXPENSE, 50_000L, 4L),
            TableCase("bayar makan dapat diskon 20k", TransactionType.EXPENSE, 20_000L, 1L),

            // === ANGKA & SLANG & KATA BILANGAN ===
            TableCase("Rp 25.000 bakso", TransactionType.EXPENSE, 25_000L, 1L),
            TableCase("Rp25000 kopi", TransactionType.EXPENSE, 25_000L, 1L),
            TableCase("bakso 25000 rupiah", TransactionType.EXPENSE, 25_000L, 1L),
            TableCase("bensin 25,000", TransactionType.EXPENSE, 25_000L, 2L),
            TableCase("kopi 1.5jt", TransactionType.EXPENSE, 1_500_000L, 1L),
            TableCase("parkir goceng", TransactionType.EXPENSE, 5_000L, 2L),
            TableCase("jajan ceban", TransactionType.EXPENSE, 10_000L, 1L),
            TableCase("bakso noban", TransactionType.EXPENSE, 20_000L, 1L),
            TableCase("bensin gocap", TransactionType.EXPENSE, 50_000L, 2L),
            TableCase("sejuta sewa kos", TransactionType.EXPENSE, 1_000_000L, 3L),
            TableCase("seribu parkir", TransactionType.EXPENSE, 1_000L, 2L),
            TableCase("setengah juta belanja baju", TransactionType.EXPENSE, 500_000L, 4L),
            TableCase("satu setengah juta sewa kos", TransactionType.EXPENSE, 1_500_000L, 3L),
            TableCase("dua juta lima ratus ribu gaji", TransactionType.INCOME, 2_500_000L, 10L),
            TableCase("2 miliar beli rumah", TransactionType.EXPENSE, 2_000_000_000L, null, expectedFlag = QuickEntryFlag.LARGE_AMOUNT),
            TableCase("3 x 15k kopi", TransactionType.EXPENSE, 45_000L, 1L),
            TableCase("3 @ 15k bakso", TransactionType.EXPENSE, 45_000L, 1L),
            TableCase("2 kopi 15k total 30k", TransactionType.EXPENSE, 30_000L, 1L),
            TableCase("makan 3 bungkus 30rb", TransactionType.EXPENSE, 30_000L, 1L, expectedNoteContains = "3 bungkus"),
            TableCase("beli air 2 botol 10k", TransactionType.EXPENSE, 10_000L, 1L, expectedNoteContains = "2 botol"),
            TableCase("kopi 2 gelas 36k", TransactionType.EXPENSE, 36_000L, 1L, expectedNoteContains = "2 gelas"),
            TableCase("nonton 2 orang 100k", TransactionType.EXPENSE, 100_000L, 5L, expectedNoteContains = "2 orang"),

            // === TANGGAL ===
            TableCase("bakso 20k hari ini", TransactionType.EXPENSE, 20_000L, 1L, expectedDate = today),
            TableCase("kopi 18k tadi pagi", TransactionType.EXPENSE, 18_000L, 1L, expectedDate = today),
            TableCase("makan 25k tadi siang", TransactionType.EXPENSE, 25_000L, 1L, expectedDate = today),
            TableCase("jajan 10k tadi sore", TransactionType.EXPENSE, 10_000L, 1L, expectedDate = today),
            TableCase("sate 30k tadi malam", TransactionType.EXPENSE, 30_000L, 1L, expectedDate = today),
            TableCase("bensin 30k kemarin lusa", TransactionType.EXPENSE, 30_000L, 2L, expectedDate = today.minusDays(2)),
            TableCase("tiket kereta 200k lusa", TransactionType.EXPENSE, 200_000L, 2L, expectedDate = today.plusDays(2)),
            TableCase("wifi 300k tgl 20", TransactionType.EXPENSE, 300_000L, 3L, expectedDate = LocalDate.of(2026, 11, 20)),
            TableCase("listrik 200k tanggal 29", TransactionType.EXPENSE, 200_000L, 3L, expectedDate = LocalDate.of(2026, 10, 29)),
            TableCase("makan 50k senin kemarin", TransactionType.EXPENSE, 50_000L, 1L, expectedDate = LocalDate.of(2026, 11, 23)),
            TableCase("belanja 100k minggu lalu", TransactionType.EXPENSE, 100_000L, 4L, expectedDate = today.minusWeeks(1)),
            TableCase("sewa kos 1jt bulan lalu", TransactionType.EXPENSE, 1_000_000L, 3L, expectedDate = today.minusMonths(1)),

            // === TRANSFER & DOMPET ===
            TableCase("pindah bca ke ovo 250rb", TransactionType.TRANSFER, 250_000L, null, walletBca.id, walletOvo.id),
            TableCase("transfer ke mandiri 500rb dari gopay", TransactionType.TRANSFER, 500_000L, null, walletGopay.id, walletMandiri.id),
            TableCase("kopi 15k cash", TransactionType.EXPENSE, 15_000L, 1L, walletTunai.id),
            TableCase("bakso 20k kontan", TransactionType.EXPENSE, 20_000L, 1L, walletTunai.id),

            // === NORMALISASI & TYPO ===
            TableCase("mkn bakso 20rb kmrn", TransactionType.EXPENSE, 20_000L, 1L, expectedDate = today.minusDays(1)),
            TableCase("dibayarin parkir 2k", TransactionType.EXPENSE, 2_000L, 2L),
            TableCase("bensn 30k", TransactionType.EXPENSE, 30_000L, 2L),
            TableCase("listrk 200rb", TransactionType.EXPENSE, 200_000L, 3L),
            TableCase("blnja baju 100k", TransactionType.EXPENSE, 100_000L, 4L)
        )

        assertTrue("Harus ada minimal 150 kalimat uji, saat ini: ${cases.size}", cases.size >= 150)

        for ((idx, tc) in cases.withIndex()) {
            val result = QuickEntryPipeline.parse(tc.input, baseContext())
            assertEquals("Case #$idx ('${tc.input}') entries count", tc.expectedEntriesCount, result.entries.size)
            val entry = result.entries.first()
            assertEquals("Case #$idx ('${tc.input}') type", tc.expectedType, entry.type)
            assertEquals("Case #$idx ('${tc.input}') amount", tc.expectedAmount, entry.amount)
            assertEquals("Case #$idx ('${tc.input}') categoryId", tc.expectedCategoryId, entry.categoryId)
            if (tc.expectedWalletId != null) {
                assertEquals("Case #$idx ('${tc.input}') walletId", tc.expectedWalletId, entry.walletId)
            }
            if (tc.expectedToWalletId != null) {
                assertEquals("Case #$idx ('${tc.input}') toWalletId", tc.expectedToWalletId, entry.toWalletId)
            }
            if (tc.expectedDate != null) {
                val expectedMillis = tc.expectedDate.atStartOfDay(zone).toInstant().toEpochMilli()
                assertEquals("Case #$idx ('${tc.input}') dateMillis", expectedMillis, entry.dateMillis)
            }
            if (tc.expectedNoteContains != null) {
                assertTrue(
                    "Case #$idx ('${tc.input}') note '${entry.note}' should contain '${tc.expectedNoteContains}'",
                    entry.note.contains(tc.expectedNoteContains, ignoreCase = true)
                )
            }
            if (tc.expectedFlag != null) {
                assertTrue(
                    "Case #$idx ('${tc.input}') flags ${entry.flags} should contain ${tc.expectedFlag}",
                    tc.expectedFlag in entry.flags
                )
            }
        }
    }

    @Test
    fun emptyAndBlankInput_returnsEmptyWithoutCrash() {
        val r1 = QuickEntryPipeline.parse("", baseContext())
        assertTrue(r1.entries.isEmpty())

        val r2 = QuickEntryPipeline.parse("   ", baseContext())
        assertTrue(r2.entries.isEmpty())
    }

    @Test
    fun multiEntrySegmentation_sharesWalletAndDateAcrossAllEntries() {
        val r = QuickEntryPipeline.parse(
            "geprek 10k es teh 3k parkir 2k pakai gopay kemarin",
            baseContext()
        )
        assertEquals(3, r.entries.size)
        val expectedDate = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        assertEquals(10_000L, r.entries[0].amount)
        assertEquals(1L, r.entries[0].categoryId)
        assertEquals(walletGopay.id, r.entries[0].walletId)
        assertEquals(expectedDate, r.entries[0].dateMillis)

        assertEquals(3_000L, r.entries[1].amount)
        assertEquals(1L, r.entries[1].categoryId)
        assertEquals(walletGopay.id, r.entries[1].walletId)
        assertEquals(expectedDate, r.entries[1].dateMillis)

        assertEquals(2_000L, r.entries[2].amount)
        assertEquals(2L, r.entries[2].categoryId)
        assertEquals(walletGopay.id, r.entries[2].walletId)
        assertEquals(expectedDate, r.entries[2].dateMillis)
    }

    @Test
    fun multiEntrySegmentation_delimitersCommaNewlinePlusDan() {
        val rComma = QuickEntryPipeline.parse("bakso 20k, parkir 2k", baseContext())
        assertEquals(2, rComma.entries.size)

        val rNewline = QuickEntryPipeline.parse("bakso 20k\nparkir 2k", baseContext())
        assertEquals(2, rNewline.entries.size)

        val rPlus = QuickEntryPipeline.parse("bakso 20k + parkir 2k", baseContext())
        assertEquals(2, rPlus.entries.size)

        val rDan = QuickEntryPipeline.parse("bakso 20k dan parkir 2k", baseContext())
        assertEquals(2, rDan.entries.size)
    }

    @Test
    fun categoryResolver_layeredPriority_userCorrectionBeatsHistoryAndBuiltin() {
        val ctx = baseContext().copy(
            userCorrections = mapOf("kopi" to "Hiburan"),
            historyKeywordMap = mapOf("kopi" to mapOf("Tagihan" to 5))
        )
        val r = QuickEntryPipeline.parse("kopi 25k", ctx)
        val entry = r.entries.first()
        assertEquals(catHiburan.id, entry.categoryId)
        assertTrue(entry.categoryCandidates.size <= 3)
        assertEquals(CategoryMatchSource.USER_CORRECTION, entry.categoryCandidates.first().source)
    }

    @Test
    fun categoryResolver_historyMapUsedWhenNoCorrection() {
        val historyTxs = listOf(
            TransactionEntity(1L, 50_000L, TransactionType.EXPENSE, catHiburan.id, 1L, null, nowMillis, "Matcha latte", nowMillis),
            TransactionEntity(2L, 50_000L, TransactionType.EXPENSE, catHiburan.id, 1L, null, nowMillis, "Matcha", nowMillis)
        )
        val historyMap = CategoryResolver.buildHistoryKeywordMap(historyTxs, categories.associateBy { it.id })
        val ctx = baseContext().copy(historyKeywordMap = historyMap)
        val r = QuickEntryPipeline.parse("matcha 30k", ctx)
        assertEquals(catHiburan.id, r.entries.first().categoryId)
    }

    @Test
    fun categoryResolver_lowConfidenceLeavesCategoryNullAndReturnsCandidates() {
        val customCat = CategoryEntity(99L, "Langganan", CategoryType.EXPENSE, "receipt_long", "coral", false)
        val ctx = baseContext().copy(categories = categories + customCat)
        // "langanan" has edit distance 1 (score 0.65f), let's test distance 2 on a >=7 char category
        val customCat2 = CategoryEntity(100L, "Investasi", CategoryType.EXPENSE, "savings", "coral", false)
        val ctx2 = baseContext().copy(categories = categories + customCat2)
        val r = QuickEntryPipeline.parse("invesxxsi 100k", ctx2)
        val entry = r.entries.first()
        assertNull(entry.categoryId)
        assertTrue(QuickEntryFlag.NO_CATEGORY in entry.flags)
        assertTrue(entry.categoryCandidates.isNotEmpty())
        assertEquals(100L, entry.categoryCandidates.first().categoryId)
    }

    @Test
    fun walletResolver_usesLastUsedWalletWhenNotMentioned() {
        val r = QuickEntryPipeline.parse("bakso 20k", baseContext(lastUsedWalletId = walletOvo.id))
        val entry = r.entries.first()
        assertEquals(walletOvo.id, entry.walletId)
        assertEquals("OVO", entry.walletName)
    }

    @Test
    fun multiAmountAmbiguous_setsFlagAndLowersConfidence() {
        val r = QuickEntryPipeline.parse("bakso 20k 25k", baseContext())
        val entry = r.entries.first()
        assertTrue(QuickEntryFlag.MULTI_AMOUNT_AMBIGUOUS in entry.flags)
        assertTrue(entry.confidence < 0.8f)
    }

    @Test
    fun note_exactExampleCheck() {
        val r = QuickEntryPipeline.parse("nyewa mobil 10 hari tunai 1jt", baseContext())
        val entry = r.entries.first()
        assertEquals("Sewa mobil 10 hari", entry.note)
        assertEquals(1_000_000L, entry.amount)
        assertEquals(catTransport.id, entry.categoryId)
        assertEquals(walletTunai.id, entry.walletId)
    }

    @Test
    fun performance_singleSentenceUnder20msOnJvm() {
        val ctx = baseContext()
        // Warmup JIT
        repeat(20) {
            QuickEntryPipeline.parse("nyewa mobil 10 hari tunai 1jt kemarin", ctx)
        }
        val samples = listOf(
            "beli bakso 20k",
            "nyewa mobil 10 hari tunai 1jt kemarin",
            "geprek 10k es teh 3k parkir 2k pakai gopay",
            "seratus lima puluh ribu servis motor bca",
            "transfer dari bca ke gopay 100rb"
        )
        val startNs = System.nanoTime()
        for (s in samples) {
            QuickEntryPipeline.parse(s, ctx)
        }
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
        val avgMs = elapsedMs / samples.size
        assertTrue("Rata-rata waktu per kalimat harus < 20 ms, aktual: $avgMs ms", avgMs < 20.0)
    }
}
