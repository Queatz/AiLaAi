package app.group

import Styles
import aiPhoto
import aiStyles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import app.dialog.inputDialog
import app.menu.Menu
import appString
import application
import com.queatz.db.AiPhotoRequest
import com.queatz.db.GroupExtended
import com.queatz.db.Message
import com.queatz.db.PhotosAttachment
import com.queatz.db.Sticker
import com.queatz.db.StickerAttachment
import components.IconButton
import focusable
import json
import kotlinx.browser.window
import kotlinx.coroutines.awaitAnimationFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.attributes.autoFocus
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.rad
import org.jetbrains.compose.web.css.transform
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
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
import kotlin.math.PI

@OptIn(ExperimentalComposeWebApi::class)
@Composable
fun GroupMessageBar(group: GroupExtended, reloadMessages: suspend () -> Unit) {
    val scope = rememberCoroutineScope()

    var messageText by remember {
        mutableStateOf("")
    }

    var isSending by remember(group) {
        mutableStateOf(false)
    }

    var isGenerating by remember(group) {
        mutableStateOf(false)
    }

    var showStickers by remember(group) {
        mutableStateOf(false)
    }

    var aiPrompt by remember {
        mutableStateOf("")
    }

    var aiStyle by remember {
        mutableStateOf<String?>(null)
    }

    var aiStyles by remember {
        mutableStateOf<List<Pair<String, String>>>(emptyList())
    }

    fun describePhoto() {
        if (aiStyles.isEmpty()) {
            scope.launch {
                api.aiStyles {
                    aiStyles = it
                }
            }
        }

        scope.launch {
            val result = inputDialog(
                title = null,
                placeholder = application.appString { describePhoto },
                confirmButton = application.appString { confirm },
                defaultValue = aiPrompt,
                singleLine = false,
                inputStyles = {
                    width(32.r)
                }
            ) { _, _, _ ->
                Div({
                    style {
                        marginTop(1.r)
                        marginLeft(1.r)
                        marginBottom(.5.r)
                        opacity(.5f)
                        fontSize(14.px)
                    }
                }) {
                    // todo translate
                    Text("Style")
                }
                Div({
                    style {
                        overflowY("auto")
                        height(8.r)
                    }
                }) {
                    aiStyles.forEach { (name, style) ->
                        Div({
                            classes(
                                listOf(AppStyles.groupItem, AppStyles.groupItemOnSurface)
                            )

                            if (aiStyle == style) {
                                classes(AppStyles.groupItemSelected)
                            }

                            onClick {
                                aiStyle = if (aiStyle == style) null else style
                            }

                            focusable()
                        }) {
                            Div {
                                Div({
                                    classes(AppStyles.groupItemName)
                                }) {
                                    Text(name)
                                }
                            }
                        }
                    }
                }
            }

            if (!result.isNullOrBlank()) {
                aiPrompt = result
                isGenerating = true
                api.aiPhoto(AiPhotoRequest(result, aiStyle)) { photo ->
                    api.sendMessage(
                        group.group!!.id!!,
                        Message(
                            attachment = json.encodeToString(PhotosAttachment(photos = listOf(photo.photo))),
                        )
                    ) {
                        reloadMessages()
                    }
                }
                isGenerating = false
            }
        }
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

    var photoMenuTarget by remember {
        mutableStateOf<DOMRect?>(null)
    }

    if (photoMenuTarget != null) {
        Menu({ photoMenuTarget = null }, photoMenuTarget!!, above = true) {
            item(appString { describePhoto }) {
                describePhoto()
            }
            item(appString { choosePhoto }) {
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

                val start = remember { Clock.System.now().toEpochMilliseconds() }
                var rotation by remember { mutableStateOf(0.rad) }

                LaunchedEffect(isGenerating) {
                    if (isGenerating) while (true) {
                        rotation = ((Clock.System.now().toEpochMilliseconds() - start) / 2_000.0 * PI).rad
                        delay(50)
                        window.awaitAnimationFrame()
                    }
                }

                IconButton(if (isGenerating) "progress_activity" else "image", appString { sendPhoto }, styles = {
                    marginLeft(1.r)
                }, iconStyles = {
                    if (isGenerating) {
                        property("font-smooth", "never")
                        transform {
                            rotate(rotation)
                        }
                    }
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
                onDispose {}
            }
        }
    }
}
