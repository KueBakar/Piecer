package com.piecer.app.util.update

import java.net.URI
import java.net.URL
import java.util.Locale

/**
 * Validasi allowlist URL dan host pembaruan sebagai lapisan pertahanan tambahan.
 * Pengaman utama tetap verifikasi SHA-256 dan tanda tangan APK di [ApkVerifier].
 */
object UpdateUrlValidator {

    private val exactAllowedHosts = setOf(
        "github.com",
        "githubusercontent.com"
    )

    private val ipv4Regex = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")
    private val digitsOnlyRegex = Regex("^\\d+$")

    sealed interface RedirectValidation {
        data class Allowed(val resolvedUrl: String) : RedirectValidation
        data class Rejected(val rejectedHost: String?) : RedirectValidation
        data object InvalidLocation : RedirectValidation
    }

    fun isAllowedUrl(urlString: String, allowGithubIo: Boolean = true): Boolean {
        if (urlString.isBlank()) return false
        val uri = try {
            URI(urlString.trim())
        } catch (_: Exception) {
            return false
        }
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return false
        if (scheme != "https") return false
        if (uri.rawUserInfo != null || uri.userInfo != null || uri.rawAuthority?.contains('@') == true) {
            return false
        }
        if (uri.port != -1 && uri.port != 443) {
            return false
        }
        val host = uri.host?.lowercase(Locale.ROOT)?.trimEnd('.') ?: return false
        return isAllowedHost(host, allowGithubIo = allowGithubIo)
    }

    fun isAllowedHost(host: String, allowGithubIo: Boolean = true): Boolean {
        val normalized = host.lowercase(Locale.ROOT).trimEnd('.')
        if (normalized.isEmpty() || normalized.contains("..")) return false
        if (isIpLiteral(normalized)) return false
        if (normalized in exactAllowedHosts) return true
        if (normalized.endsWith(".githubusercontent.com") &&
            normalized.length > ".githubusercontent.com".length &&
            !normalized.startsWith(".")
        ) {
            return true
        }
        if (allowGithubIo &&
            normalized.endsWith(".github.io") &&
            normalized.length > ".github.io".length &&
            !normalized.startsWith(".")
        ) {
            return true
        }
        return false
    }

    fun extractHost(urlString: String): String? {
        if (urlString.isBlank()) return null
        return try {
            URI(urlString.trim()).host?.lowercase(Locale.ROOT)?.trimEnd('.')?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    fun resolveAndValidateRedirect(
        currentUrl: String,
        location: String?,
        allowGithubIo: Boolean = true
    ): RedirectValidation {
        if (location.isNullOrBlank()) return RedirectValidation.InvalidLocation
        val resolved = try {
            URL(URL(currentUrl), location.trim()).toString()
        } catch (_: Exception) {
            return RedirectValidation.InvalidLocation
        }
        return if (isAllowedUrl(resolved, allowGithubIo = allowGithubIo)) {
            RedirectValidation.Allowed(resolved)
        } else {
            RedirectValidation.Rejected(extractHost(resolved))
        }
    }

    private fun isIpLiteral(host: String): Boolean {
        if (host.startsWith("[") || host.endsWith("]") || host.contains(':')) return true
        if (ipv4Regex.matches(host)) return true
        if (digitsOnlyRegex.matches(host)) return true
        return false
    }
}
