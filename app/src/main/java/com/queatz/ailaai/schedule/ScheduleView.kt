package com.queatz.ailaai.schedule

import com.queatz.ailaai.data.Reminder
import com.queatz.ailaai.data.ReminderOccurrence
import com.queatz.ailaai.extensions.format
import com.queatz.ailaai.extensions.nameOfDayOfWeek
import kotlinx.datetime.Instant

enum class ScheduleView {
    Daily,
    Weekly,
    Monthly,
    Yearly
}

enum class ReminderEventType {
    Start,
    Occur,
    End
}

val ScheduleView.range
    get() = when (this) {
        ScheduleView.Daily -> 7
        ScheduleView.Weekly -> 4
        ScheduleView.Monthly -> 3
        ScheduleView.Yearly -> 2
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

val ScheduleView.dateTimeFormat
    get() = when (this) {
        ScheduleView.Daily -> "h:mm"
        ScheduleView.Weekly -> "d"
        ScheduleView.Monthly -> "d"
        ScheduleView.Yearly -> "d"
    }

fun Instant.formatTitle(view: ScheduleView) = format(view.titleFormat)

fun Instant.formatEvent(view: ScheduleView) = view.eventFormat?.let(::format)

fun Instant.formatDateTime(view: ScheduleView) = format(view.dateTimeFormat)

fun Instant.formatDateTimeHint(view: ScheduleView) = when (view) {
    ScheduleView.Daily -> format("a")
    ScheduleView.Weekly -> nameOfDayOfWeek()
    ScheduleView.Monthly -> nameOfDayOfWeek()
    ScheduleView.Yearly -> format("MMM")
}

data class ReminderEvent(
    /**
     * The reminder that spawned this event
     */
    val reminder: Reminder,
    /**
     * The date of the event
     */
    val date: Instant,
    /**
     * The type of event.
     */
    val event: ReminderEventType,
    /**
     * The `ReminderOccurrence` associated with this event, if any.
     */
    val occurrence: ReminderOccurrence?
)
