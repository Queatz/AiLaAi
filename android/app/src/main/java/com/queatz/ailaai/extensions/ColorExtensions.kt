package com.queatz.ailaai.extensions

import androidx.compose.ui.graphics.Color

val Color.hex get() = (0xFFFFFFUL and (value shr (ULong.SIZE_BITS / 2)))
    .toString(16)
    .padStart(6, '0')
    .let { "#$it" }
