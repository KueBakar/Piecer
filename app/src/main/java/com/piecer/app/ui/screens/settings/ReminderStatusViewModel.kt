package com.piecer.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.preferences.AppSettings
import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.util.DailyReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ReminderStatusViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val dailyReminderScheduler: DailyReminderScheduler
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    private val _testRequestedAtMillis = MutableStateFlow<Long?>(null)
    val testRequestedAtMillis: StateFlow<Long?> = _testRequestedAtMillis.asStateFlow()

    private val _testScheduledForMillis = MutableStateFlow<Long?>(null)
    val testScheduledForMillis: StateFlow<Long?> = _testScheduledForMillis.asStateFlow()

    fun scheduleTestReminder() {
        val now = System.currentTimeMillis()
        _testRequestedAtMillis.value = now
        _testScheduledForMillis.value = dailyReminderScheduler.scheduleTestReminder()
    }
}
