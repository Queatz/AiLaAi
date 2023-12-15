package com.queatz

import kotlinx.datetime.*
import java.time.temporal.ChronoUnit

val String.notBlank get() = takeIf { it.isNotBlank() }

fun <T> String.notBlank(block: (String) -> T) = notBlank?.let(block)

fun Instant.startOfSecond() = toJavaInstant().truncatedTo(ChronoUnit.SECONDS).toKotlinInstant()
