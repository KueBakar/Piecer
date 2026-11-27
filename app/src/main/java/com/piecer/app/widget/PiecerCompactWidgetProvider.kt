package com.piecer.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.piecer.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PiecerCompactWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        PiecerWidgetUpdater.scheduleMidnightUpdate(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        PiecerWidgetUpdater.scheduleMidnightUpdate(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = PiecerWidgetUpdater.loadWidgetData(context)
                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_piecer_compact)
                    views.setTextViewText(R.id.widget_allowance_amount, data.dailyAllowanceText)
                    views.setOnClickPendingIntent(
                        R.id.widget_root,
                        PiecerWidgetUpdater.createOpenAppPendingIntent(context, appWidgetId)
                    )
                    views.setOnClickPendingIntent(
                        R.id.widget_add_button,
                        PiecerWidgetUpdater.createAddTransactionPendingIntent(context, appWidgetId + 1000)
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
