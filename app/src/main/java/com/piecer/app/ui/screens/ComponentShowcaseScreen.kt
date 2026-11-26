package com.piecer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.piecer.app.R
import com.piecer.app.ui.components.AmountText
import com.piecer.app.ui.components.AmountTone
import com.piecer.app.ui.components.EmptyState
import com.piecer.app.ui.components.PiecerButton
import com.piecer.app.ui.components.PiecerButtonVariant
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.components.PiecerChip
import com.piecer.app.ui.components.PiecerIconButton
import com.piecer.app.ui.components.ProgressBarPiece
import com.piecer.app.ui.theme.PiecerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentShowcaseScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.showcase_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.showcase_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.xl)
        ) {
            PiecerTheme(darkTheme = false) {
                ShowcaseSection(title = stringResource(id = R.string.showcase_light_mode))
            }

            PiecerTheme(darkTheme = true) {
                ShowcaseSection(title = stringResource(id = R.string.showcase_dark_mode))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShowcaseSection(title: String) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme

    var chipActive by remember { mutableStateOf(true) }
    var isAmountHidden by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.background)
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = colorScheme.onBackground
            )

            // 1. PiecerCard
            SectionLabel(text = stringResource(id = R.string.showcase_cards))
            PiecerCard(
                variant = PiecerCardVariant.Light,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.showcase_card_light_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = stringResource(id = R.string.showcase_card_light_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PiecerTheme.colors.mutedText
                )
            }
            PiecerCard(
                variant = PiecerCardVariant.Dark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.showcase_card_dark_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = stringResource(id = R.string.showcase_card_dark_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PiecerTheme.colors.mutedText
                )
            }

            // 2. PiecerButton & PiecerIconButton
            SectionLabel(text = stringResource(id = R.string.showcase_buttons))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                PiecerButton(
                    text = stringResource(id = R.string.showcase_btn_primary),
                    onClick = {},
                    variant = PiecerButtonVariant.Primary
                )
                PiecerButton(
                    text = stringResource(id = R.string.showcase_btn_secondary),
                    onClick = {},
                    variant = PiecerButtonVariant.Secondary
                )
                PiecerIconButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = stringResource(id = R.string.nav_add),
                    onClick = {},
                    variant = PiecerButtonVariant.Primary
                )
                PiecerIconButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = stringResource(id = R.string.nav_add),
                    onClick = {},
                    variant = PiecerButtonVariant.Secondary
                )
            }

            // 3. PiecerChip
            SectionLabel(text = stringResource(id = R.string.showcase_chips))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                PiecerChip(
                    label = stringResource(id = R.string.showcase_chip_active),
                    selected = chipActive,
                    onClick = { chipActive = true }
                )
                PiecerChip(
                    label = stringResource(id = R.string.showcase_chip_inactive),
                    selected = !chipActive,
                    onClick = { chipActive = false }
                )
            }

            // 4. AmountText
            SectionLabel(text = stringResource(id = R.string.showcase_amounts))
            PiecerCard(
                variant = PiecerCardVariant.Dark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AmountText(
                        amount = 1_250_000L,
                        isHidden = isAmountHidden
                    )
                    PiecerIconButton(
                        icon = if (isAmountHidden) {
                            Icons.Rounded.VisibilityOff
                        } else {
                            Icons.Rounded.Visibility
                        },
                        contentDescription = stringResource(
                            id = if (isAmountHidden) {
                                R.string.action_show_amount
                            } else {
                                R.string.action_hide_amount
                            }
                        ),
                        onClick = { isAmountHidden = !isAmountHidden },
                        variant = PiecerButtonVariant.Secondary
                    )
                }
                Spacer(modifier = Modifier.height(spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                    AmountText(
                        amount = 500_000L,
                        isHidden = isAmountHidden,
                        tone = AmountTone.Income,
                        style = MaterialTheme.typography.titleMedium
                    )
                    AmountText(
                        amount = 250_000L,
                        isHidden = isAmountHidden,
                        tone = AmountTone.Expense,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // 5. ProgressBarPiece
            SectionLabel(text = stringResource(id = R.string.showcase_progress))
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    text = stringResource(id = R.string.showcase_progress_mint),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onBackground
                )
                ProgressBarPiece(progress = 0.45f)

                Text(
                    text = stringResource(id = R.string.showcase_progress_amber),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onBackground
                )
                ProgressBarPiece(progress = 0.80f)

                Text(
                    text = stringResource(id = R.string.showcase_progress_coral),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onBackground
                )
                ProgressBarPiece(progress = 1.0f)
            }

            // 6. EmptyState
            SectionLabel(text = stringResource(id = R.string.showcase_empty))
            PiecerCard(
                variant = PiecerCardVariant.Light,
                modifier = Modifier.fillMaxWidth()
            ) {
                EmptyState(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = stringResource(id = R.string.showcase_empty_title),
                    description = stringResource(id = R.string.showcase_empty_desc)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = PiecerTheme.colors.mutedText
    )
}
