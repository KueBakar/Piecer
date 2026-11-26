package com.piecer.app.data.backup

import com.piecer.app.data.local.entity.BudgetEntity
import com.piecer.app.data.local.entity.BudgetPeriodType
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.GoalEntity
import com.piecer.app.data.local.entity.PieceAllocationEntity
import com.piecer.app.data.local.entity.PieceCategoryEntity
import com.piecer.app.data.local.entity.PieceEntity
import com.piecer.app.data.local.entity.RecurringEntity
import com.piecer.app.data.local.entity.RecurringInterval
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
            pieces = listOf(
                PieceEntity(1L, "Kebutuhan", 50, "ink", "home", 0, 1_760_000_000_000L),
                PieceEntity(2L, "Keinginan", 30, "amber", "shopping_bag", 1, 1_760_000_000_000L),
                PieceEntity(3L, "Tabungan", 20, "mint", "savings", 2, 1_760_000_000_000L)
            ),
            pieceCategories = listOf(
                PieceCategoryEntity(pieceId = 1L, categoryId = 1L)
            ),
            pieceAllocations = listOf(
                PieceAllocationEntity(1L, 1L, 10L, 500_000L, 1_765_000_000_000L)
            ),
            recurring = listOf(
                RecurringEntity(
                    id = 1L,
                    type = TransactionType.EXPENSE,
                    amount = 150_000L,
                    categoryId = 1L,
                    walletId = 1L,
                    toWalletId = null,
                    note = "Internet",
                    interval = RecurringInterval.MONTHLY,
                    dayOfMonth = 20,
                    dayOfWeek = null,
                    nextDueDate = 1_770_000_000_000L,
                    isActive = true,
                    remindDaysBefore = 3,
                    createdAt = 1_760_000_000_000L
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
                reduceMotion = true,
                piecesEnabled = true
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

    @Test
    fun `decode supports legacy v1 backup without pieces or recurring`() {
        val v1Json = """
            {"version":1,"exportedAt":1770000000000,"wallets":[{"id":1,"name":"Tunai","type":"CASH","initialBalance":100000,"iconKey":"wallet","colorKey":"ink","createdAt":1760000000000}],"categories":[{"id":1,"name":"Makan","type":"EXPENSE","iconKey":"restaurant","colorKey":"coral","isDefault":true}],"transactions":[],"budgets":[],"goals":[],"settings":{"themeMode":"LIGHT","cycleStartDay":1,"isBalanceHidden":false,"dailyReminderEnabled":false,"dailyReminderHour":20,"dailyReminderMinute":0,"blockScreenshots":false,"reduceMotion":false}}
        """.trimIndent()
        val decoded = BackupJsonCodec.decode(v1Json)
        assertEquals(1, decoded.version)
        assertEquals(1, decoded.wallets.size)
        assertEquals(emptyList<PieceEntity>(), decoded.pieces)
        assertEquals(emptyList<PieceCategoryEntity>(), decoded.pieceCategories)
        assertEquals(emptyList<PieceAllocationEntity>(), decoded.pieceAllocations)
        assertEquals(emptyList<RecurringEntity>(), decoded.recurring)
        assertEquals(false, decoded.settings.piecesEnabled)
    }
}
