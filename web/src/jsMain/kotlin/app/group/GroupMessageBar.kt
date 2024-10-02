package app.group

import Styles
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
import json
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.web.attributes.autoFocus
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.DisplayStyle
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
import pickPhotos
import r
import resize
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

    var showStickers by remember(group) {
        mutableStateOf(false)
    }

    val choosePhoto = rememberChoosePhotoDialog()

    val isGenerating = choosePhoto.isGenerating.collectAsState().value

    var focus by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(replyMessage) {
        if (replyMessage != null) {
            focus?.invoke()
        }
    }

    fun describePhoto() {
        choosePhoto.launch { photo ->
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
                e.printStackTrace()
                return@launch
            }

            isSending = false
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
//                    IconButton("mic", "Record audio", styles = { marginLeft(1.r) }) {
//                        // todo
//                    }
                IconButton("image", appString { sendPhoto }, isLoading = isGenerating, styles = {
                    marginLeft(1.r)
                }) {
                    it.stopPropagation()
                    photoMenuTarget =
                        if (photoMenuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
                }
                IconButton(if (showStickers) "expand_less" else "expand_more", appString { stickers }, styles = {
                    marginLeft(1.r)
                    marginRight(1.r)
                }) {
                    showStickers = !showStickers
                }
            } else {
                IconButton("send", appString { sendMessage }, styles = { marginLeft(1.r) }) {
                    sendMessage()
                }
            }
        }
        val messageString = if (isSending) appString { sending } else appString { message }
        // todo can be EditField
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
