package com.piecer.app.util

import com.piecer.app.data.local.entity.AssetEntity
import com.piecer.app.data.local.entity.AssetHistoryEntity
import com.piecer.app.data.local.entity.AssetType
import com.piecer.app.data.local.entity.DebtDirection
import com.piecer.app.data.local.entity.DebtEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class NetWorthCalculatorTest {

    @Test
    fun `calculate computes assets plus wallets minus unpaid I_OWE debts`() {
        val assets = listOf(
            AssetEntity(1L, "Emas", AssetType.EMAS, 10_000_000L, updatedAt = 1L),
            AssetEntity(2L, "Saham", AssetType.SAHAM, 5_000_000L, updatedAt = 1L)
        )
        val debts = listOf(
            DebtEntity(1L, "Bank", 4_000_000L, 1_000_000L, DebtDirection.I_OWE, createdAt = 1L),
            DebtEntity(2L, "Teman", 2_000_000L, 0L, DebtDirection.OWE_ME, createdAt = 1L)
        )
        val summary = NetWorthCalculator.calculate(
            assets = assets,
            totalWalletBalance = 3_000_000L,
            debts = debts
        )
        assertEquals(15_000_000L, summary.totalAssets)
        assertEquals(3_000_000L, summary.totalWalletBalance)
        assertEquals(3_000_000L, summary.totalDebts)
        assertEquals(15_000_000L, summary.netWorth)
    }

    @Test
    fun `buildAssetTimeline aggregates latest values across assets over time`() {
        val assets = listOf(
            AssetEntity(1L, "Emas", AssetType.EMAS, 12_000_000L, updatedAt = 300L),
            AssetEntity(2L, "Reksadana", AssetType.REKSADANA, 5_000_000L, updatedAt = 200L)
        )
        val history = listOf(
            AssetHistoryEntity(1L, 1L, 10_000_000L, 100L),
            AssetHistoryEntity(2L, 2L, 5_000_000L, 200L),
            AssetHistoryEntity(3L, 1L, 12_000_000L, 300L)
        )
        val timeline = NetWorthCalculator.buildAssetTimeline(assets, history)
        assertEquals(3, timeline.size)
        assertEquals(10_000_000L, timeline[0].totalValue)
        assertEquals(15_000_000L, timeline[1].totalValue)
        assertEquals(17_000_000L, timeline[2].totalValue)
    }
}
