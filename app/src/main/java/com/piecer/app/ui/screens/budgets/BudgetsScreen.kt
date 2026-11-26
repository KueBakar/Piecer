package com.piecer.app.ui.screens.budgets

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.R
import com.piecer.app.data.local.entity.CategoryEntity
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
import com.piecer.app.util.BudgetStatus
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    onBack: () -> Unit,
    onBudgetSaved: () -> Unit,
    onBudgetDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.budgets_title),
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
                actions = {
                    PiecerIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = stringResource(id = R.string.budgets_add),
                        onClick = viewModel::openAddForm,
                        variant = PiecerButtonVariant.Primary,
                        enabled = uiState.availableCategoriesForAdd.isNotEmpty(),
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
        if (uiState.items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(spacing.screenPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmptyState(
                    icon = Icons.Rounded.PieChart,
                    title = stringResource(id = R.string.budgets_empty_title),
                    description = stringResource(id = R.string.budgets_empty_desc)
                )
                Spacer(modifier = Modifier.height(spacing.md))
                PiecerButton(
                    text = stringResource(id = R.string.budgets_add),
                    onClick = viewModel::openAddForm,
                    leadingIcon = Icons.Rounded.Add
                )
            }
        } else {
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
                items(
                    items = uiState.items,
                    key = { it.budget.id }
                ) { item ->
                    BudgetItemCard(
                        item = item,
                        onEdit = { viewModel.openEditForm(item) },
                        onDelete = { viewModel.requestDelete(item) }
                    )
                }
            }
        }
    }

    if (uiState.form.isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val categories = if (uiState.form.editingBudgetId != null) {
            uiState.allExpenseCategories.filter { it.id == uiState.form.selectedCategoryId }
        } else {
            uiState.availableCategoriesForAdd
        }

        ModalBottomSheet(
            onDismissRequest = viewModel::closeForm,
            sheetState = sheetState,
            containerColor = colorScheme.surface
        ) {
            BudgetFormSheetContent(
                form = uiState.form,
                categories = categories,
                onCategorySelect = viewModel::onCategorySelect,
                onLimitInputChange = viewModel::onLimitInputChange,
                onSave = { viewModel.saveBudget(onBudgetSaved) },
                onCancel = viewModel::closeForm
            )
        }
    }

    uiState.pendingDeleteItem?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = {
                Text(text = stringResource(id = R.string.budgets_delete_title, target.category.name))
            },
            text = {
                Text(text = stringResource(id = R.string.budgets_delete_message))
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete(onBudgetDeleted) }) {
                    Text(
                        text = stringResource(id = R.string.history_delete_confirm),
                        color = colors.coral
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) {
                    Text(text = stringResource(id = R.string.history_delete_cancel))
                }
            }
        )
    }
}

@Composable
private fun BudgetItemCard(
    item: BudgetCardItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val progress = item.progress

    val badgeColor = when (progress.status) {
        BudgetStatus.SAFE -> colors.mint
        BudgetStatus.WARNING -> colors.amber
        BudgetStatus.EXCEEDED -> colors.coral
    }

    PiecerCard(
        modifier = Modifier.fillMaxWidth(),
        variant = PiecerCardVariant.Light,
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.category.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onCardLight
            )
            Text(
                text = stringResource(id = R.string.budgets_percent_label, progress.percentageInt),
                style = MaterialTheme.typography.labelLarge,
                color = badgeColor
            )
        }

        Spacer(modifier = Modifier.height(spacing.sm))
        ProgressBarPiece(progress = progress.progressRatio)
        Spacer(modifier = Modifier.height(spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(id = R.string.budgets_spent_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText
                )
                AmountText(
                    amount = progress.spent,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.budgets_limit_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText
                )
                AmountText(
                    amount = progress.limit,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(
                        id = if (progress.remaining < 0L) R.string.budgets_over_label else R.string.budgets_remaining_label
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText
                )
                Text(
                    text = formatRupiah(abs(progress.remaining)),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (progress.remaining < 0L) colors.coral else colors.mint
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(id = R.string.budgets_edit),
                    tint = colors.mutedText
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(id = R.string.history_delete),
                    tint = colors.coral
                )
            }
        }
    }
}

@Composable
private fun BudgetFormSheetContent(
    form: BudgetFormState,
    categories: List<CategoryEntity>,
    onCategorySelect: (Long) -> Unit,
    onLimitInputChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenPadding)
            .padding(bottom = spacing.xl)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Text(
            text = stringResource(
                id = if (form.editingBudgetId == null) R.string.budgets_add else R.string.budgets_edit
            ),
            style = MaterialTheme.typography.titleLarge,
            color = colorScheme.onSurface
        )

        Text(
            text = stringResource(id = R.string.tx_label_category),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurface
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            items(categories, key = { it.id }) { cat ->
                PiecerChip(
                    label = cat.name,
                    selected = form.selectedCategoryId == cat.id,
                    onClick = { onCategorySelect(cat.id) }
                )
            }
        }

        OutlinedTextField(
            value = form.limitInput,
            onValueChange = onLimitInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.budgets_limit_input_label)) },
            placeholder = { Text(text = stringResource(id = R.string.budgets_limit_input_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium,
            isError = form.error == BudgetFormError.INVALID_LIMIT
        )

        form.error?.let { err ->
            val msgRes = when (err) {
                BudgetFormError.NO_CATEGORY -> R.string.budgets_error_category
                BudgetFormError.INVALID_LIMIT -> R.string.budgets_error_limit
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
                text = stringResource(id = R.string.history_delete_cancel),
                onClick = onCancel,
                variant = PiecerButtonVariant.Secondary,
                modifier = Modifier.weight(1f)
            )
            PiecerButton(
                text = stringResource(id = R.string.tx_save),
                onClick = onSave,
                variant = PiecerButtonVariant.Primary,
                enabled = !form.isSaving,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
