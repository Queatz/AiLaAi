package com.queatz.ailaai.extensions

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ibm.icu.text.DecimalFormat
import com.queatz.ailaai.R
import kotlin.math.ceil
import kotlin.math.roundToInt

fun Float.approximate(unit: Int) = ceil(this / unit).times(unit).toInt()

val Int.px: Dp
    @Composable get() {
    return (this / LocalDensity.current.density).dp
}

val Dp.px: Int
    @Composable get() {
        return (value * LocalDensity.current.density).roundToInt()
    }

private val decimalFormat = DecimalFormat("#,###.##")

fun Number.format() = decimalFormat.format(this)!!

@Composable
fun Int.formatMini() = when {
    this < 1_000 -> toString()
    this < 1_000_000 -> stringResource(R.string.x_thousands_mini, this / 1_000)
    this < 100_000_000 -> stringResource(R.string.x_millions_mini, this / 1_000_000)
    else -> stringResource(R.string.many)
}
