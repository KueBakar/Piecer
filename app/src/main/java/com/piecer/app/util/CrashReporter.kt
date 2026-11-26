package com.piecer.app.util

import android.content.Context
import android.os.Build
import com.piecer.app.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pemformat laporan crash agar seragam dan mudah diuji di unit test.
 */
object CrashReportFormatter {
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS z", Locale.US)

    fun format(
        timestampMillis: Long,
        androidRelease: String,
        sdkInt: Int,
        manufacturer: String,
        model: String,
        device: String,
        threadName: String,
        stackTrace: String,
        breadcrumbs: List<String>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val formattedTime = timeFormatter.format(Instant.ofEpochMilli(timestampMillis).atZone(zoneId))
        val recentBreadcrumbs = breadcrumbs.takeLast(BreadcrumbStore.DEFAULT_MAX_ENTRIES)

        return buildString {
            appendLine("=== PIECER CRASH REPORT ===")
            appendLine("Waktu: $formattedTime")
            appendLine("Android: $androidRelease (API $sdkInt)")
            appendLine("Perangkat: $manufacturer $model ($device)")
            appendLine("Thread: $threadName")
            appendLine()
            appendLine("--- Stack Trace ---")
            appendLine(stackTrace.trimEnd())
            appendLine()
            appendLine("--- Jejak Kejadian (${recentBreadcrumbs.size} terakhir) ---")
            if (recentBreadcrumbs.isEmpty()) {
                appendLine("(tidak ada jejak kejadian)")
            } else {
                recentBreadcrumbs.forEach { appendLine(it) }
            }
        }.trimEnd()
    }
}

/**
 * Pencatat uncaught exception lokal (hanya aktif di build debug).
 */
object CrashReporter {
    private const val REPORT_FILE_NAME = "debug_crash_report.txt"
    private const val UNSEEN_FLAG_NAME = "debug_crash_unseen.flag"

    @Volatile
    private var installed = false

    fun install(context: Context, enabled: Boolean = BuildConfig.DEBUG) {
        if (!enabled || installed) return
        installed = true
        val filesDir = context.filesDir
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrashReport(filesDir = filesDir, thread = thread, throwable = throwable)
            } catch (_: Throwable) {
                // Jangan pernah melempar exception dari uncaught exception handler
            } finally {
                try {
                    defaultHandler?.uncaughtException(thread, throwable)
                } catch (_: Throwable) {
                    // Abaikan kegagalan handler bawaan
                }
            }
        }
    }

    fun hasUnseenCrashReport(context: Context): Boolean {
        if (!BuildConfig.DEBUG) return false
        return runCatching {
            val flagFile = File(context.filesDir, UNSEEN_FLAG_NAME)
            val reportFile = File(context.filesDir, REPORT_FILE_NAME)
            flagFile.exists() && reportFile.exists()
        }.getOrDefault(false)
    }

    fun markCrashReportSeen(context: Context) {
        if (!BuildConfig.DEBUG) return
        runCatching {
            val flagFile = File(context.filesDir, UNSEEN_FLAG_NAME)
            if (flagFile.exists()) {
                flagFile.delete()
            }
        }
    }

    fun readCrashReport(context: Context): String? {
        if (!BuildConfig.DEBUG) return null
        return runCatching {
            val reportFile = File(context.filesDir, REPORT_FILE_NAME)
            if (reportFile.exists()) {
                reportFile.readText(Charsets.UTF_8).takeIf { it.isNotBlank() }
            } else {
                null
            }
        }.getOrNull()
    }

    fun clearCrashReport(context: Context) {
        if (!BuildConfig.DEBUG) return
        runCatching {
            File(context.filesDir, REPORT_FILE_NAME).delete()
            File(context.filesDir, UNSEEN_FLAG_NAME).delete()
        }
    }

    private fun writeCrashReport(filesDir: File, thread: Thread, throwable: Throwable) {
        val stackTrace = StringWriter().also { sw ->
            PrintWriter(sw).use { pw -> throwable.printStackTrace(pw) }
        }.toString()

        BreadcrumbLog.record(
            category = "CRASH",
            message = "${throwable::class.java.simpleName}: ${throwable.message ?: "no message"} on ${thread.name}",
            syncWrite = true
        )

        val report = CrashReportFormatter.format(
            timestampMillis = System.currentTimeMillis(),
            androidRelease = Build.VERSION.RELEASE ?: "unknown",
            sdkInt = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER ?: "unknown",
            model = Build.MODEL ?: "unknown",
            device = Build.DEVICE ?: "unknown",
            threadName = thread.name,
            stackTrace = stackTrace,
            breadcrumbs = BreadcrumbLog.entries()
        )

        if (!filesDir.exists()) {
            filesDir.mkdirs()
        }
        File(filesDir, REPORT_FILE_NAME).writeText(report, Charsets.UTF_8)
        File(filesDir, UNSEEN_FLAG_NAME).writeText("1", Charsets.UTF_8)
    }
}
