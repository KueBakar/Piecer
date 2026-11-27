package com.piecer.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PiecerWidgetMidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        PiecerWidgetUpdater.updateAllWidgets(context)
        PiecerWidgetUpdater.scheduleMidnightUpdate(context)
    }
}
