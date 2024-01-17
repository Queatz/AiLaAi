package app.call

import GroupCall
import Styles
import androidx.compose.runtime.*
import app.messaages.inList
import app.nav.name
import appString
import application
import call
import components.IconButton
import ellipsize
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Audio
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Video
import r

@Composable
fun CallLayout(activeCall: GroupCall) {
    val me by application.me.collectAsState()

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

        if (activeCall.streams.isEmpty()) {
            if (activeCall.localShare != null || activeCall.localVideo != null) {
                key(activeCall.localShare ?: activeCall.localVideo) {
                    Video({
                        attr("playsinline", "true")
                        attr("disablePictureInPicture", "true")
                        attr("controlsList", "nodownload")

                        style {
                            width(0.r)
                            height(100.percent)
                            flex(1)
                            property("object-fit", "cover")
                        }

                        ref {
                            it.srcObject = activeCall.localShare ?: activeCall.localVideo
                            it.play()

                            onDispose { }
                        }
                    }) {}
                }
            }
        } else {
            activeCall.streams.forEach { participant ->
                if (activeCall.pinnedStream != null && activeCall.pinnedStream != participant.stream) {
                    return@forEach
                }
                key(participant.stream) {

                    if (participant.kind == "video" || participant.kind == "share") {
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
                                    // todo translate
                                    IconButton(
                                        if (activeCall.pinnedStream != null) "fullscreen_exit" else "fullscreen",
                                        "Fullscreen"
                                    ) {
                                        call.togglePin(participant.stream)
                                    }
                                }
                            }
                        }
                    } else {
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
                }
            }
        }

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
            }
        }) {
            Text(activeCall.group.name(
                someone = appString { someone },
                omit = me?.id?.inList() ?: emptyList(),
                emptyGroup = appString { newGroup }
            ))
        }

        IconButton("close", appString { close }, styles = {
            top(0.r)
            right(0.r)
            opacity(.5)
            position(Position.Absolute)
        }) {
            call.end()
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
            }
        }) {
            // todo translate
            IconButton(if (activeCall.localAudio != null) "mic" else "mic_off", "Microphone", background = true, styles = {
                marginRight(.5.r)
                if (activeCall.localAudio == null) {
                backgroundColor(Styles.colors.red)
                    }
            }) {
                call.toggleMic()
            }
            // todo translate
            IconButton(if (activeCall.localVideo != null) "videocam" else "videocam_off", "Camera", background = true, styles = {
                marginRight(.5.r)
                if (activeCall.localVideo == null) {
                    backgroundColor(Styles.colors.red)
                }
            }) {
                call.toggleCamera()
            }
            IconButton(
                if (activeCall.localShare != null) "cancel_presentation" else "present_to_all",
                // todo translate
                "Share screen",
                background = true,
                styles = {
                    marginRight(.5.r)
                    if (activeCall.localShare != null) {
                        backgroundColor(Styles.colors.primary)
                    }
                }) {
                call.toggleScreenShare()
            }
        }
    }
}
