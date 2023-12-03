package app.messaages

import Styles
import androidx.compose.runtime.*
import api
import app.AppStyles
import app.StickerItem
import app.ailaai.api.message
import app.dialog.photoDialog
import appString
import baseUrl
import com.queatz.ailaai.api.story
import com.queatz.db.*
import components.CardItem
import components.Icon
import components.LinkifyText
import ellipsize
import kotlinx.browser.window
import kotlinx.coroutines.launch
import lib.formatDistanceToNow
import notBlank
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLVideoElement
import r
import stories.textContent
import kotlin.js.Date

@OptIn(ExperimentalComposeWebApi::class)
@Composable
fun MessageContent(message: Message, myMember: MemberAndPerson?, isReply: Boolean = false) {
    val scope = rememberCoroutineScope()
    val isMe = message.member == myMember?.member?.id

    val attachment = remember(message) {
        message.getAttachment()
    }

    var reply by remember(message) {
        mutableStateOf<Message?>(null)
    }

    LaunchedEffect(attachment) {
        reply = null

        val replyAttachment = message.getAllAttachments().firstNotNullOfOrNull { it as? ReplyAttachment }

        if (isReply || replyAttachment == null) {
            return@LaunchedEffect
        }

        api.message(replyAttachment.message!!) {
            reply = it
        }
    }

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            if (isMe) {
                alignItems(AlignItems.FlexEnd)
            } else {
                alignItems(AlignItems.FlexStart)
            }
        }
        title(
            "${message.createdAt?.let { formatDistanceToNow(Date(it.toEpochMilliseconds()), js("{ addSuffix: true }")) }}\n${message.createdAt?.let { Date(it.toEpochMilliseconds()) }.toString()}"
        )
    }) {
        reply?.let { reply ->
            Div({
                classes(
                    listOf(AppStyles.messageReply) + if (isMe) {
                        listOf(AppStyles.myMessageReply)
                    } else {
                        emptyList()
                    }
                )
            }) {
                Div({
                    classes("material-symbols-outlined")
                    style {
                        position(Position.Absolute)
                        if (isMe) {
                            top(1.r / 4)
                            right(1.r / 4)
                        } else {
                            top(1.r / 4)
                            left(1.r / 4)
                        }
                        opacity(.75)
                        fontSize(12.px)
                    }
                }) {
                    Text("reply")
                }
                MessageContent(reply, myMember, isReply = true)
            }
        }

        when (attachment) {
            is PhotosAttachment -> {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexWrap(FlexWrap.Wrap)

                        if (isMe) {
                            justifyContent(JustifyContent.FlexEnd)
                        }
                    }
                }) {
                    attachment.photos?.forEach { photo ->
                        Img(src = "$baseUrl$photo") {
                            classes(AppStyles.messageItemPhoto)
                            onClick {
                                scope.launch {
                                    photoDialog("$baseUrl$photo")
                                }
                            }
                        }
                    }
                }
            }

            is AudioAttachment -> {
                Audio({
                    attr("controls", "")
                    style {
                        borderRadius(1.r)
                    }
                }) {
                    Source({
                        attr("src", "$baseUrl${attachment.audio}")
                        attr("type", "audio/mp4")
                    })
                }
            }

            is VideosAttachment -> {
                attachment.videos?.forEach {
                    var isPlaying by remember {
                        mutableStateOf(false)
                    }
//                    var videoElement by remember { mutableStateOf<HTMLVideoElement?>(null) }
                    Div({
                        style {
                            position(Position.Relative)
                        }
                    }) {
                        Video({
//                    attr("autoplay", "")
                            attr("loop", "")
                            attr("playsinline", "")
//                        attr("muted", "")
                            classes(AppStyles.messageVideo)
                            onClick {
                                (it.target as? HTMLVideoElement)?.apply {
                                    if (paused) {
                                        play()
                                        isPlaying = true
                                    } else {
                                        pause()
                                        isPlaying = false
                                    }
                                    muted = false
                                }
                            }
//                        // Do this so that auto-play works on page load, but unmute on page navigation
//                        ref { videoEl ->
//                            videoEl.onloadedmetadata = {
//                                videoEl.muted = true
//                                videoElement = videoEl
//                                it
//                            }
//                            onDispose { }
//                        }
                        }) {
                            Source({
                                attr("src", "$baseUrl$it")
                                attr("type", "video/webm")
                            })
                        }
                        if (!isPlaying) {
                            Icon("play_arrow", null) {
                                position(Position.Absolute)
                                borderRadius(100.percent)
                                backgroundColor(rgba(0, 0, 0, .5))
                                padding(.5.r)
                                top(50.percent)
                                left(50.percent)
                                property("pointer-events", "none")

                                transform {
                                    translateX(-50.percent)
                                    translateY(-50.percent)
                                }
                            }
                        }
                    }
                }
            }

            is CardAttachment -> {
                CardItem(attachment.card!!, openInNewWindow = true) {
                    maxWidth(320.px)

                    if (message.text.isNullOrBlank().not()) {
                        marginBottom(1.r)
                    }
                }
            }

            is ReplyAttachment -> {

            }

            is StoryAttachment -> {
                var story by remember(message) {
                    mutableStateOf<Story?>(null)
                }

                LaunchedEffect(Unit) {
                    api.story(attachment.story!!) {
                        story = it
                    }
                }

                story?.let { story ->
                    Div({
                        classes(AppStyles.messageItemStory)

                        onClick {
                            window.open("/story/${story.url ?: story.id}", target = "_blank")
                        }
                    }) {
                        Div({
                            style {
                                marginBottom(.5.r)
                                fontSize(24.px)
                            }
                        }) {
                            Text(story.title ?: appString { createStory })
                        }
                        Div({
                            style {
                                marginBottom(.5.r)
                                color(Styles.colors.secondary)
                                fontSize(16.px)
                            }
                        }) {
                            val someone = appString { someone }
                            Text("${if (story.publishDate != null) appString { published } else appString { draft }} ${appString { inlineBy }} ${story.authors?.joinToString { it.name ?: someone }}")
                        }
                        Div({
                            style {
                                marginBottom(.5.r)
                                ellipsize()
                            }
                        }) {
                            Text(story.textContent())
                        }
                    }
                }
            }

            is StickerAttachment -> {
                StickerItem(
                    attachment.photo!!,
                    attachment.message,
                    96.px,
                    messageAlign = if (isMe) AlignItems.Start else AlignItems.End
                ) {}
            }
        }

        message.text?.notBlank?.let { text ->
            Div({
                classes(
                    listOf(AppStyles.messageItem) + if (isMe) {
                        listOf(AppStyles.myMessage)
                    } else {
                        emptyList()
                    }
                )
            }) {
                LinkifyText(text)
            }
        }
    }
}
