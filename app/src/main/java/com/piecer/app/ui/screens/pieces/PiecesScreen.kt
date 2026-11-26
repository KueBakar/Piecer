package com.piecer.app.ui.screens.pieces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.R
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.ui.components.AmountText
import com.piecer.app.ui.components.AmountTone
import com.piecer.app.ui.components.PiecerButton
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.components.PiecerChip
import com.piecer.app.ui.components.ProgressBarPiece
import com.piecer.app.ui.components.formatRupiah
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.PieceBalanceSummary
import com.piecer.app.util.PieceTemplateType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PiecesScreen(
    viewModel: PiecesViewModel,
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
                        text = stringResource(id = R.string.pieces_title),
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
                    if (uiState.overview.summaries.isNotEmpty()) {
                        IconButton(onClick = viewModel::openPercentEditor) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = stringResource(id = R.string.pieces_edit_percents)
                            )
                        }
                        IconButton(onClick = viewModel::openAddPiece) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(id = R.string.pieces_add)
                            )
                        }
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
            item {
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
                                text = stringResource(id = R.string.pieces_toggle_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onCardLight
                            )
                            Text(
                                text = stringResource(id = R.string.pieces_toggle_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedText
                            )
                        }
                        Spacer(modifier = Modifier.width(spacing.md))
                        Switch(
                            checked = uiState.piecesEnabled,
                            onCheckedChange = viewModel::setPiecesEnabled
                        )
                    }
                }
            }

            if (uiState.overview.summaries.isNotEmpty() && !uiState.isPercentValid) {
                item {
                    PiecerCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = PiecerCardVariant.Light
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.pieces_percent_invalid_banner,
                                uiState.totalPercent
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.coral
                        )
                        Spacer(modifier = Modifier.height(spacing.sm))
                        OutlinedButton(onClick = viewModel::openPercentEditor) {
                            Text(text = stringResource(id = R.string.pieces_edit_percents))
                        }
                    }
                }
            }

            if (uiState.overview.summaries.isEmpty()) {
                item {
                    PiecesTemplateSetupCard(
                        onSelectTemplate = { template ->
                            viewModel.applyTemplate(template)
                        }
                    )
                }
            } else {
                item {
                    PiecerCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = PiecerCardVariant.Dark
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.lg)
                        ) {
                            PiecesSegmentedDonut(
                                summaries = uiState.overview.summaries,
                                size = 128.dp,
                                strokeWidth = 18.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.pieces_total_remaining),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.mutedText
                                )
                                Spacer(modifier = Modifier.height(spacing.xxs))
                                AmountText(
                                    amount = uiState.overview.totalRemaining,
                                    tone = if (uiState.overview.totalRemaining >= 0L) AmountTone.Income else AmountTone.Expense,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Spacer(modifier = Modifier.height(spacing.xs))
                                Text(
                                    text = stringResource(
                                        id = R.string.pieces_allocated_summary,
                                        formatRupiah(uiState.overview.totalAllocated)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.mutedText
                                )
                            }
                        }
                    }
                }

                items(uiState.overview.summaries, key = { it.piece.id }) { summary ->
                    PieceSummaryCard(
                        summary = summary,
                        categories = uiState.expenseCategories,
                        onEdit = { viewModel.openEditPiece(summary) },
                        onDelete = { viewModel.requestDeletePiece(summary.piece) },
                        onEditCategories = { viewModel.openCategoryMapping(summary.piece) }
                    )
                }

                if (uiState.overview.unassignedExpense > 0L) {
                    item {
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
                                        text = stringResource(id = R.string.pieces_unassigned_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.onCardLight
                                    )
                                    Text(
                                        text = stringResource(id = R.string.pieces_unassigned_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.mutedText
                                    )
                                }
                                AmountText(
                                    amount = uiState.overview.unassignedExpense,
                                    tone = AmountTone.Neutral,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { viewModel.applyTemplate(PieceTemplateType.BALANCED_50_30_20) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.pieces_reset_template))
                    }
                }
            }
        }
    }

    if (uiState.pieceForm.isVisible) {
        val emptyNameErr = stringResource(id = R.string.pieces_error_name)
        val invalidPctErr = stringResource(id = R.string.pieces_error_percent)
        val sumErrTemplate = stringResource(id = R.string.pieces_error_total_100)
        PieceFormDialog(
            state = uiState.pieceForm,
            onNameChange = viewModel::onPieceNameChange,
            onPercentChange = viewModel::onPiecePercentChange,
            onColorSelect = viewModel::onPieceColorSelect,
            onDismiss = viewModel::closePieceForm,
            onSave = {
                viewModel.savePiece(
                    emptyNameError = emptyNameErr,
                    invalidPercentError = invalidPctErr,
                    totalMustBe100Error = { total -> String.format(sumErrTemplate, total) }
                )
            }
        )
    }

    if (uiState.percentEditor.isVisible) {
        val sumErrTemplate = stringResource(id = R.string.pieces_error_total_100)
        PercentEditorDialog(
            summaries = uiState.overview.summaries,
            state = uiState.percentEditor,
            onPercentChange = viewModel::onEditorPercentChange,
            onDismiss = viewModel::closePercentEditor,
            onSave = {
                viewModel.saveAllPercents { total -> String.format(sumErrTemplate, total) }
            }
        )
    }

    if (uiState.categoryMapping.isVisible && uiState.categoryMapping.targetPiece != null) {
        val targetPiece = uiState.categoryMapping.targetPiece!!
        CategoryMappingDialog(
            pieceName = targetPiece.name,
            pieceId = targetPiece.id,
            categories = uiState.expenseCategories,
            categoryToPieceId = uiState.categoryToPieceId,
            onToggle = { catId, assigned ->
                viewModel.toggleCategoryForPiece(catId, targetPiece.id, assigned)
            },
            onDismiss = viewModel::closeCategoryMapping
        )
    }

    uiState.pendingDeletePiece?.let { piece ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeletePiece,
            title = { Text(text = stringResource(id = R.string.pieces_delete_title, piece.name)) },
            text = { Text(text = stringResource(id = R.string.pieces_delete_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeletePiece) {
                    Text(text = stringResource(id = R.string.action_delete), color = colors.coral)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeletePiece) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun PiecesSegmentedDonut(
    summaries: List<PieceBalanceSummary>,
    size: Dp,
    strokeWidth: Dp,
    modifier: Modifier = Modifier
) {
    val colors = PiecerTheme.colors
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val totalValue = summaries.sumOf { it.remaining.coerceAtLeast(0L) }
    val usePercentFallback = totalValue <= 0L

    Canvas(modifier = modifier.size(size)) {
        val strokePx = strokeWidth.toPx()
        val diameter = this.size.minDimension - strokePx
        val topLeft = Offset(strokePx / 2f, strokePx / 2f)
        val arcSize = Size(diameter, diameter)
        val gapDegrees = if (summaries.size > 1) 5f else 0f
        val totalGap = gapDegrees * summaries.size
        val availableDegrees = (360f - totalGap).coerceAtLeast(0f)

        if (summaries.isEmpty()) {
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            return@Canvas
        }

        var startAngle = -90f
        val denom = if (usePercentFallback) {
            summaries.sumOf { it.piece.percent }.coerceAtLeast(1).toFloat()
        } else {
            totalValue.toFloat()
        }

        summaries.forEach { summary ->
            val value = if (usePercentFallback) {
                summary.piece.percent.toFloat()
            } else {
                summary.remaining.coerceAtLeast(0L).toFloat()
            }
            val sweep = (value / denom) * availableDegrees
            if (sweep > 0.5f) {
                drawArc(
                    color = resolvePieceColor(summary.piece.colorKey, colors),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
            startAngle += sweep + gapDegrees
        }
    }
}

fun resolvePieceColor(colorKey: String, colors: com.piecer.app.ui.theme.PiecerColors): Color {
    return when (colorKey) {
        "mint" -> colors.mint
        "amber" -> colors.amber
        "coral" -> colors.coral
        "stone" -> colors.mutedText
        else -> colors.onCardLight
    }
}

@Composable
private fun PiecesTemplateSetupCard(
    onSelectTemplate: (PieceTemplateType) -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors

    PiecerCard(
        modifier = Modifier.fillMaxWidth(),
        variant = PiecerCardVariant.Light
    ) {
        Text(
            text = stringResource(id = R.string.pieces_setup_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onCardLight
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = stringResource(id = R.string.pieces_setup_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.mutedText
        )
        Spacer(modifier = Modifier.height(spacing.md))

        TemplateOptionRow(
            title = stringResource(id = R.string.pieces_template_503020_title),
            subtitle = stringResource(id = R.string.pieces_template_503020_desc),
            onClick = { onSelectTemplate(PieceTemplateType.BALANCED_50_30_20) }
        )
        Spacer(modifier = Modifier.height(spacing.sm))
        TemplateOptionRow(
            title = stringResource(id = R.string.pieces_template_702010_title),
            subtitle = stringResource(id = R.string.pieces_template_702010_desc),
            onClick = { onSelectTemplate(PieceTemplateType.SAVING_70_20_10) }
        )
        Spacer(modifier = Modifier.height(spacing.sm))
        TemplateOptionRow(
            title = stringResource(id = R.string.pieces_template_custom_title),
            subtitle = stringResource(id = R.string.pieces_template_custom_desc),
            onClick = { onSelectTemplate(PieceTemplateType.CUSTOM) }
        )
    }
}

@Composable
private fun TemplateOptionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(spacing.borderWidth, colors.cardLightBorder)
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onCardLight
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedText
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PieceSummaryCard(
    summary: PieceBalanceSummary,
    categories: List<CategoryEntity>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEditCategories: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val pieceColor = resolvePieceColor(summary.piece.colorKey, colors)
    val categoryNames = categories.filter { it.id in summary.categoryIds }.map { it.name }

    PiecerCard(
        modifier = Modifier.fillMaxWidth(),
        variant = PiecerCardVariant.Light
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(pieceColor)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(
                    text = summary.piece.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onCardLight
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(
                    text = "${summary.piece.percent}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.mutedText
                )
            }
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(id = R.string.pieces_edit),
                        tint = colors.mutedText,
                        modifier = Modifier.size(spacing.iconMd)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            AmountText(
                amount = summary.remaining,
                tone = if (summary.remaining >= 0L) AmountTone.Income else AmountTone.Expense,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(
                    id = R.string.pieces_spent_of_allocated,
                    formatRupiah(summary.spent),
                    formatRupiah(summary.allocated)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedText
            )
        }

        Spacer(modifier = Modifier.height(spacing.sm))
        ProgressBarPiece(progress = summary.progressRatio)

        Spacer(modifier = Modifier.height(spacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            categoryNames.forEach { catName ->
                PiecerChip(
                    label = catName,
                    selected = false,
                    onClick = onEditCategories
                )
            }
            PiecerChip(
                label = stringResource(id = R.string.pieces_manage_categories),
                selected = true,
                onClick = onEditCategories
            )
        }
    }
}

@Composable
private fun PieceFormDialog(
    state: PieceFormState,
    onNameChange: (String) -> Unit,
    onPercentChange: (String) -> Unit,
    onColorSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colorOptions = listOf("ink", "mint", "amber", "coral", "stone")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    id = if (state.editingPiece != null) R.string.pieces_edit else R.string.pieces_add
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text(text = stringResource(id = R.string.pieces_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.percentInput,
                    onValueChange = onPercentChange,
                    label = { Text(text = stringResource(id = R.string.pieces_percent_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    colorOptions.forEach { key ->
                        PiecerChip(
                            label = key,
                            selected = state.colorKey == key,
                            onClick = { onColorSelect(key) }
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
}

@Composable
private fun PercentEditorDialog(
    summaries: List<PieceBalanceSummary>,
    state: PercentEditorState,
    onPercentChange: (Long, String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val currentTotal = state.percentsById.values.sumOf { it.toIntOrNull() ?: 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.pieces_edit_percents)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                summaries.forEach { summary ->
                    val valStr = state.percentsById[summary.piece.id] ?: summary.piece.percent.toString()
                    OutlinedTextField(
                        value = valStr,
                        onValueChange = { onPercentChange(summary.piece.id, it) },
                        label = { Text(text = "${summary.piece.name} (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    text = stringResource(id = R.string.pieces_total_percent_label, currentTotal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (currentTotal == 100) PiecerTheme.colors.mint else PiecerTheme.colors.coral
                )
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
}

@Composable
private fun CategoryMappingDialog(
    pieceName: String,
    pieceId: Long,
    categories: List<CategoryEntity>,
    categoryToPieceId: Map<Long, Long>,
    onToggle: (Long, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.pieces_categories_for, pieceName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                categories.forEach { category ->
                    val checked = categoryToPieceId[category.id] == pieceId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(category.id, !checked) }
                            .padding(vertical = spacing.xxs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onToggle(category.id, it) }
                        )
                        Spacer(modifier = Modifier.width(spacing.xs))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.tx_close))
            }
        }
    )
}
