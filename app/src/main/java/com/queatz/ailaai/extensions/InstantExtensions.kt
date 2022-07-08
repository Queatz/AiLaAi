package com.queatz.ailaai.extensions

import android.icu.text.RelativeDateTimeFormatter
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.Duration
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

fun Instant.timeAgo() = Duration.between(
    toJavaInstant(),
    Clock.System.now().toJavaInstant()
).toKotlinDuration().let {
    val formatter = RelativeDateTimeFormatter.getInstance(AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault())

    when {
        it < 1.minutes -> formatter.format(RelativeDateTimeFormatter.Direction.PLAIN, RelativeDateTimeFormatter.AbsoluteUnit.NOW)
        it < 1.hours -> formatter.format(-it.inWholeMinutes.toDouble(), RelativeDateTimeFormatter.RelativeDateTimeUnit.MINUTE)
        it < 1.days -> formatter.format(-it.inWholeHours.toDouble(), RelativeDateTimeFormatter.RelativeDateTimeUnit.HOUR)
        it < 30.days -> formatter.format(-it.inWholeDays.toDouble(), RelativeDateTimeFormatter.RelativeDateTimeUnit.DAY)
        it < 365.days -> formatter.format(-(it.inWholeDays / 30).toDouble(), RelativeDateTimeFormatter.RelativeDateTimeUnit.MONTH)
        else -> formatter.format(-(it.inWholeDays / 365).toDouble(), RelativeDateTimeFormatter.RelativeDateTimeUnit.YEAR)
    }
}!!
