package com.queatz.ailaai.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import com.queatz.ailaai.R
import com.queatz.ailaai.api.deleteReminderOccurrence
import com.queatz.ailaai.api.updateReminderOccurrence
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.theme.PaddingDefault
import com.queatz.db.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@Composable
fun RowScope.PeriodEvent(
    view: ScheduleView,
    event: ReminderEvent,
    onExpand: MutableSharedFlow<Unit>,
    onUpdated: (ReminderEvent) -> Unit
) {
    val scope = rememberCoroutineScope()
    var expanded by rememberStateOf(false)
    var showEditNote by rememberStateOf(false)
    var showDelete by rememberStateOf(false)
    val done = event.occurrence?.done == true

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
            title = stringResource(com.queatz.ailaai.R.string.edit_note),
            button = stringResource(com.queatz.ailaai.R.string.update),
            initialValue = event.occurrence?.note ?: event.reminder.note ?: "",
        ) { note ->
            api.updateReminderOccurrence(
                event.reminder.id!!,
                event.date,
                ReminderOccurrence(note = note)
            ) {
                onUpdated(event)
                showEditNote = false
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable {
                scope.launch {
                    if (!expanded) {
                        onExpand.emit(Unit)
                    }
                    expanded = !expanded
                }
            }
            .padding(PaddingDefault)
            .weight(1f)
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
                event.date.formatEvent(view),
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
                onDone = {
                    scope.launch {
                        api.updateReminderOccurrence(
                            event.reminder.id!!,
                            event.date,
                            ReminderOccurrence(done = !done)
                        ) {
                            onUpdated(event)
                        }
                    }
                },
                onOpen = {

                },
                onEdit = {
                    showEditNote = true
                },
                onRemove = {
                    showDelete = true
                }
            )
        }
    }
}
