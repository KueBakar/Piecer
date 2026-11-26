package com.piecer.app.util

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class GoalProgress(
    val savedAmount: Long,
    val targetAmount: Long,
    val remainingAmount: Long,
    val ratio: Float,
    val percentageInt: Int,
    val isAchieved: Boolean,
    val estimatedMonthlySaving: Long?
)

object GoalCalculator {

    fun calculate(
        savedAmount: Long,
        targetAmount: Long,
        deadlineMillis: Long?,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): GoalProgress {
        val safeTarget = targetAmount.coerceAtLeast(0L)
        val safeSaved = savedAmount.coerceAtLeast(0L)
        val remaining = (safeTarget - safeSaved).coerceAtLeast(0L)
        val isAchieved = safeTarget > 0L && safeSaved >= safeTarget
        val ratio = if (safeTarget <= 0L) 0f else safeSaved.toFloat() / safeTarget.toFloat()
        val percentageInt = if (safeTarget <= 0L) 0 else ((safeSaved * 100L) / safeTarget).toInt()
        val monthly = estimateMonthlySaving(
            remainingAmount = remaining,
            deadlineMillis = deadlineMillis,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        return GoalProgress(
            savedAmount = safeSaved,
            targetAmount = safeTarget,
            remainingAmount = remaining,
            ratio = ratio,
            percentageInt = percentageInt,
            isAchieved = isAchieved,
            estimatedMonthlySaving = monthly
        )
    }

    fun estimateMonthlySaving(
        remainingAmount: Long,
        deadlineMillis: Long?,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        if (deadlineMillis == null || remainingAmount <= 0L) return null
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val deadlineDate = Instant.ofEpochMilli(deadlineMillis).atZone(zoneId).toLocalDate()
        val daysRemaining = ChronoUnit.DAYS.between(today, deadlineDate)
        val monthsRemaining = if (daysRemaining <= 0L) {
            1L
        } else {
            ((daysRemaining + 29L) / 30L).coerceAtLeast(1L)
        }
        return (remainingAmount + monthsRemaining - 1L) / monthsRemaining
    }
}
