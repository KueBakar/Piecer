package com.piecer.app

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.preferences.AppSettings
import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.util.PinHasher
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
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private var hasInitializedLock = false
    private var lastBackgroundElapsedMs = 0L

    init {
        viewModelScope.launch {
            val initial = settingsRepository.settingsFlow.first()
            if (!hasInitializedLock) {
                hasInitializedLock = true
                if (initial.isPinEnabled) {
                    _isLocked.value = true
                }
            }
        }
    }

    fun onAppStart() {
        val backgroundedAt = lastBackgroundElapsedMs
        if (backgroundedAt > 0L) {
            lastBackgroundElapsedMs = 0L
            val elapsed = SystemClock.elapsedRealtime() - backgroundedAt
            if (elapsed > BACKGROUND_LOCK_THRESHOLD_MS && settings.value.isPinEnabled) {
                _isLocked.value = true
                _pinError.value = null
            }
        }
    }

    fun onAppStop(isChangingConfigurations: Boolean) {
        if (!isChangingConfigurations) {
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
                return@launch
            }

            val now = System.currentTimeMillis()
            if (current.pinLockoutUntilMillis > now) {
                return@launch
            }

            if (PinHasher.verifyPin(pin, salt, hash)) {
                settingsRepository.updatePinAttempts(0, 0L)
                _pinError.value = null
                _isLocked.value = false
            } else {
                val nextAttempts = current.failedPinAttempts + 1
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
        viewModelScope.launch {
            settingsRepository.updatePinAttempts(0, 0L)
        }
        _pinError.value = null
        _isLocked.value = false
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
        }
    }

    companion object {
        private const val BACKGROUND_LOCK_THRESHOLD_MS = 30_000L
    }
}
