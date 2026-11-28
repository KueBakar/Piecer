package com.piecer.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SeedSchemaTest {

    @Test
    fun `all seeded rows provide every non-null column without defaultValue in latest Room schema`() {
        val schemaFile = locateLatestSchemaFile()
        val root = SchemaJsonParser(schemaFile.readText()).parseObject()
        @Suppress("UNCHECKED_CAST")
        val database = root["database"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val entities = database["entities"] as List<Map<String, Any?>>
        val entitiesByTable = entities.associateBy { it["tableName"] as String }

        val seedByTable = SeedData.rowsByTable(nowMillis = 1_700_000_000_000L)
        assertTrue("Expected at least one seeded table", seedByTable.isNotEmpty())

        seedByTable.forEach { (tableName, rows) ->
            assertTrue("Seeded table '$tableName' must have at least one row", rows.isNotEmpty())

            val entity = entitiesByTable[tableName]
            assertNotNull("Table '$tableName' not found in schema ${schemaFile.name}", entity)
            entity!!

            @Suppress("UNCHECKED_CAST")
            val primaryKey = entity["primaryKey"] as Map<String, Any?>
            val autoGenerate = primaryKey["autoGenerate"] as? Boolean ?: false
            @Suppress("UNCHECKED_CAST")
            val pkColumns = (primaryKey["columnNames"] as? List<String>).orEmpty().toSet()
            val autoGenPkColumns = if (autoGenerate) pkColumns else emptySet()

            @Suppress("UNCHECKED_CAST")
            val fields = entity["fields"] as List<Map<String, Any?>>
            val allSchemaColumns = fields.map { it["columnName"] as String }.toSet()

            val requiredColumns = fields.filter { field ->
                val columnName = field["columnName"] as String
                val notNull = field["notNull"] as? Boolean ?: false
                val hasDefault = field.containsKey("defaultValue") && field["defaultValue"] != null
                notNull && !hasDefault && columnName !in autoGenPkColumns
            }.map { it["columnName"] as String }

            rows.forEachIndexed { index, row ->
                requiredColumns.forEach { col ->
                    assertTrue(
                        "Table '$tableName' seed row #$index is missing required NOT NULL column '$col' (schema ${schemaFile.name})",
                        row.containsKey(col) && row[col] != null
                    )
                }
                row.keys.forEach { col ->
                    assertTrue(
                        "Table '$tableName' seed row #$index contains unknown column '$col' not in schema ${schemaFile.name}",
                        col in allSchemaColumns
                    )
                }
            }
        }
    }

    @Test
    fun `seed contains expected default wallet and categories`() {
        val wallets = SeedData.walletRows(nowMillis = 1_700_000_000_000L)
        assertEquals(1, wallets.size)
        assertEquals("Tunai", wallets.first()["name"])

        val categories = SeedData.categoryRows()
        assertEquals(12, categories.size)
        assertTrue(categories.all { it["isArchived"] == 0 })
        assertTrue(categories.all { it["isDefault"] == 1 })
        assertFalse(categories.any { it["name"] == null })
    }

    private fun locateLatestSchemaFile(): File {
        val candidates = listOf(
            File("schemas/com.piecer.app.data.local.PiecerDatabase"),
            File("app/schemas/com.piecer.app.data.local.PiecerDatabase")
        )
        val schemaDir = candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate Room schema directory in: $candidates (cwd=${File(".").absolutePath})")

        val versionedFiles = schemaDir.listFiles { file ->
            file.isFile && file.extension == "json" && file.nameWithoutExtension.toIntOrNull() != null
        }.orEmpty()

        return versionedFiles.maxByOrNull { it.nameWithoutExtension.toInt() }
            ?: error("No versioned schema JSON files found in ${schemaDir.absolutePath}")
    }

    private class SchemaJsonParser(private val input: String) {
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
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return map
                    }
                    else -> error("Expected ',' or '}' at position $pos")
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
                list.add(parseValue())
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return list
                    }
                    else -> error("Expected ',' or ']' at position $pos")
                }
            }
        }

        private fun parseValue(): Any? {
            skipWhitespace()
            return when (val c = peek()) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                '-', in '0'..'9' -> parseNumber()
                else -> error("Unexpected character '$c' at position $pos")
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < input.length) {
                val c = input[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        val esc = input[pos++]
                        when (esc) {
                            '"', '\\', '/' -> sb.append(esc)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                val hex = input.substring(pos, pos + 4)
                                sb.append(hex.toInt(16).toChar())
                                pos += 4
                            }
                            else -> error("Invalid escape '\\$esc' at position $pos")
                        }
                    }
                    else -> sb.append(c)
                }
            }
            error("Unterminated string")
        }

        private fun parseBoolean(): Boolean = if (input.startsWith("true", pos)) {
            pos += 4
            true
        } else if (input.startsWith("false", pos)) {
            pos += 5
            false
        } else {
            error("Invalid boolean at position $pos")
        }

        private fun parseNull(): Any? {
            if (input.startsWith("null", pos)) {
                pos += 4
                return null
            }
            error("Invalid null at position $pos")
        }

        private fun parseNumber(): Number {
            val start = pos
            if (input[pos] == '-') pos++
            while (pos < input.length && input[pos] in '0'..'9') pos++
            var isDecimal = false
            if (pos < input.length && input[pos] == '.') {
                isDecimal = true
                pos++
                while (pos < input.length && input[pos] in '0'..'9') pos++
            }
            if (pos < input.length && (input[pos] == 'e' || input[pos] == 'E')) {
                isDecimal = true
                pos++
                if (pos < input.length && (input[pos] == '+' || input[pos] == '-')) pos++
                while (pos < input.length && input[pos] in '0'..'9') pos++
            }
            val raw = input.substring(start, pos)
            return if (isDecimal) raw.toDouble() else raw.toLong()
        }

        private fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) pos++
        }

        private fun peek(): Char {
            if (pos >= input.length) error("Unexpected end of JSON at position $pos")
            return input[pos]
        }

        private fun expect(expected: Char) {
            val actual = peek()
            if (actual != expected) error("Expected '$expected' but got '$actual' at position $pos")
            pos++
        }
    }
}
