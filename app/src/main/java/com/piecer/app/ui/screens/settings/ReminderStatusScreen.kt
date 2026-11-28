package com.piecer.app.ui.screens.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piecer.app.R
import com.piecer.app.ui.components.PiecerCard
import com.piecer.app.ui.components.PiecerCardVariant
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.util.NotificationChannelHelper
import com.piecer.app.util.ReminderScheduleCalculator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderStatusScreen(
    viewModel: ReminderStatusViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val spacing = PiecerTheme.spacing
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val openErrorText = stringResource(id = R.string.reminder_status_open_settings_error)

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val testRequestedAt by viewModel.testRequestedAtMillis.collectAsStateWithLifecycle()
    val testScheduledFor by viewModel.testScheduledForMillis.collectAsStateWithLifecycle()

    var refreshTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hasPermission = remember(refreshTick) {
        NotificationChannelHelper.hasNotificationPermission(context)
    }
    val dailyChannelEnabled = remember(refreshTick) {
        NotificationChannelHelper.isChannelEnabled(context, NotificationChannelHelper.CHANNEL_DAILY_REMINDER)
    }
    val recurringChannelEnabled = remember(refreshTick) {
        NotificationChannelHelper.isChannelEnabled(context, NotificationChannelHelper.CHANNEL_RECURRING_REMINDERS)
    }
    val debtChannelEnabled = remember(refreshTick) {
        NotificationChannelHelper.isChannelEnabled(context, NotificationChannelHelper.CHANNEL_DEBT_REMINDERS)
    }
    val isBatteryExempt = remember(refreshTick) {
        isIgnoringBatteryOptimizations(context)
    }

    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault())
    }
    val timeOnlyFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
    }
    val zoneId = remember { ZoneId.systemDefault() }

    val nextScheduleText = if (settings.dailyReminderEnabled) {
        val nextMillis = ReminderScheduleCalculator.nextDailyTriggerMillis(
            hour = settings.dailyReminderHour,
            minute = settings.dailyReminderMinute,
            nowMillis = System.currentTimeMillis(),
            zoneId = zoneId
        )
        Instant.ofEpochMilli(nextMillis).atZone(zoneId).format(dateTimeFormatter)
    } else {
        stringResource(id = R.string.reminder_status_next_schedule_off)
    }

    val lastFiredText = if (settings.lastReminderFiredAtMillis > 0L) {
        Instant.ofEpochMilli(settings.lastReminderFiredAtMillis).atZone(zoneId).format(dateTimeFormatter)
    } else {
        stringResource(id = R.string.reminder_status_last_fired_never)
    }

    val manufacturerText = remember {
        Build.MANUFACTURER.orEmpty().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }.ifBlank { "-" }
    }

    val showLaunchError: () -> Unit = {
        scope.launch { snackbarHostState.showSnackbar(openErrorText) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.reminder_status_title),
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
                    containerColor = colorScheme.background
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
                Text(
                    text = stringResource(id = R.string.reminder_status_section_device),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurfaceVariant
                )
            }

            item {
                PiecerCard(variant = PiecerCardVariant.Light) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        StatusRow(
                            label = stringResource(id = R.string.reminder_status_permission_label),
                            value = stringResource(
                                id = if (hasPermission) {
                                    R.string.reminder_status_permission_granted
                                } else {
                                    R.string.reminder_status_permission_denied
                                }
                            ),
                            isPositive = hasPermission
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        StatusRow(
                            label = stringResource(id = R.string.reminder_status_channel_daily_label),
                            value = stringResource(
                                id = if (dailyChannelEnabled) {
                                    R.string.reminder_status_channel_enabled
                                } else {
                                    R.string.reminder_status_channel_disabled
                                }
                            ),
                            isPositive = dailyChannelEnabled
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        StatusRow(
                            label = stringResource(id = R.string.reminder_status_channel_recurring_label),
                            value = stringResource(
                                id = if (recurringChannelEnabled) {
                                    R.string.reminder_status_channel_enabled
                                } else {
                                    R.string.reminder_status_channel_disabled
                                }
                            ),
                            isPositive = recurringChannelEnabled
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        StatusRow(
                            label = stringResource(id = R.string.reminder_status_channel_debt_label),
                            value = stringResource(
                                id = if (debtChannelEnabled) {
                                    R.string.reminder_status_channel_enabled
                                } else {
                                    R.string.reminder_status_channel_disabled
                                }
                            ),
                            isPositive = debtChannelEnabled
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        StatusRow(
                            label = stringResource(id = R.string.reminder_status_battery_label),
                            value = stringResource(
                                id = if (isBatteryExempt) {
                                    R.string.reminder_status_battery_exempt
                                } else {
                                    R.string.reminder_status_battery_restricted
                                }
                            ),
                            isPositive = isBatteryExempt
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        StatusRow(
                            label = stringResource(id = R.string.reminder_status_next_schedule_label),
                            value = nextScheduleText,
                            isPositive = settings.dailyReminderEnabled
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        StatusRow(
                            label = stringResource(id = R.string.reminder_status_last_fired_label),
                            value = lastFiredText,
                            isPositive = settings.lastReminderFiredAtMillis > 0L
                        )
                        HorizontalDivider(color = colorScheme.outlineVariant)
                        StatusRow(
                            label = stringResource(id = R.string.reminder_status_manufacturer_label),
                            value = manufacturerText,
                            isPositive = null
                        )
                    }
                }
            }

            item {
                PiecerCard(variant = PiecerCardVariant.Light) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Text(
                            text = stringResource(id = R.string.reminder_status_background_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                if (!openNotificationSettings(context)) showLaunchError()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(id = R.string.reminder_status_btn_notif_settings))
                        }
                        OutlinedButton(
                            onClick = {
                                if (!openBatterySettings(context)) showLaunchError()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(id = R.string.reminder_status_btn_battery_settings))
                        }
                        OutlinedButton(
                            onClick = {
                                if (!openManufacturerAutostartSettings(context)) showLaunchError()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(id = R.string.reminder_status_btn_autostart_settings))
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(id = R.string.reminder_status_test_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurfaceVariant
                )
            }

            item {
                PiecerCard(variant = PiecerCardVariant.Light) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Button(
                            onClick = viewModel::scheduleTestReminder,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(id = R.string.reminder_status_btn_test))
                        }

                        val requestedAt = testRequestedAt
                        val scheduledFor = testScheduledFor
                        if (requestedAt != null) {
                            if (settings.lastReminderFiredAtMillis >= requestedAt) {
                                val receivedTime = Instant.ofEpochMilli(settings.lastReminderFiredAtMillis)
                                    .atZone(zoneId)
                                    .format(timeOnlyFormatter)
                                Text(
                                    text = stringResource(
                                        id = R.string.reminder_status_test_received,
                                        receivedTime
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.mint,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else if (scheduledFor != null) {
                                val scheduledTime = Instant.ofEpochMilli(scheduledFor)
                                    .atZone(zoneId)
                                    .format(timeOnlyFormatter)
                                Text(
                                    text = stringResource(
                                        id = R.string.reminder_status_test_waiting,
                                        scheduledTime
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    isPositive: Boolean?
) {
    val colors = PiecerTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val valueColor = when (isPositive) {
        true -> colors.mint
        false -> colors.amber
        null -> colorScheme.onSurface
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return runCatching { pm.isIgnoringBatteryOptimizations(context.packageName) }.getOrDefault(false)
}

private fun openNotificationSettings(context: Context): Boolean {
    val notifIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (tryStartActivity(context, notifIntent)) return true
    return openAppDetailsSettings(context)
}

private fun openBatterySettings(context: Context): Boolean {
    val batteryIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (tryStartActivity(context, batteryIntent)) return true
    return openAppDetailsSettings(context)
}

private fun openManufacturerAutostartSettings(context: Context): Boolean {
    val manufacturer = Build.MANUFACTURER.orEmpty().lowercase(Locale.ROOT)
    val candidates = when {
        manufacturer.contains("vivo") -> listOf(
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
        )
        manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> listOf(
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
        )
        manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        )
        manufacturer.contains("huawei") || manufacturer.contains("honor") -> listOf(
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
        )
        manufacturer.contains("samsung") -> listOf(
            ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
            ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")
        )
        else -> emptyList()
    }

    for (component in candidates) {
        val intent = Intent().apply {
            this.component = component
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (tryStartActivity(context, intent)) return true
    }
    return openAppDetailsSettings(context)
}

private fun openAppDetailsSettings(context: Context): Boolean {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return tryStartActivity(context, intent)
}

private fun tryStartActivity(context: Context, intent: Intent): Boolean {
    return try {
        context.startActivity(intent)
        true
    } catch (_: Throwable) {
        false
    }
}
