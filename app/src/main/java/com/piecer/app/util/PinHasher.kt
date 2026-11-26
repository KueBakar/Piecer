package com.piecer.app.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    private const val SALT_BYTES = 16
    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256
    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MILLIS = 30_000L

    fun isValidPinFormat(pin: String): Boolean {
        return pin.length in 4..6 && pin.all { it.isDigit() }
    }

    fun generateSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hashPin(pin: String, salt: String): String {
        val saltBytes = Base64.getDecoder().decode(salt)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derived = factory.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(derived)
    }

    fun verifyPin(pin: String, salt: String, expectedHash: String): Boolean {
        if (!isValidPinFormat(pin)) return false
        val actualBytes = Base64.getDecoder().decode(hashPin(pin, salt))
        val expectedBytes = Base64.getDecoder().decode(expectedHash)
        return MessageDigest.isEqual(actualBytes, expectedBytes)
    }
}
