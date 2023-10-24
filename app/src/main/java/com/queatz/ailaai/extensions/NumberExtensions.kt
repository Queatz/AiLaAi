package com.queatz.ailaai.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ibm.icu.text.DecimalFormat
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
