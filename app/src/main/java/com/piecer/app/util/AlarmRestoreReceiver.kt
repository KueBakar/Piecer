package com.piecer.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmRestoreReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmRescheduler: AlarmRescheduler

    override fun onReceive(context: Context, intent: Intent?) {
        if (!ReminderScheduleCalculator.shouldRestoreAlarms(intent?.action)) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                alarmRescheduler.rescheduleAll()
            } catch (_: Throwable) {
                // Jangan biarkan penerima melempar exception di latar belakang
            } finally {
                pendingResult.finish()
            }
        }
    }
}
