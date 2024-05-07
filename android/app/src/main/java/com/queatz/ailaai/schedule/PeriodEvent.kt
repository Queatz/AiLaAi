package com.queatz.ailaai.schedule

import ReminderEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.ailaai.api.deleteReminderOccurrence
import app.ailaai.api.updateReminderOccurrence
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.bulletedString
import com.queatz.ailaai.extensions.contactPhoto
import com.queatz.ailaai.extensions.ifNotEmpty
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.navigate
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.authors
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.ReminderOccurrence
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant.Companion.fromEpochMilliseconds
import updateDate
import kotlin.random.Random

@Composable
fun PeriodEvent(
    view: ScheduleView,
    event: ReminderEvent,
    showOpen: Boolean,
    showFullTime: Boolean,
    onExpand: MutableSharedFlow<Unit>,
    modifier: Modifier = Modifier,
    onUpdated: (ReminderEvent) -> Unit
) {
    val scope = rememberCoroutineScope()
    var expanded by rememberStateOf(false)
    var showEditNote by rememberStateOf(false)
    var showReschedule by rememberStateOf(false)
    var showDelete by rememberStateOf(false)
    val done = event.occurrence?.done == true
    val nav = nav
    val me = me

    LaunchedEffect(Unit) {
        onExpand.collect {
            expanded = false
        }
    }

    if (showDelete) {
        AlertDialog(
            {
                showDelete = false
            },
            title = {
                Text(stringResource(R.string.delete_this_occurrence))
            },
            dismissButton = {
                TextButton({
                    showDelete = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button({
                    scope.launch {
                        api.deleteReminderOccurrence(
                            event.reminder.id!!,
                            event.date
                        ) {
                            onUpdated(event)
                        }
                    }
                    showDelete = false
                }) {
                    Text(stringResource(R.string.yes_delete))
                }
            }
        )
    }

    if (showEditNote) {
        TextFieldDialog(
            {
                showEditNote = false
            },
            title = stringResource(R.string.edit_note),
            button = stringResource(R.string.update),
            initialValue = event.occurrence?.note ?: event.reminder.note ?: "",
        ) { note ->
            api.updateReminderOccurrence(
                event.reminder.id!!,
                event.updateDate,
                ReminderOccurrence(note = note)
            ) {
                onUpdated(event)
                showEditNote = false
            }
        }
    }

    if (showReschedule) {
        RescheduleDialog(
            {
                showReschedule = false
            },
            event.date
        ) {
            scope.launch {
                api.updateReminderOccurrence(
                    event.reminder.id!!,
                    event.updateDate,
                    ReminderOccurrence(date = it)
                ) {
                    onUpdated(event)
                    showEditNote = false
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable {
                scope.launch {
                    if (!expanded) {
                        onExpand.emit(Unit)
                    }
                    expanded = !expanded
                }
            }
            .padding(1.pad)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.pad),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            (event.reminder.person!!.inList() + (event.reminder.people ?: emptyList()))
                .distinct()
                .filter { it != me?.id }
                .ifNotEmpty
                ?.mapNotNull { authors.get(it)?.contactPhoto() }
                ?.sortedByDescending { it.seen ?: fromEpochMilliseconds(0) }
                ?.let { people ->
                    GroupPhoto(
                        people,
                        padding = 0.pad,
                        size = 32.dp
                    )
                }
            Column {
                Text(
                    event.reminder.title ?: "",
                    style = MaterialTheme.typography.bodyMedium.let {
                        if (done) {
                            it.copy(textDecoration = TextDecoration.LineThrough)
                        } else {
                            it
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurface.let {
                        if (done) {
                            it.copy(alpha = .5f)
                        } else {
                            it
                        }
                    }
                )
                Text(
                    bulletedString(
                        if (showFullTime) event.date.formatEventFull(ScheduleView.Yearly) else event.date.formatEvent(
                            view
                        ),
                        event.occurrence?.note ?: event.reminder.note
                    ),
                    style = MaterialTheme.typography.labelSmall.let {
                        if (done) {
                            it.copy(textDecoration = TextDecoration.LineThrough)
                        } else {
                            it
                        }
                    },
                    color = MaterialTheme.colorScheme.secondary.let {
                        if (done) {
                            it.copy(alpha = .5f)
                        } else {
                            it
                        }
                    }
                )
            }
        }
        AnimatedVisibility(expanded) {
            ScheduleItemActions(
                {
                    expanded = false
                },
                showOpen = showOpen,
                onDone = {
                    scope.launch {
                        api.updateReminderOccurrence(
                            event.reminder.id!!,
                            event.updateDate,
                            ReminderOccurrence(done = !done)
                        ) {
                            onUpdated(event)
                        }
                    }
                },
                onOpen = {
                    nav.navigate(AppNav.Reminder(event.reminder.id!!))
                },
                onEdit = {
                    showEditNote = true
                },
                onReschedule = {
                    showReschedule = true
                },
                onRemove = {
                    showDelete = true
                }
            )
        }
    }
}
