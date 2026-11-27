package com.piecer.app.ui.components

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.piecer.app.R
import com.piecer.app.ui.theme.LocalReduceMotion
import com.piecer.app.ui.theme.PiecerTheme
import com.piecer.app.ui.theme.PlusJakartaSans
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private val LongToVector: TwoWayConverter<Long, AnimationVector1D> = TwoWayConverter(
    convertToVector = { AnimationVector1D(it.toFloat()) },
    convertFromVector = { it.value.roundToLong() }
)

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
    style: TextStyle = MaterialTheme.typography.headlineLarge,
    animate: Boolean = false
) {
    val colors = PiecerTheme.colors
    val reduceMotion = LocalReduceMotion.current
    val animatedAmount by animateValueAsState(
        targetValue = amount,
        typeConverter = LongToVector,
        animationSpec = tween(durationMillis = if (!animate || reduceMotion) 0 else 250),
        label = "amount_animation"
    )
    val displayAmount = if (animate && !reduceMotion && !isHidden) animatedAmount else amount

    val textColor = when (tone) {
        AmountTone.Neutral -> LocalContentColor.current
        AmountTone.Income -> colors.mint
        AmountTone.Expense -> colors.coral
    }

    val displayText = if (isHidden) {
        stringResource(id = R.string.amount_hidden)
    } else {
        formatRupiah(displayAmount)
    }

    val baseFontSize = if (style.fontSize != TextUnit.Unspecified) style.fontSize else 16.sp
    var scaledFontSize by remember(amount, isHidden, baseFontSize) {
        mutableStateOf(baseFontSize)
    }

    Text(
        text = displayText,
        modifier = modifier,
        color = textColor,
        style = style.copy(
            fontFamily = style.fontFamily ?: PlusJakartaSans,
            fontSize = scaledFontSize
        ),
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            if (result.didOverflowWidth && scaledFontSize > 10.sp) {
                val nextSize = (scaledFontSize.value * 0.9f).coerceAtLeast(10f)
                scaledFontSize = nextSize.sp
            }
        }
    )
}
