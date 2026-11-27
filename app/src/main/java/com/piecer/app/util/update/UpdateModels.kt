package com.piecer.app.util.update

data class UpdateMetadata(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val minSdk: Int,
    val releaseNotes: String
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val metadata: UpdateMetadata) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Incompatible(val minSdk: Int) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

sealed interface DownloadProgress {
    data class InProgress(val bytesRead: Long, val totalBytes: Long) : DownloadProgress
    data class Completed(val filePath: String) : DownloadProgress
    data object Cancelled : DownloadProgress
    data class Failed(val message: String) : DownloadProgress
}

sealed interface InstallDecision {
    data object Allowed : InstallDecision
    data class Rejected(val reason: InstallRejectReason) : InstallDecision
}

enum class InstallRejectReason {
    PACKAGE_MISMATCH,
    NOT_NEWER_VERSION,
    MIN_SDK_UNSUPPORTED,
    SIGNATURE_MISMATCH,
    MISSING_SIGNATURE,
    SIZE_MISMATCH,
    SHA256_MISMATCH,
    INVALID_APK
}
