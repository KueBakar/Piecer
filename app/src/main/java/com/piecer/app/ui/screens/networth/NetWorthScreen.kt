package com.piecer.app.ui.screens.networth

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.R
import com.piecer.app.data.local.entity.AssetEntity
import com.piecer.app.data.local.entity.AssetType
import com.piecer.app.ui.components.AmountText
import com.piecer.app.ui.components.AmountTone
import com.piecer.app.ui.components.EmptyState
import com.piecer.app.ui.components.PiecerButton
import com.piecer.app.ui.components.PiecerButtonVariant
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.components.PiecerChip
import com.piecer.app.ui.components.PiecerIconButton
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.AssetHistoryPoint
import com.piecer.app.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthScreen(
    viewModel: NetWorthViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val savedMsg = stringResource(id = R.string.networth_saved_snackbar)
    val deletedMsg = stringResource(id = R.string.networth_deleted_snackbar)

    Scaffold(
        modifier = modifier,
        containerColor = colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.networth_title),
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
                        contentDescription = stringResource(id = R.string.networth_add_asset),
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
                    Text(
                        text = stringResource(id = R.string.networth_total_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    AmountText(
                        amount = uiState.summary.netWorth,
                        isHidden = uiState.isBalanceHidden,
                        tone = if (uiState.summary.netWorth >= 0L) AmountTone.Income else AmountTone.Expense,
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(spacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.networth_assets_sum),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.mutedText
                            )
                            AmountText(
                                amount = uiState.summary.totalAssets,
                                isHidden = uiState.isBalanceHidden,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.networth_wallets_sum),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.mutedText
                            )
                            AmountText(
                                amount = uiState.summary.totalWalletBalance,
                                isHidden = uiState.isBalanceHidden,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.networth_debts_sum),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.mutedText
                            )
                            AmountText(
                                amount = uiState.summary.totalDebts,
                                isHidden = uiState.isBalanceHidden,
                                tone = AmountTone.Expense,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(id = R.string.networth_manual_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
            }

            if (uiState.timeline.size >= 2 && !uiState.isBalanceHidden) {
                item {
                    PiecerCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = PiecerCardVariant.Light
                    ) {
                        Text(
                            text = stringResource(id = R.string.networth_chart_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onCardLight
                        )
                        Spacer(modifier = Modifier.height(spacing.sm))
                        AssetHistoryChart(
                            points = uiState.timeline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                }
            }

            if (uiState.assets.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.AccountBalance,
                        title = stringResource(id = R.string.networth_empty_title),
                        description = stringResource(id = R.string.networth_empty_desc)
                    )
                }
            } else {
                items(uiState.assets, key = { it.id }) { asset ->
                    AssetItemCard(
                        asset = asset,
                        isBalanceHidden = uiState.isBalanceHidden,
                        onUpdateValue = { viewModel.openUpdateValueDialog(asset) },
                        onEdit = { viewModel.openEditForm(asset) },
                        onDelete = { viewModel.requestDelete(asset) }
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
            AssetFormSheet(
                form = uiState.form,
                onNameChange = viewModel::onNameChange,
                onTypeSelect = viewModel::onTypeSelect,
                onValueChange = viewModel::onValueChange,
                onNoteChange = viewModel::onNoteChange,
                onSave = {
                    viewModel.saveAsset {
                        scope.launch { snackbarHostState.showSnackbar(savedMsg) }
                    }
                },
                onDismiss = viewModel::dismissForm
            )
        }
    }

    uiState.updateValueDialog.asset?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissUpdateValueDialog,
            title = { Text(text = stringResource(id = R.string.networth_update_value_title, target.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    OutlinedTextField(
                        value = uiState.updateValueDialog.valueInput,
                        onValueChange = viewModel::onUpdateValueInputChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(id = R.string.networth_value_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.updateValueDialog.error
                    )
                    if (uiState.updateValueDialog.error) {
                        Text(
                            text = stringResource(id = R.string.networth_error_value),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.coral
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmUpdateValue {
                            scope.launch { snackbarHostState.showSnackbar(savedMsg) }
                        }
                    },
                    enabled = !uiState.updateValueDialog.isSaving
                ) {
                    Text(text = stringResource(id = R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissUpdateValueDialog) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    uiState.pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text(text = stringResource(id = R.string.networth_delete_title)) },
            text = { Text(text = stringResource(id = R.string.networth_delete_confirm, target.name)) },
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
private fun AssetHistoryChart(
    points: List<AssetHistoryPoint>,
    modifier: Modifier = Modifier
) {
    val colors = PiecerTheme.colors
    val lineColor = colors.mint
    val dotColor = colors.mint
    val gridColor = colors.cardLightBorder

    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val minVal = points.minOf { it.totalValue }
        val maxVal = points.maxOf { it.totalValue }
        val range = (maxVal - minVal).coerceAtLeast(1L)
        val pad = 12f
        val w = (size.width - pad * 2).coerceAtLeast(1f)
        val h = (size.height - pad * 2).coerceAtLeast(1f)

        drawLine(
            color = gridColor,
            start = Offset(pad, size.height - pad),
            end = Offset(size.width - pad, size.height - pad),
            strokeWidth = 2f
        )

        val path = Path()
        val coords = points.mapIndexed { index, pt ->
            val x = pad + (w * index.toFloat() / (points.size - 1).toFloat())
            val ratio = (pt.totalValue - minVal).toFloat() / range.toFloat()
            val y = pad + h * (1f - ratio)
            Offset(x, y)
        }
        coords.forEachIndexed { idx, pt ->
            if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        coords.forEach { pt ->
            drawCircle(color = dotColor, radius = 7f, center = pt)
        }
    }
}

@Composable
private fun AssetItemCard(
    asset: AssetEntity,
    isBalanceHidden: Boolean,
    onUpdateValue: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors

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
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(
                        text = asset.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onCardLight
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = assetTypeLabel(asset.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.mutedText,
                            modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.xxs)
                        )
                    }
                }
                if (asset.note.isNotBlank()) {
                    Text(
                        text = asset.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                }
                Text(
                    text = stringResource(id = R.string.networth_updated_label, DateUtils.formatDate(asset.updatedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText
                )
            }
            AmountText(
                amount = asset.value,
                isHidden = isBalanceHidden,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onUpdateValue) {
                Text(text = stringResource(id = R.string.networth_update_value_btn))
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(id = R.string.action_edit),
                        tint = colors.mutedText,
                        modifier = Modifier.size(spacing.iconMd)
                    )
                }
                IconButton(onClick = onDelete) {
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

@Composable
private fun AssetFormSheet(
    form: AssetFormState,
    onNameChange: (String) -> Unit,
    onTypeSelect: (AssetType) -> Unit,
    onValueChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenPadding)
            .padding(bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Text(
            text = stringResource(
                id = if (form.editingId == null) R.string.networth_add_asset else R.string.networth_edit_asset
            ),
            style = MaterialTheme.typography.titleLarge,
            color = colorScheme.onSurface
        )

        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.networth_asset_name_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            isError = form.error == AssetFormError.EMPTY_NAME
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            items(AssetType.entries) { type ->
                PiecerChip(
                    label = assetTypeLabel(type),
                    selected = form.type == type,
                    onClick = { onTypeSelect(type) }
                )
            }
        }

        OutlinedTextField(
            value = form.valueInput,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.networth_value_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium,
            isError = form.error == AssetFormError.INVALID_VALUE
        )

        OutlinedTextField(
            value = form.note,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.tx_note_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        form.error?.let { err ->
            val msgRes = when (err) {
                AssetFormError.EMPTY_NAME -> R.string.networth_error_name
                AssetFormError.INVALID_VALUE -> R.string.networth_error_value
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
}

@Composable
private fun assetTypeLabel(type: AssetType): String = when (type) {
    AssetType.EMAS -> stringResource(id = R.string.asset_type_emas)
    AssetType.SAHAM -> stringResource(id = R.string.asset_type_saham)
    AssetType.REKSADANA -> stringResource(id = R.string.asset_type_reksadana)
    AssetType.CRYPTO -> stringResource(id = R.string.asset_type_crypto)
    AssetType.PROPERTI -> stringResource(id = R.string.asset_type_properti)
    AssetType.KENDARAAN -> stringResource(id = R.string.asset_type_kendaraan)
    AssetType.LAINNYA -> stringResource(id = R.string.asset_type_lainnya)
}
