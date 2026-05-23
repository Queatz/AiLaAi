package com.queatz.ailaai.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.bluesource.choicesdk.maps.common.LatLng
import coil3.compose.rememberAsyncImagePainter
import com.queatz.ailaai.extensions.formatTime
import com.queatz.ailaai.extensions.toList
import com.queatz.ailaai.R
import app.ailaai.api.uploadAudio
import com.queatz.ailaai.api.uploadPhotosFromUris
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.asInputProvider
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.helpers.audioRecorder
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.SignalAttachmentsEditor
import com.queatz.ailaai.ui.theme.pad
import kotlin.math.roundToInt
import java.util.Calendar
import com.queatz.db.*
import kotlinx.coroutines.launch

@Composable
fun SendSignalDialog(
    signal: Signal,
    geo: LatLng?,
    onDismissRequest: () -> Unit,
    onSubmit: (SendSignalBody) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf(1000.0) }
    var duration by remember { mutableLongStateOf(60 * 60 * 1000L) }
    var audience by remember { mutableStateOf(SignalAudience.Nearby) }
    var selectedGroups by remember { mutableStateOf(listOf<Group>()) }
    var showChooseGroups by remember { mutableStateOf(false) }
    var photo by remember { mutableStateOf<android.net.Uri?>(null) }
    var audio by remember { mutableStateOf<String?>(null) }
    var showChoosePhoto by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingAudioDuration by remember { mutableLongStateOf(0L) }
    var isUploading by remember { mutableStateOf(false) }

    val audioRecorder = audioRecorder(
        onIsRecordingAudio = { isRecordingAudio = it },
        onRecordingAudioDuration = { recordingAudioDuration = it },
        onPermissionDenied = { context.toast(R.string.permission_denied) },
        onFailed = { context.showDidntWork() }
    ) { file ->
        audio = android.net.Uri.fromFile(file).toString()
        var success = false
        isUploading = true
        api.uploadAudio(file.asInputProvider(), onError = {
            isUploading = false
            audio = null
            context.showDidntWork()
        }) { response ->
            audio = response.urls.firstOrNull()
            success = true
        }
        isUploading = false
        success
    }

    val durations = listOf(
        15 * 60 * 1000L to "15m",
        30 * 60 * 1000L to "30m",
        45 * 60 * 1000L to "45m",
        60 * 60 * 1000L to "1h",
        2 * 60 * 60 * 1000L to "2h",
        3 * 60 * 60 * 1000L to "3h",
        4 * 60 * 60 * 1000L to "4h"
    )
    val radii = listOf(
        100.0,
        250.0,
        500.0,
        1000.0,
        2500.0,
        5000.0
    )


    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(3.pad)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = signal.emoji ?: "👋",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(1.pad))

            Text(
                text = signal.name ?: "",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.pad)
            )

            AnimatedVisibility(!isRecordingAudio) {
                Column {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text(stringResource(R.string.optional_message)) },
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(2.pad))

                    Text(stringResource(R.string.audience), style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(0.5f.pad),
                        modifier = Modifier.padding(vertical = 0.5f.pad)
                    ) {
                        SignalAudience.entries.forEach { a ->
                            FilterChip(
                                selected = audience == a,
                                onClick = { audience = a },
                                label = {
                                    Text(
                                        when (a) {
                                            SignalAudience.Nearby -> stringResource(R.string.nearby)
                                            SignalAudience.Friends -> stringResource(R.string.friends)
                                            SignalAudience.Groups -> stringResource(R.string.groups)
                                        }
                                    )
                                },
                                shape = MaterialTheme.shapes.large
                            )
                        }
                    }

                    if (audience == SignalAudience.Groups) {
                        Button(
                            onClick = { showChooseGroups = true },
                            modifier = Modifier.padding(vertical = 0.5f.pad)
                        ) {
                            Text(
                                if (selectedGroups.isEmpty()) stringResource(R.string.choose_group)
                                else pluralStringResource(
                                    R.plurals.x_groups,
                                    selectedGroups.size,
                                    selectedGroups.size.toString()
                                )
                            )
                        }
                    }

                    if (audience == SignalAudience.Nearby) {
                        Spacer(Modifier.height(1.pad))
                        Text(
                            text = if (radius >= 1000) {
                                pluralStringResource(
                                    R.plurals.x_km,
                                    (radius / 1000).toInt(),
                                    if (radius % 1000 == 0.0) (radius / 1000).toInt().toString() else (radius / 1000).toString()
                                )
                            } else {
                                pluralStringResource(R.plurals.x_m, radius.toInt(), radius.toInt().toString())
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = radii.indexOf(radius).toFloat(),
                            onValueChange = { radius = radii[it.roundToInt()] },
                            valueRange = 0f..(radii.size - 1).toFloat(),
                            steps = radii.size - 2
                        )
                    }

                    Spacer(Modifier.height(1.pad))

                    Text(stringResource(R.string.duration), style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(0.5f.pad),
                        modifier = Modifier.padding(vertical = 0.5f.pad)
                    ) {
                        durations.forEach { (d, label) ->
                            FilterChip(
                                selected = duration == d,
                                onClick = { duration = d },
                                label = { Text(label) },
                                shape = MaterialTheme.shapes.large
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(isRecordingAudio) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.pad)
                ) {
                    IconButton(onClick = { audioRecorder.cancelRecording() }) {
                        Icon(
                            Icons.Outlined.Delete,
                            stringResource(R.string.discard_recording),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        stringResource(R.string.recording_audio, recordingAudioDuration.formatTime()),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(1.pad))

            Text(stringResource(R.string.attachments), style = MaterialTheme.typography.labelMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.pad),
                modifier = Modifier.padding(vertical = 0.5f.pad)
            ) {
                IconButton(onClick = { showChoosePhoto = true }, enabled = !isRecordingAudio) {
                    Icon(Icons.Outlined.Photo, stringResource(R.string.attach_photo))
                }
                IconButton(
                    onClick = {
                        if (isRecordingAudio) {
                            audioRecorder.sendActiveRecording()
                        } else {
                            audioRecorder.recordAudio()
                        }
                    },
                    colors = if (isRecordingAudio) IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else IconButtonDefaults.iconButtonColors()
                ) {
                    Icon(if (isRecordingAudio) Icons.AutoMirrored.Outlined.Send else Icons.Outlined.Mic, stringResource(R.string.record_audio))
                }
            }

            SignalAttachmentsEditor(
                photo = photo,
                audio = audio,
                onClearPhoto = { photo = null },
                onClearAudio = { audio = null },
                api = api,
                modifier = Modifier.padding(vertical = 1.pad)
            )

            if (isUploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 1.pad))
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.pad)
            ) {
                TextButton(onClick = onDismissRequest, enabled = !isUploading) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        isUploading = true
                        scope.launch {
                            var photoPath: String? = null

                            photo?.let {
                                api.uploadPhotosFromUris(context, listOf(it), onError = {
                                    isUploading = false
                                    context.showDidntWork()
                                }) {
                                    photoPath = it.urls.firstOrNull()
                                }
                            }

                            if (isUploading) {
                                onSubmit(
                                    SendSignalBody(
                                        signal = signal.id!!,
                                        message = message.takeIf { it.isNotBlank() },
                                        photo = photoPath,
                                        audio = audio,
                                        duration = duration,
                                        radius = if (audience == SignalAudience.Nearby) radius else null,
                                        geo = if (audience == SignalAudience.Nearby) geo?.toList() else null,
                                        audience = audience,
                                        groups = selectedGroups.map { it.id!! }.takeIf { it.isNotEmpty() },
                                        localHour = java.util.Calendar
                                            .getInstance()
                                            .get(java.util.Calendar.HOUR_OF_DAY)
                                    )
                                )
                            }
                        }
                    },
                    enabled = !isUploading && (audience != SignalAudience.Groups || selectedGroups.isNotEmpty())
                ) {
                    Text(stringResource(R.string.send))
                }
            }
        }
    }

    if (showChoosePhoto) {
        ChoosePhotoDialog(
            scope = scope,
            multiple = false,
            onDismissRequest = { showChoosePhoto = false },
            onPhotos = {
                photo = it.firstOrNull()
                showChoosePhoto = false
            },
            onIsGeneratingPhoto = {},
            onGeneratedPhoto = {}
        )
    }

    if (showChooseGroups) {
        ChooseGroupDialog(
            onDismissRequest = { showChooseGroups = false },
            title = stringResource(R.string.choose_group),
            confirmFormatter = { pluralStringResource(R.plurals.x_groups, it.size, it.size.toString()) },
            preselect = selectedGroups,
            onGroupsSelected = {
                selectedGroups = it
                showChooseGroups = false
            }
        )
    }
}
