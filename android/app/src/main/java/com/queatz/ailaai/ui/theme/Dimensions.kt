package com.queatz.ailaai.ui.theme

import androidx.compose.ui.unit.dp

private val ElevationDefault = 8.dp
private val PaddingDefault = 8.dp

val Int.pad get() = PaddingDefault * this
val Float.pad get() = PaddingDefault * this

val Int.elevation get() = ElevationDefault * this
val Float.elevation get() = ElevationDefault * this
