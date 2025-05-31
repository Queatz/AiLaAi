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
import app.dialog.rememberChoosePhotoDialog
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
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.files.File
import pickAudio
import r
import toBytes

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
        mutableStateOf<web.blob.Blob?>(null)
    }

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
            onSubmit = {
                sendMessage()
                true
            },
            onDismissRequest = {
                // No action needed
            },
            onPhotoSelected = { url: String, _: Int?, _: Int? ->
                sendPhotos(listOf(File(arrayOf(url), "pasted-image.jpg")))
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
