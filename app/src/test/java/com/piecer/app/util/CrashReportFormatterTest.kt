package com.piecer.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class CrashReportFormatterTest {

    @Test
    fun `format includes device info stack trace and up to 100 breadcrumbs`() {
        val breadcrumbs = (1..120).map { "entry-$it" }
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
            zoneId = ZoneOffset.UTC
        )

        assertTrue(formatted.contains("=== PIECER CRASH REPORT ==="))
        assertTrue(formatted.contains("Android: 8.1.0 (API 27)"))
        assertTrue(formatted.contains("Perangkat: vivo vivo 1807 (1807)"))
        assertTrue(formatted.contains("Thread: main"))
        assertTrue(formatted.contains("IllegalStateException: Test crash"))
        assertTrue(formatted.contains("Jejak Kejadian (100 terakhir)"))
        assertFalse(formatted.contains("entry-20\n"))
        assertTrue(formatted.contains("entry-21"))
        assertTrue(formatted.contains("entry-120"))
    }
}
