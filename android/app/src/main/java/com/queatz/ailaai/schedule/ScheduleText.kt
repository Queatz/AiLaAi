package com.queatz.ailaai.schedule

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.*
import com.queatz.db.Reminder
import kotlinx.datetime.Clock

// todo translate
val Reminder.scheduleText @Composable get(): String = buildString {
    val now = Clock.System.now()

    if (schedule == null) {
        if (end != null) {
            append("From ${start!!.formatDateAndTime()} until ${end!!.formatDateAndTime()}")
        } else {
            append(start!!.formatDateAndTime())
        }

        return@buildString
    }

    append("Every")
    append(" ")

    val dayStrings = (schedule?.weekdays?.map {
        it.dayOfWeekName
    } ?: emptyList()) + (schedule?.days?.map {
        if (it == -1) "last" else it.ordinal
    } ?: emptyList())

    if (dayStrings.isNotEmpty()) {
        append(dayStrings.asNaturalList { it })

        if (schedule?.days?.ifNotEmpty != null) {
            append(" day of the month")
        }

        append(" ")
    } else {
        append("day ")
    }

    schedule?.hours?.ifNotEmpty?.let { hours ->
        val mins = start!!.minute()
        append("at ")
        append(hours.asNaturalList { now.at(it, mins).format("h:mm a") })
        append(" ")
    }

    var during = "during"

    schedule?.weeks?.ifNotEmpty?.let { weeks ->
        append("$during the ")
        during = "of"
        append(weeks.asNaturalList { it.ordinal })
        append(" week")
        append(" ")

        if (schedule?.months?.ifNotEmpty == null) {
            append("of every month")
            append(" ")
        }
    }

    schedule?.months?.ifNotEmpty?.let { months ->
        append("$during ")
        during = "of"
        append(months.asNaturalList { it.monthName })
        append(" ")
    }

    schedule?.years?.ifNotEmpty?.let { years ->
        append("$during ")
        during = "of"
        append(years.asNaturalList { "$it" })
        append(" ")
    }

    val start = start!!
    if (start > now) {
        append("from ${ start.formatDateAndTime() } ")
    }

    end?.let { end ->
        append("until ${ end.formatDateAndTime() } ")
    }
}.trimEnd()

@Composable
fun <T> List<T>.asNaturalList(transform: (T) -> String) = when (size) {
    0 -> ""
    1 -> transform(first())
    2 -> "${transform(first())} ${stringResource(R.string.inline_and)} ${transform(last())}"
    else -> {
        "${dropLast(1).joinToString(", ", transform = transform)} ${stringResource(R.string.inline_and)} ${transform(last())}"
    }
}
