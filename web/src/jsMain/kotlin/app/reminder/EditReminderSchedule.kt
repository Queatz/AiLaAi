package app.reminder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.components.MultiSelect
import appString
import application
import com.queatz.db.ReminderSchedule
import com.queatz.db.ReminderStickiness
import components.LabeledCheckbox
import format
import kotlinx.datetime.toKotlinInstant
import lib.addHours
import lib.getMinutes
import lib.parse
import lib.setMinutes
import lib.startOfDay
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import parseDateTime
import r
import time.format
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
    initialReoccurringMonths: List<Int>? = null,
    initialStickiness: ReminderStickiness? = null
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
    var stickiness by mutableStateOf(initialStickiness ?: ReminderStickiness.None)
    var hasStickiness by mutableStateOf(initialStickiness != null && initialStickiness != ReminderStickiness.None)
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
        date = schedule.date,
        time = schedule.time,
        onDate = { schedule.date = it },
        onTime = { schedule.time = it },
        disabled = disabled,
        styles = {
            padding(.5.r, .5.r, 1.r, .5.r)
        }
    )

    LabeledCheckbox(
        value = schedule.reoccurs,
        onValue = {
            schedule.reoccurs = it
        },
        enabled = !disabled,
        text = appString { reoccurs },
        styles = {
            padding(0.r, .5.r, 1.r, .5.r)
        }
    )
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
                option("-", appString { everyDay })
                (1..7).forEach {
                    // todo: localize
                    val n = application.locale.localize.day(it - 1).toString()
                    option("weekday:$it", n)
                }
                (1..31).forEach {
                    // todo: localize
                    option("day:$it", appString { ofTheMonth }.format(application.locale.localize.ordinalNumber(it)))
                }
                option("day:-1", appString { lastDayOfTheMonth })
            }

            MultiSelect(schedule.reoccurringWeeks, { schedule.reoccurringWeeks = it }, {
                style {
                    marginTop(1.r)
                }

                if (disabled) {
                    disabled()
                }
            }) {
                option("-", appString { everyWeek })
                (1..5).forEach {
                    option("$it", appString { nthWeek }.format(application.locale.localize.ordinalNumber(it)))
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
                option("-", appString { everyMonth })
                (1..12).forEach {
                    option("$it", application.locale.localize.month(it - 1).toString())
                }
            }
        }
    }

    LabeledCheckbox(
        value = schedule.until,
        onValue = {
            schedule.until = it
            schedule.untilDate = schedule.date
            schedule.untilTime = schedule.time
        },
        enabled = !disabled,
        text = appString { until },
        styles = {
            padding(0.r, .5.r, 1.r, .5.r)
        }
    )

    if (schedule.until) {
        ReminderDateTime(
            date = schedule.untilDate,
            time = schedule.untilTime,
            onDate = { schedule.untilDate = it },
            onTime = { schedule.untilTime = it },
            disabled = disabled,
            styles = {
                marginBottom(1.r)
                padding(0.r, .5.r)
            }
        )
    }

    LabeledCheckbox(
        value = schedule.hasStickiness,
        onValue = {
            schedule.hasStickiness = it
        },
        enabled = !disabled,
        text = appString { stickiness },
        styles = {
            padding(0.r, .5.r, 1.r, .5.r)
        }
    )

    if (schedule.hasStickiness) {
        Div({
            style {
                padding(0.r, .5.r, 1.r, .5.r)
                marginBottom(1.r)
            }
        }) {
            MultiSelect(
                selected = listOf(schedule.stickiness.toString()),
                onSelected = { newValue ->
                    if (newValue.isNotEmpty()) {
                        schedule.stickiness = ReminderStickiness.valueOf(newValue.first())
                    } else {
                        schedule.stickiness = ReminderStickiness.None
                    }
                }, attrs = {
                    style {
                        width(100.percent)
                    }

                    if (disabled) {
                        disabled()
                    }
                }
            ) {
                ReminderStickiness.entries.forEach { stickinessOption ->
                    option(
                        value = stickinessOption.toString(),
                        title = when(stickinessOption) {
                            ReminderStickiness.None -> application.appString { none }
                            ReminderStickiness.Hourly -> application.appString { hourly }
                            ReminderStickiness.Daily -> application.appString { daily }
                            ReminderStickiness.Weekly -> application.appString { weekly }
                            ReminderStickiness.Monthly -> application.appString { monthly }
                            ReminderStickiness.Yearly -> application.appString { yearly }
                        }
                    )
                }
            }
        }
    }
}
