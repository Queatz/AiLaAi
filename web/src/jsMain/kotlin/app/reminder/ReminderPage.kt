package app.reminder

import LocalConfiguration
import Strings.duration
import androidx.compose.runtime.*
import api
import app.AppStyles
import app.PageTopBar
import app.ailaai.api.deleteReminder
import app.ailaai.api.updateReminder
import app.components.EditField
import app.dialog.dialog
import app.dialog.inputDialog
import app.dialog.searchDialog
import app.menu.Menu
import appString
import application
import bulletedString
import com.queatz.db.Reminder
import focusable
import kotlinx.coroutines.launch
import lib.RawTimeZone
import lib.rawTimeZones
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.NumberInput
import org.jetbrains.compose.web.dom.RangeInput
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r
import kotlin.js.Date
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Composable
fun ReminderPage(
    reminder: Reminder,
    onUpdate: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit
) {
    val scope = rememberCoroutineScope()

    var menuTarget by remember(reminder) {
        mutableStateOf<DOMRect?>(null)
    }

    val schedule by remember(reminder) {
        mutableStateOf(
            EditSchedule(
                initialReoccurs = reminder.schedule != null,
                initialUntil = reminder.end != null,
                initialDate = Date(reminder.start!!.toEpochMilliseconds()),
                initialUntilDate = reminder.end?.let { Date(it.toEpochMilliseconds()) },
                initialReoccurringHours = reminder.schedule?.hours,
                initialReoccurringDays = reminder.schedule?.days,
                initialReoccurringWeekdays = reminder.schedule?.weekdays,
                initialReoccurringWeeks = reminder.schedule?.weeks,
                initialReoccurringMonths = reminder.schedule?.months,
            )
        )
    }

    menuTarget?.let { target ->
        Menu({ menuTarget = null }, target) {
            item(appString { rename }) {
                scope.launch {
                    val title = inputDialog(application.appString { this.reminder },
                        application.appString { title },
                        application.appString { update },
                        defaultValue = reminder.title ?: ""
                    )

                    if (title == null) return@launch

                    api.updateReminder(
                        id = reminder.id!!,
                        reminder = Reminder(title = title)
                    ) {
                        onUpdate(it)
                    }
                }
            }

            item(appString { reschedule }) {
                scope.launch {
                    val result = dialog(
                        title = application.appString { reschedule },
                        confirmButton = application.appString { update }
                    ) {
                        EditReminderSchedule(schedule)
                    }

                    if (result == true) {
                        api.updateReminder(
                            id = reminder.id!!,
                            reminder = Reminder(
                                start = schedule.start,
                                end = schedule.end,
                                schedule = schedule.reminderSchedule
                            )
                        ) {
                            onUpdate(it)
                        }
                    }
                }
            }

            item(
                if (reminder.alarm == true) {
                    // todo: translate
                    "Turn off alarm"
                } else {
                    // todo: translate
                    "Turn on alarm"
                },
                icon = "alarm".takeIf { reminder.alarm == true }
            ) {
                scope.launch {
                    api.updateReminder(
                        id = reminder.id!!,
                        reminder = Reminder(
                            alarm = reminder.alarm != true
                        )
                    ) {
                        onUpdate(it)
                    }
                }
            }

            val configuration = LocalConfiguration.current

            suspend fun setTimezone(timezone: RawTimeZone) {
                api.updateReminder(
                    id = reminder.id!!,
                    reminder = Reminder(
                        timezone = timezone.name,
                        utcOffset = timezone.rawOffsetInMinutes.toDouble() / 60.0
                    )
                ) {
                    onUpdate(it)
                }
            }

            val durationString = appString { duration }

            item(durationString) {
                scope.launch {
                    var duration = reminder.duration ?: 0L

                    val result = dialog(
                        title = durationString,
                        confirmButton = application.appString { confirm }
                    ) {
                        var hours by remember {
                            mutableStateOf(
                                duration.milliseconds.inWholeHours
                            )
                        }
                        var minutes by remember {
                            mutableStateOf(
                                duration.milliseconds.inWholeMinutes - hours.hours.inWholeMinutes
                            )
                        }

                        LaunchedEffect(hours, minutes) {
                            duration = hours.hours.inWholeMilliseconds + minutes.minutes.inWholeMilliseconds

                            if (hours == 24L) {
                                minutes = 0L
                            }
                        }

                        H3 {
                            // todo: translate
                            Text("Hours")
                        }
                        NumberInput(
                            value = hours,
                            min = 0,
                            max = 24,
                            attrs = {
                                classes(Styles.dateTimeInput)

                                style {
                                    padding(1.r)
                                }

                                onInput {
                                    runCatching {
                                        hours = (it.value?.toLong() ?: 0L).coerceIn(0L..24L)
                                    }
                                }
                            }
                        )
                        H3 {
                            // todo: translate
                            Text("Minutes")
                        }
                        NumberInput(
                            value = minutes,
                            min = 0,
                            max = 59,
                            attrs = {
                                classes(Styles.dateTimeInput)
                                style {
                                    padding(1.r)
                                }

                                onInput {
                                    minutes = (it.value?.toLong() ?: 0L).coerceIn(0L..59L)
                                }
                            }
                        )
                    }

                    if (result != null) {
                        api.updateReminder(
                            id = reminder.id!!,
                            reminder = Reminder(duration = duration),
                        ) {
                            onUpdate(it)
                        }
                    }
                }
            }

            item(appString { timezone }) {
                scope.launch {
                    searchDialog(
                        configuration = configuration,
                        title = application.appString { timezone },
                        defaultValue = reminder.timezone?.replace("_", " ") ?: "",
                        load = {
                            rawTimeZones.toList()
                        },
                        filter = { it, search ->
                            val hours = it.rawOffsetInMinutes.toDouble() / 60.0
                            "${it.name.replace("_", " ")} ${if (hours < 0) "" else "+"}$hours".contains(search, true)
                        }
                    ) { timezone, resolve ->
                        Div({
                            classes(AppStyles.groupItem)

                            onClick {
                                scope.launch {
                                    setTimezone(timezone)
                                    resolve(true)
                                }
                            }

                            focusable()
                        }) {
                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Column)
                                }
                            }) {
                                Div({
                                    classes(AppStyles.groupItemName)
                                }) {
                                    Text(timezone.name.replace("_", " "))
                                }
                                Div({
                                    classes(AppStyles.groupItemMessage)
                                }) {
                                    val hours = timezone.rawOffsetInMinutes.toDouble() / 60.0
                                    Text("UTC ${if (hours < 0) "" else "+"}$hours")
                                }
                            }
                        }
                    }
                }
            }

//            item("Groups") {
//
//            }

            item(appString { delete }) {
                scope.launch {
                    // Todo: translate
                    val result = dialog("Delete this reminder?", confirmButton = "Yes, delete") {
                        Text("You cannot undo this.")
                    }

                    if (result != true) return@launch

                    api.deleteReminder(reminder.id!!) {
                        onDelete(reminder)
                    }
                }
            }
        }
    }

    Div({
        style {
            flex(1)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            overflowY("auto")
            overflowX("hidden")
        }
    }) {
        EditField(
            reminder.note ?: "",
            appString { note },
            styles = {
                margin(1.r, 1.r, 0.r, 1.r)
            }
        ) {
            var success = false
            api.updateReminder(reminder.id!!, Reminder(note = it)) {
                success = true
                onUpdate(it)
            }

            success
        }
        ReminderEvents(reminder)
    }
    PageTopBar(
        title = reminder.title ?: appString { newGroup },
        description = bulletedString(
            reminder.categories?.firstOrNull(),
            reminder.scheduleText
        )
    ) {
        menuTarget = if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
    }
}
