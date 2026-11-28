package com.piecer.app.util.quickentry

import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
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

class VoiceQuickEntryTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val nowMillis = LocalDate.of(2026, 11, 28)
        .atTime(10, 0)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

    private val categories = listOf(
        CategoryEntity(1L, "Makan", CategoryType.EXPENSE, "restaurant", "coral", true),
        CategoryEntity(2L, "Transport", CategoryType.EXPENSE, "directions_car", "coral", true),
        CategoryEntity(10L, "Gaji", CategoryType.INCOME, "payments", "mint", true)
    )

    private val wallets = listOf(
        WalletEntity(1L, "Tunai", WalletType.CASH, 0L, "wallet", "ink", 1L),
        WalletEntity(2L, "GoPay", WalletType.EWALLET, 0L, "ewallet", "mint", 1L)
    )

    private val context = QuickEntryContext(
        categories = categories,
        wallets = wallets,
        lastUsedWalletId = 1L,
        nowMillis = nowMillis,
        zoneId = zone
    )

    // --- Pemilihan kandidat transkrip ---

    @Test
    fun selectBest_prefersCandidateWithNominalOverCandidateWithoutNominal() {
        val candidates = listOf(
            "kopi susu",
            "kopi susu dua puluh ribu"
        )
        val selection = VoiceTranscriptSelector.selectBest(candidates, context)
        assertNotNull(selection)
        assertEquals("kopi susu dua puluh ribu", selection!!.transcript)
        assertEquals(20_000L, selection.parseResult.entries.first().amount)
    }

    @Test
    fun selectBest_prefersHigherConfidenceWhenBothHaveNominal() {
        val candidates = listOf(
            "sesuatu 20.000", // NO_CATEGORY -> confidence lebih rendah
            "makan siang 20.000" // kategori Makan cocok -> confidence lebih tinggi
        )
        val selection = VoiceTranscriptSelector.selectBest(candidates, context)
        assertNotNull(selection)
        assertEquals("makan siang 20.000", selection!!.transcript)
        assertEquals(1L, selection.parseResult.entries.first().categoryId)
    }

    @Test
    fun selectBest_parsesSpokenWordsDottedNumberAndRpPrefix() {
        val spoken = VoiceTranscriptSelector.selectBest(listOf("kopi dua puluh ribu"), context)
        assertEquals(20_000L, spoken!!.parseResult.entries.first().amount)

        val dotted = VoiceTranscriptSelector.selectBest(listOf("kopi 20.000"), context)
        assertEquals(20_000L, dotted!!.parseResult.entries.first().amount)

        val rpPrefixed = VoiceTranscriptSelector.selectBest(listOf("kopi Rp 20.000"), context)
        assertEquals(20_000L, rpPrefixed!!.parseResult.entries.first().amount)
    }

    @Test
    fun selectBest_returnsNullForEmptyOrBlankCandidates() {
        assertNull(VoiceTranscriptSelector.selectBest(emptyList(), context))
        assertNull(VoiceTranscriptSelector.selectBest(listOf("   ", ""), context))
    }

    // --- Logika kapan pratinjau muncul ---

    @Test
    fun previewPolicy_returnsFalseForSingleEntryWithoutFlags() {
        val result = QuickEntryPipeline.parse("makan siang 25rb gopay", context)
        assertEquals(1, result.entries.size)
        assertTrue(result.entries.first().flags.isEmpty())
        assertFalse(QuickEntryPreviewPolicy.shouldShowPreview(result))
    }

    @Test
    fun previewPolicy_returnsTrueForMultipleEntries() {
        val result = QuickEntryPipeline.parse("kopi 20k dan makan siang 35k", context)
        assertTrue(result.entries.size > 1)
        assertTrue(QuickEntryPreviewPolicy.shouldShowPreview(result))
    }

    @Test
    fun previewPolicy_returnsTrueWhenEntryHasAmbiguousMFlag() {
        val result = QuickEntryPipeline.parse("beli motor 15m", context)
        assertEquals(1, result.entries.size)
        assertTrue(QuickEntryFlag.AMBIGUOUS_M in result.entries.first().flags)
        assertTrue(QuickEntryPreviewPolicy.shouldShowPreview(result))
    }

    @Test
    fun previewPolicy_returnsTrueWhenEntryHasNoCategoryFlag() {
        val result = QuickEntryPipeline.parse("sesuatu yang asing 50k", context)
        assertEquals(1, result.entries.size)
        assertTrue(QuickEntryFlag.NO_CATEGORY in result.entries.first().flags)
        assertTrue(QuickEntryPreviewPolicy.shouldShowPreview(result))
    }

    @Test
    fun previewPolicy_returnsTrueWhenEntryHasLargeAmountFlag() {
        val result = QuickEntryPipeline.parse("makan 2miliar", context)
        assertEquals(1, result.entries.size)
        assertTrue(QuickEntryFlag.LARGE_AMOUNT in result.entries.first().flags)
        assertTrue(QuickEntryPreviewPolicy.shouldShowPreview(result))
    }

    @Test
    fun previewPolicy_returnsFalseForEmptyResult() {
        val result = QuickEntryParseResult(entries = emptyList(), normalizedText = "")
        assertFalse(QuickEntryPreviewPolicy.shouldShowPreview(result))
    }

    // --- Penanganan ketiadaan layanan suara ---

    @Test
    fun voiceAvailability_whenUnavailableShowsMessageOnceAndHidesMic() {
        val initial = VoiceAvailabilityState()
        val first = VoiceAvailabilityHandler.evaluateAvailability(initial, serviceAvailable = false)
        assertFalse(first.state.shouldShowMicButton)
        assertTrue(first.showUnavailableMessageNow)
        assertTrue(first.state.hasShownUnavailableMessage)

        val second = VoiceAvailabilityHandler.evaluateAvailability(first.state, serviceAvailable = false)
        assertFalse(second.state.shouldShowMicButton)
        assertFalse(second.showUnavailableMessageNow)
    }

    @Test
    fun voiceAvailability_whenLaunchFailsHidesMicAndShowsMessageOnce() {
        val initial = VoiceAvailabilityState(isServiceAvailable = true, hasShownUnavailableMessage = false)
        val failed = VoiceAvailabilityHandler.onLaunchFailure(initial)
        assertFalse(failed.state.shouldShowMicButton)
        assertTrue(failed.showUnavailableMessageNow)

        val failedAgain = VoiceAvailabilityHandler.onLaunchFailure(failed.state)
        assertFalse(failedAgain.state.shouldShowMicButton)
        assertFalse(failedAgain.showUnavailableMessageNow)
    }

    @Test
    fun voiceAvailability_whenAvailableKeepsMicVisibleAndNoMessage() {
        val res = VoiceAvailabilityHandler.evaluateAvailability(VoiceAvailabilityState(), serviceAvailable = true)
        assertTrue(res.state.shouldShowMicButton)
        assertFalse(res.showUnavailableMessageNow)
    }
}
