package com.piecer.app.util

import android.content.Context
import android.os.Build
import android.os.Process
import com.piecer.app.BuildConfig
import com.piecer.app.ui.crash.CrashActivity
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/**
 * Pemformat laporan crash agar seragam dan mudah diuji di unit test.
 */
object CrashReportFormatter {
    const val MAX_REPORT_BREADCRUMBS = 50
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
        appVersion: String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        buildType: String = BuildConfig.BUILD_TYPE,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val formattedTime = timeFormatter.format(Instant.ofEpochMilli(timestampMillis).atZone(zoneId))
        val recentBreadcrumbs = breadcrumbs.takeLast(MAX_REPORT_BREADCRUMBS)

        return buildString {
            appendLine("=== PIECER CRASH REPORT ===")
            appendLine("Waktu: $formattedTime")
            appendLine("Versi App: $appVersion ($buildType)")
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
 * Pencatat uncaught exception lokal (aktif di semua tipe build termasuk release).
 */
object CrashReporter {
    private const val REPORT_FILE_NAME = "debug_crash_report.txt"
    private const val UNSEEN_FLAG_NAME = "debug_crash_unseen.flag"

    @Volatile
    private var installed = false

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    private val handlingCrash = AtomicBoolean(false)

    fun install(context: Context) {
        if (installed) return
        installed = true
        appContext = context.applicationContext ?: context
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            reportUncaughtException(thread = thread, throwable = throwable)
        }
    }

    fun reportUncaughtException(thread: Thread, throwable: Throwable) {
        val previousHandler = defaultHandler
        if (!handlingCrash.compareAndSet(false, true)) {
            previousHandler?.uncaughtException(thread, throwable)
            return
        }

        val ctx = appContext
        if (ctx == null) {
            previousHandler?.uncaughtException(thread, throwable)
            return
        }

        try {
            writeCrashReport(filesDir = ctx.filesDir, thread = thread, throwable = throwable)
            ctx.startActivity(CrashActivity.createLaunchIntent(ctx))
            Process.killProcess(Process.myPid())
            exitProcess(10)
        } catch (_: Throwable) {
            // Cegah loop: bila handler sendiri gagal, teruskan ke handler bawaan
            try {
                previousHandler?.uncaughtException(thread, throwable)
            } catch (_: Throwable) {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }

    fun hasUnseenCrashReport(context: Context): Boolean {
        return runCatching {
            val flagFile = File(context.filesDir, UNSEEN_FLAG_NAME)
            val reportFile = File(context.filesDir, REPORT_FILE_NAME)
            flagFile.exists() && reportFile.exists()
        }.getOrDefault(false)
    }

    fun markCrashReportSeen(context: Context) {
        runCatching {
            val flagFile = File(context.filesDir, UNSEEN_FLAG_NAME)
            if (flagFile.exists()) {
                flagFile.delete()
            }
        }
    }

    fun readCrashReport(context: Context): String? {
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
        runCatching {
            File(context.filesDir, REPORT_FILE_NAME).delete()
            File(context.filesDir, UNSEEN_FLAG_NAME).delete()
        }
    }

    internal fun formatThrowableStackTrace(throwable: Throwable): String {
        return StringWriter().also { sw ->
            PrintWriter(sw).use { pw -> throwable.printStackTrace(pw) }
        }.toString()
    }

    internal fun writeCrashReportToDir(
        filesDir: File,
        thread: Thread,
        throwable: Throwable,
        breadcrumbs: List<String>,
        appVersion: String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        buildType: String = BuildConfig.BUILD_TYPE
    ): File {
        val stackTrace = formatThrowableStackTrace(throwable)
        val report = CrashReportFormatter.format(
            timestampMillis = System.currentTimeMillis(),
            androidRelease = Build.VERSION.RELEASE ?: "unknown",
            sdkInt = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER ?: "unknown",
            model = Build.MODEL ?: "unknown",
            device = Build.DEVICE ?: "unknown",
            threadName = thread.name,
            stackTrace = stackTrace,
            breadcrumbs = breadcrumbs,
            appVersion = appVersion,
            buildType = buildType
        )

        if (!filesDir.exists()) {
            filesDir.mkdirs()
        }
        val reportFile = File(filesDir, REPORT_FILE_NAME)
        reportFile.writeText(report, Charsets.UTF_8)
        File(filesDir, UNSEEN_FLAG_NAME).writeText("1", Charsets.UTF_8)
        return reportFile
    }

    private fun writeCrashReport(filesDir: File, thread: Thread, throwable: Throwable) {
        BreadcrumbLog.recordCrashAndFlushSync(
            category = "CRASH",
            message = "${throwable::class.java.simpleName}: ${throwable.message ?: "no message"} on ${thread.name}"
        )
        writeCrashReportToDir(
            filesDir = filesDir,
            thread = thread,
            throwable = throwable,
            breadcrumbs = BreadcrumbLog.entries()
        )
    }
}
