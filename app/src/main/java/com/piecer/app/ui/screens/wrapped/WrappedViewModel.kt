package com.piecer.app.ui.screens.wrapped

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.data.repository.CategoryRepository
import com.piecer.app.data.repository.TransactionRepository
import com.piecer.app.util.WrappedCalculator
import com.piecer.app.util.WrappedPeriodMode
import com.piecer.app.util.WrappedSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class WrappedUiState(
    val mode: WrappedPeriodMode = WrappedPeriodMode.MONTH,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val monthZeroBased: Int = Calendar.getInstance().get(Calendar.MONTH),
    val hideAmountsOnShare: Boolean = false,
    val reduceMotion: Boolean = false,
    val summary: WrappedSummary? = null
)

@HiltViewModel
class WrappedViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _mode = MutableStateFlow(WrappedPeriodMode.MONTH)
    private val _year = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _month = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _hideAmountsOnShare = MutableStateFlow(false)

    val uiState: StateFlow<WrappedUiState> = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        settingsRepository.settingsFlow,
        _mode,
        _year,
        _month,
        _hideAmountsOnShare
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val transactions = values[0] as List<com.piecer.app.data.local.entity.TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val categories = values[1] as List<com.piecer.app.data.local.entity.CategoryEntity>
        val settings = values[2] as com.piecer.app.data.preferences.AppSettings
        val mode = values[3] as WrappedPeriodMode
        val year = values[4] as Int
        val month = values[5] as Int
        val hideShare = values[6] as Boolean

        val label = formatPeriodLabel(mode, year, month)
        val summary = WrappedCalculator.calculate(
            transactions = transactions,
            categories = categories,
            mode = mode,
            year = year,
            monthZeroBased = month,
            cycleStartDay = settings.cycleStartDay,
            nowMillis = System.currentTimeMillis(),
            periodLabel = label
        )

        WrappedUiState(
            mode = mode,
            year = year,
            monthZeroBased = month,
            hideAmountsOnShare = hideShare,
            reduceMotion = settings.reduceMotion,
            summary = summary
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WrappedUiState()
        )

    fun setMode(mode: WrappedPeriodMode) {
        _mode.value = mode
    }

    fun previousPeriod() {
        if (_mode.value == WrappedPeriodMode.YEAR) {
            _year.value -= 1
        } else {
            if (_month.value == Calendar.JANUARY) {
                _month.value = Calendar.DECEMBER
                _year.value -= 1
            } else {
                _month.value -= 1
            }
        }
    }

    fun nextPeriod() {
        if (_mode.value == WrappedPeriodMode.YEAR) {
            _year.value += 1
        } else {
            if (_month.value == Calendar.DECEMBER) {
                _month.value = Calendar.JANUARY
                _year.value += 1
            } else {
                _month.value += 1
            }
        }
    }

    fun toggleHideAmountsOnShare() {
        _hideAmountsOnShare.value = !_hideAmountsOnShare.value
    }

    private fun formatPeriodLabel(mode: WrappedPeriodMode, year: Int, monthZeroBased: Int): String {
        return if (mode == WrappedPeriodMode.YEAR) {
            year.toString()
        } else {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthZeroBased)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(cal.time)
        }
    }
}
