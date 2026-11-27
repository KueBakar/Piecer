package com.piecer.app.util

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Menyimpan buffer ring jejak kejadian (maksimal [maxEntries] entri terakhir)
 * ke file kecil secara sinkron agar tidak hilang saat proses dimatikan mendadak.
 */
class BreadcrumbStore(
    private val file: File?,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    private val lock = Any()
    private val buffer = ArrayDeque<String>()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    init {
        require(maxEntries > 0) { "maxEntries harus lebih dari 0" }
        loadFromFile()
    }

    fun record(category: String, message: String, syncWrite: Boolean = true) {
        val sanitizedCategory = sanitize(category).ifBlank { "EVENT" }
        val sanitizedMessage = sanitize(message)
        val timestamp = formatter.format(Instant.ofEpochMilli(clock()).atZone(zoneId))
        val entry = "[$timestamp] [$sanitizedCategory] $sanitizedMessage"

        synchronized(lock) {
            buffer.addLast(entry)
            while (buffer.size > maxEntries) {
                buffer.removeFirst()
            }
            if (syncWrite) {
                persistLocked()
            }
        }
    }

    fun flushSync() {
        synchronized(lock) {
            persistLocked()
        }
    }

    fun entries(): List<String> = synchronized(lock) {
        buffer.toList()
    }

    fun formatted(): String = synchronized(lock) {
        buffer.joinToString(separator = "\n")
    }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            runCatching {
                if (file != null && file.exists()) {
                    file.delete()
                }
            }
        }
    }

    private fun loadFromFile() {
        val target = file ?: return
        synchronized(lock) {
            runCatching {
                if (!target.exists()) return
                val lines = target.readLines(Charsets.UTF_8)
                    .filter { it.isNotBlank() }
                val startIndex = (lines.size - maxEntries).coerceAtLeast(0)
                for (i in startIndex until lines.size) {
                    buffer.addLast(lines[i])
                }
            }
        }
    }

    private fun persistLocked() {
        val target = file ?: return
        runCatching {
            target.parentFile?.let { parent ->
                if (!parent.exists()) {
                    parent.mkdirs()
                }
            }
            target.writeText(buffer.joinToString(separator = "\n"), Charsets.UTF_8)
        }
    }

    private fun sanitize(text: String): String {
        return text.replace('\r', ' ').replace('\n', ' ').trim()
    }

    companion object {
        const val DEFAULT_MAX_ENTRIES = 100
    }
}

/**
 * Pencatat jejak kejadian global (aktif di build diagnostik).
 * Penulisan kejadian biasa dijalankan di executor satu-thread agar tidak memblokir main thread;
 * hanya flush sinkron saat terjadi crash.
 */
object BreadcrumbLog {
    private const val FILE_NAME = "debug_breadcrumbs.log"

    private val ioExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "piecer-breadcrumb-io").apply { isDaemon = true }
    }

    @Volatile
    var isEnabled: Boolean = false
        private set

    @Volatile
    private var store: BreadcrumbStore? = null

    fun init(filesDir: File, enabled: Boolean) {
        isEnabled = enabled
        if (enabled) {
            ioExecutor.execute {
                runCatching {
                    store = BreadcrumbStore(File(filesDir, FILE_NAME))
                }
            }
        } else {
            store = null
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun record(category: String, message: String, syncWrite: Boolean = false) {
        if (!isEnabled) return
        ioExecutor.execute {
            runCatching {
                store?.record(category = category, message = message, syncWrite = true)
            }
        }
    }

    fun recordCrashAndFlushSync(category: String, message: String) {
        if (!isEnabled) return
        runCatching {
            store?.record(category = category, message = message, syncWrite = true)
            store?.flushSync()
        }
    }

    fun entries(): List<String> {
        if (!isEnabled) return emptyList()
        return store?.entries().orEmpty()
    }

    fun formatted(): String {
        if (!isEnabled) return ""
        return store?.formatted().orEmpty()
    }

    fun clear() {
        ioExecutor.execute {
            runCatching {
                store?.clear()
            }
        }
    }
}
