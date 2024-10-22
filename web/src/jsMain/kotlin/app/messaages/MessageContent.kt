package app.messaages

import Styles
import androidx.compose.runtime.*
import api
import app.AppNavigation
import app.AppStyles
import app.StickerItem
import app.ailaai.api.group
import app.ailaai.api.message
import app.appNav
import app.dialog.photoDialog
import app.group.GroupInfo
import app.group.GroupItem
import appString
import baseUrl
import com.queatz.ailaai.api.story
import com.queatz.db.*
import components.CardItem
import components.Icon
import components.LinkifyText
import components.LoadingText
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
import stories.StoryStyles
import stories.textContent
import kotlin.js.Date

@OptIn(ExperimentalComposeWebApi::class)
@Composable
fun MessageContent(
    message: Message,
    myMember: MemberAndPerson?,
    isReply: Boolean = false,
    onShowingStickerMessage: ((Boolean) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val isMe = message.member == myMember?.member?.id

    val attachments = remember(message) {
        message.getAllAttachments()
    }

    var reply by remember(message) {
        mutableStateOf<Message?>(null)
    }

    LaunchedEffect(attachments) {
        reply = null

        val replyAttachment = attachments.firstNotNullOfOrNull { it as? ReplyAttachment }

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
//            width(100.percent)
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

        attachments.forEach { attachment ->
            when (attachment) {
                is PhotosAttachment -> {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            flexWrap(FlexWrap.Wrap)
                            gap(.5.r)

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

                is GroupAttachment -> {
                    Div({
                        classes(StoryStyles.contentGroups, StoryStyles.contentGroupsInMessage)
                    }) {
                        attachment.group?.let { groupId ->
                            var group by remember(groupId) {
                                mutableStateOf<GroupExtended?>(null)
                            }

                            LaunchedEffect(groupId) {
                                api.group(groupId) {
                                    group = it
                                }
                            }

                            LoadingText(group != null, appString { loadingGroup }) {
                                group?.let { group ->
                                    GroupItem(
                                        group = group,
                                        selectable = true,
                                        selected = false,
                                        onBackground = true,
                                        onSelected = {
                                            scope.launch {
                                                appNav.navigate(AppNavigation.Group(group.group!!.id!!, group))
                                            }
                                        },
                                        info = GroupInfo.LatestMessage
                                    )
                                }
                            }
                        }
                    }
                }

                is StickerAttachment -> {
                    StickerItem(
                        photo = attachment.photo!!,
                        message = attachment.message,
                        size = 96.px,
                        messageAlign = if (isMe) AlignItems.Start else AlignItems.End,
                        onMessageShown = onShowingStickerMessage
                    ) {}
                }

                is UrlAttachment -> {
                    UrlPreview(attachment)
                }

                is TradeAttachment -> {
                    // todo
                }
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
