package com.piecer.app.ui.screens.wallets

import androidx.annotation.StringRes
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
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.R
import com.piecer.app.data.local.dao.WalletWithBalance
import com.piecer.app.data.local.entity.WalletType
import com.piecer.app.ui.components.AmountText
import com.piecer.app.ui.components.AmountTone
import com.piecer.app.ui.components.PiecerButton
import com.piecer.app.ui.components.PiecerButtonVariant
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.components.PiecerChip
import com.piecer.app.ui.components.PiecerIconButton
import com.piecer.app.ui.components.formatRupiah
import com.piecer.app.ui.theme.PiecerTheme

private val walletIconKeys = listOf(
    "wallet",
    "account_balance",
    "smartphone",
    "credit_card",
    "savings",
    "payments"
)

private val walletColorKeys = listOf(
    "ink",
    "graphite",
    "stone",
    "mint",
    "amber",
    "coral"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsScreen(
    viewModel: WalletsViewModel,
    onBack: () -> Unit,
    onTransferClick: () -> Unit,
    onWalletSaved: () -> Unit,
    onWalletDeleted: () -> Unit,
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
                        text = stringResource(id = R.string.wallets_title),
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
                        contentDescription = stringResource(id = R.string.wallets_add),
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
                        text = stringResource(id = R.string.wallets_total_balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    AmountText(
                        amount = uiState.totalBalance,
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = stringResource(
                            id = R.string.wallets_count_label,
                            uiState.wallets.size
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(spacing.lg))
                    PiecerButton(
                        text = stringResource(id = R.string.wallets_quick_transfer),
                        onClick = onTransferClick,
                        variant = PiecerButtonVariant.Secondary,
                        leadingIcon = Icons.Rounded.SwapHoriz,
                        enabled = uiState.wallets.size >= 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            items(
                items = uiState.wallets,
                key = { it.walletId }
            ) { wallet ->
                WalletItemCard(
                    wallet = wallet,
                    canDelete = uiState.canDeleteWallet,
                    onEdit = { viewModel.openEditForm(wallet) },
                    onDelete = { viewModel.requestDelete(wallet) }
                )
            }
        }
    }

    if (uiState.form.isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::closeForm,
            sheetState = sheetState,
            containerColor = colorScheme.surface
        ) {
            WalletFormSheetContent(
                form = uiState.form,
                onNameChange = viewModel::onNameChange,
                onTypeSelect = viewModel::onTypeSelect,
                onInitialBalanceChange = viewModel::onInitialBalanceChange,
                onIconSelect = viewModel::onIconSelect,
                onColorSelect = viewModel::onColorSelect,
                onSave = { viewModel.saveWallet(onWalletSaved) },
                onCancel = viewModel::closeForm
            )
        }
    }

    uiState.pendingDeleteWallet?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = {
                Text(text = stringResource(id = R.string.wallets_delete_title, target.name))
            },
            text = {
                Text(text = stringResource(id = R.string.wallets_delete_message))
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete(onWalletDeleted) }
                ) {
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
private fun WalletItemCard(
    wallet: WalletWithBalance,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val badgeColor = resolveWalletColor(wallet.colorKey)

    PiecerCard(
        modifier = Modifier.fillMaxWidth(),
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
                    .background(badgeColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = resolveWalletIcon(wallet.iconKey),
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(spacing.iconMd)
                )
            }
            Spacer(modifier = Modifier.width(spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wallet.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onCardLight
                )
                Text(
                    text = stringResource(id = walletTypeLabelRes(wallet.type)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                AmountText(
                    amount = wallet.currentBalance,
                    tone = if (wallet.currentBalance < 0L) AmountTone.Expense else AmountTone.Neutral,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(
                        id = R.string.wallets_initial_balance_sub,
                        formatRupiah(wallet.initialBalance)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText
                )
            }
        }
        Spacer(modifier = Modifier.height(spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(id = R.string.wallets_edit),
                    tint = colors.mutedText
                )
            }
            IconButton(
                onClick = onDelete,
                enabled = canDelete
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(id = R.string.history_delete),
                    tint = if (canDelete) colors.coral else colors.mutedText.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun WalletFormSheetContent(
    form: WalletFormState,
    onNameChange: (String) -> Unit,
    onTypeSelect: (WalletType) -> Unit,
    onInitialBalanceChange: (String) -> Unit,
    onIconSelect: (String) -> Unit,
    onColorSelect: (String) -> Unit,
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
                id = if (form.editingWalletId == null) R.string.wallets_add else R.string.wallets_edit
            ),
            style = MaterialTheme.typography.titleLarge,
            color = colorScheme.onSurface
        )

        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.wallets_name_label)) },
            placeholder = { Text(text = stringResource(id = R.string.wallets_name_placeholder)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            isError = form.error == WalletFormError.EMPTY_NAME
        )

        Text(
            text = stringResource(id = R.string.wallets_type_label),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurface
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            items(WalletType.entries) { walletType ->
                PiecerChip(
                    label = stringResource(id = walletTypeLabelRes(walletType)),
                    selected = form.type == walletType,
                    onClick = { onTypeSelect(walletType) }
                )
            }
        }

        OutlinedTextField(
            value = form.initialBalanceInput,
            onValueChange = onInitialBalanceChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.wallets_initial_balance_label)) },
            placeholder = { Text(text = stringResource(id = R.string.wallets_initial_balance_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium,
            isError = form.error == WalletFormError.INVALID_BALANCE
        )

        Text(
            text = stringResource(id = R.string.wallets_icon_label),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurface
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            items(walletIconKeys) { key ->
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
                            imageVector = resolveWalletIcon(key),
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
            items(walletColorKeys) { key ->
                val selected = form.colorKey == key
                val swatch = resolveWalletColor(key)
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
                WalletFormError.EMPTY_NAME -> R.string.wallets_error_name
                WalletFormError.INVALID_BALANCE -> R.string.wallets_error_balance
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

@StringRes
private fun walletTypeLabelRes(type: WalletType): Int = when (type) {
    WalletType.CASH -> R.string.wallet_type_cash
    WalletType.BANK -> R.string.wallet_type_bank
    WalletType.EWALLET -> R.string.wallet_type_ewallet
    WalletType.OTHER -> R.string.wallet_type_other
}

private fun resolveWalletIcon(key: String): ImageVector = when (key) {
    "account_balance" -> Icons.Rounded.AccountBalance
    "smartphone" -> Icons.Rounded.Smartphone
    "credit_card" -> Icons.Rounded.CreditCard
    "savings" -> Icons.Rounded.Savings
    "payments" -> Icons.Rounded.Payments
    else -> Icons.Rounded.AccountBalanceWallet
}

@Composable
private fun resolveWalletColor(key: String): Color {
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    return when (key) {
        "ink" -> colorScheme.onBackground
        "graphite" -> colorScheme.secondary
        "stone" -> colors.mutedText
        "mint" -> colors.mint
        "amber" -> colors.amber
        "coral" -> colors.coral
        else -> colorScheme.onBackground
    }
}
