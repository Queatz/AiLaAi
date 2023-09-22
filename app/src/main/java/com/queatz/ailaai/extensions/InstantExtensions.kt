package com.queatz.ailaai.extensions

import android.icu.text.MessageFormat
import android.icu.text.RelativeDateTimeFormatter
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.datetime.*
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

fun LocalDate.previous(dayOfWeek: DayOfWeek) = this + DatePeriod(
    days = when {
        this.dayOfWeek.isoDayNumber > dayOfWeek.isoDayNumber ->
            this.dayOfWeek.isoDayNumber - dayOfWeek.isoDayNumber

        this.dayOfWeek.isoDayNumber < dayOfWeek.isoDayNumber ->
            dayOfWeek.isoDayNumber - this.dayOfWeek.isoDayNumber - 7

        else -> 0
    }
)

fun Instant.plus(days: Int = 0, weeks: Int = 0, months: Int = 0, years: Int = 0, zone: TimeZone = TimeZone.currentSystemDefault()) = toLocalDateTime(zone).let {
    (LocalDate(it.year, it.month, it.dayOfMonth) + DatePeriod(days = days + weeks * 7, months = months, years = years)).atTime(it.time)
}.toInstant(zone)


fun Instant.startOfDay(zone: TimeZone = TimeZone.currentSystemDefault()) = toLocalDateTime(zone).let {
    LocalDate(it.year, it.month, it.dayOfMonth)
}.atStartOfDayIn(zone)

fun Instant.startOfWeek(zone: TimeZone = TimeZone.currentSystemDefault()) = toLocalDateTime(zone).let {
    LocalDate(it.year, it.month, it.dayOfMonth).previous(DayOfWeek.SUNDAY)
}.atStartOfDayIn(zone)

fun Instant.startOfMonth(zone: TimeZone = TimeZone.currentSystemDefault()) = toLocalDateTime(zone).let {
    LocalDate(it.year, it.month, 1)
}.atStartOfDayIn(zone)

fun Instant.startOfYear(zone: TimeZone = TimeZone.currentSystemDefault()) = toLocalDateTime(zone).let {
    LocalDate(it.year, 1, 1)
}.atStartOfDayIn(zone)

fun Instant.monthYear() = DateTimeFormatter.ofPattern("MMM yyyy")
    .format(toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())!!

fun Instant.dayMonthYear() = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    .format(toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())!!

fun Instant.dayOfMonth() = MessageFormat.format("{0,ordinal}", toLocalDateTime(TimeZone.currentSystemDefault()).dayOfMonth)!!
fun Instant.day() = toLocalDateTime(TimeZone.currentSystemDefault()).dayOfMonth
fun Instant.nameOfDayOfWeek() = DateTimeFormatter.ofPattern("EEE")
    .format(toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())!!

fun Instant.format(pattern: String) = DateTimeFormatter.ofPattern(pattern)
    .format(toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())!!

fun Instant.timeAgo() = Duration.between(
    toJavaInstant(),
    Clock.System.now().toJavaInstant()
).toKotlinDuration().let {
    val formatter = RelativeDateTimeFormatter.getInstance(AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault())

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
