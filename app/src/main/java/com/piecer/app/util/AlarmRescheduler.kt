package com.piecer.app.util

import android.content.Context
import com.piecer.app.data.local.PiecerDatabase
import com.piecer.app.data.preferences.SettingsRepository
import com.piecer.app.widget.PiecerWidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRescheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val dailyReminderScheduler: DailyReminderScheduler,
    private val recurringReminderScheduler: RecurringReminderScheduler,
    private val debtReminderHelper: DebtReminderHelper,
    private val database: PiecerDatabase
) {
    suspend fun rescheduleAll() {
        NotificationChannelHelper.ensureAllChannels(context)

        val settings = settingsRepository.getSettings()
        if (settings.dailyReminderEnabled) {
            dailyReminderScheduler.schedule(
                settings.dailyReminderHour,
                settings.dailyReminderMinute
            )
        } else {
            dailyReminderScheduler.cancel()
        }

        val activeRecurring = database.recurringDao().getAllActive()
        if (activeRecurring.isNotEmpty()) {
            recurringReminderScheduler.scheduleNext()
        } else {
            recurringReminderScheduler.cancel()
        }

        debtReminderHelper.rescheduleAllUpcoming()
        PiecerWidgetUpdater.scheduleMidnightUpdate(context)
    }
}
