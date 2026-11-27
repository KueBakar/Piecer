package com.piecer.app.ui.screens.splitbill

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.R
import com.piecer.app.ui.components.AmountText
import com.piecer.app.ui.components.PiecerButton
import com.piecer.app.ui.components.PiecerButtonVariant
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.components.PiecerChip
import com.piecer.app.ui.components.formatRupiah
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.SplitBillCalculator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillScreen(
    viewModel: SplitBillViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val result = uiState.result
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val shareTitle = stringResource(id = R.string.split_share_chooser)
    val noReceivableMsg = stringResource(id = R.string.split_no_receivable)

    Scaffold(
        modifier = modifier,
        containerColor = colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.split_title),
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
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = spacing.screenPadding,
                vertical = spacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            item {
                PiecerCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = PiecerCardVariant.Dark
                ) {
                    Text(
                        text = stringResource(id = R.string.split_grand_total),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    AmountText(
                        amount = result.grandTotal,
                        style = MaterialTheme.typography.displayLarge
                    )
                    if (result.taxAmount > 0L || result.serviceAmount > 0L) {
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = stringResource(
                                id = R.string.split_breakdown,
                                formatRupiah(result.subtotal),
                                formatRupiah(result.taxAmount),
                                formatRupiah(result.serviceAmount)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.mutedText
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    PiecerChip(
                        label = stringResource(id = R.string.split_mode_equal),
                        selected = uiState.mode == SplitMode.EQUAL,
                        onClick = { viewModel.onModeChange(SplitMode.EQUAL) }
                    )
                    PiecerChip(
                        label = stringResource(id = R.string.split_mode_custom),
                        selected = uiState.mode == SplitMode.CUSTOM,
                        onClick = { viewModel.onModeChange(SplitMode.CUSTOM) }
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = viewModel::onTitleChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(id = R.string.split_bill_title_label)) },
                        placeholder = { Text(text = stringResource(id = R.string.split_bill_title_placeholder)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )

                    if (uiState.mode == SplitMode.EQUAL) {
                        OutlinedTextField(
                            value = uiState.totalInput,
                            onValueChange = viewModel::onTotalChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = stringResource(id = R.string.split_total_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = MaterialTheme.shapes.medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        OutlinedTextField(
                            value = uiState.taxPercentInput,
                            onValueChange = viewModel::onTaxPercentChange,
                            modifier = Modifier.weight(1f),
                            label = { Text(text = stringResource(id = R.string.split_tax_percent)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = MaterialTheme.shapes.medium
                        )
                        OutlinedTextField(
                            value = uiState.servicePercentInput,
                            onValueChange = viewModel::onServicePercentChange,
                            modifier = Modifier.weight(1f),
                            label = { Text(text = stringResource(id = R.string.split_service_percent)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.split_participants_title, uiState.participants.size),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onBackground
                    )
                    TextButton(onClick = viewModel::addParticipant) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(spacing.xs))
                        Text(text = stringResource(id = R.string.split_add_person))
                    }
                }
            }

            items(uiState.participants, key = { it.id }) { row ->
                val shareAmount = result.shares
                    .getOrNull(uiState.participants.indexOfFirst { it.id == row.id })
                    ?.amount ?: 0L

                PiecerCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = PiecerCardVariant.Light
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        OutlinedTextField(
                            value = row.name,
                            onValueChange = { viewModel.updateParticipantName(row.id, it) },
                            modifier = Modifier.weight(1f),
                            label = { Text(text = stringResource(id = R.string.split_person_name)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                        if (uiState.mode == SplitMode.CUSTOM) {
                            OutlinedTextField(
                                value = row.amountInput,
                                onValueChange = { viewModel.updateParticipantAmount(row.id, it) },
                                modifier = Modifier.weight(1f),
                                label = { Text(text = stringResource(id = R.string.split_person_amount)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                        if (uiState.participants.size > 2) {
                            IconButton(onClick = { viewModel.removeParticipant(row.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(id = R.string.action_delete),
                                    tint = colors.mutedText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.xs))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = row.includeAsReceivable,
                                onCheckedChange = { viewModel.toggleParticipantReceivable(row.id) }
                            )
                            Text(
                                text = stringResource(id = R.string.split_include_receivable),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedText
                            )
                        }
                        AmountText(
                            amount = shareAmount,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    PiecerButton(
                        text = stringResource(id = R.string.split_share_btn),
                        onClick = {
                            val text = SplitBillCalculator.buildShareText(uiState.title, result)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, shareTitle))
                        },
                        variant = PiecerButtonVariant.Secondary,
                        enabled = result.grandTotal > 0L,
                        modifier = Modifier.weight(1f)
                    )
                    PiecerButton(
                        text = stringResource(id = R.string.split_create_debts_btn),
                        onClick = {
                            viewModel.createReceivables { count ->
                                scope.launch {
                                    val msg = if (count > 0) {
                                        context.getString(R.string.split_created_debts_snackbar, count)
                                    } else {
                                        noReceivableMsg
                                    }
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        },
                        enabled = result.grandTotal > 0L && !uiState.isCreatingDebts,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
