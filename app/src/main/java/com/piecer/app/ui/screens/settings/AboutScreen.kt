package com.piecer.app.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.piecer.app.BuildConfig
import com.piecer.app.R
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.crash.CrashActivity
import com.piecer.app.ui.theme.Charcoal
import com.piecer.app.ui.theme.Graphite
import com.piecer.app.ui.theme.Ink
import com.piecer.app.ui.theme.LocalReduceMotion
import com.piecer.app.ui.theme.Mint
import com.piecer.app.ui.theme.Mist
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.ui.theme.White
import com.piecer.app.util.update.AppLinks
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

private val STATIC_FLOAT_STATE = object : State<Float> {
    override val value: Float = 0f
}

private class CachedPieceGeometry(
    val spec: AboutPieceSpec,
    val radiusPx: Float,
    val mainTopLeft: Offset,
    val mainSize: Size,
    val sliceTopLeft: Offset,
    val sliceSize: Size,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val reduceMotion = LocalReduceMotion.current
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val openErrorMessage = stringResource(id = R.string.about_link_open_error)

    val openUrl: (String) -> Unit = { url ->
        if (!openExternalUrl(context, url)) {
            scope.launch { snackbarHostState.showSnackbar(openErrorMessage) }
        }
    }

    var isLifecycleStarted by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            isLifecycleStarted = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pieceCountState = remember(reduceMotion) {
        mutableIntStateOf(resolveAboutPieceCount(reduceMotion = reduceMotion, lowFpsDetected = false))
    }
    var lowFpsEvaluated by remember(reduceMotion) { mutableStateOf(reduceMotion) }

    LaunchedEffect(reduceMotion, isLifecycleStarted) {
        if (reduceMotion || !isLifecycleStarted || lowFpsEvaluated) return@LaunchedEffect
        var prevFrameNanos = withFrameNanos { it }
        var elapsedMs = 0f
        var sampleCount = 0
        var sampleTotalNs = 0L

        while (elapsedMs <= ABOUT_FRAME_SAMPLE_WINDOW_MS) {
            val frameNanos = withFrameNanos { it }
            val rawDeltaNs = frameNanos - prevFrameNanos
            prevFrameNanos = frameNanos
            elapsedMs += clampAboutFrameDeltaMs(rawDeltaNs)
            if (rawDeltaNs > 0L) {
                sampleTotalNs += rawDeltaNs
                sampleCount++
            }
        }
        val lowFps = shouldUseReducedAboutPieces(sampleCount, sampleTotalNs)
        pieceCountState.intValue = resolveAboutPieceCount(
            reduceMotion = reduceMotion,
            lowFpsDetected = lowFps
        )
        lowFpsEvaluated = true
    }

    val entryProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            entryProgress.snapTo(1f)
        } else {
            entryProgress.snapTo(0f)
            entryProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = ABOUT_ENTRY_DURATION_MS, easing = LinearEasing)
            )
        }
    }

    val isLoopActive = !reduceMotion && isLifecycleStarted
    val loopProgressState: State<Float>
    val ringAngleState: State<Float>
    if (isLoopActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "aboutPiecesLoop")
        loopProgressState = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = ABOUT_LOOP_DURATION_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "piecesFloatProgress"
        )
        ringAngleState = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = ABOUT_RING_DURATION_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "profileRingAngle"
        )
    } else {
        loopProgressState = STATIC_FLOAT_STATE
        ringAngleState = STATIC_FLOAT_STATE
    }

    val devPhotoResId = remember(context) {
        runCatching {
            context.resources.getIdentifier("dev_photo", "drawable", context.packageName)
        }.getOrDefault(0)
    }

    val headerMinHeight = (LocalConfiguration.current.screenHeightDp * 0.40f).dp.coerceAtLeast(260.dp)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.about_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Ink,
                    titleContentColor = White,
                    navigationIconContentColor = White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            item {
                AboutHeaderSection(
                    minHeight = headerMinHeight,
                    pageBackgroundColor = colorScheme.background,
                    devPhotoResId = devPhotoResId,
                    loopProgressState = loopProgressState,
                    ringAngleState = ringAngleState,
                    pieceCountState = pieceCountState,
                    entryProgress = entryProgress.asState()
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = spacing.screenPadding)
                        .graphicsLayer {
                            alpha = computeStaggeredAlpha(entryProgress.value, 0.40f, 1.0f)
                        }
                ) {
                    PiecerCard(variant = PiecerCardVariant.Light) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            Text(
                                text = DevProfile.DEV_BIO,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(id = R.string.about_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(id = R.string.about_links_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = spacing.screenPadding)
                        .graphicsLayer {
                            alpha = computeStaggeredAlpha(entryProgress.value, 0.45f, 1.0f)
                        }
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = spacing.screenPadding)
                        .graphicsLayer {
                            alpha = computeStaggeredAlpha(entryProgress.value, 0.45f, 1.0f)
                        }
                ) {
                    PiecerCard(variant = PiecerCardVariant.Light) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            if (DevProfile.LINK_DEV.isNotBlank()) {
                                AboutLinkRow(
                                    title = stringResource(id = R.string.about_link_dev),
                                    subtitle = stringResource(id = R.string.about_link_dev_desc),
                                    onClick = { openUrl(DevProfile.LINK_DEV) }
                                )
                                HorizontalDivider(color = colorScheme.outlineVariant)
                            }
                            AboutLinkRow(
                                title = stringResource(id = R.string.about_link_source),
                                subtitle = stringResource(id = R.string.about_link_source_desc),
                                onClick = { openUrl(AppLinks.SOURCE_CODE_URL) }
                            )
                            if (AppLinks.TELEGRAM_CHANNEL_URL.isNotBlank()) {
                                HorizontalDivider(color = colorScheme.outlineVariant)
                                AboutLinkRow(
                                    title = stringResource(id = R.string.about_link_telegram),
                                    subtitle = stringResource(id = R.string.about_link_telegram_desc),
                                    onClick = { openUrl(AppLinks.TELEGRAM_CHANNEL_URL) }
                                )
                            }
                            if (AppLinks.DONATE_URL.isNotBlank()) {
                                HorizontalDivider(color = colorScheme.outlineVariant)
                                AboutLinkRow(
                                    title = stringResource(id = R.string.about_link_donate),
                                    subtitle = stringResource(id = R.string.about_link_donate_desc),
                                    onClick = { openUrl(AppLinks.DONATE_URL) }
                                )
                            }
                            HorizontalDivider(color = colorScheme.outlineVariant)
                            AboutLinkRow(
                                title = stringResource(id = R.string.about_last_crash_title),
                                subtitle = stringResource(id = R.string.about_last_crash_desc),
                                onClick = {
                                    context.startActivity(Intent(context, CrashActivity::class.java))
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(id = R.string.about_licenses_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = spacing.screenPadding)
                        .graphicsLayer {
                            alpha = computeStaggeredAlpha(entryProgress.value, 0.50f, 1.0f)
                        }
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = spacing.screenPadding)
                        .graphicsLayer {
                            alpha = computeStaggeredAlpha(entryProgress.value, 0.50f, 1.0f)
                        }
                ) {
                    PiecerCard(variant = PiecerCardVariant.Light) {
                        Text(
                            text = stringResource(id = R.string.about_licenses_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutHeaderSection(
    minHeight: androidx.compose.ui.unit.Dp,
    pageBackgroundColor: Color,
    devPhotoResId: Int,
    loopProgressState: State<Float>,
    ringAngleState: State<Float>,
    pieceCountState: State<Int>,
    entryProgress: State<Float>
) {
    val spacing = PiecerTheme.spacing
    val twoPi = (2.0 * PI).toFloat()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .background(Ink)
            .drawWithCache {
                val cachedPieces = ABOUT_PIECE_SPECS.map { spec ->
                    val r = spec.radiusDp.dp.toPx()
                    val sliceR = 0.94f * r
                    val baseColor = if (spec.isMintAccent) Mint else White
                    CachedPieceGeometry(
                        spec = spec,
                        radiusPx = r,
                        mainTopLeft = Offset(-r, -r),
                        mainSize = Size(2f * r, 2f * r),
                        sliceTopLeft = Offset(0.20f * r - sliceR, -0.20f * r - sliceR),
                        sliceSize = Size(2f * sliceR, 2f * sliceR),
                        color = baseColor.copy(alpha = spec.alpha)
                    )
                }
                val glowCenter = Offset(size.width * 0.5f, size.height * 0.34f)
                val glowRadius = size.minDimension * 0.55f
                val radialBrush = Brush.radialGradient(
                    colors = listOf(
                        White.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = glowCenter,
                    radius = glowRadius
                )
                val blendStartY = size.height * 0.72f
                val bottomBlendBrush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, pageBackgroundColor),
                    startY = blendStartY,
                    endY = size.height
                )
                val blendTopLeft = Offset(0f, blendStartY)
                val blendSize = Size(size.width, size.height - blendStartY)

                onDrawBehind {
                    drawCircle(
                        brush = radialBrush,
                        radius = glowRadius,
                        center = glowCenter
                    )

                    val progress = loopProgressState.value
                    val count = pieceCountState.value.coerceAtMost(cachedPieces.size)
                    for (i in 0 until count) {
                        val piece = cachedPieces[i]
                        val spec = piece.spec
                        val angleX = twoPi * (progress * spec.driftCyclesX + spec.phaseOffset)
                        val angleY = twoPi * (progress * spec.driftCyclesY + spec.phaseOffset)
                        val cx = size.width * (spec.anchorXFraction + spec.driftXFraction * sin(angleX))
                        val cy = size.height * (spec.anchorYFraction + spec.driftYFraction * cos(angleY))
                        val rotation = spec.baseRotationDeg + progress * 360f * spec.rotationTurns

                        translate(left = cx, top = cy) {
                            rotate(degrees = rotation, pivot = Offset.Zero) {
                                drawArc(
                                    color = piece.color,
                                    startAngle = 0f,
                                    sweepAngle = 270f,
                                    useCenter = true,
                                    topLeft = piece.mainTopLeft,
                                    size = piece.mainSize
                                )
                                drawArc(
                                    color = piece.color,
                                    startAngle = -90f,
                                    sweepAngle = 90f,
                                    useCenter = true,
                                    topLeft = piece.sliceTopLeft,
                                    size = piece.sliceSize
                                )
                            }
                        }
                    }

                    drawRect(
                        brush = bottomBlendBrush,
                        topLeft = blendTopLeft,
                        size = blendSize
                    )
                }
            }
            .padding(horizontal = spacing.screenPadding, vertical = spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            DevProfileAvatar(
                devPhotoResId = devPhotoResId,
                ringAngleState = ringAngleState
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            Text(
                text = DevProfile.DEV_NAME,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = White,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = computeStaggeredAlpha(entryProgress.value, 0.0f, 0.45f)
                }
            )

            Text(
                text = DevProfile.DEV_TAGLINE,
                style = MaterialTheme.typography.bodyMedium,
                color = Mist.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = computeStaggeredAlpha(entryProgress.value, 0.20f, 0.70f)
                }
            )

            Spacer(modifier = Modifier.height(2.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = Charcoal,
                border = BorderStroke(1.dp, Graphite),
                modifier = Modifier.graphicsLayer {
                    alpha = computeStaggeredAlpha(entryProgress.value, 0.20f, 0.70f)
                }
            ) {
                Text(
                    text = stringResource(id = R.string.about_version_chip_fmt, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelMedium,
                    color = White,
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun DevProfileAvatar(
    devPhotoResId: Int,
    ringAngleState: State<Float>
) {
    val avatarSize = 96.dp
    val ringContainerSize = 108.dp

    if (devPhotoResId != 0) {
        Box(
            modifier = Modifier
                .size(ringContainerSize)
                .drawWithCache {
                    val strokePx = 2.dp.toPx()
                    val inset = strokePx / 2f
                    val arcSize = Size(size.width - strokePx, size.height - strokePx)
                    val arcTopLeft = Offset(inset, inset)
                    val strokeStyle = Stroke(width = strokePx, cap = StrokeCap.Round)
                    val ringBrush = Brush.sweepGradient(
                        0.0f to White.copy(alpha = 0.85f),
                        0.55f to White.copy(alpha = 0.25f),
                        1.0f to Color.Transparent
                    )
                    onDrawBehind {
                        rotate(degrees = ringAngleState.value) {
                            drawArc(
                                brush = ringBrush,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = strokeStyle
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = devPhotoResId),
                contentDescription = stringResource(id = R.string.about_dev_photo_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .drawWithCache {
                    val bgRadius = size.minDimension / 2f
                    val borderStroke = Stroke(width = 1.dp.toPx())
                    val r = size.minDimension / 3.4f
                    val mainCenter = Offset(
                        x = size.width / 2f - 0.07f * r,
                        y = size.height / 2f + 0.07f * r
                    )
                    val mainTopLeft = Offset(mainCenter.x - r, mainCenter.y - r)
                    val mainSize = Size(2f * r, 2f * r)
                    val sliceR = 0.94f * r
                    val sliceCenter = Offset(
                        x = mainCenter.x + 0.20f * r,
                        y = mainCenter.y - 0.20f * r
                    )
                    val sliceTopLeft = Offset(sliceCenter.x - sliceR, sliceCenter.y - sliceR)
                    val sliceSize = Size(2f * sliceR, 2f * sliceR)

                    onDrawBehind {
                        drawCircle(color = Charcoal, radius = bgRadius)
                        drawCircle(color = Graphite, radius = bgRadius, style = borderStroke)
                        drawArc(
                            color = White,
                            startAngle = 0f,
                            sweepAngle = 270f,
                            useCenter = true,
                            topLeft = mainTopLeft,
                            size = mainSize
                        )
                        drawArc(
                            color = White,
                            startAngle = -90f,
                            sweepAngle = 90f,
                            useCenter = true,
                            topLeft = sliceTopLeft,
                            size = sliceSize
                        )
                    }
                }
        )
    }
}

@Composable
private fun AboutLinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        color = colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.sm, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openExternalUrl(context: Context, url: String): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: Exception) {
        false
    }
}
