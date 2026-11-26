package com.piecer.app

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.preferences.AppSettings
import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.util.AppLockPolicy
import com.piecer.app.util.BreadcrumbLog
import com.piecer.app.util.PinHasher
import com.piecer.app.util.RecurringProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val recurringProcessor: RecurringProcessor
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    private val _isAppReady = MutableStateFlow(false)
    val isAppReady: StateFlow<Boolean> = _isAppReady.asStateFlow()

    private val _showSplash = MutableStateFlow(true)
    val showSplash: StateFlow<Boolean> = _showSplash.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private var hasInitializedLock = false
    private var lastBackgroundElapsedMs = 0L
    private var isBiometricPromptShowing = false
    private var consecutiveBiometricErrors = 0

    init {
        viewModelScope.launch {
            val initial = settingsRepository.settingsFlow.first()
            if (!hasInitializedLock) {
                hasInitializedLock = true
                if (initial.isPinEnabled) {
                    _isLocked.value = true
                    BreadcrumbLog.record("LOCK", "State=LOCKED reason=cold_start_pin_enabled", syncWrite = true)
                }
            }
            _isAppReady.value = true
            runCatching { recurringProcessor.processDueAndNotify() }
        }
    }

    fun onSplashFinished() {
        _showSplash.value = false
    }

    fun setBiometricPromptShowing(showing: Boolean) {
        isBiometricPromptShowing = showing
        if (showing) {
            lastBackgroundElapsedMs = 0L
        }
    }

    fun markSessionUnlocked() {
        hasInitializedLock = true
        lastBackgroundElapsedMs = 0L
        consecutiveBiometricErrors = 0
        _pinError.value = null
        _isLocked.value = false
        BreadcrumbLog.record("LOCK", "State=UNLOCKED reason=pin_configured", syncWrite = true)
    }

    fun onAppStart() {
        val backgroundedAt = lastBackgroundElapsedMs
        lastBackgroundElapsedMs = 0L
        if (AppLockPolicy.shouldLockOnForeground(
                isPinEnabled = settings.value.isPinEnabled,
                backgroundedAtElapsedMs = backgroundedAt,
                nowElapsedMs = SystemClock.elapsedRealtime(),
                isBiometricPromptShowing = isBiometricPromptShowing
            )
        ) {
            _isLocked.value = true
            _pinError.value = null
            BreadcrumbLog.record("LOCK", "State=LOCKED reason=foreground_timeout", syncWrite = true)
        }
    }

    fun onAppStop(isChangingConfigurations: Boolean) {
        if (AppLockPolicy.shouldRecordBackgroundTimestamp(
                isChangingConfigurations = isChangingConfigurations,
                isBiometricPromptShowing = isBiometricPromptShowing
            )
        ) {
            lastBackgroundElapsedMs = SystemClock.elapsedRealtime()
        }
    }

    fun submitPin(pin: String, wrongPinMessage: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings()
            val hash = current.pinHash
            val salt = current.pinSalt
            if (hash.isNullOrBlank() || salt.isNullOrBlank()) {
                _isLocked.value = false
                BreadcrumbLog.record("LOCK", "State=UNLOCKED reason=pin_missing_hash_fallback", syncWrite = true)
                return@launch
            }

            val now = System.currentTimeMillis()
            if (current.pinLockoutUntilMillis > now) {
                return@launch
            }

            if (PinHasher.verifyPin(pin, salt, hash)) {
                settingsRepository.updatePinAttempts(0, 0L)
                consecutiveBiometricErrors = 0
                _pinError.value = null
                _isLocked.value = false
                BreadcrumbLog.record("LOCK", "State=UNLOCKED reason=pin_verified", syncWrite = true)
            } else {
                val nextAttempts = current.failedPinAttempts + 1
                BreadcrumbLog.record("LOCK", "PIN verification failed (attempts=$nextAttempts)", syncWrite = true)
                if (nextAttempts >= PinHasher.MAX_FAILED_ATTEMPTS) {
                    settingsRepository.updatePinAttempts(
                        attempts = 0,
                        lockoutUntilMillis = now + PinHasher.LOCKOUT_DURATION_MILLIS
                    )
                    _pinError.value = null
                } else {
                    settingsRepository.updatePinAttempts(nextAttempts, 0L)
                    _pinError.value = wrongPinMessage
                }
            }
        }
    }

    fun onBiometricSuccess() {
        isBiometricPromptShowing = false
        lastBackgroundElapsedMs = 0L
        consecutiveBiometricErrors = 0
        viewModelScope.launch {
            settingsRepository.updatePinAttempts(0, 0L)
        }
        _pinError.value = null
        _isLocked.value = false
        BreadcrumbLog.record("LOCK", "State=UNLOCKED reason=biometric_verified", syncWrite = true)
    }

    fun recordBiometricNonFatalErrorAndShouldDisable(): Boolean {
        isBiometricPromptShowing = false
        consecutiveBiometricErrors += 1
        return consecutiveBiometricErrors >= 2
    }

    fun disableBiometricDueToFailure(reason: String, fallbackMessage: String) {
        isBiometricPromptShowing = false
        consecutiveBiometricErrors = 0
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(false)
        }
        _pinError.value = fallbackMessage
        BreadcrumbLog.record("BIOMETRIC", "Auto-disabled biometric: $reason", syncWrite = true)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
        }
    }
}
