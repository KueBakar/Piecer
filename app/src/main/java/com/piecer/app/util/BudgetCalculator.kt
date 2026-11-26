package com.piecer.app.util

enum class BudgetStatus {
    SAFE,
    WARNING,
    EXCEEDED
}

enum class BudgetThreshold(val percent: Int) {
    REACHED_80(80),
    REACHED_100(100)
}

data class BudgetProgress(
    val spent: Long,
    val limit: Long,
    val remaining: Long,
    val percentageInt: Int,
    val progressRatio: Float,
    val status: BudgetStatus,
    val threshold: BudgetThreshold?
)

object BudgetCalculator {

    fun calculate(spent: Long, limit: Long): BudgetProgress {
        val safeSpent = spent.coerceAtLeast(0L)
        if (limit <= 0L) {
            return BudgetProgress(
                spent = safeSpent,
                limit = limit,
                remaining = 0L,
                percentageInt = 0,
                progressRatio = 0f,
                status = BudgetStatus.SAFE,
                threshold = null
            )
        }

        val percentageInt = ((safeSpent * 100L) / limit).toInt()
        val progressRatio = safeSpent.toFloat() / limit.toFloat()
        val remaining = limit - safeSpent

        val status = when {
            percentageInt >= 100 -> BudgetStatus.EXCEEDED
            percentageInt >= 70 -> BudgetStatus.WARNING
            else -> BudgetStatus.SAFE
        }

        val threshold = resolveNotificationThreshold(percentageInt)

        return BudgetProgress(
            spent = safeSpent,
            limit = limit,
            remaining = remaining,
            percentageInt = percentageInt,
            progressRatio = progressRatio,
            status = status,
            threshold = threshold
        )
    }

    fun resolveNotificationThreshold(percentageInt: Int): BudgetThreshold? = when {
        percentageInt >= 100 -> BudgetThreshold.REACHED_100
        percentageInt >= 80 -> BudgetThreshold.REACHED_80
        else -> null
    }

    fun <T> findMostCritical(
        items: List<T>,
        progressSelector: (T) -> BudgetProgress
    ): T? {
        return items.maxWithOrNull(
            compareBy<T> { progressSelector(it).percentageInt }
                .thenBy { progressSelector(it).spent }
        )
    }
}
