package com.piecer.app.ui.screens.recurring

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.R
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.RecurringInterval
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.ui.components.AmountText
import com.piecer.app.ui.components.AmountTone
import com.piecer.app.ui.components.EmptyState
import com.piecer.app.ui.components.PiecerButton
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.components.PiecerChip
import com.piecer.app.ui.components.formatRupiah
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    viewModel: RecurringViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.recurring_title),
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
                actions = {
                    IconButton(onClick = viewModel::openAddForm) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(id = R.string.recurring_add)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.onBackground,
                    navigationIconContentColor = colorScheme.onBackground,
                    actionIconContentColor = colorScheme.onBackground
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
            if (uiState.suggestions.isNotEmpty()) {
                items(uiState.suggestions) { suggestion ->
                    PiecerCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = PiecerCardVariant.Dark
                    ) {
                        Text(
                            text = stringResource(id = R.string.recurring_suggestion_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.amber
                        )
                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = "${suggestion.note} • ${formatRupiah(suggestion.amount)}/bln",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onCardDark
                        )
                        Spacer(modifier = Modifier.height(spacing.sm))
                        OutlinedButton(
                            onClick = { viewModel.acceptSuggestion(suggestion) }
                        ) {
                            Text(text = stringResource(id = R.string.recurring_suggestion_action))
                        }
                    }
                }
            }

            if (uiState.items.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        EmptyState(
                            icon = Icons.Rounded.Repeat,
                            title = stringResource(id = R.string.recurring_empty_title),
                            description = stringResource(id = R.string.recurring_empty_desc)
                        )
                        PiecerButton(
                            text = stringResource(id = R.string.recurring_add),
                            onClick = viewModel::openAddForm
                        )
                    }
                }
            } else {
                items(uiState.items, key = { it.entity.id }) { item ->
                    val r = item.entity
                    val intervalLabel = stringResource(
                        id = if (r.interval == RecurringInterval.MONTHLY) {
                            R.string.recurring_interval_monthly
                        } else {
                            R.string.recurring_interval_weekly
                        }
                    )
                    PiecerCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = PiecerCardVariant.Light
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.onCardLight
                                )
                                Text(
                                    text = "$intervalLabel • ${item.walletName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.mutedText
                                )
                            }
                            Switch(
                                checked = r.isActive,
                                onCheckedChange = { viewModel.toggleActive(r, it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.xs))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AmountText(
                                amount = r.amount,
                                tone = if (r.type == TransactionType.INCOME) AmountTone.Income else AmountTone.Expense,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Row {
                                IconButton(
                                    onClick = { viewModel.openEditForm(r) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = stringResource(id = R.string.recurring_edit),
                                        tint = colors.mutedText,
                                        modifier = Modifier.size(spacing.iconMd)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.requestDelete(r) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = stringResource(id = R.string.action_delete),
                                        tint = colors.mutedText,
                                        modifier = Modifier.size(spacing.iconMd)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(spacing.xs))
                        Text(
                            text = stringResource(id = R.string.recurring_next_due_label, item.nextDueText),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText
                        )
                    }
                }
            }
        }
    }

    if (uiState.form.isVisible) {
        val invalidAmountErr = stringResource(id = R.string.tx_error_amount)
        val noWalletErr = stringResource(id = R.string.tx_error_wallet)
        RecurringFormDialog(
            state = uiState.form,
            expenseCategories = uiState.expenseCategories,
            incomeCategories = uiState.incomeCategories,
            wallets = uiState.wallets,
            onTypeSelect = viewModel::onTypeSelect,
            onAmountChange = viewModel::onAmountChange,
            onCategorySelect = viewModel::onCategorySelect,
            onWalletSelect = viewModel::onWalletSelect,
            onNoteChange = viewModel::onNoteChange,
            onIntervalSelect = viewModel::onIntervalSelect,
            onNextDueDateSelect = viewModel::onNextDueDateSelect,
            onRemindDaysSelect = viewModel::onRemindDaysSelect,
            onDismiss = viewModel::closeForm,
            onSave = { viewModel.saveForm(invalidAmountErr, noWalletErr) }
        )
    }

    uiState.pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text(text = stringResource(id = R.string.recurring_delete_title)) },
            text = { Text(text = stringResource(id = R.string.recurring_delete_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text(text = stringResource(id = R.string.action_delete), color = colors.coral)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringFormDialog(
    state: RecurringFormState,
    expenseCategories: List<CategoryEntity>,
    incomeCategories: List<CategoryEntity>,
    wallets: List<WalletEntity>,
    onTypeSelect: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategorySelect: (Long) -> Unit,
    onWalletSelect: (Long) -> Unit,
    onNoteChange: (String) -> Unit,
    onIntervalSelect: (RecurringInterval) -> Unit,
    onNextDueDateSelect: (Long) -> Unit,
    onRemindDaysSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    var showDatePicker by remember { mutableStateOf(false) }
    val categories = if (state.type == TransactionType.INCOME) incomeCategories else expenseCategories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    id = if (state.editingItem != null) R.string.recurring_edit else R.string.recurring_add
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    PiecerChip(
                        label = stringResource(id = R.string.tx_type_expense),
                        selected = state.type == TransactionType.EXPENSE,
                        onClick = { onTypeSelect(TransactionType.EXPENSE) }
                    )
                    PiecerChip(
                        label = stringResource(id = R.string.tx_type_income),
                        selected = state.type == TransactionType.INCOME,
                        onClick = { onTypeSelect(TransactionType.INCOME) }
                    )
                }

                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = onAmountChange,
                    label = { Text(text = stringResource(id = R.string.recurring_amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    label = { Text(text = stringResource(id = R.string.recurring_note_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(id = R.string.tx_label_category),
                    style = MaterialTheme.typography.labelMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    items(categories, key = { it.id }) { cat ->
                        PiecerChip(
                            label = cat.name,
                            selected = state.categoryId == cat.id,
                            onClick = { onCategorySelect(cat.id) }
                        )
                    }
                }

                Text(
                    text = stringResource(id = R.string.tx_label_wallet),
                    style = MaterialTheme.typography.labelMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    items(wallets, key = { it.id }) { w ->
                        PiecerChip(
                            label = w.name,
                            selected = state.walletId == w.id,
                            onClick = { onWalletSelect(w.id) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    PiecerChip(
                        label = stringResource(id = R.string.recurring_interval_monthly),
                        selected = state.interval == RecurringInterval.MONTHLY,
                        onClick = { onIntervalSelect(RecurringInterval.MONTHLY) }
                    )
                    PiecerChip(
                        label = stringResource(id = R.string.recurring_interval_weekly),
                        selected = state.interval == RecurringInterval.WEEKLY,
                        onClick = { onIntervalSelect(RecurringInterval.WEEKLY) }
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(spacing.borderWidth, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(spacing.iconMd)
                        )
                        Spacer(modifier = Modifier.width(spacing.sm))
                        Text(
                            text = stringResource(
                                id = R.string.recurring_next_due_label,
                                DateUtils.formatDate(state.nextDueDateMillis)
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Text(
                    text = stringResource(id = R.string.recurring_remind_label),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    listOf(0, 1, 3, 7).forEach { days ->
                        PiecerChip(
                            label = if (days == 0) {
                                stringResource(id = R.string.recurring_remind_off)
                            } else {
                                "H-$days"
                            },
                            selected = state.remindDaysBefore == days,
                            onClick = { onRemindDaysSelect(days) }
                        )
                    }
                }

                state.errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            PiecerButton(
                text = stringResource(id = R.string.action_save),
                onClick = onSave
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.nextDueDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let(onNextDueDateSelect)
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
            DatePicker(state = pickerState)
        }
    }
}
