package com.piecer.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.backup.BackupJsonCodec
import com.piecer.app.data.backup.BackupRepository
import com.piecer.app.data.backup.PiecerBackup
import com.piecer.app.data.preferences.AppSettings
import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.data.preferences.ThemeMode
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.data.repository.TransactionRepository
import com.piecer.app.data.repository.WalletRepository
import com.piecer.app.util.BreadcrumbLog
import com.piecer.app.util.CsvExporter
import com.piecer.app.util.DailyReminderScheduler
import com.piecer.app.util.PinHasher
import com.piecer.app.util.quickentry.QuickEntryLearningManager
import com.piecer.app.util.update.UpdateManager
import com.piecer.app.util.update.UpdateUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val pendingRestoreBackup: PiecerBackup? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val dailyReminderScheduler: DailyReminderScheduler,
    private val backupRepository: BackupRepository,
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    private val updateManager: UpdateManager,
    private val quickEntryLearningManager: QuickEntryLearningManager
) : ViewModel() {

    val updateState: StateFlow<UpdateUiState> = updateManager.state

    private val pendingRestore = MutableStateFlow<PiecerBackup?>(null)
    private val statusMsg = MutableStateFlow<String?>(null)
    private val errorMsg = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settingsFlow,
        pendingRestore,
        statusMsg,
        errorMsg
    ) { settings, restore, status, error ->
        SettingsUiState(
            settings = settings,
            pendingRestoreBackup = restore,
            statusMessage = status,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setCycleStartDay(day: Int) {
        viewModelScope.launch { settingsRepository.setCycleStartDay(day) }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReduceMotion(enabled) }
    }

    fun setQuickEntryLearningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setQuickEntryLearningEnabled(enabled)
            if (enabled) {
                quickEntryLearningManager.refreshHistoryIndex()
            }
        }
    }

    fun setVoiceEntryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVoiceEntryEnabled(enabled)
        }
    }

    fun resetQuickEntryLearning(doneMessage: String) {
        viewModelScope.launch {
            quickEntryLearningManager.resetLearning()
            statusMsg.value = doneMessage
        }
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoUpdateEnabled(enabled) }
    }

    fun setUpdateWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch { settingsRepository.setUpdateWifiOnly(wifiOnly) }
    }

    fun checkForUpdatesManually() {
        try {
            updateManager.checkManually()
        } catch (_: Throwable) {
            // Kegagalan sudah ditangani di UpdateManager
        }
    }

    fun setBlockScreenshots(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBlockScreenshots(enabled) }
    }

    fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setDailyReminder(enabled, hour, minute)
            if (enabled) {
                dailyReminderScheduler.schedule(hour, minute)
            } else {
                dailyReminderScheduler.cancel()
            }
        }
    }

    fun savePin(pin: String) {
        if (!PinHasher.isValidPinFormat(pin)) return
        viewModelScope.launch {
            val salt = PinHasher.generateSalt()
            val hash = PinHasher.hashPin(pin, salt)
            settingsRepository.setPin(hash, salt)
            BreadcrumbLog.record("SECURITY", "PIN enabled/updated", syncWrite = true)
        }
    }

    fun disablePin() {
        viewModelScope.launch {
            settingsRepository.clearPin()
            BreadcrumbLog.record("SECURITY", "PIN disabled", syncWrite = true)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
            BreadcrumbLog.record("SECURITY", "Biometric setting changed: enabled=$enabled", syncWrite = true)
        }
    }

    fun defaultBackupFileName(): String {
        val dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        return "piecer-backup-$dateStr.json"
    }

    fun defaultCsvFileName(): String {
        val dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        return "piecer-transaksi-$dateStr.csv"
    }

    fun exportBackupToUri(uri: Uri, successMessage: String, failMessage: String) {
        viewModelScope.launch {
            runCatching {
                val backup = backupRepository.createBackup()
                val json = BackupJsonCodec.encode(backup)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Tidak dapat membuka file tujuan")
                }
            }.onSuccess {
                statusMsg.value = successMessage
            }.onFailure {
                errorMsg.value = failMessage
            }
        }
    }

    fun prepareRestoreFromUri(uri: Uri, invalidMessage: String) {
        viewModelScope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error("Tidak dapat membaca file")
                }
                BackupJsonCodec.decode(json)
            }.onSuccess { backup ->
                pendingRestore.value = backup
            }.onFailure {
                errorMsg.value = invalidMessage
            }
        }
    }

    fun dismissRestoreDialog() {
        pendingRestore.value = null
    }

    fun confirmRestore(successMessage: String, failMessage: String) {
        val backup = pendingRestore.value ?: return
        viewModelScope.launch {
            runCatching {
                backupRepository.restoreBackup(backup)
                val s = backup.settings
                if (s.dailyReminderEnabled) {
                    dailyReminderScheduler.schedule(s.dailyReminderHour, s.dailyReminderMinute)
                } else {
                    dailyReminderScheduler.cancel()
                }
            }.onSuccess {
                pendingRestore.value = null
                statusMsg.value = successMessage
            }.onFailure {
                pendingRestore.value = null
                errorMsg.value = failMessage
            }
        }
    }

    fun exportCsvToUri(
        uri: Uri,
        startMillis: Long,
        endMillis: Long,
        successMessage: String,
        failMessage: String
    ) {
        viewModelScope.launch {
            runCatching {
                val txs = transactionRepository.observeAll().first()
                val wallets = walletRepository.observeAll().first().associateBy { it.id }
                val categories = categoryRepository.observeAll().first().associateBy { it.id }
                val csv = CsvExporter.exportTransactions(
                    transactions = txs,
                    wallets = wallets,
                    categories = categories,
                    startMillis = startMillis,
                    endMillis = endMillis
                )
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: error("Tidak dapat membuka file tujuan")
                }
            }.onSuccess {
                statusMsg.value = successMessage
            }.onFailure {
                errorMsg.value = failMessage
            }
        }
    }

    fun clearMessages() {
        statusMsg.value = null
        errorMsg.value = null
    }
}
