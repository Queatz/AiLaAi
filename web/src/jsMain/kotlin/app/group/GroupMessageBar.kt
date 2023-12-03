package app.group

import Styles
import androidx.compose.runtime.*
import api
import app.AppStyles
import app.StickersTray
import app.ailaai.api.sendMedia
import app.ailaai.api.sendMessage
import appString
import com.queatz.db.GroupExtended
import com.queatz.db.Message
import com.queatz.db.Sticker
import com.queatz.db.StickerAttachment
import components.IconButton
import json
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.web.attributes.autoFocus
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.get
import org.w3c.files.File
import pickPhotos
import r
import toBytes

@Composable
fun GroupMessageBar(group: GroupExtended, reloadMessages: suspend () -> Unit) {
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

    fun sendPhotos(files: List<File>, message: Message? = null) {

        isSending = true
    scope.launch {
        try {
            val photos = files.map { it.toBytes() }

            api.sendMedia(
                group.group!!.id!!,
                photos,
                message
            ) {
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
                Message(text = text),
                onError = {
                    if (messageText.isBlank()) {
                        messageText = text
                    }
                }
            ) {
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
                    )
                )
            ) {
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
//                    IconButton("mic", "Record audio", styles = { marginLeft(1.r) }) {
//                        // todo
//                    }
                IconButton("image", appString { sendPhoto }, styles = { marginLeft(1.r) }) {
                    pickPhotos {
                        sendPhotos(it)
                    }
                }
                IconButton(if (showStickers) "expand_less" else "expand_more", appString { stickers }, styles = {
                    marginLeft(1.r)
                    marginRight(1.r)
                }) {
                    showStickers = !showStickers
                }
            } else {
                IconButton("send", appString { sendMessage }, styles = { marginLeft(1.r) }) {
                    // todo
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
                        (it.target as HTMLTextAreaElement).style.height = "0"
                        (it.target as HTMLTextAreaElement).style.height =
                            "${(it.target as HTMLTextAreaElement).scrollHeight + 2}px"
                    }
                }
            }

            onInput {
                messageText = it.value
                it.target.style.height = "0"
                it.target.style.height = "${it.target.scrollHeight + 2}px"
            }

            onChange {
                it.target.style.height = "0"
                it.target.style.height = "${it.target.scrollHeight + 2}px"
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
                onDispose {}
            }
        }
    }
}
