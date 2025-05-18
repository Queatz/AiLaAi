package app.group

import Styles
import aiTranscribe
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import app.dialog.rememberChoosePhotoDialog
import app.menu.Menu
import app.messaages.attachmentText
import appString
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.autoFocus
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.get
import org.w3c.files.File
import pickAudio
import pickPhotos
import r
import resize
import toBytes
import web.events.EventHandler
import web.media.recorder.BlobEvent
import web.media.recorder.MediaRecorder
import web.media.recorder.MediaRecorderOptions
import web.media.streams.MediaStream
import web.media.streams.MediaStreamConstraints
import web.media.streams.MediaTrackConstraints
import web.navigator.navigator

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

    var isTranscribingAudio by remember(group) {
        mutableStateOf(false)
    }

    var isSendingAudio by remember(group) {
        mutableStateOf(false)
    }

    var showStickers by remember(group) {
        mutableStateOf(false)
    }

    var isRecording by remember(group) {
        mutableStateOf(false)
    }

    var mediaRecorder by remember(group) {
        mutableStateOf<MediaRecorder?>(null)
    }

    var mediaStream by remember(group) {
        mutableStateOf<MediaStream?>(null)
    }

    var audioBlob by remember(group) {
        mutableStateOf<web.blob.Blob?>(null)
    }

    val choosePhoto = rememberChoosePhotoDialog()
    val isGenerating = choosePhoto.isGenerating.collectAsState().value
    var focus by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun startRecording() {
        scope.launch {
            try {
                val constraints = MediaStreamConstraints(audio = MediaTrackConstraints())

                val stream = navigator.mediaDevices.getUserMedia(constraints)
                mediaStream = stream

                val options = MediaRecorderOptions(
                    mimeType = "audio/webm"
                )

                mediaRecorder = MediaRecorder(mediaStream!!, options).apply {
                    ondataavailable = EventHandler { event: BlobEvent ->
                        audioBlob = event.data
                    }

                    onstop = EventHandler {
                        mediaStream?.getTracks()?.forEach { it.stop() }
                        mediaStream = null
                    }

                    start()
                }

                isRecording = true
            } catch (e: Exception) {
                console.error("Error starting recording: ${e.message}")
            }
        }
    }

    LaunchedEffect(audioBlob) {
        audioBlob?.let { blob ->
            isTranscribingAudio = true

            scope.launch {
                try {
                    val bytes = blob.bytes().toByteArray()

                    api.aiTranscribe(
                        audio = bytes,
                        onError = { error ->
                            console.error("Error transcribing audio: ${error.message}")
                        }
                    ) { response ->
                        response.text.let { text ->
                            if (text.isNotBlank()) {
                                messageText += if (messageText.isNotBlank()) " $text" else text
                                focus?.invoke()
                            }
                        }
                    }
                } catch (e: Exception) {
                    console.error("Error processing audio: ${e.message}")
                }

                isTranscribingAudio = false
            }

            audioBlob = null
        }
    }

    fun stopRecording(isCancel: Boolean = false) {
        mediaRecorder?.apply {
            if (isCancel) {
                ondataavailable = null
                stream.getTracks().forEach {
                    it.stop()
                }
            }
            stop()
        }

        mediaRecorder = null
        isRecording = false
    }

    fun cancelRecording() {
        stopRecording(isCancel = true)
    }

    DisposableEffect(Unit) {
        onDispose {
            cancelRecording()
        }
    }

    LaunchedEffect(replyMessage) {
        if (replyMessage != null) {
            focus?.invoke()
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
                    group.group!!.id!!,
                    photos,
                    replyMessage?.let { replyMessage ->
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

    var photoMenuTarget by remember {
        mutableStateOf<DOMRect?>(null)
    }

    if (photoMenuTarget != null) {
        Menu({ photoMenuTarget = null }, photoMenuTarget!!, above = true) {
            item(appString { describePhoto }) {
                describePhoto()
            }
            item(appString { this.choosePhoto }) {
                pickPhotos {
                    sendPhotos(it)
                }
            }
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
                IconButton(
                    name = "play_circle",
                    // todo: translate
                    title = "Send audio",
                    isLoading = isSendingAudio,
                    styles = { marginLeft(1.r) }
                ) {
                    pickAudio { file ->
                        sendAudio(file)
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
                    photoMenuTarget =
                        if (photoMenuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
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
        Div({
            style {
                position(Position.Relative)
                width(100.percent)
            }
        }) {
            TextArea(messageText) {
                classes(Styles.textarea)
                style {
                    width(100.percent)
                    height(3.5.r)
                    maxHeight(6.5.r)
                }

                placeholder(messageString)

                onKeyDown {
                    if (it.key == "Enter" && !it.shiftKey) {
                        sendMessage()
                        it.preventDefault()
                        scope.launch {
                            delay(1)
                            (it.target as HTMLTextAreaElement).resize()
                        }
                    } else if (it.key == "Escape") {
                        cancelRecording()
                    }
                }

                onInput {
                    messageText = it.value
                    it.target.resize()
                }

                onChange {
                    it.target.resize()
                }

                onPaste {
                    val items = it.clipboardData?.items ?: return@onPaste

                    val photos = (0 until items.length).mapNotNull {
                        items[it]
                    }.filter {
                        it.type.startsWith("image/")
                    }.mapNotNull {
                        it.getAsFile()
                    }

                    if (photos.isEmpty()) return@onPaste

                    sendPhotos(photos)

                    it.preventDefault()
                }

                autoFocus()

                ref { element ->
                    element.focus()
                    focus = { element.focus() }
                    onDispose {
                        focus = null
                    }
                }
            }
            IconButton(
                name = if (isRecording) "stop" else "mic",
                // todo: translate
                title = if (isRecording) "Finish voice input" else "Voice input",
                isLoading = !isRecording && isTranscribingAudio,
                styles = {
                    position(Position.Absolute)
                    right(.5.r)
                    top(.5.r)
                    bottom(.5.r)
                    opacity(.5f)
                }
            ) {
                if (isRecording) {
                    stopRecording()
                } else {
                    startRecording()
                }
                focus?.invoke()
            }
        }
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
