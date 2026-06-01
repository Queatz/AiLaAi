package com.queatz.ailaai.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.queatz.ailaai.extensions.formatTime
import com.queatz.ailaai.R
import app.ailaai.api.uploadAudio
import com.queatz.ailaai.api.uploadPhotosFromUris
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.asInputProvider
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.helpers.audioRecorder
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.SignalAttachments
import com.queatz.ailaai.ui.components.SignalAttachmentsEditor
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.SignalReplyBody
import com.queatz.db.SignalSendExtended
import kotlinx.coroutines.launch

@Composable
fun ReplySignalDialog(
    signalSend: SignalSendExtended,
    onDismissRequest: () -> Unit,
    onSubmit: (SignalReplyBody) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<android.net.Uri?>(null) }
    var audio by remember { mutableStateOf<String?>(null) }
    var showChoosePhoto by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingAudioDuration by remember { mutableLongStateOf(0L) }
    var isUploading by remember { mutableStateOf(false) }
    var photoToShow by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

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

    val now = kotlin.time.Clock.System.now()
    val isExpired = signalSend.signalSend.expiry!! <= now
    val alreadyReplied = signalSend.replies?.isNotEmpty() ?: false

    LaunchedEffect(Unit) {
        if (!isExpired && !alreadyReplied) {
            focusRequester.requestFocus()
        }
    }


    photoToShow?.let {
        PhotoDialog(
            onDismissRequest = { photoToShow = null },
            initialMedia = Media.Photo(it),
            medias = listOf(Media.Photo(it))
        )
    }

    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(3.pad)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.reply_to_x, signalSend.person?.name ?: stringResource(R.string.someone)) + " ${signalSend.signal?.emoji ?: ""} ${signalSend.signal?.name ?: ""}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 1.pad)
            )

            if (!signalSend.signalSend.message.isNullOrBlank()) {
                Text(
                    text = signalSend.signalSend.message!!,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 2.pad)
                )
            }

            SignalAttachments(
                photo = signalSend.signalSend.photo,
                audio = signalSend.signalSend.audio,
                api = api,
                modifier = Modifier.padding(bottom = 2.pad),
                onClickPhoto = { photoToShow = it }
            )

            if (isExpired) {
                Text(
                    text = stringResource(R.string.signal_expired),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 2.pad)
                )
            } else if (alreadyReplied) {
                Text(
                    text = stringResource(R.string.already_replied),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.pad)
                )
            }

            if (!isExpired && !alreadyReplied) {
                AnimatedVisibility(!isRecordingAudio) {
                    Column {
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text(stringResource(R.string.your_reply)) },
                            shape = MaterialTheme.shapes.large,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
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
                        Icon(
                            if (isRecordingAudio) Icons.AutoMirrored.Outlined.Send else Icons.Outlined.Mic,
                            stringResource(R.string.record_audio)
                        )
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
            }

            if (isUploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 1.pad))
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().padding(top = 2.pad)
            ) {
                TextButton(onClick = onDismissRequest, enabled = !isUploading) {
                    Text(if (isExpired || alreadyReplied) stringResource(R.string.close) else stringResource(R.string.cancel))
                }
                if (!isExpired && !alreadyReplied) {
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
                                        SignalReplyBody(
                                            message = message.takeIf { it.isNotBlank() },
                                            photo = photoPath,
                                            audio = audio
                                        )
                                    )
                                }
                            }
                        },
                        enabled = !isUploading
                    ) {
                        Text(stringResource(R.string.reply))
                    }
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
}
