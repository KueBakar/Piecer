package com.piecer.app.util.update

import android.content.Intent
import com.piecer.app.BuildConfig
import com.piecer.app.data.preferences.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UpdatePhase {
    data object Idle : UpdatePhase
    data object Checking : UpdatePhase
    data object UpToDate : UpdatePhase
    data class Available(val metadata: UpdateMetadata) : UpdatePhase
    data class Downloading(
        val metadata: UpdateMetadata,
        val bytesRead: Long,
        val totalBytes: Long
    ) : UpdatePhase

    data class Verifying(val metadata: UpdateMetadata) : UpdatePhase
    data class NeedsUnknownSourcesPermission(
        val metadata: UpdateMetadata,
        val apkPath: String
    ) : UpdatePhase

    data class Installing(val metadata: UpdateMetadata) : UpdatePhase
    data object Installed : UpdatePhase
    data class Error(val message: String, val metadata: UpdateMetadata? = null) : UpdatePhase
}

data class UpdateUiState(
    val isFeatureEnabled: Boolean = BuildConfig.UPDATE_ENABLED,
    val phase: UpdatePhase = UpdatePhase.Idle,
    val availableUpdate: UpdateMetadata? = null,
    val showUpdateDialog: Boolean = false,
    val pendingConfirmIntent: Intent? = null
)

@Singleton
class UpdateManager @Inject constructor(
    private val updateChecker: UpdateChecker,
    private val apkDownloader: ApkDownloader,
    private val apkVerifier: ApkVerifier,
    private val apkInstaller: ApkInstaller,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var downloadJob: Job? = null
    private var activeMetadata: UpdateMetadata? = null

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    init {
        scope.launch {
            UpdateInstallEvents.events.collect { event ->
                when (event) {
                    is InstallStatusEvent.PendingUserAction -> {
                        _state.update { it.copy(pendingConfirmIntent = event.confirmIntent) }
                    }

                    is InstallStatusEvent.Success -> {
                        apkDownloader.clearCachedApks()
                        _state.update {
                            it.copy(
                                phase = UpdatePhase.Installed,
                                availableUpdate = null,
                                showUpdateDialog = false,
                                pendingConfirmIntent = null
                            )
                        }
                    }

                    is InstallStatusEvent.Cancelled -> {
                        apkDownloader.clearCachedApks()
                        val meta = activeMetadata
                        _state.update {
                            it.copy(
                                phase = if (meta != null) UpdatePhase.Available(meta) else UpdatePhase.Idle,
                                pendingConfirmIntent = null
                            )
                        }
                    }

                    is InstallStatusEvent.Failure -> {
                        apkDownloader.clearCachedApks()
                        _state.update {
                            it.copy(
                                phase = UpdatePhase.Error(event.message, activeMetadata),
                                pendingConfirmIntent = null
                            )
                        }
                    }
                }
            }
        }
    }

    fun consumePendingConfirmIntent() {
        _state.update { it.copy(pendingConfirmIntent = null) }
    }

    fun openUpdateDialog() {
        val available = _state.value.availableUpdate ?: return
        _state.update {
            it.copy(
                showUpdateDialog = true,
                phase = if (it.phase is UpdatePhase.Idle || it.phase is UpdatePhase.UpToDate) {
                    UpdatePhase.Available(available)
                } else {
                    it.phase
                }
            )
        }
    }

    fun dismissUpdateDialog() {
        val currentPhase = _state.value.phase
        if (currentPhase is UpdatePhase.Downloading || currentPhase is UpdatePhase.Verifying) {
            return
        }
        _state.update { it.copy(showUpdateDialog = false) }
    }

    fun checkInBackgroundIfDue() {
        if (!BuildConfig.UPDATE_ENABLED) return
        scope.launch {
            val settings = settingsRepository.getSettings()
            if (!settings.autoUpdateEnabled) return@launch
            val now = System.currentTimeMillis()
            if (now - settings.lastUpdateCheckMillis < CHECK_INTERVAL_MS) return@launch
            performCheck(showDialogOnAvailable = false)
        }
    }

    fun checkManually() {
        if (!BuildConfig.UPDATE_ENABLED) {
            _state.update {
                it.copy(phase = UpdatePhase.Error("Pemeriksaan pembaruan hanya aktif pada build rilis."))
            }
            return
        }
        scope.launch {
            performCheck(showDialogOnAvailable = true)
        }
    }

    private suspend fun performCheck(showDialogOnAvailable: Boolean) {
        _state.update { it.copy(phase = UpdatePhase.Checking) }
        val now = System.currentTimeMillis()
        when (val result = updateChecker.checkForUpdate()) {
            is UpdateCheckResult.UpdateAvailable -> {
                activeMetadata = result.metadata
                settingsRepository.recordUpdateCheck(
                    timestampMillis = now,
                    status = "Tersedia versi ${result.metadata.versionName}"
                )
                _state.update {
                    it.copy(
                        phase = UpdatePhase.Available(result.metadata),
                        availableUpdate = result.metadata,
                        showUpdateDialog = showDialogOnAvailable
                    )
                }
            }

            is UpdateCheckResult.UpToDate -> {
                settingsRepository.recordUpdateCheck(
                    timestampMillis = now,
                    status = "Sudah versi terbaru"
                )
                _state.update {
                    it.copy(
                        phase = UpdatePhase.UpToDate,
                        availableUpdate = null,
                        showUpdateDialog = false
                    )
                }
            }

            is UpdateCheckResult.Incompatible -> {
                val msg = "Versi terbaru memerlukan Android API ${result.minSdk} atau lebih baru."
                settingsRepository.recordUpdateCheck(timestampMillis = now, status = msg)
                _state.update {
                    it.copy(phase = UpdatePhase.Error(msg))
                }
            }

            is UpdateCheckResult.Error -> {
                settingsRepository.recordUpdateCheck(timestampMillis = now, status = result.message)
                _state.update {
                    it.copy(phase = UpdatePhase.Error(result.message))
                }
            }
        }
    }

    fun startDownload(wifiOnly: Boolean) {
        val metadata = _state.value.availableUpdate ?: activeMetadata ?: return
        activeMetadata = metadata
        downloadJob?.cancel()
        downloadJob = scope.launch {
            apkDownloader.download(metadata, wifiOnly).collect { progress ->
                when (progress) {
                    is DownloadProgress.InProgress -> {
                        _state.update {
                            it.copy(
                                phase = UpdatePhase.Downloading(
                                    metadata = metadata,
                                    bytesRead = progress.bytesRead,
                                    totalBytes = progress.totalBytes
                                ),
                                showUpdateDialog = true
                            )
                        }
                    }

                    is DownloadProgress.Completed -> {
                        verifyAndInstall(metadata, File(progress.filePath))
                    }

                    is DownloadProgress.Cancelled -> {
                        _state.update {
                            it.copy(phase = UpdatePhase.Available(metadata))
                        }
                    }

                    is DownloadProgress.Failed -> {
                        _state.update {
                            it.copy(phase = UpdatePhase.Error(progress.message, metadata))
                        }
                    }
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        apkDownloader.clearCachedApks()
        val meta = activeMetadata
        _state.update {
            it.copy(
                phase = if (meta != null) UpdatePhase.Available(meta) else UpdatePhase.Idle
            )
        }
    }

    fun createUnknownSourcesIntent(): Intent = apkInstaller.createUnknownSourcesSettingsIntent()

    fun retryInstallAfterPermission() {
        val phase = _state.value.phase as? UpdatePhase.NeedsUnknownSourcesPermission ?: return
        scope.launch {
            verifyAndInstall(phase.metadata, File(phase.apkPath))
        }
    }

    private suspend fun verifyAndInstall(metadata: UpdateMetadata, apkFile: File) {
        _state.update { it.copy(phase = UpdatePhase.Verifying(metadata)) }
        when (val decision = apkVerifier.verifyDownloadedApk(apkFile, metadata)) {
            is InstallDecision.Allowed -> {
                if (!apkInstaller.canRequestPackageInstalls()) {
                    _state.update {
                        it.copy(
                            phase = UpdatePhase.NeedsUnknownSourcesPermission(
                                metadata = metadata,
                                apkPath = apkFile.absolutePath
                            )
                        )
                    }
                    return
                }
                _state.update { it.copy(phase = UpdatePhase.Installing(metadata)) }
                val installResult = apkInstaller.installApk(apkFile)
                if (installResult.isFailure) {
                    apkDownloader.clearCachedApks()
                    _state.update {
                        it.copy(
                            phase = UpdatePhase.Error(
                                installResult.exceptionOrNull()?.message
                                    ?: "Gagal memulai pemasangan APK.",
                                metadata
                            )
                        )
                    }
                }
            }

            is InstallDecision.Rejected -> {
                apkDownloader.clearCachedApks()
                val msg = when (decision.reason) {
                    InstallRejectReason.SIZE_MISMATCH -> "Ukuran berkas APK tidak sesuai dengan metadata."
                    InstallRejectReason.SHA256_MISMATCH -> "Verifikasi SHA-256 APK gagal. Berkas dihapus."
                    InstallRejectReason.PACKAGE_MISMATCH -> "ID paket APK tidak cocok dengan PIECER."
                    InstallRejectReason.NOT_NEWER_VERSION -> "Versi APK tidak lebih baru dari aplikasi terpasang."
                    InstallRejectReason.MIN_SDK_UNSUPPORTED -> "Perangkat tidak memenuhi syarat versi Android minimum."
                    InstallRejectReason.SIGNATURE_MISMATCH -> "Tanda tangan APK tidak cocok dengan aplikasi terpasang."
                    InstallRejectReason.MISSING_SIGNATURE -> "Tanda tangan APK tidak ditemukan."
                    InstallRejectReason.INVALID_APK -> "Berkas APK rusak atau tidak valid."
                }
                _state.update { it.copy(phase = UpdatePhase.Error(msg, metadata)) }
            }
        }
    }
}
