package com.queatz.ailaai.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// todo translations
fun Long.formatTime() = when {
    this < 1.seconds.inWholeMilliseconds -> "0s"
    this < 1.minutes.inWholeMilliseconds -> "${milliseconds.inWholeSeconds}s"
    this < 1.hours.inWholeMilliseconds -> "${milliseconds.inWholeMinutes}m ${milliseconds.inWholeSeconds - milliseconds.inWholeMinutes.minutes.inWholeSeconds}s"
    else -> "${milliseconds.inWholeHours}h ${milliseconds.inWholeMinutes - milliseconds.inWholeHours.hours.inWholeMinutes}m"
}

@Composable
fun Instant.shortAgo() = (Clock.System.now() - this).let {
    when {
        it.inWholeDays >= 365 -> pluralStringResource(R.plurals.time_x_years, (it.inWholeDays / 365).toInt(), (it.inWholeDays / 365).toInt())
        it.inWholeDays >= 30 -> pluralStringResource(R.plurals.time_x_months, (it.inWholeDays / 30).toInt(), (it.inWholeDays / 30).toInt())
        it.inWholeDays >= 7 -> pluralStringResource(R.plurals.time_x_weeks, (it.inWholeDays / 7).toInt(), (it.inWholeDays / 7).toInt())
        it.inWholeDays >= 1 -> pluralStringResource(R.plurals.time_x_days, it.inWholeDays.toInt(), it.inWholeDays.toInt())
        it.inWholeHours >= 1 -> pluralStringResource(R.plurals.time_x_hours, it.inWholeHours.toInt(), it.inWholeHours.toInt())
        it.inWholeMinutes > 0 -> pluralStringResource(R.plurals.time_x_minutes, it.inWholeMinutes.toInt(), it.inWholeMinutes.toInt())
        else -> stringResource(R.string.now)
    }
}
