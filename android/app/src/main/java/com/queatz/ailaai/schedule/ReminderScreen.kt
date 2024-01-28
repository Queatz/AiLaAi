package com.queatz.ailaai.schedule

import ReminderEvent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ailaai.api.deleteReminder
import app.ailaai.api.reminder
import app.ailaai.api.reminderOccurrences
import app.ailaai.api.updateReminder
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.hint
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.popBackStackOrFinish
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.AppBar
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.CardToolbar
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.dialogs.Alert
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Reminder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import toEvents

@Composable
fun ReminderScreen(reminderId: String) {
    val scope = rememberCoroutineScope()
    var isLoading by rememberStateOf(false)
    var showEditNote by rememberStateOf(false)
    var showReschedule by rememberStateOf(false)
    var showEditTitle by rememberStateOf(false)
    var showDelete by rememberStateOf(false)
    var reminder by rememberStateOf<Reminder?>(null)
    var events by rememberStateOf(emptyList<ReminderEvent>())
    val onExpand = remember {
        MutableSharedFlow<Unit>()
    }
    val nav = nav

    suspend fun reloadEvents() {
        if (reminder == null) {
            return
        }

        api.reminderOccurrences(
            reminderId,
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

    LaunchedEffect(Unit) {
        reload()
    }

    if (showEditNote) {
        TextFieldDialog({
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
            {
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
            {
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
            {
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
            CardToolbar {
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
                    Icons.Outlined.Delete,
                    stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error
                ) {
                    showDelete = true
                }
            }
            reminder?.note?.notBlank?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(
                            horizontal = 2.pad,
                            vertical = 1.pad
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                        .clip(MaterialTheme.shapes.large)
                        .padding(
                            horizontal = 2.pad,
                            vertical = 1.pad
                        )
                )
            }
            Text(
                stringResource(R.string.history),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(horizontal = 1.pad)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 1.pad)
            ) {
                items(events) {
                    PeriodEvent(
                        ScheduleView.Yearly,
                        it,
                        showOpen = false,
                        showFullTime = true,
                        modifier = Modifier
                            .fillMaxWidth(),
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
