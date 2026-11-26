package com.piecer.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `isValidPinFormat accepts 4 to 6 digits only`() {
        assertTrue(PinHasher.isValidPinFormat("1234"))
        assertTrue(PinHasher.isValidPinFormat("12345"))
        assertTrue(PinHasher.isValidPinFormat("123456"))
        assertFalse(PinHasher.isValidPinFormat("123"))
        assertFalse(PinHasher.isValidPinFormat("1234567"))
        assertFalse(PinHasher.isValidPinFormat("12a4"))
    }

    @Test
    fun `hashPin with salt verifies matching pin and rejects wrong pin`() {
        val salt1 = PinHasher.generateSalt()
        val salt2 = PinHasher.generateSalt()
        val hash1 = PinHasher.hashPin("482910", salt1)
        val hash2 = PinHasher.hashPin("482910", salt2)

        assertNotEquals(hash1, hash2)
        assertTrue(PinHasher.verifyPin("482910", salt1, hash1))
        assertFalse(PinHasher.verifyPin("482911", salt1, hash1))
    }
}
