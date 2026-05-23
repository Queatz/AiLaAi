package com.queatz.ailaai.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.asInputProvider
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.SignalAttachments
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.SignalSendExtended
import kotlin.time.Clock

@Composable
fun SignalRepliesDialog(
    signalSend: SignalSendExtended,
    onDismissRequest: () -> Unit,
    onCancelSignal: () -> Unit,
    onCreateGroup: (List<String>) -> Unit
) {
    val selectedPeople = remember { mutableStateListOf<String>() }
    var showCancelConfirmation by remember { mutableStateOf(false) }

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
                        modifier = Modifier.fillMaxWidth().padding(bottom = 1.pad)
                    )

                    if (!signalSend.signalSend.message.isNullOrBlank()) {
                        Text(
                            text = signalSend.signalSend.message!!,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 1.pad)
                        )
                    }

                    SignalAttachments(
                        photo = signalSend.signalSend.photo,
                        audio = signalSend.signalSend.audio,
                        api = api,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 1.pad)
                    )

                    val now = Clock.System.now()
                    val totalDuration = (signalSend.signalSend.expiry!! - signalSend.signalSend.createdAt!!).inWholeMilliseconds
                    val remaining = (signalSend.signalSend.expiry!! - now).inWholeMilliseconds
                    val progress = (remaining.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().clip(CircleShape).padding(top = 1.pad)
                    )

                    val remainingDuration = (signalSend.signalSend.expiry!! - now)
                    val hours = remainingDuration.inWholeHours
                    val minutes = remainingDuration.inWholeMinutes % 60
                    Text(
                        text = if (hours > 0) stringResource(R.string.hours_and_minutes, hours, minutes) else stringResource(R.string.minutes_remaining, minutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 1.pad)
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
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val id = person?.id ?: reply.signalReply.person
                                id?.let {
                                    if (selectedPeople.contains(it)) selectedPeople.remove(it)
                                    else selectedPeople.add(it)
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(1.pad)
                        ) {
                            Checkbox(
                                checked = (person?.id ?: reply.signalReply.person)?.let { selectedPeople.contains(it) } ?: false,
                                onCheckedChange = null,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            AsyncImage(
                                model = person?.photo?.let { api.url(it) },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .padding(top = 1.pad)
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                            Column(modifier = Modifier.padding(start = 1.pad).weight(1f)) {
                                Text(person?.name ?: stringResource(R.string.someone), style = MaterialTheme.typography.titleSmall)
                                
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
                                    horizontalAlignment = Alignment.Start
                                )
                            }
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(top = 2.pad)
            ) {
                TextButton(onClick = { showCancelConfirmation = true }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.error)
                }
                Row {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.close))
                    }
                    Button(
                        onClick = { onCreateGroup(selectedPeople.toList()) },
                        enabled = selectedPeople.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.create_group_from_signal))
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
