package com.piecer.app.ui.screens.addtransaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.piecer.app.R
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.ui.components.PiecerButton
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.components.PiecerChip
import com.piecer.app.ui.components.formatRupiah
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.DateUtils
import com.piecer.app.util.quickentry.QuickEntryFlag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEntryPreviewBottomSheet(
    entries: List<QuickEntryPreviewItem>,
    expenseCategories: List<CategoryEntity>,
    incomeCategories: List<CategoryEntity>,
    wallets: List<WalletEntity>,
    isSaving: Boolean,
    showValidationError: Boolean,
    onDismiss: () -> Unit,
    onAmountChange: (Long, Long) -> Unit,
    onConvertAmbiguousMToBillion: (Long) -> Unit,
    onTypeChange: (Long, TransactionType) -> Unit,
    onCategorySelect: (Long, Long) -> Unit,
    onWalletSelect: (Long, Long) -> Unit,
    onToWalletSelect: (Long, Long) -> Unit,
    onDateSelect: (Long, Long) -> Unit,
    onNoteChange: (Long, String) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onSaveAll: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    var datePickerItemId by remember { mutableStateOf<Long?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screenPadding)
                .padding(bottom = spacing.md)
        ) {
            Text(
                text = stringResource(id = R.string.quick_entry_preview_title),
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = stringResource(id = R.string.quick_entry_preview_subtitle, entries.size),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(spacing.md))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                contentPadding = PaddingValues(bottom = spacing.sm)
            ) {
                items(entries, key = { it.id }) { item ->
                    val categoriesForType = when (item.type) {
                        TransactionType.EXPENSE -> expenseCategories
                        TransactionType.INCOME -> incomeCategories
                        TransactionType.TRANSFER -> emptyList()
                    }
                    PreviewEntryCard(
                        index = entries.indexOf(item) + 1,
                        item = item,
                        categories = categoriesForType,
                        wallets = wallets,
                        onAmountChange = { onAmountChange(item.id, it) },
                        onConvertAmbiguousMToBillion = { onConvertAmbiguousMToBillion(item.id) },
                        onTypeChange = { onTypeChange(item.id, it) },
                        onCategorySelect = { onCategorySelect(item.id, it) },
                        onWalletSelect = { onWalletSelect(item.id, it) },
                        onToWalletSelect = { onToWalletSelect(item.id, it) },
                        onOpenDatePicker = { datePickerItemId = item.id },
                        onNoteChange = { onNoteChange(item.id, it) },
                        onRemove = { onRemoveEntry(item.id) }
                    )
                }
            }

            if (showValidationError) {
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = stringResource(id = R.string.quick_entry_preview_invalid_entry),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(spacing.md))

            PiecerButton(
                text = stringResource(id = R.string.quick_entry_preview_save_all),
                onClick = onSaveAll,
                enabled = !isSaving && entries.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    val pickerItem = entries.firstOrNull { it.id == datePickerItemId }
    if (pickerItem != null) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = pickerItem.dateMillis)
        DatePickerDialog(
            onDismissRequest = { datePickerItemId = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateSelect(pickerItem.id, it) }
                        datePickerItemId = null
                    }
                ) {
                    Text(text = stringResource(id = R.string.tx_date_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerItemId = null }) {
                    Text(text = stringResource(id = R.string.tx_date_picker_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun PreviewEntryCard(
    index: Int,
    item: QuickEntryPreviewItem,
    categories: List<CategoryEntity>,
    wallets: List<WalletEntity>,
    onAmountChange: (Long) -> Unit,
    onConvertAmbiguousMToBillion: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onCategorySelect: (Long) -> Unit,
    onWalletSelect: (Long) -> Unit,
    onToWalletSelect: (Long) -> Unit,
    onOpenDatePicker: () -> Unit,
    onNoteChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    val amber = PiecerTheme.colors.amber
    var showAllCategories by remember(item.id, item.type) {
        mutableStateOf(item.categoryCandidates.isEmpty())
    }

    PiecerCard(variant = PiecerCardVariant.Light) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#$index · ${formatRupiah(item.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurface
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(id = R.string.quick_entry_preview_delete_entry),
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }

            if (QuickEntryFlag.AMBIGUOUS_M in item.flags) {
                PreviewFlagBanner(
                    message = stringResource(id = R.string.quick_entry_preview_ambiguous_m),
                    actionLabel = stringResource(id = R.string.quick_entry_preview_convert_billion),
                    onAction = onConvertAmbiguousMToBillion,
                    accentColor = amber
                )
            }
            if (QuickEntryFlag.LARGE_AMOUNT in item.flags) {
                PreviewFlagBanner(
                    message = stringResource(id = R.string.quick_entry_preview_large_amount),
                    accentColor = amber
                )
            }
            if (QuickEntryFlag.NO_CATEGORY in item.flags && item.type != TransactionType.TRANSFER) {
                PreviewFlagBanner(
                    message = stringResource(id = R.string.quick_entry_preview_no_category),
                    accentColor = amber
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                items(
                    listOf(
                        TransactionType.EXPENSE to R.string.tx_type_expense,
                        TransactionType.INCOME to R.string.tx_type_income,
                        TransactionType.TRANSFER to R.string.tx_type_transfer
                    ),
                    key = { it.first.name }
                ) { (typeOption, labelRes) ->
                    PiecerChip(
                        label = stringResource(id = labelRes),
                        selected = item.type == typeOption,
                        onClick = { onTypeChange(typeOption) }
                    )
                }
            }

            OutlinedTextField(
                value = if (item.amount > 0L) item.amount.toString() else "",
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(15)
                    onAmountChange(digits.toLongOrNull() ?: 0L)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(id = R.string.quick_entry_preview_amount_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outline
                )
            )

            if (item.type != TransactionType.TRANSFER) {
                val topSuggestions = item.categoryCandidates
                    .take(3)
                    .mapNotNull { cand -> categories.firstOrNull { it.id == cand.categoryId } }
                    .distinctBy { it.id }

                val displayedCategories = if (showAllCategories || topSuggestions.isEmpty()) {
                    categories
                } else {
                    val selectedOutsideTop = categories.firstOrNull { cat ->
                        cat.id == item.categoryId && topSuggestions.none { it.id == cat.id }
                    }
                    if (selectedOutsideTop != null) topSuggestions + selectedOutsideTop else topSuggestions
                }

                Text(
                    text = stringResource(id = R.string.quick_entry_preview_category_suggestions),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    items(displayedCategories, key = { it.id }) { cat ->
                        PiecerChip(
                            label = cat.name,
                            selected = cat.id == item.categoryId,
                            onClick = { onCategorySelect(cat.id) }
                        )
                    }
                    if (!showAllCategories && categories.size > displayedCategories.size) {
                        item {
                            PiecerChip(
                                label = stringResource(id = R.string.quick_entry_preview_other_categories),
                                selected = false,
                                onClick = { showAllCategories = true }
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(
                    id = if (item.type == TransactionType.TRANSFER) {
                        R.string.tx_label_wallet_from
                    } else {
                        R.string.tx_label_wallet
                    }
                ),
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                items(wallets, key = { it.id }) { wallet ->
                    PiecerChip(
                        label = wallet.name,
                        selected = wallet.id == item.walletId,
                        onClick = { onWalletSelect(wallet.id) }
                    )
                }
            }

            if (item.type == TransactionType.TRANSFER) {
                Text(
                    text = stringResource(id = R.string.tx_label_wallet_to),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    items(wallets, key = { it.id }) { wallet ->
                        PiecerChip(
                            label = wallet.name,
                            selected = wallet.id == item.toWalletId,
                            onClick = { onToWalletSelect(wallet.id) }
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenDatePicker),
                shape = MaterialTheme.shapes.medium,
                color = colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md, vertical = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DateUtils.formatDate(item.dateMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface
                    )
                }
            }

            OutlinedTextField(
                value = item.note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(id = R.string.tx_label_note)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outline
                )
            )
        }
    }
}

@Composable
private fun PreviewFlagBanner(
    message: String,
    accentColor: androidx.compose.ui.graphics.Color,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val spacing = PiecerTheme.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.sm, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}
