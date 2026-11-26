package com.piecer.app.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.R
import com.piecer.app.ui.components.EmptyState
import com.piecer.app.ui.components.PiecerChip
import com.piecer.app.ui.components.TransactionRowItem
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.DayHeaderLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onEditTransaction: (Long) -> Unit,
    onTransactionDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = PiecerTheme.spacing
    val colorScheme = MaterialTheme.colorScheme

    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.screenPadding)
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(text = stringResource(id = R.string.history_search_placeholder))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(id = R.string.history_search_clear)
                        )
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorScheme.primary,
                unfocusedBorderColor = colorScheme.outline,
                focusedContainerColor = colorScheme.surface,
                unfocusedContainerColor = colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        val filterOptions = listOf(
            TransactionFilter.ALL to R.string.history_filter_all,
            TransactionFilter.INCOME to R.string.tx_type_income,
            TransactionFilter.EXPENSE to R.string.tx_type_expense,
            TransactionFilter.TRANSFER to R.string.tx_type_transfer
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            contentPadding = PaddingValues(vertical = spacing.xs)
        ) {
            uiState.selectedCategoryName?.let { categoryName ->
                item(key = "category_filter") {
                    PiecerChip(
                        label = stringResource(id = R.string.history_filter_category, categoryName),
                        selected = true,
                        onClick = viewModel::clearCategoryFilter
                    )
                }
            }
            items(filterOptions, key = { it.first.name }) { (filter, labelRes) ->
                PiecerChip(
                    label = stringResource(id = labelRes),
                    selected = uiState.selectedFilter == filter,
                    onClick = { viewModel.onFilterSelected(filter) }
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.sm))

        if (uiState.groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = stringResource(
                        id = if (uiState.hasAnyTransaction) {
                            R.string.history_empty_filtered_title
                        } else {
                            R.string.history_empty_title
                        }
                    ),
                    description = stringResource(
                        id = if (uiState.hasAnyTransaction) {
                            R.string.history_empty_filtered_desc
                        } else {
                            R.string.history_empty_desc
                        }
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                contentPadding = PaddingValues(bottom = spacing.xl)
            ) {
                uiState.groups.forEach { group ->
                    item(key = "header_${group.dayStartMillis}") {
                        Text(
                            text = resolveDayHeader(group.headerLabel),
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = spacing.sm, bottom = spacing.xs)
                        )
                    }

                    items(
                        items = group.items,
                        key = { it.transaction.id }
                    ) { item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    pendingDeleteId = item.transaction.id
                                }
                                false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.large)
                                        .background(colorScheme.error)
                                        .padding(horizontal = spacing.lg),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = stringResource(id = R.string.history_delete),
                                        tint = colorScheme.onError
                                    )
                                }
                            }
                        ) {
                            TransactionRowItem(
                                item = item,
                                onClick = { onEditTransaction(item.transaction.id) },
                                onDeleteClick = { pendingDeleteId = item.transaction.id }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDeleteId?.let { targetId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(text = stringResource(id = R.string.history_delete_title)) },
            text = { Text(text = stringResource(id = R.string.history_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(targetId)
                        pendingDeleteId = null
                        onTransactionDeleted()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.history_delete_confirm),
                        color = colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(text = stringResource(id = R.string.history_delete_cancel))
                }
            }
        )
    }
}

@Composable
private fun resolveDayHeader(label: DayHeaderLabel): String {
    return when (label) {
        DayHeaderLabel.Today -> stringResource(id = R.string.history_day_today)
        DayHeaderLabel.Yesterday -> stringResource(id = R.string.history_day_yesterday)
        is DayHeaderLabel.Formatted -> label.text
    }
}
