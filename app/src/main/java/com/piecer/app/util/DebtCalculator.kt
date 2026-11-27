package com.piecer.app.util

import com.piecer.app.data.local.entity.DebtDirection
import com.piecer.app.data.local.entity.DebtEntity
import com.piecer.app.ui.components.formatRupiah
import java.time.Instant
import java.time.ZoneId

object DebtCalculator {

    fun remainingAmount(debt: DebtEntity): Long =
        (debt.totalAmount - debt.paidAmount).coerceAtLeast(0L)

    fun isPaidOff(debt: DebtEntity): Boolean = remainingAmount(debt) == 0L

    fun progress(debt: DebtEntity): Float {
        if (debt.totalAmount <= 0L) return 1f
        return (debt.paidAmount.toFloat() / debt.totalAmount.toFloat()).coerceIn(0f, 1f)
    }

    fun totalRemainingByDirection(debts: List<DebtEntity>, direction: DebtDirection): Long =
        debts.filter { it.direction == direction }.sumOf { remainingAmount(it) }

    fun isOverdue(
        debt: DebtEntity,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        val due = debt.dueDate ?: return false
        if (isPaidOff(debt)) return false
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val dueLocal = Instant.ofEpochMilli(due).atZone(zoneId).toLocalDate()
        return dueLocal.isBefore(today)
    }

    fun buildPoliteReminderText(debt: DebtEntity, dueDateText: String?): String {
        val remaining = formatRupiah(remainingAmount(debt))
        val duePart = if (!dueDateText.isNullOrBlank()) " dengan jatuh tempo $dueDateText" else ""
        val notePart = if (debt.note.isNotBlank()) " (${debt.note})" else ""
        return if (debt.direction == DebtDirection.OWE_ME) {
            "Halo ${debt.personName}, izin mengingatkan dengan ramah ya terkait catatan piutang$notePart sebesar $remaining$duePart. Terima kasih banyak!"
        } else {
            "Halo ${debt.personName}, saya ingin mengonfirmasi terkait catatan hutang$notePart sebesar $remaining$duePart. Terima kasih banyak!"
        }
    }
}
