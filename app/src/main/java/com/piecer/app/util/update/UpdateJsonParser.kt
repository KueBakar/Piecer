package com.piecer.app.util.update

import org.json.JSONObject
import java.util.Locale

object UpdateJsonParser {

    const val MAX_JSON_BYTES = 64 * 1024 // 64 KB
    const val MAX_APK_BYTES = 200L * 1024L * 1024L // 200 MB sanity cap

    private val sha256Regex = Regex("^[0-9a-f]{64}$")

    fun parseAndValidate(
        rawJson: String,
        currentVersionCode: Int,
        currentSdkInt: Int,
        maxJsonBytes: Int = MAX_JSON_BYTES
    ): UpdateCheckResult {
        if (rawJson.toByteArray(Charsets.UTF_8).size > maxJsonBytes) {
            return UpdateCheckResult.Error("Metadata pembaruan terlalu besar.")
        }
        if (rawJson.isBlank()) {
            return UpdateCheckResult.Error("Metadata pembaruan kosong.")
        }

        val metadata = try {
            parseWithOrgJson(rawJson)
        } catch (e: RuntimeException) {
            val msg = e.message.orEmpty()
            if (msg.contains("Stub", ignoreCase = true) || msg.contains("not mocked", ignoreCase = true)) {
                try {
                    parseWithFallbackJson(rawJson)
                } catch (_: Exception) {
                    return UpdateCheckResult.Error("Format metadata pembaruan tidak valid.")
                }
            } else {
                return UpdateCheckResult.Error("Format metadata pembaruan tidak valid.")
            }
        } catch (_: Exception) {
            return UpdateCheckResult.Error("Format metadata pembaruan tidak valid.")
        }

        return validateMetadata(metadata, currentVersionCode, currentSdkInt)
    }

    private fun parseWithOrgJson(rawJson: String): UpdateMetadata {
        val obj = JSONObject(rawJson)
        return UpdateMetadata(
            versionCode = obj.getInt("versionCode"),
            versionName = obj.getString("versionName").trim(),
            apkUrl = obj.getString("apkUrl").trim(),
            sha256 = obj.getString("sha256").trim().lowercase(Locale.ROOT),
            sizeBytes = obj.getLong("sizeBytes"),
            minSdk = obj.getInt("minSdk"),
            releaseNotes = obj.optString("releaseNotes", "").trim()
        )
    }

    /**
     * Pure-Kotlin fallback used only when running under Android's unmocked JVM stub (android.jar).
     */
    private fun parseWithFallbackJson(rawJson: String): UpdateMetadata {
        val map = SimpleJsonObjectParser(rawJson).parseObject()
        val versionCode = (map["versionCode"] as? Number)?.toInt() ?: error("missing versionCode")
        val versionName = (map["versionName"] as? String)?.trim() ?: error("missing versionName")
        val apkUrl = (map["apkUrl"] as? String)?.trim() ?: error("missing apkUrl")
        val sha256 = (map["sha256"] as? String)?.trim()?.lowercase(Locale.ROOT) ?: error("missing sha256")
        val sizeBytes = (map["sizeBytes"] as? Number)?.toLong() ?: error("missing sizeBytes")
        val minSdk = (map["minSdk"] as? Number)?.toInt() ?: error("missing minSdk")
        val releaseNotes = (map["releaseNotes"] as? String)?.trim().orEmpty()
        return UpdateMetadata(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = apkUrl,
            sha256 = sha256,
            sizeBytes = sizeBytes,
            minSdk = minSdk,
            releaseNotes = releaseNotes
        )
    }

    fun validateMetadata(
        metadata: UpdateMetadata,
        currentVersionCode: Int,
        currentSdkInt: Int
    ): UpdateCheckResult {
        if (metadata.versionCode <= 0 || metadata.versionName.isBlank()) {
            return UpdateCheckResult.Error("Informasi versi pembaruan tidak valid.")
        }
        if (!UpdateUrlValidator.isAllowedUrl(metadata.apkUrl, allowGithubIo = false)) {
            return UpdateCheckResult.Error("URL APK tidak berasal dari sumber tepercaya.")
        }
        if (!sha256Regex.matches(metadata.sha256)) {
            return UpdateCheckResult.Error("Checksum SHA-256 pada metadata tidak valid.")
        }
        if (metadata.sizeBytes <= 0L || metadata.sizeBytes > MAX_APK_BYTES) {
            return UpdateCheckResult.Error("Ukuran APK pada metadata tidak valid.")
        }
        if (metadata.minSdk < 1) {
            return UpdateCheckResult.Error("Syarat minSdk tidak valid.")
        }
        if (metadata.versionCode <= currentVersionCode) {
            return UpdateCheckResult.UpToDate
        }
        if (currentSdkInt < metadata.minSdk) {
            return UpdateCheckResult.Incompatible(metadata.minSdk)
        }
        return UpdateCheckResult.UpdateAvailable(metadata)
    }

    private class SimpleJsonObjectParser(private val src: String) {
        private var pos = 0

        fun parseObject(): Map<String, Any?> {
            skipWhitespace()
            expect('{')
            val result = linkedMapOf<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                pos++
                skipWhitespace()
                require(pos == src.length) { "Trailing characters" }
                return result
            }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                skipWhitespace()
                val value = parseValue()
                result[key] = value
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        break
                    }
                    else -> error("Expected ',' or '}'")
                }
            }
            skipWhitespace()
            require(pos == src.length) { "Trailing characters" }
            return result
        }

        private fun parseValue(): Any? {
            skipWhitespace()
            return when (val c = peek()) {
                '"' -> parseString()
                '-', in '0'..'9' -> parseNumber()
                't' -> { expectLiteral("true"); true }
                'f' -> { expectLiteral("false"); false }
                'n' -> { expectLiteral("null"); null }
                else -> error("Unexpected char: $c")
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < src.length) {
                val c = src[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        require(pos < src.length) { "Unterminated escape" }
                        when (val esc = src[pos++]) {
                            '"', '\\', '/' -> sb.append(esc)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                require(pos + 4 <= src.length) { "Bad unicode escape" }
                                val code = src.substring(pos, pos + 4).toInt(16)
                                sb.append(code.toChar())
                                pos += 4
                            }
                            else -> error("Invalid escape: $esc")
                        }
                    }
                    else -> sb.append(c)
                }
            }
            error("Unterminated string")
        }

        private fun parseNumber(): Number {
            val start = pos
            if (src[pos] == '-') pos++
            while (pos < src.length && src[pos].isDigit()) pos++
            val token = src.substring(start, pos)
            return token.toLong()
        }

        private fun expectLiteral(lit: String) {
            require(src.startsWith(lit, pos)) { "Expected $lit" }
            pos += lit.length
        }

        private fun expect(ch: Char) {
            require(pos < src.length && src[pos] == ch) { "Expected $ch" }
            pos++
        }

        private fun peek(): Char {
            require(pos < src.length) { "Unexpected EOF" }
            return src[pos]
        }

        private fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }
    }
}
