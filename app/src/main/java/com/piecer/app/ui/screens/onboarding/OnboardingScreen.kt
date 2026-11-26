package com.piecer.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.piecer.app.R
import com.piecer.app.ui.components.PiecerButton
import com.piecer.app.ui.theme.PiecerTheme
import kotlinx.coroutines.launch

private data class OnboardingSlide(
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int
)

private val ONBOARDING_SLIDES = listOf(
    OnboardingSlide(
        icon = Icons.Rounded.PieChart,
        titleRes = R.string.onboarding_slide1_title,
        descRes = R.string.onboarding_slide1_desc
    ),
    OnboardingSlide(
        icon = Icons.Rounded.Bolt,
        titleRes = R.string.onboarding_slide2_title,
        descRes = R.string.onboarding_slide2_desc
    ),
    OnboardingSlide(
        icon = Icons.Rounded.Shield,
        titleRes = R.string.onboarding_slide3_title,
        descRes = R.string.onboarding_slide3_desc
    )
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_SLIDES.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == ONBOARDING_SLIDES.lastIndex

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screenPadding, vertical = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isLastPage) {
                    TextButton(onClick = onFinish) {
                        Text(
                            text = stringResource(id = R.string.onboarding_skip),
                            color = colors.mutedText
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val slide = ONBOARDING_SLIDES[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = slide.icon,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.xl))
                    Text(
                        text = stringResource(id = slide.titleRes),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(spacing.md))
                    Text(
                        text = stringResource(id = slide.descRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.mutedText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(ONBOARDING_SLIDES.size) { idx ->
                    val selected = idx == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .size(width = if (selected) 24.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) colorScheme.primary else colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            PiecerButton(
                text = stringResource(
                    id = if (isLastPage) R.string.onboarding_start_default_wallet
                    else R.string.onboarding_next
                ),
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
