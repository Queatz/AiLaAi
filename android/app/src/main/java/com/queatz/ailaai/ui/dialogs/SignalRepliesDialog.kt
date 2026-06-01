package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.SignalAttachments
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Person
import com.queatz.db.Signal
import com.queatz.db.SignalSendExtended
import kotlin.time.Clock

@Composable
fun SignalRepliesDialog(
    signalSend: SignalSendExtended,
    onDismissRequest: () -> Unit,
    onCancelSignal: () -> Unit,
    onCreateGroup: (List<String>) -> Unit,
) {
    val selectedPeople = remember { mutableStateListOf<String>() }
    var showCancelConfirmation by remember { mutableStateOf(false) }
    var personToStatus by remember { mutableStateOf<Person?>(null) }
    var affinitySignalsToDialog by remember { mutableStateOf<List<Signal>?>(null) }
    var photoToShow by remember { mutableStateOf<String?>(null) }
    val nav = nav

    val someone = stringResource(R.string.someone)
    val confirmFormatter = defaultConfirmFormatter<Person>(
        R.string.create_group,
        R.string.new_group_with_person,
        R.string.new_group_with_people,
        R.string.new_group_with_x_people
    ) { it.name ?: someone }

    personToStatus?.let { person ->
        PersonStatusDialog(
            onDismissRequest = { personToStatus = null },
            person = person,
            personStatus = null,
            onMessageClick = {
                personToStatus = null
                person.id?.let { nav.appNavigate(AppNav.Group(it)) }
            },
            onProfileClick = {
                personToStatus = null
                person.id?.let { nav.appNavigate(AppNav.Profile(it)) }
            },
            onUseStatus = {
                personToStatus = null
            },
            affinitySignals = affinitySignalsToDialog
        )
    }

    photoToShow?.let {
        PhotoDialog(
            onDismissRequest = { photoToShow = null },
            initialMedia = Media.Photo(it),
            medias = listOf(Media.Photo(it))
        )
    }

    DialogBase(
        onDismissRequest,
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Vertical))
    ) {
        Column(
            modifier = Modifier
                .padding(3.pad)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.pad)
            ) {
                item {
                    Text(
                        text = signalSend.signal?.emoji ?: "👋",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = signalSend.signal?.name ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 1.pad)
                    )

                    if (!signalSend.signalSend.message.isNullOrBlank()) {
                        Text(
                            text = signalSend.signalSend.message!!,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 1.pad)
                        )
                    }

                    SignalAttachments(
                        photo = signalSend.signalSend.photo,
                        audio = signalSend.signalSend.audio,
                        api = api,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 1.pad),
                        onClickPhoto = { photoToShow = it }
                    )

                    val now = Clock.System.now()
                    val totalDuration =
                        (signalSend.signalSend.expiry!! - signalSend.signalSend.createdAt!!).inWholeMilliseconds
                    val remaining = (signalSend.signalSend.expiry!! - now).inWholeMilliseconds
                    val progress = (remaining.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .padding(top = 1.pad)
                    )

                    val remainingDuration = (signalSend.signalSend.expiry!! - now)
                    val hours = remainingDuration.inWholeHours
                    val minutes = remainingDuration.inWholeMinutes % 60

                    Text(
                        text = if (hours > 0) stringResource(
                            R.string.hours_and_minutes,
                            hours,
                            minutes
                        ) else stringResource(R.string.minutes_remaining, minutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 1.pad)
                    )

                    Text(
                        text = stringResource(R.string.replies),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 1.pad, bottom = 1.pad)
                    )
                }

                val replies = signalSend.replies ?: emptyList()
                if (replies.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.waiting_for_replies),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                items(replies) { reply ->
                    val person = reply.person
                    Surface(
                        onClick = {
                            val id = person?.id ?: reply.signalReply.person
                            id?.let {
                                if (selectedPeople.contains(it)) selectedPeople.remove(it)
                                else selectedPeople.add(it)
                            }
                        },
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(1.pad),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(1.pad)
                        ) {
                            Checkbox(
                                checked = (person?.id ?: reply.signalReply.person)?.let { selectedPeople.contains(it) }
                                    ?: false,
                                onCheckedChange = null,
                                modifier = Modifier.padding(horizontal = 0.5f.pad)
                            )
                            AsyncImage(
                                model = person?.photo?.let { api.url(it) },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .padding(vertical = 1.pad)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        personToStatus = person ?: Person().apply {
                                            id = reply.signalReply.person
                                            name = someone
                                        }
                                        affinitySignalsToDialog = reply.affinitySignals
                                    }
                            )
                            Column(modifier = Modifier
                                .padding(start = 1.pad)
                                .weight(1f)) {
                                Text(
                                    person?.name ?: stringResource(R.string.someone),
                                    style = MaterialTheme.typography.titleSmall
                                )

                                val affinitySignals = reply.affinitySignals
                                if (!affinitySignals.isNullOrEmpty()) {
                                    Text(
                                        affinitySignals.joinToString(" ") { it.emoji ?: "" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                if (!reply.signalReply.message.isNullOrBlank()) {
                                    Text(reply.signalReply.message!!, style = MaterialTheme.typography.bodyMedium)
                                }

                                SignalAttachments(
                                    photo = reply.signalReply.photo,
                                    audio = reply.signalReply.audio,
                                    api = api,
                                    modifier = Modifier.padding(top = 0.5f.pad),
                                    horizontalAlignment = Alignment.Start,
                                    onClickPhoto = { photoToShow = it }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.pad)
            ) {
                if (selectedPeople.isEmpty()) {
                    TextButton(onClick = { showCancelConfirmation = true }) {
                        Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.error)
                    }
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.close))
                    }
                    Button(
                        onClick = { onCreateGroup(selectedPeople.toList()) },
                        enabled = selectedPeople.isNotEmpty()
                    ) {
                        Text(
                            confirmFormatter(
                                selectedPeople.map { id ->
                                    signalSend.replies?.find { (it.person?.id ?: it.signalReply.person) == id }?.person
                                        ?: Person().apply {
                                            this.id = id
                                        }
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = { Text(stringResource(R.string.cancel_signal_q)) },
            text = { Text(stringResource(R.string.cancel_signal_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    showCancelConfirmation = false
                    onCancelSignal()
                }) {
                    Text(stringResource(R.string.yes), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmation = false }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }
}
