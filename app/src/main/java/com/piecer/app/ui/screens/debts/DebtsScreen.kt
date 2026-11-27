package com.piecer.app.ui.screens.debts

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.R
import com.piecer.app.data.local.entity.DebtDirection
import com.piecer.app.data.local.entity.WalletEntity
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
import com.piecer.app.ui.components.formatRupiah
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.DateUtils
import com.piecer.app.util.DebtCalculator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    viewModel: DebtsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val savedMsg = stringResource(id = R.string.debts_saved_snackbar)
    val paidMsg = stringResource(id = R.string.debts_paid_snackbar)
    val deletedMsg = stringResource(id = R.string.debts_deleted_snackbar)
    val shareChooserTitle = stringResource(id = R.string.debts_share_title)

    Scaffold(
        modifier = modifier,
        containerColor = colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.debts_title),
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
                    PiecerIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = stringResource(id = R.string.debts_add),
                        onClick = viewModel::openAddForm,
                        variant = PiecerButtonVariant.Primary,
                        modifier = Modifier.padding(end = spacing.sm)
                    )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.debts_total_owe_me),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedText
                            )
                            Spacer(modifier = Modifier.height(spacing.xs))
                            AmountText(
                                amount = uiState.totalOweMe,
                                isHidden = uiState.isBalanceHidden,
                                tone = AmountTone.Income,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Spacer(modifier = Modifier.width(spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.debts_total_i_owe),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedText
                            )
                            Spacer(modifier = Modifier.height(spacing.xs))
                            AmountText(
                                amount = uiState.totalIOwe,
                                isHidden = uiState.isBalanceHidden,
                                tone = AmountTone.Expense,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    item {
                        PiecerChip(
                            label = stringResource(id = R.string.debts_filter_all),
                            selected = uiState.filter == DebtFilter.ALL,
                            onClick = { viewModel.setFilter(DebtFilter.ALL) }
                        )
                    }
                    item {
                        PiecerChip(
                            label = stringResource(id = R.string.debts_type_owe_me),
                            selected = uiState.filter == DebtFilter.OWE_ME,
                            onClick = { viewModel.setFilter(DebtFilter.OWE_ME) }
                        )
                    }
                    item {
                        PiecerChip(
                            label = stringResource(id = R.string.debts_type_i_owe),
                            selected = uiState.filter == DebtFilter.I_OWE,
                            onClick = { viewModel.setFilter(DebtFilter.I_OWE) }
                        )
                    }
                }
            }

            if (uiState.items.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Handshake,
                        title = stringResource(id = R.string.debts_empty_title),
                        description = stringResource(id = R.string.debts_empty_desc)
                    )
                }
            } else {
                items(uiState.items, key = { it.debt.id }) { item ->
                    DebtItemCard(
                        item = item,
                        isBalanceHidden = uiState.isBalanceHidden,
                        onPayClick = { viewModel.openPaymentForm(item.debt) },
                        onMarkPaidClick = {
                            viewModel.markPaidOff(item.debt) {
                                scope.launch { snackbarHostState.showSnackbar(paidMsg) }
                            }
                        },
                        onShareClick = {
                            val dueText = item.debt.dueDate?.let { DateUtils.formatDate(it) }
                            val text = DebtCalculator.buildPoliteReminderText(item.debt, dueText)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, shareChooserTitle))
                        },
                        onEditClick = { viewModel.openEditForm(item.debt) },
                        onDeleteClick = { viewModel.requestDelete(item.debt) }
                    )
                }
            }
        }
    }

    if (uiState.form.isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissForm,
            sheetState = sheetState,
            containerColor = colorScheme.surface
        ) {
            DebtFormSheet(
                form = uiState.form,
                wallets = uiState.wallets,
                onPersonNameChange = viewModel::onPersonNameChange,
                onAmountChange = viewModel::onAmountChange,
                onDirectionSelect = viewModel::onDirectionSelect,
                onDueDateSelect = viewModel::onDueDateSelect,
                onNoteChange = viewModel::onNoteChange,
                onWalletSelect = viewModel::onWalletSelect,
                onRecordAsTransactionChange = viewModel::onRecordAsTransactionChange,
                onSave = {
                    viewModel.saveDebt {
                        scope.launch { snackbarHostState.showSnackbar(savedMsg) }
                    }
                },
                onDismiss = viewModel::dismissForm
            )
        }
    }

    uiState.paymentForm.debt?.let { targetDebt ->
        DebtPaymentDialog(
            form = uiState.paymentForm,
            debtPersonName = targetDebt.personName,
            wallets = uiState.wallets,
            onAmountChange = viewModel::onPaymentAmountChange,
            onWalletSelect = viewModel::onPaymentWalletSelect,
            onNoteChange = viewModel::onPaymentNoteChange,
            onRecordAsTransactionChange = viewModel::onPaymentRecordAsTransactionChange,
            onConfirm = {
                viewModel.submitPayment {
                    scope.launch { snackbarHostState.showSnackbar(paidMsg) }
                }
            },
            onDismiss = viewModel::dismissPaymentForm
        )
    }

    uiState.pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text(text = stringResource(id = R.string.debts_delete_title)) },
            text = { Text(text = stringResource(id = R.string.debts_delete_confirm, target.personName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDelete {
                            scope.launch { snackbarHostState.showSnackbar(deletedMsg) }
                        }
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.action_delete),
                        color = colors.coral
                    )
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

@Composable
private fun DebtItemCard(
    item: DebtListItem,
    isBalanceHidden: Boolean,
    onPayClick: () -> Unit,
    onMarkPaidClick: () -> Unit,
    onShareClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val debt = item.debt
    val tone = if (debt.direction == DebtDirection.OWE_ME) AmountTone.Income else AmountTone.Expense

    PiecerCard(
        modifier = Modifier.fillMaxWidth(),
        variant = PiecerCardVariant.Light,
        onClick = onEditClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(
                        text = debt.personName,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onCardLight
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (debt.direction == DebtDirection.OWE_ME) {
                            colors.mint.copy(alpha = 0.16f)
                        } else {
                            colors.coral.copy(alpha = 0.16f)
                        }
                    ) {
                        Text(
                            text = stringResource(
                                id = if (debt.direction == DebtDirection.OWE_ME) {
                                    R.string.debts_type_owe_me
                                } else {
                                    R.string.debts_type_i_owe
                                }
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (debt.direction == DebtDirection.OWE_ME) colors.mint else colors.coral,
                            modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.xxs)
                        )
                    }
                    if (item.isOverdue) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = colors.coral.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = stringResource(id = R.string.debts_overdue_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.coral,
                                modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.xxs)
                            )
                        }
                    }
                }
                if (debt.note.isNotBlank()) {
                    Text(
                        text = debt.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                }
                debt.dueDate?.let { due ->
                    Text(
                        text = stringResource(id = R.string.debts_due_label, DateUtils.formatDate(due)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isOverdue) colors.coral else colors.mutedText
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                AmountText(
                    amount = item.remaining,
                    isHidden = isBalanceHidden,
                    tone = tone,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isBalanceHidden) {
                        stringResource(id = R.string.amount_hidden)
                    } else {
                        stringResource(id = R.string.debts_of_total, formatRupiah(debt.totalAmount))
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.sm))
        ProgressBarPiece(progress = item.progress, isGoal = true)
        Spacer(modifier = Modifier.height(spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!item.isPaidOff) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    TextButton(onClick = onPayClick) {
                        Text(text = stringResource(id = R.string.debts_add_payment))
                    }
                    TextButton(onClick = onMarkPaidClick) {
                        Text(
                            text = stringResource(id = R.string.debts_mark_paid),
                            color = colors.mint
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = colors.mint,
                        modifier = Modifier.size(spacing.iconSm)
                    )
                    Text(
                        text = stringResource(id = R.string.debts_paid_off_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.mint
                    )
                }
            }

            Row {
                if (!item.isPaidOff) {
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(id = R.string.debts_share),
                            tint = colors.mutedText,
                            modifier = Modifier.size(spacing.iconMd)
                        )
                    }
                }
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(id = R.string.action_edit),
                        tint = colors.mutedText,
                        modifier = Modifier.size(spacing.iconMd)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(id = R.string.action_delete),
                        tint = colors.coral,
                        modifier = Modifier.size(spacing.iconMd)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtFormSheet(
    form: DebtFormState,
    wallets: List<WalletEntity>,
    onPersonNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDirectionSelect: (DebtDirection) -> Unit,
    onDueDateSelect: (Long?) -> Unit,
    onNoteChange: (String) -> Unit,
    onWalletSelect: (Long?) -> Unit,
    onRecordAsTransactionChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenPadding)
            .padding(bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Text(
            text = stringResource(
                id = if (form.editingId == null) R.string.debts_add else R.string.debts_edit
            ),
            style = MaterialTheme.typography.titleLarge,
            color = colorScheme.onSurface
        )

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            PiecerChip(
                label = stringResource(id = R.string.debts_type_owe_me),
                selected = form.direction == DebtDirection.OWE_ME,
                onClick = { onDirectionSelect(DebtDirection.OWE_ME) }
            )
            PiecerChip(
                label = stringResource(id = R.string.debts_type_i_owe),
                selected = form.direction == DebtDirection.I_OWE,
                onClick = { onDirectionSelect(DebtDirection.I_OWE) }
            )
        }

        OutlinedTextField(
            value = form.personName,
            onValueChange = onPersonNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.debts_person_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            isError = form.error == DebtFormError.EMPTY_NAME
        )

        OutlinedTextField(
            value = form.amountInput,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.debts_amount_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium,
            isError = form.error == DebtFormError.INVALID_AMOUNT
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = form.dueDate?.let {
                        stringResource(id = R.string.debts_due_label, DateUtils.formatDate(it))
                    } ?: stringResource(id = R.string.debts_due_optional)
                )
            }
            if (form.dueDate != null) {
                TextButton(onClick = { onDueDateSelect(null) }) {
                    Text(text = stringResource(id = R.string.debts_clear_due))
                }
            }
        }

        OutlinedTextField(
            value = form.note,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.tx_note_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        if (wallets.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                items(wallets, key = { it.id }) { wallet ->
                    PiecerChip(
                        label = wallet.name,
                        selected = form.walletId == wallet.id,
                        onClick = { onWalletSelect(wallet.id) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.debts_record_tx_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = R.string.debts_record_tx_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
            }
            Switch(
                checked = form.recordAsTransaction,
                onCheckedChange = onRecordAsTransactionChange
            )
        }

        form.error?.let { err ->
            val msgRes = when (err) {
                DebtFormError.EMPTY_NAME -> R.string.debts_error_name
                DebtFormError.INVALID_AMOUNT -> R.string.debts_error_amount
            }
            Text(
                text = stringResource(id = msgRes),
                style = MaterialTheme.typography.bodySmall,
                color = colors.coral
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            PiecerButton(
                text = stringResource(id = R.string.action_cancel),
                onClick = onDismiss,
                variant = PiecerButtonVariant.Secondary,
                modifier = Modifier.weight(1f)
            )
            PiecerButton(
                text = stringResource(id = R.string.action_save),
                onClick = onSave,
                enabled = !form.isSaving,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.dueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDueDateSelect(it) }
                        showDatePicker = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DebtPaymentDialog(
    form: DebtPaymentFormState,
    debtPersonName: String,
    wallets: List<WalletEntity>,
    onAmountChange: (String) -> Unit,
    onWalletSelect: (Long?) -> Unit,
    onNoteChange: (String) -> Unit,
    onRecordAsTransactionChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.debts_payment_dialog_title, debtPersonName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(
                    value = form.amountInput,
                    onValueChange = onAmountChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(id = R.string.debts_payment_amount_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = form.error
                )
                OutlinedTextField(
                    value = form.note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(id = R.string.tx_note_label)) },
                    singleLine = true
                )
                if (wallets.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        items(wallets, key = { it.id }) { wallet ->
                            PiecerChip(
                                label = wallet.name,
                                selected = form.walletId == wallet.id,
                                onClick = { onWalletSelect(wallet.id) }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.debts_record_tx_title),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = form.recordAsTransaction,
                        onCheckedChange = onRecordAsTransactionChange
                    )
                }
                if (form.error) {
                    Text(
                        text = stringResource(id = R.string.debts_error_amount),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.coral
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !form.isSaving) {
                Text(text = stringResource(id = R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )
}
