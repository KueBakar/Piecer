package com.piecer.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.piecer.app.data.preferences.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RecurringReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var recurringReminderScheduler: RecurringReminderScheduler

    @Inject
    lateinit var recurringProcessor: RecurringProcessor

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RecurringReminderScheduler.ACTION_RECURRING_CHECK) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Jadwalkan dulu pemeriksaan hari berikutnya sebelum memproses DB
                recurringReminderScheduler.scheduleNext()
                NotificationChannelHelper.ensureAllChannels(appContext)
                recurringProcessor.processDueAndNotify()
                settingsRepository.setLastReminderFiredAt(System.currentTimeMillis())
            } catch (_: Throwable) {
                // Jangan biarkan penerima melempar exception di latar belakang
            } finally {
                pendingResult.finish()
            }
        }
    }
}
