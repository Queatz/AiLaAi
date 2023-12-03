package app.reminder

import androidx.compose.runtime.*
import app.components.MultiSelect
import com.queatz.db.ReminderSchedule
import kotlinx.datetime.toKotlinInstant
import lib.*
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.CheckboxInput
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Text
import parseDateTime
import r
import kotlin.js.Date

class EditSchedule(
    initialReoccurs: Boolean = false,
    initialUntil: Boolean = false,
    initialDate: Date = Date(),
    initialUntilDate: Date? = null,
    initialReoccurringHours: List<Int>? = null,
    initialReoccurringDays: List<Int>? = null,
    initialReoccurringWeekdays: List<Int>? = null,
    initialReoccurringWeeks: List<Int>? = null,
    initialReoccurringMonths: List<Int>? = null
) {
    var reoccurs by mutableStateOf(initialReoccurs)
    var date by mutableStateOf(format(initialDate, "yyyy-MM-dd"))
    var time by mutableStateOf(format(initialDate, "HH:mm"))
    var until by mutableStateOf(initialUntil)
    var untilDate by mutableStateOf(format(initialUntilDate ?: Date(), "yyyy-MM-dd"))
    var untilTime by mutableStateOf(format(initialUntilDate ?: Date(), "HH:mm"))
    var reoccurringHours by mutableStateOf(initialReoccurringHours?.map { it.toString() } ?: listOf(time.split(":").first().toInt().toString()))
    var reoccurringDays by mutableStateOf((initialReoccurringDays?.map { "day:$it" } ?: emptyList()) + (initialReoccurringWeekdays?.map { "weekday:$it" } ?: emptyList()))
    var reoccurringWeeks by mutableStateOf(initialReoccurringWeeks?.map { it.toString() } ?: emptyList<String>())
    var reoccurringMonths by mutableStateOf(initialReoccurringMonths?.map { it.toString() } ?: emptyList<String>())
}

val EditSchedule.reminderSchedule get() = if (reoccurs) {
    ReminderSchedule(
        hours = reoccurringHours.filter { it != "-" }.map { it.toInt() },
        days = reoccurringDays.filter { it != "-" }.mapNotNull { it.split(":").let { if (it[0] == "day") it[1] else null } }.map { it.toInt() },
        weekdays = reoccurringDays.filter { it != "-" }.mapNotNull { it.split(":").let { if (it[0] == "weekday") it[1] else null } }.map { it.toInt() },
        weeks = reoccurringWeeks.filter { it != "-" }.map { it.toInt() },
        months = reoccurringMonths.filter { it != "-" }.map { it.toInt() },
    )
} else {
    null
}

val EditSchedule.start get() = parseDateTime(date, time).toKotlinInstant()
val EditSchedule.end get() = if (until) parseDateTime(untilDate, untilTime).toKotlinInstant() else null

@Composable
fun EditReminderSchedule(
    schedule: EditSchedule,
    disabled: Boolean = false
) {
    LaunchedEffect(schedule.reoccurringHours) {
        if (schedule.reoccurringHours.isEmpty()) schedule.reoccurringHours = listOf("${parseDateTime(schedule.date, schedule.time).getHours()}")
    }

    LaunchedEffect(schedule.reoccurringDays) {
        if (schedule.reoccurringDays.isEmpty()) schedule.reoccurringDays =
            listOf("-") else if (schedule.reoccurringDays.size > 1 && "-" in schedule.reoccurringDays) schedule.reoccurringDays -= "-"
    }

    LaunchedEffect(schedule.reoccurringWeeks) {
        if (schedule.reoccurringWeeks.isEmpty()) schedule.reoccurringWeeks =
            listOf("-") else if (schedule.reoccurringWeeks.size > 1 && "-" in schedule.reoccurringWeeks) schedule.reoccurringWeeks -= "-"
    }

    LaunchedEffect(schedule.reoccurringMonths) {
        if (schedule.reoccurringMonths.isEmpty()) schedule.reoccurringMonths =
            listOf("-") else if (schedule.reoccurringMonths.size > 1 && "-" in schedule.reoccurringMonths) schedule.reoccurringMonths -= "-"
    }

    ReminderDateTime(
        schedule.date,
        schedule.time,
        { schedule.date = it },
        { schedule.time = it },
        disabled,
        {
            padding(.5.r, .5.r, 1.r, .5.r)
        }
    )

    Label(attrs = {
        style {
            padding(0.r, .5.r, 1.r, .5.r)
        }
    }) {
        CheckboxInput(schedule.reoccurs) {
            onChange {
                schedule.reoccurs = it.value
            }

            if (disabled) {
                disabled()
            }
        }
        Text("Reoccurs")
    }
    if (schedule.reoccurs) {
        Div({
            style {
                padding(0.r, .5.r)
                marginBottom(1.r)
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
            }
        }) {
            MultiSelect(schedule.reoccurringHours, { schedule.reoccurringHours = it }, {
                if (disabled) {
                    disabled()
                }
            }) {
                val d = parseDateTime(schedule.date, schedule.time)
                val startOfDayWithMinutes = setMinutes(
                    startOfDay(d),
                    getMinutes(parse(schedule.time, "HH:mm", d))
                )
                (0..23).forEach {
                    option("$it", format(addHours(startOfDayWithMinutes, it.toDouble()), "h:mm a"))
                }
            }

            MultiSelect(schedule.reoccurringDays, { schedule.reoccurringDays = it }, {
                style {
                    marginTop(1.r)
                }

                if (disabled) {
                    disabled()
                }
            }) {
                option("-", "Every day")
                (1..7).forEach {
                    val n = enUS.localize.day(it - 1).toString()
                    option("weekday:$it", n)
                }
                (1..31).forEach {
                    option("day:$it", "${enUS.localize.ordinalNumber(it)} of the month")
                }
                option("day:-1", "Last day of the month")
            }

            MultiSelect(schedule.reoccurringWeeks, { schedule.reoccurringWeeks = it }, {
                style {
                    marginTop(1.r)
                }

                if (disabled) {
                    disabled()
                }
            }) {
                option("-", "Every week")
                (1..5).forEach {
                    option("$it", "${enUS.localize.ordinalNumber(it)} week")
                }
            }

            MultiSelect(schedule.reoccurringMonths, { schedule.reoccurringMonths = it }, {
                style {
                    marginTop(1.r)
                }

                if (disabled) {
                    disabled()
                }
            }) {
                option("-", "Every month")
                (1..12).forEach {
                    option("$it", enUS.localize.month(it - 1).toString())
                }
            }
        }
    }

    Label(attrs = {
        style {
            padding(0.r, .5.r, 1.r, .5.r)
        }
    }) {
        CheckboxInput(schedule.until) {
            onChange {
                schedule.until = it.value
                schedule.untilDate = schedule.date
                schedule.untilTime = schedule.time
            }

            if (disabled) {
                disabled()
            }
        }
        Text("Until")
    }

    if (schedule.until) {
        ReminderDateTime(
            schedule.untilDate,
            schedule.untilTime,
            { schedule.untilDate = it },
            { schedule.untilTime = it },
            disabled,
            {
                marginBottom(1.r)
                padding(0.r, .5.r)
            }
        )
    }
}
