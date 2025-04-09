package com.queatz.ailaai.schedule

import ReminderEvent
import android.R.id.toggle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import app.ailaai.api.deleteReminder
import app.ailaai.api.reminder
import app.ailaai.api.reminderOccurrences
import app.ailaai.api.updateReminder
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.ifNotEmpty
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.notEmpty
import com.queatz.ailaai.extensions.popBackStackOrFinish
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.authors
import com.queatz.ailaai.ui.components.AppBar
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.Toolbar
import com.queatz.ailaai.ui.components.Friends
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.dialogs.Alert
import com.queatz.ailaai.ui.dialogs.ChooseCategoryDialog
import com.queatz.ailaai.ui.dialogs.ChoosePeopleDialog
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Reminder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant.Companion.fromEpochMilliseconds
import toEvents

@Composable
fun ReminderScreen(reminderId: String) {
    val scope = rememberCoroutineScope()
    var isLoading by rememberStateOf(false)
    var showEditNote by rememberStateOf(false)
    var showAddPerson by rememberStateOf(false)
    var showReschedule by rememberStateOf(false)
    var showEditTitle by rememberStateOf(false)
    var showCategory by rememberStateOf(false)
    var showDelete by rememberStateOf(false)
    var showLeave by rememberStateOf(false)
    var reminder by rememberStateOf<Reminder?>(null)
    var events by rememberStateOf(emptyList<ReminderEvent>())
    val onExpand = remember {
        MutableSharedFlow<Unit>()
    }
    val nav = nav
    val me = me

    suspend fun reloadEvents() {
        if (reminder == null) {
            return
        }

        api.reminderOccurrences(
            id = reminderId,
            start = reminder!!.start!!,
            end = reminder!!.end ?: Clock.System.now()
        ) {
            events = it.toEvents().asReversed()
        }
    }

    suspend fun reload() {
        isLoading = true
        api.reminder(reminderId) {
            reminder = it
            reloadEvents()
        }
        isLoading = false
    }

    fun toggleAlarm() {
        scope.launch {
            val alarm = reminder?.alarm != true
            api.updateReminder(
                id = reminderId,
                reminder = Reminder(alarm = alarm)
            ) {
                reminder = it
                reloadEvents()
            }
        }
    }

    fun togglePosted() {
        scope.launch {
            val open = reminder?.open == true
            api.updateReminder(
                id = reminderId,
                reminder = Reminder(open = open)
            ) {
                reminder = it
                reloadEvents()
            }
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    if (showCategory) {
        ChooseCategoryDialog(
            onDismissRequest = {
                showCategory = false
            },
            preselect = reminder?.categories?.firstOrNull(),
        ) {
            scope.launch {
                api.updateReminder(reminderId, Reminder(
                    categories = it.inList()
                )) {
                    api.reminder(reminderId) {
                        reminder = it
                    }
                }
            }
        }
    }

    if (showAddPerson) {
        val someone = stringResource(R.string.someone)
        ChoosePeopleDialog(
            onDismissRequest = {
                showAddPerson = false
            },
            title = stringResource(R.string.invite_someone),
            confirmFormatter = defaultConfirmFormatter(
                R.string.invite_someone,
                R.string.invite_person,
                R.string.invite_x_and_y,
                R.string.invite_x_people
            ) { it.name ?: someone },
            onPeopleSelected = { people ->
                api.updateReminder(
                    id = reminderId,
                    reminder = Reminder(people = ((reminder!!.people ?: emptyList()) + people.map { it.id!! }).distinct())
                ) {
                    reload()
                }
            },
            omit = { it.id!! in (reminder?.people ?: emptyList()) + me?.id!! }
        )
    }

    if (showEditNote) {
        TextFieldDialog(
            onDismissRequest = {
                showEditNote = false
            },
            title = stringResource(R.string.edit_note),
            button = stringResource(R.string.update),
            showDismiss = true,
            dismissButtonText = stringResource(R.string.cancel),
            initialValue = reminder?.note ?: ""
        ) {
            api.updateReminder(reminderId, Reminder(note = it)) {
                reload()
                showEditNote = false
            }
        }
    }

    if (showReschedule && reminder != null) {
        ScheduleReminderDialog(
            onDismissRequest = {
                showReschedule = false
            },
            initialReminder = reminder!!
        ) {
            api.updateReminder(reminderId, Reminder(
                start = it.start,
                end = it.end,
                schedule = it.schedule
            )) {
                reload()
                showReschedule = false
            }
        }
    }

    if (showEditTitle) {
        TextFieldDialog(
            onDismissRequest = {
                showEditTitle = false
            },
            title = stringResource(R.string.title),
            button = stringResource(R.string.update),
            showDismiss = true,
            dismissButtonText = stringResource(R.string.cancel),
            initialValue = reminder?.title ?: ""
        ) {
            api.updateReminder(reminderId, Reminder(title = it)) {
                reload()
                showEditTitle = false
            }
        }
    }

    if (showDelete) {
        Alert(
            onDismissRequest = {
                showDelete = false
            },
            title = stringResource(R.string.delete_this_reminder),
            text = stringResource(R.string.you_cannot_undo_this_reminder),
            dismissButton = stringResource(R.string.cancel),
            confirmButton = stringResource(R.string.delete),
            confirmColor = MaterialTheme.colorScheme.error
        ) {
            scope.launch {
                api.deleteReminder(reminderId) {
                    nav.popBackStackOrFinish()
                }
            }
        }
    }

    if (showLeave) {
        Alert(
            onDismissRequest = {
                showLeave = false
            },
            title = stringResource(R.string.leave_reminder),
            text = null,
            dismissButton = stringResource(R.string.cancel),
            confirmButton = stringResource(R.string.yes),
            confirmColor = MaterialTheme.colorScheme.error
        ) {
            scope.launch {
                api.updateReminder(reminderId, Reminder(people = reminder?.people?.filter { it != me?.id })) {
                    nav.popBackStackOrFinish()
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        AppBar(
            title = {
                Column {
                    Text(reminder?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    reminder?.scheduleText?.notBlank?.let {
                        Text(
                            it,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            },
            navigationIcon = {
                BackButton()
            }
        )

        if (isLoading) {
            Loading()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(1.pad),
                modifier = Modifier
                    .fillMaxSize()
            ) {
                item {
                    Toolbar {
                        item(
                            icon = if (reminder?.open == true) Icons.Outlined.ToggleOn else Icons.Outlined.ToggleOff,
                            name = stringResource(if (reminder?.open == true) R.string.posted else R.string.not_posted),
                            selected = reminder?.open == true
                        ) {
                            togglePosted()
                        }
                        item(
                            Icons.Outlined.PersonAdd,
                            stringResource(R.string.invite_someone)
                        ) {
                            showAddPerson = true
                        }
                        item(
                            Icons.Outlined.EditNote,
                            stringResource(R.string.edit_note)
                        ) {
                            showEditNote = true
                        }
                        item(
                            Icons.Outlined.Update,
                            stringResource(R.string.reschedule)
                        ) {
                            showReschedule = true
                        }
                        item(
                            Icons.Outlined.Edit,
                            stringResource(R.string.rename)
                        ) {
                            showEditTitle = true
                        }
                        item(
                            icon = if (reminder?.alarm == true) Icons.Outlined.Alarm else Icons.Outlined.AlarmOff,
                            name = if (reminder?.alarm == true) stringResource(R.string.alarm_on) else stringResource(R.string.alarm_off),
                            selected = reminder?.alarm == true
                        ) {
                            toggleAlarm()
                        }
                        val category = reminder?.categories?.firstOrNull()
                        item(
                            Icons.Outlined.Category,
                            category ?: stringResource(R.string.set_category),
                                    selected = category != null
                        ) {
                            showCategory = true
                        }
                        if (reminder?.person == me?.id) {
                            item(
                                Icons.Outlined.Delete,
                                stringResource(R.string.delete),
                                color = MaterialTheme.colorScheme.error
                            ) {
                                showDelete = true
                            }
                        } else if (me?.id in (reminder?.people ?: emptyList())) {
                            item(
                                Icons.Outlined.Clear,
                                stringResource(R.string.leave),
                                color = MaterialTheme.colorScheme.error
                            ) {
                                showLeave = true
                            }
                        }
                    }
                    reminder?.note?.notBlank?.let {
                        OutlinedCard(
                            onClick = {
                                showEditNote = true
                            },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(1.pad)
                        ) {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .padding(1.pad)
                            )
                        }
                    }
                    reminder?.let { reminder ->
                        (reminder.person!!.inList() + (reminder.people ?: emptyList()))
                            .distinct()
                            .mapNotNull { authors.get(it) }
                            .sortedByDescending { it.seen ?: fromEpochMilliseconds(0) }
                            .takeIf { it.size > 1 }
                            ?.let { people ->
                                Text(
                                    stringResource(R.string.people) + " (${people.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier
                                        .padding(horizontal = 1.pad)
                                )
                                Friends(
                                    people = people,
                                    modifier = Modifier
                                        .padding(vertical = 1.pad)
                                ) {
                                    nav.appNavigate(AppNav.Profile(it.id!!))
                                }
                            }
                    }
                    Text(
                        stringResource(R.string.history),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .padding(horizontal = 1.pad)
                    )
                    if (events.isEmpty()) {
                        Text(
                            stringResource(R.string.none),
                            modifier = Modifier
                                .padding(horizontal = 1.pad)
                        )
                    }
                }
                items(events) {
                    PeriodEvent(
                        ScheduleView.Yearly,
                        it,
                        showOpen = false,
                        showFullTime = true,
                        onExpand = onExpand,
                        onUpdated = {
                            scope.launch {
                                reloadEvents()
                            }
                        }
                    )
                }
            }
        }
    }
}
