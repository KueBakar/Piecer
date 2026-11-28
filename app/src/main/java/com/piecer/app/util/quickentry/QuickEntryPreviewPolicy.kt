package com.piecer.app.util.quickentry

/**
 * Menentukan apakah hasil parse Ketik cepat perlu ditampilkan di lembar pratinjau konfirmasi.
 * Entri tunggal tanpa flag cukup mengisi form utama; multi-entri atau entri ber-flag
 * harus dikonfirmasi pengguna terlebih dahulu.
 */
object QuickEntryPreviewPolicy {

    fun shouldShowPreview(result: QuickEntryParseResult): Boolean {
        if (result.entries.isEmpty()) return false
        if (result.entries.size > 1) return true
        return result.entries.any { it.flags.isNotEmpty() }
    }
}
