package com.queatz.ailaai.schedule

import android.content.Context
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.*
import com.queatz.db.Reminder
import com.queatz.db.ReminderOccurrence
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

enum class ScheduleView {
    Daily,
    Weekly,
    Monthly,
    Yearly
}

val ScheduleView.range
    get() = when (this) {
        ScheduleView.Daily -> 7
        ScheduleView.Weekly -> 4
        ScheduleView.Monthly -> 3
        ScheduleView.Yearly -> 2
    }

val ScheduleView.duration
    get() = when (this) {
        // todo load larger chunks when loading up is more fluid
        ScheduleView.Daily -> 1.days// * 7
        ScheduleView.Weekly -> 7.days// * 4
        ScheduleView.Monthly -> 30.days// * 3
        ScheduleView.Yearly -> 365.days// * 2
    }

val ScheduleView.titleFormat
    get() = when (this) {
        ScheduleView.Daily -> "EEEE, MMMM d"
        ScheduleView.Weekly -> "EEEE, MMMM d"
        ScheduleView.Monthly -> "MMMM, yyyy"
        ScheduleView.Yearly -> "yyyy G"
    }

val ScheduleView.eventFormat
    get() = when (this) {
        ScheduleView.Daily -> null
        else -> "h:mm a"
    }

val ScheduleView.eventFormatFull
    get() = when (this) {
        ScheduleView.Daily -> "h:mm a"
        ScheduleView.Weekly -> "MMMM d, EEEE, h:mm a"
        ScheduleView.Monthly -> "d, EEEE, h:mm a"
        ScheduleView.Yearly -> "MMMM d, EEEE, h:mm a"
    }

val ScheduleView.dateTimeFormat
    get() = when (this) {
        ScheduleView.Daily -> "h:mm"
        ScheduleView.Weekly -> "d"
        ScheduleView.Monthly -> "d"
        ScheduleView.Yearly -> "d"
    }

fun Instant.formatTitle(context: Context, view: ScheduleView) = format(view.titleFormat).let {
    when (view == ScheduleView.Daily) {
        true -> {
            when {
                isToday() -> "${context.getString(R.string.today)}, $it"
                isTomorrow() -> "${context.getString(R.string.tomorrow)}, $it"
                isYesterday() -> "${context.getString(R.string.yesterday)}, $it"
                else -> it
            }
        }
        else -> it
    }
}

fun Instant.formatEvent(view: ScheduleView) = view.eventFormat?.let(::format)

fun Instant.formatEventFull(view: ScheduleView) = view.eventFormatFull.let(::format)

fun Instant.formatDateTime(view: ScheduleView) = format(view.dateTimeFormat)

fun Instant.formatDateTimeHint(view: ScheduleView) = when (view) {
    ScheduleView.Daily -> format("a")
    ScheduleView.Weekly -> nameOfDayOfWeek()
    ScheduleView.Monthly -> nameOfDayOfWeek()
    ScheduleView.Yearly -> format("MMM")
}
