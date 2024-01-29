package com.queatz.ailaai.schedule

import ReminderEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import app.ailaai.api.deleteReminderOccurrence
import app.ailaai.api.updateReminderOccurrence
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.ReminderOccurrence
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import updateDate

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
            listOfNotNull(
                if (showFullTime) event.date.formatEventFull(ScheduleView.Yearly) else event.date.formatEvent(view),
                event.occurrence?.note ?: event.reminder.note
            ).joinToString(" • "),
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
                    nav.navigate("reminder/${event.reminder.id!!}")
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
