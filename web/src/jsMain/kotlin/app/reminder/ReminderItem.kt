package app.reminder

import androidx.compose.runtime.Composable
import app.AppStyles
import appString
import application
import asNaturalList
import bulletedString
import com.queatz.db.Reminder
import focusable
import format
import lib.getMinutes
import lib.isAfter
import lib.parse
import notBlank
import notEmpty
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import time.format
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
                Text(reminder.title ?: appString { newReminder })
            }
            Div({
                classes(AppStyles.groupItemMessage)
            }) {
                Text(
                    bulletedString(
                        reminder.categories?.firstOrNull(),
                        reminder.scheduleText
                    )
                )
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

// todo: translate
val Reminder.scheduleText @Composable get(): String = buildString {
    if (schedule == null) {
        if (end != null) {
            append(
                appString { fromUntil }.format(
                    Date(start!!.toEpochMilliseconds()).format(),
                    Date(end!!.toEpochMilliseconds()).format()
                )
            )
        } else {
            append(Date(start!!.toEpochMilliseconds()).format())
        }

        return@buildString
    }

    append(appString { every })
    append(" ")

    val dayStrings = (schedule?.weekdays?.map {
        application.locale.localize.day(it - 1)
    } ?: emptyList()) + (schedule?.days?.map {
        if (it == -1) appString { inlineLast } else application.locale.localize.ordinalNumber(it)
    } ?: emptyList())

    if (dayStrings.isNotEmpty()) {
        append(dayStrings.asNaturalList { it })

        if (schedule?.days?.notEmpty != null) {
            append(" ${appString { inlineDayOfTheMonth }}")
        }

        append(" ")
    } else {
        append("${appString { inlineDay }} ")
    }

    schedule?.hours?.notEmpty?.let { hours ->
        val mins = getMinutes(Date(start!!.toEpochMilliseconds()))
        append("${appString { inlineAt }} ")
        append(hours.asNaturalList { format(parse("$it:$mins", "HH:mm", Date()), "h:mm a") })
        append(" ")
    }

    var during = appString { inlineDuring }

    schedule?.weeks?.notEmpty?.let { weeks ->
        append("$during ${appString { inlineThe }} ")
        during = appString { inlineOf }
        append(weeks.asNaturalList { application.locale.localize.ordinalNumber(it) })
        append(" ${appString { inlineWeekly }}")
        append(" ")

        if (schedule?.months?.notEmpty == null) {
            append(appString { inlineOfEveryMonth })
            append(" ")
        }
    }

    schedule?.months?.notEmpty?.let { months ->
        append("$during ")
        during = appString { inlineOf }
        append(months.asNaturalList { application.locale.localize.month(it - 1) })
        append(" ")
    }

    schedule?.years?.notEmpty?.let { years ->
        append("$during ")
        during = appString { inlineOf }
        append(years.asNaturalList { "$it" })
        append(" ")
    }

    val start = Date(start!!.toEpochMilliseconds())
    if (isAfter(start, Date())) {
        append("${appString { inlineFrom }} ${ start.format() } ")
    }

    end?.let { Date(it.toEpochMilliseconds()) }?.let { end ->
        append("${appString { inlineUntil }} ${ end.format() } ")
    }
}.trimEnd()
