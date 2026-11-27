package com.piecer.app.util

import com.piecer.app.data.local.entity.AssetEntity
import com.piecer.app.data.local.entity.AssetHistoryEntity
import com.piecer.app.data.local.entity.DebtDirection
import com.piecer.app.data.local.entity.DebtEntity

data class NetWorthSummary(
    val totalAssets: Long,
    val totalWalletBalance: Long,
    val totalDebts: Long,
    val netWorth: Long
)

data class AssetHistoryPoint(
    val date: Long,
    val totalValue: Long
)

object NetWorthCalculator {

    fun calculate(
        assets: List<AssetEntity>,
        totalWalletBalance: Long,
        debts: List<DebtEntity>
    ): NetWorthSummary {
        val totalAssets = assets.sumOf { it.value }
        val totalDebts = DebtCalculator.totalRemainingByDirection(debts, DebtDirection.I_OWE)
        val netWorth = totalAssets + totalWalletBalance - totalDebts
        return NetWorthSummary(
            totalAssets = totalAssets,
            totalWalletBalance = totalWalletBalance,
            totalDebts = totalDebts,
            netWorth = netWorth
        )
    }

    /**
     * Membangun titik grafik total nilai aset sepanjang waktu dari riwayat perubahan nilai.
     */
    fun buildAssetTimeline(
        assets: List<AssetEntity>,
        history: List<AssetHistoryEntity>
    ): List<AssetHistoryPoint> {
        val activeIds = assets.map { it.id }.toSet()
        val relevant = history.filter { it.assetId in activeIds }.sortedWith(compareBy({ it.date }, { it.id }))
        if (relevant.isEmpty()) return emptyList()

        val latestByAsset = HashMap<Long, Long>()
        val points = ArrayList<AssetHistoryPoint>(relevant.size)
        for (entry in relevant) {
            latestByAsset[entry.assetId] = entry.value
            val sum = latestByAsset.values.sum()
            if (points.isNotEmpty() && points.last().date == entry.date) {
                points[points.lastIndex] = AssetHistoryPoint(date = entry.date, totalValue = sum)
            } else {
                points.add(AssetHistoryPoint(date = entry.date, totalValue = sum))
            }
        }
        return points
    }
}
