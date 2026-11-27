package com.piecer.app.data.backup

import com.piecer.app.data.local.entity.AssetEntity
import com.piecer.app.data.local.entity.AssetHistoryEntity
import com.piecer.app.data.local.entity.AssetType
import com.piecer.app.data.local.entity.BudgetEntity
import com.piecer.app.data.local.entity.BudgetPeriodType
import com.piecer.app.data.local.entity.CategoryEntity
import com.piecer.app.data.local.entity.CategoryType
import com.piecer.app.data.local.entity.DebtDirection
import com.piecer.app.data.local.entity.DebtEntity
import com.piecer.app.data.local.entity.DebtPaymentEntity
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

object BackupJsonCodec {

    fun encode(backup: PiecerBackup): String {
        val sb = StringBuilder(2048)
        sb.append("{\n")
        sb.append("  \"version\": ").append(backup.version).append(",\n")
        sb.append("  \"exportedAt\": ").append(backup.exportedAt).append(",\n")

        sb.append("  \"wallets\": ")
        appendArray(sb, backup.wallets) { w ->
            appendObject(
                "id" to w.id,
                "name" to w.name,
                "type" to w.type.name,
                "initialBalance" to w.initialBalance,
                "iconKey" to w.iconKey,
                "colorKey" to w.colorKey,
                "createdAt" to w.createdAt
            )
        }
        sb.append(",\n")

        sb.append("  \"categories\": ")
        appendArray(sb, backup.categories) { c ->
            appendObject(
                "id" to c.id,
                "name" to c.name,
                "type" to c.type.name,
                "iconKey" to c.iconKey,
                "colorKey" to c.colorKey,
                "isDefault" to c.isDefault,
                "isArchived" to c.isArchived
            )
        }
        sb.append(",\n")

        sb.append("  \"transactions\": ")
        appendArray(sb, backup.transactions) { t ->
            appendObject(
                "id" to t.id,
                "type" to t.type.name,
                "amount" to t.amount,
                "walletId" to t.walletId,
                "toWalletId" to t.toWalletId,
                "categoryId" to t.categoryId,
                "note" to t.note,
                "date" to t.date,
                "createdAt" to t.createdAt
            )
        }
        sb.append(",\n")

        sb.append("  \"budgets\": ")
        appendArray(sb, backup.budgets) { b ->
            appendObject(
                "id" to b.id,
                "categoryId" to b.categoryId,
                "limitAmount" to b.limitAmount,
                "periodType" to b.periodType.name,
                "createdAt" to b.createdAt
            )
        }
        sb.append(",\n")

        sb.append("  \"goals\": ")
        appendArray(sb, backup.goals) { g ->
            appendObject(
                "id" to g.id,
                "name" to g.name,
                "targetAmount" to g.targetAmount,
                "savedAmount" to g.savedAmount,
                "deadline" to g.deadline,
                "iconKey" to g.iconKey,
                "colorKey" to g.colorKey,
                "createdAt" to g.createdAt
            )
        }
        sb.append(",\n")

        sb.append("  \"pieces\": ")
        appendArray(sb, backup.pieces) { p ->
            appendObject(
                "id" to p.id,
                "name" to p.name,
                "percent" to p.percent,
                "colorKey" to p.colorKey,
                "iconKey" to p.iconKey,
                "sortOrder" to p.sortOrder,
                "createdAt" to p.createdAt
            )
        }
        sb.append(",\n")

        sb.append("  \"pieceCategories\": ")
        appendArray(sb, backup.pieceCategories) { pc ->
            appendObject(
                "pieceId" to pc.pieceId,
                "categoryId" to pc.categoryId
            )
        }
        sb.append(",\n")

        sb.append("  \"pieceAllocations\": ")
        appendArray(sb, backup.pieceAllocations) { pa ->
            appendObject(
                "id" to pa.id,
                "pieceId" to pa.pieceId,
                "transactionId" to pa.transactionId,
                "amount" to pa.amount,
                "createdAt" to pa.createdAt
            )
        }
        sb.append(",\n")

        sb.append("  \"recurring\": ")
        appendArray(sb, backup.recurring) { r ->
            appendObject(
                "id" to r.id,
                "type" to r.type.name,
                "amount" to r.amount,
                "categoryId" to r.categoryId,
                "walletId" to r.walletId,
                "toWalletId" to r.toWalletId,
                "note" to r.note,
                "interval" to r.interval.name,
                "dayOfMonth" to r.dayOfMonth,
                "dayOfWeek" to r.dayOfWeek,
                "nextDueDate" to r.nextDueDate,
                "isActive" to r.isActive,
                "remindDaysBefore" to r.remindDaysBefore,
                "createdAt" to r.createdAt
            )
        }
        sb.append(",\n")

        sb.append("  \"debts\": ")
        appendArray(sb, backup.debts) { d ->
            appendObject(
                "id" to d.id,
                "personName" to d.personName,
                "totalAmount" to d.totalAmount,
                "paidAmount" to d.paidAmount,
                "direction" to d.direction.name,
                "dueDate" to d.dueDate,
                "note" to d.note,
                "walletId" to d.walletId,
                "recordAsTransaction" to d.recordAsTransaction,
                "createdAt" to d.createdAt
            )
        }
        sb.append(",\n")

        sb.append("  \"debtPayments\": ")
        appendArray(sb, backup.debtPayments) { dp ->
            appendObject(
                "id" to dp.id,
                "debtId" to dp.debtId,
                "amount" to dp.amount,
                "date" to dp.date,
                "walletId" to dp.walletId,
                "note" to dp.note
            )
        }
        sb.append(",\n")

        sb.append("  \"assets\": ")
        appendArray(sb, backup.assets) { a ->
            appendObject(
                "id" to a.id,
                "name" to a.name,
                "type" to a.type.name,
                "value" to a.value,
                "note" to a.note,
                "updatedAt" to a.updatedAt
            )
        }
        sb.append(",\n")

        sb.append("  \"assetHistory\": ")
        appendArray(sb, backup.assetHistory) { ah ->
            appendObject(
                "id" to ah.id,
                "assetId" to ah.assetId,
                "value" to ah.value,
                "date" to ah.date
            )
        }
        sb.append(",\n")

        sb.append("  \"settings\": ")
        sb.appendObject(
            "themeMode" to backup.settings.themeMode.name,
            "cycleStartDay" to backup.settings.cycleStartDay,
            "isBalanceHidden" to backup.settings.isBalanceHidden,
            "dailyReminderEnabled" to backup.settings.dailyReminderEnabled,
            "dailyReminderHour" to backup.settings.dailyReminderHour,
            "dailyReminderMinute" to backup.settings.dailyReminderMinute,
            "blockScreenshots" to backup.settings.blockScreenshots,
            "reduceMotion" to backup.settings.reduceMotion,
            "piecesEnabled" to backup.settings.piecesEnabled
        )
        sb.append("\n}")
        return sb.toString()
    }

    fun decode(json: String): PiecerBackup {
        val root = JsonParser(json).parseObject()
        val version = root.reqInt("version")
        if (version !in 1..PiecerBackup.CURRENT_VERSION) {
            throw IllegalArgumentException("Versi backup tidak didukung: $version")
        }
        val exportedAt = root.reqLong("exportedAt")

        val wallets = root.reqArray("wallets").map { elem ->
            val obj = elem.asObj()
            WalletEntity(
                id = obj.reqLong("id"),
                name = obj.reqString("name"),
                type = WalletType.valueOf(obj.reqString("type")),
                initialBalance = obj.reqLong("initialBalance"),
                iconKey = obj.reqString("iconKey"),
                colorKey = obj.reqString("colorKey"),
                createdAt = obj.reqLong("createdAt")
            )
        }

        val categories = root.reqArray("categories").map { elem ->
            val obj = elem.asObj()
            CategoryEntity(
                id = obj.reqLong("id"),
                name = obj.reqString("name"),
                type = CategoryType.valueOf(obj.reqString("type")),
                iconKey = obj.reqString("iconKey"),
                colorKey = obj.reqString("colorKey"),
                isDefault = obj.reqBoolean("isDefault"),
                isArchived = obj.optBoolean("isArchived") ?: false
            )
        }

        val transactions = root.reqArray("transactions").map { elem ->
            val obj = elem.asObj()
            TransactionEntity(
                id = obj.reqLong("id"),
                type = TransactionType.valueOf(obj.reqString("type")),
                amount = obj.reqLong("amount"),
                walletId = obj.reqLong("walletId"),
                toWalletId = obj.optLong("toWalletId"),
                categoryId = obj.optLong("categoryId"),
                note = obj.optString("note") ?: "",
                date = obj.reqLong("date"),
                createdAt = obj.reqLong("createdAt")
            )
        }

        val budgets = root.reqArray("budgets").map { elem ->
            val obj = elem.asObj()
            BudgetEntity(
                id = obj.reqLong("id"),
                categoryId = obj.reqLong("categoryId"),
                limitAmount = obj.reqLong("limitAmount"),
                periodType = BudgetPeriodType.valueOf(obj.reqString("periodType")),
                createdAt = obj.reqLong("createdAt")
            )
        }

        val goals = root.reqArray("goals").map { elem ->
            val obj = elem.asObj()
            GoalEntity(
                id = obj.reqLong("id"),
                name = obj.reqString("name"),
                targetAmount = obj.reqLong("targetAmount"),
                savedAmount = obj.reqLong("savedAmount"),
                deadline = obj.optLong("deadline"),
                iconKey = obj.reqString("iconKey"),
                colorKey = obj.reqString("colorKey"),
                createdAt = obj.reqLong("createdAt")
            )
        }

        val pieces = root.optArray("pieces").map { elem ->
            val obj = elem.asObj()
            PieceEntity(
                id = obj.reqLong("id"),
                name = obj.reqString("name"),
                percent = obj.reqInt("percent"),
                colorKey = obj.reqString("colorKey"),
                iconKey = obj.reqString("iconKey"),
                sortOrder = obj.reqInt("sortOrder"),
                createdAt = obj.reqLong("createdAt")
            )
        }

        val pieceCategories = root.optArray("pieceCategories").map { elem ->
            val obj = elem.asObj()
            PieceCategoryEntity(
                pieceId = obj.reqLong("pieceId"),
                categoryId = obj.reqLong("categoryId")
            )
        }

        val pieceAllocations = root.optArray("pieceAllocations").map { elem ->
            val obj = elem.asObj()
            PieceAllocationEntity(
                id = obj.reqLong("id"),
                pieceId = obj.reqLong("pieceId"),
                transactionId = obj.reqLong("transactionId"),
                amount = obj.reqLong("amount"),
                createdAt = obj.reqLong("createdAt")
            )
        }

        val recurring = root.optArray("recurring").map { elem ->
            val obj = elem.asObj()
            RecurringEntity(
                id = obj.reqLong("id"),
                type = TransactionType.valueOf(obj.reqString("type")),
                amount = obj.reqLong("amount"),
                categoryId = obj.optLong("categoryId"),
                walletId = obj.reqLong("walletId"),
                toWalletId = obj.optLong("toWalletId"),
                note = obj.optString("note") ?: "",
                interval = RecurringInterval.valueOf(obj.reqString("interval")),
                dayOfMonth = obj.optInt("dayOfMonth"),
                dayOfWeek = obj.optInt("dayOfWeek"),
                nextDueDate = obj.reqLong("nextDueDate"),
                isActive = obj.reqBoolean("isActive"),
                remindDaysBefore = obj.reqInt("remindDaysBefore"),
                createdAt = obj.reqLong("createdAt")
            )
        }

        val debts = root.optArray("debts").map { elem ->
            val obj = elem.asObj()
            DebtEntity(
                id = obj.reqLong("id"),
                personName = obj.reqString("personName"),
                totalAmount = obj.reqLong("totalAmount"),
                paidAmount = obj.reqLong("paidAmount"),
                direction = DebtDirection.valueOf(obj.reqString("direction")),
                dueDate = obj.optLong("dueDate"),
                note = obj.optString("note") ?: "",
                walletId = obj.optLong("walletId"),
                recordAsTransaction = obj.optBoolean("recordAsTransaction") ?: false,
                createdAt = obj.reqLong("createdAt")
            )
        }

        val debtPayments = root.optArray("debtPayments").map { elem ->
            val obj = elem.asObj()
            DebtPaymentEntity(
                id = obj.reqLong("id"),
                debtId = obj.reqLong("debtId"),
                amount = obj.reqLong("amount"),
                date = obj.reqLong("date"),
                walletId = obj.optLong("walletId"),
                note = obj.optString("note") ?: ""
            )
        }

        val assets = root.optArray("assets").map { elem ->
            val obj = elem.asObj()
            AssetEntity(
                id = obj.reqLong("id"),
                name = obj.reqString("name"),
                type = AssetType.valueOf(obj.reqString("type")),
                value = obj.reqLong("value"),
                note = obj.optString("note") ?: "",
                updatedAt = obj.reqLong("updatedAt")
            )
        }

        val assetHistory = root.optArray("assetHistory").map { elem ->
            val obj = elem.asObj()
            AssetHistoryEntity(
                id = obj.reqLong("id"),
                assetId = obj.reqLong("assetId"),
                value = obj.reqLong("value"),
                date = obj.reqLong("date")
            )
        }

        val settingsObj = root.reqObj("settings")
        val settings = BackupSettings(
            themeMode = ThemeMode.valueOf(settingsObj.reqString("themeMode")),
            cycleStartDay = settingsObj.reqInt("cycleStartDay").coerceIn(1, 28),
            isBalanceHidden = settingsObj.reqBoolean("isBalanceHidden"),
            dailyReminderEnabled = settingsObj.reqBoolean("dailyReminderEnabled"),
            dailyReminderHour = settingsObj.reqInt("dailyReminderHour").coerceIn(0, 23),
            dailyReminderMinute = settingsObj.reqInt("dailyReminderMinute").coerceIn(0, 59),
            blockScreenshots = settingsObj.reqBoolean("blockScreenshots"),
            reduceMotion = settingsObj.reqBoolean("reduceMotion"),
            piecesEnabled = settingsObj.optBoolean("piecesEnabled") ?: false
        )

        return PiecerBackup(
            version = version,
            exportedAt = exportedAt,
            wallets = wallets,
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            goals = goals,
            pieces = pieces,
            pieceCategories = pieceCategories,
            pieceAllocations = pieceAllocations,
            recurring = recurring,
            debts = debts,
            debtPayments = debtPayments,
            assets = assets,
            assetHistory = assetHistory,
            settings = settings
        )
    }

    private fun <T> appendArray(sb: StringBuilder, items: List<T>, itemWriter: StringBuilder.(T) -> Unit) {
        sb.append("[")
        items.forEachIndexed { index, item ->
            if (index > 0) sb.append(",")
            sb.itemWriter(item)
        }
        sb.append("]")
    }

    private fun StringBuilder.appendObject(vararg fields: Pair<String, Any?>) {
        append("{")
        fields.forEachIndexed { index, (key, value) ->
            if (index > 0) append(",")
            appendJsonString(key)
            append(":")
            when (value) {
                null -> append("null")
                is Boolean, is Int, is Long -> append(value.toString())
                is String -> appendJsonString(value)
                else -> appendJsonString(value.toString())
            }
        }
        append("}")
    }

    private fun StringBuilder.appendJsonString(value: String) {
        append('"')
        for (ch in value) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        append(String.format("\\u%04x", ch.code))
                    } else {
                        append(ch)
                    }
                }
            }
        }
        append('"')
    }

    private class JsonParser(private val input: String) {
        private var pos = 0

        fun parseObject(): Map<String, Any?> {
            skipWhitespace()
            expect('{')
            val map = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                pos++
                return map
            }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                skipWhitespace()
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return map
                    }
                    else -> throw IllegalArgumentException("Format JSON tidak valid di posisi $pos")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val list = ArrayList<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                pos++
                return list
            }
            while (true) {
                skipWhitespace()
                list.add(parseValue())
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return list
                    }
                    else -> throw IllegalArgumentException("Format array JSON tidak valid di posisi $pos")
                }
            }
        }

        private fun parseValue(): Any? {
            skipWhitespace()
            if (pos >= input.length) throw IllegalArgumentException("JSON terpotong")
            return when (val ch = input[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> parseLiteral("true", true)
                'f' -> parseLiteral("false", false)
                'n' -> parseLiteral("null", null)
                '-', in '0'..'9' -> parseNumber()
                else -> throw IllegalArgumentException("Token JSON tidak valid '$ch' di posisi $pos")
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < input.length) {
                val ch = input[pos++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= input.length) throw IllegalArgumentException("Escape tidak lengkap")
                        when (val esc = input[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > input.length) throw IllegalArgumentException("Unicode escape tidak lengkap")
                                val hex = input.substring(pos, pos + 4)
                                sb.append(hex.toInt(16).toChar())
                                pos += 4
                            }
                            else -> throw IllegalArgumentException("Escape tidak dikenal \\$esc")
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            throw IllegalArgumentException("String JSON tidak ditutup")
        }

        private fun parseNumber(): Long {
            val start = pos
            if (input[pos] == '-') pos++
            while (pos < input.length && input[pos] in '0'..'9') {
                pos++
            }
            val token = input.substring(start, pos)
            return token.toLongOrNull()
                ?: throw IllegalArgumentException("Angka JSON tidak valid: $token")
        }

        private fun parseLiteral(literal: String, value: Any?): Any? {
            if (!input.startsWith(literal, pos)) {
                throw IllegalArgumentException("Literal JSON tidak valid di posisi $pos")
            }
            pos += literal.length
            return value
        }

        private fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) {
                pos++
            }
        }

        private fun peek(): Char {
            if (pos >= input.length) throw IllegalArgumentException("JSON terpotong")
            return input[pos]
        }

        private fun expect(expected: Char) {
            if (pos >= input.length || input[pos] != expected) {
                throw IllegalArgumentException("Diharapkan '$expected' di posisi $pos")
            }
            pos++
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asObj(): Map<String, Any?> {
        return this as? Map<String, Any?> ?: throw IllegalArgumentException("Objek JSON tidak valid")
    }

    private fun Map<String, Any?>.reqObj(key: String): Map<String, Any?> {
        return this[key].asObj()
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.reqArray(key: String): List<Any?> {
        return this[key] as? List<Any?> ?: throw IllegalArgumentException("Array '$key' tidak ditemukan")
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.optArray(key: String): List<Any?> {
        val v = this[key] ?: return emptyList()
        return v as? List<Any?> ?: throw IllegalArgumentException("Array '$key' tidak valid")
    }

    private fun Map<String, Any?>.reqString(key: String): String {
        return this[key] as? String ?: throw IllegalArgumentException("Field '$key' tidak valid")
    }

    private fun Map<String, Any?>.optString(key: String): String? {
        val v = this[key] ?: return null
        return v as? String ?: throw IllegalArgumentException("Field '$key' harus berupa string")
    }

    private fun Map<String, Any?>.reqLong(key: String): Long {
        return (this[key] as? Number)?.toLong()
            ?: throw IllegalArgumentException("Field '$key' harus berupa angka")
    }

    private fun Map<String, Any?>.optLong(key: String): Long? {
        val v = this[key] ?: return null
        return (v as? Number)?.toLong()
            ?: throw IllegalArgumentException("Field '$key' harus berupa angka")
    }

    private fun Map<String, Any?>.reqInt(key: String): Int {
        return reqLong(key).toInt()
    }

    private fun Map<String, Any?>.optInt(key: String): Int? {
        return optLong(key)?.toInt()
    }

    private fun Map<String, Any?>.reqBoolean(key: String): Boolean {
        return this[key] as? Boolean ?: throw IllegalArgumentException("Field '$key' harus berupa boolean")
    }

    private fun Map<String, Any?>.optBoolean(key: String): Boolean? {
        val v = this[key] ?: return null
        return v as? Boolean ?: throw IllegalArgumentException("Field '$key' harus berupa boolean")
    }
}
