package com.queatz.ailaai.ui.event

import ReminderEvent
import ReminderEventType
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ailaai.api.joinReminder
import app.ailaai.api.leaveReminder
import coil3.compose.AsyncImage
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.contactPhoto
import com.queatz.ailaai.extensions.formatDateAndTime
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.services.authors
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.theme.pad
import kotlinx.coroutines.launch

@Composable
fun EventItem(
    event: ReminderEvent,
    modifier: Modifier = Modifier,
    onUpdated: () -> Unit,
    onClick: () -> Unit
) {
    EventItemContent(
        modifier = modifier,
        onClick = onClick,
        event = event,
        onLeave = {
            api.leaveReminder(
                id = event.reminder.id!!,
            ) {
                onUpdated()
            }
        },
        onJoin = {
            api.joinReminder(
                id = event.reminder.id!!,
            ) {
                onUpdated()
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventItemContent(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    event: ReminderEvent,
    onLeave: suspend () -> Unit,
    onJoin: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val me = me
    val reminder = event.reminder
    val isJoined = me?.id?.let { userId ->
        reminder.person == userId || (reminder.people ?: emptyList()).contains(userId)
    } ?: false

    val title = reminder.title ?: stringResource(R.string.untitled_event)
    val description = event.occurrence?.note?.notBlank ?: reminder.note?.notBlank ?: ""

    // Create a list of people photos
    val hostId = reminder.person!!
    val peopleIds = reminder.people ?: emptyList()
    val allPeopleIds = listOf(hostId) + peopleIds

    // For now, we'll just use placeholder photos
    val photos = allPeopleIds.mapNotNull { authors.get(it) }.map {
        it.contactPhoto()
    }

    // Format the event time
    val eventTime = when (event.event) {
        // todo: translate
        ReminderEventType.Start -> "Starts ${event.date.formatDateAndTime()}"
        // todo: translate
        ReminderEventType.End -> "Ends ${event.date.formatDateAndTime()}"
        ReminderEventType.Occur -> event.date.formatDateAndTime()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(1.pad),
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .let { mod ->
                if (onClick == null) {
                    mod
                } else {
                    mod.clickable {
                        onClick()
                    }
                }
            }
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            .padding(1.5f.pad)
    ) {
        reminder.photo?.notBlank?.let { photo ->
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

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            GroupPhoto(photos)
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 5,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Text(
            text = eventTime,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 1.pad)
        )

        // Show Join or Leave button
        if (reminder.open == true && me?.id != reminder.person) {
            var isLoadingJoinOrLeave by rememberStateOf(false)

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                if (isJoined) {
                    OutlinedButton(
                        onClick = {
                            isLoadingJoinOrLeave = true
                            scope.launch {
                                onLeave()
                                isLoadingJoinOrLeave = false
                            }
                        },
                        enabled = !isLoadingJoinOrLeave
                    ) {
                        Text(stringResource(R.string.leave))
                    }
                } else {
                    Button(
                        onClick = {
                            isLoadingJoinOrLeave = true
                            scope.launch {
                                onJoin()
                                isLoadingJoinOrLeave = false
                            }
                        },
                        enabled = !isLoadingJoinOrLeave
                    ) {
                        Text(stringResource(R.string.join))
                    }
                }
            }
        }
    }
}
