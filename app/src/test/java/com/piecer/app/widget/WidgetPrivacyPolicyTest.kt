package com.piecer.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPrivacyPolicyTest {

    @Test
    fun shouldHideAmounts_falseOnlyWhenAllPrivacyFlagsOff() {
        assertFalse(
            WidgetPrivacyPolicy.shouldHideAmounts(
                isBalanceHidden = false,
                isPinEnabled = false,
                biometricEnabled = false
            )
        )
    }

    @Test
    fun shouldHideAmounts_trueWhenBalanceHiddenOrPinOrBiometricActive() {
        assertTrue(
            WidgetPrivacyPolicy.shouldHideAmounts(
                isBalanceHidden = true,
                isPinEnabled = false,
                biometricEnabled = false
            )
        )
        assertTrue(
            WidgetPrivacyPolicy.shouldHideAmounts(
                isBalanceHidden = false,
                isPinEnabled = true,
                biometricEnabled = false
            )
        )
        assertTrue(
            WidgetPrivacyPolicy.shouldHideAmounts(
                isBalanceHidden = false,
                isPinEnabled = false,
                biometricEnabled = true
            )
        )
    }

    @Test
    fun formatAmount_masksWhenHiddenAndFormatsWhenVisible() {
        assertEquals(WidgetPrivacyPolicy.MASKED_AMOUNT, WidgetPrivacyPolicy.formatAmount(125_000L, true))
        assertEquals("Rp125.000", WidgetPrivacyPolicy.formatAmount(125_000L, false))
    }
}
