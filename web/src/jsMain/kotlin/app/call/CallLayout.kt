package app.call

import GroupCall
import Styles
import androidx.compose.runtime.*
import app.AppNavigation
import app.appNav
import app.messaages.inList
import app.nav.name
import appString
import application
import call
import components.IconButton
import ellipsize
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Audio
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Video
import r

@OptIn(ExperimentalComposeWebApi::class)
@Composable
fun CallLayout(activeCall: GroupCall) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()
//    val router = Router.current

    var fullscreen by remember {
        mutableStateOf(false)
    }

    Div({
        classes(CallStyles.callRoot)

        if (fullscreen) {
            classes(CallStyles.callRootFullscreen)
        }

        onClick {
            fullscreen = !fullscreen
        }
    }) {
        val activeVideoStreams = activeCall.streams.count {
            it.kind == "video" || it.kind == "share"
        }

        if (activeCall.localShare != null || activeCall.localVideo != null) {
            key(activeCall.localShare ?: activeCall.localVideo) {
                Video({
                    attr("playsinline", "true")
                    attr("disablePictureInPicture", "true")
                    attr("controlsList", "nodownload")

                    style {
                        if (activeCall.streams.isEmpty()) {
                            width(0.r)
                            height(100.percent)
                            flex(1)
                        } else {
                            position(Position.Absolute)
                            height(15.percent)
                            width(15.percent)
                            bottom(1.r)
                            right(1.r)
                            borderRadius(1.r)
                            overflow("hidden")
                            transform {
                                translateZ(1.px)
                            }
                            property("z-index", "1")
                        }

                        if (activeCall.localShare == null) {
                            property("object-fit", "cover")
                        }
                    }

                    ref {
                        it.srcObject = activeCall.localShare ?: activeCall.localVideo
                        it.play()

                        onDispose { }
                    }
                }) {}
            }
        }

        val audioStreams = remember(activeCall) {
            activeCall.streams.filter { it.kind == "audio" }
        }

        val videoStreams = remember(activeCall) {
            activeCall.streams.filter {
                it.kind == "video" || it.kind == "share"
            }.filter {
                activeCall.pinnedStream == null || activeCall.pinnedStream == it.stream
            }
        }

        audioStreams.forEach { participant ->
            key(participant.stream to participant.kind) {
                Audio({
                    attr("playsinline", "true")
                    attr("autoplay", "false")

                    style {
                        display(DisplayStyle.None)
                    }

                    ref {
                        it.srcObject = participant.stream
                        it.play()

                        onDispose { }
                    }
                }) {}
            }

            videoStreams.forEach { participant ->
                key(participant.stream to participant.kind) {
                    Div({
                        style {
                            width(0.r)
                            height(100.percent)
                            flex(1)
                            position(Position.Relative)
                        }
                    }) {
                        Video({
                            attr("playsinline", "true")
                            attr("disablePictureInPicture", "true")
                            attr("controlsList", "nodownload")

                            style {
                                width(100.percent)
                                height(100.percent)

                                if (participant.kind != "share") {
                                    property("object-fit", "cover")
                                }
                            }

                            ref {
                                it.srcObject = participant.stream
                                it.play()

                                onDispose { }
                            }
                        }) {}

                        if (activeVideoStreams > 1) {
                            Div({
                                classes(CallStyles.participantControls)
                            }) {
                                IconButton(
                                    name = if (activeCall.pinnedStream != null) "fullscreen_exit" else "fullscreen",
                                    title = appString { this.fullscreen }
                                ) {
                                    call.togglePin(participant.stream)
                                }
                            }
                        }
                    }
                }
            }
        }

        val openGroup = appString { actionOpenGroup }

        Div({
            style {
                position(Position.Absolute)
                top(.5.r)
                right(1.5.r)
                left(1.5.r)
                fontSize(14.px)
                textAlign("center")
                ellipsize()
                fontWeight("bold")
                cursor("pointer")
            }

            title(openGroup)

            onClick {
                it.stopPropagation()
                scope.launch {
                    appNav.navigate(
                        AppNavigation.Group(activeCall.group.group!!.id!!, activeCall.group)
                    )
//                    router.navigate("/")
                }
            }
        }) {
            Text(activeCall.group.name(
                someone = appString { someone },
                omit = me?.id?.inList() ?: emptyList(),
                emptyGroup = appString { newGroup }
            ))
        }

        IconButton(
            if (fullscreen) "collapse_content" else "expand_content",
            if (fullscreen) appString { minimize } else appString { maximize },
            styles = {
                top(0.r)
                right(0.r)
                opacity(.5)
                position(Position.Absolute)
            }
        ) {
            fullscreen = !fullscreen
        }

        Div({
            style {
                position(Position.Absolute)
                bottom(.5.r)
                left(.5.r)
                right(.5.r)
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                justifyContent(JustifyContent.Center)
                property("z-index", "2")
            }
        }) {
            IconButton(
                name = if (activeCall.localAudio != null) "mic" else "mic_off",
                title = appString { microphone },
                background = true,
                styles = {
                    marginRight(.5.r)
                    if (activeCall.localAudio == null) {
                        backgroundColor(Styles.colors.red)
                    }
                }) {
                call.toggleMic()
            }
            IconButton(
                name = if (activeCall.localVideo != null) "videocam" else "videocam_off",
                title = appString { camera },
                background = true,
                styles = {
                    marginRight(.5.r)
                    if (activeCall.localVideo == null) {
                        backgroundColor(Styles.colors.red)
                    }
                }) {
                call.toggleCamera()
            }
            IconButton(
                name = if (activeCall.localShare != null) "cancel_presentation" else "present_to_all",
                title = appString { shareScreen },
                background = true,
                styles = {
                    marginRight(.5.r)
                    if (activeCall.localShare != null) {
                        backgroundColor(Styles.colors.primary)
                    }
                }) {
                call.toggleScreenShare()
            }
            IconButton(
                name = "call_end",
                title = appString { leave },
                background = true,
                styles = {
                    backgroundColor(Styles.colors.red)
                }
            ) {
                call.end()
            }
        }
    }
}
