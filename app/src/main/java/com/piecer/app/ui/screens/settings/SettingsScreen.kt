package com.piecer.app.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.BuildConfig
import com.piecer.app.R
import com.piecer.app.util.update.UpdatePhase
import java.text.DateFormat
import java.util.Date
import com.piecer.app.data.preferences.ThemeMode
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.components.PiecerChip
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.DateUtils
import com.piecer.app.util.FinancialCycleHelper
import com.piecer.app.util.PinHasher
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onPinConfigured: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenCrashReport: () -> Unit = {},
    onReplaySplash: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showCycleDayDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showCsvRangeDialog by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val currentCycle = FinancialCycleHelper.currentPeriod(now, settings.cycleStartDay)
    var csvStartMillis by remember { mutableLongStateOf(currentCycle.startMillis) }
    var csvEndMillis by remember { mutableLongStateOf(currentCycle.endMillis) }

    val backupSuccessMsg = stringResource(id = R.string.settings_backup_success)
    val backupFailMsg = stringResource(id = R.string.settings_backup_failed)
    val restoreInvalidMsg = stringResource(id = R.string.settings_restore_invalid)
    val restoreSuccessMsg = stringResource(id = R.string.settings_restore_success)
    val restoreFailMsg = stringResource(id = R.string.settings_restore_failed)
    val csvSuccessMsg = stringResource(id = R.string.settings_csv_success)
    val csvFailMsg = stringResource(id = R.string.settings_csv_failed)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setDailyReminder(
                enabled = true,
                hour = settings.dailyReminderHour,
                minute = settings.dailyReminderMinute
            )
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToUri(uri, backupSuccessMsg, backupFailMsg)
        }
    }

    val openRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.prepareRestoreFromUri(uri, restoreInvalidMsg)
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsvToUri(
                uri = uri,
                startMillis = csvStartMillis,
                endMillis = csvEndMillis,
                successMessage = csvSuccessMsg,
                failMessage = csvFailMsg
            )
        }
    }

    val canUseBiometric = remember(context) {
        try {
            val manager = BiometricManager.from(context)
            manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Throwable) {
            false
        }
    }

    LaunchedEffect(uiState.statusMessage, uiState.errorMessage) {
        val msg = uiState.statusMessage ?: uiState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_title),
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
                SectionHeader(text = stringResource(id = R.string.settings_section_appearance))
            }

            item {
                PiecerCard(variant = PiecerCardVariant.Light) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Text(
                            text = stringResource(id = R.string.settings_theme_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onCardLight
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            PiecerChip(
                                label = stringResource(id = R.string.settings_theme_light),
                                selected = settings.themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                            )
                            PiecerChip(
                                label = stringResource(id = R.string.settings_theme_dark),
                                selected = settings.themeMode == ThemeMode.DARK,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                            )
                            PiecerChip(
                                label = stringResource(id = R.string.settings_theme_system),
                                selected = settings.themeMode == ThemeMode.SYSTEM,
                                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                            )
                        }
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_reduce_motion),
                            subtitle = stringResource(id = R.string.settings_reduce_motion_desc),
                            checked = settings.reduceMotion,
                            onCheckedChange = viewModel::setReduceMotion
                        )
                    }
                }
            }

            item {
                SectionHeader(text = stringResource(id = R.string.settings_section_finance))
            }

            item {
                PiecerCard(variant = PiecerCardVariant.Light) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCycleDayDialog = true }
                                .padding(vertical = spacing.xs),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.settings_cycle_day_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.onCardLight
                                )
                                Text(
                                    text = stringResource(id = R.string.settings_cycle_day_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.mutedText
                                )
                            }
                            Spacer(modifier = Modifier.width(spacing.sm))
                            Text(
                                text = stringResource(
                                    id = R.string.settings_cycle_day_value,
                                    settings.cycleStartDay
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.primary
                            )
                        }

                        HorizontalDivider(color = colorScheme.outlineVariant)

                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_reminder_title),
                            subtitle = stringResource(id = R.string.settings_reminder_desc),
                            checked = settings.dailyReminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!granted) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        return@SettingsSwitchRow
                                    }
                                }
                                viewModel.setDailyReminder(
                                    enabled = enabled,
                                    hour = settings.dailyReminderHour,
                                    minute = settings.dailyReminderMinute
                                )
                            }
                        )

                        if (settings.dailyReminderEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTimePickerDialog = true }
                                    .padding(vertical = spacing.xs),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.settings_reminder_time_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onCardLight
                                )
                                Text(
                                    text = String.format(
                                        Locale.getDefault(),
                                        "%02d:%02d",
                                        settings.dailyReminderHour,
                                        settings.dailyReminderMinute
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(text = stringResource(id = R.string.settings_section_security))
            }

            item {
                PiecerCard(variant = PiecerCardVariant.Light) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_pin_title),
                            subtitle = stringResource(id = R.string.settings_pin_desc),
                            checked = settings.isPinEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    showPinDialog = true
                                } else {
                                    viewModel.disablePin()
                                }
                            }
                        )

                        if (settings.isPinEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPinDialog = true }
                                    .padding(vertical = spacing.xs),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.settings_pin_change),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.primary
                                )
                            }

                            if (canUseBiometric) {
                                HorizontalDivider(color = colorScheme.outlineVariant)
                                SettingsSwitchRow(
                                    title = stringResource(id = R.string.settings_biometric_title),
                                    subtitle = stringResource(id = R.string.settings_biometric_desc),
                                    checked = settings.biometricEnabled,
                                    onCheckedChange = viewModel::setBiometricEnabled
                                )
                            }
                        }

                        HorizontalDivider(color = colorScheme.outlineVariant)

                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_block_screenshots_title),
                            subtitle = stringResource(id = R.string.settings_block_screenshots_desc),
                            checked = settings.blockScreenshots,
                            onCheckedChange = viewModel::setBlockScreenshots
                        )
                    }
                }
            }

            item {
                SectionHeader(text = stringResource(id = R.string.settings_section_backup))
            }

            item {
                PiecerCard(variant = PiecerCardVariant.Light) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        SettingsActionRow(
                            title = stringResource(id = R.string.settings_backup_title),
                            subtitle = stringResource(id = R.string.settings_backup_desc),
                            onClick = {
                                createBackupLauncher.launch(viewModel.defaultBackupFileName())
                            }
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        SettingsActionRow(
                            title = stringResource(id = R.string.settings_restore_title),
                            subtitle = stringResource(id = R.string.settings_restore_desc),
                            onClick = {
                                openRestoreLauncher.launch(arrayOf("application/json", "*/*"))
                            }
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        SettingsActionRow(
                            title = stringResource(id = R.string.settings_csv_title),
                            subtitle = stringResource(id = R.string.settings_csv_desc),
                            onClick = { showCsvRangeDialog = true }
                        )
                    }
                }
            }

            item {
                SectionHeader(text = stringResource(id = R.string.settings_section_update))
            }

            item {
                val isChecking = updateState.phase is UpdatePhase.Checking
                val lastCheckText = if (settings.lastUpdateCheckMillis > 0L) {
                    val timeText = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(settings.lastUpdateCheckMillis))
                    stringResource(
                        id = R.string.settings_update_last_check_fmt,
                        timeText,
                        settings.lastUpdateCheckStatus.ifBlank { "-" }
                    )
                } else {
                    stringResource(id = R.string.settings_update_last_check_never)
                }

                PiecerCard(variant = PiecerCardVariant.Light) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Text(
                            text = stringResource(
                                id = R.string.settings_update_current_version,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = lastCheckText,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        if (!updateState.isFeatureEnabled) {
                            Text(
                                text = stringResource(id = R.string.settings_update_disabled_build),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        val updateError = (updateState.phase as? UpdatePhase.Error)?.message
                        if (!updateError.isNullOrBlank() && !updateState.showUpdateDialog) {
                            Text(
                                text = updateError,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.error
                            )
                        }
                        OutlinedButton(
                            onClick = viewModel::checkForUpdatesManually,
                            enabled = updateState.isFeatureEnabled && !isChecking,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(
                                    id = if (isChecking) {
                                        R.string.settings_update_checking
                                    } else {
                                        R.string.settings_update_check_button
                                    }
                                )
                            )
                        }
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_update_auto_title),
                            subtitle = stringResource(id = R.string.settings_update_auto_desc),
                            checked = settings.autoUpdateEnabled,
                            onCheckedChange = viewModel::setAutoUpdateEnabled
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_update_wifi_only_title),
                            subtitle = stringResource(id = R.string.settings_update_wifi_only_desc),
                            checked = settings.updateWifiOnly,
                            onCheckedChange = viewModel::setUpdateWifiOnly
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        SettingsActionRow(
                            title = stringResource(id = R.string.settings_about_title),
                            subtitle = stringResource(id = R.string.settings_about_desc),
                            onClick = onOpenAbout
                        )
                    }
                }
            }

            if (BuildConfig.CRASH_DIAGNOSTICS_ENABLED) {
                item {
                    SectionHeader(text = stringResource(id = R.string.settings_section_debug))
                }

                item {
                    PiecerCard(variant = PiecerCardVariant.Light) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            SettingsActionRow(
                                title = stringResource(id = R.string.settings_crash_report_title),
                                subtitle = stringResource(id = R.string.settings_crash_report_desc),
                                onClick = onOpenCrashReport
                            )
                            HorizontalDivider(color = colorScheme.outlineVariant)
                            SettingsActionRow(
                                title = stringResource(id = R.string.settings_replay_splash_title),
                                subtitle = stringResource(id = R.string.settings_replay_splash_desc),
                                onClick = onReplaySplash
                            )
                            HorizontalDivider(color = colorScheme.outlineVariant)
                            SettingsActionRow(
                                title = stringResource(id = R.string.settings_trigger_test_crash_title),
                                subtitle = stringResource(id = R.string.settings_trigger_test_crash_desc),
                                onClick = {
                                    throw IllegalStateException(
                                        "Uji crash dari Pengaturan > Diagnostik (${Thread.currentThread().name})"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCycleDayDialog) {
        AlertDialog(
            onDismissRequest = { showCycleDayDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_cycle_day_title)) },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.height(220.dp)
                ) {
                    items((1..28).toList()) { day ->
                        PiecerChip(
                            label = day.toString(),
                            selected = settings.cycleStartDay == day,
                            onClick = {
                                viewModel.setCycleStartDay(day)
                                showCycleDayDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCycleDayDialog = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    if (showTimePickerDialog) {
        val timeState = rememberTimePickerState(
            initialHour = settings.dailyReminderHour,
            initialMinute = settings.dailyReminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_reminder_time_label)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setDailyReminder(
                            enabled = true,
                            hour = timeState.hour,
                            minute = timeState.minute
                        )
                        showTimePickerDialog = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onSave = { pin ->
                viewModel.savePin(pin)
                onPinConfigured()
                showPinDialog = false
            }
        )
    }

    uiState.pendingRestoreBackup?.let { backup ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRestoreDialog,
            title = { Text(text = stringResource(id = R.string.settings_restore_confirm_title)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.settings_restore_confirm_message,
                        backup.transactions.size,
                        backup.wallets.size,
                        backup.goals.size
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmRestore(restoreSuccessMsg, restoreFailMsg) }
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_restore_confirm_action),
                        color = colors.coral
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRestoreDialog) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    if (showCsvRangeDialog) {
        CsvRangeDialog(
            startMillis = csvStartMillis,
            endMillis = csvEndMillis,
            onStartChange = { csvStartMillis = it },
            onEndChange = { csvEndMillis = it },
            onDismiss = { showCsvRangeDialog = false },
            onExport = {
                showCsvRangeDialog = false
                createCsvLauncher.launch(viewModel.defaultCsvFileName())
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = PiecerTheme.spacing.xs)
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = PiecerTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
        Spacer(modifier = Modifier.width(PiecerTheme.spacing.sm))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = PiecerTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = PiecerTheme.spacing.xs)
    ) {
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

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.settings_pin_setup_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PiecerTheme.spacing.sm)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) { pin = it; errorRes = null } },
                    label = { Text(text = stringResource(id = R.string.settings_pin_input_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) { confirmPin = it; errorRes = null } },
                    label = { Text(text = stringResource(id = R.string.settings_pin_confirm_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorRes?.let { res ->
                    Text(
                        text = stringResource(id = res),
                        style = MaterialTheme.typography.bodySmall,
                        color = PiecerTheme.colors.coral
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        !PinHasher.isValidPinFormat(pin) -> errorRes = R.string.settings_pin_error_length
                        pin != confirmPin -> errorRes = R.string.settings_pin_error_mismatch
                        else -> onSave(pin)
                    }
                }
            ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CsvRangeDialog(
    startMillis: Long,
    endMillis: Long,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.settings_csv_range_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PiecerTheme.spacing.sm)) {
                OutlinedButton(
                    onClick = { pickingStart = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.settings_csv_start_date,
                            DateUtils.formatDate(startMillis)
                        )
                    )
                }
                OutlinedButton(
                    onClick = { pickingEnd = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.settings_csv_end_date,
                            DateUtils.formatDate(endMillis)
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onExport) {
                Text(text = stringResource(id = R.string.settings_csv_export_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )

    if (pickingStart) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startMillis)
        DatePickerDialog(
            onDismissRequest = { pickingStart = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let {
                            onStartChange(DateUtils.startOfDayMillis(it))
                        }
                        pickingStart = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingStart = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (pickingEnd) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endMillis)
        DatePickerDialog(
            onDismissRequest = { pickingEnd = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let {
                            onEndChange(DateUtils.startOfDayMillis(it) + 86_399_999L)
                        }
                        pickingEnd = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingEnd = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
