package com.piecer.app.ui.screens.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Work
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.R
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.ui.components.EmptyState
import com.piecer.app.ui.components.PiecerButton
import com.piecer.app.ui.components.PiecerButtonVariant
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.components.PiecerIconButton
import com.piecer.app.ui.theme.PiecerTheme
import kotlinx.coroutines.launch

private val categoryIconKeys = listOf(
    "restaurant",
    "directions_car",
    "shopping_bag",
    "receipt_long",
    "movie",
    "medical_services",
    "school",
    "payments",
    "card_giftcard",
    "redeem",
    "home",
    "work",
    "flight",
    "fitness_center",
    "pets",
    "more_horiz"
)

private val categoryColorKeys = listOf(
    "ink",
    "graphite",
    "stone",
    "mint",
    "amber",
    "coral"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val savedMsg = stringResource(id = R.string.categories_saved_snackbar)
    val archivedMsg = stringResource(id = R.string.categories_archived_snackbar)
    val restoredMsg = stringResource(id = R.string.categories_restored_snackbar)
    val deletedMsg = stringResource(id = R.string.categories_deleted_snackbar)
    val inUseArchivedMsg = stringResource(id = R.string.categories_in_use_archived_snackbar)

    Scaffold(
        modifier = modifier,
        containerColor = colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.categories_title),
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
                        contentDescription = stringResource(id = R.string.categories_add),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val selectedIndex = if (uiState.selectedTab == CategoryType.EXPENSE) 0 else 1
            TabRow(
                selectedTabIndex = selectedIndex,
                containerColor = colorScheme.background,
                contentColor = colorScheme.primary
            ) {
                Tab(
                    selected = selectedIndex == 0,
                    onClick = { viewModel.selectTab(CategoryType.EXPENSE) },
                    text = { Text(text = stringResource(id = R.string.tx_type_expense)) }
                )
                Tab(
                    selected = selectedIndex == 1,
                    onClick = { viewModel.selectTab(CategoryType.INCOME) },
                    text = { Text(text = stringResource(id = R.string.tx_type_income)) }
                )
            }

            if (uiState.activeItems.isEmpty() && uiState.archivedItems.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Category,
                    title = stringResource(id = R.string.categories_empty_title),
                    description = stringResource(id = R.string.categories_empty_desc),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = spacing.screenPadding,
                        vertical = spacing.md
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    items(uiState.activeItems, key = { it.category.id }) { item ->
                        CategoryRowCard(
                            item = item,
                            onEdit = { viewModel.openEditForm(item.category) },
                            onArchive = {
                                viewModel.archiveCategory(item.category) {
                                    scope.launch { snackbarHostState.showSnackbar(archivedMsg) }
                                }
                            },
                            onDelete = {
                                viewModel.requestDelete(item.category) {
                                    scope.launch { snackbarHostState.showSnackbar(inUseArchivedMsg) }
                                }
                            }
                        )
                    }

                    if (uiState.archivedItems.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(spacing.md))
                            Text(
                                text = stringResource(id = R.string.categories_archived_section),
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.mutedText
                            )
                        }
                        items(uiState.archivedItems, key = { it.category.id }) { item ->
                            CategoryRowCard(
                                item = item,
                                onEdit = { viewModel.openEditForm(item.category) },
                                onRestore = {
                                    viewModel.restoreCategory(item.category) {
                                        scope.launch { snackbarHostState.showSnackbar(restoredMsg) }
                                    }
                                },
                                onDelete = {
                                    viewModel.requestDelete(item.category) {
                                        scope.launch { snackbarHostState.showSnackbar(inUseArchivedMsg) }
                                    }
                                }
                            )
                        }
                    }
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
            CategoryFormSheet(
                form = uiState.form,
                onNameChange = viewModel::onNameChange,
                onIconSelect = viewModel::onIconSelect,
                onColorSelect = viewModel::onColorSelect,
                onSave = {
                    viewModel.saveCategory {
                        scope.launch { snackbarHostState.showSnackbar(savedMsg) }
                    }
                },
                onDismiss = viewModel::dismissForm
            )
        }
    }

    uiState.pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text(text = stringResource(id = R.string.categories_delete_title)) },
            text = { Text(text = stringResource(id = R.string.categories_delete_confirm, target.name)) },
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
private fun CategoryRowCard(
    item: CategoryListItem,
    onEdit: () -> Unit,
    onArchive: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val category = item.category
    val badgeColor = resolveCategoryColor(category.colorKey)

    PiecerCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (category.isArchived) 0.72f else 1f),
        variant = PiecerCardVariant.Light,
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.iconButtonSize)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = resolveCategoryIcon(category.iconKey),
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(spacing.iconMd)
                )
            }
            Spacer(modifier = Modifier.width(spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onCardLight
                )
                val statusText = when {
                    category.isArchived -> stringResource(id = R.string.categories_status_archived)
                    item.isInUse -> stringResource(id = R.string.categories_status_in_use)
                    else -> stringResource(id = R.string.categories_status_unused)
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(id = R.string.action_edit),
                    tint = colors.mutedText,
                    modifier = Modifier.size(spacing.iconMd)
                )
            }
            if (onArchive != null) {
                IconButton(onClick = onArchive) {
                    Icon(
                        imageVector = Icons.Outlined.Archive,
                        contentDescription = stringResource(id = R.string.categories_archive),
                        tint = colors.mutedText,
                        modifier = Modifier.size(spacing.iconMd)
                    )
                }
            }
            if (onRestore != null) {
                IconButton(onClick = onRestore) {
                    Icon(
                        imageVector = Icons.Outlined.Unarchive,
                        contentDescription = stringResource(id = R.string.categories_restore),
                        tint = colors.mint,
                        modifier = Modifier.size(spacing.iconMd)
                    )
                }
            }
            if (!item.isInUse) {
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
private fun CategoryFormSheet(
    form: CategoryFormState,
    onNameChange: (String) -> Unit,
    onIconSelect: (String) -> Unit,
    onColorSelect: (String) -> Unit,
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
                id = if (form.editingId == null) R.string.categories_add else R.string.categories_edit
            ),
            style = MaterialTheme.typography.titleLarge,
            color = colorScheme.onSurface
        )

        OutlinedTextField(
            value = form.nameInput,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.categories_name_label)) },
            placeholder = { Text(text = stringResource(id = R.string.categories_name_placeholder)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            isError = form.error != null
        )

        Text(
            text = stringResource(id = R.string.wallets_icon_label),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurface
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            items(categoryIconKeys) { key ->
                val selected = form.iconKey == key
                Surface(
                    onClick = { onIconSelect(key) },
                    modifier = Modifier.size(spacing.iconButtonSize),
                    shape = CircleShape,
                    color = if (selected) colorScheme.primary else colorScheme.surfaceVariant,
                    contentColor = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = resolveCategoryIcon(key),
                            contentDescription = null,
                            modifier = Modifier.size(spacing.iconMd)
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(id = R.string.wallets_color_label),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurface
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            items(categoryColorKeys) { key ->
                val selected = form.colorKey == key
                val swatch = resolveCategoryColor(key)
                Surface(
                    modifier = Modifier
                        .size(spacing.iconButtonSize)
                        .clip(CircleShape)
                        .clickable { onColorSelect(key) },
                    shape = CircleShape,
                    color = swatch,
                    border = if (selected) {
                        BorderStroke(spacing.xxs + spacing.xxs, colorScheme.onSurface)
                    } else {
                        null
                    }
                ) {
                    if (selected) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colorScheme.surface,
                                modifier = Modifier.size(spacing.iconSm)
                            )
                        }
                    }
                }
            }
        }

        form.error?.let { err ->
            val msgRes = when (err) {
                CategoryFormError.EMPTY_NAME -> R.string.categories_error_empty
                CategoryFormError.DUPLICATE_NAME -> R.string.categories_error_duplicate
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
fun resolveCategoryColor(colorKey: String): Color {
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    return when (colorKey) {
        "mint" -> colors.mint
        "amber" -> colors.amber
        "coral" -> colors.coral
        "stone" -> colors.mutedText
        "graphite" -> colorScheme.secondary
        else -> colorScheme.primary
    }
}

fun resolveCategoryIcon(iconKey: String): ImageVector = when (iconKey) {
    "restaurant" -> Icons.Rounded.Restaurant
    "directions_car" -> Icons.Rounded.DirectionsCar
    "shopping_bag" -> Icons.Rounded.ShoppingBag
    "receipt_long" -> Icons.AutoMirrored.Rounded.ReceiptLong
    "movie" -> Icons.Rounded.Movie
    "medical_services" -> Icons.Rounded.MedicalServices
    "school" -> Icons.Rounded.School
    "payments" -> Icons.Rounded.Payments
    "card_giftcard" -> Icons.Rounded.CardGiftcard
    "redeem" -> Icons.Rounded.Redeem
    "home" -> Icons.Rounded.Home
    "work" -> Icons.Rounded.Work
    "flight" -> Icons.Rounded.Flight
    "fitness_center" -> Icons.Rounded.FitnessCenter
    "pets" -> Icons.Rounded.Pets
    "more_horiz" -> Icons.Rounded.MoreHoriz
    else -> Icons.Rounded.Category
}
