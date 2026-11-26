package com.piecer.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.ZoneOffset

class BreadcrumbLogTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `record keeps at most 100 entries in chronological order`() {
        val logFile = File(tempFolder.root, "breadcrumbs.log")
        var currentMillis = 1_700_000_000_000L
        val store = BreadcrumbStore(
            file = logFile,
            clock = { currentMillis++ },
            zoneId = ZoneOffset.UTC
        )

        for (i in 1..150) {
            store.record("TEST", "event-$i", syncWrite = false)
        }

        val entries = store.entries()
        assertEquals(100, entries.size)
        assertTrue(entries.first().endsWith("] [TEST] event-51"))
        assertTrue(entries.last().endsWith("] [TEST] event-150"))
    }

    @Test
    fun `record serializes synchronously and reloads from file`() {
        val logFile = File(tempFolder.root, "breadcrumbs_sync.log")
        val writer = BreadcrumbStore(
            file = logFile,
            clock = { 1_700_000_000_000L },
            zoneId = ZoneOffset.UTC
        )

        writer.record("PROCESS", "Cold start\nline2", syncWrite = true)
        writer.record("BIOMETRIC", "launchBiometricPrompt", syncWrite = true)

        assertTrue(logFile.exists())

        val reloaded = BreadcrumbStore(
            file = logFile,
            clock = { 1_700_000_000_000L },
            zoneId = ZoneOffset.UTC
        )
        val entries = reloaded.entries()
        assertEquals(2, entries.size)
        assertTrue(entries[0].contains("[PROCESS] Cold start line2"))
        assertTrue(entries[1].contains("[BIOMETRIC] launchBiometricPrompt"))
    }

    @Test
    fun `clear removes entries and deletes file`() {
        val logFile = File(tempFolder.root, "breadcrumbs_clear.log")
        val store = BreadcrumbStore(
            file = logFile,
            clock = { 1_700_000_000_000L },
            zoneId = ZoneOffset.UTC
        )

        store.record("LOCK", "State=LOCKED", syncWrite = true)
        assertTrue(logFile.exists())

        store.clear()
        assertTrue(store.entries().isEmpty())
        assertFalse(logFile.exists())
    }
}
