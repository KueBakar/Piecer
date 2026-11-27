package com.piecer.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashTimelineTest {

    @Test
    fun clampFrameDeltaMs_clampsNegativeAndLargeDeltas() {
        assertEquals(0f, clampFrameDeltaMs(-5_000_000L), 0.001f)
        assertEquals(16.6f, clampFrameDeltaMs(16_600_000L), 0.001f)
        assertEquals(50f, clampFrameDeltaMs(120_000_000L), 0.001f)
    }

    @Test
    fun shouldUseLightMode_evaluatesAverageFrameTime() {
        assertFalse(shouldUseLightMode(sampleCount = 0, sampleTotalNs = 0L))
        assertFalse(shouldUseLightMode(sampleCount = 24, sampleTotalNs = 24 * 16_600_000L))
        assertFalse(shouldUseLightMode(sampleCount = 10, sampleTotalNs = 10 * 22_000_000L))
        assertTrue(shouldUseLightMode(sampleCount = 15, sampleTotalNs = 15 * 25_000_000L))
    }

    @Test
    fun computeMainBodyState_followsFullLightAndReduceMotionTimelines() {
        val initial = computeMainBodyState(50f, reduceMotion = false, lightMode = false)
        assertEquals(0f, initial.sweepDeg, 0.001f)
        assertEquals(0.94f, initial.scale, 0.001f)

        val mid = computeMainBodyState(450f, reduceMotion = false, lightMode = false)
        assertTrue(mid.sweepDeg in 100f..260f)
        assertTrue(mid.scale in 0.95f..0.999f)

        val fullEnd = computeMainBodyState(800f, reduceMotion = false, lightMode = false)
        assertEquals(270f, fullEnd.sweepDeg, 0.001f)
        assertEquals(1f, fullEnd.scale, 0.001f)

        val lightEnd = computeMainBodyState(650f, reduceMotion = false, lightMode = true)
        assertEquals(270f, lightEnd.sweepDeg, 0.001f)
        assertEquals(1f, lightEnd.scale, 0.001f)

        val reduced = computeMainBodyState(0f, reduceMotion = true, lightMode = false)
        assertEquals(270f, reduced.sweepDeg, 0.001f)
        assertEquals(1f, reduced.scale, 0.001f)
    }

    @Test
    fun computeSliceState_overshootsInFullModeAndNotInLightMode() {
        val before = computeSliceState(500f, reduceMotion = false, lightMode = false)
        assertEquals(0f, before.alpha, 0.001f)
        assertEquals(0f, before.progress, 0.001f)

        // 30% dari 550..1150 ms = 550 + 180 = 730 ms -> alpha harus 1.0
        val thirtyPercent = computeSliceState(730f, reduceMotion = false, lightMode = false)
        assertEquals(1f, thirtyPercent.alpha, 0.001f)

        // Sekitar 75% dari 550..1150 ms = 1000 ms -> overshoot > 1.0
        val overshootSample = computeSliceState(1000f, reduceMotion = false, lightMode = false)
        assertTrue(overshootSample.progress > 1f)

        val fullEnd = computeSliceState(1150f, reduceMotion = false, lightMode = false)
        assertEquals(1f, fullEnd.progress, 0.001f)
        assertEquals(1f, fullEnd.alpha, 0.001f)

        // Mode ringan: tanpa overshoot
        val lightMid = computeSliceState(800f, reduceMotion = false, lightMode = true)
        assertTrue(lightMid.progress <= 1f)
    }

    @Test
    fun computeLogoPulseScale_onlyActiveInFullMode() {
        assertEquals(1f, computeLogoPulseScale(1040f, reduceMotion = false, lightMode = false), 0.001f)
        assertEquals(1.035f, computeLogoPulseScale(1150f, reduceMotion = false, lightMode = false), 0.001f)
        assertEquals(1f, computeLogoPulseScale(1250f, reduceMotion = false, lightMode = false), 0.001f)

        assertEquals(1f, computeLogoPulseScale(1150f, reduceMotion = false, lightMode = true), 0.001f)
        assertEquals(1f, computeLogoPulseScale(1150f, reduceMotion = true, lightMode = false), 0.001f)
    }

    @Test
    fun computeLetterState_staggersInFullModeAndFadesTogetherInLightMode() {
        val firstStart = computeLetterState(1190f, letterIndex = 0, reduceMotion = false, lightMode = false)
        val lastNotStarted = computeLetterState(1190f, letterIndex = 5, reduceMotion = false, lightMode = false)
        assertTrue(firstStart.alpha > 0f)
        assertEquals(0f, lastNotStarted.alpha, 0.001f)
        assertEquals(1f, lastNotStarted.offsetFraction, 0.001f)

        val allDone = computeLetterState(1500f, letterIndex = 5, reduceMotion = false, lightMode = false)
        assertEquals(1f, allDone.alpha, 0.001f)
        assertEquals(0f, allDone.offsetFraction, 0.001f)

        val lightFirst = computeLetterState(925f, letterIndex = 0, reduceMotion = false, lightMode = true)
        val lightLast = computeLetterState(925f, letterIndex = 5, reduceMotion = false, lightMode = true)
        assertEquals(lightFirst.alpha, lightLast.alpha, 0.001f)
        assertEquals(0f, lightFirst.offsetFraction, 0.001f)
    }

    @Test
    fun computeTaglineAndExitState_behaveAsExpected() {
        assertEquals(0f, computeTaglineAlpha(1350f, reduceMotion = false, lightMode = false), 0.001f)
        assertEquals(1f, computeTaglineAlpha(1700f, reduceMotion = false, lightMode = false), 0.001f)
        assertEquals(1f, computeTaglineAlpha(0f, reduceMotion = true, lightMode = false), 0.001f)

        val beforeExit = computeExitState(1750f, exitStartMs = 1800f, exitDurationMs = 200f)
        assertEquals(1f, beforeExit.alpha, 0.001f)
        assertEquals(1f, beforeExit.scale, 0.001f)

        val midExit = computeExitState(1900f, exitStartMs = 1800f, exitDurationMs = 200f)
        assertEquals(0.5f, midExit.alpha, 0.001f)
        assertEquals(0.985f, midExit.scale, 0.001f)

        val endExit = computeExitState(2000f, exitStartMs = 1800f, exitDurationMs = 200f)
        assertEquals(0f, endExit.alpha, 0.001f)
        assertEquals(0.97f, endExit.scale, 0.001f)
    }
}
