package com.queatz.ailaai.schedule

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.*
import com.queatz.db.Reminder
import kotlinx.datetime.Clock

val Reminder.scheduleText @Composable get(): String = buildString {
    val now = Clock.System.now()

    if (schedule == null) {
        if (end != null) {
            append(stringResource(R.string.from_x_until_x, start!!.formatDateAndTime(), end!!.formatDateAndTime()))
        } else {
            append(start!!.formatDateAndTime())
        }

        return@buildString
    }

    append(stringResource(R.string.every))
    append(" ")

    val dayStrings = (schedule?.weekdays?.map {
        it.dayOfWeekName
    } ?: emptyList()) + (schedule?.days?.map {
        if (it == -1) stringResource(R.string.inline_last) else it.ordinal
    } ?: emptyList())

    if (dayStrings.isNotEmpty()) {
        append(dayStrings.asNaturalList { it })

        if (schedule?.days?.ifNotEmpty != null) {
            append(" ${stringResource(R.string.inline_day_of_the_month)}")
        }

        append(" ")
    } else {
        append("${stringResource(R.string.inline_day)} ")
    }

    schedule?.hours?.ifNotEmpty?.let { hours ->
        val mins = start!!.minute()
        append("${stringResource(R.string.inline_at)} ")
        append(hours.asNaturalList { now.at(it, mins).format("h:mm a") })
        append(" ")
    }

    var during = stringResource(R.string.inline_during)

    schedule?.weeks?.ifNotEmpty?.let { weeks ->
        append("$during${stringResource(R.string.inline_the).notBlank?.let { " $it" }} ")
        during = stringResource(R.string.inline_of)
        append(weeks.asNaturalList { it.ordinal })
        append(" ${stringResource(R.string.inline_week)}")
        append(" ")

        if (schedule?.months?.ifNotEmpty == null) {
            append(stringResource(R.string.inline_of_every_month))
            append(" ")
        }
    }

    schedule?.months?.ifNotEmpty?.let { months ->
        append("$during ")
        during = stringResource(R.string.inline_of)
        append(months.asNaturalList { it.monthName })
        append(" ")
    }

    schedule?.years?.ifNotEmpty?.let { years ->
        append("$during ")
        during = stringResource(R.string.inline_the)
        append(years.asNaturalList { "$it" })
        append(" ")
    }

    val start = start!!
    if (start > now) {
        append("${stringResource(R.string.inline_from)} ${ start.formatDateAndTime() } ")
    }

    end?.let { end ->
        append("${stringResource(R.string.inline_until)} ${ end.formatDateAndTime() } ")
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

fun <T> List<T>.asNaturalList(context: Context, transform: (T) -> String) = when (size) {
    0 -> ""
    1 -> transform(first())
    2 -> "${transform(first())} ${context.getString(R.string.inline_and)} ${transform(last())}"
    else -> {
        "${dropLast(1).joinToString(", ", transform = transform)} ${context.getString(R.string.inline_and)} ${transform(last())}"
    }
}
