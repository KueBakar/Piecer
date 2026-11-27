package com.piecer.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.piecer.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PiecerLargeWidgetProvider : AppWidgetProvider() {

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
                val rowIds = listOf(
                    Triple(R.id.widget_tx_row_1, R.id.widget_tx_title_1, R.id.widget_tx_amount_1),
                    Triple(R.id.widget_tx_row_2, R.id.widget_tx_title_2, R.id.widget_tx_amount_2),
                    Triple(R.id.widget_tx_row_3, R.id.widget_tx_title_3, R.id.widget_tx_amount_3)
                )
                val mintColor = ContextCompat.getColor(context, R.color.widget_accent_mint)
                val coralColor = ContextCompat.getColor(context, R.color.widget_accent_coral)
                val neutralColor = ContextCompat.getColor(context, R.color.widget_text_primary)

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_piecer_large)
                    views.setTextViewText(R.id.widget_allowance_amount, data.dailyAllowanceText)
                    views.setTextViewText(R.id.widget_total_balance_amount, data.totalBalanceText)

                    if (data.recentTransactions.isEmpty()) {
                        views.setViewVisibility(R.id.widget_tx_empty, View.VISIBLE)
                        rowIds.forEach { (rowId, _, _) -> views.setViewVisibility(rowId, View.GONE) }
                    } else {
                        views.setViewVisibility(R.id.widget_tx_empty, View.GONE)
                        rowIds.forEachIndexed { index, (rowId, titleId, amountId) ->
                            val item = data.recentTransactions.getOrNull(index)
                            if (item == null) {
                                views.setViewVisibility(rowId, View.GONE)
                            } else {
                                views.setViewVisibility(rowId, View.VISIBLE)
                                views.setTextViewText(titleId, item.title)
                                views.setTextViewText(amountId, item.amountText)
                                val color = when {
                                    data.hideAmounts -> neutralColor
                                    item.isIncome -> mintColor
                                    item.isExpense -> coralColor
                                    else -> neutralColor
                                }
                                views.setTextColor(amountId, color)
                            }
                        }
                    }

                    views.setOnClickPendingIntent(
                        R.id.widget_root,
                        PiecerWidgetUpdater.createOpenAppPendingIntent(context, appWidgetId)
                    )
                    views.setOnClickPendingIntent(
                        R.id.widget_add_button,
                        PiecerWidgetUpdater.createAddTransactionPendingIntent(context, appWidgetId + 2000)
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
