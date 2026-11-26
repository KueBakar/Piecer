package com.piecer.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import com.piecer.app.R

enum class PiecerTab(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Home(
        route = "home",
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    Transactions(
        route = "transactions",
        labelRes = R.string.nav_transactions,
        selectedIcon = Icons.AutoMirrored.Rounded.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong
    ),
    Statistics(
        route = "statistics",
        labelRes = R.string.nav_statistics,
        selectedIcon = Icons.Rounded.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    ),
    More(
        route = "more",
        labelRes = R.string.nav_more,
        selectedIcon = Icons.Rounded.MoreHoriz,
        unselectedIcon = Icons.Outlined.MoreHoriz
    )
}

object PiecerRoutes {
    const val ADD_TRANSACTION = "add_transaction"
    const val WALLETS = "wallets"
    const val BUDGETS = "budgets"
    const val GOALS = "goals"
    const val SETTINGS = "settings"
    const val CRASH_REPORT = "crash_report"
}
