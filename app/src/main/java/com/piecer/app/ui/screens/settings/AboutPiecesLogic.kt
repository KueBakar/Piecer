package com.piecer.app.ui.screens.settings

internal const val ABOUT_FULL_PIECE_COUNT = 7
internal const val ABOUT_REDUCED_PIECE_COUNT = 3
internal const val ABOUT_FRAME_SAMPLE_WINDOW_MS = 400f
internal const val ABOUT_LOW_FPS_FRAME_THRESHOLD_MS = 22f
internal const val ABOUT_FRAME_DELTA_CAP_NS = 50_000_000L
internal const val ABOUT_ENTRY_DURATION_MS = 600
internal const val ABOUT_LOOP_DURATION_MS = 24_000
internal const val ABOUT_RING_DURATION_MS = 6_000

internal data class AboutPieceSpec(
    val anchorXFraction: Float,
    val anchorYFraction: Float,
    val radiusDp: Float,
    val driftXFraction: Float,
    val driftYFraction: Float,
    val driftCyclesX: Int,
    val driftCyclesY: Int,
    val phaseOffset: Float,
    val baseRotationDeg: Float,
    val rotationTurns: Int,
    val alpha: Float,
    val isMintAccent: Boolean = false
)

internal val ABOUT_PIECE_SPECS = listOf(
    AboutPieceSpec(
        anchorXFraction = 0.14f,
        anchorYFraction = 0.22f,
        radiusDp = 28f,
        driftXFraction = 0.025f,
        driftYFraction = 0.035f,
        driftCyclesX = 1,
        driftCyclesY = 2,
        phaseOffset = 0.05f,
        baseRotationDeg = 18f,
        rotationTurns = 1,
        alpha = 0.07f
    ),
    AboutPieceSpec(
        anchorXFraction = 0.84f,
        anchorYFraction = 0.20f,
        radiusDp = 34f,
        driftXFraction = 0.030f,
        driftYFraction = 0.025f,
        driftCyclesX = 2,
        driftCyclesY = 1,
        phaseOffset = 0.22f,
        baseRotationDeg = -24f,
        rotationTurns = -1,
        alpha = 0.06f
    ),
    AboutPieceSpec(
        anchorXFraction = 0.76f,
        anchorYFraction = 0.72f,
        radiusDp = 24f,
        driftXFraction = 0.022f,
        driftYFraction = 0.030f,
        driftCyclesX = 1,
        driftCyclesY = 1,
        phaseOffset = 0.41f,
        baseRotationDeg = 42f,
        rotationTurns = 1,
        alpha = 0.09f,
        isMintAccent = true
    ),
    AboutPieceSpec(
        anchorXFraction = 0.20f,
        anchorYFraction = 0.76f,
        radiusDp = 20f,
        driftXFraction = 0.028f,
        driftYFraction = 0.022f,
        driftCyclesX = 2,
        driftCyclesY = 1,
        phaseOffset = 0.58f,
        baseRotationDeg = 110f,
        rotationTurns = -1,
        alpha = 0.08f
    ),
    AboutPieceSpec(
        anchorXFraction = 0.48f,
        anchorYFraction = 0.14f,
        radiusDp = 16f,
        driftXFraction = 0.020f,
        driftYFraction = 0.028f,
        driftCyclesX = 1,
        driftCyclesY = 2,
        phaseOffset = 0.73f,
        baseRotationDeg = -65f,
        rotationTurns = 1,
        alpha = 0.05f
    ),
    AboutPieceSpec(
        anchorXFraction = 0.90f,
        anchorYFraction = 0.50f,
        radiusDp = 18f,
        driftXFraction = 0.018f,
        driftYFraction = 0.032f,
        driftCyclesX = 1,
        driftCyclesY = 1,
        phaseOffset = 0.84f,
        baseRotationDeg = 145f,
        rotationTurns = -1,
        alpha = 0.045f
    ),
    AboutPieceSpec(
        anchorXFraction = 0.08f,
        anchorYFraction = 0.52f,
        radiusDp = 22f,
        driftXFraction = 0.024f,
        driftYFraction = 0.026f,
        driftCyclesX = 2,
        driftCyclesY = 2,
        phaseOffset = 0.92f,
        baseRotationDeg = -120f,
        rotationTurns = 1,
        alpha = 0.10f
    )
)

internal fun clampAboutFrameDeltaMs(deltaNs: Long): Float {
    return deltaNs.coerceIn(0L, ABOUT_FRAME_DELTA_CAP_NS) / 1_000_000f
}

internal fun shouldUseReducedAboutPieces(sampleCount: Int, sampleTotalNs: Long): Boolean {
    if (sampleCount <= 0) return false
    val avgFrameMs = sampleTotalNs.toFloat() / sampleCount / 1_000_000f
    return avgFrameMs > ABOUT_LOW_FPS_FRAME_THRESHOLD_MS
}

internal fun resolveAboutPieceCount(
    reduceMotion: Boolean,
    lowFpsDetected: Boolean
): Int {
    return when {
        lowFpsDetected -> ABOUT_REDUCED_PIECE_COUNT
        reduceMotion -> ABOUT_FULL_PIECE_COUNT
        else -> ABOUT_FULL_PIECE_COUNT
    }
}

internal fun computeStaggeredAlpha(
    progress: Float,
    startFraction: Float,
    endFraction: Float
): Float {
    if (progress <= startFraction) return 0f
    if (progress >= endFraction) return 1f
    return ((progress - startFraction) / (endFraction - startFraction)).coerceIn(0f, 1f)
}
