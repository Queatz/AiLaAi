package com.queatz.ailaai.extensions

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun Long.formatTime() = when {
    this < 1.seconds.inWholeMilliseconds -> "0s"
    this < 1.minutes.inWholeMilliseconds -> "${this.milliseconds.inWholeSeconds}s"
    this < 1.hours.inWholeMilliseconds -> "${this.milliseconds.inWholeMinutes}s ${this.milliseconds.inWholeSeconds}s"
    else -> "${this.milliseconds.inWholeHours}s ${this.milliseconds.inWholeMinutes}s"
}
