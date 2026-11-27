package com.piecer.app.util.update

import android.os.Build
import com.piecer.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

@Singleton
class UpdateChecker @Inject constructor() {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val MAX_REDIRECTS = 5
    }

    suspend fun checkForUpdate(
        updateJsonUrl: String = AppLinks.UPDATE_JSON_URL,
        currentVersionCode: Int = BuildConfig.VERSION_CODE,
        currentSdkInt: Int = Build.VERSION.SDK_INT,
        isUpdateEnabled: Boolean = BuildConfig.UPDATE_ENABLED
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!isUpdateEnabled) {
            return@withContext UpdateCheckResult.Error("Pemeriksaan pembaruan tidak aktif pada build ini.")
        }
        if (!UpdateUrlValidator.isAllowedUrl(updateJsonUrl)) {
            return@withContext UpdateCheckResult.Error("URL pembaruan tidak valid atau belum dikonfigurasi.")
        }

        try {
            val rawJson = fetchJsonWithAllowlistedRedirects(updateJsonUrl)
            UpdateJsonParser.parseAndValidate(
                rawJson = rawJson,
                currentVersionCode = currentVersionCode,
                currentSdkInt = currentSdkInt,
                maxJsonBytes = UpdateJsonParser.MAX_JSON_BYTES
            )
        } catch (e: RedirectNotAllowedException) {
            UpdateCheckResult.Error(e.message ?: "Pengalihan URL keluar dari domain tepercaya ditolak.")
        } catch (e: PayloadTooLargeException) {
            UpdateCheckResult.Error(e.message ?: "Ukuran metadata pembaruan melebihi batas.")
        } catch (e: IOException) {
            UpdateCheckResult.Error("Tidak dapat terhubung ke server pembaruan. Periksa koneksi internet Anda.")
        } catch (_: Exception) {
            UpdateCheckResult.Error("Terjadi kesalahan saat memeriksa pembaruan.")
        }
    }

    private fun fetchJsonWithAllowlistedRedirects(initialUrl: String): String {
        var currentUrl = initialUrl
        repeat(MAX_REDIRECTS + 1) {
            if (!UpdateUrlValidator.isAllowedUrl(currentUrl)) {
                throw RedirectNotAllowedException("URL tidak termasuk dalam daftar host yang diizinkan.")
            }
            val connection = (URL(currentUrl).openConnection() as? HttpsURLConnection)
                ?: throw IOException("Koneksi harus menggunakan HTTPS.")
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "PIECER-Android/${BuildConfig.VERSION_NAME}")

                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("Respons pengalihan tidak memiliki tujuan.")
                    val resolved = URL(URL(currentUrl), location).toString()
                    if (!UpdateUrlValidator.isAllowedUrl(resolved)) {
                        throw RedirectNotAllowedException("Pengalihan keluar dari domain tepercaya ditolak.")
                    }
                    currentUrl = resolved
                    return@repeat
                }

                if (code != HttpURLConnection.HTTP_OK) {
                    throw IOException("Server pembaruan merespons dengan kode $code.")
                }

                val contentLength = connection.contentLengthLong
                if (contentLength > UpdateJsonParser.MAX_JSON_BYTES) {
                    throw PayloadTooLargeException("Ukuran metadata pembaruan melebihi 64 KB.")
                }

                return connection.inputStream.use { input ->
                    val buffer = ByteArray(4096)
                    val out = ByteArrayOutputStream()
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        total += read
                        if (total > UpdateJsonParser.MAX_JSON_BYTES) {
                            throw PayloadTooLargeException("Ukuran metadata pembaruan melebihi 64 KB.")
                        }
                        out.write(buffer, 0, read)
                    }
                    out.toString(Charsets.UTF_8.name())
                }
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Terlalu banyak pengalihan URL.")
    }

    private class RedirectNotAllowedException(message: String) : IOException(message)
    private class PayloadTooLargeException(message: String) : IOException(message)
}
