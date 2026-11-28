package com.piecer.app.util

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.data.local.entity.WalletType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class QuickEntryParserTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val today = LocalDate.of(2026, 11, 27)
    private val nowMillis = today.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()

    private val categories = listOf(
        CategoryEntity(1L, "Makan", CategoryType.EXPENSE, "restaurant", "coral", true),
        CategoryEntity(2L, "Transport", CategoryType.EXPENSE, "directions_car", "coral", true),
        CategoryEntity(3L, "Tagihan", CategoryType.EXPENSE, "receipt_long", "coral", true),
        CategoryEntity(4L, "Belanja", CategoryType.EXPENSE, "shopping_bag", "coral", true),
        CategoryEntity(5L, "Hiburan", CategoryType.EXPENSE, "movie", "coral", true),
        CategoryEntity(6L, "Kesehatan", CategoryType.EXPENSE, "medical_services", "coral", true),
        CategoryEntity(7L, "Pendidikan", CategoryType.EXPENSE, "school", "coral", true),
        CategoryEntity(10L, "Gaji", CategoryType.INCOME, "payments", "mint", true),
        CategoryEntity(11L, "Bonus", CategoryType.INCOME, "card_giftcard", "mint", true)
    )

    private val wallets = listOf(
        WalletEntity(1L, "Tunai", WalletType.CASH, 0L, "wallet", "ink", 1L),
        WalletEntity(2L, "GoPay", WalletType.EWALLET, 0L, "ewallet", "mint", 1L),
        WalletEntity(3L, "BCA", WalletType.BANK, 0L, "bank", "ink", 1L)
    )

    private fun parse(input: String) = QuickEntryParser.parse(input, categories, wallets, nowMillis, zone)

    @Test fun case01_makanSiang25rbGopay() {
        val r = parse("makan siang 25rb gopay")
        assertEquals(TransactionType.EXPENSE, r.type)
        assertEquals(25_000L, r.amount)
        assertEquals(1L, r.categoryId)
        assertEquals(2L, r.walletId)
        assertEquals("Makan siang", r.note)
    }

    @Test fun case02_plainDigits() {
        val r = parse("bakso 25000 tunai")
        assertEquals(25_000L, r.amount)
        assertEquals(1L, r.categoryId)
        assertEquals(1L, r.walletId)
    }

    @Test fun case03_dottedThousands() {
        val r = parse("bayar listrik 250.000 bca")
        assertEquals(250_000L, r.amount)
        assertEquals(3L, r.categoryId)
        assertEquals(3L, r.walletId)
    }

    @Test fun case04_kSuffix() {
        val r = parse("kopi 18k gopay")
        assertEquals(18_000L, r.amount)
        assertEquals(1L, r.categoryId)
        assertEquals(2L, r.walletId)
    }

    @Test fun case05_ribuSuffix() {
        val r = parse("ojek 15ribu")
        assertEquals(15_000L, r.amount)
        assertEquals(2L, r.categoryId)
        assertNull(r.walletId)
    }

    @Test fun case06_decimalCommaMillion() {
        val r = parse("gaji 1,5jt bca")
        assertEquals(TransactionType.INCOME, r.type)
        assertEquals(1_500_000L, r.amount)
        assertEquals(10L, r.categoryId)
        assertEquals(3L, r.walletId)
    }

    @Test fun case07_integerMillion() {
        val r = parse("terima bonus 2jt bca")
        assertEquals(TransactionType.INCOME, r.type)
        assertEquals(2_000_000L, r.amount)
        assertEquals(11L, r.categoryId)
        assertEquals(3L, r.walletId)
    }

    @Test fun case08_slangGoceng() {
        val r = parse("parkir goceng tunai")
        assertEquals(5_000L, r.amount)
        assertEquals(2L, r.categoryId)
        assertEquals(1L, r.walletId)
    }

    @Test fun case09_slangCeban() {
        val r = parse("jajan ceban")
        assertEquals(10_000L, r.amount)
        assertEquals(1L, r.categoryId)
    }

    @Test fun case10_slangGopek() {
        val r = parse("parkir gopek tunai")
        assertEquals(500L, r.amount)
        assertEquals(2L, r.categoryId)
    }

    @Test fun case11_slangCepek() {
        val r = parse("kembalian cepek")
        assertEquals(100L, r.amount)
    }

    @Test fun case12_incomeDapat() {
        val r = parse("dapat proyek 500rb bca")
        assertEquals(TransactionType.INCOME, r.type)
        assertEquals(500_000L, r.amount)
        assertEquals(3L, r.walletId)
        assertNull(r.categoryId) // "proyek" tidak ada di kategori
    }

    @Test fun case13_transferKe() {
        val r = parse("transfer ke gopay 200rb dari bca")
        assertEquals(TransactionType.TRANSFER, r.type)
        assertEquals(200_000L, r.amount)
        assertEquals(3L, r.walletId)
        assertEquals(2L, r.toWalletId)
        assertNull(r.categoryId)
    }

    @Test fun case14_dateKemarin() {
        val r = parse("grab 30rb kemarin gopay")
        val expectedDate = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(30_000L, r.amount)
        assertEquals(2L, r.categoryId)
        assertEquals(2L, r.walletId)
        assertEquals(expectedDate, r.dateMillis)
    }

    @Test fun case15_dateTadiPagi() {
        val r = parse("sarapan 12rb tadi pagi")
        val expectedDate = today.atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(12_000L, r.amount)
        assertEquals(1L, r.categoryId)
        assertEquals(expectedDate, r.dateMillis)
    }

    @Test fun case16_dateTanggal20NotParsedAsAmount() {
        val r = parse("wifi 300rb tanggal 20 bca")
        val expectedDate = today.withDayOfMonth(20).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(300_000L, r.amount)
        assertEquals(3L, r.categoryId)
        assertEquals(3L, r.walletId)
        assertEquals(expectedDate, r.dateMillis)
    }

    @Test fun case17_tanggal20WithoutAmountLeavesAmountNull() {
        val r = parse("bayar wifi tanggal 20")
        val expectedDate = today.withDayOfMonth(20).atStartOfDay(zone).toInstant().toEpochMilli()
        assertNull(r.amount)
        assertEquals(3L, r.categoryId)
        assertEquals(expectedDate, r.dateMillis)
    }

    @Test fun case18_noAmountAtAll() {
        val r = parse("beli kopi di gopay")
        assertNull(r.amount)
        assertEquals(1L, r.categoryId)
        assertEquals(2L, r.walletId)
    }

    @Test fun case19_unknownCategoryDoesNotGuess() {
        val r = parse("urusan mendadak 150rb bca")
        assertEquals(150_000L, r.amount)
        assertNull(r.categoryId)
        assertEquals(3L, r.walletId)
    }

    @Test fun case20_emptyInput() {
        val r = parse("   ")
        assertNull(r.amount)
        assertNull(r.categoryId)
        assertNull(r.walletId)
        assertNull(r.dateMillis)
        assertEquals("", r.note)
    }

    @Test fun case21_belanjaSupermarket() {
        val r = parse("belanja alfamart 75rb gopay")
        assertEquals(75_000L, r.amount)
        assertEquals(4L, r.categoryId)
        assertEquals(2L, r.walletId)
    }

    @Test fun case22_hiburanBioskop() {
        val r = parse("nonton bioskop 50k")
        assertEquals(50_000L, r.amount)
        assertEquals(5L, r.categoryId)
    }

    @Test fun case23_kesehatanApotek() {
        val r = parse("beli obat apotek 45rb tunai")
        assertEquals(45_000L, r.amount)
        assertEquals(6L, r.categoryId)
        assertEquals(1L, r.walletId)
    }

    @Test fun case24_pendidikanBuku() {
        val r = parse("beli buku 120rb bca")
        assertEquals(120_000L, r.amount)
        assertEquals(7L, r.categoryId)
        assertEquals(3L, r.walletId)
    }

    @Test fun case25_pulsaTagihan() {
        val r = parse("isi pulsa 50rb gopay")
        assertEquals(50_000L, r.amount)
        assertEquals(3L, r.categoryId)
        assertEquals(2L, r.walletId)
    }

    @Test fun case26_ambiguousNoteOnly() {
        val r = parse("urusan mendadak")
        assertEquals(TransactionType.EXPENSE, r.type)
        assertNull(r.amount)
        assertNull(r.categoryId)
        assertNull(r.walletId)
        assertEquals("Urusan mendadak", r.note)
    }

    @Test fun case27_bensinPertamax() {
        val r = parse("bensin 35ribu tunai")
        assertEquals(35_000L, r.amount)
        assertEquals(2L, r.categoryId)
        assertEquals(1L, r.walletId)
    }

    @Test fun case28_customCategoryRecognizedAndArchivedIgnored() {
        val customCats = categories + listOf(
            CategoryEntity(99L, "Kopi Susu", CategoryType.EXPENSE, "restaurant", "coral", false, isArchived = false),
            CategoryEntity(100L, "Langganan Lama", CategoryType.EXPENSE, "receipt_long", "stone", false, isArchived = true)
        )
        val r1 = QuickEntryParser.parse("beli kopi susu 28rb", customCats, wallets, nowMillis, zone)
        assertEquals(28_000L, r1.amount)
        assertEquals(99L, r1.categoryId)

        val r2 = QuickEntryParser.parse("bayar langganan lama 50rb", customCats, wallets, nowMillis, zone)
        assertEquals(50_000L, r2.amount)
        assertNull(r2.categoryId)
    }
}
