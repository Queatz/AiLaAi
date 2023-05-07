package com.queatz.ailaai.extensions

import android.icu.text.RelativeDateTimeFormatter
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

fun Instant.monthYear() = DateTimeFormatter.ofPattern("MMM yyyy")
    .format(toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())!!

fun Instant.dayMonthYear() = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    .format(toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())!!

fun Instant.timeAgo() = Duration.between(
    toJavaInstant(),
    Clock.System.now().toJavaInstant()
).toKotlinDuration().let {
    val formatter = RelativeDateTimeFormatter.getInstance(AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault())

    if (false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        when {
            it < 1.minutes -> formatter.format(
                RelativeDateTimeFormatter.Direction.PLAIN,
                RelativeDateTimeFormatter.AbsoluteUnit.NOW
            )

            it < 1.hours -> formatter.format(
                -it.inWholeMinutes.toDouble(),
                RelativeDateTimeFormatter.RelativeDateTimeUnit.MINUTE
            )

            it < 1.days -> formatter.format(
                -it.inWholeHours.toDouble(),
                RelativeDateTimeFormatter.RelativeDateTimeUnit.HOUR
            )

            it < 30.days -> formatter.format(
                -it.inWholeDays.toDouble(),
                RelativeDateTimeFormatter.RelativeDateTimeUnit.DAY
            )

            it < 365.days -> formatter.format(
                -(it.inWholeDays / 30).toDouble(),
                RelativeDateTimeFormatter.RelativeDateTimeUnit.MONTH
            )

            else -> formatter.format(
                -(it.inWholeDays / 365).toDouble(),
                RelativeDateTimeFormatter.RelativeDateTimeUnit.YEAR
            )
        }
    } else {
        when {
            it <1.minutes -> formatter.format(
                RelativeDateTimeFormatter.Direction.PLAIN,
                RelativeDateTimeFormatter.AbsoluteUnit.NOW
            )

            it < 1.hours -> formatter.format(
                it.inWholeMinutes.toDouble(),
                RelativeDateTimeFormatter.Direction.LAST,
                RelativeDateTimeFormatter.RelativeUnit.MINUTES
            )

            it < 1.days -> formatter.format(
                it.inWholeHours.toDouble(),
                RelativeDateTimeFormatter.Direction.LAST,
                RelativeDateTimeFormatter.RelativeUnit.HOURS
            )

            it < 30.days -> formatter.format(
                it.inWholeDays.toDouble(),
                RelativeDateTimeFormatter.Direction.LAST,
                RelativeDateTimeFormatter.RelativeUnit.DAYS
            )

            it < 365.days -> formatter.format(
                (it.inWholeDays / 30).toDouble(),
                RelativeDateTimeFormatter.Direction.LAST,
                RelativeDateTimeFormatter.RelativeUnit.MONTHS
            )

            else -> formatter.format(
                (it.inWholeDays / 365).toDouble(),
                RelativeDateTimeFormatter.Direction.LAST,
                RelativeDateTimeFormatter.RelativeUnit.YEARS
            )
        }
    }
}!!
