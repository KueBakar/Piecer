package com.piecer.app.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutPiecesLogicTest {

    @Test
    fun resolveAboutPieceCount_returnsExpectedCountForModes() {
        assertEquals(
            ABOUT_FULL_PIECE_COUNT,
            resolveAboutPieceCount(reduceMotion = false, lowFpsDetected = false)
        )
        assertEquals(
            ABOUT_REDUCED_PIECE_COUNT,
            resolveAboutPieceCount(reduceMotion = false, lowFpsDetected = true)
        )
        assertEquals(
            ABOUT_FULL_PIECE_COUNT,
            resolveAboutPieceCount(reduceMotion = true, lowFpsDetected = false)
        )
        assertEquals(
            ABOUT_REDUCED_PIECE_COUNT,
            resolveAboutPieceCount(reduceMotion = true, lowFpsDetected = true)
        )
    }

    @Test
    fun shouldUseReducedAboutPieces_checks22MsThreshold() {
        assertFalse(shouldUseReducedAboutPieces(sampleCount = 0, sampleTotalNs = 0L))
        assertFalse(shouldUseReducedAboutPieces(sampleCount = 24, sampleTotalNs = 24 * 16_600_000L))
        assertFalse(shouldUseReducedAboutPieces(sampleCount = 10, sampleTotalNs = 10 * 22_000_000L))
        assertTrue(shouldUseReducedAboutPieces(sampleCount = 15, sampleTotalNs = 15 * 24_000_000L))
    }

    @Test
    fun computeStaggeredAlpha_clampsAndInterpolates() {
        assertEquals(0f, computeStaggeredAlpha(0.10f, 0.20f, 0.70f), 0.001f)
        assertEquals(0.5f, computeStaggeredAlpha(0.45f, 0.20f, 0.70f), 0.001f)
        assertEquals(1f, computeStaggeredAlpha(0.80f, 0.20f, 0.70f), 0.001f)
    }
}
