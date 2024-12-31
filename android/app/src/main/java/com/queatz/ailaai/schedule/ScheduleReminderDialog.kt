package com.queatz.ailaai.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.ui.components.Check
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Reminder
import com.queatz.db.ReminderSchedule
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun ScheduleReminderDialog(
    onDismissRequest: () -> Unit,
    initialReminder: Reminder,
    confirmText: String? = null,
    showTitle: Boolean = false,
    onUpdate: suspend (Reminder) -> Unit
) {
    var reminder by remember(initialReminder) {
        mutableStateOf(
            Reminder(
                start = initialReminder.start,
                end = initialReminder.end,
                schedule = initialReminder.schedule
            )
        )
    }
    val scope = rememberCoroutineScope()
    var isLoading by rememberStateOf(false)
    var reoccurs by rememberStateOf(reminder.schedule != null)
    var until by rememberStateOf(reminder.end != null)
    var showStart by rememberStateOf(false)
    var showEnd by rememberStateOf(false)
    var showHours by rememberStateOf(false)
    var showDays by rememberStateOf(false)
    var showWeeks by rememberStateOf(false)
    var showMonths by rememberStateOf(false)
    val today = Clock.System.now().startOfDay()

    fun invalidate() {
        reminder = Reminder(
            person = reminder.person,
            people = reminder.people,
            groups = reminder.groups,
            attachment = reminder.attachment,
            open = reminder.open,
            title = reminder.title,
            note = reminder.note,
            start = reminder.start,
            end = reminder.end,
            timezone = reminder.timezone,
            utcOffset = reminder.utcOffset,
            schedule = reminder.schedule,
        )
    }

    LaunchedEffect(reminder.schedule?.hours?.isNotEmpty() != true) {
        if (reminder.schedule?.hours?.isNotEmpty() != true) {
            if (reminder.schedule == null) {
                reminder.schedule = ReminderSchedule()
            }
            reminder.schedule = reminder.schedule!!.copy(
                hours = (reminder.schedule?.hours ?: emptyList()) + reminder.start!!.hour()
            )
            invalidate()
        }
    }

    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(3.pad)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(1.pad)
            ) {
                if (showTitle) {
                    var title by rememberStateOf(initialReminder.title ?: "")
                    val focusRequester = remember { FocusRequester() }

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            reminder.title = it
                        },
                        label = {
                            Text(stringResource(R.string.title))
                        },
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
                OutlinedButton(
                    onClick = {
                        showStart = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(reminder.start!!.formatFull())
                }
                DateTimeSuggestions {
                    reminder.start = it
                    invalidate()
                }
                Check(reoccurs, { reoccurs = it }) {
                    Text(stringResource(R.string.reoccurs))
                }
                if (reoccurs) {
                    OutlinedButton(
                        onClick = {
                            showHours = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            reminder.schedule?.hours?.asNaturalList {
                                today.at(hour = it, minute = reminder.start!!.minute())
                                    .formatEventFull(ScheduleView.Daily)
                            } ?: stringResource(R.string.every_hour)
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            showDays = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val days = ((reminder.schedule?.weekdays?.map { it.dayOfWeekName } ?: emptyList()) + (reminder.schedule?.days?.map {
                            if (it == -1) {
                                stringResource(R.string.last_day_of_the_month)
                            } else {
                                stringResource(R.string.day_of_the_month, it.ordinal)
                            }
                        } ?: emptyList())).ifNotEmpty
                        Text(
                            days?.asNaturalList { it } ?: stringResource(R.string.every_day)
                        )
                    }
                    OutlinedButton(
                        {
                            showWeeks = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val context = LocalContext.current
                        Text(
                            reminder.schedule?.weeks?.ifNotEmpty?.asNaturalList {
                                context.getString(R.string.x_week, it.ordinal)
                            } ?: stringResource(R.string.every_week)
                        )
                    }
                    OutlinedButton(
                        {
                            showMonths = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            reminder.schedule?.months?.ifNotEmpty?.asNaturalList {
                                it.monthName
                            } ?: stringResource(R.string.every_month)
                        )
                    }
                }
                Check(until, { until = it }) {
                    Text(stringResource(R.string.until))
                }
                if (until) {
                    OutlinedButton(
                        onClick = {
                            showEnd = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text((reminder.end ?: reminder.start!!).formatFull())
                    }
                    DateTimeSuggestions {
                        reminder.end = it
                        invalidate()
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                TextButton(
                    onClick = {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            onUpdate(
                                Reminder(
                                    start = reminder.start,
                                    end = reminder.end?.takeIf { until },
                                    schedule = reminder.schedule?.takeIf { reoccurs },
                                    title = reminder.title?.takeIf { showTitle }
                                )
                            )
                            isLoading = false
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text(confirmText ?: stringResource(R.string.update))
                }
            }
        }
    }

    if (showHours) {
        Menu({
            showHours = false
        }) {
            (0..23).forEach {
                val checked = reminder.schedule?.hours?.contains(it) == true
                menuItem(
                    today.at(hour = it, minute = reminder.start!!.minute()).formatEventFull(ScheduleView.Daily),
                    icon = if (checked) Icons.Outlined.Check else null
                ) {
                    if (reminder.schedule == null) {
                        reminder.schedule = ReminderSchedule()
                    }
                    if (!checked) {
                        reminder.schedule = reminder.schedule!!.copy(
                            hours = ((reminder.schedule!!.hours ?: emptyList()) + it).sorted()
                        )
                    } else {
                        reminder.schedule = reminder.schedule!!.copy(
                            hours = (reminder.schedule!!.hours ?: emptyList()) - it
                        )
                    }
                    invalidate()
                }
            }
        }
    }

    if (showDays) {
        Menu({
            showDays = false
        }) {
            (1..7).forEach {
                val checked = reminder.schedule?.weekdays?.contains(it) == true
                menuItem(
                    it.dayOfWeekName,
                    icon = if (checked) Icons.Outlined.Check else null
                ) {
                    if (reminder.schedule == null) {
                        reminder.schedule = ReminderSchedule()
                    }
                    if (!checked) {
                        reminder.schedule = reminder.schedule!!.copy(
                            weekdays = ((reminder.schedule!!.weekdays ?: emptyList()) + it).sorted()
                        )
                    } else {
                        reminder.schedule = reminder.schedule!!.copy(
                            weekdays = (reminder.schedule!!.weekdays ?: emptyList()) - it
                        )
                    }
                    invalidate()
                }
            }
            (1..31).forEach {
                val checked = reminder.schedule?.days?.contains(it) == true
                menuItem(
                    title = stringResource(R.string.day_of_the_month, it.ordinal),
                    icon = if (checked) Icons.Outlined.Check else null
                ) {
                    if (reminder.schedule == null) {
                        reminder.schedule = ReminderSchedule()
                    }
                    if (!checked) {
                        reminder.schedule = reminder.schedule!!.copy(
                            days = ((reminder.schedule!!.days ?: emptyList()) + it).sortedBy {
                                if (it == -1) Int.MAX_VALUE else it
                            }
                        )
                    } else {
                        reminder.schedule = reminder.schedule!!.copy(
                            days = (reminder.schedule!!.days ?: emptyList()) - it
                        )
                    }
                    invalidate()
                }
            }
            run {
                val checked = reminder.schedule?.days?.contains(-1) == true
                menuItem(
                    title = stringResource(R.string.last_day_of_the_month),
                    icon = if (checked) Icons.Outlined.Check else null
                ) {
                    if (reminder.schedule == null) {
                        reminder.schedule = ReminderSchedule()
                    }
                    if (!checked) {
                        reminder.schedule = reminder.schedule!!.copy(
                            days = ((reminder.schedule!!.days ?: emptyList()) + -1).sortedBy {
                                if (it == -1) Int.MAX_VALUE else it
                            }
                        )
                    } else {
                        reminder.schedule = reminder.schedule!!.copy(
                            days = (reminder.schedule!!.days ?: emptyList()) - -1
                        )
                    }
                    invalidate()
                }
            }
        }
    }

    if (showWeeks) {
        Menu({
            showWeeks = false
        }) {
            (1..5).forEach {
                val checked = reminder.schedule?.weeks?.contains(it) == true
                menuItem(
                    title = stringResource(R.string.x_week, it.ordinal),
                    icon = if (checked) Icons.Outlined.Check else null
                ) {
                    if (reminder.schedule == null) {
                        reminder.schedule = ReminderSchedule()
                    }
                    if (!checked) {
                        reminder.schedule = reminder.schedule!!.copy(
                            weeks = ((reminder.schedule!!.weeks ?: emptyList()) + it).sorted()
                        )
                    } else {
                        reminder.schedule = reminder.schedule!!.copy(
                            weeks = (reminder.schedule!!.weeks ?: emptyList()) - it
                        )
                    }
                    invalidate()
                }
            }
        }
    }

    if (showMonths) {
        Menu({
            showMonths = false
        }) {
            (1..12).forEach {
                val checked = reminder.schedule?.months?.contains(it) == true
                menuItem(
                    title = it.monthName,
                    icon = if (checked) Icons.Outlined.Check else null
                ) {
                    if (reminder.schedule == null) {
                        reminder.schedule = ReminderSchedule()
                    }
                    if (!checked) {
                        reminder.schedule = reminder.schedule!!.copy(
                            months = ((reminder.schedule!!.months ?: emptyList()) + it).sorted()
                        )
                    } else {
                        reminder.schedule = reminder.schedule!!.copy(
                            months = (reminder.schedule!!.months ?: emptyList()) - it
                        )
                    }
                    invalidate()
                }
            }
        }
    }

    if (showStart) {
        RescheduleDialog(
            onDismissRequest = {
                showStart = false
            },
            date = reminder.start ?: Clock.System.now()
        ) {
            reminder.start = it
            invalidate()
        }
    }

    if (showEnd) {
        RescheduleDialog(
            onDismissRequest = {
                showEnd = false
            },
            date = reminder.end ?: Clock.System.now()
        ) {
            reminder.end = it
            invalidate()
        }
    }
}
