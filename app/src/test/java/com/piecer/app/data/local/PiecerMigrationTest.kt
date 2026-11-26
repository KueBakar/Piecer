package com.piecer.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class PiecerMigrationTest {

    @Test
    fun `migration 1 to 2 has correct version range and executes budgets DDL`() {
        val migration = PiecerDatabase.MIGRATION_1_2
        assertEquals(1, migration.startVersion)
        assertEquals(2, migration.endVersion)

        val executedSql = mutableListOf<String>()
        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        migration.migrate(dbProxy)

        assertEquals(2, executedSql.size)
        val createTableSql = executedSql[0]
        assertTrue(createTableSql.contains("CREATE TABLE IF NOT EXISTS `budgets`"))
        assertTrue(createTableSql.contains("`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
        assertTrue(createTableSql.contains("`categoryId` INTEGER NOT NULL"))
        assertTrue(createTableSql.contains("`limitAmount` INTEGER NOT NULL"))
        assertTrue(createTableSql.contains("`periodType` TEXT NOT NULL DEFAULT 'MONTHLY'"))
        assertTrue(createTableSql.contains("`createdAt` INTEGER NOT NULL"))

        val createIndexSql = executedSql[1]
        assertTrue(createIndexSql.contains("CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_categoryId_periodType`"))
    }
}
