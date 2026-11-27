package com.piecer.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piecer.app.R
import com.piecer.app.ui.theme.PiecerTheme
import kotlin.math.cos
import kotlin.math.sin

private const val ASSEMBLE_END_MS = 700f
private const val POP_START_MS = 700f
private const val POP_PEAK_MS = 810f
private const val POP_END_MS = 950f
private const val TITLE_START_MS = 850f
private const val TITLE_END_MS = 1200f
private const val TAGLINE_START_MS = 1000f
private const val TAGLINE_END_MS = 1300f
private const val FADE_OUT_START_MS = 1300f
private const val FADE_OUT_DURATION_MS = 200f
private const val TOTAL_DURATION_MS = 1500f

private const val REDUCE_MOTION_HOLD_MS = 500f
private const val REDUCE_MOTION_TOTAL_MS = 700f

private const val ADAPTIVE_SAMPLE_WINDOW_MS = 400f
private const val LOW_FPS_FRAME_THRESHOLD_MS = 22f

private data class PieSliceTemplate(
    val startAngle: Float,
    val sweepAngle: Float,
    val scatterDistanceDp: Float,
    val scatterRotationDeg: Float
)

private class CachedPieSlice(
    val path: Path,
    val color: Color,
    val dirX: Float,
    val dirY: Float,
    val scatterDistancePx: Float,
    val scatterRotationDeg: Float
)

private class SplashAdaptiveState {
    var lowEndMode: Boolean = false
}

private val PIE_SLICE_TEMPLATES = listOf(
    PieSliceTemplate(startAngle = -90f, sweepAngle = 76f, scatterDistanceDp = 22f, scatterRotationDeg = -10f),
    PieSliceTemplate(startAngle = -9.5f, sweepAngle = 68f, scatterDistanceDp = 24f, scatterRotationDeg = 9f),
    PieSliceTemplate(startAngle = 63f, sweepAngle = 72f, scatterDistanceDp = 20f, scatterRotationDeg = -8f),
    PieSliceTemplate(startAngle = 139.5f, sweepAngle = 66f, scatterDistanceDp = 25f, scatterRotationDeg = 11f),
    PieSliceTemplate(startAngle = 210f, sweepAngle = 55.5f, scatterDistanceDp = 26f, scatterRotationDeg = -12f)
)

@Composable
fun SplashAnimation(
    reduceMotion: Boolean,
    isAppReady: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val piecerColors = PiecerTheme.colors
    val spacing = PiecerTheme.spacing
    val density = LocalDensity.current
    val titleSlidePx = remember(density) { with(density) { 12.dp.toPx() } }

    var hasFinished by remember { mutableStateOf(false) }
    val latestIsAppReady by rememberUpdatedState(isAppReady)
    val latestOnFinished by rememberUpdatedState(onFinished)

    val finishOnce: () -> Unit = remember {
        {
            if (!hasFinished) {
                hasFinished = true
                latestOnFinished()
            }
        }
    }

    // Satu progress utama berbasis waktu (ms), hanya dibaca di fase gambar
    val elapsedMs = remember { Animatable(0f) }
    val adaptiveState = remember { SplashAdaptiveState() }

    LaunchedEffect(reduceMotion) {
        adaptiveState.lowEndMode = false
        elapsedMs.snapTo(0f)

        if (reduceMotion) {
            val startNanos = withFrameNanos { it }
            while (true) {
                val frameNanos = withFrameNanos { it }
                val elapsed = (frameNanos - startNanos) / 1_000_000f
                if (elapsed >= REDUCE_MOTION_HOLD_MS && latestIsAppReady) {
                    break
                }
            }
            val fadeStartNanos = withFrameNanos { it }
            while (true) {
                val frameNanos = withFrameNanos { it }
                val fadeElapsed = (frameNanos - fadeStartNanos) / 1_000_000f
                elapsedMs.snapTo((REDUCE_MOTION_HOLD_MS + fadeElapsed).coerceAtMost(REDUCE_MOTION_TOTAL_MS))
                if (fadeElapsed >= FADE_OUT_DURATION_MS) break
            }
            finishOnce()
        } else {
            val startNanos = withFrameNanos { it }
            var prevFrameNanos = startNanos
            var sampleCount = 0
            var sampleTotalNs = 0L
            var adaptiveEvaluated = false

            while (true) {
                val frameNanos = withFrameNanos { it }
                val deltaNs = frameNanos - prevFrameNanos
                prevFrameNanos = frameNanos
                val currentElapsed = (frameNanos - startNanos) / 1_000_000f

                if (!adaptiveEvaluated && currentElapsed <= ADAPTIVE_SAMPLE_WINDOW_MS && deltaNs > 0L) {
                    sampleTotalNs += deltaNs
                    sampleCount++
                } else if (!adaptiveEvaluated && currentElapsed > ADAPTIVE_SAMPLE_WINDOW_MS) {
                    adaptiveEvaluated = true
                    if (sampleCount > 0) {
                        val avgFrameMs = sampleTotalNs.toFloat() / sampleCount / 1_000_000f
                        if (avgFrameMs > LOW_FPS_FRAME_THRESHOLD_MS) {
                            adaptiveState.lowEndMode = true
                        }
                    }
                }

                elapsedMs.snapTo(currentElapsed.coerceAtMost(FADE_OUT_START_MS))
                if (currentElapsed >= FADE_OUT_START_MS && latestIsAppReady) {
                    break
                }
            }

            val fadeStartNanos = withFrameNanos { it }
            while (true) {
                val frameNanos = withFrameNanos { it }
                val fadeElapsed = (frameNanos - fadeStartNanos) / 1_000_000f
                elapsedMs.snapTo((FADE_OUT_START_MS + fadeElapsed).coerceAtMost(TOTAL_DURATION_MS))
                if (fadeElapsed >= FADE_OUT_DURATION_MS) break
            }
            finishOnce()
        }
    }

    val bgColor = colorScheme.background
    val titleColor = colorScheme.onBackground
    val taglineColor = piecerColors.mutedText
    val sliceColors = piecerColors.splashPieColors
    val glowColor = piecerColors.mint

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val t = elapsedMs.value
                alpha = if (reduceMotion) {
                    1f - ((t - REDUCE_MOTION_HOLD_MS) / FADE_OUT_DURATION_MS).coerceIn(0f, 1f)
                } else {
                    1f - ((t - FADE_OUT_START_MS) / FADE_OUT_DURATION_MS).coerceIn(0f, 1f)
                }
            }
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (latestIsAppReady) finishOnce()
                }
            ),
        contentAlignment = BiasAlignment(0f, -0.08f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.xl, vertical = spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .graphicsLayer {
                        if (reduceMotion || adaptiveState.lowEndMode) {
                            scaleX = 1f
                            scaleY = 1f
                        } else {
                            val t = elapsedMs.value
                            val scale = when {
                                t < POP_START_MS || t >= POP_END_MS -> 1f
                                t < POP_PEAK_MS -> {
                                    val p = FastOutSlowInEasing.transform(
                                        ((t - POP_START_MS) / (POP_PEAK_MS - POP_START_MS)).coerceIn(0f, 1f)
                                    )
                                    1f + 0.06f * p
                                }
                                else -> {
                                    val p = FastOutSlowInEasing.transform(
                                        ((t - POP_PEAK_MS) / (POP_END_MS - POP_PEAK_MS)).coerceIn(0f, 1f)
                                    )
                                    1.06f - 0.06f * p
                                }
                            }
                            scaleX = scale
                            scaleY = scale
                        }
                    }
                    .drawWithCache {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val diameter = size.minDimension * 0.66f
                        val radius = diameter / 2f
                        val pieRect = Rect(
                            left = center.x - radius,
                            top = center.y - radius,
                            right = center.x + radius,
                            bottom = center.y + radius
                        )
                        val glowRadius = radius * 1.6f
                        val glowBrush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = 0.22f), Color.Transparent),
                            center = center,
                            radius = glowRadius
                        )
                        val cachedSlices = PIE_SLICE_TEMPLATES.mapIndexed { index, spec ->
                            val midAngleRad = Math.toRadians((spec.startAngle + spec.sweepAngle / 2f).toDouble())
                            val path = Path().apply {
                                moveTo(center.x, center.y)
                                arcTo(
                                    rect = pieRect,
                                    startAngleDegrees = spec.startAngle,
                                    sweepAngleDegrees = spec.sweepAngle,
                                    forceMoveTo = false
                                )
                                close()
                            }
                            CachedPieSlice(
                                path = path,
                                color = sliceColors[index % sliceColors.size],
                                dirX = cos(midAngleRad).toFloat(),
                                dirY = sin(midAngleRad).toFloat(),
                                scatterDistancePx = spec.scatterDistanceDp.dp.toPx(),
                                scatterRotationDeg = spec.scatterRotationDeg
                            )
                        }

                        onDrawBehind {
                            val t = elapsedMs.value
                            val assembleProgress = if (reduceMotion) {
                                1f
                            } else {
                                FastOutSlowInEasing.transform((t / ASSEMBLE_END_MS).coerceIn(0f, 1f))
                            }
                            val inverse = 1f - assembleProgress
                            val pieceAlpha = (0.35f + 0.65f * assembleProgress).coerceIn(0f, 1f)

                            if (!reduceMotion && !adaptiveState.lowEndMode && t in POP_START_MS..POP_END_MS) {
                                val glowAlpha = if (t < POP_PEAK_MS) {
                                    FastOutSlowInEasing.transform(
                                        ((t - POP_START_MS) / (POP_PEAK_MS - POP_START_MS)).coerceIn(0f, 1f)
                                    )
                                } else {
                                    1f - FastOutSlowInEasing.transform(
                                        ((t - POP_PEAK_MS) / (POP_END_MS - POP_PEAK_MS)).coerceIn(0f, 1f)
                                    )
                                }
                                if (glowAlpha > 0.005f) {
                                    drawCircle(
                                        brush = glowBrush,
                                        radius = glowRadius,
                                        center = center,
                                        alpha = glowAlpha
                                    )
                                }
                            }

                            for (i in cachedSlices.indices) {
                                val slice = cachedSlices[i]
                                if (inverse <= 0.0005f) {
                                    drawPath(
                                        path = slice.path,
                                        color = slice.color,
                                        alpha = pieceAlpha
                                    )
                                } else {
                                    val dx = slice.dirX * slice.scatterDistancePx * inverse
                                    val dy = slice.dirY * slice.scatterDistancePx * inverse
                                    val rot = slice.scatterRotationDeg * inverse
                                    withTransform({
                                        translate(left = dx, top = dy)
                                        rotate(degrees = rot, pivot = center)
                                    }) {
                                        drawPath(
                                            path = slice.path,
                                            color = slice.color,
                                            alpha = pieceAlpha
                                        )
                                    }
                                }
                            }
                        }
                    }
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp
                ),
                color = titleColor,
                maxLines = 1,
                modifier = Modifier.graphicsLayer {
                    if (reduceMotion) {
                        alpha = 1f
                        translationY = 0f
                    } else {
                        val raw = ((elapsedMs.value - TITLE_START_MS) / (TITLE_END_MS - TITLE_START_MS))
                            .coerceIn(0f, 1f)
                        val eased = FastOutSlowInEasing.transform(raw)
                        alpha = eased
                        translationY = if (adaptiveState.lowEndMode) 0f else (1f - eased) * titleSlidePx
                    }
                }
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            Text(
                text = stringResource(id = R.string.splash_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = taglineColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = if (reduceMotion) {
                        1f
                    } else {
                        val raw = ((elapsedMs.value - TAGLINE_START_MS) / (TAGLINE_END_MS - TAGLINE_START_MS))
                            .coerceIn(0f, 1f)
                        FastOutSlowInEasing.transform(raw)
                    }
                }
            )
        }
    }
}
