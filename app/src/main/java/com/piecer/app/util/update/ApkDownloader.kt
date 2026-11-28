package com.piecer.app.util.update

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.piecer.app.BuildConfig
import com.piecer.app.util.BreadcrumbLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

@Singleton
class ApkDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val MAX_REDIRECTS = 5
        private const val UPDATE_CACHE_DIR = "updates"
        private const val PARTIAL_FILE_NAME = "piecer-update.apk.part"
        private const val FINAL_FILE_NAME = "piecer-update.apk"
    }

    fun getUpdateCacheDir(): File = File(context.cacheDir, UPDATE_CACHE_DIR)

    fun clearCachedApks() {
        val dir = getUpdateCacheDir()
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.delete() }
        }
    }

    @SuppressLint("MissingPermission")
    fun isWifiConnectedOrUnknown(): Boolean {
        val hasNetworkStatePerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_NETWORK_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasNetworkStatePerm) return true
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (_: SecurityException) {
            true
        }
    }

    fun download(
        metadata: UpdateMetadata,
        wifiOnly: Boolean
    ): Flow<DownloadProgress> = flow {
        if (!UpdateUrlValidator.isAllowedUrl(metadata.apkUrl)) {
            emit(DownloadProgress.Failed("URL unduhan APK tidak diizinkan."))
            return@flow
        }
        if (wifiOnly && !isWifiConnectedOrUnknown()) {
            emit(DownloadProgress.Failed("Unduhan diatur hanya melalui Wi-Fi. Hubungkan ke Wi-Fi lalu coba lagi."))
            return@flow
        }

        val dir = getUpdateCacheDir()
        if (!dir.exists()) dir.mkdirs()
        val partFile = File(dir, PARTIAL_FILE_NAME)
        val finalFile = File(dir, FINAL_FILE_NAME)
        partFile.delete()
        finalFile.delete()

        var connection: HttpsURLConnection? = null
        try {
            connection = openAllowlistedConnection(metadata.apkUrl)
            val contentLength = connection.contentLengthLong
            val expectedTotal = if (contentLength > 0L) contentLength else metadata.sizeBytes
            if (expectedTotal > UpdateJsonParser.MAX_APK_BYTES) {
                throw IOException("Ukuran berkas APK melebihi batas yang diizinkan.")
            }

            var bytesRead = 0L
            emit(DownloadProgress.InProgress(0L, expectedTotal))

            connection.inputStream.use { input ->
                FileOutputStream(partFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var lastEmitBytes = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read <= 0) break
                        bytesRead += read
                        if (bytesRead > UpdateJsonParser.MAX_APK_BYTES) {
                            throw IOException("Ukuran unduhan melebihi batas yang diizinkan.")
                        }
                        output.write(buffer, 0, read)
                        if (bytesRead - lastEmitBytes >= 32 * 1024 || bytesRead == expectedTotal) {
                            lastEmitBytes = bytesRead
                            emit(DownloadProgress.InProgress(bytesRead, expectedTotal))
                        }
                    }
                    output.flush()
                }
            }

            if (!partFile.renameTo(finalFile)) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }
            emit(DownloadProgress.Completed(finalFile.absolutePath))
        } catch (e: CancellationException) {
            partFile.delete()
            finalFile.delete()
            emit(DownloadProgress.Cancelled)
            throw e
        } catch (e: IOException) {
            partFile.delete()
            finalFile.delete()
            emit(DownloadProgress.Failed(e.message ?: "Gagal mengunduh pembaruan."))
        } catch (t: Throwable) {
            partFile.delete()
            finalFile.delete()
            if (BuildConfig.CRASH_DIAGNOSTICS_ENABLED) {
                BreadcrumbLog.record(
                    "UPDATE",
                    "ApkDownloader gagal (${t::class.java.simpleName}): ${t.message ?: "tanpa pesan"}",
                    syncWrite = true
                )
            }
            emit(DownloadProgress.Failed("Terjadi kesalahan saat mengunduh APK."))
        } finally {
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun openAllowlistedConnection(initialUrl: String): HttpsURLConnection {
        var currentUrl = initialUrl
        repeat(MAX_REDIRECTS + 1) {
            if (!UpdateUrlValidator.isAllowedUrl(currentUrl)) {
                throw IOException("Pengalihan URL keluar dari domain tepercaya ditolak.")
            }
            val conn = (URL(currentUrl).openConnection() as? HttpsURLConnection)
                ?: throw IOException("Koneksi harus menggunakan HTTPS.")
            conn.instanceFollowRedirects = false
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "PIECER-Android/${BuildConfig.VERSION_NAME}")

            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IOException("Respons pengalihan tidak memiliki URL tujuan.")
                }
                val resolved = URL(URL(currentUrl), location).toString()
                if (!UpdateUrlValidator.isAllowedUrl(resolved)) {
                    throw IOException("Pengalihan keluar dari domain tepercaya ditolak.")
                }
                currentUrl = resolved
                return@repeat
            }

            if (code != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                throw IOException("Server merespons dengan kode $code.")
            }
            return conn
        }
        throw IOException("Terlalu banyak pengalihan URL.")
    }
}
