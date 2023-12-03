package app.reminder

import androidx.compose.runtime.Composable
import app.AppStyles
import asNaturalList
import com.queatz.db.*
import focusable
import format
import lib.*
import notBlank
import notEmpty
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import kotlin.js.Date

@Composable
fun ReminderItem(reminder: Reminder, selected: Boolean, onSelected: () -> Unit) {
    Div({
        classes(
            listOf(AppStyles.groupItem) + if (selected) {
                listOf(AppStyles.groupItemSelected)
            } else {
                emptyList()
            }
        )
        focusable()
        onClick {
            onSelected()
        }
    }) {
        Div({
            style {
                width(0.px)
                flexGrow(1)
            }
        }) {
            Div({
                classes(AppStyles.groupItemName)
            }) {
                Text(reminder.title ?: "New reminder")
            }
            Div({
                classes(AppStyles.groupItemMessage)
            }) {
                Text(reminder.scheduleText)
            }
            reminder.note?.notBlank?.let {
                Div({
                    classes(AppStyles.groupItemMessage)
                }) {
                    Text(it)
                }
            }
        }
    }
}

val Reminder.scheduleText @Composable get(): String = buildString {
    if (schedule == null) {
        if (end != null) {
            append("From ${Date(start!!.toEpochMilliseconds()).format()} until ${Date(end!!.toEpochMilliseconds()).format()}")
        } else {
            append(Date(start!!.toEpochMilliseconds()).format())
        }

        return@buildString
    }

    append("Every")
    append(" ")

    val dayStrings = (schedule?.weekdays?.map {
        enUS.localize.day(it - 1)
    } ?: emptyList()) + (schedule?.days?.map {
        if (it == -1) "last" else enUS.localize.ordinalNumber(it)
    } ?: emptyList())

    if (dayStrings.isNotEmpty()) {
        append(dayStrings.asNaturalList { it })

        if (schedule?.days?.notEmpty != null) {
            append(" day of the month")
        }

        append(" ")
    } else {
        append("day ")
    }

    schedule?.hours?.notEmpty?.let { hours ->
        val mins = getMinutes(Date(start!!.toEpochMilliseconds()))
        append("at ")
        append(hours.asNaturalList { format(parse("$it:$mins", "HH:mm", Date()), "h:mm a") })
        append(" ")
    }

    var during = "during"

    schedule?.weeks?.notEmpty?.let { weeks ->
        append("$during the ")
        during = "of"
        append(weeks.asNaturalList { enUS.localize.ordinalNumber(it) })
        append(" week")
        append(" ")

        if (schedule?.months?.notEmpty == null) {
            append("of every month")
            append(" ")
        }
    }

    schedule?.months?.notEmpty?.let { months ->
        append("$during ")
        during = "of"
        append(months.asNaturalList { enUS.localize.month(it - 1) })
        append(" ")
    }

    schedule?.years?.notEmpty?.let { years ->
        append("$during ")
        during = "of"
        append(years.asNaturalList { "$it" })
        append(" ")
    }

    val start = Date(start!!.toEpochMilliseconds())
    if (isAfter(start, Date())) {
        append("from ${ start.format() } ")
    }

    end?.let { Date(it.toEpochMilliseconds()) }?.let { end ->
        append("until ${ end.format() } ")
    }
}.trimEnd()
