package com.piecer.app.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.piecer.app.R
import com.piecer.app.ui.theme.LocalReduceMotion
import com.piecer.app.ui.theme.PiecerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FRAME_BUDGET_THRESHOLD_MS = 22f
private const val FRAME_SAMPLE_COUNT = 12
private const val PULSE_INTERVAL_MS = 6000L

@Composable
fun PiecerBottomBar(
    currentRoute: String?,
    onTabSelected: (PiecerTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    val reduceMotion = LocalReduceMotion.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var isLightMode by remember { mutableStateOf(false) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            isLightMode = false
            return@LaunchedEffect
        }
        var prevNanos = 0L
        var totalDeltaNanos = 0L
        for (i in 0..FRAME_SAMPLE_COUNT) {
            withFrameNanos { frameNanos ->
                if (i > 0) {
                    totalDeltaNanos += (frameNanos - prevNanos)
                }
                prevNanos = frameNanos
            }
        }
        val avgFrameMs = totalDeltaNanos / (FRAME_SAMPLE_COUNT * 1_000_000f)
        if (avgFrameMs > FRAME_BUDGET_THRESHOLD_MS) {
            isLightMode = true
        }
    }

    val tabs = PiecerTab.entries
    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.let { if (it >= 0) it else 0 }
    val pillProgress = remember { Animatable(selectedIndex.toFloat()) }
    val tabCenters = remember { FloatArray(tabs.size) }
    val tabWidths = remember { FloatArray(tabs.size) }

    LaunchedEffect(selectedIndex, reduceMotion, isLightMode) {
        val target = selectedIndex.toFloat()
        when {
            reduceMotion -> pillProgress.snapTo(target)
            isLightMode -> pillProgress.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
            )
            else -> pillProgress.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val pillColor = colorScheme.surfaceVariant
    val pillHorizontalInsetPx = with(density) { 6.dp.toPx() }
    val pillVerticalInsetPx = with(density) { 12.dp.toPx() }
    val minPillWidthPx = with(density) { 44.dp.toPx() }

    val fabInteractionSource = remember { MutableInteractionSource() }
    val isFabPressed by fabInteractionSource.collectIsPressedAsState()
    val fabScale by animateFloatAsState(
        targetValue = if (isFabPressed && !reduceMotion) 0.88f else 1f,
        animationSpec = when {
            reduceMotion -> snap()
            isLightMode -> tween(durationMillis = 120, easing = FastOutSlowInEasing)
            else -> spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
        },
        label = "fabScale"
    )
    val fabIconRotation = remember { Animatable(0f) }
    val pressRingProgress = remember { Animatable(0f) }
    val pulseRingProgress = remember { Animatable(0f) }

    val pressRingStroke = remember(density) { Stroke(width = with(density) { 2.dp.toPx() }) }
    val pulseRingStroke = remember(density) { Stroke(width = with(density) { 1.5.dp.toPx() }) }
    val ringColor = colorScheme.primary

    LaunchedEffect(reduceMotion, isLightMode) {
        if (!reduceMotion && !isLightMode) {
            while (true) {
                delay(PULSE_INTERVAL_MS)
                pulseRingProgress.snapTo(0f)
                pulseRingProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
                )
            }
        } else {
            pulseRingProgress.snapTo(0f)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colorScheme.surface,
        border = BorderStroke(spacing.borderWidth, colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(spacing.bottomBarHeight)
                .padding(horizontal = spacing.sm)
                .drawBehind {
                    val maxIdx = (tabs.size - 1).toFloat()
                    val clamped = pillProgress.value.coerceIn(0f, maxIdx)
                    val lower = clamped.toInt().coerceIn(0, tabs.size - 2)
                    val upper = lower + 1
                    val fraction = clamped - lower
                    val centerX = tabCenters[lower] + (tabCenters[upper] - tabCenters[lower]) * fraction
                    val tabW = tabWidths[lower] + (tabWidths[upper] - tabWidths[lower]) * fraction
                    if (tabW > 0f) {
                        val pillW = (tabW - pillHorizontalInsetPx).coerceAtLeast(minPillWidthPx)
                        val pillH = (size.height - pillVerticalInsetPx).coerceAtLeast(minPillWidthPx)
                        val left = centerX - pillW / 2f
                        val top = (size.height - pillH) / 2f
                        val radius = pillH / 2f
                        drawRoundRect(
                            color = pillColor,
                            topLeft = Offset(left, top),
                            size = Size(pillW, pillH),
                            cornerRadius = CornerRadius(radius, radius)
                        )
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BottomNavItem(
                tab = PiecerTab.Home,
                selected = selectedIndex == 0,
                reduceMotion = reduceMotion,
                isLightMode = isLightMode,
                onClick = {
                    if (currentRoute != PiecerTab.Home.route) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onTabSelected(PiecerTab.Home)
                },
                modifier = Modifier
                    .weight(1f)
                    .onPlaced { coordinates ->
                        tabCenters[0] = coordinates.positionInParent().x + coordinates.size.width / 2f
                        tabWidths[0] = coordinates.size.width.toFloat()
                    }
            )
            BottomNavItem(
                tab = PiecerTab.Transactions,
                selected = selectedIndex == 1,
                reduceMotion = reduceMotion,
                isLightMode = isLightMode,
                onClick = {
                    if (currentRoute != PiecerTab.Transactions.route) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onTabSelected(PiecerTab.Transactions)
                },
                modifier = Modifier
                    .weight(1f)
                    .onPlaced { coordinates ->
                        tabCenters[1] = coordinates.positionInParent().x + coordinates.size.width / 2f
                        tabWidths[1] = coordinates.size.width.toFloat()
                    }
            )

            // Tombol (+) di tengah
            Box(
                modifier = Modifier
                    .size(spacing.fabSize)
                    .drawBehind {
                        val baseRadius = size.minDimension / 2f
                        val pulseP = pulseRingProgress.value
                        if (pulseP in 0.001f..0.999f) {
                            drawCircle(
                                color = ringColor,
                                radius = baseRadius * (1f + 0.42f * pulseP),
                                alpha = (1f - pulseP) * 0.25f,
                                style = pulseRingStroke
                            )
                        }
                        val pressP = pressRingProgress.value
                        if (pressP in 0.001f..0.999f) {
                            drawCircle(
                                color = ringColor,
                                radius = baseRadius * (1f + 0.55f * pressP),
                                alpha = (1f - pressP) * 0.45f,
                                style = pressRingStroke
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (!reduceMotion) {
                            scope.launch {
                                fabIconRotation.animateTo(
                                    targetValue = fabIconRotation.value + 90f,
                                    animationSpec = if (isLightMode) {
                                        tween(durationMillis = 140, easing = FastOutSlowInEasing)
                                    } else {
                                        spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)
                                    }
                                )
                            }
                            if (!isLightMode) {
                                scope.launch {
                                    pressRingProgress.snapTo(0f)
                                    pressRingProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                                    )
                                }
                            }
                        }
                        onAddClick()
                    },
                    modifier = Modifier
                        .size(spacing.fabSize)
                        .graphicsLayer {
                            scaleX = fabScale
                            scaleY = fabScale
                        },
                    shape = CircleShape,
                    color = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    interactionSource = fabInteractionSource
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(id = R.string.nav_add),
                            modifier = Modifier
                                .size(spacing.iconLg)
                                .graphicsLayer {
                                    rotationZ = fabIconRotation.value
                                }
                        )
                    }
                }
            }

            BottomNavItem(
                tab = PiecerTab.Statistics,
                selected = selectedIndex == 2,
                reduceMotion = reduceMotion,
                isLightMode = isLightMode,
                onClick = {
                    if (currentRoute != PiecerTab.Statistics.route) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onTabSelected(PiecerTab.Statistics)
                },
                modifier = Modifier
                    .weight(1f)
                    .onPlaced { coordinates ->
                        tabCenters[2] = coordinates.positionInParent().x + coordinates.size.width / 2f
                        tabWidths[2] = coordinates.size.width.toFloat()
                    }
            )
            BottomNavItem(
                tab = PiecerTab.More,
                selected = selectedIndex == 3,
                reduceMotion = reduceMotion,
                isLightMode = isLightMode,
                onClick = {
                    if (currentRoute != PiecerTab.More.route) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onTabSelected(PiecerTab.More)
                },
                modifier = Modifier
                    .weight(1f)
                    .onPlaced { coordinates ->
                        tabCenters[3] = coordinates.positionInParent().x + coordinates.size.width / 2f
                        tabWidths[3] = coordinates.size.width.toFloat()
                    }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: PiecerTab,
    selected: Boolean,
    reduceMotion: Boolean,
    isLightMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val contentColor = if (selected) colorScheme.onSurface else colorScheme.onSurfaceVariant

    val selectionAnim = remember { Animatable(if (selected) 1f else 0f) }
    LaunchedEffect(selected, reduceMotion, isLightMode) {
        val target = if (selected) 1f else 0f
        when {
            reduceMotion -> selectionAnim.snapTo(target)
            isLightMode -> selectionAnim.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
            )
            else -> selectionAnim.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val inactiveOffsetPx = with(density) { 5.dp.toPx() }
    val activeOffsetPx = with(density) { (-1).dp.toPx() }

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = stringResource(id = tab.labelRes),
            tint = contentColor,
            modifier = Modifier
                .size(spacing.iconLg)
                .graphicsLayer {
                    val t = selectionAnim.value
                    val s = 0.94f + (1.12f - 0.94f) * t
                    scaleX = s
                    scaleY = s
                    translationY = inactiveOffsetPx * (1f - t) + activeOffsetPx * t
                    alpha = (0.55f + 0.45f * t).coerceIn(0f, 1f)
                }
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = stringResource(id = tab.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.graphicsLayer {
                val t = selectionAnim.value.coerceIn(0f, 1f)
                alpha = t
                val s = 0.82f + 0.18f * t
                scaleX = s
                scaleY = s
            }
        )
    }
}
