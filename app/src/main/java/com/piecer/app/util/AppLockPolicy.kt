package com.piecer.app.util

/**
 * Aturan penentuan apakah aplikasi harus dikunci saat kembali ke foreground
 * atau mencatat timestamp saat masuk background.
 */
object AppLockPolicy {
    const val BACKGROUND_LOCK_THRESHOLD_MS = 30_000L

    fun shouldRecordBackgroundTimestamp(
        isChangingConfigurations: Boolean,
        isBiometricPromptShowing: Boolean
    ): Boolean {
        return !isChangingConfigurations && !isBiometricPromptShowing
    }

    fun shouldLockOnForeground(
        isPinEnabled: Boolean,
        backgroundedAtElapsedMs: Long,
        nowElapsedMs: Long,
        isBiometricPromptShowing: Boolean
    ): Boolean {
        if (!isPinEnabled) return false
        if (isBiometricPromptShowing) return false
        if (backgroundedAtElapsedMs <= 0L) return false
        val elapsed = nowElapsedMs - backgroundedAtElapsedMs
        return elapsed > BACKGROUND_LOCK_THRESHOLD_MS
    }
}
