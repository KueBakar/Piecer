package com.piecer.app.data.backup

import com.piecer.app.data.local.entity.BudgetEntity
import com.piecer.app.data.local.entity.BudgetPeriodType
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.GoalEntity
import com.piecer.app.data.local.entity.TransactionEntity
import com.piecer.app.data.local.entity.TransactionType
import com.piecer.app.data.local.entity.WalletEntity
import com.piecer.app.data.local.entity.WalletType
import com.piecer.app.data.preferences.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupJsonCodecTest {

    @Test
    fun `round-trip encode and decode produces identical backup`() {
        val original = PiecerBackup(
            version = PiecerBackup.CURRENT_VERSION,
            exportedAt = 1_770_000_000_000L,
            wallets = listOf(
                WalletEntity(
                    id = 1L,
                    name = "Tunai \"Utama\"",
                    type = WalletType.CASH,
                    initialBalance = 500_000L,
                    iconKey = "wallet",
                    colorKey = "ink",
                    createdAt = 1_760_000_000_000L
                ),
                WalletEntity(
                    id = 2L,
                    name = "Bank BCA",
                    type = WalletType.BANK,
                    initialBalance = 2_500_000L,
                    iconKey = "account_balance",
                    colorKey = "mint",
                    createdAt = 1_760_000_100_000L
                )
            ),
            categories = listOf(
                CategoryEntity(1L, "Makan", CategoryType.EXPENSE, "restaurant", "coral", true),
                CategoryEntity(2L, "Gaji", CategoryType.INCOME, "payments", "mint", true)
            ),
            transactions = listOf(
                TransactionEntity(
                    id = 10L,
                    type = TransactionType.EXPENSE,
                    amount = 35_000L,
                    walletId = 1L,
                    toWalletId = null,
                    categoryId = 1L,
                    note = "Nasi padang\npedas",
                    date = 1_765_000_000_000L,
                    createdAt = 1_765_000_000_000L
                ),
                TransactionEntity(
                    id = 11L,
                    type = TransactionType.TRANSFER,
                    amount = 200_000L,
                    walletId = 2L,
                    toWalletId = 1L,
                    categoryId = null,
                    note = "",
                    date = 1_766_000_000_000L,
                    createdAt = 1_766_000_000_000L
                )
            ),
            budgets = listOf(
                BudgetEntity(
                    id = 5L,
                    categoryId = 1L,
                    limitAmount = 1_500_000L,
                    periodType = BudgetPeriodType.MONTHLY,
                    createdAt = 1_760_000_000_000L
                )
            ),
            goals = listOf(
                GoalEntity(
                    id = 3L,
                    name = "Dana Darurat",
                    targetAmount = 20_000_000L,
                    savedAmount = 5_000_000L,
                    deadline = 1_800_000_000_000L,
                    iconKey = "savings",
                    colorKey = "mint",
                    createdAt = 1_761_000_000_000L
                ),
                GoalEntity(
                    id = 4L,
                    name = "Laptop Baru",
                    targetAmount = 15_000_000L,
                    savedAmount = 0L,
                    deadline = null,
                    iconKey = "laptop",
                    colorKey = "ink",
                    createdAt = 1_762_000_000_000L
                )
            ),
            settings = BackupSettings(
                themeMode = ThemeMode.DARK,
                cycleStartDay = 25,
                isBalanceHidden = true,
                dailyReminderEnabled = true,
                dailyReminderHour = 21,
                dailyReminderMinute = 30,
                blockScreenshots = true,
                reduceMotion = true
            )
        )

        val json = BackupJsonCodec.encode(original)
        val decoded = BackupJsonCodec.decode(json)

        assertEquals(original, decoded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decode rejects unsupported backup version`() {
        val json = """
            {"version":99,"exportedAt":1,"wallets":[],"categories":[],"transactions":[],"budgets":[],"goals":[],"settings":{"themeMode":"SYSTEM","cycleStartDay":1,"isBalanceHidden":false,"dailyReminderEnabled":false,"dailyReminderHour":20,"dailyReminderMinute":0,"blockScreenshots":false,"reduceMotion":false}}
        """.trimIndent()
        BackupJsonCodec.decode(json)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decode rejects malformed json`() {
        BackupJsonCodec.decode("{invalid json")
    }
}
