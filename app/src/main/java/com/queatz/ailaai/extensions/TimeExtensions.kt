package com.queatz.ailaai.extensions

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun Long.formatTime() = when {
    this < 1.seconds.inWholeMilliseconds -> "0s"
    this < 1.minutes.inWholeMilliseconds -> "${milliseconds.inWholeSeconds}s"
    this < 1.hours.inWholeMilliseconds -> "${milliseconds.inWholeMinutes}m ${milliseconds.inWholeSeconds - milliseconds.inWholeMinutes.minutes.inWholeSeconds}s"
    else -> "${milliseconds.inWholeHours}h ${milliseconds.inWholeMinutes - milliseconds.inWholeHours.hours.inWholeMinutes}m"
}
