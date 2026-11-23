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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ailaai.api.deleteReminderOccurrence
import app.ailaai.api.updateReminderOccurrence
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.bulletedString
import com.queatz.ailaai.extensions.contactPhoto
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.notEmpty
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
import kotlin.time.Instant.Companion.fromEpochMilliseconds
import updateDate

private const val PeriodEventMaxLines = 4

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
    var showPreview by rememberStateOf(false)
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

    if (showPreview) {
        ScheduleEventPreviewDialog(
            onDismissRequest = {
                showPreview = false
            },
            event = event,
            onOpen = if (showOpen) {
                {
                    nav.appNavigate(AppNav.Reminder(event.reminder.id!!))
                }
            } else {
                null
            }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = {
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
            onDismissRequest = {
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
            onDismissRequest = {
                showReschedule = false
            },
            date = event.date
        ) {
            scope.launch {
                api.updateReminderOccurrence(
                    id = event.reminder.id!!,
                    occurrence = event.updateDate,
                    update = ReminderOccurrence(date = it)
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
                .notEmpty
                ?.mapNotNull { authors.get(it)?.contactPhoto() }
                ?.sortedByDescending { it.seen ?: fromEpochMilliseconds(0) }
                ?.let { people ->
                    GroupPhoto(
                        people,
                        padding = 0.pad,
                        size = 32.dp
                    )
                }
            val title = event.reminder.title ?: ""
            val subtitle = bulletedString(
                "⏰".takeIf { event.reminder.alarm == true },
                if (showFullTime) event.date.formatEventFull(ScheduleView.Yearly) else event.date.formatEvent(
                    view
                )?.notBlank,
                event.reminder.categories?.firstOrNull(),
                (event.occurrence?.note ?: event.reminder.note)?.notBlank
            )
            val titleMaxLines = if (subtitle.isNotBlank()) PeriodEventMaxLines - 1 else PeriodEventMaxLines
            var titleLineCount by remember(title, subtitle) {
                mutableIntStateOf(if (title.isNotBlank()) 1 else 0)
            }
            val subtitleMaxLines = (PeriodEventMaxLines - titleLineCount).coerceAtLeast(1)

            Column {
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        maxLines = titleMaxLines,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { result ->
                            if (titleLineCount != result.lineCount) {
                                titleLineCount = result.lineCount
                            }
                        },
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
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        maxLines = subtitleMaxLines,
                        overflow = TextOverflow.Ellipsis,
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
        }
        AnimatedVisibility(expanded) {
            ScheduleItemActions(
                onDismissRequest = {
                    expanded = false
                },
                showOpen = showOpen,
                onPreview = {
                    showPreview = true
                },
                onDone = {
                    scope.launch {
                        api.updateReminderOccurrence(
                            id = event.reminder.id!!,
                            occurrence = event.updateDate,
                            update = ReminderOccurrence(done = !done)
                        ) {
                            onUpdated(event)
                        }
                    }
                },
                onOpen = {
                    nav.appNavigate(AppNav.Reminder(event.reminder.id!!))
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
