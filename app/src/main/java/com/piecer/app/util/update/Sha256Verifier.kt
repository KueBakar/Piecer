package com.piecer.app.util.update

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object Sha256Verifier {

    fun computeSha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xFF)
        }
    }

    fun computeSha256(file: File): String {
        return file.inputStream().buffered().use { computeSha256(it) }
    }

    fun matchesSha256(file: File, expectedLowerHex: String): Boolean {
        if (!file.isFile) return false
        val actual = computeSha256(file)
        return actual.equals(expectedLowerHex.trim(), ignoreCase = false)
    }
}
