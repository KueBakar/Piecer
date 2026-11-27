package com.piecer.app.quick

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.piecer.app.MainActivity
import com.piecer.app.R
import com.piecer.app.ui.navigation.PiecerRoutes
import com.piecer.app.widget.PiecerWidgetUpdater

class QuickAddTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { tile ->
            tile.label = getString(R.string.qs_tile_quick_add)
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(PiecerWidgetUpdater.EXTRA_START_ROUTE, PiecerRoutes.ADD_TRANSACTION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                3401,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
