package com.piecer.app.widget

import com.piecer.app.util.RupiahUtils

object WidgetPrivacyPolicy {
    const val MASKED_AMOUNT = "Rp••••"

    fun shouldHideAmounts(
        isBalanceHidden: Boolean,
        isPinEnabled: Boolean,
        biometricEnabled: Boolean
    ): Boolean = isBalanceHidden || isPinEnabled || biometricEnabled

    fun formatAmount(amount: Long, hideAmounts: Boolean): String {
        return if (hideAmounts) MASKED_AMOUNT else RupiahUtils.format(amount)
    }
}
