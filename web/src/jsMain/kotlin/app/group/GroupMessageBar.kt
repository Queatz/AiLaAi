package app.group

import aiTranscribe
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.StickersTray
import app.ailaai.api.sendAudio
import app.ailaai.api.sendMedia
import app.ailaai.api.sendMessage
import app.components.FlexInput
import app.components.FlexInputControl
import app.dialog.dialog
import app.dialog.rememberChoosePhotoDialog
import app.menu.Menu
import app.messaages.attachmentText
import appString
import com.queatz.db.AiTranscribeResponse
import com.queatz.db.GroupExtended
import com.queatz.db.Message
import com.queatz.db.PhotosAttachment
import com.queatz.db.ReplyAttachment
import com.queatz.db.Sticker
import com.queatz.db.StickerAttachment
import components.Icon
import components.IconButton
import js.typedarrays.toByteArray
import json
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import org.w3c.files.File
import pickAudio
import r
import toBytes
import web.blob.Blob
import web.events.EventHandler
import web.media.recorder.BlobEvent
import web.media.recorder.MediaRecorder
import web.media.streams.MediaStream
import web.media.streams.MediaStreamConstraints
import web.media.streams.MediaTrackConstraints
import web.navigator.navigator
import Styles
import format
import format1Decimal
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.px
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.web.css.alignItems
import pad
import kotlin.js.Date

@Composable
fun GroupMessageBar(
    group: GroupExtended,
    replyMessage: Message?,
    clearReplyMessage: () -> Unit,
    reloadMessages: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var messageText by remember {
        mutableStateOf("")
    }

    var isSending by remember(group) {
        mutableStateOf(false)
    }


    var isSendingAudio by remember(group) {
        mutableStateOf(false)
    }

    var showStickers by remember(group) {
        mutableStateOf(false)
    }

    var audioBlob by remember(group) {
        mutableStateOf<Blob?>(null)
    }
    // Audio menu state
    var showAudioMenu by remember { mutableStateOf(false) }
    var audioMenuTarget by remember { mutableStateOf<DOMRect?>(null) }
    var audioIconRef by remember { mutableStateOf<HTMLElement?>(null) }

    val choosePhoto = rememberChoosePhotoDialog(showUpload = true)
    val isGenerating = choosePhoto.isGenerating.collectAsState().value
    val control = remember {
        FlexInputControl()
    }

    LaunchedEffect(audioBlob) {
        audioBlob?.let { blob ->
            scope.launch {
                try {
                    val bytes = blob.bytes().toByteArray()

                    api.aiTranscribe(
                        audio = bytes,
                        onError = { error: Throwable ->
                            console.error("Error transcribing audio: ${error.message}")
                        }
                    ) { response: AiTranscribeResponse ->
                        response.text.let { text ->
                            if (text.isNotBlank()) {
                                messageText += if (messageText.isNotBlank()) " $text" else text
                                control.focus()
                            }
                        }
                    }
                } catch (e: Exception) {
                    console.error("Error processing audio: ${e.message}")
                }
            }

            audioBlob = null
        }
    }

    LaunchedEffect(replyMessage) {
        if (replyMessage != null) {
            control.focus()
        }
    }

    fun describePhoto() {
        choosePhoto.launch(
            multiple = true
        ) { photo, _, _ ->
            api.sendMessage(
                group.group!!.id!!,
                Message(
                    attachment = json.encodeToString(PhotosAttachment(photos = listOf(photo))),
                    attachments = replyMessage?.id?.let {
                        listOf(json.encodeToString(ReplyAttachment(it)))
                    }
                )
            ) {
                clearReplyMessage()
                reloadMessages()
            }
        }
    }

    fun sendPhotos(files: List<File>) {
        isSending = true
        scope.launch {
            try {
                val photos = files.map { it.toBytes() }

                api.sendMedia(
                    group = group.group!!.id!!,
                    photos = photos,
                    message = replyMessage?.let { replyMessage ->
                        Message(
                            attachments = replyMessage.id?.let {
                                listOf(json.encodeToString(ReplyAttachment(it)))
                            }
                        )
                    }
                ) {
                    clearReplyMessage()
                    reloadMessages()
                }
            } catch (e: Throwable) {
                console.error(e)
                return@launch
            }

            isSending = false
        }
    }

    fun sendAudio(file: File) {
        isSendingAudio = true
        scope.launch {
            try {
                // Convert File to ByteArray
                val audioBytes = file.toBytes()

                api.sendAudio(
                    group = group.group!!.id!!,
                    audio = audioBytes,
                    message = replyMessage?.let { replyMessage ->
                        Message(
                            attachments = replyMessage.id?.let {
                                listOf(json.encodeToString(ReplyAttachment(it)))
                            }
                        )
                    }
                ) {
                    clearReplyMessage()
                    reloadMessages()
                }
            } catch (e: Throwable) {
                isSendingAudio = false
                console.error(e)
                return@launch
            }

            isSendingAudio = false
        }
    }

    fun recordAudioDialog() {
        showAudioMenu = false
        scope.launch {
            val enableConfirm = MutableStateFlow(false)
            var recordedFile = MutableStateFlow<File?>(null)
            val result = dialog(
                title = null,
                confirmButton = "Send audio",
                cancelButton = "Cancel",
                enableConfirm = {
                    enableConfirm.collectAsState().value
                }
            ) { _ ->
                var isRecording by remember { mutableStateOf(false) }
                var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
                // Recording timer state
                var startTime by remember { mutableStateOf(0.0) }
                var elapsedSecs by remember { mutableStateOf(0) }

                LaunchedEffect(isRecording) {
                    if (isRecording) {
                        startTime = Date.now()
                        while (isRecording) {
                            elapsedSecs = ((Date.now() - startTime) / 1000).toInt()
                            delay(200)
                        }
                    }
                }

                val r = recordedFile.collectAsState().value

                LaunchedEffect(isRecording, r) {
                    enableConfirm.value = !isRecording && r != null
                }

                // Recording controls
                Div({ style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    gap(1.r)
                } }) {
                    // Record/stop button
                    IconButton(
                        name = if (!isRecording) "mic" else "stop",
                        title = if (!isRecording) "Start recording" else "Stop recording",
                        background = isRecording,
                        backgroundColor = Styles.colors.red,
                        styles = { fontSize(32.px) },
                        onClick = {
                            if (!isRecording) {
                                scope.launch {
                                    val constraints = MediaStreamConstraints(
                                        audio = MediaTrackConstraints(),
                                        video = null
                                    )
                                    val stream: MediaStream = navigator.mediaDevices.getUserMedia(constraints)
                                    val rec = MediaRecorder(stream)
                                    recorder = rec
                                    rec.ondataavailable = EventHandler { event: BlobEvent ->
                                        recordedFile.value = File(arrayOf(event.data), "recording.webm")
                                    }
                                    rec.start()
                                    isRecording = true
                                    // Start timer
                                    startTime = Date.now()
                                }
                            } else {
                                recorder?.stop()
                                isRecording = false
                            }
                        }
                    )
                    // Timer display
                    if (isRecording) {
                        Text("${(elapsedSecs / 60)}:${(elapsedSecs % 60).pad()}")
                    } else {
                        Text("Click to record")
                    }
                }
            }
            if (result == true && recordedFile.value != null) {
                sendAudio(recordedFile.value!!)
            }
        }
    }

    fun sendMessage() {
        if (messageText.isBlank()) return

        val text = messageText
        messageText = ""

        isSending = true

        scope.launch {
            api.sendMessage(
                group.group!!.id!!,
                Message(
                    text = text,
                    attachments = replyMessage?.id?.let {
                        listOf(json.encodeToString(ReplyAttachment(it)))
                    }
                ),
                onError = {
                    if (messageText.isBlank()) {
                        messageText = text
                    }
                }
            ) {
                clearReplyMessage()
                reloadMessages()
            }

            isSending = false
        }
    }

    fun sendSticker(sticker: Sticker) {
        isSending = true
        scope.launch {
            api.sendMessage(
                group.group!!.id!!,
                Message(
                    attachment = json.encodeToString(
                        StickerAttachment(
                            sticker.photo,
                            sticker.id,
                            sticker.message
                        )
                    ),
                    attachments = replyMessage?.id?.let {
                        listOf(json.encodeToString(ReplyAttachment(it)))
                    }
                )
            ) {
                clearReplyMessage()
                reloadMessages()
            }

            isSending = false
        }
    }

    if (showStickers) {
        Div({
            classes(AppStyles.tray)
            style {
                marginLeft(1.r)
                marginRight(1.r)
                marginBottom(1.r)
            }
        }) {
            StickersTray {
                sendSticker(it)
                showStickers = false
            }
        }
    }

    Div({
        classes(AppStyles.messageBar)
    }) {
        Div({
            style {
                flexShrink(0)
                display(DisplayStyle.Flex)
            }
        }) {
            if (messageText.isBlank()) {
                // Audio menu icon
                Span({
                    style { marginLeft(1.r) }
                    ref {
                        audioIconRef = it
                        onDispose { }
                    }
                }) {
                    IconButton(
                        name = "play_circle",
                        // todo: translate
                        title = "Send audio",
                        isLoading = isSendingAudio,
                    ) {
                        audioIconRef?.getBoundingClientRect()?.let { rect ->
                            audioMenuTarget = rect
                            showAudioMenu = true
                        }
                    }
                    if (showAudioMenu && audioMenuTarget != null) {
                        Menu(
                            onDismissRequest = { showAudioMenu = false },
                            above = true,
                            target = audioMenuTarget!!
                        ) {
                            item(
                                title = "Upload audio",
                                icon = "upload",
                                onClick = {
                                    pickAudio { file -> sendAudio(file) }
                                }
                            )
                            item(
                                title = "Record audio",
                                icon = "mic",
                                onClick = {
                                    recordAudioDialog()
                                }
                            )
                        }
                    }
                }
                IconButton(
                    name = "image",
                    title = appString { sendPhoto },
                    isLoading = isGenerating,
                    styles = {
                        marginLeft(1.r)
                    }
                ) {
                    it.stopPropagation()
                    describePhoto()
                }
                IconButton(
                    name = if (showStickers) "expand_less" else "expand_more",
                    title = appString { stickers },
                    styles = {
                        marginLeft(1.r)
                        marginRight(1.r)
                    }
                ) {
                    showStickers = !showStickers
                }
            } else {
                IconButton(
                    name = "send",
                    title = appString { sendMessage },
                    styles = { marginLeft(1.r) }
                ) {
                    sendMessage()
                }
            }
        }
        val messageString = if (isSending) appString { sending } else appString { message }

        FlexInput(
            value = messageText,
            onChange = { newValue -> messageText = newValue },
            control = control,
            placeholder = messageString,
            autoFocus = true,
            autoSize = true,
            enablePhotoPasting = true,
            useDefaultWidth = false,
            onSubmit = {
                sendMessage()
                true
            },
            onDismissRequest = {
                // No action needed
            },
            onPhotoFiles = { photos: List<File> ->
                sendPhotos(photos)
            },
            styles = {
                width(0.r)
                flexGrow(1)
            }
        )
    }

    replyMessage?.let { replyMessage ->
        Div({
            classes(AppStyles.groupMessageReply)

            onClick {
                clearReplyMessage()
            }
        }) {
            Icon("close")
            Span({
                style {
                    flexGrow(1)
                }
            }) {
                Text(replyMessage.text ?: replyMessage.attachmentText().orEmpty())
            }
            Icon("reply", styles = {
                opacity(.5f)
            })
        }
    }
}
