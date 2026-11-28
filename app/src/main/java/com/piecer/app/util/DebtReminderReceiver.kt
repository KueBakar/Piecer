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
class DebtReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var debtReminderHelper: DebtReminderHelper

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DebtReminderHelper.ACTION_DEBT_REMINDER) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Jadwalkan dulu pengingat utang berikutnya sebelum menampilkan notifikasi
                debtReminderHelper.rescheduleAllUpcoming()
                NotificationChannelHelper.ensureAllChannels(appContext)
                debtReminderHelper.checkUpcomingReminders()
                settingsRepository.setLastReminderFiredAt(System.currentTimeMillis())
            } catch (_: Throwable) {
                // Jangan biarkan penerima melempar exception di latar belakang
            } finally {
                pendingResult.finish()
            }
        }
    }
}
