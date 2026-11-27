package com.piecer.app.util.update

import java.net.URI
import java.util.Locale

object UpdateUrlValidator {

    private val exactAllowedHosts = setOf(
        "github.com",
        "objects.githubusercontent.com",
        "githubusercontent.com"
    )

    fun isAllowedUrl(urlString: String): Boolean {
        if (urlString.isBlank()) return false
        val uri = try {
            URI(urlString.trim())
        } catch (_: Exception) {
            return false
        }
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return false
        if (scheme != "https") return false
        if (!uri.userInfo.isNullOrEmpty()) return false
        val host = uri.host?.lowercase(Locale.ROOT)?.trimEnd('.') ?: return false
        return isAllowedHost(host)
    }

    fun isAllowedHost(host: String): Boolean {
        val normalized = host.lowercase(Locale.ROOT).trimEnd('.')
        if (normalized.isEmpty()) return false
        if (normalized in exactAllowedHosts) return true
        return normalized.endsWith(".github.io") && normalized.length > ".github.io".length
    }
}
