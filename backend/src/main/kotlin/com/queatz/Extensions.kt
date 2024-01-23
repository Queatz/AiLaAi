package com.queatz

import kotlinx.coroutines.delay
import kotlinx.datetime.*
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.minutes

val String.notBlank get() = takeIf { it.isNotBlank() }

fun <T> String.notBlank(block: (String) -> T) = notBlank?.let(block)

fun Instant.startOfSecond() = toJavaInstant().truncatedTo(ChronoUnit.SECONDS).toKotlinInstant()

fun Instant.startOfMinute() = toJavaInstant().truncatedTo(ChronoUnit.MINUTES).toKotlinInstant()

suspend fun delayUntilNextMinute() = delay(
    Clock.System.now().let { now -> (now + 1.minutes).startOfMinute() - now }
)
