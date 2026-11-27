package com.piecer.app.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing

internal const val SPLASH_LOGO_MAX_DP = 124f
internal const val SPLASH_LOGO_MAX_WIDTH_FRACTION = 0.38f

internal const val SPLASH_FRAME_DELTA_CAP_NS = 50_000_000L
internal const val SPLASH_ADAPTIVE_SAMPLE_WINDOW_MS = 400f
internal const val SPLASH_LOW_FPS_FRAME_THRESHOLD_MS = 22f

internal const val SPLASH_MAIN_SWEEP_START_MS = 100f
internal const val SPLASH_MAIN_SWEEP_END_MS = 800f
internal const val SPLASH_MAIN_SCALE_START = 0.94f

internal const val SPLASH_SLICE_START_MS = 550f
internal const val SPLASH_SLICE_END_MS = 1150f

internal const val SPLASH_PULSE_START_MS = 1050f
internal const val SPLASH_PULSE_PEAK_MS = 1150f
internal const val SPLASH_PULSE_END_MS = 1250f
internal const val SPLASH_PULSE_MAX_SCALE = 1.035f

internal const val SPLASH_TITLE_START_MS = 1150f
internal const val SPLASH_TITLE_LETTER_STAGGER_MS = 55f
internal const val SPLASH_TITLE_LETTER_DURATION_MS = 75f

internal const val SPLASH_TAGLINE_START_MS = 1400f
internal const val SPLASH_TAGLINE_END_MS = 1700f

internal const val SPLASH_HOLD_END_MS = 1800f
internal const val SPLASH_EXIT_DURATION_MS = 200f
internal const val SPLASH_SKIP_EXIT_DURATION_MS = 150f

internal const val SPLASH_REDUCE_MOTION_HOLD_MS = 500f
internal const val SPLASH_REDUCE_MOTION_EXIT_DURATION_MS = 200f

internal const val SPLASH_LIGHT_MAIN_END_MS = 650f
internal const val SPLASH_LIGHT_SLICE_START_MS = 450f
internal const val SPLASH_LIGHT_SLICE_END_MS = 900f
internal const val SPLASH_LIGHT_TITLE_START_MS = 800f
internal const val SPLASH_LIGHT_TITLE_END_MS = 1050f
internal const val SPLASH_LIGHT_TAGLINE_START_MS = 900f
internal const val SPLASH_LIGHT_TAGLINE_END_MS = 1100f
internal const val SPLASH_LIGHT_HOLD_END_MS = 1100f

internal val SplashSliceOvershootEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

internal data class MainBodyAnimState(
    val sweepDeg: Float,
    val scale: Float
)

internal data class SliceAnimState(
    val progress: Float,
    val alpha: Float
)

internal data class LetterAnimState(
    val alpha: Float,
    val offsetFraction: Float
)

internal data class ExitAnimState(
    val alpha: Float,
    val scale: Float
)

internal object SplashSessionConfig {
    @Volatile
    var lightMode: Boolean? = null
}

internal fun clampFrameDeltaMs(deltaNs: Long): Float {
    return deltaNs.coerceIn(0L, SPLASH_FRAME_DELTA_CAP_NS) / 1_000_000f
}

internal fun shouldUseLightMode(sampleCount: Int, sampleTotalNs: Long): Boolean {
    if (sampleCount <= 0) return false
    val avgFrameMs = sampleTotalNs.toFloat() / sampleCount / 1_000_000f
    return avgFrameMs > SPLASH_LOW_FPS_FRAME_THRESHOLD_MS
}

internal fun computeHoldEndMs(reduceMotion: Boolean, lightMode: Boolean): Float {
    return when {
        reduceMotion -> SPLASH_REDUCE_MOTION_HOLD_MS
        lightMode -> SPLASH_LIGHT_HOLD_END_MS
        else -> SPLASH_HOLD_END_MS
    }
}

internal fun computeMainBodyState(
    elapsedMs: Float,
    reduceMotion: Boolean,
    lightMode: Boolean
): MainBodyAnimState {
    if (reduceMotion) return MainBodyAnimState(sweepDeg = 270f, scale = 1f)
    val endMs = if (lightMode) SPLASH_LIGHT_MAIN_END_MS else SPLASH_MAIN_SWEEP_END_MS
    if (elapsedMs <= SPLASH_MAIN_SWEEP_START_MS) {
        return MainBodyAnimState(sweepDeg = 0f, scale = SPLASH_MAIN_SCALE_START)
    }
    val raw = ((elapsedMs - SPLASH_MAIN_SWEEP_START_MS) / (endMs - SPLASH_MAIN_SWEEP_START_MS))
        .coerceIn(0f, 1f)
    val eased = FastOutSlowInEasing.transform(raw)
    return MainBodyAnimState(
        sweepDeg = 270f * eased,
        scale = SPLASH_MAIN_SCALE_START + (1f - SPLASH_MAIN_SCALE_START) * eased
    )
}

internal fun computeSliceState(
    elapsedMs: Float,
    reduceMotion: Boolean,
    lightMode: Boolean
): SliceAnimState {
    if (reduceMotion) return SliceAnimState(progress = 1f, alpha = 1f)
    val startMs = if (lightMode) SPLASH_LIGHT_SLICE_START_MS else SPLASH_SLICE_START_MS
    val endMs = if (lightMode) SPLASH_LIGHT_SLICE_END_MS else SPLASH_SLICE_END_MS
    if (elapsedMs <= startMs) return SliceAnimState(progress = 0f, alpha = 0f)
    val raw = ((elapsedMs - startMs) / (endMs - startMs)).coerceIn(0f, 1f)
    val progress = if (lightMode) {
        FastOutSlowInEasing.transform(raw)
    } else {
        SplashSliceOvershootEasing.transform(raw)
    }
    val alpha = (raw / 0.30f).coerceIn(0f, 1f)
    return SliceAnimState(progress = progress, alpha = alpha)
}

internal fun computeLogoPulseScale(
    elapsedMs: Float,
    reduceMotion: Boolean,
    lightMode: Boolean
): Float {
    if (reduceMotion || lightMode) return 1f
    return when {
        elapsedMs < SPLASH_PULSE_START_MS || elapsedMs >= SPLASH_PULSE_END_MS -> 1f
        elapsedMs < SPLASH_PULSE_PEAK_MS -> {
            val p = FastOutSlowInEasing.transform(
                ((elapsedMs - SPLASH_PULSE_START_MS) / (SPLASH_PULSE_PEAK_MS - SPLASH_PULSE_START_MS))
                    .coerceIn(0f, 1f)
            )
            1f + (SPLASH_PULSE_MAX_SCALE - 1f) * p
        }
        else -> {
            val p = FastOutSlowInEasing.transform(
                ((elapsedMs - SPLASH_PULSE_PEAK_MS) / (SPLASH_PULSE_END_MS - SPLASH_PULSE_PEAK_MS))
                    .coerceIn(0f, 1f)
            )
            SPLASH_PULSE_MAX_SCALE - (SPLASH_PULSE_MAX_SCALE - 1f) * p
        }
    }
}

internal fun computeLetterState(
    elapsedMs: Float,
    letterIndex: Int,
    reduceMotion: Boolean,
    lightMode: Boolean
): LetterAnimState {
    if (reduceMotion) return LetterAnimState(alpha = 1f, offsetFraction = 0f)
    if (lightMode) {
        val raw = ((elapsedMs - SPLASH_LIGHT_TITLE_START_MS) /
            (SPLASH_LIGHT_TITLE_END_MS - SPLASH_LIGHT_TITLE_START_MS)).coerceIn(0f, 1f)
        val eased = FastOutSlowInEasing.transform(raw)
        return LetterAnimState(alpha = eased, offsetFraction = 0f)
    }
    val letterStart = SPLASH_TITLE_START_MS + letterIndex * SPLASH_TITLE_LETTER_STAGGER_MS
    val raw = ((elapsedMs - letterStart) / SPLASH_TITLE_LETTER_DURATION_MS).coerceIn(0f, 1f)
    val eased = FastOutSlowInEasing.transform(raw)
    return LetterAnimState(alpha = eased, offsetFraction = 1f - eased)
}

internal fun computeTaglineAlpha(
    elapsedMs: Float,
    reduceMotion: Boolean,
    lightMode: Boolean
): Float {
    if (reduceMotion) return 1f
    val startMs = if (lightMode) SPLASH_LIGHT_TAGLINE_START_MS else SPLASH_TAGLINE_START_MS
    val endMs = if (lightMode) SPLASH_LIGHT_TAGLINE_END_MS else SPLASH_TAGLINE_END_MS
    val raw = ((elapsedMs - startMs) / (endMs - startMs)).coerceIn(0f, 1f)
    return FastOutSlowInEasing.transform(raw)
}

internal fun computeExitState(
    elapsedMs: Float,
    exitStartMs: Float,
    exitDurationMs: Float
): ExitAnimState {
    if (exitStartMs.isInfinite() || elapsedMs <= exitStartMs) {
        return ExitAnimState(alpha = 1f, scale = 1f)
    }
    val p = ((elapsedMs - exitStartMs) / exitDurationMs).coerceIn(0f, 1f)
    return ExitAnimState(
        alpha = 1f - p,
        scale = 1f - 0.03f * p
    )
}
