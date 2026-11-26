package com.piecer.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piecer.app.R
import com.piecer.app.ui.theme.PiecerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private data class PiePieceSpec(
    val startAngle: Float,
    val sweepAngle: Float,
    val scatterDistancePx: Float,
    val scatterAngleOffset: Float
)

@Composable
fun SplashAnimation(
    reduceMotion: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val spacing = PiecerTheme.spacing

    var hasFinished by remember { mutableStateOf(false) }
    val finishOnce: () -> Unit = {
        if (!hasFinished) {
            hasFinished = true
            onFinished()
        }
    }

    val assembleProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val logoScale = remember { Animatable(1f) }
    val glowAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val textOffsetY = remember { Animatable(if (reduceMotion) 0f else 24f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            delay(600L)
            finishOnce()
        } else {
            val smoothEase = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
            assembleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 820, easing = smoothEase)
            )

            launch {
                logoScale.animateTo(
                    targetValue = 1.08f,
                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                )
                logoScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                )
            }

            launch {
                glowAlpha.animateTo(
                    targetValue = 0.28f,
                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                )
                glowAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                )
            }

            launch {
                delay(100L)
                textAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                )
            }

            launch {
                delay(100L)
                textOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 420, easing = smoothEase)
                )
            }

            delay(760L)
            screenAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
            finishOnce()
        }
    }

    val logoColor = colorScheme.onBackground
    val bgColor = colorScheme.background

    val pieces = remember {
        listOf(
            PiePieceSpec(startAngle = -88f, sweepAngle = 86f, scatterDistancePx = 56f, scatterAngleOffset = -14f),
            PiePieceSpec(startAngle = 2f, sweepAngle = 98f, scatterDistancePx = 48f, scatterAngleOffset = 10f),
            PiePieceSpec(startAngle = 104f, sweepAngle = 88f, scatterDistancePx = 60f, scatterAngleOffset = -12f),
            PiePieceSpec(startAngle = 196f, sweepAngle = 72f, scatterDistancePx = 52f, scatterAngleOffset = 14f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha.value }
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = finishOnce
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val progress = assembleProgress.value
                    val inverse = 1f - progress
                    val diameter = size.minDimension * 0.62f
                    val radius = diameter / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    if (glowAlpha.value > 0.001f) {
                        val glowRadius = radius * 1.65f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    logoColor.copy(alpha = glowAlpha.value),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = glowRadius
                            ),
                            radius = glowRadius,
                            center = center
                        )
                    }

                    pieces.forEach { piece ->
                        val midAngleDeg = piece.startAngle + piece.sweepAngle / 2f
                        val midAngleRad = Math.toRadians(midAngleDeg.toDouble())
                        val offsetDistance = piece.scatterDistancePx * inverse
                        val dx = (cos(midAngleRad) * offsetDistance).toFloat()
                        val dy = (sin(midAngleRad) * offsetDistance).toFloat()

                        val topLeft = Offset(
                            x = center.x - radius + dx,
                            y = center.y - radius + dy
                        )
                        val pieceAlpha = (0.35f + 0.65f * progress).coerceIn(0f, 1f)
                        val startAngle = piece.startAngle + piece.scatterAngleOffset * inverse
                        // Saat menyatu penuh (progress -> 1f), celah tipis menghilang menjadi lingkaran utuh
                        val gapCloseBoost = 4f * progress

                        drawArc(
                            color = logoColor.copy(alpha = pieceAlpha),
                            startAngle = startAngle - gapCloseBoost / 2f,
                            sweepAngle = piece.sweepAngle + gapCloseBoost,
                            useCenter = true,
                            topLeft = topLeft,
                            size = Size(diameter, diameter)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp
                ),
                color = logoColor,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationY = textOffsetY.value
                }
            )
        }
    }
}
