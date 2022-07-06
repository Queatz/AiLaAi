package com.queatz.ailaai.extensions

import android.icu.text.RelativeDateTimeFormatter
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

fun Instant.timeAgo() = Duration.between(
    toJavaInstant(),
    Clock.System.now().toJavaInstant()
).toKotlinDuration().let {
    when {
        it < 1.minutes -> RelativeDateTimeFormatter.getInstance().format(RelativeDateTimeFormatter.Direction.PLAIN, RelativeDateTimeFormatter.AbsoluteUnit.NOW)
        it < 1.hours -> RelativeDateTimeFormatter.getInstance().format(-it.inWholeMinutes.toDouble(), RelativeDateTimeFormatter.RelativeDateTimeUnit.MINUTE)
        it < 1.days -> RelativeDateTimeFormatter.getInstance().format(-it.inWholeHours.toDouble(), RelativeDateTimeFormatter.RelativeDateTimeUnit.HOUR)
        it < 30.days -> RelativeDateTimeFormatter.getInstance().format(-it.inWholeDays.toDouble(), RelativeDateTimeFormatter.RelativeDateTimeUnit.DAY)
        it < 365.days -> RelativeDateTimeFormatter.getInstance().format(-(it.inWholeDays / 30).toDouble(), RelativeDateTimeFormatter.RelativeDateTimeUnit.MONTH)
        else -> RelativeDateTimeFormatter.getInstance().format(-(it.inWholeDays / 365).toDouble(), RelativeDateTimeFormatter.RelativeDateTimeUnit.YEAR)
    }
}!!
