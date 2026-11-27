package com.piecer.app.util

import com.piecer.app.ui.components.formatRupiah

data class SplitParticipantInput(
    val name: String,
    val customAmount: Long = 0L
)

data class SplitParticipantShare(
    val name: String,
    val amount: Long
)

data class SplitBillResult(
    val subtotal: Long,
    val taxAmount: Long,
    val serviceAmount: Long,
    val grandTotal: Long,
    val shares: List<SplitParticipantShare>
)

object SplitBillCalculator {

    fun calculateGrandTotal(
        subtotal: Long,
        taxPercent: Int = 0,
        servicePercent: Int = 0
    ): Triple<Long, Long, Long> {
        val safeSub = subtotal.coerceAtLeast(0L)
        val safeTaxPct = taxPercent.coerceIn(0, 100).toLong()
        val safeSvcPct = servicePercent.coerceIn(0, 100).toLong()
        val taxAmount = (safeSub * safeTaxPct) / 100L
        val serviceAmount = (safeSub * safeSvcPct) / 100L
        return Triple(taxAmount, serviceAmount, safeSub + taxAmount + serviceAmount)
    }

    /**
     * Membagi tagihan secara rata sebagai Long penuh; sisa pembulatan (+1) dibagikan
     * adil ke peserta awal sehingga jumlah semua bagian tepat sama dengan grandTotal.
     */
    fun splitEqual(
        subtotal: Long,
        participantNames: List<String>,
        taxPercent: Int = 0,
        servicePercent: Int = 0
    ): SplitBillResult {
        val names = participantNames.mapIndexed { idx, raw ->
            raw.trim().ifEmpty { "Orang ${idx + 1}" }
        }
        val (taxAmount, serviceAmount, grandTotal) = calculateGrandTotal(
            subtotal = subtotal,
            taxPercent = taxPercent,
            servicePercent = servicePercent
        )
        if (names.isEmpty()) {
            return SplitBillResult(subtotal.coerceAtLeast(0L), taxAmount, serviceAmount, grandTotal, emptyList())
        }
        val count = names.size.toLong()
        val base = grandTotal / count
        val remainder = (grandTotal % count).toInt()
        val shares = names.mapIndexed { index, name ->
            val extra = if (index < remainder) 1L else 0L
            SplitParticipantShare(name = name, amount = base + extra)
        }
        return SplitBillResult(
            subtotal = subtotal.coerceAtLeast(0L),
            taxAmount = taxAmount,
            serviceAmount = serviceAmount,
            grandTotal = grandTotal,
            shares = shares
        )
    }

    /**
     * Membagi tagihan berdasarkan nominal sendiri per orang, lalu mendistribusikan
     * pajak & servis secara proporsional menggunakan aritmatika Long dengan sisa pembulatan adil.
     */
    fun splitCustom(
        participants: List<SplitParticipantInput>,
        taxPercent: Int = 0,
        servicePercent: Int = 0
    ): SplitBillResult {
        if (participants.isEmpty()) {
            return SplitBillResult(0L, 0L, 0L, 0L, emptyList())
        }
        val subtotal = participants.sumOf { it.customAmount.coerceAtLeast(0L) }
        val (taxAmount, serviceAmount, grandTotal) = calculateGrandTotal(
            subtotal = subtotal,
            taxPercent = taxPercent,
            servicePercent = servicePercent
        )
        if (subtotal == 0L) {
            return SplitBillResult(
                subtotal = 0L,
                taxAmount = 0L,
                serviceAmount = 0L,
                grandTotal = 0L,
                shares = participants.mapIndexed { idx, p ->
                    SplitParticipantShare(p.name.trim().ifEmpty { "Orang ${idx + 1}" }, 0L)
                }
            )
        }

        val rawShares = participants.map { p ->
            val amt = p.customAmount.coerceAtLeast(0L)
            (amt * grandTotal) / subtotal
        }.toMutableList()

        val allocated = rawShares.sum()
        var remainder = (grandTotal - allocated).toInt()
        var idx = 0
        while (remainder > 0 && rawShares.isNotEmpty()) {
            if (participants[idx].customAmount > 0L) {
                rawShares[idx] = rawShares[idx] + 1L
                remainder--
            }
            idx = (idx + 1) % rawShares.size
        }

        val shares = participants.mapIndexed { index, p ->
            SplitParticipantShare(
                name = p.name.trim().ifEmpty { "Orang ${index + 1}" },
                amount = rawShares[index]
            )
        }
        return SplitBillResult(
            subtotal = subtotal,
            taxAmount = taxAmount,
            serviceAmount = serviceAmount,
            grandTotal = grandTotal,
            shares = shares
        )
    }

    fun buildShareText(title: String, result: SplitBillResult): String {
        val sb = StringBuilder()
        val header = title.trim().ifEmpty { "Patungan" }
        sb.append(header).append('\n')
        sb.append("Total: ").append(formatRupiah(result.grandTotal)).append('\n')
        if (result.taxAmount > 0L || result.serviceAmount > 0L) {
            sb.append("(Subtotal ${formatRupiah(result.subtotal)}")
            if (result.taxAmount > 0L) sb.append(", Pajak ${formatRupiah(result.taxAmount)}")
            if (result.serviceAmount > 0L) sb.append(", Servis ${formatRupiah(result.serviceAmount)}")
            sb.append(")\n")
        }
        sb.append('\n')
        result.shares.forEach { share ->
            sb.append("- ").append(share.name).append(": ").append(formatRupiah(share.amount)).append('\n')
        }
        return sb.toString().trimEnd()
    }
}
