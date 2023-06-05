package com.queatz.ailaai.extensions

import kotlin.math.ceil

fun Float.approximate(unit: Int) = ceil(this / unit).times(unit).toInt()
