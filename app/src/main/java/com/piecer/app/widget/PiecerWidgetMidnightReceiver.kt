package com.piecer.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PiecerWidgetMidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Jadwalkan dulu tengah malam berikutnya sebelum memperbarui widget
                PiecerWidgetUpdater.scheduleMidnightUpdate(appContext)
                PiecerWidgetUpdater.updateAllWidgets(appContext)
            } catch (_: Throwable) {
                // Jangan biarkan penerima melempar exception
            } finally {
                pendingResult.finish()
            }
        }
    }
}
