package com.queatz.ailaai.schedule

import ReminderEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.bulletedString
import com.queatz.ailaai.extensions.contactPhoto
import com.queatz.ailaai.extensions.fadingEdge
import com.queatz.ailaai.extensions.format
import com.queatz.ailaai.extensions.formatDateAndTime
import com.queatz.ailaai.extensions.formatTime
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.notEmpty
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.services.authors
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.dialogs.DialogCloseButton
import com.queatz.ailaai.ui.story.StoryContents
import com.queatz.ailaai.ui.story.StorySource
import com.queatz.ailaai.ui.story.asStoryContents
import com.queatz.ailaai.ui.theme.pad
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant.Companion.fromEpochMilliseconds

@Composable
fun ScheduleEventPreviewDialog(
    onDismissRequest: () -> Unit,
    event: ReminderEvent,
    onOpen: (() -> Unit)? = null,
) {
    val me = me
    val done = event.occurrence?.done == true
    val note = (event.occurrence?.note ?: event.reminder.note)?.notBlank
    val durationText = (event.occurrence?.duration ?: event.reminder.duration)
        ?.takeIf { it > 0 }
        ?.let { duration ->
            "${stringResource(R.string.inline_to)} ${(event.date + duration.milliseconds).formatTime()} (${duration.milliseconds.format()})"
        }
    val dateTimeText = listOfNotNull(
        event.date.formatDateAndTime().notBlank,
        durationText
    ).joinToString(" ").notBlank

    val metadata = bulletedString(
        "⏰".takeIf { event.reminder.alarm == true },
        dateTimeText,
        event.reminder.categories?.firstOrNull()
    )

    val scheduleText = if (event.reminder.schedule != null) {
        event.reminder.scheduleText.notBlank
    } else {
        null
    }

    val people = (event.reminder.person?.inList().orEmpty() + (event.reminder.people ?: emptyList()))
        .distinct()
        .filter { it != me?.id }
        .notEmpty
        ?.mapNotNull { authors.get(it)?.contactPhoto() }
        ?.sortedByDescending { it.seen ?: fromEpochMilliseconds(0) }

    DialogBase(onDismissRequest) {
        DialogLayout(
            scrollable = false,
            content = {
                val scrollState = rememberScrollState()
                var viewport by rememberStateOf(Size.Zero)

                Column(
                    verticalArrangement = Arrangement.spacedBy(1.pad),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(bottom = 1.pad)
                        .verticalScroll(scrollState)
                        .onPlaced { viewport = it.boundsInParent().size }
                        .fadingEdge(viewport, scrollState, factor = 10f)
                ) {
                    SelectionContainer {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(1.pad),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(1.pad),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                people?.let {
                                    GroupPhoto(
                                        it,
                                        padding = 0.pad,
                                        size = 32.dp
                                    )
                                }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(.5f.pad),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    event.reminder.title?.notBlank?.let { title ->
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodyMedium.let {
                                                if (done) {
                                                    it.copy(textDecoration = TextDecoration.LineThrough)
                                                } else {
                                                    it
                                                }
                                            },
                                            color = MaterialTheme.colorScheme.onSurface.let {
                                                if (done) it.copy(alpha = .5f) else it
                                            }
                                        )
                                    }
                                    if (metadata.isNotBlank()) {
                                        Text(
                                            text = metadata,
                                            style = MaterialTheme.typography.labelSmall.let {
                                                if (done) {
                                                    it.copy(textDecoration = TextDecoration.LineThrough)
                                                } else {
                                                    it
                                                }
                                            },
                                            color = MaterialTheme.colorScheme.secondary.let {
                                                if (done) it.copy(alpha = .5f) else it
                                            }
                                        )
                                    }
                                    scheduleText?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary.let { color ->
                                                if (done) color.copy(alpha = .5f) else color
                                            }
                                        )
                                    }
                                }
                            }

                            note?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall.let { style ->
                                        if (done) {
                                            style.copy(textDecoration = TextDecoration.LineThrough)
                                        } else {
                                            style
                                        }
                                    },
                                    color = MaterialTheme.colorScheme.secondary.let { color ->
                                        if (done) color.copy(alpha = .5f) else color
                                    }
                                )
                            }
                        }
                    }

                    event.reminder.photo?.notBlank?.let { photo ->
                        AsyncImage(
                            model = photo.let(api::url),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.5f)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(0.dp))
                        )
                    }

                    event.reminder.content?.notBlank?.let { content ->
                        event.reminder.id?.let { reminderId ->
                            val gridState = rememberLazyGridState()
                            val contents by remember(content) { mutableStateOf(content.asStoryContents()) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp)
                            ) {
                                StoryContents(
                                    source = StorySource.Reminder(reminderId),
                                    content = contents,
                                    state = gridState,
                                    horizontalPadding = 0.dp,
                                    fade = true
                                )
                            }
                        }
                    }
                }
            },
            actions = {
                DialogCloseButton(onDismissRequest)
                if (onOpen != null) {
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            onOpen()
                        }
                    ) {
                        Text(stringResource(R.string.open))
                    }
                }
            }
        )
    }
}
