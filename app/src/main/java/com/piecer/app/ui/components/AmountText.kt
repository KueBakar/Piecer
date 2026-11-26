package com.piecer.app.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.piecer.app.R
import com.piecer.app.ui.theme.PiecerTheme
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

enum class AmountTone {
    Neutral,
    Income,
    Expense
}

/**
 * Format bilangan bulat rupiah (Long) ke format Indonesia: "Rp1.250.000".
 * Mengikuti aturan proyek: uang SELALU disimpan sebagai Long (rupiah penuh).
 */
fun formatRupiah(amount: Long): String {
    val symbols = DecimalFormatSymbols(Locale("in", "ID")).apply {
        groupingSeparator = '.'
    }
    val formatter = DecimalFormat("#,###", symbols)
    val formatted = formatter.format(abs(amount))
    return if (amount < 0L) "-Rp$formatted" else "Rp$formatted"
}

@Composable
fun AmountText(
    amount: Long,
    modifier: Modifier = Modifier,
    isHidden: Boolean = false,
    tone: AmountTone = AmountTone.Neutral,
    style: TextStyle = MaterialTheme.typography.headlineLarge
) {
    val colors = PiecerTheme.colors
    val textColor = when (tone) {
        AmountTone.Neutral -> LocalContentColor.current
        AmountTone.Income -> colors.mint
        AmountTone.Expense -> colors.coral
    }

    val displayText = if (isHidden) {
        stringResource(id = R.string.amount_hidden)
    } else {
        formatRupiah(amount)
    }

    Text(
        text = displayText,
        modifier = modifier,
        color = textColor,
        style = style
    )
}
