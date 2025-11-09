package com.queatz.ailaai.extensions

import android.icu.text.MessageFormat
import android.icu.text.RelativeDateTimeFormatter
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.appLanguage
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.time.Clock.System.now
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import java.time.Duration
import kotlin.time.Duration as KotlinDuration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinDuration
import kotlin.time.toKotlinInstant

fun formatCurrentLocalTime(offsetHours: Double): String = DateTimeFormatter.ofPattern("MMMM d, h:mm a").format(
    ZonedDateTime.now(
        UtcOffset(hours = offsetHours.toInt(), minutes = (offsetHours - offsetHours.toInt()).hours.inWholeMinutes.toInt()).toJavaZoneOffset()
    )
)

suspend fun delayUntilNextMinute() = delay(
    now().let { now -> (now + 1.minutes).startOfMinute() - now }
)

fun LocalDate.previous(dayOfWeek: DayOfWeek) = this + DatePeriod(
    days = when {
        this.dayOfWeek.isoDayNumber > dayOfWeek.isoDayNumber ->
            this.dayOfWeek.isoDayNumber - dayOfWeek.isoDayNumber

        this.dayOfWeek.isoDayNumber < dayOfWeek.isoDayNumber ->
            dayOfWeek.isoDayNumber - this.dayOfWeek.isoDayNumber - 7

        else -> 0
    }
)

fun Instant.plus(
    days: Int = 0,
    weeks: Int = 0,
    months: Int = 0,
    years: Int = 0,
    zone: TimeZone = TimeZone.currentSystemDefault(),
) = toLocalDateTime(zone).let {
    (LocalDate(it.year, it.month, it.dayOfMonth) + DatePeriod(days = days + weeks * 7, months = months, years = years)).atTime(it.time)
}.toInstant(zone)

fun Instant.startOfMinute() = toJavaInstant().truncatedTo(ChronoUnit.MINUTES).toKotlinInstant()

fun Instant.startOfDay(zone: TimeZone = TimeZone.currentSystemDefault()) = toLocalDateTime(zone).let {
    it.date
}.atStartOfDayIn(zone)

fun Instant.startOfWeek(zone: TimeZone = TimeZone.currentSystemDefault()) = toLocalDateTime(zone).let {
    it.date.previous(DayOfWeek.SUNDAY)
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
fun Instant.minute() = toLocalDateTime(TimeZone.currentSystemDefault()).minute
fun Instant.hour() = toLocalDateTime(TimeZone.currentSystemDefault()).hour
fun Instant.nameOfDayOfWeek() = DateTimeFormatter.ofPattern("EEE")
    .format(toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())!!

fun Instant.at(
    hour: Int = 0,
    minute: Int = 0,
    second: Int = 0,
    zone: TimeZone = TimeZone.currentSystemDefault()
) = toLocalDateTime(zone)
    .date
    .atTime(LocalTime(hour, minute, second))
    .toInstant(zone)

fun Instant.format(pattern: String) = DateTimeFormatter.ofPattern(pattern)
    .format(toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())!!

fun Instant.isToday(zone: TimeZone = TimeZone.currentSystemDefault()) = toLocalDateTime(zone).let {
    it.date == now().toLocalDateTime(zone).date
}

fun Instant.isTomorrow(zone: TimeZone = TimeZone.currentSystemDefault()) = toLocalDateTime(zone).let {
    it.date == ((now() + 1.days).toLocalDateTime(zone).date)
}

fun Instant.isYesterday(zone: TimeZone = TimeZone.currentSystemDefault()) = toLocalDateTime(zone).let {
    it.date == ((now() - 1.days).toLocalDateTime(zone).date)
}

fun Instant.isThisYear(zone: TimeZone = TimeZone.currentSystemDefault()) = toLocalDateTime(zone).let {
    it.year == now().toLocalDateTime(zone).year
}

// todo: internationalize
@Composable
fun Instant.formatFuture() = when {
    isToday() -> {
        "${format("h:mm a")} ${stringResource(R.string.inline_today)}"
    }

    isTomorrow() -> {
        "${format("h:mm a")} ${stringResource(R.string.inline_tomorrow)}"
    }

    isThisYear() -> {
        format("EEEE, MMMM d")
    }

    else -> {
        format("EEEE, MMMM d, yyyy")
    }
}

// todo: internationalize
fun Instant.formatDate() = format("EEEE, MMMM d")

// todo: internationalize
fun Instant.formatTime() = format("h:mm a")

// todo: internationalize
@Composable
fun Instant.formatDateForToday() = when {
    isYesterday() -> {
        bulletedString(
            stringResource(R.string.yesterday),
            format("EEEE, MMMM d")
        )
    }
    isToday() -> {
        bulletedString(
            stringResource(R.string.today),
            format("EEEE, MMMM d")
        )
    }

    isTomorrow() -> {
        bulletedString(
            stringResource(R.string.tomorrow),
            format("EEEE, MMMM d")
        )
    }

    isThisYear() -> {
        format("EEEE, MMMM d")
    }

    else -> {
        format("EEEE, MMMM d, yyyy")
    }
}

fun Instant.formatDateStamp() = format("MM/dd")

@Composable
fun Instant.formatDateAndTime() = "${format("EEEE, MMMM d")} ${stringResource(R.string.inline_at)} ${format("h:mm a")}"

fun Instant.timeAgo() = Duration.between(
    toJavaInstant(),
    now().toJavaInstant()
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
            it < 1.minutes -> formatter.format(
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

fun Instant.timeUntil() = Duration.between(
    now().toJavaInstant(),
    toJavaInstant()
).toKotlinDuration().let {
    val formatter = RelativeDateTimeFormatter.getInstance(AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault())

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        when {
            it < 1.minutes -> formatter.format(
                RelativeDateTimeFormatter.Direction.PLAIN,
                RelativeDateTimeFormatter.AbsoluteUnit.NOW
            )

            it < 1.hours -> formatter.format(
                it.inWholeMinutes.toDouble(),
                RelativeDateTimeFormatter.RelativeDateTimeUnit.MINUTE
            )

            it < 1.days -> formatter.format(
                it.inWholeHours.toDouble(),
                RelativeDateTimeFormatter.RelativeDateTimeUnit.HOUR
            )

            it < 30.days -> formatter.format(
                it.inWholeDays.toDouble(),
                RelativeDateTimeFormatter.RelativeDateTimeUnit.DAY
            )

            it < 365.days -> formatter.format(
                (it.inWholeDays / 30).toDouble(),
                RelativeDateTimeFormatter.RelativeDateTimeUnit.MONTH
            )

            else -> formatter.format(
                (it.inWholeDays / 365).toDouble(),
                RelativeDateTimeFormatter.RelativeDateTimeUnit.YEAR
            )
        }
    } else {
        when {
            it < 1.minutes -> formatter.format(
                RelativeDateTimeFormatter.Direction.PLAIN,
                RelativeDateTimeFormatter.AbsoluteUnit.NOW
            )

            it < 1.hours -> formatter.format(
                it.inWholeMinutes.toDouble(),
                RelativeDateTimeFormatter.Direction.NEXT,
                RelativeDateTimeFormatter.RelativeUnit.MINUTES
            )

            it < 1.days -> formatter.format(
                it.inWholeHours.toDouble(),
                RelativeDateTimeFormatter.Direction.NEXT,
                RelativeDateTimeFormatter.RelativeUnit.HOURS
            )

            it < 30.days -> formatter.format(
                it.inWholeDays.toDouble(),
                RelativeDateTimeFormatter.Direction.NEXT,
                RelativeDateTimeFormatter.RelativeUnit.DAYS
            )

            it < 365.days -> formatter.format(
                (it.inWholeDays / 30).toDouble(),
                RelativeDateTimeFormatter.Direction.NEXT,
                RelativeDateTimeFormatter.RelativeUnit.MONTHS
            )

            else -> formatter.format(
                (it.inWholeDays / 365).toDouble(),
                RelativeDateTimeFormatter.Direction.NEXT,
                RelativeDateTimeFormatter.RelativeUnit.YEARS
            )
        }
    }
}!!

fun KotlinDuration.format(): String {
    val language = appLanguage ?: "en"
    val seconds = inWholeSeconds
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    val months = days / 30
    val years = days / 365

    val s = seconds % 60
    val m = minutes % 60
    val h = hours % 24
    val d = days % 7
    val w = weeks % 4
    val mo = months % 12
    val y = years

    return when {
        language.startsWith("vi") -> buildString {
            if (y > 0) append("$y năm ")
            if (mo > 0) append("$mo tháng ")
            if (w > 0) append("$w tuần ")
            if (d > 0) append("$d ngày ")
            if (h > 0) append("$h tiếng ")
            if (m > 0) append("$m phút ")
            if (s > 0) append("$s giây")
        }.trim()

        else -> buildString {
            if (y > 0) append("${y}y ")
            if (mo > 0) append("${mo}mo ")
            if (w > 0) append("${w}w ")
            if (d > 0) append("${d}d ")
            if (h > 0) append("${h}h ")
            if (m > 0) append("${m}m ")
            if (s > 0) append("${s}s")
        }.trim()
    }
}

@Composable
fun Instant.formatDistance() = Duration.between(
    now().toJavaInstant(),
    toJavaInstant()
).toKotlinDuration().let {
    when {
        it < 1.minutes -> it.inWholeSeconds.toInt().let { count -> pluralStringResource(R.plurals.x_seconds, count, count)  }
        it < 1.hours -> it.inWholeMinutes.toInt().let { count -> pluralStringResource(R.plurals.x_minutes, count, count)  }
        it < 1.days -> it.inWholeHours.toInt().let { count -> pluralStringResource(R.plurals.x_hours, count, count)  }
        it < 30.days -> it.inWholeDays.toInt().let { count -> pluralStringResource(R.plurals.x_days, count, count)  }
        it < 365.days -> (it.inWholeDays / 30).toInt().let { count ->pluralStringResource(R.plurals.x_months, count, count) }
        else -> (it.inWholeDays / 365).toInt().let { count ->pluralStringResource(R.plurals.x_years, count, count) }
    }
}
