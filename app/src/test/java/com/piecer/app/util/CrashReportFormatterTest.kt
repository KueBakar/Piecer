package com.piecer.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.ZoneOffset

class CrashReportFormatterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `format includes app version build type device info stack trace and up to 50 breadcrumbs`() {
        val breadcrumbs = (1..80).map { "entry-$it" }
        val formatted = CrashReportFormatter.format(
            timestampMillis = 1_700_000_000_000L,
            androidRelease = "8.1.0",
            sdkInt = 27,
            manufacturer = "vivo",
            model = "vivo 1807",
            device = "1807",
            threadName = "main",
            stackTrace = "java.lang.IllegalStateException: Test crash\n\tat com.piecer.app.MainActivity.onCreate(MainActivity.kt:1)",
            breadcrumbs = breadcrumbs,
            appVersion = "1.0.0-staging (1)",
            buildType = "staging",
            zoneId = ZoneOffset.UTC
        )

        assertTrue(formatted.contains("=== PIECER CRASH REPORT ==="))
        assertTrue(formatted.contains("Versi App: 1.0.0-staging (1) (staging)"))
        assertTrue(formatted.contains("Android: 8.1.0 (API 27)"))
        assertTrue(formatted.contains("Perangkat: vivo vivo 1807 (1807)"))
        assertTrue(formatted.contains("Thread: main"))
        assertTrue(formatted.contains("IllegalStateException: Test crash"))
        assertTrue(formatted.contains("Jejak Kejadian (50 terakhir)"))
        assertFalse(formatted.contains("entry-30\n"))
        assertTrue(formatted.contains("entry-31"))
        assertTrue(formatted.contains("entry-80"))
    }

    @Test
    fun `writeCrashReportToDir preserves full caused by chain synchronously`() {
        val rootCause = IllegalArgumentException("Root cause failure")
        val middleCause = IllegalStateException("Middle failure", rootCause)
        val topException = RuntimeException("Top failure", middleCause)

        val reportFile = CrashReporter.writeCrashReportToDir(
            filesDir = tempFolder.root,
            thread = Thread("worker-1"),
            throwable = topException,
            breadcrumbs = listOf("[2026-11-28] [PROCESS] Cold start"),
            appVersion = "1.0.0 (1)",
            buildType = "release"
        )

        val content = reportFile.readText(Charsets.UTF_8)
        assertTrue(content.contains("Versi App: 1.0.0 (1) (release)"))
        assertTrue(content.contains("Thread: worker-1"))
        assertTrue(content.contains("RuntimeException: Top failure"))
        assertTrue(content.contains("Caused by: java.lang.IllegalStateException: Middle failure"))
        assertTrue(content.contains("Caused by: java.lang.IllegalArgumentException: Root cause failure"))
        assertTrue(content.contains("[PROCESS] Cold start"))
    }
}
