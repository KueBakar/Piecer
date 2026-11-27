package com.piecer.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piecer.app.R
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.ui.theme.PlusJakartaSans
import com.piecer.app.ui.theme.SplashInk
import com.piecer.app.ui.theme.SplashWhite

private val BRAND_LETTERS = arrayOf("P", "I", "E", "C", "E", "R")

private class SplashPlaybackState {
    var lightMode: Boolean = SplashSessionConfig.lightMode == true
    var skipRequested: Boolean = false
    var exitStartMs: Float = Float.POSITIVE_INFINITY
    var exitDurationMs: Float = SPLASH_EXIT_DURATION_MS
}

@Composable
fun SplashAnimation(
    reduceMotion: Boolean,
    isAppReady: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val piecerColors = PiecerTheme.colors
    val spacing = PiecerTheme.spacing
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val logoSizeDp = remember(screenWidthDp) {
        minOf(SPLASH_LOGO_MAX_DP, screenWidthDp * SPLASH_LOGO_MAX_WIDTH_FRACTION).dp
    }
    val letterSlidePx = remember(density) { with(density) { 10.dp.toPx() } }

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

    val elapsedMs = remember { Animatable(0f) }
    val playbackState = remember { SplashPlaybackState() }

    LaunchedEffect(reduceMotion) {
        playbackState.lightMode = SplashSessionConfig.lightMode == true
        playbackState.skipRequested = false
        playbackState.exitStartMs = Float.POSITIVE_INFINITY
        playbackState.exitDurationMs = if (reduceMotion) {
            SPLASH_REDUCE_MOTION_EXIT_DURATION_MS
        } else {
            SPLASH_EXIT_DURATION_MS
        }
        elapsedMs.snapTo(0f)

        var prevFrameNanos = withFrameNanos { it }
        var currentElapsed = 0f
        var sampleCount = 0
        var sampleTotalNs = 0L
        var adaptiveEvaluated = reduceMotion || SplashSessionConfig.lightMode != null

        while (true) {
            val frameNanos = withFrameNanos { it }
            val rawDeltaNs = frameNanos - prevFrameNanos
            prevFrameNanos = frameNanos

            currentElapsed += clampFrameDeltaMs(rawDeltaNs)

            if (!adaptiveEvaluated && rawDeltaNs > 0L) {
                if (currentElapsed <= SPLASH_ADAPTIVE_SAMPLE_WINDOW_MS) {
                    sampleTotalNs += rawDeltaNs
                    sampleCount++
                } else {
                    adaptiveEvaluated = true
                    val detectedLight = shouldUseLightMode(sampleCount, sampleTotalNs)
                    playbackState.lightMode = detectedLight
                    SplashSessionConfig.lightMode = detectedLight
                }
            }

            if (playbackState.exitStartMs.isInfinite()) {
                val holdEndMs = computeHoldEndMs(reduceMotion, playbackState.lightMode)
                if (playbackState.skipRequested && latestIsAppReady) {
                    playbackState.exitStartMs = currentElapsed
                    playbackState.exitDurationMs = SPLASH_SKIP_EXIT_DURATION_MS
                } else if (currentElapsed >= holdEndMs && latestIsAppReady) {
                    playbackState.exitStartMs = currentElapsed
                    playbackState.exitDurationMs = if (reduceMotion) {
                        SPLASH_REDUCE_MOTION_EXIT_DURATION_MS
                    } else {
                        SPLASH_EXIT_DURATION_MS
                    }
                }
            }

            elapsedMs.snapTo(currentElapsed)

            if (!playbackState.exitStartMs.isInfinite() &&
                currentElapsed >= playbackState.exitStartMs + playbackState.exitDurationMs
            ) {
                break
            }
        }
        finishOnce()
    }

    val taglineColor = piecerColors.mutedText

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val exit = computeExitState(
                    elapsedMs = elapsedMs.value,
                    exitStartMs = playbackState.exitStartMs,
                    exitDurationMs = playbackState.exitDurationMs
                )
                alpha = exit.alpha
            }
            .background(SplashInk)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    playbackState.skipRequested = true
                }
            ),
        contentAlignment = BiasAlignment(0f, -0.08f)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = spacing.xl, vertical = spacing.lg)
                .graphicsLayer {
                    val exit = computeExitState(
                        elapsedMs = elapsedMs.value,
                        exitStartMs = playbackState.exitStartMs,
                        exitDurationMs = playbackState.exitDurationMs
                    )
                    scaleX = exit.scale
                    scaleY = exit.scale
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(logoSizeDp)
                    .graphicsLayer {
                        val pulseScale = computeLogoPulseScale(
                            elapsedMs = elapsedMs.value,
                            reduceMotion = reduceMotion,
                            lightMode = playbackState.lightMode
                        )
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .drawWithCache {
                        val r = size.minDimension / 2.14f
                        val mainCenter = Offset(
                            x = size.width / 2f - 0.07f * r,
                            y = size.height / 2f + 0.07f * r
                        )
                        val sliceR = 0.94f * r
                        val sliceCenterFinal = Offset(
                            x = mainCenter.x + 0.20f * r,
                            y = mainCenter.y - 0.20f * r
                        )
                        val sliceFinalTopLeft = Offset(
                            x = sliceCenterFinal.x - sliceR,
                            y = sliceCenterFinal.y - sliceR
                        )
                        val sliceSize = Size(2f * sliceR, 2f * sliceR)
                        val sliceStartOffset = Offset(1.0f * r, -1.0f * r)

                        onDrawBehind {
                            val t = elapsedMs.value
                            val light = playbackState.lightMode

                            val mainState = computeMainBodyState(t, reduceMotion, light)
                            if (mainState.sweepDeg > 0.01f) {
                                val rScaled = r * mainState.scale
                                drawArc(
                                    color = SplashWhite,
                                    startAngle = 0f,
                                    sweepAngle = mainState.sweepDeg,
                                    useCenter = true,
                                    topLeft = Offset(mainCenter.x - rScaled, mainCenter.y - rScaled),
                                    size = Size(2f * rScaled, 2f * rScaled)
                                )
                            }

                            val sliceState = computeSliceState(t, reduceMotion, light)
                            if (sliceState.alpha > 0.005f) {
                                val inv = 1f - sliceState.progress
                                drawArc(
                                    color = SplashWhite,
                                    startAngle = -90f,
                                    sweepAngle = 90f,
                                    useCenter = true,
                                    topLeft = Offset(
                                        x = sliceFinalTopLeft.x + sliceStartOffset.x * inv,
                                        y = sliceFinalTopLeft.y + sliceStartOffset.y * inv
                                    ),
                                    size = sliceSize,
                                    alpha = sliceState.alpha
                                )
                            }
                        }
                    }
            )

            Spacer(modifier = Modifier.height(28.dp))

            val letterStyle = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (index in BRAND_LETTERS.indices) {
                    Text(
                        text = BRAND_LETTERS[index],
                        style = letterStyle,
                        color = SplashWhite,
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            val state = computeLetterState(
                                elapsedMs = elapsedMs.value,
                                letterIndex = index,
                                reduceMotion = reduceMotion,
                                lightMode = playbackState.lightMode
                            )
                            alpha = state.alpha
                            translationY = state.offsetFraction * letterSlidePx
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            Text(
                text = stringResource(id = R.string.splash_tagline),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = taglineColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = computeTaglineAlpha(
                        elapsedMs = elapsedMs.value,
                        reduceMotion = reduceMotion,
                        lightMode = playbackState.lightMode
                    )
                }
            )
        }
    }
}
