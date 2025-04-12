package com.queatz.ailaai.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ibm.icu.text.DecimalFormat
import com.queatz.ailaai.R
import kotlin.div
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.text.compareTo

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

@Composable
fun Number.formatDistance() = toInt().let {
    when {
        it < 1_000 -> pluralStringResource(R.plurals.x_m, it, it.format())
        else -> pluralStringResource(R.plurals.x_km, it / 1_000, (it / 1_000).format())
    }
}
