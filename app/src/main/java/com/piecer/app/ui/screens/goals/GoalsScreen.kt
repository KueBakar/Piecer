package com.piecer.app.ui.screens.goals

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.R
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

private val GOAL_ICONS = listOf(
    "savings" to Icons.Rounded.Savings,
    "flag" to Icons.Rounded.Flag,
    "flight" to Icons.Rounded.Flight,
    "home" to Icons.Rounded.Home,
    "car" to Icons.Rounded.DirectionsCar,
    "laptop" to Icons.Rounded.Laptop,
    "school" to Icons.Rounded.School
)

private fun resolveGoalIcon(key: String): ImageVector {
    return GOAL_ICONS.firstOrNull { it.first == key }?.second ?: Icons.Rounded.Savings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    onBack: () -> Unit,
    onGoalSaved: () -> Unit,
    onGoalDeleted: () -> Unit,
    onTopUpSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = modifier,
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.goals_title),
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
                        contentDescription = stringResource(id = R.string.goals_add),
                        onClick = viewModel::openAddForm
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
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
                    .padding(horizontal = spacing.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmptyState(
                    icon = Icons.Rounded.Savings,
                    title = stringResource(id = R.string.goals_empty_title),
                    description = stringResource(id = R.string.goals_empty_desc)
                )
                PiecerButton(
                    text = stringResource(id = R.string.goals_add),
                    onClick = viewModel::openAddForm
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
                    key = { it.goal.id }
                ) { item ->
                    GoalCard(
                        item = item,
                        onTopUp = { viewModel.openTopUp(item.goal) },
                        onEdit = { viewModel.openEditForm(item) },
                        onDelete = { viewModel.requestDelete(item) }
                    )
                }
            }
        }
    }

    if (uiState.form.isVisible) {
        GoalFormSheet(
            form = uiState.form,
            onDismiss = viewModel::closeForm,
            onNameChange = viewModel::onNameChange,
            onTargetInputChange = viewModel::onTargetInputChange,
            onSavedInputChange = viewModel::onSavedInputChange,
            onDeadlineSelect = viewModel::onDeadlineSelect,
            onIconSelect = viewModel::onIconSelect,
            onSave = { viewModel.saveGoal(onGoalSaved) }
        )
    }

    uiState.topUp.goal?.let { targetGoal ->
        AlertDialog(
            onDismissRequest = viewModel::closeTopUp,
            title = { Text(text = stringResource(id = R.string.goals_topup_title, targetGoal.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    OutlinedTextField(
                        value = uiState.topUp.amountInput,
                        onValueChange = viewModel::onTopUpInputChange,
                        label = { Text(text = stringResource(id = R.string.goals_topup_amount_label)) },
                        prefix = { Text(text = stringResource(id = R.string.rupiah_prefix)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = uiState.topUp.error == GoalFormError.INVALID_TOPUP,
                        supportingText = if (uiState.topUp.error == GoalFormError.INVALID_TOPUP) {
                            { Text(text = stringResource(id = R.string.goals_error_invalid_topup)) }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmTopUp(onTopUpSaved) }) {
                    Text(text = stringResource(id = R.string.goals_topup_action))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeTopUp) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    uiState.pendingDeleteItem?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text(text = stringResource(id = R.string.goals_delete_title)) },
            text = { Text(text = stringResource(id = R.string.goals_delete_message, target.goal.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete(onGoalDeleted) }) {
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
private fun GoalCard(
    item: GoalCardItem,
    onTopUp: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val goal = item.goal
    val progress = item.progress

    PiecerCard(
        modifier = Modifier.fillMaxWidth(),
        variant = PiecerCardVariant.Light
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(spacing.iconButtonSize)
                        .clip(CircleShape)
                        .background(
                            if (progress.isAchieved) colors.mint.copy(alpha = 0.16f)
                            else colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolveGoalIcon(goal.iconKey),
                        contentDescription = null,
                        tint = if (progress.isAchieved) colors.mint else colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(spacing.iconMd)
                    )
                }
                Spacer(modifier = Modifier.width(spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onCardLight
                    )
                    Text(
                        text = stringResource(
                            id = R.string.goals_saved_of_target,
                            formatRupiah(progress.savedAmount),
                            formatRupiah(progress.targetAmount)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(id = R.string.goals_edit),
                        tint = colors.mutedText
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(id = R.string.action_delete),
                        tint = colors.mutedText
                    )
                }
            }

            ProgressBarPiece(
                progress = progress.ratio,
                isGoal = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (progress.isAchieved) {
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
                            text = stringResource(id = R.string.goals_achieved_badge),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.mint
                        )
                    }
                } else {
                    Text(
                        text = stringResource(
                            id = R.string.goals_remaining,
                            formatRupiah(progress.remainingAmount)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onCardLight
                    )
                }
                Text(
                    text = "${progress.percentageInt}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (progress.isAchieved) colors.mint else colors.mutedText
                )
            }

            if (!progress.isAchieved && goal.deadline != null && progress.estimatedMonthlySaving != null) {
                Text(
                    text = stringResource(
                        id = R.string.goals_monthly_estimate,
                        formatRupiah(progress.estimatedMonthlySaving),
                        DateUtils.formatDate(goal.deadline)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
            } else if (goal.deadline != null) {
                Text(
                    text = stringResource(
                        id = R.string.goals_deadline_label_value,
                        DateUtils.formatDate(goal.deadline)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
            }

            PiecerButton(
                text = stringResource(id = R.string.goals_topup_button),
                onClick = onTopUp,
                variant = if (progress.isAchieved) PiecerButtonVariant.Secondary else PiecerButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalFormSheet(
    form: GoalFormState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onTargetInputChange: (String) -> Unit,
    onSavedInputChange: (String) -> Unit,
    onDeadlineSelect: (Long?) -> Unit,
    onIconSelect: (String) -> Unit,
    onSave: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenPadding)
                .navigationBarsPadding()
                .padding(bottom = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                text = stringResource(
                    id = if (form.editingGoalId != null) R.string.goals_edit_title else R.string.goals_add_title
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = form.name,
                onValueChange = onNameChange,
                label = { Text(text = stringResource(id = R.string.goals_name_label)) },
                singleLine = true,
                isError = form.error == GoalFormError.EMPTY_NAME,
                supportingText = if (form.error == GoalFormError.EMPTY_NAME) {
                    { Text(text = stringResource(id = R.string.goals_error_empty_name)) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.targetInput,
                onValueChange = onTargetInputChange,
                label = { Text(text = stringResource(id = R.string.goals_target_label)) },
                prefix = { Text(text = stringResource(id = R.string.rupiah_prefix)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = form.error == GoalFormError.INVALID_TARGET,
                supportingText = if (form.error == GoalFormError.INVALID_TARGET) {
                    { Text(text = stringResource(id = R.string.goals_error_invalid_target)) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.savedInput,
                onValueChange = onSavedInputChange,
                label = { Text(text = stringResource(id = R.string.goals_saved_label)) },
                prefix = { Text(text = stringResource(id = R.string.rupiah_prefix)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = form.error == GoalFormError.INVALID_SAVED,
                supportingText = if (form.error == GoalFormError.INVALID_SAVED) {
                    { Text(text = stringResource(id = R.string.goals_error_invalid_saved)) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (form.deadlineMillis != null) {
                            stringResource(
                                id = R.string.goals_deadline_selected,
                                DateUtils.formatDate(form.deadlineMillis)
                            )
                        } else {
                            stringResource(id = R.string.goals_deadline_optional)
                        }
                    )
                }
                if (form.deadlineMillis != null) {
                    IconButton(onClick = { onDeadlineSelect(null) }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(id = R.string.goals_deadline_clear),
                            tint = colors.mutedText
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = stringResource(id = R.string.goals_icon_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.mutedText
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    items(GOAL_ICONS) { (key, _) ->
                        PiecerChip(
                            label = key.replaceFirstChar { it.uppercase() },
                            selected = form.iconKey == key,
                            onClick = { onIconSelect(key) }
                        )
                    }
                }
            }

            PiecerButton(
                text = stringResource(id = R.string.action_save),
                onClick = onSave,
                enabled = !form.isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.deadlineMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeadlineSelect(datePickerState.selectedDateMillis)
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
