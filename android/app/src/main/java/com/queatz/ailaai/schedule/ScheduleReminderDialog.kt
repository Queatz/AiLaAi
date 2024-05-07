package com.queatz.ailaai.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    onUpdate: suspend (Reminder) -> Unit
) {
    val reminder = remember(initialReminder) {
        Reminder(
            start = initialReminder.start,
            end = initialReminder.end,
            schedule = initialReminder.schedule
        )
    }
    val scope = rememberCoroutineScope()
    val recomposeScope = currentRecomposeScope
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

    LaunchedEffect(reminder.schedule?.hours?.isNotEmpty() != true) {
        if (reminder.schedule?.hours?.isNotEmpty() != true) {
            if (reminder.schedule == null) {
                reminder.schedule = ReminderSchedule()
            }
            reminder.schedule = reminder.schedule!!.copy(
                hours = (reminder.schedule?.hours ?: emptyList()) + reminder.start!!.hour()
            )
            recomposeScope.invalidate()
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
                OutlinedButton(
                    {
                        showStart = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(reminder.start!!.formatFull())
                }
                DateTimeSuggestions {
                    reminder.start = it
                    recomposeScope.invalidate()
                }
                Check(reoccurs, { reoccurs = it }) {
                    Text(stringResource(R.string.reoccurs))
                }
                if (reoccurs) {
                    OutlinedButton(
                        {
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
                        {
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
                        {
                            showEnd = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text((reminder.end ?: reminder.start!!).formatFull())
                    }
                    DateTimeSuggestions {
                        reminder.end = it
                        recomposeScope.invalidate()
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    {
                        scope.launch {
                            isLoading = true
                            onUpdate(
                                Reminder(
                                    start = reminder.start,
                                    end = reminder.end?.takeIf { until },
                                    schedule = reminder.schedule?.takeIf { reoccurs }
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
                    recomposeScope.invalidate()
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
                    recomposeScope.invalidate()
                }
            }
            (1..31).forEach {
                val checked = reminder.schedule?.days?.contains(it) == true
                menuItem(
                    stringResource(R.string.day_of_the_month, it.ordinal),
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
                    recomposeScope.invalidate()
                }
            }
            run {
                val checked = reminder.schedule?.days?.contains(-1) == true
                menuItem(
                    stringResource(R.string.last_day_of_the_month),
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
                    recomposeScope.invalidate()
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
                    stringResource(R.string.x_week, it.ordinal),
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
                    recomposeScope.invalidate()
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
                    it.monthName,
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
                    recomposeScope.invalidate()
                }
            }
        }
    }

    if (showStart) {
        RescheduleDialog(
            {
                showStart = false
            },
            reminder.start ?: Clock.System.now()
        ) {
            reminder.start = it
            recomposeScope.invalidate()
        }
    }

    if (showEnd) {
        RescheduleDialog(
            {
                showEnd = false
            },
            reminder.end ?: Clock.System.now()
        ) {
            reminder.end = it
            recomposeScope.invalidate()
        }
    }
}
