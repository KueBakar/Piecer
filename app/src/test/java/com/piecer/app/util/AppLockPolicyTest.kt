package com.piecer.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockPolicyTest {

    @Test
    fun `tidak mengunci jika PIN nonaktif meskipun background lebih dari 30 detik`() {
        val shouldLock = AppLockPolicy.shouldLockOnForeground(
            isPinEnabled = false,
            backgroundedAtElapsedMs = 1_000L,
            nowElapsedMs = 60_000L,
            isBiometricPromptShowing = false
        )
        assertFalse(shouldLock)
    }

    @Test
    fun `tidak mengunci jika dialog biometrik sedang tampil`() {
        val shouldLock = AppLockPolicy.shouldLockOnForeground(
            isPinEnabled = true,
            backgroundedAtElapsedMs = 1_000L,
            nowElapsedMs = 60_000L,
            isBiometricPromptShowing = true
        )
        assertFalse(shouldLock)
    }

    @Test
    fun `tidak mengunci jika waktu background belum melewati 30 detik`() {
        val shouldLock = AppLockPolicy.shouldLockOnForeground(
            isPinEnabled = true,
            backgroundedAtElapsedMs = 10_000L,
            nowElapsedMs = 40_000L, // tepat 30_000ms
            isBiometricPromptShowing = false
        )
        assertFalse(shouldLock)
    }

    @Test
    fun `mengunci jika PIN aktif dan waktu background lebih dari 30 detik`() {
        val shouldLock = AppLockPolicy.shouldLockOnForeground(
            isPinEnabled = true,
            backgroundedAtElapsedMs = 10_000L,
            nowElapsedMs = 40_001L,
            isBiometricPromptShowing = false
        )
        assertTrue(shouldLock)
    }

    @Test
    fun `tidak mengunci jika tidak ada timestamp background`() {
        val shouldLock = AppLockPolicy.shouldLockOnForeground(
            isPinEnabled = true,
            backgroundedAtElapsedMs = 0L,
            nowElapsedMs = 60_000L,
            isBiometricPromptShowing = false
        )
        assertFalse(shouldLock)
    }

    @Test
    fun `tidak mencatat timestamp background saat dialog biometrik tampil atau rotasi`() {
        assertFalse(
            AppLockPolicy.shouldRecordBackgroundTimestamp(
                isChangingConfigurations = false,
                isBiometricPromptShowing = true
            )
        )
        assertFalse(
            AppLockPolicy.shouldRecordBackgroundTimestamp(
                isChangingConfigurations = true,
                isBiometricPromptShowing = false
            )
        )
        assertTrue(
            AppLockPolicy.shouldRecordBackgroundTimestamp(
                isChangingConfigurations = false,
                isBiometricPromptShowing = false
            )
        )
    }
}
