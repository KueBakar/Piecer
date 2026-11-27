package com.piecer.app.ui.screens.addtransaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.R
import com.piecer.app.data.local.entity.RecurringInterval
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.ui.components.AmountText
import com.piecer.app.ui.components.AmountTone
import com.piecer.app.ui.components.PiecerButton
import com.piecer.app.ui.components.PiecerChip
import com.piecer.app.ui.components.formatRupiah
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.DateUtils
import com.piecer.app.util.TransactionValidationError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    focusQuickEntry: Boolean = false,
    onQuickEntryFocusHandled: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    var showDatePicker by remember { mutableStateOf(false) }
    val quickEntryFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(focusQuickEntry) {
        if (focusQuickEntry && uiState.editingId == null) {
            quickEntryFocusRequester.requestFocus()
            onQuickEntryFocusHandled()
        }
    }

    val amountTone = when (uiState.type) {
        TransactionType.EXPENSE -> AmountTone.Expense
        TransactionType.INCOME -> AmountTone.Income
        TransactionType.TRANSFER -> AmountTone.Neutral
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            id = if (uiState.editingId != null) {
                                R.string.tx_edit_title
                            } else {
                                R.string.tx_add_title
                            }
                        ),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(id = R.string.tx_close)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.onBackground,
                    navigationIconContentColor = colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenPadding)
            ) {
                if (uiState.editingId == null) {
                    OutlinedTextField(
                        value = uiState.quickEntryText,
                        onValueChange = viewModel::onQuickEntryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(quickEntryFocusRequester),
                        label = { Text(text = stringResource(id = R.string.tx_quick_entry_label)) },
                        placeholder = { Text(text = stringResource(id = R.string.tx_quick_entry_placeholder)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outline,
                            focusedContainerColor = colorScheme.surface,
                            unfocusedContainerColor = colorScheme.surface
                        )
                    )
                    Spacer(modifier = Modifier.height(spacing.md))
                }

                TransactionTypeSelector(
                    selectedType = uiState.type,
                    onTypeSelected = viewModel::onTypeChange
                )

                Spacer(modifier = Modifier.height(spacing.lg))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    AmountText(
                        amount = uiState.amount,
                        tone = amountTone,
                        style = MaterialTheme.typography.displayLarge
                    )
                }

                uiState.error?.let { error ->
                    Text(
                        text = errorMessageFor(error),
                        color = colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.sm)
                    )
                }

                Spacer(modifier = Modifier.height(spacing.sm))

                if (uiState.type != TransactionType.TRANSFER) {
                    SectionLabel(text = stringResource(id = R.string.tx_label_category))
                    Spacer(modifier = Modifier.height(spacing.xs))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        contentPadding = PaddingValues(vertical = spacing.xs)
                    ) {
                        items(uiState.activeCategories, key = { it.id }) { category ->
                            PiecerChip(
                                label = category.name,
                                selected = category.id == uiState.selectedCategoryId,
                                onClick = { viewModel.onCategorySelect(category.id) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.md))
                }

                if (uiState.type == TransactionType.TRANSFER) {
                    SectionLabel(text = stringResource(id = R.string.tx_label_wallet_from))
                    Spacer(modifier = Modifier.height(spacing.xs))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        contentPadding = PaddingValues(vertical = spacing.xs)
                    ) {
                        items(uiState.wallets, key = { it.id }) { wallet ->
                            PiecerChip(
                                label = wallet.name,
                                selected = wallet.id == uiState.selectedWalletId,
                                onClick = { viewModel.onWalletSelect(wallet.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.md))

                    SectionLabel(text = stringResource(id = R.string.tx_label_wallet_to))
                    Spacer(modifier = Modifier.height(spacing.xs))
                    if (uiState.wallets.size < 2) {
                        Text(
                            text = stringResource(id = R.string.tx_no_other_wallet),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            contentPadding = PaddingValues(vertical = spacing.xs)
                        ) {
                            items(uiState.wallets, key = { it.id }) { wallet ->
                                PiecerChip(
                                    label = wallet.name,
                                    selected = wallet.id == uiState.selectedToWalletId,
                                    onClick = { viewModel.onToWalletSelect(wallet.id) }
                                )
                            }
                        }
                    }
                } else {
                    SectionLabel(text = stringResource(id = R.string.tx_label_wallet))
                    Spacer(modifier = Modifier.height(spacing.xs))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        contentPadding = PaddingValues(vertical = spacing.xs)
                    ) {
                        items(uiState.wallets, key = { it.id }) { wallet ->
                            PiecerChip(
                                label = wallet.name,
                                selected = wallet.id == uiState.selectedWalletId,
                                onClick = { viewModel.onWalletSelect(wallet.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.md))

                SectionLabel(text = stringResource(id = R.string.tx_label_date))
                Spacer(modifier = Modifier.height(spacing.xs))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = MaterialTheme.shapes.medium,
                    color = colorScheme.surface,
                    border = BorderStroke(spacing.borderWidth, colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = spacing.lg,
                            vertical = spacing.md
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(spacing.iconMd)
                        )
                        Spacer(modifier = Modifier.width(spacing.sm))
                        Text(
                            text = DateUtils.formatDate(uiState.dateMillis),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.md))

                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = viewModel::onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(id = R.string.tx_label_note)) },
                    placeholder = { Text(text = stringResource(id = R.string.tx_note_placeholder)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surface
                    )
                )

                if (uiState.type == TransactionType.INCOME && uiState.pieces.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(spacing.md))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(quickEntryFocusRequester),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.tx_split_to_pieces),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.onSurface
                        )
                        Switch(
                            checked = uiState.splitToPieces,
                            onCheckedChange = viewModel::onSplitToPiecesChange
                        )
                    }
                    if (uiState.splitToPieces && uiState.pieceSplitPreview.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = spacing.xs),
                            shape = MaterialTheme.shapes.medium,
                            color = colorScheme.surface,
                            border = BorderStroke(spacing.borderWidth, colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(spacing.md),
                                verticalArrangement = Arrangement.spacedBy(spacing.xxs)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.tx_split_preview_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PiecerTheme.colors.mutedText
                                )
                                uiState.pieceSplitPreview.forEach { item ->
                                    Row(
                                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(quickEntryFocusRequester),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${item.pieceName} (${item.percent}%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = formatRupiah(item.amount),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PiecerTheme.colors.mint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.editingId == null) {
                    Spacer(modifier = Modifier.height(spacing.md))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(quickEntryFocusRequester),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.tx_make_recurring),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.onSurface
                        )
                        Switch(
                            checked = uiState.makeRecurring,
                            onCheckedChange = viewModel::onMakeRecurringChange
                        )
                    }
                    if (uiState.makeRecurring) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            modifier = Modifier.padding(top = spacing.xs)
                        ) {
                            PiecerChip(
                                label = stringResource(id = R.string.recurring_interval_monthly),
                                selected = uiState.recurringInterval == RecurringInterval.MONTHLY,
                                onClick = { viewModel.onRecurringIntervalSelect(RecurringInterval.MONTHLY) }
                            )
                            PiecerChip(
                                label = stringResource(id = R.string.recurring_interval_weekly),
                                selected = uiState.recurringInterval == RecurringInterval.WEEKLY,
                                onClick = { viewModel.onRecurringIntervalSelect(RecurringInterval.WEEKLY) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.md))
            }

            CustomAmountKeypad(
                onDigitClick = viewModel::onDigitClick,
                onTripleZeroClick = viewModel::onTripleZeroClick,
                onBackspaceClick = viewModel::onBackspaceClick,
                modifier = Modifier.padding(horizontal = spacing.screenPadding)
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            PiecerButton(
                text = stringResource(id = R.string.tx_save),
                onClick = { viewModel.save(onSaved = onSaved) },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenPadding)
                    .padding(bottom = spacing.md)
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.dateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selected ->
                            viewModel.onDateSelect(selected)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.tx_date_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(id = R.string.tx_date_picker_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    val spacing = PiecerTheme.spacing
    val options = listOf(
        TransactionType.EXPENSE to R.string.tx_type_expense,
        TransactionType.INCOME to R.string.tx_type_income,
        TransactionType.TRANSFER to R.string.tx_type_transfer
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        options.forEach { (type, labelRes) ->
            val selected = type == selectedType
            Surface(
                onClick = { onTypeSelected(type) },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = if (selected) null else BorderStroke(spacing.borderWidth, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = stringResource(id = labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = spacing.sm)
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
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CustomAmountKeypad(
    onDigitClick: (Int) -> Unit,
    onTripleZeroClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                row.forEach { digit ->
                    KeypadButton(
                        label = digit,
                        onClick = { onDigitClick(digit.toInt()) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            KeypadButton(
                label = stringResource(id = R.string.tx_triple_zero),
                onClick = onTripleZeroClick,
                modifier = Modifier.weight(1f)
            )
            KeypadButton(
                label = "0",
                onClick = { onDigitClick(0) },
                modifier = Modifier.weight(1f)
            )
            KeypadIconButton(
                onClick = onBackspaceClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.height(spacing.buttonHeight),
        shape = MaterialTheme.shapes.medium,
        color = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        border = BorderStroke(spacing.borderWidth, colorScheme.outlineVariant)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun KeypadIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.height(spacing.buttonHeight),
        shape = MaterialTheme.shapes.medium,
        color = colorScheme.surfaceVariant,
        contentColor = colorScheme.onSurface
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = stringResource(id = R.string.tx_backspace),
                modifier = Modifier.size(spacing.iconMd)
            )
        }
    }
}

@Composable
private fun errorMessageFor(error: TransactionValidationError): String {
    return when (error) {
        TransactionValidationError.AMOUNT_REQUIRED -> stringResource(id = R.string.tx_error_amount)
        TransactionValidationError.CATEGORY_REQUIRED -> stringResource(id = R.string.tx_error_category)
        TransactionValidationError.WALLET_REQUIRED -> stringResource(id = R.string.tx_error_wallet)
        TransactionValidationError.TRANSFER_DEST_REQUIRED -> stringResource(id = R.string.tx_error_transfer_dest)
        TransactionValidationError.TRANSFER_SAME_WALLET -> stringResource(id = R.string.tx_error_transfer_same)
    }
}
