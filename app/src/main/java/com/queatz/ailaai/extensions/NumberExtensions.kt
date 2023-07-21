package com.queatz.ailaai.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

fun Float.approximate(unit: Int) = ceil(this / unit).times(unit).toInt()

val Int.px: Dp
    @Composable get() {
    return (this / LocalDensity.current.density).dp
}
