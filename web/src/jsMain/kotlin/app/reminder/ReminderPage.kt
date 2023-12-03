package app.reminder

import LocalConfiguration
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
import com.queatz.db.Reminder
import focusable
import kotlinx.coroutines.launch
import lib.RawTimeZone
import lib.getTimezoneOffset
import lib.rawTimeZones
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r
import kotlin.js.Date

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
                        reminder.id!!,
                        Reminder(title = title)
                    ) {
                        onUpdate(it)
                    }
                }
            }

            item(appString { reschedule }) {
                scope.launch {
                    val result = dialog(application.appString { reschedule }, application.appString { update }) {
                        EditReminderSchedule(schedule)
                    }

                    if (result == true) {
                        api.updateReminder(
                            reminder.id!!,
                            Reminder(
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

            val configuration = LocalConfiguration.current

            suspend fun setTimezone(timezone: RawTimeZone) {
                api.updateReminder(
                    reminder.id!!,
                    Reminder(
                        timezone = timezone.name,
                        utcOffset = timezone.rawOffsetInMinutes.toDouble() / 60.0
                    )
                ) {
                    onUpdate(it)
                }
            }

            item(appString { timezone }) {
                scope.launch {
                    searchDialog(
                        configuration,
                        application.appString { timezone },
                        defaultValue = reminder.timezone?.replace("_", " ") ?: "",
                        load = {
                            rawTimeZones.toList()
                        },
                        filter = { it, search ->
                            it.name.replace("_", " ").contains(search, true)
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
        reminder.title ?: appString { newGroup },
        reminder.scheduleText
    ) {
        menuTarget = if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
    }
}
