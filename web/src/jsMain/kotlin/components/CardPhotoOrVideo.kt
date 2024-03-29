package components

import Styles
import androidx.compose.runtime.*
import app.dialog.photoDialog
import baseUrl
import com.queatz.db.Card
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Source
import org.jetbrains.compose.web.dom.Video
import org.w3c.dom.HTMLVideoElement

@Composable
fun CardPhotoOrVideo(card: Card, styles: StyleScope.() -> Unit = {}) {
    val scope = rememberCoroutineScope()

    card.photo?.let {
        val url = "$baseUrl$it"

        Div({
            style {
                width(100.percent)
                backgroundColor(Styles.colors.background)
                backgroundImage("url($url)")
                backgroundPosition("center")
                backgroundSize("cover")
                maxHeight(50.vh)
                property("aspect-ratio", "2")
                cursor("pointer")
                styles()
            }

            onClick {
                scope.launch {
                    photoDialog(url)
                }
            }
        }) {}
    } ?: card.video?.let {
        var videoElement by remember { mutableStateOf<HTMLVideoElement?>(null) }
//                            LaunchedEffect(videoElement) {
//                                if (videoElement != null) {
//                                    delay(250)
//                                    try {
////                                        if (window.navigator.getAutoplayPolicy)
//                                        videoElement!!.muted = false
//                                    } catch (e: Throwable) {
//                                        // ignore
//                                    }
//                                }
//                            }
        Video({
            attr("muted", "muted")
            attr("autoplay", "")
            attr("loop", "")
            attr("playsinline", "")
            style {
                property("object-fit", "cover")
                width(100.percent)
                backgroundColor(Styles.colors.background)
                property("aspect-ratio", "2")
                styles()
            }
            onClick {
                (it.target as? HTMLVideoElement)?.apply {
                    play()
                    muted = false
                }
            }
            // Do this so that auto-play works on page load, but unmute on page navigation
            ref { videoEl ->
                videoEl.onloadedmetadata = {
                    videoEl.muted = true
                    videoElement = videoEl
                    it
                }
                onDispose {  }
            }
        }) {
            Source({
                attr("src", "$baseUrl$it")
                attr("type", "video/webm")
            })
        }
    }
}
